package fashion;

import fashion.application.*;
import fashion.command.*;
import fashion.cli.TrendCLI;
import fashion.data.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import static fashion.application.AgentRequest.Operation.*;

/** Exercises the same controller/command/facade path used by interface clients. */
public final class ApplicationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var agent = new FashionTrendAgent(new TrendRepository(new CsvTrendAdapter(new CsvReader())));
        var controller = new AgentController();
        var csv = Path.of("data/sample-trends.csv");
        check(controller.execute(new ImportCommand(agent, AgentRequest.importCsv(csv))).success(), "import succeeds");
        var search = controller.execute(new AnalyzeCommand(agent, AgentRequest.trend(SEARCH,"MARY",null,null,null)));
        check(search.matches().equals(List.of("mary jane flats")), "search shares imported state");
        var historyRequest = AgentRequest.trend(HISTORY, "mary jane flats", "footwear",
                LocalDate.of(2026,9,1), LocalDate.of(2026,10,1));
        var history = controller.execute(new AnalyzeCommand(agent, historyRequest));
        check(history.signals().size() == 2, "history filters preserved");
        var cli = controller.execute(TrendCLI.parseCommand(new String[]{"history",csv.toString(),"mary jane flats",
                "footwear","2026-09-01","2026-10-01"}, agent));
        check(cli.equals(history), "CLI and direct interface requests return identical responses");
        var analyze = controller.execute(new AnalyzeCommand(agent,AgentRequest.trend(ANALYZE,"mary jane flats",null,null,null)));
        check(analyze.analyses().get(0).lifecycle().equals("Growing"), "analysis workflow");
        var discover = controller.execute(new DiscoverCommand(agent,AgentRequest.discover("footwear",false,AgentRequest.Ranking.GROWTH)));
        check(discover.analyses().size()==2 && discover.analyses().get(0).trend().equals("mary jane flats"), "discovery workflow");
        var emerging = controller.execute(new DiscoverCommand(agent,AgentRequest.discover("accessories",true,null)));
        check(emerging.analyses().get(0).trend().equals("bag charms"), "microtrend workflow");
        var compare = controller.execute(new CompareCommand(agent,AgentRequest.compare("mary jane flats","chunky sneakers",null,null)));
        check(compare.analyses().size()==2 && compare.warnings().isEmpty(), "complete comparison");
        var partial = controller.execute(new CompareCommand(agent,AgentRequest.compare("mary jane flats","unknown",null,null)));
        check(partial.success() && partial.analyses().size()==1 && partial.warnings().size()==1, "partial comparison preserves data");
        check(!controller.execute(new CompareCommand(agent,AgentRequest.compare("missing","unknown",null,null))).success(), "both missing fail");
        check(!controller.execute(new AnalyzeCommand(agent,AgentRequest.trend(ANALYZE,"unknown",null,null,null))).success(), "missing analysis is shared error");
        check(!controller.execute(new DiscoverCommand(agent,AgentRequest.discover("unknown",false,null))).success(), "unknown category is shared error");
        Path missing = Files.createTempFile("fashion-missing-", ".csv");
        Files.delete(missing);
        check(!controller.execute(new ImportCommand(agent,AgentRequest.importCsv(missing))).success(), "IO failure converted to response");
        check(controller.execute(new AnalyzeCommand(agent,historyRequest)).equals(history), "failed import preserves session");
        var sparse = controller.execute(new CompareCommand(agent,AgentRequest.compare("mary jane flats","chunky sneakers",
                LocalDate.of(2026,10,1),LocalDate.of(2026,11,1))));
        check(sparse.success() && sparse.analyses().size()==2 && sparse.warnings().size()==2, "sparse comparison signals unavailable metrics");
        var repeated = controller.execute(new ImportCommand(agent,AgentRequest.importCsv(csv)));
        check(repeated.message().startsWith("Imported 0") && repeated.warnings().size()==12, "reimport reports duplicates");
        try {
            new ImportCommand(agent,historyRequest);
            throw new AssertionError("wrong command type accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        try {
            AgentRequest.trend(SEARCH," ",null,null,null);
            throw new AssertionError("blank search accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        try {
            history.signals().clear();
            throw new AssertionError("response mutable");
        } catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("Passed " + checks + " application workflow checks.");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
