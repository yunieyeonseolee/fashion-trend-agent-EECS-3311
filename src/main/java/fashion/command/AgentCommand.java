package fashion.command;

import fashion.application.*;
import java.util.Arrays;
import java.util.Objects;

public abstract class AgentCommand implements Command {
    protected final FashionTrendAgent receiver;
    protected final AgentRequest request;

    protected AgentCommand(FashionTrendAgent receiver, AgentRequest request, AgentRequest.Operation... allowed) {
        this.receiver = Objects.requireNonNull(receiver);
        this.request = Objects.requireNonNull(request);
        if (!Arrays.asList(allowed).contains(request.operation()))
            throw new IllegalArgumentException("Unsupported operation for command: " + request.operation());
    }
}
