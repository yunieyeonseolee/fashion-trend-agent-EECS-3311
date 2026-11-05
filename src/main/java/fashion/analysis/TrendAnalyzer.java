package fashion.analysis;

import fashion.model.TrendAnalysis;
import fashion.model.TrendSignal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Transparent heuristic scoring, not a prediction of future fashion demand. */
public final class TrendAnalyzer {
    private final TrendRankingStrategy strategy;

    public TrendAnalyzer(TrendRankingStrategy strategy) { this.strategy = strategy; }

    public TrendAnalysis calculateMomentum(List<TrendSignal> signals) {
        if (signals.isEmpty()) throw new IllegalArgumentException("No observations for this trend and range");
        var first = signals.get(0);
        if (signals.stream().anyMatch(s -> !s.trend().equals(first.trend()) || !s.category().equals(first.category())))
            throw new IllegalArgumentException("Select one trend and category for analysis");
        // Equal weighting of observations on the same date. No interpolation of missing dates.
        var grouped = signals.stream().collect(Collectors.groupingBy(TrendSignal::date,
                TreeMap::new, Collectors.averagingDouble(TrendSignal::popularity)));
        var dates = new ArrayList<>(grouped.keySet());
        int n = dates.size();
        double latest = grouped.lastEntry().getValue();
        if (n < 3) return new TrendAnalysis(first.trend(), first.category(), n, latest,
                null, null, null, null, "Unknown", "At least three distinct dates are required");
        LocalDate last = dates.get(n - 1), previous = dates.get(n - 2), before = dates.get(n - 3);
        double recent = growth(grouped.get(previous), latest, previous, last);
        double prior = growth(grouped.get(before), grouped.get(previous), before, previous);
        double longer = growth(grouped.firstEntry().getValue(), latest, dates.get(0), last);
        double acceleration = recent - prior;
        double momentum = Math.max(-100, Math.min(100, .5 * recent + .3 * longer + .2 * acceleration));
        String stage = classifyLifecycle(latest, recent, acceleration, momentum);
        return new TrendAnalysis(first.trend(), first.category(), n, latest, recent, longer,
                acceleration, momentum, stage, "Heuristic; not a forecast. Zero baselines use a 1-point floor.");
    }

    private double growth(double before, double after, LocalDate start, LocalDate end) {
        return (after - before) / Math.max(before, 1) * 100 * 30 / ChronoUnit.DAYS.between(start, end);
    }

    public String classifyLifecycle(double popularity, double recentGrowth, double acceleration, double momentum) {
        if (recentGrowth < -5 || momentum < -5) return "Declining";
        if (popularity < 40 && recentGrowth >= 10 && momentum > 0) return "Emerging";
        if (recentGrowth > 5 && momentum > 0 && !(popularity >= 70 && acceleration < 0)) return "Growing";
        return "Peaking";
    }

    public List<TrendAnalysis> rank(List<TrendAnalysis> analyses) { return strategy.rank(analyses); }

    public List<TrendAnalysis> detectMicrotrends(List<TrendAnalysis> analyses) {
        return rank(analyses.stream().filter(TrendAnalysis::reliable)
                .filter(a -> a.latestPopularity() < 40 && a.shortGrowth() >= 10 && a.momentum() > 0).toList());
    }

    public List<TrendAnalysis> compare(List<TrendSignal> left, List<TrendSignal> right) {
        return List.of(calculateMomentum(left), calculateMomentum(right));
    }
}
