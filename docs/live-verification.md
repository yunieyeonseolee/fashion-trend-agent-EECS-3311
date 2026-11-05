# Live API verification — November 5, 2026

The user configured credentials locally and authorized a live check. The key was
not displayed or committed. Configured runtime model: `gpt-6-luna`.

Both CLI commands exited successfully with actual model-generated interpretation,
not the numeric-only fallback:

```bash
./scripts/run.sh ask data/sample-trends.csv "How are mary jane flats changing over time?"
./scripts/run.sh report data/sample-trends.csv "mary jane flats"
```

| Call | Input tokens | Output tokens | Result |
| --- | --- | --- | --- |
| Ask planning | 368 | 136 | Valid HISTORY and ANALYZE plan |
| Ask synthesis | 551 | 411 | Explanation based on executed tools |
| Report synthesis | 816 | 568 | History, analysis and category evidence interpreted |
| Total | 1,735 | 1,115 | 2,850 reported tokens across three API calls |

The report included related chunky sneakers data. Both explanations identified
synthetic-example as fabricated test data, reproduced the supplied metrics and
cited tool steps/source labels. The Ask explanation loosely described negative
acceleration as easing momentum; the underlying metric is change in growth rate,
so prose should still be reviewed against the definitions.

Monetary cost was not fetched from billing; no exact charge is claimed. These are
application API tokens, separate from coding-assistant usage.

Scope: two live CLI workflows on synthetic sample data. This establishes that the
configured credentials/model and current API request format work. It is not broad
agent validation, a real-data evaluation, or a manual desktop GUI/file-picker test.
The GUI shares the tested application workflow and has separate automated Swing
coverage. Credentials remain in ignored `.env`.

No implementation code changed for this verification. Its documentation commit
must not count toward the weekly meaningful-code minimum.
