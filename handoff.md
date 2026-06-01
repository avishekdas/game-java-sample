# Session Handoff — Mystery Escape Room (Season 3)

> Read this before touching any code. It covers project identity, environment quirks,
> completed milestones, and exactly where to pick up next.

---

## 1. Project Identity

**AP Computer Science final project** for student Abhishri Das.
Pedagogical constraint: every architectural choice must demonstrably cover the AP CS rubric
(Classes & Objects, Inheritance, ArrayLists, Loops, Conditionals, File I/O, GUI).
**Do not introduce concepts beyond AP CS scope** (no reactive streams, no Kotlin, no DI frameworks
beyond Spring's built-in, no async/SwingWorker in Phase 1).

**Theme:** "The Vanishing Librarian" — player explores Thornwick Municipal Library, solves 6 puzzles,
wins by transmitting evidence via pneumatic-tube terminal.

---

## 2. Source-of-Truth Documents (read in this order)

| File | Authoritative on |
|------|-----------------|
| `idea.md` | Intent, scope, AP CS rubric matrix (§9), resolved decisions (§12) |
| `design.md` | Mechanics: DDL (§3), world.json schema+content (§4), DTOs (§5), REST API (§6), class catalogs (§7–8), sequence diagrams (§9), error model (§11), config (§13), acceptance checklist (§20) |
| `plan.md` | Execution order: TDD milestones M0–M14, per-milestone Red/Green/Acceptance gates |
| `README.md` | Operator quickstart (once code exists) |

**CLAUDE.md** has the confidence gate — do not write code until ≥95% confidence.

---

## 3. Architecture (locked, do not re-litigate)

```
Swing JVM (frontend/)  ── HTTP/JSON :8080 ──▶  Spring Boot JVM (backend/)  ──JPA──▶  H2 file DB
```

- **No game logic in the Swing client.** It renders state and posts actions only.
- **Every endpoint returns the full `GameStateDTO`** — never a delta.
- **`gameId` UUID in the URL path** is the only session token (no HTTP session, no cookies).
- **`Puzzle` JOINED inheritance** (`@Inheritance(strategy = JOINED)`) — canonical AP CS Inheritance demo.
  Four child tables visible in H2: `COMBINATION_PUZZLE`, `RIDDLE_PUZZLE`, `SEQUENCE_PUZZLE`, `ITEM_USE_PUZZLE`.
- **`@Embeddable RoomObject`** on `Room.objects` — schema: `ROOM_OBJECTS` table with columns
  `OBJECT_ID`, `OBJECT_LABEL`, `OBJECT_TYPE`, `PUZZLE_ID`, `PICKUP_ITEM_ID`, `INTERACTABLE`.
- **`jakarta.persistence.*` everywhere** — NOT `javax.*`. Spring Boot 3.x / Jakarta EE 9+.
- **Synchronous `HttpClient.send()` on Swing EDT in Phase 1** — no SwingWorker (deferred to Phase 2).
- **DTO duplication** between `backend/dto/` and `frontend/api/dto/` is intentional. No shared module.
- **UUIDs set explicitly** in service code (`UUID.randomUUID()`), not via `@GeneratedValue`.
- **`PuzzleEvaluationService` depends on `GameSessionService`** (calls `buildStateDTO`). No circular
  dependency — `GameSessionService` does not depend on `PuzzleEvaluationService`.

---

## 4. Environment Setup

### Java
```bash
# Java 17 (Temurin) is installed; default JVM is Java 11 (Corretto). Always set JAVA_HOME:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

### Maven
```bash
# Sandbox blocks writes to ~/.m2 — use --offline for all builds after initial download.
# Use 'test' goal (not 'install') — install writes to ~/.m2.
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn --offline clean test

# If a NEW dependency is added to pom.xml, user must run once WITHOUT --offline:
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn clean test
# After that, --offline works again.
```

### Running the backend manually
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
  mvn -pl backend spring-boot:run
# H2 console: http://127.0.0.1:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/escaperoom  user: sa  password: (blank)
# Health: curl http://127.0.0.1:8080/api/health
```

---

## 5. Milestone Status

| Milestone | Status | Key outputs |
|-----------|--------|-------------|
| M0 — Scaffolding | ✅ DONE | Parent POM, backend/frontend POMs, SanityTests, .gitignore |
| M1 — Backend Boot + `/api/health` | ✅ DONE | `EscapeRoomApplication`, `HealthController`, `application.properties` |
| M2 — Domain Entities & Repositories | ✅ DONE | 10 entities, 5 repos, 17 H2 tables, `@OrderColumn` canary test |
| M3 — World Seeding | ✅ DONE | Seed POJOs + `@JsonTypeInfo`, `WorldSeedService`, `WorldSeedValidator`, full `world.json` |
| M4 — Game Session Lifecycle | ✅ DONE | `GameStateDTO` + all DTOs, `GameSessionService`, `InventoryService`, `GameController`, `BackendConfig`, `GlobalExceptionHandler` |
| M5 — Navigation + Examination | ✅ DONE | `POST /move`, `POST /examine`, 409 on invalid; Conditionals rubric |
| M6 — Inventory + Pickup | ✅ DONE | `POST /pickup`, `InventoryService.addItem/hasItem/removeItem`, 409 on non-pickupable/already held |
| M7 — Puzzle Evaluation: Combo + Riddle | ✅ DONE | `PuzzleEvaluationService`, `POST /attempt-puzzle`, prereq check, idempotent reward |
| M8 — Sequence + ItemUse + Win ★ | ✅ DONE | `POST /use-item`, win condition, 75 backend tests |
| M9 — Save / Load | ✅ DONE | `SaveLoadService`, File I/O rubric, 86 backend tests |
| M10 — Swing Skeleton | ✅ DONE | `EscapeRoomApp`, `MainFrame` (4 panels), `ScenePanel`, `AssetManager`, 88 total tests |
| M11 — GameApiClient + Hotspot Clicks | ✅ DONE | All HTTP methods, `MouseListener`, `applyState`, 94 total tests |
| **M12 — Puzzle Dialogs** | ⬅ **NEXT** | `PuzzleDialog` subclasses (Combination, Riddle, Sequence, ItemUse) |
| M13–M14 — Swing Frontend | pending | M13: north-star end-to-end clicks ★; M14: polish |

**Test count as of M11:** 86 backend tests, 8 frontend tests. All green.

---

## 6. Current Codebase Layout

```
backend/src/main/java/com/abhishri/escape/
├── EscapeRoomApplication.java
├── config/
│   ├── BackendConfig.java              (CORS, ObjectMapper bean)
│   └── seed/
│       ├── WorldSeed.java + RoomSeed.java + ItemSeed.java + RoomObjectSeed.java
│       ├── PuzzleSeed.java             (@JsonTypeInfo + @JsonSubTypes)
│       └── CombinationPuzzleSeed, RiddlePuzzleSeed, SequencePuzzleSeed, ItemUsePuzzleSeed
├── controller/
│   ├── HealthController.java           GET /api/health
│   └── GameController.java             POST /new, GET /{id}, POST /{id}/move,
│                                       POST /{id}/examine, POST /{id}/pickup,
│                                       POST /{id}/attempt-puzzle, POST /{id}/use-item,
│                                       POST /{id}/save, POST /{id}/load, GET /{id}/saves
├── domain/
│   ├── GameSession.java, GameStatus.java, PlayerInventory.java
│   ├── Room.java, RoomObject.java (@Embeddable), ObjectType.java (enum), InventoryItem.java
│   └── puzzle/
│       ├── Puzzle.java                 (abstract, @Inheritance JOINED)
│       ├── CombinationPuzzle.java      attempt(): checks inputs.get("code")
│       ├── RiddlePuzzle.java           attempt(): trims, respects caseSensitive
│       ├── SequencePuzzle.java         attempt(): loop over expectedSequence (M8)
│       └── ItemUsePuzzle.java          attempt(): checks requiredItemId + targetObjectId (M8)
├── dto/
│   ├── GameStateDTO.java, RoomDTO.java, RoomObjectDTO.java, InventoryItemDTO.java
│   ├── LastActionResult.java (enum), ErrorResponseDTO.java
│   ├── MoveRequest.java, ExamineRequest.java, PickupRequest.java
│   ├── AttemptPuzzleRequest.java
│   ├── UseItemRequest.java
│   ├── LoadRequest.java, SaveConfirmationDTO.java, SaveMetadataDTO.java
│   └── GameSnapshotDTO.java
├── exception/
│   ├── ApiErrorCode.java (enum), GlobalExceptionHandler.java
│   ├── GameNotFoundException.java, InvalidMoveException.java
│   ├── PuzzleNotFoundException.java, PrerequisiteNotMetException.java
│   ├── ItemNotInInventoryException.java
│   └── SaveLoadException.java
├── repository/
│   └── GameSessionRepository, RoomRepository, PuzzleRepository,
│       PlayerInventoryRepository, InventoryItemRepository
└── service/
    ├── GameSessionService.java         createNewGame, getState, buildStateDTO,
    │                                   move, examine, pickup
    ├── InventoryService.java           snapshot, addItem, hasItem, removeItem
    ├─�� PuzzleEvaluationService.java    attempt, useItem
    ├── SaveLoadService.java            save, load, listSaves
    └── WorldSeedService.java + WorldSeedValidator.java

backend/src/main/resources/
├── application.properties
└── world.json                          (full Thornwick: 3 rooms, 6 puzzles, 5 items)

backend/src/test/resources/
├── application-test.properties         (in-memory H2, world-test.json, starting-room=test_room)
├── world-test.json                     (3 rooms, 5 puzzles, 3 items — extended through M8)
└── world-broken.json                   (referential integrity failure fixture)

frontend/src/main/java/com/abhishri/escape/ui/
├── dto/
│   ├── GameStateDTO.java, RoomDTO.java, InventoryItemDTO.java, RoomObjectDTO.java
│   ├── LastActionResult.java (enum), GameStatus.java (enum), ObjectType.java (enum)
├── AssetManager.java              (interface: getBackground, getItemIcon)
├── PlaceholderAssetManager.java   (BufferedImage colored rects — no PNG required)
├── Hotspot.java                   (POJO: id, type, label, bounds, objectId)
├── ScenePanel.java                (extends JPanel, paintComponent, List<Hotspot>)
├── InventoryPanel.java            (JList<InventoryItemDTO>, DefaultListModel)
├── DialoguePanel.java             (JTextArea in JScrollPane, non-editable)
├── StatusBar.java                 (JLabel + Save/Load/New JButtons, no listeners yet)
├── GameApiClient.java             (stub: constructor + fields only — M11 adds methods)
├── MainFrame.java                 (BorderLayout: StatusBar N, ScenePanel C, Inventory E, Dialogue S)
└── EscapeRoomApp.java             (main() — SwingUtilities.invokeLater)
```

---

## 7. What M12 Must Build

**Goal:** Clicking a hotspot whose `type == "PUZZLE"` opens a modal dialog matching the puzzle type. Solving or failing the puzzle dispatches `attemptPuzzle` or `useItem` and applies the returned state.

**Red tests first:**

- `CombinationPuzzleDialogTest` — constructs dialog with a stub `GameApiClient`, types "123" in the code field, clicks OK → `attemptPuzzle` is called with `{"code":"123"}`
- `RiddlePuzzleDialogTest` — same shape for the riddle answer field
- `PuzzleDialogFactoryTest` — `PuzzleDialogFactory.forObject(roomObjectDTO, client, gameId)` returns the correct subclass based on `objectType`

**Green:**

- `PuzzleDialog` (abstract `JDialog`) — `puzzleId`, OK/Cancel buttons, abstract `buildInputPanel()`, `collectInputs() → Map<String,String>`
- `CombinationPuzzleDialog extends PuzzleDialog` — single `JTextField` for code, `collectInputs` returns `{"code": value}`
- `RiddlePuzzleDialog extends PuzzleDialog` — single `JTextField` for answer, `collectInputs` returns `{"answer": value}`
- `SequencePuzzleDialog extends PuzzleDialog` — ordered list of inputs (e.g. 3 JTextFields), `collectInputs` returns `{"step0":..., "step1":..., "step2":...}` or `{"sequence":"a,b,c"}`
- `ItemUsePuzzleDialog` — not a dialog; handled by `handleHotspotClick` dispatching `client.useItem()` directly (item selected from inventory, no modal needed)
- `PuzzleDialogFactory` — static factory: checks `RoomObjectDTO.objectType` and `puzzleId` prefix, returns correct dialog subclass
- `MainFrame.handleHotspotClick` — updated to open the dialog for PUZZLE type

**Important notes:**
- `PuzzleDialog` extends `JDialog`, constructed with `this` (MainFrame) as owner → modal by default.
- `PuzzleDialog` subclasses are the canonical **Inheritance** rubric demo on the frontend side.
- Backend `AttemptPuzzleRequest` expects `{ "puzzleId": "...", "inputs": { ... } }` — the dialog fills `inputs`.
- For `SEQUENCE` puzzles, backend expects `inputs.sequence` as a comma-separated string matching `expectedSequence` order — check `design.md §6` for exact contract.
- `ItemUsePuzzle` in the backend is triggered via `POST /use-item` with `{ "itemId": "...", "targetObjectId": "..." }`. Hotspot click with type `PUZZLE` + an item already in inventory should dispatch `useItem`. The dialog may be skipped if the required item is already held; show a selector if multiple items are held.

**Rubric:** Inheritance — `PuzzleDialog` → 3+ concrete subclasses = AP CS Inheritance demonstration on the GUI side.

---

## 8. Key Gotchas / Lessons Learned

| # | Gotcha | Fix |
|---|--------|-----|
| 1 | `mvn install` writes to `~/.m2` — sandbox blocks it | Use `mvn --offline clean test` |
| 2 | Default JVM is Java 11 — Spring Boot 3.x needs Java 17 | Always set `JAVA_HOME` |
| 3 | Spring Boot `repackage` goal needs a main class | Skip with `<skip>true</skip>` until M1; removed after |
| 4 | Spring 6.x `@PathVariable` needs `-parameters` flag or explicit name | Added `-parameters` to compiler + explicit names |
| 5 | `@ElementCollection List<String>` ordering undefined without `@OrderColumn` | `SequencePuzzle.expectedSequence` has `@OrderColumn(name = "POSITION")` |
| 6 | `@OneToOne` (GameSession→PlayerInventory) insertion order | `createNewGame()` sets UUIDs on both, saves session (cascade handles inventory) |
| 7 | `Instant` in response DTO needs `JavaTimeModule` — risky | `ErrorResponseDTO.timestamp` is `String` (ISO-8601 via `Instant.now().toString()`) |
| 8 | `@GeneratedValue(UUID)` not simulated by Mockito `save()` mock | UUIDs set explicitly in service (`UUID.randomUUID()`) — no `@GeneratedValue` |
| 9 | `@DataJpaTest` tests pick up all `@SpringBootTest` classes for context sharing | Use `@ActiveProfiles("test")` consistently; `application-test.properties` isolates |
| 10 | `world.json` used `objectIds` (strings); replaced with `objects` (RoomObjectSeed) | `RoomSeed` now has `List<RoomObjectSeed> objects`; `Room` entity has `List<RoomObject> objects` |
| 11 | Lambda captures variable that is later reassigned — compile error | Use a new variable for the saved result: `GameSession saved = repo.save(session)` |
| 12 | `UnnecessaryStubbing` with Mockito STRICT_STUBS | Put stubs that are test-specific into individual test methods, not `@BeforeEach` |
| 13 | `WorldSeedServiceTest` count assertions break when `world-test.json` is extended | Update count expectations every time test fixture gains new entities |

---

## 9. Git Remote

```
git remote: git@github.com:avishekdas/game-java-sample.git
branch: main
last commit: feat(M11): GameApiClient HTTP methods, ScenePanel MouseListener, MainFrame.applyState
```

---

## 10. AP CS Rubric Coverage So Far

| Concept | First landed | Where |
|---------|-------------|-------|
| Classes & Objects | M2 | Every entity in `com.abhishri.escape.domain` |
| Inheritance | M2 + M7 | `Puzzle` → 4 subclasses, JOINED in H2; polymorphic `attempt()` dispatched live in M7 |
| ArrayLists | M2 | `solvedPuzzleIds`, `heldItemIds`, `connectedRoomIds`, `expectedSequence` |
| Loops | M8 | `SequencePuzzle.attempt()` — iterates `expectedSequence` to verify submitted order |
| Conditionals | M5 | Room adjacency check in `GameSessionService.move()` |
| File I/O | M3 + M9 | M3: `WorldSeedService` reads `world.json`; M9: `SaveLoadService` writes/reads `GameSnapshotDTO` JSON files |
| GUI | M10 | `MainFrame` (BorderLayout, 4 panels), `ScenePanel` (`paintComponent`, `List<Hotspot>`), `StatusBar` (3 JButtons), `AssetManager` interface + `PlaceholderAssetManager` |
