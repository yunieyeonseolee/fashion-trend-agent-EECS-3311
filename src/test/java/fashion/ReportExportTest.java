package fashion;

import fashion.application.*;
import fashion.data.*;
import fashion.export.ReportExporter;
import java.io.IOException;
import java.nio.file.*;

/** Export must preserve missing-AI diagnostics and reject absent reports. */
public final class ReportExportTest {
    public static void main(String[] args) throws Exception {
        var repo=new TrendRepository(new CsvTrendAdapter(new CsvReader()));
        repo.importCsv(Path.of("data/sample-trends.csv"));
        var agent=new FashionTrendAgent(repo,(instructions,input,schema) -> {throw new IOException("Test offline service");});
        var response=agent.generateReport(AgentRequest.trend(AgentRequest.Operation.REPORT,"mary jane flats",null,null,null));
        var dir=Files.createTempDirectory("fashion-fallback-export-");
        var output=dir.resolve("fallback.txt");
        try {
            var exporter=new ReportExporter();
            exporter.write(response,output);
            String text=Files.readString(output);
            if (!text.contains("Warning: AI interpretation unavailable") || !text.contains("Unavailable; deterministic evidence is retained."))
                throw new AssertionError("missing fallback diagnostics");
            if (!text.contains("34.39") || !text.contains("synthetic-example")) throw new AssertionError("missing evidence");
            try {exporter.write(AgentResponse.error("No report"),dir.resolve("invalid.txt"));throw new AssertionError("missing report accepted");}
            catch(IllegalArgumentException expected) { /* expected */ }
            if (Files.exists(dir.resolve("invalid.txt"))) throw new AssertionError("invalid export created file");
            System.out.println("Passed 4 report export checks.");
        } finally {Files.deleteIfExists(output);Files.deleteIfExists(dir);}
    }
}
