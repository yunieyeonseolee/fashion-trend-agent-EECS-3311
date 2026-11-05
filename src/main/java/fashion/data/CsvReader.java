package fashion.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** UTF-8 CSV parser supporting quoted commas and escaped quotes.
 * This initial import format deliberately requires one record per physical line.
 */
public final class CsvReader {
    public record Row(int line, List<String> fields, String error) {}

    public List<Row> readRows(Path path) throws IOException {
        var rows = new ArrayList<Row>();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (number == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isBlank()) continue;
                try {
                    rows.add(new Row(number, parse(line), null));
                } catch (IllegalArgumentException e) {
                    rows.add(new Row(number, List.of(), e.getMessage()));
                }
            }
        }
        return List.copyOf(rows);
    }

    private List<String> parse(String line) {
        var fields = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else field.append(c);
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                closed = false;
            } else if (closed) {
                if (!Character.isWhitespace(c))
                    throw new IllegalArgumentException("unexpected text after closing quote");
            } else if (c == '"') {
                if (!field.toString().isBlank())
                    throw new IllegalArgumentException("quote inside an unquoted field");
                field.setLength(0);
                quoted = true;
            } else field.append(c);
        }
        if (quoted) throw new IllegalArgumentException("unclosed quote; multiline fields are unsupported");
        fields.add(field.toString());
        return List.copyOf(fields);
    }
}
