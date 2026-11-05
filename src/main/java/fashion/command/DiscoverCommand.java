package fashion.command;

import fashion.application.*;
import java.io.IOException;
import static fashion.application.AgentRequest.Operation.*;

public final class DiscoverCommand extends AgentCommand {
    public DiscoverCommand(FashionTrendAgent receiver, AgentRequest request) {
        super(receiver, request, DISCOVER, EMERGING);
    }
    public AgentResponse execute() throws IOException { return receiver.run(request); }
}
