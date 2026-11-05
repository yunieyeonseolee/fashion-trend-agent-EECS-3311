# Implemented architecture

This diagram describes the implemented GUI/CLI and LLM integration. Live API
verification is pending; automated API checks use a local fake server. A shared response carries data and diagnostics so each
interface can choose its own rendering without repeating application workflows.

```mermaid
classDiagram
    class TrendCLI {
        +parseCommand(args, agent) Command
        +display(response)
    }
    class AgentController {
        +execute(command) AgentResponse
    }
    class Command {
        <<interface>>
        +execute() AgentResponse
    }
    class AgentCommand {
        <<abstract>>
        #FashionTrendAgent receiver
        #AgentRequest request
    }
    class FashionTrendAgent {
        +run(request) AgentResponse
    }
    class TrendDataImporter {
        <<interface>>
        +importData(path) ImportResult
    }
    class TrendRankingStrategy {
        <<interface>>
        +rank(analyses) List
    }
    TrendGUI --> AgentController : submits in SwingWorker
    TrendGUI --> Command : constructs
    TrendGUI --> AgentResponse : renders
    TrendGUI --> HistoryChart
    HistoryChart --> TrendSignal : plots
    TrendCLI --> AgentController : submits
    TrendCLI --> Command : constructs
    AgentController --> Command : invokes
    Command <|.. AgentCommand
    AgentCommand <|-- ImportCommand
    AgentCommand <|-- AnalyzeCommand
    AgentCommand <|-- DiscoverCommand
    AgentCommand <|-- CompareCommand
    AgentCommand <|-- ReportCommand
    FashionTrendAgent --> AgentWorkflow
    AgentWorkflow --> AgentPlanner
    AgentWorkflow --> ToolManager
    AgentWorkflow --> EvidenceBundle
    AgentWorkflow --> TrendReport
    AgentWorkflow --> LLMClient
    AgentPlanner --> LLMClient
    LLMClient <|.. OpenAILLMClient
    ToolManager --> FashionTrendAgent : runDeterministic only
    ToolManager --> TrendRepository : source labels
    TrendReport --> EvidenceBundle
    AgentCommand --> AgentRequest
    AgentCommand --> FashionTrendAgent : receiver
    FashionTrendAgent --> AgentResponse : returns
    FashionTrendAgent --> TrendRepository
    FashionTrendAgent --> TrendAnalyzer
    TrendRepository --> TrendDataImporter
    TrendDataImporter <|.. CsvTrendAdapter
    CsvTrendAdapter --> CsvReader
    CsvTrendAdapter --> ImportResult
    ImportResult --> TrendSignal
    TrendRepository --> TrendSignal
    TrendAnalyzer --> TrendAnalysis
    TrendAnalyzer --> TrendRankingStrategy
    TrendRankingStrategy <|.. MomentumRankingStrategy
    TrendRankingStrategy <|.. GrowthRankingStrategy
    AgentResponse --> TrendSignal
    AgentResponse --> TrendAnalysis
```

Example sequence after CSV import (F06/F07):

```mermaid
sequenceDiagram
    actor User
    participant CLI as TrendCLI
    participant Controller as AgentController
    participant Command as DiscoverCommand
    participant Agent as FashionTrendAgent
    participant Repository as TrendRepository
    participant Analyzer as TrendAnalyzer
    participant Strategy as TrendRankingStrategy
    User->>CLI: discover category, ranking
    CLI->>Controller: execute(command)
    Controller->>Command: execute()
    Command->>Agent: run(request)
    Agent->>Repository: findByCategory(category)
    loop Each trend in category
        Agent->>Repository: getSignals(name, category, dates)
        Agent->>Analyzer: calculateMomentum(signals)
    end
    Agent->>Analyzer: rank(analyses) or detectMicrotrends(analyses)
    Analyzer->>Strategy: rank(eligible analyses)
    Strategy-->>Agent: ranked results through analyzer
    Agent-->>CLI: AgentResponse through command/controller
    CLI-->>User: results and warnings
```

Expected I/O and request errors are converted by the controller into unsuccessful
responses. Sparse data is explicit rather than fabricated: rankings exclude it
with warnings, and comparisons retain whatever observations can be analyzed.

Natural-language question sequence (F01):

```mermaid
sequenceDiagram
    actor User
    participant View as GUI or CLI
    participant Agent as FashionTrendAgent / AgentWorkflow
    participant Planner as AgentPlanner
    participant LLM as LLMClient
    participant Tools as ToolManager
    User->>View: fashion question
    View->>Agent: AnalyzeCommand through controller
    Agent->>Planner: createPlan(question)
    Planner->>LLM: catalog + question + plan schema
    LLM-->>Planner: typed plan or clarification
    Planner->>Planner: validate all names, operations, dates, step count
    Planner-->>Agent: validated plan
    loop Each read-only step (max 4)
        Agent->>Tools: invoke(request)
        Tools-->>Agent: deterministic result and sources
    end
    Agent->>LLM: synthesize EvidenceBundle
    LLM-->>Agent: interpretation or API failure
    Agent-->>View: evidence + separate narrative / explicit fallback
    View-->>User: report and warnings
```

For a known-trend report (F10), the agent builds history, analysis and category
retrieval steps directly; it does not need a model planning call. Failed synthesis
leaves the calculated report available. Clarifications and invalid plans do not
execute tools. API text never mutates repository data or numerical metrics.
