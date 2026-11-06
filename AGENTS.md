# Project working instructions

## Weekly commit records

The user requested on November 5, 2026 that implementation work be recorded from
now on for the Stage 2 weekly commit summary. The first two periods were missed;
record that honestly and never backdate or manufacture commits.

- At meaningful implementation/test milestones, create descriptive commits and
  push to the existing course repository, unless the user directs otherwise.
- Keep `docs/weekly-commits.md` and the weekly summary in `stage2-report.md` in sync.
  Record the full commit link, Toronto-local date, concrete change, verification,
  and whether the push succeeded. Never label a local-only commit as pushed.
- Count only meaningful implementation/test commits. Documentation-only updates
  to the log/report do not count. Do not artificially split trivial changes.
- A commit cannot contain its own final hash. After creating the implementation
  commit, record its hash in a separate documentation commit. Do not amend the
  referenced commit after recording its hash.
- The reporting periods are October 20–26, October 27–November 2, and November 3–9,
  2026, inclusive in America/Toronto. Each requires five meaningful code commits.
  Keep later work separate; do not count November 10 toward the third period.
- Keep credentials and local build artifacts out of all commits.

`stage2-report.md` and `stage2-report.pdf` are review drafts. Rebuild the PDF after
report changes using `scripts/build-report.py` (see `docs/report-building.md`).
Video links, student review and unavailable usage records still need attention.
