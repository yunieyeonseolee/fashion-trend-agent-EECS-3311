package fashion;

import fashion.application.*;
import fashion.data.*;
import fashion.gui.TrendGUI;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Real Swing controls and workers, runnable without a desktop or window server. */
public final class GuiTest {
    private static int checks;
    private static TrendGUI view;

    public static void main(String[] args) throws Exception {
        var importedOffEdt = new AtomicBoolean();
        var apiCalls = new java.util.concurrent.atomic.AtomicInteger();
        var adapter = new CsvTrendAdapter(new CsvReader());
        var agent = new FashionTrendAgent(new TrendRepository(path -> {
            importedOffEdt.set(!SwingUtilities.isEventDispatchThread());
            return adapter.importData(path);
        }), (instructions,input,schema) -> {
            apiCalls.incrementAndGet();
            return new fashion.llm.LLMClient.Reply(schema == null
                ? "TEST ONLY: synthetic data explanation."
                : "{\"steps\":[{\"operation\":\"ANALYZE\",\"name\":\"mary jane flats\",\"secondName\":null,\"category\":null,\"from\":null,\"to\":null,\"ranking\":\"MOMENTUM\"}],\"clarification\":\"\"}",
                "test-only-model",10,5);
        });
        SwingUtilities.invokeAndWait(() -> { TrendGUI.configureLookAndFeel(); view = new TrendGUI(agent); });
        onEdt(() -> check(!button("run").isEnabled(), "run disabled before import"));
        var pending = new AtomicReference<CompletableFuture<AgentResponse>>();
        onEdt(() -> pending.set(view.importCsv(Path.of("data/sample-trends.csv"))));
        check(pending.get().get(10, TimeUnit.SECONDS).success(), "import via GUI");
        check(importedOffEdt.get(), "CSV import runs off EDT");
        onEdt(() -> {
            check(button("run").isEnabled(), "run enabled after import");
            field("trend").setText("mary");
            button("run").doClick();
            check(!button("import").isEnabled(), "disable concurrent imports during work");
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount() == 1 && table().getValueAt(0,0).equals("mary jane flats"), "search results rendered");
            check(!table().isCellEditable(0,0), "table is read only");
            select("History");
            field("trend").setText("mary jane flats");
            field("from").setText("2026-09-01");
            field("to").setText("2026-10-01");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount() == 2 && table().getColumnName(0).equals("Date"), "filtered history displayed");
            field("from").setText("bad-date");
            button("run").doClick();
            check(details().getText().startsWith("Error:"), "invalid date is visible");
            check(table().getRowCount() == 0, "stale results cleared on error");
            check(((JLabel)find(view,"popularitySummary")).getText().equals("—"), "stale summary cleared on error");
            select("Search");
            check(!field("from").isEnabled(), "irrelevant date field disabled");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount() == 1, "disabled invalid date does not affect search");
            select("Analyze");
            field("from").setText(""); field("to").setText("");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getValueAt(0,2).equals("Growing"), "lifecycle displayed");
            check(table().getColumnClass(5) == Double.class, "metrics sort numerically");
            check(((JLabel)find(view,"popularitySummary")).getText().equals("58.0"), "analysis summary shows calculated popularity");
            check(((JLabel)find(view,"lifecycleSummary")).getText().equals("Growing"), "analysis summary shows lifecycle");
            view.setSize(1220,760); layout(view);
            var preview = new BufferedImage(1220,760,BufferedImage.TYPE_INT_RGB);
            var graphics = preview.createGraphics(); view.printAll(graphics); graphics.dispose();
            try { javax.imageio.ImageIO.write(preview,"png",Path.of("build/gui-analysis-preview.png").toFile()); }
            catch (java.io.IOException error) { throw new RuntimeException(error); }
            select("Compare"); field("secondTrend").setText("unknown");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount()==1 && details().getText().contains("Partial comparison"), "partial comparison displayed");
            select("Emerging"); field("category").setText("accessories");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getValueAt(0,0).equals("bag charms"), "emerging control executes discovery");
            select("Discover"); field("category").setText("footwear");
            ((JComboBox<?>) find(view,"ranking")).setSelectedIndex(1);
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount()==2, "category discovery rendered");
            table().getRowSorter().toggleSortOrder(0);
            table().setRowSelectionInterval(0,0);
            check(((JLabel)find(view,"summaryTitle")).getText().startsWith(table().getValueAt(0,0).toString()),
                    "summary follows sorted selection");
            select("History"); field("category").setText("");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount()==4, "unfiltered history rendered");
            view.setSize(1220,760);
            layout(view);
            var image = new BufferedImage(1220,760,BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            view.printAll(g);
            g.dispose();
            try { javax.imageio.ImageIO.write(image,"png",Path.of("build/gui-preview.png").toFile()); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        onEdt(() -> {
            select("Ask"); field("trend").setText("How are Mary Jane flats doing?");
            check(!field("category").isEnabled(), "ask uses question instead of hidden filters");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(((JTextArea)find(view,"report")).getText().contains("TEST ONLY"), "AI interpretation rendered in report tab");
            select("Report"); field("trend").setText("mary jane flats");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> check(((JTextArea)find(view,"report")).getText().contains("DISCOVER"), "report tab includes executed tools"));
        onEdt(() -> check(button("exportReport").isEnabled(), "export enabled for generated report"));
        Path exportDirectory = java.nio.file.Files.createTempDirectory("fashion-export-test-");
        Path exported = exportDirectory.resolve("report.txt");
        var saved = new AtomicReference<CompletableFuture<Path>>();
        int callsBeforeExport = apiCalls.get();
        try {
            onEdt(() -> saved.set(view.exportReport(exported)));
            saved.get().get(10,TimeUnit.SECONDS);
            String content = java.nio.file.Files.readString(exported);
            check(content.contains("TEST ONLY") && content.contains("synthetic-example") && content.contains("34.39"),
                    "export preserves narrative, sources and calculated metrics");
            check(apiCalls.get() == callsBeforeExport, "export makes no extra API request");
            onEdt(() -> saved.set(view.exportReport(exported)));
            try { saved.get().get(10,TimeUnit.SECONDS); throw new AssertionError("existing file overwritten"); }
            catch (ExecutionException expected) { checks++; }
            check(java.nio.file.Files.readString(exported).equals(content), "existing export remains unchanged");
            onEdt(() -> check(details().getText().contains("already exists") && button("exportReport").isEnabled(),
                    "overwrite error visible and report available for retry"));
            onEdt(() -> saved.set(view.exportReport(exportDirectory.resolve("missing-folder/report.txt"))));
            try { saved.get().get(10,TimeUnit.SECONDS); throw new AssertionError("missing folder accepted"); }
            catch (ExecutionException expected) { checks++; }
            onEdt(() -> check(button("run").isEnabled(), "failed export restores controls"));
        } finally {
            java.nio.file.Files.deleteIfExists(exported);
            java.nio.file.Files.deleteIfExists(exportDirectory);
        }
        onEdt(() -> {
            select("Discover"); field("category").setText("footwear");
            button("run").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(!button("exportReport").isEnabled(), "new non-report results cannot export stale report");
            check(!button("analyzeSelected").isEnabled(), "row actions require selection");
            table().getRowSorter().toggleSortOrder(0);
            table().setRowSelectionInterval(0,0);
            check(table().getValueAt(0,0).equals("chunky sneakers"), "sorted selection uses displayed ordering");
            check(button("analyzeSelected").isEnabled(), "row actions enabled with selection");
            button("analyzeSelected").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(field("trend").getText().equals("chunky sneakers") && table().getValueAt(0,2).equals("Declining"),
                    "analysis follows selected view row after sorting");
            table().setRowSelectionInterval(0,0);
            field("trend").setText("mary jane flats");
            button("compareSelected").doClick();
            check(field("secondTrend").getText().equals("chunky sneakers") && field("trend").getText().equals("mary jane flats"),
                    "comparison fills second trend without replacing first");
            button("historySelected").doClick();
        });
        awaitReady();
        onEdt(() -> {
            check(table().getRowCount()==4 && field("trend").getText().equals("chunky sneakers"), "history follows selected analysis row");
            field("from").setText("invalid"); button("run").doClick();
            check(!button("historySelected").isEnabled(), "error clears stale row actions");
        });
        System.out.println("Passed " + checks + " Swing GUI checks; preview: build/gui-preview.png");
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container c) layout(c);
    }
    private static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component match = find(container,name);
                if (match != null) return match;
            }
        }
        return null;
    }
    private static JTextField field(String name) { return (JTextField) find(view,name); }
    private static JButton button(String name) { return (JButton) find(view,name); }
    private static JTable table() { return (JTable) find(view,"results"); }
    private static JTextArea details() { return (JTextArea) find(view,"details"); }
    private static void select(String operation) { ((JComboBox<?>) find(view,"operation")).setSelectedItem(operation); }
    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static void awaitReady() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        var ready = new AtomicBoolean();
        while (System.nanoTime() < deadline) {
            onEdt(() -> ready.set(button("run").isEnabled()));
            if (ready.get()) return;
            Thread.sleep(10);
        }
        throw new AssertionError("GUI operation timed out");
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
