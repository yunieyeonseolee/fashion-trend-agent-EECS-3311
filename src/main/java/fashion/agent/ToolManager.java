package fashion.agent;

import fashion.application.*;
import fashion.data.TrendRepository;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import static fashion.application.AgentRequest.Operation.*;

/** Only these read-only, deterministic operations can be selected by an LLM plan. */
public final class ToolManager {
    public static final Set<AgentRequest.Operation> ALLOWED=Set.of(SEARCH,HISTORY,ANALYZE,DISCOVER,EMERGING,COMPARE);
    private final FashionTrendAgent agent;
    private final TrendRepository repository;
    public ToolManager(FashionTrendAgent agent,TrendRepository repository) { this.agent=agent; this.repository=repository; }
    public EvidenceBundle.ToolResult invoke(AgentRequest request) {
        if (!ALLOWED.contains(request.operation())) throw new IllegalArgumentException("Tool is not allowed");
        AgentResponse response;
        try { response=agent.runDeterministic(request); }
        catch (IOException | IllegalArgumentException e) { response=AgentResponse.error(e.getMessage()); }
        var sources=new LinkedHashSet<String>();
        response.signals().forEach(s -> sources.add(s.source()));
        response.analyses().forEach(a -> repository.getSignals(a.trend(),a.category(),request.from(),request.to())
                .forEach(s -> sources.add(s.source())));
        return new EvidenceBundle.ToolResult(request,response,sources.stream().sorted().toList());
    }
}
