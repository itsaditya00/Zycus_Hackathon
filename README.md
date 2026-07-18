# ZipLine - Zycus Hackathon Solution 🚀

An intelligent, reactive automated order routing and agentic reassignment engine featuring dynamic, context-aware dispatching optimization via AI and rule-based pipelines.

## 📁 Project Structure
*   `hackathon-backend/` — Java / Spring Boot 3.x backend engine managing transaction domain modeling, routing context switches, and event listening.
*   `hackathon-ui/` — Angular web client constructed with TypeScript for interactive dashboard tracking, live status polling, and agent-service operations.
*   `ADR.md` — Architecture Decision Record listing engineering structural summaries.

---------

## ⚙️ Backend Module (`hackathon-backend`)

The brain of the platform, leveraging Spring Boot to implement an event-driven system supporting multiple order-routing algorithms and reactive replanning handlers.

### 🛠️ Architecture Highlights
*   **Strategy-Driven Dispatch:** Built using a decoupled `RoutingEngineContext` swapping between `RuleBasedRoutingStrategy` and `AIRoutingStrategy` connected via an external `LlmGateway`.
*   **Event-Driven Replanning:** Uses an `AgenticReplanListener` listening for dynamic workflow updates like `AgentOfflineEvent` to recalculate reassignment strategies transparently.
*   **Domain Models Managed:** Agents (`AgentStatus`), Orders (`OrderStatus`), and suggestions (`ReassignmentSuggestion`, `SuggestionStatus`, `TriggerReason`).

----

## ⚙️ Frontend Module (`hackathon-ui`)

The interactive face of the platform, leveraging Angular to deliver a reactive, real-time dashboard that tracks automated routing performance and agent dispatch telemetry.

### 🛠️ Architecture Highlights
*   **RxJS-Driven State Management:** Built using reactive streams to handle live status polling, asynchronous data pipelines, and UI state synchronization seamlessly.
*   **Centralized Data Service:** Utilizes a unified HackathonService to abstract HTTP communication, mapping backend API responses directly to client-side components.
*   **Component-Driven Modular Design:** Structured with decoupled layers separating core configurations, app architectures, styling metrics, and environment specifications.
