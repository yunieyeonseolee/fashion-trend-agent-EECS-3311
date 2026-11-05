package fashion.application;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

/** Interface-independent input. Invalid requests are rejected before any work runs. */
public record AgentRequest(Operation operation, Path csv, String name, String secondName,
                           String category, LocalDate from, LocalDate to, Ranking ranking) {
    public enum Operation { IMPORT, SEARCH, HISTORY, ANALYZE, DISCOVER, EMERGING, COMPARE, ASK, REPORT }
    public enum Ranking { MOMENTUM, GROWTH }

    public AgentRequest {
        Objects.requireNonNull(operation, "operation");
        ranking = ranking == null ? Ranking.MOMENTUM : ranking;
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must not be after end date");
        switch (operation) {
            case IMPORT -> {
                if (csv == null) throw new IllegalArgumentException("CSV path is required");
            }
            case SEARCH, HISTORY, ANALYZE, ASK, REPORT -> requireText(name, "Trend name or search query");
            case COMPARE -> {
                requireText(name, "First trend name");
                requireText(secondName, "Second trend name");
            }
            case DISCOVER, EMERGING -> requireText(category, "Category");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
    }

    public static AgentRequest importCsv(Path path) {
        return new AgentRequest(Operation.IMPORT, path, null, null, null, null, null, null);
    }

    public static AgentRequest trend(Operation operation, String name, String category, LocalDate from, LocalDate to) {
        return new AgentRequest(operation, null, name, null, category, from, to, null);
    }

    public static AgentRequest discover(String category, boolean emerging, Ranking ranking) {
        return new AgentRequest(emerging ? Operation.EMERGING : Operation.DISCOVER,
                null, null, null, category, null, null, ranking);
    }

    public static AgentRequest compare(String left, String right, LocalDate from, LocalDate to) {
        return new AgentRequest(Operation.COMPARE, null, left, right, null, from, to, null);
    }
}
