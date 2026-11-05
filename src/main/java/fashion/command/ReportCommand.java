package fashion.command;

import fashion.application.*;
import java.io.IOException;
import static fashion.application.AgentRequest.Operation.REPORT;

public final class ReportCommand extends AgentCommand {
    public ReportCommand(FashionTrendAgent receiver, AgentRequest request) { super(receiver,request,REPORT); }
    public AgentResponse execute() throws IOException { return receiver.generateReport(request); }
}
