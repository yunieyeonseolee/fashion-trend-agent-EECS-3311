# Stage 2 recording and manual verification guide

Record the running application, not these instructions or static screenshots.
Use one combined video with labeled segments or separate videos. Add accessible
links and actual start timestamps to Section 7 of stage2-report.md, then rebuild
the PDF. All ten original features must be shown; do not count shared controls as
extra features. These steps are a plan, not a claim that videos already exist.

## Before recording

1. Read the code and report, especially the five pattern roles and metric formulas.
2. Run `./scripts/test.sh` and launch `./scripts/gui.sh data/sample-trends.csv`.
3. Confirm that the actual window/file picker works, results are legible and report
   export works on your desktop. Automated Swing tests do not replace this check.
4. Keep .env, password managers, API-key pages and terminal secret commands off screen.
   A configured local key is needed for the real Ask/Report demonstrations.
5. State that the included observations are synthetic and scores are heuristic.
   Subsequent model calls can incur charges; record runtime usage separately from
   Codex development usage. Do not present the test-double narrative as live AI.
6. Use a clean export filename in a folder you can show safely. Delete no existing
   user files merely to demonstrate export.

## Suggested segment order and inputs

### F03 — Import and normalization

Show Import CSV selecting `data/sample-trends.csv` in a fresh GUI session. Expected:
12 accepted observations. Reimport in the same session to show duplicates skipped.
For invalid input, prepare the temporary fixture below and run the CLI import.
Explain CsvReader → CsvTrendAdapter → TrendRepository and the Adapter roles.

```bash
mkdir -p build/demo
cat > build/demo/invalid-rows.csv <<'CSV'
trend,category,date,popularity,source
Demo Trend,footwear,2026-08-01,10,synthetic-demo
Demo Trend,footwear,2026-09-01,20,synthetic-demo
Demo Trend,footwear,2026-10-01,30,synthetic-demo
Demo Trend,footwear,2026-10-01,30,synthetic-demo
Bad Date,footwear,2026-02-30,20,synthetic-demo
Bad Value,footwear,2026-10-01,101,synthetic-demo
CSV
./scripts/run.sh import build/demo/invalid-rows.csv
```

Expected: 3 observations accepted and 3 warnings (duplicate, invalid date, out-of-range
popularity). Keep this fixture separate from the main GUI dataset so it does not
change the later discovery results.

### F02 — Search and retrieval

GUI: Search `mary`; select the result and click View history. Explain normalized
name matching and exact-name historical retrieval. Search `no such trend` to show
an empty result. CLI: `./scripts/run.sh search data/sample-trends.csv mary`.

### F09 — History and visualization

GUI: History, trend `mary jane flats`, no category/date filters. Show the table and
History chart tabs. Set From `2026-09-01` and To `2026-10-01`; show the two remaining
points. Explain that missing dates are not invented and same-date observations are
averaged in the chart. Show matching CLI output:

```bash
./scripts/run.sh history data/sample-trends.csv 'mary jane flats' footwear 2026-09-01 2026-10-01
```

Clear the date fields before the remaining GUI demonstrations.

### F04 — Momentum

GUI Analyze `mary jane flats`; expected momentum about 34.39, short growth about
36.87%/30 days, long growth about 61.96%/30 days, acceleration about -13.13. Explain
that numbers are calculated in Java. Filter to October–November to show unavailable
metrics with fewer than three dates, then clear filters.

### F05 — Lifecycle

Show Analyze results for `mary jane flats` (Growing), `chunky sneakers` (Declining)
and `bag charms` (Emerging). The sparse-date example produces Unknown. Explain that
fixed rules assign the stage; AI can explain it through Ask/Report but cannot modify
it. Mention the heuristic limits instead of claiming market validation.

### F07 — Category discovery

GUI Discover, category `footwear`, Rank by Momentum and then Growth. Both may order
this particular sample identically; do not claim different ranking outcomes just
because the selected strategy changed. Show an unsupported category error with
`unknown`. Select a result to show follow-up analysis navigation.

### F06 — Emerging microtrends

GUI Emerging, category `accessories`: expected `bag charms`. Category `footwear`
has no qualifying sample microtrends. Explain the low-popularity/high-growth filter.
CLI: `./scripts/run.sh emerging data/sample-trends.csv accessories`.

### F08 — Comparison

GUI Compare `mary jane flats` with `chunky sneakers`. Show both metrics and lifecycle
labels. Replace the second name with `unknown` to show a partial comparison.
Demonstrate Compare with selection filling the second field from a discovery row.

```bash
./scripts/run.sh compare data/sample-trends.csv 'mary jane flats' 'chunky sneakers'
```

### F01 — Natural-language question (real API)

GUI Ask: `How are mary jane flats changing over time?` Show the Report / AI tab,
executed steps, observations, metrics, source labels and separate interpretation.
The chosen plan and wording can vary, so wait for actual completion. Explain that
the model selects a validated plan and Java executes local tools before synthesis.
Try a vague question and explain any clarification/validation result honestly.

```bash
./scripts/run.sh ask data/sample-trends.csv 'What footwear trends are emerging?'
```

An empty emerging result is legitimate; do not replace it with a fabricated trend.

### F10 — Report and export (real API)

GUI Report `mary jane flats`: show HISTORY, ANALYZE and DISCOVER evidence, related
chunky sneakers results, synthetic source labels and model interpretation. Save
report to a new `.txt` filename; open it and show that evidence and warnings survive.
Try saving again to that exact name to demonstrate overwrite rejection. Export
itself makes no new model request.

```bash
./scripts/run.sh report data/sample-trends.csv 'mary jane flats'
```

To show deterministic fallback without exposing or editing the real key, run:

```bash
LLM_API_KEY=YOUR_LOCAL_API_KEY_HERE ./scripts/run.sh report data/sample-trends.csv 'mary jane flats'
```

The placeholder environment override causes no paid request, leaves the actual
.env untouched, and produces an explicit missing-AI warning plus numeric evidence.

## Finish the submission

- Copy each video's accessible link and actual starting timestamp into the report.
- Verify access using a browser session without your recording-service login.
- Confirm coding-model metadata and usage/cost records; otherwise label unavailable
  values honestly and resolve any instructor expectations before submitting.
- Rebuild stage2-report.pdf and verify all links/figures, then remove the review-draft
  status only when the missing materials and student review are complete.
- Push the final PDF, source, README and demo links; submit the repository URL.

## Alternate Atelier UI (November 7)

Launch `./scripts/studio.sh data/sample-trends.csv` to demonstrate the new view.
Explore covers F02/F04/F05/F09; Discover covers F06/F07; Compare covers F08;
AI reports covers F01/F10; sidebar Import CSV covers F03. Use the same feature
inputs and expected results described above. The original GUI remains available.
For F10, show both AI interpretation and Evidence & sources, then Save report.
Review the native dialogs/window before recording.
