package fashion.cli;

import fashion.data.CsvReader;
import fashion.data.CsvTrendAdapter;
import fashion.data.TrendRepository;
import fashion.analysis.*;
import fashion.model.TrendAnalysis;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/** Initial data-layer CLI. Command/controller wiring follows in the next milestone. */
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
            String operation = args[0];
            boolean valid = switch (operation) {
                case "import" -> args.length == 2;
                case "search" -> args.length == 3;
                case "history", "analyze" -> args.length >= 3 && args.length <= 6;
                case "discover", "emerging" -> args.length == 3 || args.length == 4;
                case "compare" -> args.length >= 4 && args.length <= 6;
                default -> false;
            };
            if (!valid) throw new IllegalArgumentException(USAGE);
            var repository = new TrendRepository(new CsvTrendAdapter(new CsvReader()));
            var result = repository.importCsv(Path.of(args[1]));
            result.warnings().forEach(w -> System.err.println("Warning: " + w));
            var analyzer = new TrendAnalyzer(new MomentumRankingStrategy());
            switch (operation) {
                case "import" -> System.out.printf("Imported %d observations; %d warnings.%n",
                        result.signals().size(), result.warnings().size());
                case "search" -> {
                    var names = repository.findByName(args[2]);
                    if (names.isEmpty()) System.out.println("No matching trends.");
                    names.forEach(System.out::println);
                }
                case "history" -> {
                    var history = repository.getSignals(args[2], optional(args, 3),
                            date(args, 4), date(args, 5));
                    if (history.isEmpty()) System.out.println("No observations match this trend and date range.");
                    else {
                        System.out.println("date\ttrend\tcategory\tpopularity\tsource");
                        history.forEach(s -> System.out.printf(java.util.Locale.ROOT, "%s\t%s\t%s\t%.2f\t%s%n",
                                s.date(), s.trend(), s.category(), s.popularity(), s.source()));
                    }
                }
                case "analyze" -> showAnalysis(analyzer.calculateMomentum(repository.getSignals(
                        args[2], optional(args, 3), date(args, 4), date(args, 5))));
                case "discover", "emerging" -> {
                    String ranking = args.length == 4 ? args[3] : "momentum";
                    TrendRankingStrategy strategy = switch (ranking) {
                        case "momentum" -> new MomentumRankingStrategy();
                        case "growth" -> new GrowthRankingStrategy();
                        default -> throw new IllegalArgumentException("Ranking must be momentum or growth");
                    };
                    analyzer = new TrendAnalyzer(strategy);
                    var names = repository.findByCategory(args[2]);
                    if (names.isEmpty()) throw new IllegalArgumentException("No data for category. Available: "
                            + String.join(", ", repository.categories()));
                    var analyses = new ArrayList<TrendAnalysis>();
                    for (var name : names) {
                        var analysis = analyzer.calculateMomentum(repository.getSignals(name, args[2], null, null));
                        if (analysis.reliable()) analyses.add(analysis);
                        else System.err.println("Warning: excluded " + name + ": " + analysis.limitation());
                    }
                    var ranked = operation.equals("emerging") ? analyzer.detectMicrotrends(analyses) : analyzer.rank(analyses);
                    if (ranked.isEmpty()) System.out.println("No qualifying trends with sufficient history.");
                    ranked.forEach(TrendCLI::showAnalysis);
                }
                case "compare" -> {
                    var left = repository.getSignals(args[2], null, date(args, 4), date(args, 5));
                    var right = repository.getSignals(args[3], null, date(args, 4), date(args, 5));
                    analyzer.compare(left, right).forEach(TrendCLI::showAnalysis);
                }
            }
        } catch (IOException | IllegalArgumentException | java.time.DateTimeException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
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
