package fashion.command;

import fashion.application.*;
import java.io.IOException;
import static fashion.application.AgentRequest.Operation.*;

public final class AnalyzeCommand extends AgentCommand {
    public AnalyzeCommand(FashionTrendAgent receiver, AgentRequest request) {
        super(receiver, request, SEARCH, HISTORY, ANALYZE);
    }
    public AgentResponse execute() throws IOException { return receiver.run(request); }
}
