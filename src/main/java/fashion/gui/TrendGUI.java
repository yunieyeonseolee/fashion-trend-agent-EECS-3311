package fashion.gui;

import fashion.application.*;
import fashion.command.*;
import fashion.data.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static fashion.application.AgentRequest.Operation.*;

/** MVC view: gathers input and renders AgentResponse; commands own application work. */
public final class TrendGUI extends JPanel {
    private final FashionTrendAgent agent;
    private final AgentController controller = new AgentController();
    private final JComboBox<String> operation = named(new JComboBox<>(new String[]{
            "Search", "History", "Analyze", "Discover", "Emerging", "Compare", "Ask", "Report"}), "operation");
    private final JTextField trend = named(new JTextField(18), "trend");
    private final JTextField second = named(new JTextField(18), "secondTrend");
    private final JTextField category = named(new JTextField(14), "category");
    private final JTextField from = named(new JTextField(10), "from");
    private final JTextField to = named(new JTextField(10), "to");
    private final JComboBox<String> ranking = named(new JComboBox<>(new String[]{"Momentum", "Growth"}), "ranking");
    private final JButton run = named(new JButton("Run"), "run");
    private final JButton importButton = named(new JButton("Import CSV…"), "import");
    private final JLabel status = named(new JLabel("Import a CSV to begin. Data lasts for this session."), "status");
    private final JTextArea details = named(new JTextArea(5, 60), "details");
    private final JTextArea reportText = named(new JTextArea(10,60),"report");
    private final JTabbedPane narrativeTabs = new JTabbedPane();
    private final JTable table = named(new JTable(), "results");
    private final HistoryChart chart = new HistoryChart();
    private boolean busy;
    private boolean imported;

    public TrendGUI(FashionTrendAgent agent) {
        this.agent = agent;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        var header = new JPanel(new BorderLayout(8, 8));
        var title = new JLabel("Fashion Trend Intelligence");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24));
        header.add(title, BorderLayout.NORTH);
        var imports = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        imports.add(importButton);
        imports.add(Box.createHorizontalStrut(12));
        imports.add(status);
        header.add(imports, BorderLayout.CENTER);
        header.add(new JLabel("Analyze imported evidence. Scores are heuristics, not forecasts. Sample data is synthetic."), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        var form = new JPanel(new GridBagLayout());
        String[] labels = {"Operation", "Trend / question", "Compare with", "Category", "From (YYYY-MM-DD)", "To (YYYY-MM-DD)", "Rank by"};
        JComponent[] fields = {operation, trend, second, category, from, to, ranking};
        for (int i = 0; i < fields.length; i++) {
            var constraints = new GridBagConstraints();
            constraints.gridx = 0; constraints.gridy = i;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(4, 4, 4, 8);
            var label = new JLabel(labels[i]);
            label.setLabelFor(fields[i]);
            form.add(label, constraints);
            constraints.gridx = 1; constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            form.add(fields[i], constraints);
        }
        var constraints = new GridBagConstraints();
        constraints.gridx = 1; constraints.gridy = fields.length;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 4, 4, 8);
        form.add(run, constraints);
        var inputs = new JPanel(new BorderLayout());
        inputs.add(form, BorderLayout.NORTH);
        inputs.add(new JLabel("Blank category/date fields mean no filter."), BorderLayout.SOUTH);

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Double.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value == null ? "" : String.format(java.util.Locale.ROOT, "%.2f", value));
            }
        });
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        var outputs = new JPanel(new BorderLayout(8, 8));
        var tableScroll = new JScrollPane(table);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        outputs.add(tableScroll, BorderLayout.CENTER);
        outputs.add(chart, BorderLayout.SOUTH);
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputs, outputs);
        split.setResizeWeight(0);
        split.setDividerLocation(345);
        add(split, BorderLayout.CENTER);
        reportText.setEditable(false);
        reportText.setLineWrap(true);
        reportText.setWrapStyleWord(true);
        narrativeTabs.addTab("Details",new JScrollPane(details));
        narrativeTabs.addTab("Report / AI",new JScrollPane(reportText));
        add(narrativeTabs, BorderLayout.SOUTH);
        importButton.addActionListener(event -> chooseCsv());
        run.addActionListener(event -> submitForm());
        operation.addActionListener(event -> updateControls());
        trend.addActionListener(event -> { if (run.isEnabled()) submitForm(); });
        updateControls();
    }

    private static <T extends JComponent> T named(T component, String name) {
        component.setName(name);
        component.getAccessibleContext().setAccessibleName(name);
        return component;
    }

    private void chooseCsv() {
        var chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV observations", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            importCsv(chooser.getSelectedFile().toPath());
    }

    public CompletableFuture<AgentResponse> importCsv(Path path) {
        return submit(AgentRequest.importCsv(path));
    }

    private void submitForm() {
        try {
            var op = AgentRequest.Operation.valueOf(((String) operation.getSelectedItem()).toUpperCase(java.util.Locale.ROOT));
            // Only enabled fields affect the request: switching operations cannot apply hidden filters.
            var request = new AgentRequest(op, null, trend.isEnabled() ? trend.getText().strip() : null,
                    second.isEnabled() ? second.getText().strip() : null,
                    category.isEnabled() ? optional(category) : null,
                    from.isEnabled() ? date(from) : null, to.isEnabled() ? date(to) : null,
                    ranking.getSelectedIndex() == 1 ? AgentRequest.Ranking.GROWTH : AgentRequest.Ranking.MOMENTUM);
            submit(request);
        } catch (IllegalArgumentException | java.time.DateTimeException error) {
            display(AgentResponse.error(error.getMessage()));
            status.setText("Check your input and try again.");
        }
    }

    /** Starts serialized background work. Completion includes rendering on the EDT. */
    public CompletableFuture<AgentResponse> submit(AgentRequest request) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Submit on the Swing event thread");
        if (busy) return CompletableFuture.failedFuture(new IllegalStateException("An operation is already running"));
        Command command = switch (request.operation()) {
            case IMPORT -> new ImportCommand(agent, request);
            case SEARCH, HISTORY, ANALYZE, ASK -> new AnalyzeCommand(agent, request);
            case DISCOVER, EMERGING -> new DiscoverCommand(agent, request);
            case COMPARE -> new CompareCommand(agent, request);
            case REPORT -> new ReportCommand(agent,request);
        };
        var completion = new CompletableFuture<AgentResponse>();
        busy = true;
        updateControls();
        status.setText("Working…");
        new SwingWorker<AgentResponse, Void>() {
            @Override protected AgentResponse doInBackground() { return controller.execute(command); }
            @Override protected void done() {
                AgentResponse response;
                try { response = get(); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    response = AgentResponse.error("Operation interrupted. Please try again.");
                } catch (java.util.concurrent.ExecutionException e) {
                    response = AgentResponse.error("Unexpected operation failure: " + e.getCause().getClass().getSimpleName());
                }
                if (request.operation() == IMPORT && response.success()) imported = true;
                display(response);
                status.setText(response.success() ? (request.operation() == IMPORT ? response.message() : "Ready.")
                        : "Operation failed. See details below.");
                busy = false;
                updateControls();
                completion.complete(response);
            }
        }.execute();
        return completion;
    }

    private void updateControls() {
        operation.setEnabled(!busy);
        importButton.setEnabled(!busy);
        run.setEnabled(!busy && imported);
        String op = (String) operation.getSelectedItem();
        boolean discover = op.equals("Discover") || op.equals("Emerging");
        trend.setEnabled(!busy && !discover);
        second.setEnabled(!busy && op.equals("Compare"));
        category.setEnabled(!busy && !op.equals("Search") && !op.equals("Ask"));
        from.setEnabled(!busy && !op.equals("Search") && !op.equals("Ask"));
        to.setEnabled(!busy && !op.equals("Search") && !op.equals("Ask"));
        ranking.setEnabled(!busy && discover);
    }

    public void display(AgentResponse response) {
        chart.setSignals(List.of());
        reportText.setText(response.report() == null ? "" : response.report().render());
        reportText.setCaretPosition(0);
        narrativeTabs.setSelectedIndex(response.report() == null ? 0 : 1);
        var messages = new ArrayList<String>();
        if (!response.message().isEmpty()) messages.add((response.success() ? "" : "Error: ") + response.message());
        response.warnings().forEach(w -> messages.add("Warning: " + w));
        if (!response.signals().isEmpty()) {
            setTable(new String[]{"Date", "Trend", "Category", "Popularity", "Source"}, response.signals().stream()
                    .map(s -> new Object[]{s.date().toString(), s.trend(), s.category(), s.popularity(), s.source()}).toList());
            chart.setSignals(response.signals());
        } else if (!response.analyses().isEmpty()) {
            messages.add("Blank metrics indicate insufficient history. Growth rates are percent change per 30 days.");
            setTable(new String[]{"Trend", "Category", "Lifecycle", "Popularity", "Dates", "Momentum", "Short %/30d", "Long %/30d", "Acceleration"},
                    response.analyses().stream().map(a -> new Object[]{a.trend(), a.category(), a.lifecycle(), a.latestPopularity(),
                            a.observations(), a.momentum(), a.shortGrowth(), a.longGrowth(), a.acceleration()}).toList());
            response.analyses().forEach(a -> messages.add(a.trend() + ": " + a.limitation()));
        } else setTable(new String[]{"Matching trend"}, response.matches().stream().map(n -> new Object[]{n}).toList());
        details.setText(String.join("\n", messages));
        details.setCaretPosition(0);
    }

    private void setTable(String[] columns, List<Object[]> rows) {
        table.setModel(new DefaultTableModel(rows.toArray(Object[][]::new), columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) return value.getClass();
                }
                return Object.class;
            }
        });
    }

    private static String optional(JTextField field) { return field.getText().isBlank() ? null : field.getText().strip(); }
    private static LocalDate date(JTextField field) {
        String value = optional(field);
        return value == null ? null : LocalDate.parse(value);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var agent = new FashionTrendAgent(new TrendRepository(new CsvTrendAdapter(new CsvReader())));
            var view = new TrendGUI(agent);
            var frame = new JFrame("Fashion Trend Intelligence — EECS 3311");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(view);
            frame.setSize(1220, 760);
            frame.setMinimumSize(new Dimension(980, 640));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            if (args.length == 1) view.importCsv(Path.of(args[0]));
        });
    }
}
