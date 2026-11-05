package fashion.command;

import fashion.application.AgentResponse;
import java.io.IOException;

public interface Command {
    AgentResponse execute() throws IOException;
}
