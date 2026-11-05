# Implemented architecture

This diagram describes current deterministic functionality, not the still-planned
GUI or LLM integration. A shared response carries data and diagnostics so each
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
    TrendCLI --> AgentController : submits
    TrendCLI --> Command : constructs
    AgentController --> Command : invokes
    Command <|.. AgentCommand
    AgentCommand <|-- ImportCommand
    AgentCommand <|-- AnalyzeCommand
    AgentCommand <|-- DiscoverCommand
    AgentCommand <|-- CompareCommand
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
