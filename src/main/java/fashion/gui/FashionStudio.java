package fashion.gui;

import fashion.application.*;
import fashion.command.*;
import fashion.data.*;
import fashion.export.ReportExporter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import static fashion.application.AgentRequest.Operation.*;

/** Editorial desktop view; shares the controller, commands and agent with the original GUI. */
public final class FashionStudio extends JPanel {
    private static final Color PAPER=new Color(249,247,243), INK=new Color(48,43,49),
            MUTED=new Color(115,106,117), LILAC=new Color(231,223,239), LINE=new Color(222,215,220);
    private final FashionTrendAgent agent;
    private final AgentController controller=new AgentController();
    private final JLabel title=label("Explore the next chapter.",36,true);
    private final JLabel subtitle=label("Find a trend, understand its movement, and follow the evidence.",14,false);
    private final JLabel status=label("Start with a CSV of trend observations.",13,false);
    private final JLabel summary=label("Your research starts here",24,true);
    private final JTextField trend=field("trend"), second=field("second"), category=field("category"), from=field("from"), to=field("to");
    private final JComboBox<String> ranking=new JComboBox<>(new String[]{"Momentum", "Growth"});
    private final JPanel inputs=new JPanel(new GridLayout(0,4,16,12));
    private final JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
    private final JPanel metrics=new JPanel(new GridLayout(1,4,16,0));
    private final JTable table=new JTable();
    private final HistoryChart chart=new HistoryChart();
    private final JTextArea narrative=textArea(), evidence=textArea(), notes=textArea();
    private final JTabbedPane tabs=new JTabbedPane();
    private final JButton save=button("Save report",false);
    private final JProgressBar progress=new JProgressBar();
    private final List<JButton> guarded=new ArrayList<>(), navigation=new ArrayList<>();
    private AgentResponse response;
    private boolean busy, imported;
    private String page="Explore";

    public FashionStudio(FashionTrendAgent agent) {
        this.agent=agent;
        setLayout(new BorderLayout(28,0)); setBackground(PAPER);
        setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        var sidebar=new JPanel(new BorderLayout(0,32)); sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(185,500));
        var brand=new JPanel(new GridLayout(0,1,0,8)); brand.setOpaque(false);
        brand.add(label("ATELIER",27,true)); brand.add(label("FASHION INTELLIGENCE",10,false));
        sidebar.add(brand,BorderLayout.NORTH);
        var menu=new JPanel(new GridLayout(0,1,0,10)); menu.setOpaque(false);
        String[] names={"Explore", "Discover", "Compare", "AI reports"};
        for(int i=0;i<names.length;i++) {
            String name=names[i]; var item=button("0"+(i+1)+"   "+name,false);
            item.setHorizontalAlignment(SwingConstants.LEFT); item.addActionListener(e->showPage(name));
            navigation.add(item); menu.add(item);
        }
        var menuWrap=new JPanel(new BorderLayout()); menuWrap.setOpaque(false); menuWrap.add(menu,BorderLayout.NORTH);
        sidebar.add(menuWrap,BorderLayout.CENTER);
        var library=new JPanel(new GridLayout(0,1,0,12)); library.setOpaque(false);
        library.add(label("YOUR DATA LIBRARY",11,false));
        var upload=button("Import CSV",true); upload.setName("import"); guarded.add(upload);
        upload.addActionListener(e->{var chooser=new JFileChooser(); if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) submit(AgentRequest.importCsv(chooser.getSelectedFile().toPath()));});
        library.add(upload); library.add(label("Data stays in this session.",11,false));
        sidebar.add(library,BorderLayout.SOUTH); add(sidebar,BorderLayout.WEST);

        var main=new JPanel(new BorderLayout(0,10)); main.setOpaque(false);
        var heading=new JPanel(); heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS)); heading.setOpaque(false);
        heading.add(label("THE TREND EDIT   /   RESEARCH STUDIO",11,false)); heading.add(Box.createVerticalStrut(16)); heading.add(title); heading.add(Box.createVerticalStrut(10)); heading.add(subtitle);
        main.add(heading,BorderLayout.NORTH);
        var workspace=new JPanel(new BorderLayout(0,12)); workspace.setOpaque(false);
        var query=new JPanel(new BorderLayout(0,16)); query.setBackground(Color.WHITE);
        query.setBorder(BorderFactory.createEmptyBorder(14,18,14,18)); inputs.setOpaque(false); actions.setOpaque(false);
        query.add(inputs,BorderLayout.CENTER); query.add(actions,BorderLayout.SOUTH); workspace.add(query,BorderLayout.NORTH);
        var result=new JPanel(new BorderLayout(0,10)); result.setOpaque(false);
        var overview=new JPanel(new BorderLayout(0,12)); overview.setOpaque(false); metrics.setOpaque(false);
        overview.add(summary,BorderLayout.NORTH); overview.add(metrics,BorderLayout.CENTER); result.add(overview,BorderLayout.NORTH);
        table.setName("results"); table.setRowHeight(38); table.setShowGrid(false); table.setFillsViewportHeight(true);
        table.setSelectionBackground(LILAC); table.setSelectionForeground(INK); table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(100,32));
        table.setDefaultRenderer(Double.class,new javax.swing.table.DefaultTableCellRenderer(){
            @Override protected void setValue(Object value){setHorizontalAlignment(SwingConstants.RIGHT);setText(value==null?"":String.format(java.util.Locale.ROOT,"%.2f",value));}
        });
        table.getSelectionModel().addListSelectionListener(e->updateSummary());
        var resultPanel=new JPanel(new BorderLayout(0,8)); resultPanel.setBackground(Color.WHITE);
        var selectionActions=new JPanel(new FlowLayout(FlowLayout.LEFT)); selectionActions.setBackground(Color.WHITE);
        for(String action:List.of("Analyze selected", "History selected")) {
            var b=button(action,false); guarded.add(b); selectionActions.add(b);
            b.addActionListener(e->{int row=table.getSelectedRow(); if(row<0){status.setText("Select a result row first.");return;}
                int column=table.getColumnName(0).equals("Date")?1:0;
                String name=String.valueOf(table.getValueAt(row,column));
                String cat=table.getColumnCount()>column+1?String.valueOf(table.getValueAt(row,column+1)):null;
                submit(AgentRequest.trend(action.startsWith("Analyze")?ANALYZE:HISTORY,name,cat,null,null));});
        }
        resultPanel.add(selectionActions,BorderLayout.NORTH);
        var scroll=new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); resultPanel.add(scroll);
        tabs.addTab("Trend results",resultPanel); tabs.addTab("History",chart);
        tabs.addTab("AI interpretation",new JScrollPane(narrative)); tabs.addTab("Evidence & sources",new JScrollPane(evidence));
        result.add(tabs); workspace.add(result); main.add(workspace);
        var footer=new JPanel(new BorderLayout(0,8)); footer.setOpaque(false);
        progress.setPreferredSize(new Dimension(100,3)); progress.setForeground(MUTED); progress.setBorderPainted(false);
        footer.add(progress,BorderLayout.NORTH);
        var statusLine=new JPanel(new BorderLayout(12,0)); statusLine.setOpaque(false); statusLine.add(status); statusLine.add(save,BorderLayout.EAST);
        save.addActionListener(e->{var chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("trend-report.txt"));
            if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) exportReport(chooser.getSelectedFile().toPath());});
        footer.add(statusLine,BorderLayout.CENTER);
        notes.setRows(1); notes.setText("Import your observations to begin. The included sample is synthetic; scores are heuristics, not forecasts.");
        footer.add(new JScrollPane(notes),BorderLayout.SOUTH); main.add(footer,BorderLayout.SOUTH); add(main);
        narrative.setText("Ask a question or create a report. AI interpretation will appear here, separate from calculated evidence.");
        evidence.setText("Reports retain their calculations, sources and usage details here.");
        showPage("Explore");
    }

    public void showPage(String name) {
        if(busy) return;
        page=name; inputs.removeAll(); actions.removeAll();
        switch(name) {
            case "Explore" -> {title.setText("Explore the next chapter."); subtitle.setText("Search by name. Category and dates apply to analysis and history.");
                input("Trend name or search",trend); filters(); action("Find trends",SEARCH); action("Analyze trend",ANALYZE); action("View history",HISTORY);}
            case "Discover" -> {title.setText("What’s gathering momentum?"); subtitle.setText("Explore a category or look for emerging trends with smaller audiences.");
                input("Category · required",category); input("Rank results by",ranking); dates(); action("Discover trends",DISCOVER); action("Find emerging",EMERGING);}
            case "Compare" -> {title.setText("Two trends. A clearer view."); subtitle.setText("Compare their popularity, growth and lifecycle using the same date range.");
                input("First trend · exact name",trend); input("Second trend · exact name",second); dates(); action("Compare trends",COMPARE);}
            case "AI reports" -> {title.setText("From signals to perspective."); subtitle.setText("Ask a question, or report on an exact trend. Filters apply to reports only.");
                input("Question or exact trend name",trend); filters(); action("Ask a question",ASK); action("Create trend report",REPORT);}
            default -> throw new IllegalArgumentException("Unknown page");
        }
        // Changing sections clears displayed results so old content cannot look like a new answer.
        render(new AgentResponse(true,"Choose an action above to begin.",List.of(),List.of(),List.of(),List.of()));
        for(var item:navigation) item.setBackground(item.getText().endsWith(name)?LILAC:PAPER);
        updateControls(); revalidate(); repaint();
    }
    private void filters(){input("Category · optional",category);dates();}
    private void dates(){input("From · YYYY-MM-DD",from);input("To · YYYY-MM-DD",to);}
    private void input(String caption,JComponent control){var p=new JPanel(new BorderLayout(0,8));p.setOpaque(false);var l=label(caption,12,false);l.setLabelFor(control);p.add(l,BorderLayout.NORTH);p.add(control);inputs.add(p);}
    private void action(String caption,AgentRequest.Operation op){var b=button(caption,actions.getComponentCount()==0);b.setName(op.name());actions.add(b);b.addActionListener(e->{try{
        boolean search=op==SEARCH||op==ASK;
        submit(new AgentRequest(op,null,trend.getText().strip(),second.getText().strip(),search||op==COMPARE?null:optional(category),
                search?null:date(from),search?null:date(to),ranking.getSelectedIndex()==0?AgentRequest.Ranking.MOMENTUM:AgentRequest.Ranking.GROWTH));
    }catch(IllegalArgumentException|java.time.DateTimeException ex){render(AgentResponse.error(ex.getMessage()));}});}

    public CompletableFuture<AgentResponse> submit(AgentRequest request){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Use the event thread");
        if(busy)return CompletableFuture.failedFuture(new IllegalStateException("Work already running"));
        Command command=switch(request.operation()){
            case IMPORT->new ImportCommand(agent,request);case DISCOVER,EMERGING->new DiscoverCommand(agent,request);
            case COMPARE->new CompareCommand(agent,request);case REPORT->new ReportCommand(agent,request);
            default->new AnalyzeCommand(agent,request);};
        busy=true;updateControls();status.setText("Working on your request…");
        var future=new CompletableFuture<AgentResponse>();
        new SwingWorker<AgentResponse,Void>(){
            protected AgentResponse doInBackground(){return controller.execute(command);}
            protected void done(){AgentResponse result;try{result=get();}catch(Exception ex){result=AgentResponse.error("Request failed. Please try again.");}
                if(request.operation()==IMPORT&&result.success())imported=true;
                busy=false;render(result);updateControls();future.complete(result);}
        }.execute();return future;
    }
    public CompletableFuture<Path> exportReport(Path path){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Use the event thread");
        if(busy||response==null||response.report()==null)return CompletableFuture.failedFuture(new IllegalStateException("No report available"));
        var snapshot=response;var future=new CompletableFuture<Path>();busy=true;updateControls();
        new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{new ReportExporter().write(snapshot,path);return null;}
            protected void done(){Exception failure=null;try{get();status.setText("Saved to "+path.getFileName());}catch(Exception ex){failure=ex;status.setText("Could not save. Choose a new filename in a writable folder.");}
                busy=false;updateControls();if(failure==null)future.complete(path);else future.completeExceptionally(failure);}
        }.execute();return future;
    }
    private void updateControls(){for(var b:guarded)b.setEnabled(!busy);for(var b:navigation)b.setEnabled(!busy);
        for(var b:actions.getComponents())b.setEnabled(!busy&&imported);
        for(var f:List.of(trend,second,category,from,to))f.setEnabled(!busy);ranking.setEnabled(!busy);
        progress.setIndeterminate(busy);save.setEnabled(!busy&&response!=null&&response.report()!=null);}
    private void render(AgentResponse result){
        response=result;chart.setSignals(result.signals());
        String[] columns;List<Object[]> rows;
        if(!result.analyses().isEmpty()){columns=new String[]{"Trend","Category","Lifecycle","Popularity","Momentum","Short %/30d","Long %/30d","Acceleration"};
            rows=result.analyses().stream().map(a->new Object[]{a.trend(),a.category(),a.lifecycle(),a.latestPopularity(),a.momentum(),a.shortGrowth(),a.longGrowth(),a.acceleration()}).toList();}
        else if(!result.signals().isEmpty()){columns=new String[]{"Date","Trend","Category","Popularity","Source"};rows=result.signals().stream().map(s->new Object[]{s.date().toString(),s.trend(),s.category(),s.popularity(),s.source()}).toList();}
        else{columns=new String[]{"Matching trend"};rows=result.matches().stream().map(n->new Object[]{n}).toList();}
        table.setModel(new DefaultTableModel(rows.toArray(Object[][]::new),columns){public boolean isCellEditable(int r,int c){return false;}
            public Class<?> getColumnClass(int c){for(int r=0;r<getRowCount();r++)if(getValueAt(r,c)!=null)return getValueAt(r,c).getClass();return Object.class;}});
        for(int i=0;i<columns.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?210:145);
        narrative.setText(result.report()==null?"Create a report or ask a question to see an AI interpretation.":result.report().narrative().isBlank()?"AI interpretation unavailable. Calculated evidence is retained in Evidence & sources.":result.report().narrative());
        evidence.setText(result.report()==null?"Report evidence will appear here.":evidenceText(result));narrative.setCaretPosition(0);evidence.setCaretPosition(0);
        var messages=new ArrayList<>(result.warnings());for(var a:result.analyses())messages.add(a.trend()+": "+a.limitation());
        notes.setText(messages.isEmpty()?"Scores are heuristics, not forecasts. Synthetic sample observations are not real-world fashion evidence.":String.join("\n",messages));notes.setCaretPosition(0);
        status.setText(!result.success()?"Check your input: "+result.message():!result.message().isBlank()?result.message():rows.size()+(rows.size()==1?" result":" results")+" · Select a row to explore further.");
        tabs.setSelectedIndex(result.report()!=null?2:!result.signals().isEmpty()?1:0);updateSummary();updateControls();
    }
    private static String evidenceText(AgentResponse result){
        String full=result.report().render();
        int interpretation=full.indexOf("\nAI interpretation (review against the evidence):");
        int usage=full.indexOf("API usage — model:",interpretation);
        return interpretation<0?full:full.substring(0,interpretation)+(usage<0?"":"\n"+full.substring(usage));
    }
    private void updateSummary(){metrics.removeAll();summary.setText(response==null?"Your research starts here":!response.success()?"Let’s try that again.":table.getRowCount()==0?"Ready for your next discovery":table.getRowCount()+" results in your edit");
        if(response!=null&&!response.analyses().isEmpty()){
            int row=table.getSelectedRow()<0?0:table.convertRowIndexToModel(table.getSelectedRow());
            if(row<response.analyses().size()){var a=response.analyses().get(row);summary.setText(a.trend()+" / "+a.category());
                metric("POPULARITY / 100",number(a.latestPopularity()));metric("SHORT GROWTH / 30D",number(a.shortGrowth())+(a.shortGrowth()==null?"":"%"));metric("MOMENTUM",number(a.momentum()));metric("LIFECYCLE",a.lifecycle());}}
        metrics.setVisible(metrics.getComponentCount()>0);metrics.revalidate();metrics.repaint();}
    private void metric(String name,String value){var panel=new JPanel(new BorderLayout(0,8));panel.setBackground(LILAC);panel.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));panel.add(label(name,10,false),BorderLayout.NORTH);panel.add(label(value,23,true));metrics.add(panel);}
    private static String number(Double n){return n==null?"Unavailable":String.format(java.util.Locale.ROOT,"%.1f",n);}
    private static String optional(JTextField f){return f.getText().isBlank()?null:f.getText().strip();}
    private static LocalDate date(JTextField f){return optional(f)==null?null:LocalDate.parse(optional(f));}
    private static JLabel label(String text,int size,boolean serif){var l=new JLabel(text);l.setFont(new Font(serif?Font.SERIF:Font.SANS_SERIF,Font.PLAIN,size));l.setForeground(serif?INK:MUTED);return l;}
    private static JTextField field(String name){var f=new JTextField();f.setName(name);f.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));f.setPreferredSize(new Dimension(160,36));f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(7,10,7,10)));return f;}
    private static JButton button(String text,boolean primary){var b=new JButton(text);b.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));b.setBackground(primary?INK:PAPER);b.setForeground(primary?Color.WHITE:INK);b.setOpaque(true);b.setContentAreaFilled(true);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(12,16,12,16)));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;}
    private static JTextArea textArea(){var t=new JTextArea();t.setEditable(false);t.setLineWrap(true);t.setWrapStyleWord(true);t.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));t.setForeground(INK);t.setMargin(new Insets(14,18,14,18));return t;}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{var view=new FashionStudio(new FashionTrendAgent(new TrendRepository(new CsvTrendAdapter(new CsvReader()))));
        var frame=new JFrame("Atelier · Fashion Intelligence");frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);frame.setContentPane(view);frame.setSize(1320,900);frame.setMinimumSize(new Dimension(1180,780));frame.setLocationRelativeTo(null);frame.setVisible(true);
        if(args.length==1)view.submit(AgentRequest.importCsv(Path.of(args[0])));});}
}
