package fashion.application;

import fashion.analysis.*;
import fashion.agent.*;
import fashion.llm.*;
import fashion.data.TrendRepository;
import fashion.model.TrendAnalysis;
import fashion.model.TrendSignal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Facade: coordinates storage, analysis and ranking without exposing them to views.
 * Validated plans use read-only tools; numerical results remain deterministic.
 */
public final class FashionTrendAgent {
    private final TrendRepository repository;

    private final AgentWorkflow workflow;
    public FashionTrendAgent(TrendRepository repository) { this(repository,new OpenAILLMClient()); }
    public FashionTrendAgent(TrendRepository repository, LLMClient llm) {
        this.repository=repository;
        this.workflow=new AgentWorkflow(repository,llm,new ToolManager(this,repository));
    }

    public AgentResponse run(AgentRequest request) throws IOException {
        return switch(request.operation()) {
            case ASK -> workflow.answer(request.name());
            case REPORT -> generateReport(request);
            default -> runDeterministic(request);
        };
    }
    public AgentResponse generateReport(AgentRequest request) throws IOException {
        return workflow.generateReport(request);
    }
    public AgentResponse runDeterministic(AgentRequest request) throws IOException {
        var analyzer = new TrendAnalyzer(request.ranking() == AgentRequest.Ranking.GROWTH
                ? new GrowthRankingStrategy() : new MomentumRankingStrategy());
        return switch (request.operation()) {
            case IMPORT -> {
                var result = repository.importCsv(request.csv());
                yield response("Imported " + result.signals().size() + " observations; "
                        + result.warnings().size() + " warnings.", List.of(), List.of(), List.of(), result.warnings());
            }
            case SEARCH -> {
                var names = repository.findByName(request.name());
                yield response(names.isEmpty() ? "No matching trends." : "", names, List.of(), List.of(), List.of());
            }
            case HISTORY -> {
                var history = history(request, request.name());
                yield response(history.isEmpty() ? "No observations match this trend and date range." : "",
                        List.of(), history, List.of(), List.of());
            }
            case ANALYZE -> response("", List.of(), List.of(),
                    List.of(analyzer.calculateMomentum(history(request, request.name()))), List.of());
            case DISCOVER, EMERGING -> discover(request, analyzer);
            case COMPARE -> compare(request, analyzer);
            case ASK, REPORT -> throw new IllegalArgumentException("AI operations cannot execute as deterministic tools");
        };
    }

    private List<TrendSignal> history(AgentRequest request, String name) {
        return repository.getSignals(name, request.category(), request.from(), request.to());
    }

    private AgentResponse discover(AgentRequest request, TrendAnalyzer analyzer) {
        var names = repository.findByCategory(request.category());
        if (names.isEmpty()) throw new IllegalArgumentException("No data for category. Available: "
                + String.join(", ", repository.categories()));
        var analyses = new ArrayList<TrendAnalysis>();
        var warnings = new ArrayList<String>();
        for (String name : names) {
            var signals = history(request, name);
            if (signals.isEmpty()) {
                warnings.add("Excluded " + name + ": no observations in date range");
                continue;
            }
            var analysis = analyzer.calculateMomentum(signals);
            if (analysis.reliable()) analyses.add(analysis);
            else warnings.add("Excluded " + name + ": " + analysis.limitation());
        }
        var ranked = request.operation() == AgentRequest.Operation.EMERGING
                ? analyzer.detectMicrotrends(analyses) : analyzer.rank(analyses);
        return response(ranked.isEmpty() ? "No qualifying trends with sufficient history." : "",
                List.of(), List.of(), ranked, warnings);
    }

    private AgentResponse compare(AgentRequest request, TrendAnalyzer analyzer) {
        var analyses = new ArrayList<TrendAnalysis>();
        var warnings = new ArrayList<String>();
        for (String name : List.of(request.name(), request.secondName())) {
            var signals = history(request, name);
            if (signals.isEmpty()) {
                warnings.add("No observations for " + name + " in the selected range");
                continue;
            }
            // Ambiguous category is a request error; do not silently choose one category.
            var analysis = analyzer.calculateMomentum(signals);
            analyses.add(analysis);
            if (!analysis.reliable()) warnings.add(name + ": " + analysis.limitation());
        }
        if (analyses.isEmpty()) return AgentResponse.error(String.join("; ", warnings));
        return response(warnings.isEmpty() ? "" : "Partial comparison: some metrics are unavailable.",
                List.of(), List.of(), analyses, warnings);
    }

    private AgentResponse response(String message, List<String> matches, List<TrendSignal> signals,
                                   List<TrendAnalysis> analyses, List<String> warnings) {
        return new AgentResponse(true, message, matches, signals, analyses, warnings);
    }
}
