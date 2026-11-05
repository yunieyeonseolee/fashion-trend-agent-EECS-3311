package fashion.agent;

import fashion.application.*;
import java.util.List;

public record EvidenceBundle(String question, List<ToolResult> results) {
    public EvidenceBundle { results=List.copyOf(results); }
    public record ToolResult(AgentRequest request, AgentResponse response, List<String> sources) {
        public ToolResult { sources=List.copyOf(sources); }
    }
    public List<String> sources() { return results.stream().flatMap(r -> r.sources().stream()).distinct().sorted().toList(); }
}
