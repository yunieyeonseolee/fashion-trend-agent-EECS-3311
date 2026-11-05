package fashion.data;

import fashion.model.TrendSignal;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** In-memory storage; callers can re-import a CSV when starting a session. */
public final class TrendRepository {
    private final TrendDataImporter importer;
    private final LinkedHashSet<TrendSignal> signals = new LinkedHashSet<>();

    public TrendRepository(TrendDataImporter importer) { this.importer = importer; }

    public ImportResult importCsv(Path path) throws IOException {
        var result = importer.importData(path);
        var added = new ArrayList<TrendSignal>();
        var warnings = new ArrayList<>(result.warnings());
        for (var signal : result.signals()) {
            if (signals.add(signal)) added.add(signal);
            else warnings.add("Already stored: " + signal.trend() + " on " + signal.date());
        }
        return new ImportResult(added, warnings);
    }

    public List<String> findByName(String query) {
        String normalized = TrendSignal.normalize(query);
        return signals.stream().map(TrendSignal::trend).distinct()
                .filter(name -> name.contains(normalized)).sorted().toList();
    }

    public List<String> categories() {
        return signals.stream().map(TrendSignal::category).distinct().sorted().toList();
    }

    public List<String> findByCategory(String category) {
        String normalized = TrendSignal.normalize(category);
        return signals.stream().filter(s -> s.category().equals(normalized))
                .map(TrendSignal::trend).distinct().sorted().toList();
    }

    public List<TrendSignal> getSignals(String name, String category, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must not be after end date");
        String trend = TrendSignal.normalize(name);
        String normalizedCategory = category == null ? null : TrendSignal.normalize(category);
        return signals.stream().filter(s -> s.trend().equals(trend))
                .filter(s -> normalizedCategory == null || s.category().equals(normalizedCategory))
                .filter(s -> from == null || !s.date().isBefore(from))
                .filter(s -> to == null || !s.date().isAfter(to))
                .sorted(Comparator.comparing(TrendSignal::date).thenComparing(TrendSignal::source)
                        .thenComparing(TrendSignal::category).thenComparingDouble(TrendSignal::popularity))
                .toList();
    }
}
