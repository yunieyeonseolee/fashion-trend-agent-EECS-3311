package fashion.data;

import java.io.IOException;
import java.nio.file.Path;

public interface TrendDataImporter {
    ImportResult importData(Path path) throws IOException;
}
