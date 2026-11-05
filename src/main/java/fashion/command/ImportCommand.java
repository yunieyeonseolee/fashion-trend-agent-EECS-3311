package fashion.command;

import fashion.application.*;
import java.io.IOException;
import static fashion.application.AgentRequest.Operation.IMPORT;

public final class ImportCommand extends AgentCommand {
    public ImportCommand(FashionTrendAgent receiver, AgentRequest request) { super(receiver, request, IMPORT); }
    public AgentResponse execute() throws IOException { return receiver.run(request); }
}
