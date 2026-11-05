package fashion.analysis;

import fashion.model.TrendAnalysis;
import java.util.List;

public interface TrendRankingStrategy {
    List<TrendAnalysis> rank(List<TrendAnalysis> analyses);
}
