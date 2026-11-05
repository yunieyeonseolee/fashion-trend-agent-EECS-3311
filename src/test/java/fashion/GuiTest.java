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
        var adapter = new CsvTrendAdapter(new CsvReader());
        var agent = new FashionTrendAgent(new TrendRepository(path -> {
            importedOffEdt.set(!SwingUtilities.isEventDispatchThread());
            return adapter.importData(path);
        }));
        SwingUtilities.invokeAndWait(() -> view = new TrendGUI(agent));
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
