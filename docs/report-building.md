# Rebuilding the implementation report

Edit `stage2-report.md`, then regenerate the committed `stage2-report.pdf`:

```bash
python3 -m venv build/report-venv
build/report-venv/bin/pip install -r scripts/report-requirements.txt
build/report-venv/bin/python scripts/build-report.py
```

Run these commands from the repository root. Python dependencies are documentation
tools only; the Java application does not use them. Inspect the PDF after changes,
especially table wrapping, diagrams, links and page breaks. The PDF remains a
review draft until the missing video links and student review are completed.

The builder uses committed PNG figures from `docs/diagrams/`. Their editable
Mermaid sources are beside them. To rerender figures, install the Mermaid CLI
locally under ignored `build/` and run it for each changed source:

```bash
npm install --prefix build/mermaid-tools @mermaid-js/mermaid-cli@12.2.0
build/mermaid-tools/node_modules/.bin/mmdc -i docs/diagrams/class-ui.mmd -o docs/diagrams/class-ui.png -b white -s 2 -c docs/diagrams/config.json
```

Method labels in the figures are abbreviated for readability; Section 2 of the
report gives the implementation responsibilities and method mappings.
