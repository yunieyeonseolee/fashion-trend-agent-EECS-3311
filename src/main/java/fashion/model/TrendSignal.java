package fashion.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** One observation on a documented 0–100 popularity scale. */
public record TrendSignal(String trend, String category, LocalDate date,
                          double popularity, String source) {
    public TrendSignal {
        trend = normalize(trend);
        category = normalize(category);
        source = Objects.requireNonNull(source, "source").strip();
        Objects.requireNonNull(date, "date");
        if (trend.isEmpty() || category.isEmpty() || source.isEmpty())
            throw new IllegalArgumentException("trend, category and source must not be blank");
        if (!Double.isFinite(popularity) || popularity < 0 || popularity > 100)
            throw new IllegalArgumentException("popularity must be a finite number between 0 and 100");
    }

    public static String normalize(String value) {
        return Objects.requireNonNull(value).strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
