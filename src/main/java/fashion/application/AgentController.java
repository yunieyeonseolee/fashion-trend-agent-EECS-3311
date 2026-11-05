package fashion.application;

import fashion.command.Command;
import java.io.IOException;

/** Invokes any application command and converts expected failures to shared output. */
public final class AgentController {
    public AgentResponse execute(Command command) {
        try {
            return command.execute();
        } catch (IOException | IllegalArgumentException e) {
            return AgentResponse.error(e.getMessage());
        }
    }
}
