package fashion.application;

import fashion.model.TrendAnalysis;
import fashion.model.TrendSignal;
import java.util.List;

/** Common immutable output for CLI, GUI and later evidence-grounded explanations. */
public record AgentResponse(boolean success, String message, List<String> matches,
                            List<TrendSignal> signals, List<TrendAnalysis> analyses,
                            List<String> warnings) {
    public AgentResponse {
        matches = List.copyOf(matches);
        signals = List.copyOf(signals);
        analyses = List.copyOf(analyses);
        warnings = List.copyOf(warnings);
    }

    public static AgentResponse error(String message) {
        return new AgentResponse(false, message, List.of(), List.of(), List.of(), List.of());
    }
}
