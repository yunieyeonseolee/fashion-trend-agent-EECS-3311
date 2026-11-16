# Fashion Trend Intelligence Agent — EECS 3311

Java implementation of the [Stage 1 design](Stage%201/Stage1_Design_Report_220518676.pdf).
Stage 2 submission deadline: November 10, 2026.

## Current implementation

The application provides CSV import/normalization (F03), trend search
and filtered retrieval (F02), deterministic analysis and discovery/comparison (F04–F08),
and history visualization (F09). The Swing GUI and CLI use the same shared controller
and facade. Natural-language planning (F01) and evidence-grounded reports (F10) are
implemented with OpenAI Responses API integration. **Live Ask and Report smoke tests passed with `gpt-6-luna` on November 5, 2026.**
Automated API tests use a local fake server. See [live verification](docs/live-verification.md)
and the [implementation report](stage2-report.pdf) for feature mappings, design
patterns, AI collaboration records, and the weekly commit summary.

## Prerequisites

Install JDK 17 or newer and use Bash (macOS/Linux, or Git Bash/WSL on Windows).
Install `curl` plus `shasum` or `sha256sum`. Scripts download pinned Gson 2.13.2
from Maven Central on first run and verify its SHA-256, then compile Java sources.
Internet is needed for that first dependency download and live API calls. Maven is
not required. Cached dependencies live under ignored `build/deps/`. See
[dependency details](docs/dependencies.md).

## Quick start

From the repository root, launch the section-based GUI with the sample data:

```bash
./scripts/studio.sh data/sample-trends.csv
```

A graphical desktop is required for the GUI. Search, history, analysis, discovery
and comparison work without an API key. For Ask and AI report interpretation,
complete [local API configuration](#local-api-configuration).

## Command-line usage

```bash
./scripts/run.sh import data/sample-trends.csv
./scripts/run.sh search data/sample-trends.csv "mary"
./scripts/run.sh history data/sample-trends.csv "mary jane flats"
./scripts/run.sh history data/sample-trends.csv "mary jane flats" footwear 2026-09-01 2026-10-01
./scripts/run.sh analyze data/sample-trends.csv "mary jane flats"
./scripts/run.sh discover data/sample-trends.csv footwear growth
./scripts/run.sh emerging data/sample-trends.csv accessories
./scripts/run.sh compare data/sample-trends.csv "mary jane flats" "chunky sneakers"
./scripts/run.sh --help
```

Use `-` for an optional category or date filter that you want to omit. Date bounds
are inclusive. Search matches normalized name fragments; history uses an exact
normalized name. Invalid commands/imports exit with status 1. No matches produce
an informative empty result. Import warnings go to stderr.

The repository currently lives in memory. Each command loads its supplied CSV;
import does not create a persistent database. `TrendRepository` also deduplicates
repeated imports within the same process.

## Desktop GUI — section-based layout

```bash
./scripts/studio.sh data/sample-trends.csv
```

Use **Import CSV** in the sidebar to load observations, then choose a section:

| Section | Actions |
| --- | --- |
| Explore | Find trends, analyze an exact trend name, or view history |
| Discover | Rank a category by momentum/growth or find emerging trends |
| Compare | Compare two exact trend names over an optional date range |
| AI reports | Ask a natural-language question or create a report for an exact trend |

Summary cards show popularity, short growth, momentum and lifecycle. Select a
result row to view its analysis or history. The original GUI below remains available.

Search and Ask ignore category/date fields; other actions use the visible filters.
Selected-result shortcuts open unfiltered analysis/history for that trend/category.
AI interpretation and calculated evidence have separate tabs; report export retains
both. Changing sections clears the displayed results and disables stale exports.
Work runs in the background. Minimum window size is 1180×780. Headless previews
were checked at that size and 1320×900; native desktop/file dialogs still need review.

## Original GUI — operation selector

```bash
./scripts/gui.sh
# Or preload the synthetic sample CSV:
./scripts/gui.sh data/sample-trends.csv
```

Requires a graphical desktop and JDK 17+. Use **Import CSV…**, then select Search,
History, Analyze, Discover, Emerging, Compare, Ask or Report. Enter the trend/category required
by that operation and click **Run**. History and analysis require an exact trend
name; Search accepts name fragments. Category discovery requires a category.
Blank category/date fields mean no filter; dates are inclusive. Disabled fields do
not affect requests. Choose Momentum or Growth for discovery rankings.

Results appear in a sortable, read-only table. Select a row and choose **Analyze
selection** or **View history** to follow that trend without retyping its name.
**Compare with selection** fills the second trend; enter the first and click Run.
These actions respect the displayed sort order and keep date filters. Search rows
have no category; select a category manually if a name spans multiple categories.

The dashboard uses a scrollable control sidebar with a fixed Run button. Analysis
summary cards show the selected trend’s popularity, short growth, momentum and
lifecycle; cards follow sorted selections and clear on errors. Wide tables scroll
horizontally. A progress indicator shows background work.

The workspace has separate **Results**, **History chart**, and **Report / AI** tabs.
Warnings and errors remain visible in **Status & notes** below.
The history chart shows date-spaced points on a 0–100 scale, averaging observations
on the same date without interpolating unobserved dates. If a name spans multiple
categories, filter to one category to draw its chart. Import and analysis run on a
background worker, with controls disabled until the result has been rendered.
Imports accumulate in the current session; closing the app discards in-memory data.

For **Ask**, enter a natural-language question in the Trend / question field. For
**Report**, enter an exact trend name and optional category/dates. Results include a
Report / AI tab with calculated evidence and a separate AI interpretation. `./scripts/test.sh` includes headless Swing interaction checks and writes
an ignored preview image to `build/gui-preview.png`.

After Ask or Report finishes, **Save report…** exports the current evidence,
interpretation, sources, API usage and warnings as UTF-8 text. Numeric fallback
reports can also be saved. Saving does not call the API again. Choose a new filename:
existing files are never overwritten. A failed save keeps the report available for
retry; a later non-report result disables export to prevent saving stale content.
Native file-picker interaction still needs a manual desktop/demo check.

## CSV format

UTF-8 CSV, with required headers `trend,category,date,popularity,source` in any
order. Extra named columns are allowed. Dates must use `YYYY-MM-DD`, and popularity
must be finite and within 0–100. Trend/category values are lowercased and whitespace
is normalized. Source labels retain their case. Exact normalized observations are
deduplicated; different source/value observations are retained.

Quoted commas and doubled quotes are supported. Multiline fields are not supported.
Invalid rows are skipped with physical line-number warnings; missing headers,
unreadable files, or zero valid observations fail the import. Blank lines are ignored.

`data/sample-trends.csv` is **synthetic test data**, not measured fashion evidence.
Do not describe its calculated results as real-world trend findings. A future real
dataset must use a documented comparable popularity scale.

## Shared application workflow

The CLI parses input into `AgentRequest`, constructs an `ImportCommand`,
`AnalyzeCommand`, `DiscoverCommand`, `CompareCommand` or `ReportCommand`, and submits it through
`AgentController`. The command delegates to `FashionTrendAgent`, which coordinates
repository access, analysis and ranking. Every operation returns an immutable
`AgentResponse` suitable for both CLI and Swing GUI rendering. Expected
import/request failures become error responses. Comparison preserves available
results and includes warnings when one trend lacks data.

See the [current architecture diagram](docs/architecture.md).

## Analysis interpretation

Scores use documented, deterministic heuristics; they are not forecasts. At least
three distinct dates are needed. Same-date observations are averaged, and growth
rates account for elapsed days. See [formulas and limitations](stage2-report.md#44-analysis-rules-and-limitations).

## Local API configuration

If `.env` does not exist, copy the example:

```bash
cp .env.example .env
```

Edit `.env` locally, setting `LLM_API_KEY` to your OpenAI API key and `LLM_MODEL` to
an account-supported Responses API model with Structured Outputs. No model ID is
assumed from Stage 1. Do not paste your key into chat or commit it. Same-named
process environment variables override `.env`; `OPENAI_API_KEY` is a fallback if
`LLM_API_KEY` is absent. Values are reread per API request without restarting.
The file supports simple `KEY=value`, optional matching quotes, and full-line
comments; it does not execute shell commands, interpolate variables or parse inline
comments. Local `.env` variants and `config.local.json` are ignored.

```bash
./scripts/run.sh ask data/sample-trends.csv "What footwear trends are emerging?"
./scripts/run.sh report data/sample-trends.csv "mary jane flats"
```

Ask uses one API call for a typed plan and, when evidence exists, one for synthesis.
Report builds a three-tool local plan and uses one API call for synthesis. Each API
request has a 60-second timeout and a 2,000 output-token cap. Calls can incur usage
charges. Reports show provider-returned token counts for successful replies; use
provider billing for actual costs and failed-request usage. Ordinary deterministic
commands do not call the API. Questions, catalog names and relevant imported
observations/metrics are sent to OpenAI for AI operations; do not import sensitive
material you do not intend to send. Responses requests set `store: false`.

Missing credentials or failed synthesis leave explicit warnings and retain report
evidence. Ask cannot interpret a new question when planning is unavailable; use
analyze/history/report directly instead. Plans are restricted to four read-only
steps and validated before execution. No model-selected imports, paths, commands or
recursive AI calls are allowed. Evidence over 60,000 serialized characters skips
synthesis with a warning; narrow the date range. Planning catalogs include up to
200 sorted trend names. Model prose is not automatically fact-verified; review it
against the unchanged calculated evidence.

The [OpenAI Structured Outputs guide](https://developers.openai.com/api/docs/guides/structured-outputs)
documents the `text.format` schema used for planning. The application executes its
own validated plan through local tools; it does not use native API function-calling.

## Demonstration video

[Watch or download the demonstration video](demos/Demo%20Video-YeonseoLee.mp4).
Feature timestamps are linked in the Word/PDF report. F10 report generation and
export still need a supplementary recording.

## Report and supporting documentation

The [Stage 2 report (Word)](stage2-report.docx) is the current submission draft.
The [PDF](stage2-report.pdf) is an older draft and needs re-exporting from Word.
The Word report is the editing source; the older
[Markdown draft](stage2-report.md) is retained as a development record.
The report contains:

- Sections 1–4: implementation status, feature mappings, design patterns and changes.
- Section 5: AI tools, collaboration records and usage/cost information.
- Section 6: verification results and weekly commit links.
- Section 7: feature demonstration links and timestamps.

These records are maintained in the report rather than separate planning,
collaboration, demo-guide or weekly-log files.

Supporting files:

- [Architecture](docs/architecture.md) and [diagram assets](docs/diagrams/)
- [Dependencies](docs/dependencies.md)
- [Live API verification](docs/live-verification.md)
- [Report rebuilding instructions](docs/report-building.md)

## Verification

```bash
./scripts/test.sh
```

The suite checks the data layer, analysis, shared workflows, both Swing interfaces,
LLM integration using local test doubles, and report export. Automated tests do not
make paid API requests. GUI previews are generated under ignored `build/`.
Native desktop dialogs and the final demonstration recordings require manual review.

The PDF includes updated diagrams and the weekly commit summary. It remains a
review draft: the F10 demonstration and final review still need attention before
submission.
