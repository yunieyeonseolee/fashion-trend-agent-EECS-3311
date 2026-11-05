package fashion;

import fashion.data.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/** Dependency-free behavioral checks; failures throw even without Java assertions enabled. */
public final class DataLayerTest {
    private static final String HEADER = "trend,category,date,popularity,source\n";
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path csv = Files.createTempFile("fashion-import-test-", ".csv");
        try {
            var adapter = new CsvTrendAdapter(new CsvReader());
            var repository = new TrendRepository(adapter);
            Files.writeString(csv, HEADER + """
                    Mary Jane Flats, FOOTWEAR ,2026-10-01,42,"Example, source"
                     MARY   JANE FLATS ,footwear,2026-10-01,42,"Example, source"
                    Mary Jane Flats,footwear,2026-09-01,28,example
                    broken,footwear,2026-02-30,40,example
                    broken,footwear,2026-10-01,NaN,example
                    broken,footwear,2026-10-01,101,example
                    broken,footwear,2026-10-01,10,
                    "unclosed,footwear,2026-10-01,10,example
                    "quoted"junk,footwear,2026-10-01,10,example
                    "Bag ""Charms""\",accessories,2026-10-01,12,example
                    """);
            var result = repository.importCsv(csv);
            check(result.signals().size() == 3, "accept only valid unique records");
            check(result.warnings().size() == 7, "warn for each invalid/duplicate record");
            check(repository.findByName("MARY  JANE").equals(java.util.List.of("mary jane flats")), "normalized search");
            check(repository.findByName("charms").get(0).equals("bag \"charms\""), "escaped quotes");
            var history = repository.getSignals("Mary Jane Flats", null, null, null);
            check(history.get(0).date().equals(LocalDate.of(2026, 9, 1)), "chronological history");
            check(history.get(1).source().equals("Example, source"), "quoted comma");
            check(repository.getSignals("Mary Jane Flats", "FOOTWEAR", LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 10, 1)).size() == 1, "inclusive date/category filters");
            check(repository.getSignals("Mary Jane Flats", "accessories", null, null).isEmpty(), "category mismatch");
            check(repository.findByName("unknown").isEmpty(), "no matches");
            check(repository.importCsv(csv).signals().isEmpty(), "repeat import is idempotent");
            try {
                repository.getSignals("x", null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1));
                throw new AssertionError("reversed range accepted");
            } catch (IllegalArgumentException expected) { checks++; }
            Files.writeString(csv, "trend,date\nx,2026-10-01\n");
            rejects(adapter, csv, "Missing CSV column");
            Files.writeString(csv, HEADER + "x,footwear,2026-10-01,-1,example\n");
            rejects(adapter, csv, "No valid observations");
            check(repository.getSignals("Mary Jane Flats", null, null, null).size() == 2,
                    "failed imports preserve existing observations");
            Files.writeString(csv, "");
            rejects(adapter, csv, "CSV is empty");
            Files.writeString(csv, "\uFEFFsource,popularity,date,category,trend\nexample,10,2026-10-01,tops,shirt\n");
            check(adapter.importData(csv).signals().get(0).trend().equals("shirt"), "BOM and reordered headers");
            Files.delete(csv);
            try {
                adapter.importData(csv);
                throw new AssertionError("missing file accepted");
            } catch (IOException expected) { checks++; }
            System.out.println("Passed " + checks + " data-layer checks.");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void rejects(CsvTrendAdapter adapter, Path path, String message) throws Exception {
        try {
            adapter.importData(path);
            throw new AssertionError("invalid CSV accepted");
        } catch (IOException expected) {
            check(expected.getMessage().contains(message), "clear import error: " + message);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
