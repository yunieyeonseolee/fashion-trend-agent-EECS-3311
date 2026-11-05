# Fashion Trend Intelligence Agent — EECS 3311

Java implementation of the [Stage 1 design](Stage%201/Stage1_Design_Report_220518676.pdf).
Stage 2 is due November 10, 2026.

## Current implementation

The first runnable milestone provides CSV import/normalization (F03), trend search
and filtered retrieval (F02), deterministic analysis and discovery/comparison (F04–F08),
and the CLI portion of history (F09). The GUI, shared command/controller workflow,
agent planning and LLM integration are **not implemented yet**. See the
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
GUI launch instructions will be added when the GUI exists.

## Stage 2 records

- [Implementation milestones and feature/pattern status](docs/stage2-plan.md)
- [AI collaboration log](docs/ai-collaboration.md)

The final submission also needs `stage2-report.pdf`, updated diagrams, a weekly
commit summary, and accessible demonstration videos for every implemented feature.
