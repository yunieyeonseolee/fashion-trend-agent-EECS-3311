package fashion.data;

import fashion.model.TrendSignal;
import java.util.List;

/** Signals actually accepted at this boundary, plus rejected/duplicate row diagnostics. */
public record ImportResult(List<TrendSignal> signals, List<String> warnings) {
    public ImportResult {
        signals = List.copyOf(signals);
        warnings = List.copyOf(warnings);
    }
}
