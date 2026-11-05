# Fashion Trend Intelligence Agent — EECS 3311

Java implementation of the [Stage 1 design](Stage%201/Stage1_Design_Report_220518676.pdf).
Stage 2 is due November 10, 2026.

## Current implementation

The application provides CSV import/normalization (F03), trend search
and filtered retrieval (F02), deterministic analysis and discovery/comparison (F04–F08),
and history visualization (F09). The Swing GUI and CLI use the same shared controller
and facade. Natural-language planning (F01) and evidence-grounded reports (F10) are
implemented with OpenAI Responses API integration. **Live-model verification is
pending local credentials; automated API tests use a local fake server.** See the
[implementation plan and feature mapping](docs/stage2-plan.md).

## Prerequisites and running

Install JDK 17 or newer and use Bash (macOS/Linux, or Git Bash/WSL on Windows).
Install `curl` plus `shasum` or `sha256sum`. Scripts download pinned Gson 2.13.2
from Maven Central on first run and verify its SHA-256, then compile Java sources.
Internet is needed for that first dependency download and live API calls. Maven is
not required. Cached dependencies live under ignored `build/deps/`. See
[dependency details](docs/dependencies.md).

```bash
./scripts/test.sh
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

## Desktop GUI

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

Results appear in a sortable, read-only table; warnings and errors appear below.
The history chart shows date-spaced points on a 0–100 scale, averaging observations
on the same date without interpolating unobserved dates. If a name spans multiple
categories, filter to one category to draw its chart. Import and analysis run on a
background worker, with controls disabled until the result has been rendered.
Imports accumulate in the current session; closing the app discards in-memory data.

For **Ask**, enter a natural-language question in the Trend / question field. For
**Report**, enter an exact trend name and optional category/dates. Results include a
Report / AI tab with calculated evidence and a separate AI interpretation. `./scripts/test.sh` includes headless Swing interaction checks and writes
an ignored preview image to `build/gui-preview.png`.

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
rates account for elapsed days. See [formulas and limitations](docs/stage2-plan.md#analysis-formulas-and-current-verification).

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

## Stage 2 records

- [Implementation milestones and feature/pattern status](docs/stage2-plan.md)
- [AI collaboration log](docs/ai-collaboration.md)
- [Weekly commit summary](docs/weekly-commits.md)
- [Stage 2 report draft](stage2-report.md)

The final submission also needs `stage2-report.pdf`, updated diagrams, a weekly
commit summary, and accessible demonstration videos for every implemented feature.
