package fashion.agent;

import fashion.application.AgentRequest;
import fashion.llm.LLMClient;
import java.util.List;

public record AgentPlan(List<AgentRequest> steps, String clarification, LLMClient.Reply planningReply) {
    public AgentPlan { steps=List.copyOf(steps); }
}
