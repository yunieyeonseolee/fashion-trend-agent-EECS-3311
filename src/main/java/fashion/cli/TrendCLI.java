package fashion.cli;

import fashion.application.*;
import fashion.command.*;
import fashion.data.*;
import fashion.model.TrendAnalysis;
import java.nio.file.Path;
import java.time.LocalDate;
import static fashion.application.AgentRequest.Operation.*;

/** CLI boundary: parses input, submits shared commands and renders shared output. */
public final class TrendCLI {
    private static final String USAGE = """
            Usage:
              ./scripts/run.sh import <csv>
              ./scripts/run.sh search <csv> <name-fragment>
              ./scripts/run.sh history <csv> <exact-name> [category|-] [from|-] [to|-]
              ./scripts/run.sh analyze <csv> <exact-name> [category|-] [from|-] [to|-]
              ./scripts/run.sh discover <csv> <category> [momentum|growth]
              ./scripts/run.sh emerging <csv> <category> [momentum|growth]
              ./scripts/run.sh compare <csv> <first-name> <second-name> [from|-] [to|-]
            Dates use YYYY-MM-DD. Quote names containing spaces. '-' means no filter.
            Data is loaded from the supplied CSV for each invocation.
            """;

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--help")) {
            System.out.print(USAGE);
            return;
        }
        try {
            var agent = new FashionTrendAgent(new TrendRepository(new CsvTrendAdapter(new CsvReader())));
            var controller = new AgentController();
            // Parse and validate everything before importing or executing an operation.
            Command command = parseCommand(args, agent);
            if (!args[0].equals("import")) {
                var imported = controller.execute(new ImportCommand(agent, AgentRequest.importCsv(Path.of(args[1]))));
                if (!imported.success()) {
                    display(imported);
                    System.exit(1);
                    return;
                }
                imported.warnings().forEach(w -> System.err.println("Warning: " + w));
            }
            var response = controller.execute(command);
            display(response);
            if (!response.success()) System.exit(1);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public static Command parseCommand(String[] args, FashionTrendAgent agent) {
        if (args.length < 2) throw new IllegalArgumentException(USAGE);
        boolean valid = switch (args[0]) {
            case "import" -> args.length == 2;
            case "search" -> args.length == 3;
            case "history", "analyze" -> args.length >= 3 && args.length <= 6;
            case "discover", "emerging" -> args.length == 3 || args.length == 4;
            case "compare" -> args.length >= 4 && args.length <= 6;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException(USAGE);
        return switch (args[0]) {
            case "import" -> new ImportCommand(agent, AgentRequest.importCsv(Path.of(args[1])));
            case "search" -> new AnalyzeCommand(agent, AgentRequest.trend(SEARCH, args[2], null, null, null));
            case "history", "analyze" -> new AnalyzeCommand(agent, AgentRequest.trend(
                    args[0].equals("history") ? HISTORY : ANALYZE, args[2], optional(args, 3), date(args, 4), date(args, 5)));
            case "discover", "emerging" -> {
                String ranking = args.length == 4 ? args[3] : "momentum";
                var strategy = switch (ranking) {
                    case "momentum" -> AgentRequest.Ranking.MOMENTUM;
                    case "growth" -> AgentRequest.Ranking.GROWTH;
                    default -> throw new IllegalArgumentException("Ranking must be momentum or growth");
                };
                yield new DiscoverCommand(agent, AgentRequest.discover(args[2], args[0].equals("emerging"), strategy));
            }
            case "compare" -> new CompareCommand(agent, AgentRequest.compare(args[2], args[3], date(args, 4), date(args, 5)));
            default -> throw new IllegalArgumentException(USAGE);
        };
    }

    public static void display(AgentResponse response) {
        if (!response.success()) {
            System.err.println("Error: " + response.message());
            return;
        }
        if (!response.message().isEmpty()) System.out.println(response.message());
        response.warnings().forEach(w -> System.err.println("Warning: " + w));
        response.matches().forEach(System.out::println);
        if (!response.signals().isEmpty()) {
            System.out.println("date\ttrend\tcategory\tpopularity\tsource");
            response.signals().forEach(s -> System.out.printf(java.util.Locale.ROOT, "%s\t%s\t%s\t%.2f\t%s%n",
                    s.date(), s.trend(), s.category(), s.popularity(), s.source()));
        }
        response.analyses().forEach(TrendCLI::showAnalysis);
    }

    private static void showAnalysis(TrendAnalysis a) {
        System.out.printf(java.util.Locale.ROOT, "%s (%s): %s; latest popularity %.2f; %d dates%n",
                a.trend(), a.category(), a.lifecycle(), a.latestPopularity(), a.observations());
        if (a.reliable()) System.out.printf(java.util.Locale.ROOT,
                "  momentum %.2f; short growth %.2f%%/30d; long growth %.2f%%/30d; acceleration %.2f%n",
                a.momentum(), a.shortGrowth(), a.longGrowth(), a.acceleration());
        System.out.println("  " + a.limitation());
    }

    private static String optional(String[] args, int index) {
        return args.length <= index || args[index].equals("-") ? null : args[index];
    }

    private static LocalDate date(String[] args, int index) {
        String value = optional(args, index);
        return value == null ? null : LocalDate.parse(value);
    }
}
