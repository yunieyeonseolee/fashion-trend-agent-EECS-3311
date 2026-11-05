package fashion.command;

import fashion.application.*;
import java.io.IOException;
import static fashion.application.AgentRequest.Operation.COMPARE;

public final class CompareCommand extends AgentCommand {
    public CompareCommand(FashionTrendAgent receiver, AgentRequest request) { super(receiver, request, COMPARE); }
    public AgentResponse execute() throws IOException { return receiver.run(request); }
}
