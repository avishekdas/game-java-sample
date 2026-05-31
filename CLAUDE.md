# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Confidence Gate (read before any implementation work)

**Do not write or modify code until you have ≥95% confidence that you understand the task, the relevant design constraints, and the expected outcome.** If confidence is below that bar, **stop and ask clarifying questions** — do not guess, do not "make a reasonable assumption and proceed," do not scaffold a placeholder hoping it will be corrected later.

Concretely, before any `Write` or `Edit` to source code, you must be able to answer all of the following from the docs or by asking:

1. **Which milestone in `plan.md §6` does this work belong to?** Skipping ahead breaks the TDD acceptance gates.
2. **Which failing test will this code make pass?** Per `plan.md §3`, no implementation lands without a red test first.
3. **Which decision in `idea.md §12` or `design.md` constrains the shape of this code?** If the answer is "none that I can find," that itself is a signal to ask — these docs are dense and a missed constraint is the most common failure mode here.
4. **What does the AP CS rubric (`idea.md §9`) require this code to demonstrate?** A refactor that erases an Inheritance/ArrayList/Loop demonstration is a regression even if the tests pass.
5. **Is there an existing pattern in the codebase (or design doc) for this kind of change?** If yes, follow it; if no, confirm the new pattern with the user before introducing it.

If any answer is uncertain, ask. A clarifying question costs one round-trip; a wrong implementation costs a rollback, a stale test, and erodes trust in the design docs as the source of truth.

**This gate applies to all code-producing work, including:** new entities, new endpoints, new DTOs, new Swing components, new tests, edits to `world.json`, edits to `application.properties`, edits to `pom.xml`. It does **not** apply to read-only exploration, doc edits, or answering questions about the design.

---

## Repository Status

**Design-stage repo — no Java source exists yet.** Only design documents are checked in (`idea.md`, `design.md`, `plan.md`, `README.md`, plus a project proposal PDF). The first implementation milestone (`M0` in `plan.md`) is to scaffold the Maven multi-module project. Any "where is X?" lookup in source code will fail until that scaffolding lands — start from the design docs instead.

## Documentation Hierarchy (read in this order)

These three documents are the source of truth. When they disagree, the precedence below applies. Do not re-litigate decisions already recorded in `idea.md §12` — they are mentor-approved.

1. **`idea.md`** — *what* and _why_: vision, scope, theme, AP CS rubric mapping, resolved architectural decisions (§12), MVP build order. Authoritative on **intent**.
2. **`design.md`** — _how_: full DDL (§3), `world.json` schema and content (§4), DTO definitions (§5), REST reference (§6), backend/frontend class catalogs (§7–8), sequence diagrams (§9), error model (§11), config files (§13), acceptance checklist (§20). Authoritative on **mechanics**.
3. **`plan.md`** — _execution order_: TDD-driven milestone sequence (M0–M14), per-milestone tests-first/implementation/acceptance gates. Authoritative on **build order**.

`README.md` is the operator quickstart and troubleshooting reference for once the code exists.

## Project Identity & Constraints

- **Pedagogical project** — AP Computer Science final project for student Abhishri Das. Every architectural choice is constrained by the AP CS rubric (Classes & Objects, Inheritance, ArrayLists, Loops, Conditionals, File I/O, GUI). The rubric-coverage matrix in `idea.md §9` is load-bearing — do not refactor away the demonstrations it depends on.
- **Complexity ceiling deliberately held at AP CS level.** Reject suggestions that introduce concepts beyond the rubric (e.g., reactive streams, Kotlin coroutines, dependency injection frameworks beyond Spring's built-in, advanced concurrency). When in doubt, choose the more pedagogically transparent option.
- **Java 17 + Spring Boot 3.x.** Not negotiable for Phase 1.

## High-Level Architecture

Two-process system connected by HTTP/JSON. The Swing client holds **zero game logic** — it only renders state and posts player actions. The Spring Boot backend owns all rules and persistence.

```
Swing JVM (frontend/)  ── HTTP/JSON :8080 ──▶  Spring Boot JVM (backend/)  ──JPA──▶  H2 file DB (./data/)
                                                       │
                                                       └──Jackson──▶  ./saves/*.json
```

- **Stateless HTTP per request.** No cookies, no HTTP session. The `gameId` (UUID) in the URL path is the only identity token. Every endpoint returns the full `GameStateDTO` — never a delta.
- **`Puzzle` inheritance hierarchy** is the canonical Inheritance rubric demonstration. JPA strategy is `@Inheritance(strategy = InheritanceType.JOINED)` — one parent `PUZZLE` table plus four child tables (`COMBINATION_PUZZLE`, `RIDDLE_PUZZLE`, `SEQUENCE_PUZZLE`, `ITEM_USE_PUZZLE`). Chosen specifically so the hierarchy is visible at the database level for grading discussion.
- **World data (Rooms, Puzzles, InventoryItems) is seeded from `backend/src/main/resources/world.json`** by `WorldSeedService` on `@PostConstruct`. The seeder only writes if the DB is empty. To pick up edits to `world.json`, **delete `./data/` first** and restart. (Common gotcha.)
- **Save snapshots contain mutable session state only** (`gameId`, `currentRoomId`, `status`, timestamps, `solvedPuzzleIds`, `heldItemIds`). Immutable world data is never serialized into saves — it is reseeded from `world.json` at startup. Old saves remain compatible with edited `world.json` as long as referenced IDs still exist.

## Critical Invariants (do not violate)

- **No `javax.swing`, AWT, or any UI type may appear in a DTO, service, controller, or repository class.** The HTTP/JSON boundary is the entire frontend contract; any client that can speak JSON (e.g., a future React SPA) must be a drop-in replacement with zero backend changes.
- **DTO classes are intentionally duplicated between `backend/` and `frontend/`.** Do not create a `shared-dto` Maven module — that decision is recorded in `idea.md §8`. Keep them identical by hand during Phase 1.
- **`GameApiClient.send()` runs synchronously on the Swing EDT in Phase 1.** Do not introduce `SwingWorker` or async patterns — concurrency is out of AP CS scope and localhost latency makes it imperceptible. `SwingWorker` is Phase 2 backlog (`idea.md §11`).
- **Win condition is computed server-side in `GameSessionService.buildStateDTO()`** — never in the client. The client only reacts to `gameStatus == "COMPLETE"`.
- **H2 is in single-writer file mode** (`jdbc:h2:file:./data/escaperoom`). Two concurrent backend processes will collide on the OS file lock — see `README.md` troubleshooting before suggesting `rm` on lock files.

## Common Commands (once scaffolding exists)

All commands run from repo root. The project is a Maven multi-module build (parent POM + `backend/` + `frontend/`).

```
mvn clean install                                                  # builds both modules + runs all tests
mvn -pl backend spring-boot:run                                    # start backend on 127.0.0.1:8080
mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp   # start Swing client (backend must be up first)
mvn -pl backend test                                               # backend tests only
mvn -pl backend test -Dtest=PuzzleEvaluationServiceTest            # run a single test class
mvn -pl backend test -Dtest=PuzzleEvaluationServiceTest#combinationPuzzle_correctCode_marksSolved   # single test method
curl http://127.0.0.1:8080/api/health                              # backend smoke check
```

- H2 console (when backend is running): `http://127.0.0.1:8080/h2-console`. JDBC URL `jdbc:h2:file:./data/escaperoom`, user `sa`, blank password.
- Golden-path manual demo script lives at `backend/scripts/golden-path-curl.sh` (created in milestone M8).

## TDD Discipline (per `plan.md §3`)

`plan.md` mandates a red→green→refactor cycle for every milestone. Each milestone in `plan.md §6` lists its tests **before** its implementation files. Do not skip ahead to implementation — every change must trace to a failing test first, and every milestone has an acceptance gate at the bottom that must pass before the next one starts.

Test placement:
- Backend unit tests: `backend/src/test/java/...` mirroring the `main` package layout.
- Backend integration tests: same tree, suffix `IntegrationTest` (uses `@SpringBootTest`).
- Frontend tests: `frontend/src/test/java/...` — JUnit + AssertJ; Swing components driven via `SwingUtilities.invokeAndWait`.

## Where Files Will Live (per `idea.md §8`)

```
game-java-dev/
├── backend/                     Spring Boot REST API, JPA entities, world seeder, save/load
│   └── src/main/resources/
│       ├── application.properties   (full content in design.md §13a)
│       └── world.json               (full content in design.md §4b)
├── frontend/                    Swing client, GameApiClient (java.net.http), AssetManager interface + impls
├── data/                        H2 file DB (runtime, gitignored)
├── saves/                       JSON snapshots (runtime, gitignored)
└── logs/                        Logback output (runtime, gitignored)
```

The backend package root is `com.abhishri.escape`; the frontend package root is `com.abhishri.escape.ui`.
