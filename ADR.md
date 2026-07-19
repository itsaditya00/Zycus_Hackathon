# Architecture Decision Records (ADRs) - Delivery Optimization Console

This document tracks the core architectural decisions made for the hackathon-backend and hackathon-ui systems.

---

## ADR-001: Where Routing Logic Lives (Separation of Core Logic and Orchestration)

### Context
We needed to decide where the delivery routing logic, entity updates, and persistence boundaries live. Allowing a single service or domain class to balloon into handling algorithm evaluation, network API gateways, event processing, and database persistence all at once is a major design smell that violates the Single Responsibility Principle (SRP) and limits unit testability.

### Decision
We implemented the **Application Service Layer Pattern** combined with a dedicated **Strategy Engine Interface Contract** (`RoutingStrategy`)[cite: 14]. 

* **Strategy Interface (`RoutingStrategy`):** Acts as a pure computational interface layer[cite: 14]. It takes immutable snapshots (the order, a list of available agents, and a trigger reason) and maps them cleanly into routing recommendations without modifying database states or firing events directly[cite: 14, 25].
* **Application Orchestration Services (`ReassignmentService` / `AgenticReplanListener`):** Handle transaction scoping (`@Transactional`), resource fetching via repositories, and orchestrating database state transitions (such as modifying agent workload counters).
* **UI Isolation Boundary:** The frontend angular codebase abstracts these operations into a standalone data handling layer (`HackathonService`), keeping UI presentation separate from backend wire protocols.

### Consequences
* **Pros:** Highly testable and modular architecture. Routing heuristics can be unit-tested cleanly in isolation without mocking heavy database transactions or application event publishers.
* **Cons:** Increases total class file counts across core project packages.

---

## ADR-002: Mechanism for Runtime Strategy Switching

### Context
The application must dynamically change how it assigns orders on the fly at runtime without needing a application restart, code modification, or redeployment. This switching logic must seamlessly support both direct client-driven HTTP execution parameters and decoupled background automated recovery events.

### Decision
We implemented a dynamic **Strategy Pattern** managed via a thread-safe registry component (`RoutingEngineContext`)[cite: 13].

* **Strategy Registry:** Strategies are detected as Spring components and loaded into a `ConcurrentHashMap` inside `RoutingEngineContext` at application boot time.
* **Dynamic Resolution Overrides:** The registry exposes an evaluation handler (`executeStrategyWithOverride`) to service layers[cite: 13, 25].
    * **HTTP On-Demand Path:** The `OrderController` intercepts an optional query param (`@RequestParam(required = false) String strategy`)[cite: 28]. If it is omitted or blank, the service gracefully falls back to the globally active configuration name[cite: 13, 25].
    * **Async Event Re-Plan Path:** The background listener invokes `engineContext.getActiveStrategy()`, ensuring that automated agent recovery flows instantly respect the current global system configuration.
* **Sprint 2 Extensibility:** A third strategy plugs in cleanly by simply creating a new class implementing `RoutingStrategy` with a distinct `@Component("NEW_STRATEGY")` annotation[cite: 14, 25]. The registry automatically registers it at boot time without requiring any changes to existing services[cite: 13, 25].

### Consequences
* **Pros:** Strategy switching is hot-swappable at zero cost to uptime. Open extension is fully realized.
* **Cons:** Invalid runtime text overrides require fallback safety logic to avoid null pointer errors[cite: 13, 25].

---

## ADR-003: Graceful Degradation and System Health when LLM Fails

### Context
Large Language Model (LLM) execution introduces unpredictable operational risks: network request timeouts, rate-limit quota exhaustion, malformed or unparseable JSON token responses, and hallucinated agent identification IDs that do not exist in the database roster[cite: 12, 16]. The backend must maintain continuous uptime despite these failures.

### Decision
We implemented a multi-tiered degradation defense system inside `AIRoutingStrategy` and `LlmGateway`[cite: 25, 26]:

1. **Network Egress Guard Timeouts:** The underlying `RestClient` is configured with explicit connection (3s) and read execution (7s) timeouts via `SimpleClientHttpRequestFactory` to prevent slow LLM endpoints from freezing core backend execution pools[cite: 26].
2. **Malformed JSON Failures:** All responses are wrapped inside direct `try-catch` exception handling blocks in `LlmGateway` during object mapper evaluation[cite: 26]. Any unparseable string formatting immediately throws a runtime parsing exception rather than floating bad data[cite: 26].
3. **Hallucination Verification Layer:** The returned agent string identifier (`recommendedAgentId`) is strictly validated against the actual list of available agents retrieved from the database[cite: 25]. If the AI returns a hallucinated ID, the system blocks it, logs a warning, and rolls back to rule-based metrics[cite: 25].
4. **No Asynchronous Silent Drops:** In the async agent offline recovery loop, if the active strategy throws an error (due to timeout or quota limits), a `try-catch` wrapper inside the loop catches it and instantly runs `ruleBasedFallback`[cite: 27]. The recommendation is still written to the database with altered reason text so operations can act upon it[cite: 27].

### Consequences
* **Pros:** Complete tolerance against external system dependency downtime. Faults are contained cleanly.
* **Cons:** Failed AI suggestions fall back to standard rule calculations, which yield baseline confidence values in the UI tables[cite: 15].

---

## ADR-004: Asynchronous Agentic Loop Decoupling

### Context
When an agent goes offline, the system must capture this status change and process re-planning options across all stranded orders. However, calculating routes—especially via an LLM—takes too long to block a user request. The `PATCH /agents/{id}/status` endpoint must return a response to the client immediately.

### Decision
We used a fully decoupled **Domain Event Architecture** leveraging Spring's native `ApplicationEventPublisher`, `@EventListener`, and `@Async` handlers[cite: 17, 27].

* When a status request transitions an agent to offline, `AgentService` modifies the state, emits an `AgentOfflineEvent`, and immediately terminates its database transaction[cite: 17]. The controller returns a `200 OK` response instantly[cite: 16, 17].
* The `AgenticReplanListener` interceptor processes the event asynchronously in a separate worker thread pool completely off the web server request path[cite: 27].
* **UI Refresh Synchronization:** The async loop loops through all affected orders, checks for existing pending duplicates to avoid double-processing, and saves suggestions straight to the data layer[cite: 27]. Because the Angular application relies on polling filters through `fetchFilteredOrders()`, these new records surface automatically on the next UI screen poll without blocking the frontend application state machine[cite: 32].

### Consequences
* **Pros:** High application throughput and zero response latency for user operations.
* **Cons:** Since operations are asynchronous, the immediate response of the PATCH request cannot guarantee that background routing is complete, requiring loading indicators on the frontend data dashboard.

---

## ADR-005: Extension Seams and Prioritized Intentional Deferrals

### Context
We must maintain clear abstraction boundaries so that upcoming sprint requirements can be introduced with zero regression footprint, while explicitly documenting what we chose not to build to ensure core architecture correctness first.

### Decision

#### 1. Extension Seam: Sprint 2 Geographic Proximity Strategy
Our primary extension seam is the uniform interface contract `RoutingStrategy` combined with Spring's component injection registry in `RoutingEngineContext`[cite: 13, 14]. To introduce a new proximity strategy in Sprint 2, developers simply implement a new bean class matching the contract:

```java
@Component("GEOGRAPHIC_PROXIMITY")
public class GeographicRoutingStrategy implements RoutingStrategy {
    @Override
    public ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason) {
        // Compute geo coordinates relative to order destinations here
    }
    @Override
    public String getStrategyName() { return "GEOGRAPHIC_PROXIMITY"; }
}