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
- **`@Embeddable RoomObject`** on `Room.objects` (option 1, chosen this session) — replaces the
  original `ROOM_OBJECT_IDS` plain-string table. Schema: `ROOM_OBJECTS` with columns
  `OBJECT_ID`, `OBJECT_LABEL`, `OBJECT_TYPE`, `PUZZLE_ID`, `PICKUP_ITEM_ID`, `INTERACTABLE`.
- **`jakarta.persistence.*` everywhere** — NOT `javax.*`. Spring Boot 3.x / Jakarta EE 9+.
- **Synchronous `HttpClient.send()` on Swing EDT in Phase 1** — no SwingWorker (deferred to Phase 2).
- **DTO duplication** between `backend/dto/` and `frontend/api/dto/` is intentional. No shared module.
- **UUIDs set explicitly** in service code (`UUID.randomUUID()`), not via `@GeneratedValue`.

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
| **M5 — Navigation + Examination** | ⬅ **NEXT** | `POST /move`, `POST /examine`, 409 on invalid move/object |
| M6 — Inventory + Pickup | pending | `POST /pickup`, `InventoryService.addItem/hasItem` |
| M7 — Puzzle Evaluation: Combo + Riddle | pending | `PuzzleEvaluationService`, `POST /attempt-puzzle` |
| M8 — Sequence + ItemUse + Win ★ | pending | North-star: full game winnable via curl |
| M9 — Save / Load | pending | `SaveLoadService`, File I/O rubric demo |
| M10–M14 — Swing Frontend | pending | M10: skeleton; M13: north-star clicks ★ |

**Test count as of M4:** 27 backend tests, 1 frontend sanity test. All green.

---

## 6. Current Codebase Layout

```
backend/src/main/java/com/abhishri/escape/
├── EscapeRoomApplication.java
├── config/
│   ├── BackendConfig.java              (CORS, ObjectMapper bean)
│   └── seed/
│       ├── WorldSeed.java + RoomSeed.java + ItemSeed.java
│       ├── PuzzleSeed.java             (@JsonTypeInfo + @JsonSubTypes)
│       └── CombinationPuzzleSeed, RiddlePuzzleSeed, SequencePuzzleSeed, ItemUsePuzzleSeed
│       └── RoomObjectSeed.java
├── controller/
│   ├── HealthController.java           GET /api/health
│   └── GameController.java             POST /api/game/new, GET /api/game/{gameId}
├── domain/
│   ├── GameSession.java, GameStatus.java, PlayerInventory.java
│   ├── Room.java, RoomObject.java (@Embeddable), ObjectType.java (enum), InventoryItem.java
│   └── puzzle/
│       ├── Puzzle.java                 (abstract, @Inheritance JOINED)
│       ├── CombinationPuzzle.java, RiddlePuzzle.java, SequencePuzzle.java, ItemUsePuzzle.java
├── dto/
│   ├── GameStateDTO.java, RoomDTO.java, RoomObjectDTO.java, InventoryItemDTO.java
│   ├── LastActionResult.java (enum), ErrorResponseDTO.java
│   └── [request DTOs added per milestone: MoveRequest, ExamineRequest, etc.]
├── exception/
│   ├── ApiErrorCode.java (enum), GlobalExceptionHandler.java
│   ├── GameNotFoundException.java, InvalidMoveException.java
│   └── [more added per milestone]
├── repository/
│   └── GameSessionRepository, RoomRepository, PuzzleRepository,
│       PlayerInventoryRepository, InventoryItemRepository
└── service/
    ├── GameSessionService.java         createNewGame, getState, buildStateDTO
    ├── InventoryService.java           snapshot (M4), addItem/hasItem (M6)
    ├── WorldSeedService.java + WorldSeedValidator.java
    └── [PuzzleEvaluationService, SaveLoadService added in M7–M9]

backend/src/main/resources/
├── application.properties
└── world.json                          (full Thornwick: 3 rooms, 6 puzzles, 5 items)

backend/src/test/resources/
├── application-test.properties         (in-memory H2, world-test.json, starting-room=test_room)
├── world-test.json                     (1 room, 1 puzzle, 1 item)
└── world-broken.json                   (referential integrity failure fixture)

frontend/  — empty stubs only (M10+)
```

---

## 7. What M5 Must Build

**Red tests first (per plan.md §M5):**
- `MoveValidationTest` — service unit: move to adjacent room updates `currentRoomId`;
  move to non-adjacent throws `InvalidMoveException`
- `MoveIntegrationTest` — foyer→reading_hall returns 200; foyer→archives returns 409 `INVALID_MOVE`
- `ExamineIntegrationTest` — examine `wall_clock` in foyer returns 200 with `dialogueMessage`;
  examine `display_case` while in foyer returns 409 (object not in room)
- `ExamineMissingObjectTest` — empty body returns 400 `INVALID_REQUEST`

**Green:**
- `MoveRequest` DTO (`@NotBlank targetRoomId`)
- `ExamineRequest` DTO (`@NotBlank objectId`)
- `GameSessionService.move(UUID, MoveRequest)` — uses `room.isConnectedTo()` conditional
- `GameSessionService.examine(UUID, ExamineRequest)` — uses `room.containsObject()` conditional
- Wire `@PostMapping("/{gameId}/move")` and `@PostMapping("/{gameId}/examine")` in `GameController`
- Add `MethodArgumentNotValidException` handler to `GlobalExceptionHandler` (400)

**⚠ Plan gap already resolved:** Use `room.containsObject(objectId)` (method added to `Room` in M4)
for the object-in-room check. The `RoomObject` embeddable has full metadata. Examine returns
`RoomObject.label` + room description as the `dialogueMessage`.

**Rubric:** Conditionals concept demonstrated by the adjacency check in `move()`.

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

---

## 9. Git Remote

```
git remote: git@github.com:avishekdas/game-java-sample.git
branch: main
last commit: feat(M4): game session lifecycle
```

---

## 10. AP CS Rubric Coverage So Far

| Concept | First landed | Where |
|---------|-------------|-------|
| Classes & Objects | M2 | Every entity |
| Inheritance | M2 | `Puzzle` → 4 subclasses, JOINED in H2 |
| ArrayLists | M2 | `solvedPuzzleIds`, `heldItemIds`, `connectedRoomIds`, `expectedSequence` |
| Loops | M8 (pending) | `SequencePuzzle.attempt()` — iterate expected vs submitted |
| Conditionals | **M5 (next)** | Room adjacency in `GameSessionService.move()` |
| File I/O | M3 + M9 | M3: `WorldSeedService` reads `world.json`; M9: `SaveLoadService` writes JSON snapshots |
| GUI | M10–M14 (pending) | `MainFrame`, `ScenePanel`, all `PuzzleDialog` subclasses |
