package fashion.export;

import fashion.application.AgentResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Saves existing evidence and narrative without another API request or overwriting a file. */
public final class ReportExporter {
    public void write(AgentResponse response, Path destination) throws IOException {
        if (response == null || response.report() == null)
            throw new IllegalArgumentException("Generate an Ask or Report result before exporting.");
        var text = new StringBuilder();
        response.warnings().forEach(w -> text.append("Warning: ").append(w).append("\n"));
        text.append(response.report().render());
        Files.writeString(destination,text.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
}
