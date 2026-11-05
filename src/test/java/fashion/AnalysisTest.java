package fashion;

import fashion.analysis.*;
import fashion.model.*;
import java.time.LocalDate;
import java.util.List;

public final class AnalysisTest {
    private static int checks;

    public static void main(String[] args) {
        var analyzer = new TrendAnalyzer(new MomentumRankingStrategy());
        var rising = analyzer.calculateMomentum(series("rising", 10, 20, 30));
        near(rising.shortGrowth(), 50, "recent growth");
        near(rising.longGrowth(), 100, "long growth");
        near(rising.acceleration(), -50, "acceleration");
        near(rising.momentum(), 45, "weighted momentum");
        check(rising.lifecycle().equals("Emerging"), "low popularity growing trend");
        var falling = analyzer.calculateMomentum(series("falling", 80, 60, 40));
        check(falling.lifecycle().equals("Declining"), "falling series");
        var flat = analyzer.calculateMomentum(series("flat", 50, 50, 50));
        near(flat.momentum(), 0, "flat series");
        check(flat.lifecycle().equals("Peaking"), "flat classification");
        var zero = analyzer.calculateMomentum(series("zero", 0, 0, 10));
        check(Double.isFinite(zero.momentum()) && zero.momentum() <= 100, "zero baseline finite/clamped");
        var sparse = analyzer.calculateMomentum(series("sparse", 10, 20, 30).subList(0, 2));
        check(!sparse.reliable() && sparse.lifecycle().equals("Unknown"), "sparse history unavailable");
        var sourceA = series("combined", 10, 20, 30);
        var extra = new TrendSignal("combined", "footwear", sourceA.get(2).date(), 50, "other-source");
        var combined = analyzer.calculateMomentum(List.of(sourceA.get(2), extra, sourceA.get(0), sourceA.get(1)));
        near(combined.latestPopularity(), 40, "same-date observations averaged");
        check(combined.observations() == 3, "distinct dates counted");
        check(analyzer.detectMicrotrends(List.of(rising, falling, sparse, flat)).equals(List.of(rising)), "microtrend filter");
        check(analyzer.rank(List.of(falling, sparse, rising)).equals(List.of(rising, falling)), "ranking excludes sparse");
        // Deliberately different scores verify strategies actually choose different orderings.
        var a = new TrendAnalysis("a", "x", 3, 20, 10., 0., 0., 80., "Growing", "test");
        var b = new TrendAnalysis("b", "x", 3, 20, 90., 0., 0., 20., "Growing", "test");
        check(new MomentumRankingStrategy().rank(List.of(a,b)).get(0) == a, "momentum strategy");
        check(new GrowthRankingStrategy().rank(List.of(a,b)).get(0) == b, "growth strategy");
        var irregular = List.of(new TrendSignal("irregular", "x", LocalDate.of(2026,1,1),10,"test"),
                new TrendSignal("irregular", "x", LocalDate.of(2026,1,31),20,"test"),
                new TrendSignal("irregular", "x", LocalDate.of(2026,4,1),30,"test"));
        near(analyzer.calculateMomentum(irregular).shortGrowth(), 25, "irregular intervals account for elapsed days");
        check(analyzer.compare(series("left",10,20,30),series("right",50,50,50)).size() == 2, "comparison");
        try {
            analyzer.calculateMomentum(List.of(sourceA.get(0), series("other",10,20,30).get(1)));
            throw new AssertionError("mixed trends accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        System.out.println("Passed " + checks + " analysis checks.");
    }

    private static List<TrendSignal> series(String name, double a, double b, double c) {
        var start = LocalDate.of(2026, 1, 1);
        return List.of(new TrendSignal(name,"footwear",start,a,"test"),
                new TrendSignal(name,"footwear",start.plusDays(30),b,"test"),
                new TrendSignal(name,"footwear",start.plusDays(60),c,"test"));
    }
    private static void near(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 0.000001, label + ": " + actual);
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
