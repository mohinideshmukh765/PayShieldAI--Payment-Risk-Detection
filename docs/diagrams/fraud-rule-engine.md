# Fraud Rule Engine

```mermaid
flowchart TD

    A[Transaction / Payment] --> B[Build Fraud Rule Context]

    B --> C[Fraud Rule Engine]

    C --> D[Large Transaction Rule]
    C --> E[High Velocity Rule]
    C --> F[Unusual Amount Rule]
    C --> G[Account Activity Rule]
    C --> H[Destination Risk Rule]

    D --> I[Rule Results]
    E --> I
    F --> I
    G --> I
    H --> I

    I --> J[Calculate Risk Points]

    J --> K[Later: Combine With ML Signals]

    K --> L[Risk Engine]