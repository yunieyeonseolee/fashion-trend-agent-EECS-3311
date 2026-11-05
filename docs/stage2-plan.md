# Stage 2 implementation plan

Based on the Stage 1 report in this repository and the supplied Stage 2 instruction
PDF. This is a working plan, not the final implementation report. Status below
describes the implemented source, not a completed Stage 2 submission. See
[weekly commits](weekly-commits.md) for pushed milestones.

## Build order

1. **Data foundation (initial implementation):** Java domain observations, CSV adapter,
   repository, CLI import/search/history, and behavioral checks.
2. **Deterministic analysis (backend and CLI implemented):** momentum, growth, acceleration, lifecycle, category
   ranking, microtrend detection, and comparison. Define formulas, minimum history,
   zero-baseline handling, irregular dates, and aggregation of multiple sources before
   coding. Test known rising/falling/flat series and inadequate histories.
3. **Shared application workflow (implemented):** AgentRequest/AgentResponse,
   FashionTrendAgent, concrete commands, and AgentController now replace direct
   CLI-to-repository wiring. ToolManager now exposes only read-only deterministic operations to agent plans.
4. **Java GUI (implemented):** Swing supports CSV selection, search, analysis,
   discovery, comparison and history charts through the same commands as CLI.
   SwingWorker runs work off the event thread and renders results back on it.
5. **LLM agent (implemented, live verification pending):** LLMClient abstraction, provider implementation, validated request
   plans, allowlisted tools, evidence-grounded explanations and intelligence reports.
   Execute actual retrieval/analysis steps; do not implement a prompt-only wrapper.
   Make model ID configurable and verify API availability. The model ID proposed in
   Stage 1 must not be assumed to be an available public API ID.
6. **Submission:** finish integration, verify GUI/CLI from README, update diagrams,
   record each feature and relevant failure cases, and write/export stage2-report.pdf.

## Feature traceability

All ten original features remain in scope. A backend or CLI portion does not mean
the complete feature is implemented. No feature videos have been recorded yet.

| ID | Stage 1 feature | Current status | Implemented or planned participants |
| --- | --- | --- | --- |
| F01 | Natural-language query | GUI/CLI and API integration implemented; live-model check pending | AnalyzeCommand → FashionTrendAgent → AgentWorkflow → AgentPlanner/LLMClient → ToolManager → evidence synthesis |
| F02 | Search/retrieval | Backend, CLI and GUI implemented | TrendCLI → AgentController → Command → FashionTrendAgent → TrendRepository.findByName/getSignals → TrendSignal |
| F03 | CSV import/normalization | Backend, CLI and GUI implemented | TrendCLI → AgentController → Command → FashionTrendAgent → TrendRepository.importCsv → TrendDataImporter.importData → CsvTrendAdapter → CsvReader.readRows |
| F04 | Momentum analysis | Backend, CLI and GUI implemented | TrendAnalyzer.calculateMomentum, TrendAnalysis |
| F05 | Lifecycle classification | Deterministic backend, CLI and GUI implemented; AI explanation available through Ask/Report; live check pending | TrendAnalyzer.classifyLifecycle |
| F06 | Emerging microtrends | Deterministic backend, CLI and GUI implemented; AI summary available through Ask/Report; live check pending | TrendAnalyzer.detectMicrotrends, ranking strategies |
| F07 | Category discovery | Deterministic backend, CLI and GUI implemented; AI summary available through Ask/Report; live check pending | TrendRepository.findByCategory, TrendAnalyzer.rank |
| F08 | Comparison | Deterministic backend, CLI and GUI implemented; AI summary available through Ask/Report; live check pending | CompareCommand → FashionTrendAgent.compare → TrendAnalyzer.calculateMomentum per available history; warnings for missing/sparse data |
| F09 | History/visualization | CLI history and GUI history chart implemented | TrendCLI → AgentController → Command → FashionTrendAgent → TrendRepository.getSignals; TrendGUI.display → HistoryChart.setSignals |
| F10 | AI intelligence report | GUI/CLI implemented with numeric fallback; live-model check pending | ReportCommand → FashionTrendAgent.generateReport → AgentWorkflow.generateReport → ToolManager → EvidenceBundle → LLMClient → TrendReport |

Current files are under `src/main/java/fashion`: `model/TrendSignal.java`,
`data/{TrendRepository,TrendDataImporter,CsvTrendAdapter,CsvReader,ImportResult}.java`,
and `cli/TrendCLI.java`. Direct analysis remains deterministic; Ask/Report use a hybrid planning/evidence/synthesis workflow. ImportResult
returns immutable observations and diagnostics. Analysis classes are in `analysis/`; `model/TrendAnalysis.java` holds metrics and explicit unavailable states. The CLI prints normalized search
matches or chronologically ordered observations; Ask/Report can add a separately labeled model narrative.

## Patterns

| Stage 1 pattern | Current status | Design problem / roles |
| --- | --- | --- |
| Adapter | Implemented in data layer | TrendDataImporter is the target; CsvTrendAdapter converts CsvReader rows into TrendSignal; TrendRepository consumes the interface |
| MVC | Implemented | TrendGUI collects input/displays responses and HistoryChart draws observations; AgentController executes commands; repository and domain records hold model state |
| Command | Implemented | Command.execute defines the interface; AgentCommand holds request/receiver; four concrete commands invoke FashionTrendAgent; AgentController executes them |
| Facade | Implemented for deterministic operations | FashionTrendAgent.run hides repository, analysis and ranking workflows; AgentWorkflow coordinates planning/tools/LLM behind the facade |
| Strategy | Implemented in analysis layer | MomentumRankingStrategy and GrowthRankingStrategy provide interchangeable ordering through TrendRankingStrategy |

Do not count planned patterns as implemented. The completed application must use
at least five distinct course patterns meaningfully.

## Decisions and changes to track

| Original design | Current implementation choice | Reason | Impact |
| --- | --- | --- | --- |
| Java confirmed by instructor reminder; Stage 1 omits build tooling | JDK 17+ with shell build/test scripts | Small reproducible foundation without third-party dependencies | README provides commands; JDK 21 used locally |
| CSV conversion and warnings | Added ImportResult value object | Returns accepted observations and diagnostics together | F03, repository/adapter API; update final UML |
| Stored local observations, persistence unspecified | In-memory repository; reload CSV per invocation | Establish validated data pipeline first | No cross-process persistence; disclose this in demos/report |
| CLI → controller → command → facade → tools → repository | Shared controller, commands and facade now implemented; facade currently calls repository/analyzer directly | Reuse deterministic workflows across interfaces before adding LLM tools | F02–F09 use AgentRequest/AgentResponse; ToolManager and AgentPlanner are now implemented |
| Normalized CSV values, scale unspecified | Explicit 0–100 popularity input contract | Prevent incomparable or non-finite inputs | Dataset must supply that scale; no automatic rescaling |
| CSV format unspecified beyond columns | One record per physical line | Keeps row diagnostics precise in first importer | Embedded newlines unsupported; quoted commas/quotes supported |

## Submission and development records

Due **November 10, 2026**. Required code-commit periods (Toronto local time):

| Period | Minimum meaningful code commits | Remote history at initial inspection |
| --- | --- | --- |
| October 20–26 | 5 | No commits in cloned main history |
| October 27–November 2 | 5 | No commits in cloned main history |
| November 3–9 | 5 | No commits yet in cloned main history |

This observation concerns main as cloned, not an audit of all branches or push
timestamps. Check other development branches if applicable. Extra commits in one
period do not compensate for another; instructions specify a single total five-point
deduction if a period is deficient. Record genuine progress; do not backdate history
or manufacture trivial commits. Documentation-only commits do not meet this rule.

Before submission:

- Finish every original feature or explicitly account for modifications/omissions.
- Include GUI, CLI, actual LLM use, and meaningful multistep agent behavior.
- Document five implemented patterns and updated UML; update affected sequence/use-case diagrams.
- Record AI tool/model names, task usage, token counts and monetary costs from actual
  usage records. Mark unavailable values honestly instead of estimating as fact.
- Write `stage2-report.pdf`: overview/status, feature mapping, patterns, changes,
  AI collaboration, verification/weekly commit links, demonstration index.
- Give every feature an accessible video link/timestamp showing invocation, input,
  live output, AI involvement and relevant errors. Collectively show both GUI and CLI.
- Keep keys out of source, logs, report and videos. Verify setup from a fresh checkout.
- Push code and required materials to the existing public course repository and
  submit its URL through the course submission system.

## Analysis formulas and current verification

The Stage 1 report did not specify numerical formulas. The following are new
implementation decisions for F04–F08; include them in the final changes record:

- Average observations per date for one exact trend/category. Sources receive equal
  observation weights. Different popularity values from the same source/date are
  retained and also averaged; a future data policy may choose to reject conflicts.
- Require at least three distinct dates. Otherwise return Unknown and null metrics;
  exclude these series from rankings and microtrend results with a warning.
- Rate between two points = `(after - before) / max(before, 1) * 100 * 30 / elapsedDays`.
  This is linear percent change per 30 days, not compounded growth. The denominator
  floor makes zero baselines finite and must be disclosed when interpreting results.
- Short growth uses the last interval; long growth uses first-to-last; acceleration
  is short growth minus the preceding interval's rate.
- Momentum = `clamp(0.5 * short + 0.3 * long + 0.2 * acceleration, -100, 100)`.
- Lifecycle: Declining if short growth or momentum is below -5; otherwise Emerging
  if latest popularity <40, short growth >=10 and momentum >0; otherwise Growing
  if short growth >5 and momentum >0, unless popularity >=70 and acceleration <0;
  otherwise Peaking. These are provisional heuristics, not empirically validated
  stages. In particular, the four-stage model labels flat low-popularity series
  Peaking; this limitation should be discussed before finalizing the model.
- Microtrends require latest popularity <40, short growth >=10 and momentum >0.
- Rankings use descending momentum or short growth and break ties by name/category.
- Comparison applies the same formulas and optional date bounds to each trend. It
  does not interpolate missing observations or enforce identical observation dates;
  unequal histories must be considered when interpreting comparisons.

`./scripts/test.sh` currently runs 17 data checks, 19 analysis checks and 19 shared
application workflow checks, 21 Swing GUI checks and 32 LLM integration checks (108 total). CLI smoke
checks cover sample import, history filtering, analysis, microtrend discovery and
comparison. Synthetic data is used only to exercise actual calculations.

Next priority: live API verification with local credentials, GUI polish, human review, real-data selection and final report/demo preparation.

The [current architecture diagram](architecture.md) shows the implemented classes
and command sequence. Stage 1 diagrams must still be reconciled with the final system.

## GUI milestone

`gui/TrendGUI.java` collects operation-specific inputs, constructs the same concrete
commands as the CLI, and displays immutable AgentResponse objects. SwingWorker
serializes operations without blocking the event thread. `gui/HistoryChart.java`
plots actual observation dates; averaging and missing-data presentation are disclosed
on the chart. `scripts/gui.sh` compiles and launches the desktop application.

The chosen Swing toolkit is an implementation detail absent from Stage 1. The MVC
roles match the design. One reusable operation form replaces separate screens,
reducing duplicated controls while preserving the required workflows. GUI checks
exercise import, search, date filtering/validation, analysis, discovery, emerging
trends, partial comparison and read-only result tables. Native file-picker and
window interactions still need a manual desktop/demo pass.

## LLM milestone and changes from Stage 1

F01 uses AgentPlanner to request a strict JSON plan, validates all 1–4 steps, then
ToolManager runs allowlisted operations. Each local step may retrieve and analyze
observations; results and source labels form EvidenceBundle. A second LLM call
synthesizes only that evidence. Ambiguity yields a clarification instead of tools.

F10 uses a deterministic history → analysis → category discovery plan for a known
trend; this avoids spending a planning call on a fixed report workflow. Planning
and synthesis orchestration live in the added AgentWorkflow class, leaving the
facade compact. TrendReport retains evidence separately from AI text. Token counts
are reported for successful replies, not estimated monetary costs.

The Stage 1 model name is replaced with required local LLM_MODEL configuration:
account/model access has not been verified. Added Gson for safe JSON handling and
standard Java HttpClient for Responses API transport. No native function-calling
protocol is claimed: structured plan output drives application-level tool selection.

F05–F08 standalone commands remain deterministic; AI explanations are available by
asking the corresponding question or requesting a report. Live-model verification
is pending credentials. Automated tests exercise local fake API responses and do
not establish real provider availability or narrative quality. AgentPlanner's name
catalog is limited to 200 entries; generated prose still needs human checking.
