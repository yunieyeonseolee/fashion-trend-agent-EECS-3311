# Fashion Trend Intelligence Agent — EECS 3311

Java implementation of the [Stage 1 design](Stage%201/Stage1_Design_Report_220518676.pdf).
Stage 2 is due November 10, 2026.

## Current implementation

The first runnable milestone provides CSV import/normalization (F03), trend search
and filtered retrieval (F02), deterministic analysis and discovery/comparison (F04–F08),
and history visualization (F09). The Swing GUI and CLI use the same shared controller
and facade. Agent planning and LLM integration are **not implemented yet**. See the
[implementation plan and feature mapping](docs/stage2-plan.md).

## Prerequisites and running

Install JDK 17 or newer and use Bash (macOS/Linux, or Git Bash/WSL on Windows).
There are no third-party dependencies in this milestone. The scripts compile Java
sources before running; Maven is not required.

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
History, Analyze, Discover, Emerging or Compare. Enter the trend/category required
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

The GUI has no AI query/report buttons yet; those will be added with working LLM
integration. `./scripts/test.sh` includes headless Swing interaction checks and writes
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
`AnalyzeCommand`, `DiscoverCommand` or `CompareCommand`, and submits it through
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

## Local API configuration (future milestone)

```bash
cp .env.example .env
```

When LLM integration is implemented, enter your API key locally and choose a model
ID supported by your provider. The current CLI does not read `.env` or call an LLM.
Never commit credentials; local `.env` variants and `config.local.json` are ignored.
The desktop GUI does not call an LLM yet either.

## Stage 2 records

- [Implementation milestones and feature/pattern status](docs/stage2-plan.md)
- [AI collaboration log](docs/ai-collaboration.md)
- [Weekly commit summary](docs/weekly-commits.md)
- [Stage 2 report draft](stage2-report.md)

The final submission also needs `stage2-report.pdf`, updated diagrams, a weekly
commit summary, and accessible demonstration videos for every implemented feature.
