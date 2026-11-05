package fashion.data;

import fashion.model.TrendSignal;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/** Adapter: translates CsvReader rows into validated domain observations. */
public final class CsvTrendAdapter implements TrendDataImporter {
    private final CsvReader reader;

    public CsvTrendAdapter(CsvReader reader) { this.reader = reader; }

    @Override
    public ImportResult importData(Path path) throws IOException {
        var rows = reader.readRows(path);
        if (rows.isEmpty()) throw new IOException("CSV is empty");
        var header = rows.get(0);
        if (header.error() != null) throw new IOException("Invalid CSV header: " + header.error());
        var columns = new HashMap<String, Integer>();
        for (int i = 0; i < header.fields().size(); i++) {
            String name = TrendSignal.normalize(header.fields().get(i));
            if (columns.put(name, i) != null) throw new IOException("Duplicate header: " + name);
        }
        for (String name : List.of("trend", "category", "date", "popularity", "source"))
            if (!columns.containsKey(name)) throw new IOException("Missing CSV column: " + name);
        var signals = new LinkedHashSet<TrendSignal>();
        var warnings = new ArrayList<String>();
        for (var row : rows.subList(1, rows.size())) {
            try {
                if (row.error() != null) throw new IllegalArgumentException(row.error());
                if (row.fields().size() != header.fields().size())
                    throw new IllegalArgumentException("column count does not match header");
                var f = row.fields();
                var signal = new TrendSignal(f.get(columns.get("trend")), f.get(columns.get("category")),
                        LocalDate.parse(f.get(columns.get("date")).strip()),
                        Double.parseDouble(f.get(columns.get("popularity")).strip()),
                        f.get(columns.get("source")));
                if (!signals.add(signal)) warnings.add("Line " + row.line() + ": duplicate observation skipped");
            } catch (RuntimeException e) {
                warnings.add("Line " + row.line() + ": " + e.getMessage());
            }
        }
        if (signals.isEmpty()) throw new IOException("No valid observations. " + String.join("; ", warnings));
        return new ImportResult(new ArrayList<>(signals), warnings);
    }
}
