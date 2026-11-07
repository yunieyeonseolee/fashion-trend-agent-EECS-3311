package fashion;

import fashion.gui.FashionStudio;
import fashion.application.*;
import fashion.data.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static fashion.application.AgentRequest.Operation.*;

/** End-to-end checks for the alternate view, using deterministic local model replies. */
public final class StudioTest {
    private static FashionStudio view;
    private static int checks;
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->view=new FashionStudio(new FashionTrendAgent(
            new TrendRepository(new CsvTrendAdapter(new CsvReader())),(instructions,input,schema)->
            new fashion.llm.LLMClient.Reply(schema==null?"TEST interpretation":"{\"steps\":[{\"operation\":\"ANALYZE\",\"name\":\"mary jane flats\",\"secondName\":null,\"category\":null,\"from\":null,\"to\":null,\"ranking\":\"MOMENTUM\"}],\"clarification\":\"\"}","test",10,5))));
        edt(()->check(!button("SEARCH").isEnabled(),"requires import"));
        check(submit(AgentRequest.importCsv(Path.of("data/sample-trends.csv"))).success(),"imports data");
        edt(()->{field("trend").setText("mary");button("SEARCH").doClick();});ready();
        edt(()->check(table().getRowCount()==1,"search button uses shared workflow"));
        edt(()->{field("trend").setText("mary jane flats");button("ANALYZE").doClick();});ready();
        edt(()->{check(table().getValueAt(0,2).equals("Growing"),"analysis table");snapshot("studio-preview.png",1320,900);snapshot("studio-small-preview.png",1180,780);});
        edt(()->{view.showPage("Discover");field("category").setText("footwear");button("DISCOVER").doClick();});ready();
        edt(()->{check(table().getRowCount()==2,"discovery screen");table().getRowSorter().toggleSortOrder(0);table().setRowSelectionInterval(0,0);
            check(hasLabel(view,"chunky sneakers / footwear"),"summary follows sorted selection");});
        edt(()->{view.showPage("Compare");field("trend").setText("mary jane flats");field("second").setText("chunky sneakers");button("COMPARE").doClick();});ready();
        edt(()->check(table().getRowCount()==2,"comparison screen"));
        edt(()->{view.showPage("Explore");field("from").setText("bad date");button("HISTORY").doClick();
            check(table().getRowCount()==0,"bad input clears stale output");
            check(hasLabel(view,"Let’s try that again."),"error feedback");field("from").setText("");button("HISTORY").doClick();});ready();
        edt(()->check(table().getRowCount()==4,"history screen"));
        edt(()->{view.showPage("AI reports");button("REPORT").doClick();});ready();
        edt(()->check(hasText(view,"TEST interpretation"),"separate AI narrative"));
        var directory=Files.createTempDirectory("studio-report-");var path=directory.resolve("report.txt");
        var export=new AtomicReference<CompletableFuture<Path>>();edt(()->export.set(view.exportReport(path)));export.get().get(10,TimeUnit.SECONDS);
        check(Files.readString(path).contains("Calculated evidence"),"export retains evidence");
        edt(()->export.set(view.exportReport(path)));
        try{export.get().get(10,TimeUnit.SECONDS);throw new AssertionError("overwrite accepted");}catch(ExecutionException expected){checks++;}
        edt(()->{field("trend").setText("How are mary jane flats doing?");button("ASK").doClick();});ready();
        edt(()->check(hasText(view,"TEST interpretation"),"Ask screen"));
        edt(()->view.showPage("Explore"));edt(()->export.set(view.exportReport(directory.resolve("stale.txt"))));
        check(export.get().isCompletedExceptionally(),"navigation prevents stale export");
        Files.delete(path);Files.delete(directory);
        System.out.println("Passed "+checks+" Atelier UI checks; preview: build/studio-preview.png");
    }
    private static AgentResponse submit(AgentRequest r)throws Exception{var f=new AtomicReference<CompletableFuture<AgentResponse>>();edt(()->f.set(view.submit(r)));return f.get().get(10,TimeUnit.SECONDS);}
    private static void edt(Runnable r)throws Exception{SwingUtilities.invokeAndWait(r);}
    private static void ready()throws Exception{for(int i=0;i<200;i++){var ready=new java.util.concurrent.atomic.AtomicBoolean();edt(()->ready.set(button("import").isEnabled()));if(ready.get())return;Thread.sleep(25);}throw new AssertionError("worker timeout");}
    private static Component find(Container root,String name){for(var c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container nested){var result=find(nested,name);if(result!=null)return result;}}return null;}
    private static JTextField field(String name){return (JTextField)find(view,name);}
    private static JButton button(String name){return (JButton)find(view,name);}
    private static JTable table(){return (JTable)find(view,"results");}
    private static boolean hasLabel(Container root,String text){for(var c:root.getComponents()){if(c instanceof JLabel l&&l.getText().equals(text))return true;if(c instanceof Container n&&hasLabel(n,text))return true;}return false;}
    private static boolean hasText(Container root,String text){for(var c:root.getComponents()){if(c instanceof JTextArea t&&t.getText().equals(text))return true;if(c instanceof Container n&&hasText(n,text))return true;}return false;}
    private static void layout(Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof Container n)layout(n);}
    private static void snapshot(String file,int width,int height){view.setSize(width,height);layout(view);var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();view.printAll(g);g.dispose();try{javax.imageio.ImageIO.write(image,"png",Path.of("build",file).toFile());}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
