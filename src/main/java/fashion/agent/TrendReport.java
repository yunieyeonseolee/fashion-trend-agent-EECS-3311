package fashion.agent;

import java.util.Locale;
import fashion.llm.LLMClient;
import java.util.List;

/** AI prose never overwrites the deterministic evidence. Usage is per successful API reply. */
public record TrendReport(EvidenceBundle evidence, String narrative, List<LLMClient.Reply> usage) {
    public TrendReport { usage=List.copyOf(usage); }
    private static String number(double value) { return String.format(Locale.ROOT,"%.2f",value); }
    public String render() {
        var out=new StringBuilder("TREND INTELLIGENCE REPORT\n");
        out.append(evidence.question()).append("\n\nCalculated evidence (heuristics, not forecasts):\n");
        int index=0;
        for (var result:evidence.results()) {
            out.append(++index).append(". ").append(result.request().operation()).append("\n");
            var request=result.request();
            if (request.name()!=null) out.append("Trend / search: ").append(request.name()).append("\n");
            if (request.secondName()!=null) out.append("Compared with: ").append(request.secondName()).append("\n");
            if (request.category()!=null) out.append("Category: ").append(request.category()).append("\n");
            if (request.from()!=null || request.to()!=null) out.append("Date range: ")
                    .append(request.from()==null?"earliest":request.from()).append(" to ")
                    .append(request.to()==null?"latest":request.to()).append("\n");
            var response=result.response();
            if (!response.message().isBlank()) out.append(response.success()?"":"Error: ").append(response.message()).append("\n");
            response.matches().forEach(s -> out.append("Match: ").append(s).append("\n"));
            response.analyses().forEach(a -> {
                out.append(a.trend()).append(" (").append(a.category()).append("): ").append(a.lifecycle())
                        .append("; latest popularity ").append(number(a.latestPopularity())).append("\n");
                if (a.reliable()) out.append("Momentum: ").append(number(a.momentum()))
                        .append("; short growth: ").append(number(a.shortGrowth())).append("%/30d; long growth: ")
                        .append(number(a.longGrowth())).append("%/30d; acceleration: ").append(number(a.acceleration())).append("\n");
                out.append(a.limitation()).append("\n");
            });
            response.signals().forEach(s -> out.append(s.date()).append(" | ").append(s.trend()).append(" | ")
                    .append(number(s.popularity())).append(" | ").append(s.source()).append("\n"));
            response.warnings().forEach(w -> out.append("Warning: ").append(w).append("\n"));
        }
        out.append("\nSources: ").append(String.join(", ",evidence.sources())).append("\n");
        out.append("Synthetic source labels denote test data, not measured fashion evidence.\n");
        out.append("\nAI interpretation (review against the evidence):\n")
                .append(narrative.isBlank()?"Unavailable; deterministic evidence is retained.":narrative).append("\n");
        for (var reply:usage) out.append("API usage — model: ").append(reply.model()).append("; input tokens: ")
                .append(reply.inputTokens()).append("; output tokens: ").append(reply.outputTokens()).append("\n");
        if (!usage.isEmpty()) out.append("Token count -1 means unavailable. Failed requests may incur additional usage; verify provider billing.\n");
        return out.toString();
    }
}
