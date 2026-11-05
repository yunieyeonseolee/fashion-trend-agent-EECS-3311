package fashion.analysis;

import fashion.model.TrendAnalysis;
import java.util.Comparator;
import java.util.List;

public final class GrowthRankingStrategy implements TrendRankingStrategy {
    public List<TrendAnalysis> rank(List<TrendAnalysis> analyses) {
        return analyses.stream().filter(TrendAnalysis::reliable)
                .sorted(Comparator.comparingDouble((TrendAnalysis a) -> a.shortGrowth()).reversed()
                        .thenComparing(TrendAnalysis::trend).thenComparing(TrendAnalysis::category)).toList();
    }
}
