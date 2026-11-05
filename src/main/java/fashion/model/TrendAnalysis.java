package fashion.model;

/** Growth fields are percent change per 30 days; acceleration is their difference.
 * Null metrics explicitly mean insufficient distinct-date observations.
 */
public record TrendAnalysis(String trend, String category, int observations,
                            double latestPopularity, Double shortGrowth,
                            Double longGrowth, Double acceleration, Double momentum,
                            String lifecycle, String limitation) {
    public boolean reliable() { return momentum != null; }
}
