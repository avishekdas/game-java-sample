# Mystery Escape Room (Season 3) — `plan.md`

> Companion to `idea.md` (what & why) and `design.md` (how). This document is the **execution plan**: a TDD-driven, milestone-based sequence of work that converts the design into shipped Phase 1 code. Every milestone produces a testable build slice; every step traces back to the **north star** below. No milestone is finished until its acceptance gate passes.

---

## 1. North Star

Phase 1 is "done" when **every checkbox** in `design.md §20` is green and **the golden path** in `idea.md §3` plays end-to-end through the Swing UI.

Restated as a single sentence:

> A player launches the Swing app, the foyer renders, they click through three rooms solving six puzzles, the win screen fires, and they can save mid-game and resume after a backend restart — all backed by a Spring Boot 3 + JPA + H2 + Jackson backend on `localhost:8080` that they can also drive end-to-end with `curl` alone.

This north star sits in front of every milestone. At every decision point — what to build next, what tests to write, what to defer — the question is: **does this advance us toward that sentence, or are we yak-shaving?**

The full §20 acceptance checklist is reproduced at the end of this document (Section 8) with a milestone tag on each box.

---

## 2. Guiding Principles

1. **Test-first, always.** No production class is written before the test that asserts what it must do. Red → Green → Refactor, every cycle. The TDD doctrine is in Section 3.
2. **Milestones ship a slice, not a layer.** Each milestone produces something a human can **see** working — a passing test, a `curl` response, a window on screen, a clicked button. We do not spend three weeks building "the data layer" with nothing to show.
3. **North-star alignment, every milestone.** Each milestone declares which §20 acceptance boxes it closes and which `idea.md §3` walkthrough steps it enables. If a milestone closes no boxes and enables no steps, it does not exist.
4. **Done = automated + manual.** A milestone is complete when (a) all new tests pass, (b) all prior tests still pass, and (c) the milestone-specific **manual demo** produces the documented result.
5. **No new architecture decisions.** All architecture is locked in `idea.md §12` and `design.md`. Plan.md only sequences the work. If a question surfaces, it's a flag for the mentor, not an in-flight pivot.
6. **Fail loud, fail early.** Prefer crashes at boot over silent corruption. The seed validator (M3), the @OrderColumn check (M2), and the schemaVersion guard (M9) are all examples of "loud failures we want."

---

## 3. TDD Workflow

### 3a. The core loop

For every behavior we add:

1. **Red** — Write a test that captures the behavior. Run it. It must fail (compile error counts; assertion failure preferred). If the test passes on first run, the test is wrong.
2. **Green** — Write the minimum production code that makes the test pass. Resist the urge to add "obvious next" code. If you wrote a class to handle one case, the next test forces the next case.
3. **Refactor** — Tidy with the tests as a safety net. Rename, extract methods, remove duplication. Run all tests after each refactor. Stop when the code is clean *and* every test still passes.

Then repeat for the next behavior.

### 3b. Granularity

Tests are small. A test asserts **one behavior**:

- ✅ `combinationPuzzle_correctCode_returnsTrue`
- ❌ `combinationPuzzle_works`

A class typically has one test class per behavior cluster (`PuzzleAttemptTests`, `InventoryServiceTests`, `GameFlowIntegrationTest`). Test counts in `design.md §16` are floors, not ceilings — if a behavior surfaces during Green that wasn't tested, write the test and add it.

### 3c. Minimum-viable Red (entity edge case)

Strict TDD says "write the test first." For JPA entities, the test (`@DataJpaTest`) won't even *compile* until the entity class and its fields exist. The pragmatic interpretation:

1. **Create the entity class as an empty stub** (`@Entity public class Foo { @Id private UUID id; }` — fields not yet added).
2. **Write the failing test** asserting the round-trip of all expected fields. The test fails to compile against the stub, *or* compiles and fails at assertion time once the missing fields are added as `null` defaults.
3. **Add fields one at a time**, each driven by a specific failing assertion, until the test passes.
4. **Refactor** (extract `@MappedSuperclass` for shared timestamps, etc.) with the test as a safety net.

The "empty stub" step is the minimum viable Red. Anything more than `class declaration + @Entity + @Id` is jumping ahead.

The same rule applies to Spring `@RestController`, `@Service`, `@Repository`, and DTOs: stub-then-test-then-flesh.

### 3d. Backend vs. frontend

**Backend (M0–M9): strict TDD.** Every service, every controller, every entity gets tested first. Spring Boot + JUnit 5 + MockMvc + `@DataJpaTest` + `@SpringBootTest` give us everything we need. No excuses.

**Frontend (M10–M14): pragmatic TDD.** Swing's mouse/render path is hard to unit-test rigorously. We test what is testable:

- `GameApiClient` (HTTP + Jackson) — strict TDD against a real running backend in a test fixture.
- `PuzzleDialog` input extraction — strict TDD; construct dialog, set widgets programmatically, assert `getInputs()` map.
- `AssetManager` implementations — strict TDD; assert non-null results, correct dimensions.
- `MainFrame` / panels / mouse dispatch — **smoke tests**: launch on EDT, assert key invariants (window visible, panel count, hotspot list size). Visual correctness is verified by the **manual demo** in each milestone's acceptance.

Pragmatic does not mean lazy. Every frontend milestone has at least one automated test plus a precise manual demo script.

### 3e. Test placement

```
backend/src/test/java/com/abhishri/escape/
    ├── PuzzleAttemptTests.java
    ├── InventoryServiceTests.java
    ├── GameSessionServiceTest.java
    ├── WorldSeedDeserializationTest.java
    ├── WorldSeedServiceTest.java
    ├── GameFlowIntegrationTest.java        # @SpringBootTest, golden path
    ├── SaveLoadIntegrationTest.java        # @SpringBootTest
    └── ... (one file per cluster)

frontend/src/test/java/com/abhishri/escape/ui/
    ├── GameApiClientTest.java              # against random-port backend
    ├── PuzzleDialogInputTest.java
    ├── AssetManagerTest.java
    └── MainFrameSmokeTest.java
```

### 3f. CI / pre-commit discipline

There is no CI server. Discipline is mentor-enforced at each milestone gate. Before declaring a milestone done:

```
mvn clean install      # from repo root; runs every test in both modules
```

must exit `0` with **zero failures, zero errors, zero skipped**.

---

## 4. Milestone Map

15 milestones. Backend through M9, frontend M10–M14. Two **NORTH STAR CHECKPOINTS** at M8 (game winnable via curl) and M13 (game winnable via clicks).

| #   | Name                                          | Output                                                                   |
|-----|-----------------------------------------------|--------------------------------------------------------------------------|
| M0  | Project Scaffolding & Tooling                 | `mvn clean install` succeeds on empty multi-module skeleton              |
| M1  | Backend Boot + `/api/health`                  | Spring Boot starts, health endpoint returns `{"status":"ok"}`            |
| M2  | Domain Entities & Repositories                | 16 H2 tables; @DataJpaTest round-trips for every entity                  |
| M3  | World Seeding from `world.json`               | Thornwick world loaded into H2 on `@PostConstruct`                       |
| M4  | Game Session Lifecycle                        | `POST /api/game/new` + `GET /api/game/{id}` work                         |
| M5  | Navigation + Examination                      | `/move`, `/examine` work; 409 on invalid                                 |
| M6  | Inventory + Pickup                            | `/pickup` works; inventory persists                                      |
| M7  | Puzzle Evaluation: Combination + Riddle       | First 2 puzzle types solvable via curl                                   |
| M8  | Sequence + ItemUse + Win Condition            | **NORTH STAR CHECKPOINT**: full game winnable via curl                   |
| M9  | Save / Load (File I/O)                        | JSON snapshots, restart-safe                                             |
| M10 | Frontend Skeleton                             | Swing window opens with placeholder foyer                                |
| M11 | Frontend ↔ Backend Wiring                     | Foyer renders from real API response                                     |
| M12 | Hotspot Click → Examine / Move                | Navigate rooms by clicking                                               |
| M13 | Puzzle Dialogs + Win Screen                   | **NORTH STAR CHECKPOINT**: golden path playable end-to-end via clicks    |
| M14 | Save / Load UI + Phase 1 Acceptance           | §20 acceptance checklist fully green                                     |

---

## 5. Universal Definition of Done (every milestone)

Before a milestone is marked complete, all of these must be true:

- [ ] **All new tests pass.** Tests listed in the milestone's Red section exist and pass.
- [ ] **All prior tests still pass.** `mvn clean install` from repo root exits `0`.
- [ ] **Manual demo works.** The milestone's documented `curl` command, click sequence, or smoke command produces the documented result on a clean workstation.
- [ ] **No `TODO`/`FIXME` left in the changed code without a tracking note.** Stubs are explicit (`throw new UnsupportedOperationException("M{n}")`) and tracked.
- [ ] **No `javax.persistence`, `javax.validation`, or `javax.servlet` imports.** Boot 3 = `jakarta.*` (per `design.md §3c`).
- [ ] **Mentor reviewed the diff** and signed off in writing (Git commit acknowledgment or message reply).

---

## 6. Milestones

### M0 — Project Scaffolding & Tooling

**Goal.** A multi-module Maven skeleton that builds clean with `mvn clean install` and contains the shells of both modules.

**North-star tie.** Foundation for everything. Closes §20 box 1 ("`mvn clean install` builds both modules").

**Pre-conditions.** None — this is the first milestone.

**Red (write these tests first).**

- `backend/src/test/java/.../SanityTest.java` — single assertion: `assertEquals(2, 1 + 1)`. Verifies JUnit 5 actually wires up.
- `frontend/src/test/java/.../SanityTest.java` — same.

**Green (production code / artifacts).**

- Repo-root `pom.xml` (aggregator from `design.md §13d`).
- `backend/pom.xml` (from `design.md §13b`) — Spring Boot 3.2.5, JPA, H2, validation, Jackson, jsr310, starter-test.
- `frontend/pom.xml` (from `design.md §13c`) — Jackson, JUnit 5, exec-maven-plugin, shade-plugin.
- `.gitignore` (from `design.md §14c`).
- Empty source directories matching the tree in `design.md §14a`.
- `README.md` already exists from prior session — confirm it points at idea.md and design.md.

**Refactor.** None — code is too thin.

**Acceptance.**

- Automated: `mvn clean install` from repo root exits `0`. Both `SanityTest` classes report 1/1 passed.
- Manual demo: `cd <project-root> && mvn -pl backend test` shows `Tests run: 1, Failures: 0`.

**Risks / fallbacks.** None significant. If `maven-shade-plugin` ever conflicts with Spring Boot's `repackage` goal (it won't — they're in different modules), revisit per-module config.

---

### M1 — Backend Boot + `/api/health`

**Goal.** Spring Boot starts on port 8080 and serves a working health endpoint.

**North-star tie.** Foundation for every REST endpoint. Closes §20 box 3 ("`/api/health` returns 200"). First proof of life for the backend JVM.

**Pre-conditions.** M0 complete.

**Red.**

- `HealthControllerTest` (`@WebMvcTest(HealthController.class)`): `mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"))`.

**Green.**

- `EscapeRoomApplication.java` — `@SpringBootApplication` with `main`.
- `HealthController.java` — `@RestController`, single `@GetMapping("/api/health")` returning `Map.of("status","ok")`.
- `backend/src/main/resources/application.properties` from `design.md §13a`. Drop the JPA/H2/world keys for this milestone (they'll error on missing entities); add them back in M2. Minimum keys for M1: `spring.application.name`, `server.port=8080`, `server.address=127.0.0.1`, `logging.*`.

**Refactor.** None.

**Acceptance.**

- Automated: `HealthControllerTest` passes.
- Manual demo:
  ```
  mvn -pl backend spring-boot:run
  # in another terminal:
  curl http://127.0.0.1:8080/api/health
  # expect: {"status":"ok"}
  ```

**Risks / fallbacks.** If port 8080 is in use, fail fast and tell the user (`server.port` is explicit, no auto-pick). If logback can't write to `./logs/`, comment out `logging.file.name` for M1 and add it back in M2 after we know the dir is created.

---

### M2 — Domain Entities & Repositories

**Goal.** Every JPA entity in `design.md §3a` exists, persists, and round-trips through H2 via `@DataJpaTest`. 16 tables generated on first boot.

**North-star tie.** Every game state change persists through these entities. Closes §20 box 4 ("H2 console lists all 17 tables from §3a") and prepares ground for box 5 (seed counts). Demonstrates **Inheritance** (rubric concept) via the JOINED Puzzle hierarchy.

**Pre-conditions.** M1 complete.

**Red (write these first).**

- `GameSessionEntityTest` (`@DataJpaTest`): persist a `GameSession` with status `IN_PROGRESS`, current room `room_foyer`, empty solved-puzzle list, attached `PlayerInventory`. Flush, evict, reload by ID. Assert all fields match.
- `RoomEntityTest`: persist a `Room` with non-empty `connectedRoomIds`, `objectIds`, `puzzleIds`. Reload. Assert collections contain the same strings (order not asserted for these — only `SequencePuzzle.expectedSequence` cares about order).
- `PuzzleHierarchyTest`: persist one of each — `CombinationPuzzle`, `RiddlePuzzle`, `SequencePuzzle`, `ItemUsePuzzle`. Reload via `PuzzleRepository.findById(String)`. Assert `instanceof` matches the expected subclass and subclass-specific fields are populated. Confirms JOINED inheritance correctness.
- **`SequencePuzzleOrderTest`** (critical, per `design.md §19`): persist a `SequencePuzzle` whose `expectedSequence = ["510","520","610"]`. Reload. Assert `getExpectedSequence()` returns the list **in that exact order**. This test will fail if `@OrderColumn` is missing — it is the canary for the §19 ordering risk.
- `PlayerInventoryHeldItemsTest`: persist a `PlayerInventory` with `heldItemIds = ["desk_key","brass_magnifying_glass"]`. Reload. Assert both items present.
- `InventoryItemEntityTest`: persist + reload an `InventoryItem`.

**Green.**

All entities under `com.abhishri.escape.domain` using **`jakarta.persistence.*`** imports (per `design.md §3c`):

- `GameSession`, `GameStatus` enum, `PlayerInventory`, `Room`, `InventoryItem`.
- `Puzzle` (abstract, `@Inheritance(strategy = InheritanceType.JOINED)`).
- `CombinationPuzzle`, `RiddlePuzzle`, `SequencePuzzle`, `ItemUsePuzzle`.
- `SequencePuzzle.expectedSequence` annotated with `@OrderColumn(name = "POSITION")`.

All repositories under `com.abhishri.escape.repository` extending `JpaRepository`. `PuzzleRepository.findByRoomId(String)` for later use.

Re-enable JPA/H2 config keys in `application.properties` (full file per `design.md §13a` now).

**Refactor.** Extract `BaseEntity` with `@MappedSuperclass` only if it earns its keep (createdAt/updatedAt timestamps shared between Game-side mutable entities). If not, skip — premature abstraction.

**Acceptance.**

- Automated: all 6 entity tests pass. `mvn clean install` exits 0.
- Manual demo: `mvn -pl backend spring-boot:run`, browse `http://127.0.0.1:8080/h2-console`, log in, count tables — expect **17**: GAME_SESSION, GAME_SESSION_SOLVED_PUZZLES, PLAYER_INVENTORY, PLAYER_INVENTORY_HELD_ITEMS, ROOM, ROOM_CONNECTED_ROOMS, ROOM_OBJECT_IDS, ROOM_PUZZLE_IDS, INVENTORY_ITEM, PUZZLE, PUZZLE_PREREQUISITE_IDS, COMBINATION_PUZZLE, RIDDLE_PUZZLE, SEQUENCE_PUZZLE, SEQUENCE_PUZZLE_EXPECTED, SEQUENCE_PUZZLE_AVAILABLE, ITEM_USE_PUZZLE.
- Confirm `SEQUENCE_PUZZLE_EXPECTED` has a `POSITION` column.

**Risks / fallbacks.** If `@OneToOne` between `GameSession` and `PlayerInventory` causes a chicken/egg insertion failure, save the inventory first, then attach it to the session, then save the session (per `design.md §19` risk #4). If `JOINED` produces unexpected DDL, double-check `@DiscriminatorColumn` is **absent** (JOINED uses table-row presence, not discriminator). If a test fails due to lazy-loading after eviction, add `@Transactional` to the test class.

---

### M3 — World Seeding from `world.json`

**Goal.** The complete Thornwick Library world (3 rooms, 6 puzzles, 5 items) loads from `world.json` into H2 on backend startup. Editing `world.json` and restarting picks up the change.

**North-star tie.** The story content — the *actual game* — exists in the database. Closes §20 box 5 (3 rooms, 6 puzzles, 5 items) and box 12 (editing `world.json` is honored without recompile).

**Pre-conditions.** M2 complete.

**Red.**

- `WorldSeedDeserializationTest`: feed the full Thornwick `world.json` (read from classpath in test) to a Jackson `ObjectMapper` with polymorphic config. Assert deserialization produces 3 `RoomSeed`s, 5 `ItemSeed`s, 6 `PuzzleSeed`s with the correct subclass for each (`RiddlePuzzleSeed` for `puzzle_clock`, `CombinationPuzzleSeed` for `puzzle_display_case`, etc.).
- `WorldSeedServiceTest` (`@SpringBootTest`, `@ActiveProfiles("test")` with `escape.world.seed-file=classpath:world-test.json` pointing at a single-room test fixture): after context loads, assert `RoomRepository.count() == 1`, `PuzzleRepository.count() == 1`, `InventoryItemRepository.count() == 1`.
- `WorldSeedReferentialIntegrityTest`: feed a `world-broken.json` with a puzzle referencing a non-existent `roomId`. Assert context startup **fails loudly** with a clear error mentioning the bad reference (this is the "loud failure" risk #3 in `design.md §19`).
- `WorldSeedIdempotencyTest`: simulate two `@PostConstruct` runs (e.g., call `seedIfEmpty()` twice). Assert counts are stable — no duplicates inserted.

**Green.**

- Seed POJOs under `com.abhishri.escape.config.seed`: `WorldSeed`, `RoomSeed`, `ItemSeed`, abstract `PuzzleSeed` with `@JsonTypeInfo` + `@JsonSubTypes`, four concrete `*PuzzleSeed` subclasses. Code skeleton in `design.md §4c`.
- `WorldSeedService` in `com.abhishri.escape.service`: `@PostConstruct init()` that calls `seedIfEmpty()`. Reads file from `${escape.world.seed-file}` via `ResourceLoader`, deserializes, validates referential integrity, maps seed POJOs to JPA entities, saves via repositories.
- `backend/src/main/resources/world.json` — the **complete** Phase 1 content from `design.md §4b`.
- `backend/src/test/resources/world-test.json` — minimal 1-room/1-puzzle fixture.
- `backend/src/test/resources/world-broken.json` — same but with a bad reference, for the loud-failure test.
- `backend/src/test/resources/application-test.properties` — `spring.datasource.url=jdbc:h2:mem:test;...` (in-memory) + `escape.world.seed-file=classpath:world-test.json`.

**Refactor.** If validation logic accumulates (referential checks, cycle detection in prereqs, numeric `expectedCode` constraints), extract a `WorldSeedValidator` class. Keep `WorldSeedService` focused on orchestration.

**Acceptance.**

- Automated: all 4 seed tests pass.
- Manual demo: `mvn -pl backend spring-boot:run`, open H2 console, run `SELECT COUNT(*) FROM ROOM` (expect 3), `SELECT COUNT(*) FROM PUZZLE` (expect 6), `SELECT COUNT(*) FROM INVENTORY_ITEM` (expect 5). Run `SELECT * FROM SEQUENCE_PUZZLE_EXPECTED ORDER BY SEQUENCE_PUZZLE_ID, POSITION` and verify the Dewey numbers appear in `510, 520, 610, 621, 720, 810` order.
- Edit `world.json` — change `puzzle_clock.expectedAnswer` from `"11:47"` to `"11:48"`. Delete `./data/` (forces re-seed). Restart. Re-query H2 console — confirm the new value is present.

**Risks / fallbacks.** If Jackson polymorphism fails with a cryptic stack trace, the `@JsonSubTypes.Type(name = ...)` strings must match the `type` strings in JSON exactly (`COMBINATION`, `RIDDLE`, `SEQUENCE`, `ITEM_USE`) — case-sensitive per `design.md §19` risk #3. If `WorldSeedService` runs before the schema exists, ensure it depends on JPA (Spring Boot's lifecycle handles this when the service is annotated `@Service` — no manual ordering needed).

---

### M4 — Game Session Lifecycle

**Goal.** A new game can be started via `POST /api/game/new` and retrieved via `GET /api/game/{gameId}`. Unknown IDs return 404 with `GAME_NOT_FOUND`.

**North-star tie.** First state-changing endpoint. Enables `idea.md §3` step 1 ("Player launches the Swing application. The main window opens showing the Entry Foyer scene"). Foundation for every subsequent gameplay endpoint.

**Pre-conditions.** M3 complete.

**Red.**

- `GameSessionServiceTest` (unit, mocked repositories): `createNewGame()` returns a session with status `IN_PROGRESS`, `currentRoomId = "room_foyer"`, empty solvedPuzzleIds, empty inventory. Verifies `gameSessionRepository.save()` is called once.
- `GameStateDTOAssemblerTest`: given a saved `GameSession`, `buildStateDTO()` produces a fully populated `GameStateDTO` — `currentRoom` is the foyer with all `RoomObjectDTO`s, `inventory` is empty, `lastActionResult = NEW_GAME`.
- `NewGameIntegrationTest` (`@SpringBootTest`): POST `/api/game/new` returns 201 with a `GameStateDTO` body matching expectations (foyer, empty inventory, valid UUID, `IN_PROGRESS`).
- `GetStateIntegrationTest`: after creating a new game, GET `/api/game/{gameId}` returns the same state.
- `GameNotFoundIntegrationTest`: GET `/api/game/{random-uuid}` returns 404 with `errorCode = GAME_NOT_FOUND` and a populated `ErrorResponseDTO`.

**Green.**

- DTOs in `com.abhishri.escape.dto`: `GameStateDTO`, `RoomDTO`, `RoomObjectDTO`, `InventoryItemDTO`, `LastActionResult` enum, `ErrorResponseDTO`, `ApiErrorCode` enum.
- `GameSession.isComplete()` helper.
- `GameSessionService` with `createNewGame()`, `getState(UUID)`, `buildStateDTO(GameSession, String, LastActionResult)`.
- `GameController` with `@PostMapping("/api/game/new")` and `@GetMapping("/api/game/{gameId}")`.
- `GameNotFoundException` + `GlobalExceptionHandler` (start with just the one handler — extend as we add more exception types in later milestones).
- `BackendConfig` skeleton with the CORS bean and an `ObjectMapper` bean configured with `JavaTimeModule` and `WRITE_DATES_AS_TIMESTAMPS = false`.

**Refactor.** If `buildStateDTO` grows complex (mapping `Room` → `RoomDTO`, `InventoryItem` → `InventoryItemDTO`), extract small mapper helpers — but keep them in the service for now. No MapStruct or ModelMapper — manual mapping keeps it AP-CS-readable.

**Acceptance.**

- Automated: 5 new tests pass.
- Manual demo:
  ```
  curl -X POST http://127.0.0.1:8080/api/game/new
  # capture {gameId} from the response
  curl http://127.0.0.1:8080/api/game/{gameId}
  # expect identical state in both responses
  curl -i http://127.0.0.1:8080/api/game/00000000-0000-0000-0000-000000000000
  # expect 404 with errorCode "GAME_NOT_FOUND"
  ```

**Risks / fallbacks.** If `@OneToOne` insertion order trips on `createNewGame()` (per `design.md §19` risk #4), explicitly save `PlayerInventory` first, then attach to the new `GameSession`, then save the session. Add a code comment in `GameSessionService.createNewGame()` flagging this order.

---

### M5 — Navigation + Examination

**Goal.** `POST /api/game/{id}/move` traverses between adjacent rooms. `POST /api/game/{id}/examine` returns object flavor text. Illegal moves return 409 `INVALID_MOVE`.

**North-star tie.** Enables `idea.md §3` steps 2, 5, 9 (room transitions) and step 2 (examining the wall clock). Demonstrates the rubric **Conditional** concept in the room-adjacency check.

**Pre-conditions.** M4 complete.

**Red.**

- `MoveValidationTest` (service unit): `move()` to an adjacent room updates `currentRoomId`. Move to a non-adjacent room throws `InvalidMoveException`. Move with an unknown `targetRoomId` throws `InvalidMoveException`.
- `MoveIntegrationTest`: from foyer, POST `/move` with `targetRoomId=room_reading_hall` returns 200 with updated state. POST `/move` with `targetRoomId=room_archives` (not adjacent from foyer) returns 409 `INVALID_MOVE`.
- `ExamineIntegrationTest`: in foyer, POST `/examine` with `objectId=wall_clock` returns 200 with `dialogueMessage` containing the riddle prompt. POST `/examine` with `objectId=display_case` (not in foyer) returns 409 `INVALID_MOVE` (per fixed `design.md §6`).
- `ExamineMissingObjectTest`: POST `/examine` with empty body returns 400 `INVALID_REQUEST` (Spring's `@Valid` + `MethodArgumentNotValidException` handler).

**Green.**

- DTOs: `MoveRequest` (`@NotBlank targetRoomId`), `ExamineRequest` (`@NotBlank objectId`). Validation imports use `jakarta.validation.constraints.NotBlank`.
- `Room.isConnectedTo(String)` helper.
- `GameSessionService.move(UUID, MoveRequest)` and `examine(UUID, ExamineRequest)`.
- `InvalidMoveException` + handler in `GlobalExceptionHandler` mapping to 409.
- `MethodArgumentNotValidException` handler producing `ErrorResponseDTO` with 400 + `INVALID_REQUEST`.

> **⚠ PLAN GAP — mentor decision required before M5 begins.**
>
> `design.md §5` specifies `RoomObjectDTO` with fields `label`, `description`, `puzzleId`, `pickupItemId`, `objectType` — but `design.md §3a` only models a `ROOM_OBJECT_IDS` table with `(ROOM_ID, OBJECT_ID)`. **There is no schema for object metadata** (label, description, etc.). The examine endpoint cannot return per-object flavor text without one.
>
> Three options for resolution (mentor picks one, then update `design.md` before this milestone starts):
> 1. **Embeddable `RoomObject`** — replace `ROOM_OBJECT_IDS` with `ROOM_OBJECTS` table holding all metadata columns; `Room.objects` is `@ElementCollection List<RoomObject>` where `RoomObject` is `@Embeddable`. Cleanest, most JPA-idiomatic.
> 2. **Separate `RoomObject` entity** — full `@Entity` with FK to `Room`. More verbose but more flexible (e.g., per-object state in Phase 2).
> 3. **Out-of-band metadata cache** — keep `ROOM_OBJECT_IDS` as-is; load object metadata from `world.json` into an in-memory `Map<String, RoomObjectMetadata>` at startup; merge into DTOs at response-assembly time. Sidesteps schema entirely; fastest to ship.
>
> Recommended: **option 1** (`@Embeddable`). Smallest schema change, plays well with JPA, makes Phase 2 hotspot rendering trivial.
>
> Do not begin M5 until this is resolved. The same gap blocks M6 (pickup needs `pickupItemId`).

**Refactor.** Once the gap above is closed and metadata is queryable, update `world.json` to populate `label` + `description` for every clickable object across all 3 rooms. Add a tiny test that exercises the lookup.

**Acceptance.**

- Automated: 4 new tests pass plus all prior.
- Manual demo:
  ```
  # create new game, save gameId
  curl -X POST -H 'Content-Type: application/json' \
       -d '{"targetRoomId":"room_reading_hall"}' \
       http://127.0.0.1:8080/api/game/{gameId}/move
  # expect 200, currentRoom.name == "The Reading Hall"

  curl -X POST -H 'Content-Type: application/json' \
       -d '{"objectId":"display_case"}' \
       http://127.0.0.1:8080/api/game/{gameId}/examine
  # expect 200, dialogueMessage describing the case
  ```

**Risks / fallbacks.** If adding `objectDescriptions` to `Room` creates a JPA migration headache in the persistent H2 file, wipe `./data/` and restart. (DDL `update` is additive, but new `@ElementCollection` tables generate fresh; column-level changes can be sticky.)

---

### M6 — Inventory + Pickup

**Goal.** `POST /api/game/{id}/pickup` adds a pickupable item to the player's inventory. Duplicate pickup returns 409.

**North-star tie.** Inventory is one of the three core rubric features (rooms + inventory + puzzles). Phase 1 Thornwick uses puzzle-reward inventory for all items, but the pickup endpoint is part of the documented API surface and is exercised against test fixtures here.

**Pre-conditions.** M5 complete.

**Red.**

- `InventoryServiceTests` (from `design.md §16`): `addItem_appendsToHeldItemIds`, `addItem_duplicateRejected` (calling twice does not double-insert), `hasItem_trueWhenHeld`, `hasItem_falseWhenNotHeld`.
- `PickupIntegrationTest` (uses `world-test.json` variant with a pickupable item — extend the test fixture): POST `/pickup` with valid `objectId` adds the item; second call returns 409 `INVALID_MOVE` with message indicating already held.
- `PickupNonPickupableTest`: POST `/pickup` with `objectId=wall_clock` (which is `objectType=SCENERY`, not pickupable) returns 409.

**Green.**

- `InventoryService` with `addItem(GameSession, String)`, `hasItem(GameSession, String)`, `removeItem(GameSession, String)` (the last is for symmetry; not used in Phase 1 but cheap to add), `snapshot(GameSession)` returning the inventory DTO list.
- `PickupRequest` DTO with `@NotBlank objectId`.
- `GameSessionService.pickup(UUID, PickupRequest)` — consult the room's object metadata (resolved through the M5 design fix) for `pickupItemId`; if non-null and not already held, delegate to `InventoryService`. Otherwise throw `InvalidMoveException`.

> **⚠ PLAN GAP — same root cause as M5.** Pickup requires `RoomObject.pickupItemId` storage. Whatever solution the mentor chose for M5 (embeddable / separate entity / in-memory cache) carries the `pickupItemId` column/field too. No additional decision needed here — just ensure the M5 fix accommodates this field. Phase 1 Thornwick `world.json` keeps these all `null`; the test fixture sets one for the pickup test.

**Refactor.** `InventoryService.addItem` is idempotent — adding the same item twice is a service-level no-op success, not a 409. The 409 is raised by `pickup()` only after `InventoryService.hasItem()` returns true, before calling `addItem()`. Keep the boundary clear.

**Acceptance.**

- Automated: 4 inventory tests + 3 pickup tests pass.
- Manual demo: against the test profile only (Thornwick has no by-click pickups):
  ```
  # via test profile or by adjusting world-test.json
  curl -X POST -H 'Content-Type: application/json' \
       -d '{"objectId":"coin_on_floor"}' \
       http://127.0.0.1:8080/api/game/{gameId}/pickup
  # expect 200, inventory now contains "coin"
  ```

**Risks / fallbacks.** None significant.

---

### M7 — Puzzle Evaluation: Combination + Riddle

**Goal.** `POST /api/game/{id}/attempt-puzzle` works for `CombinationPuzzle` and `RiddlePuzzle`. Correct answers award reward items and mark the puzzle solved. Wrong answers return 200 with `lastActionResult = PUZZLE_FAILED`.

**North-star tie.** Puzzle-solving is THE core mechanic. Enables `idea.md §3` steps 3, 6, 7, 11 (clock, display case, iron chest). Demonstrates rubric **Inheritance** (polymorphic `attempt()`).

**Pre-conditions.** M6 complete.

**Red.**

From `design.md §16`:

- `combinationPuzzle_correctCode_returnsTrue`
- `combinationPuzzle_wrongCode_returnsFalse`
- `combinationPuzzle_missingInput_returnsFalse`
- `riddlePuzzle_correctAnswer_caseInsensitive`
- `riddlePuzzle_correctAnswer_caseSensitive`
- `riddlePuzzle_whitespaceTrimmed`

Plus service-level tests:

- `PuzzleEvaluationServicePrereqTest`: puzzle with unsatisfied prereqs throws `PrerequisiteNotMetException`.
- `PuzzleEvaluationServiceRewardTest`: solved puzzle's `rewardItemId` is added to player inventory.
- `PuzzleEvaluationServiceIdempotencyTest`: solving an already-solved puzzle returns success but does not double-insert the reward.

Integration:

- `AttemptPuzzleClockHappyPathTest`: in a fresh foyer session, POST `/attempt-puzzle` for `puzzle_clock` with `{"answer":"11:47"}` returns 200, `lastActionResult = PUZZLE_SOLVED`, `desk_key` in inventory, `puzzle_clock` in `solvedPuzzleIds`.
- `AttemptPuzzleClockWrongAnswerTest`: same with `{"answer":"99:99"}` returns 200, `lastActionResult = PUZZLE_FAILED`, no state change beyond `lastUpdatedAt`.
- `AttemptPuzzleDisplayCaseHappyPathTest`: combination puzzle, code `"384"`, expect 200 + `brass_magnifying_glass` reward.
- `AttemptPuzzleUnknownIdTest`: POST with `puzzleId=puzzle_nope` returns 404 `PUZZLE_NOT_FOUND`.
- `AttemptPuzzlePrereqTest`: attempting `puzzle_iron_chest` (prereq: `puzzle_cipher_wheel`) before prereq is solved returns 409 `PREREQUISITE_NOT_MET`.

**Green.**

- `Puzzle.attempt(Map<String,String>)` abstract method. Implementations in `CombinationPuzzle`, `RiddlePuzzle` per the SQL/data shapes in `design.md §5a`.
- `PuzzleEvaluationService.attempt(UUID, AttemptPuzzleRequest)` — orchestrates: load session, load puzzle, check prereqs (`session.solvedPuzzleIds.containsAll(puzzle.prerequisitePuzzleIds)`), call `puzzle.attempt()`, on success update `solvedPuzzleIds` + award reward via `InventoryService.addItem`, build and return `GameStateDTO`.
- DTO: `AttemptPuzzleRequest` (`@NotBlank puzzleId`, `@NotNull inputs` map).
- Exceptions: `PuzzleNotFoundException`, `PrerequisiteNotMetException`, mapped in `GlobalExceptionHandler` to 404 and 409 respectively.
- `GameController.attemptPuzzle` endpoint.
- `RiddlePuzzle.attempt()` must trim whitespace and respect `caseSensitive` flag — captured by the existing test.

**Refactor.** If `attempt()` accumulates branching for "solved already" vs "first time," extract a small `PuzzleAttemptOutcome` value object. Otherwise leave it.

**Acceptance.**

- Automated: 12+ tests pass.
- Manual demo:
  ```
  # new game, then:
  curl -X POST -H 'Content-Type: application/json' \
       -d '{"puzzleId":"puzzle_clock","inputs":{"answer":"11:47"}}' \
       http://127.0.0.1:8080/api/game/{gameId}/attempt-puzzle
  # expect: lastActionResult PUZZLE_SOLVED, desk_key in inventory
  ```

**Risks / fallbacks.** If `Map<String,String>` deserialization for `inputs` misbehaves on numeric values, force-quote in tests (`{"code":"384"}` not `{"code":384}`); the design already declares strings.

---

### M8 — Sequence + ItemUse + Win Condition

**NORTH STAR CHECKPOINT** — at the end of this milestone, the entire game is winnable through `curl` alone with no UI. This is the single most important backend milestone.

**Goal.** `SequencePuzzle` and `ItemUsePuzzle` solvable. `POST /api/game/{id}/use-item` works. When all 6 puzzles are solved, `gameStatus` flips to `COMPLETE`.

**North-star tie.** Completes the backend half of the golden path. Enables `idea.md §3` steps 8, 10, 12, 13. Demonstrates rubric **Loops** (sequence comparison) and **File I/O** is still pending (M9), but every other rubric concept is now demonstrably present.

**Pre-conditions.** M7 complete.

**Red.**

From `design.md §16`:

- `sequencePuzzle_correctOrder_returnsTrue`
- `sequencePuzzle_wrongOrder_returnsFalse`
- `sequencePuzzle_extraItems_returnsFalse`
- `itemUsePuzzle_correctItemAndTarget_returnsTrue`
- `itemUsePuzzle_wrongItem_returnsFalse`
- `itemUsePuzzle_wrongTarget_returnsFalse`

Integration:

- `UseItemHappyPathTest`: after solving `puzzle_display_case` (which awards `brass_magnifying_glass`), POST `/use-item` with `{"itemId":"brass_magnifying_glass","targetObjectId":"cipher_wheel"}` returns 200, `puzzle_cipher_wheel` solved.
- `UseItemNotInInventoryTest`: POST `/use-item` with an item not held returns 409 `ITEM_NOT_IN_INVENTORY`.
- `UseItemNoMatchingPuzzleTest`: POST `/use-item` with `itemId=desk_key,targetObjectId=cipher_wheel` (no matching ItemUsePuzzle) returns 404 `PUZZLE_NOT_FOUND`.
- **`GameFlowIntegrationTest.goldenPath_solveAllPuzzles_winConditionFires`** (the headline test): from `POST /new`, walk through every puzzle in the canonical order via the API. Assert the final response has `gameStatus = COMPLETE` and `solvedPuzzleIds` contains all 6 puzzle IDs.

**Green.**

- `SequencePuzzle.attempt()` — splits the comma-joined `inputs.get("sequence")` into a list, compares element-by-element to `expectedSequence`. Demonstrates a **loop** (rubric concept).
- `ItemUsePuzzle.attempt()` — checks `inputs.get("itemId")` matches `requiredItemId` AND `inputs.get("targetObjectId")` matches `targetObjectId`.
- `UseItemRequest` DTO.
- `PuzzleEvaluationService.useItem(UUID, UseItemRequest)` — finds the matching `ItemUsePuzzle` by `(requiredItemId, targetObjectId, currentRoom)`. If none found, throws `PuzzleNotFoundException`. If found, evaluates like a normal `attempt()`.
- `ItemNotInInventoryException` + handler.
- `GameController.useItem` endpoint.
- **Win-condition check** in `GameSessionService.buildStateDTO()`:
  ```java
  if (!session.isComplete()
        && session.getSolvedPuzzleIds().containsAll(allPuzzleIds)) {
      session.setStatus(GameStatus.COMPLETE);
      gameSessionRepository.save(session);
  }
  ```
  where `allPuzzleIds` is computed once at startup and cached (size = 6 in Phase 1).
- After `COMPLETE`, any further state-changing endpoint returns 409 `INVALID_MOVE` with message "Game already complete."

**Refactor.** Extract `WinConditionEvaluator` if the cached-puzzle-ID logic feels like it belongs out of `GameSessionService`. Otherwise leave it.

**Acceptance.**

- Automated: 10+ tests pass, including `GameFlowIntegrationTest.goldenPath_solveAllPuzzles_winConditionFires`.
- Manual demo: a single bash script that drives the entire golden path via `curl` and asserts the final state contains `"gameStatus":"COMPLETE"`. Document this script under `backend/scripts/golden-path-curl.sh` for reuse during demos.

**Risks / fallbacks.** If `puzzle.attempt(Map<String,String>)` for `SequencePuzzle` fights against the chosen input format (comma-joined string vs. array), pick **comma-joined** (matches frontend `SequencePuzzleDialog.getInputs()` shape in M13) and document.

---

### M9 — Save / Load (File I/O)

**Goal.** `POST /api/game/{id}/save` writes a human-readable JSON snapshot to `./saves/`. `POST /api/game/{id}/load` rehydrates the session. `GET /api/game/{id}/saves` lists saves for that gameId.

**North-star tie.** Closes the explicit **File I/O** rubric concept (the H2 file mode also counts, but this is the visible artifact). Closes §20 box 8 (save creates a JSON file) and box 9 (load restores state). Enables player-facing save/resume.

**Pre-conditions.** M8 complete.

**Red.**

- `SaveLoadServiceTest` (unit-ish with a temp directory): `save(gameId)` writes a file matching `<gameId>-<timestamp>.json`, content matches a `GameSnapshotDTO` for the session, `schemaVersion = 1`.
- `LoadHappyPathTest`: write a snapshot manually, then call `load(gameId, filename)`. Assert session state is overwritten to match the snapshot.
- `LoadSchemaVersionTest`: load a snapshot with `schemaVersion = 99`. Assert it returns 500 `LOAD_FAILED` with a clear message ("incompatible save version"). This guards `design.md §19` risk #5.
- `PathTraversalGuardTest`: load with `filename = "../../etc/passwd"` returns 400 `INVALID_REQUEST`. Same for filenames containing `/` or `\`.
- **`SaveLoadIntegrationTest.saveThenLoad_restoresState`** (from `design.md §16`): play partway through golden path, save, mutate further, load, assert state matches the saved point.

**Green.**

- `GameSnapshotDTO` (per `design.md §5` — mutable session state only).
- `LoadRequest`, `SaveConfirmationDTO`, `SaveMetadataDTO`.
- `SaveLoadService`:
  - `save(UUID)` — assembles snapshot, ensures `./saves/` exists (`Files.createDirectories`), writes via `ObjectMapper.writeValue(File, Object)`, returns `SaveConfirmationDTO`.
  - `load(UUID, String filename)` — validates filename (no path separators, ends in `.json`), reads via `ObjectMapper.readValue(File, GameSnapshotDTO.class)`, checks `schemaVersion == 1`, rehydrates `GameSession` and `PlayerInventory` via repositories.
  - `listSaves(UUID)` — `Files.list(savesDir)` filtered by prefix `gameId-`.
- `SaveLoadException` + handler.
- `GameController.save`, `GameController.load`, `GameController.listSaves` endpoints.
- Filename validation moved into `LoadRequest` with a `@Pattern` annotation backed by `jakarta.validation`.

**Refactor.** If the loader's "delete old element-collection rows, insert new ones" logic for `solvedPuzzleIds` and `heldItemIds` gets messy, isolate it into a `SnapshotApplier` helper.

**Acceptance.**

- Automated: 5 new tests pass, including the integration test.
- Manual demo:
  ```
  # midway through a game:
  curl -X POST http://127.0.0.1:8080/api/game/{gameId}/save
  # response includes "filename": "<gameId>-20260531T143200.json"
  ls saves/
  # confirm the file exists
  cat saves/<that-file>
  # confirm it's human-readable JSON with the right schemaVersion

  # kill backend, restart
  curl http://127.0.0.1:8080/api/game/{gameId}/saves
  # lists the save
  curl -X POST -d '{"filename":"<that-file>"}' -H 'Content-Type: application/json' \
       http://127.0.0.1:8080/api/game/{gameId}/load
  # state restored
  ```

**Risks / fallbacks.** If `./saves/` can't be created at runtime, fail fast with `SAVE_FAILED` 500 and a clear log line. If Jackson refuses to serialize `LocalDateTime` without configuration, ensure `jackson-datatype-jsr310` is on the classpath (already in `pom.xml`) and registered in the `ObjectMapper` bean (`mapper.registerModule(new JavaTimeModule())`).

---

### M10 — Frontend Skeleton

**Goal.** `mvn -pl frontend exec:java` opens a Swing window showing a placeholder foyer scene. No backend connection yet — the window opens even with the backend down.

**North-star tie.** First visible game. Closes §20 box 6 ("Swing window displays the foyer scene with hotspot rectangles labeled").

**Pre-conditions.** M9 complete (or in parallel with M9 — they don't depend on each other, but conventionally finish backend first).

**Red.**

- `PlaceholderAssetManagerTest`: `getRoomBackground("room_foyer")` returns non-null `ImageIcon` with dimensions matching `design.md §15` (1024 × 640). `getItemIcon("item_key")` returns 64 × 64.
- `MainFrameSmokeTest`: launch `MainFrame` via `SwingUtilities.invokeAndWait`, assert `frame.isVisible()`, `frame.getContentPane().getComponentCount() == 4` (status bar, scene, inventory, dialogue).

**Green.**

- `EscapeRoomApp.main()` — `SwingUtilities.invokeLater(() -> new MainFrame(...).setVisible(true))`.
- `MainFrame extends JFrame` — BorderLayout with `StatusBar` north, `ScenePanel` center, `InventoryPanel` east, `DialoguePanel` south. Wires no listeners yet beyond the `WindowAdapter` for clean close. Renders a hardcoded foyer placeholder (no API call).
- `ScenePanel extends JPanel` — `paintComponent` draws the room background via `AssetManager`, then iterates `List<Hotspot>` drawing labeled rectangles.
- `InventoryPanel extends JPanel` — placeholder vertical box, empty for now.
- `DialoguePanel extends JPanel` — `JTextArea` inside `JScrollPane`, single welcome line.
- `StatusBar extends JPanel` — `JLabel` for room name + three `JButton`s (Save/Load/New) with no listeners.
- `Hotspot` data class (final fields: `Rectangle bounds`, `String objectId`, `ObjectType type`, optional `String puzzleId`, optional `String puzzleType`).
- `ObjectType` enum: `SCENERY`, `PUZZLE`, `ITEM`, `EXIT`.
- `AssetManager` interface, `PlaceholderAssetManager` impl (returns colored `BufferedImage` with text label), `FileAssetManager` stub (returns null for now — files don't exist in Phase 1).

**Refactor.** Composite asset-manager pattern: `MainFrame` wraps a `FileAssetManager` inside a `PlaceholderAssetManager`-fallback wrapper so future PNG drops "just work." Defer the composite until M14 polish if it complicates this milestone.

**Acceptance.**

- Automated: smoke test + asset manager test pass.
- Manual demo: `mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp` — window opens, you see a colored rectangle labeled "The Entry Foyer", an empty inventory area, and a status bar with three buttons.

**Risks / fallbacks.** If running headless (CI server, SSH session), tests must set `-Djava.awt.headless=false` or use `GraphicsEnvironment.isHeadless()` guards. AP CS environment is interactive — likely a non-issue.

---

### M11 — Frontend ↔ Backend Wiring

**Goal.** Swing window starts a new game via the backend on launch and renders the real foyer (room name from `world.json`, hotspot rectangles for every `roomObject`).

**North-star tie.** First full-stack vertical slice. Enables `idea.md §3` step 1 for real (not placeholder).

**Pre-conditions.** M10 complete.

**Red.**

- `GameApiClientNewGameTest` — the frontend module does **not** depend on Spring Boot, so `@SpringBootTest` is unavailable here. Stand up a stub server with JDK's built-in `com.sun.net.httpserver.HttpServer` on an ephemeral port (`new InetSocketAddress(0)`) inside `@BeforeEach`; register a handler for `POST /api/game/new` that responds with a canned `GameStateDTO` JSON body and `200 OK`; point `GameApiClient` at `http://127.0.0.1:<port>`; assert `client.newGame()` returns a valid `GameStateDTO` with `IN_PROGRESS` status and `room_foyer`. `@AfterEach` stops the server. No Spring on the classpath; no port conflict with a real backend.
- `GameApiClientGetStateTest`: after `newGame()`, `getState(gameId)` returns the same state.
- `GameApiClientErrorTest`: `getState(random-uuid)` throws `ApiException` whose `ErrorResponseDTO.errorCode == GAME_NOT_FOUND`.
- `MainFrameRenderStateTest` (Swing unit-ish): construct `MainFrame`, manually call `renderState(mockDto)`, assert `scenePanel.getHotspots().size() == mockDto.currentRoom.objects.size()`, assert `statusBar.getRoomLabel().getText()` matches the room name.

**Green.**

- All DTOs duplicated under `com.abhishri.escape.ui.api.dto` (per `idea.md §8`). Identical shapes to backend DTOs.
- `GameApiClient` — uses `java.net.http.HttpClient` (built into JDK 11+, no extra dependency). One method per backend endpoint, all returning typed DTOs or throwing `ApiException`. JSON via Jackson `ObjectMapper` (configured with `JavaTimeModule`).
- `ApiException extends RuntimeException` — wraps an `ErrorResponseDTO`.
- `MainFrame` constructor takes a `GameApiClient`. In the constructor (still on the EDT), calls `client.newGame()` and invokes `renderState(dto)`.
- `MainFrame.renderState(GameStateDTO)` — pushes `dto.currentRoom` to `scenePanel.setRoom()`, `dto.inventory` to `inventoryPanel.setItems()`, `dto.dialogueMessage` to `dialoguePanel.append()`, `dto.gameStatus`/`dto.currentRoom.name` to `statusBar.update()`.
- `ScenePanel.setRoom(RoomDTO)` — builds a `List<Hotspot>` from the room's objects (one per `RoomObjectDTO`, plus one per exit), positioned at evenly-spaced rectangles (placeholder grid layout for now; real positions need real art).

**Refactor.** Move HTTP error handling out of every `GameApiClient.send()` call into one private helper that throws `ApiException` on 4xx/5xx.

**Acceptance.**

- Automated: 4 new tests pass.
- Manual demo: start backend (`mvn -pl backend spring-boot:run`); in another terminal, launch frontend (`mvn -pl frontend exec:java -Dexec.mainClass=...`). The window now shows:
  - Status bar: "The Entry Foyer" + Save/Load/New buttons.
  - Scene panel: 5 labeled rectangles ("Wall Clock", "Reception Desk", etc.) + 1 exit rectangle ("→ Reading Hall").
  - Dialogue panel: the foyer description text from `world.json`.
  - Inventory panel: empty.

**Risks / fallbacks.** If the backend is down when frontend launches, `MainFrame` constructor throws `ApiException` and `EscapeRoomApp.main` shows a `JOptionPane.showMessageDialog` ("Backend not reachable on :8080") and exits cleanly. Don't crash with a stack trace.

---

### M12 — Hotspot Click → Examine / Move

**Goal.** Clicking a hotspot triggers the right API call: SCENERY → examine, EXIT → move, ITEM → pickup. Backend response renders into the panels via `renderState`. Errors surface as `JOptionPane.WARNING_MESSAGE`, not crashes.

**North-star tie.** Enables `idea.md §3` steps 2 (examine clock — opens dialog in M13 actually; for now, examine non-puzzle objects shows their description), 5, 9 (room transitions). Player can navigate the world by clicking.

**Pre-conditions.** M11 complete.

**Red.**

- `HotspotHitTestTest`: given a `List<Hotspot>` with known bounds and a `MouseEvent` at a specific `(x, y)`, `ScenePanel.findHotspotAt(x, y)` returns the matching `Hotspot` or null.
- `HotspotClickDispatchTest` (mocked `GameApiClient`): simulate click on a `SCENERY`-type hotspot → assert `client.examine(gameId, objectId)` was called. EXIT → `client.move(...)`. ITEM → `client.pickup(...)`.
- `ApiErrorRendersDialogTest`: inject a `CapturingErrorReporter` (test impl of the `ErrorReporter` seam — see Green below) into `MainFrame`; simulate a `GameApiClient` that throws `ApiException` with `errorCode = INVALID_MOVE`; trigger a hotspot click whose dispatch calls the failing method; assert `capturingErrorReporter.lastReported().getErrorCode().equals("INVALID_MOVE")` and that the reported message matches the exception's message. No `JOptionPane` static mocking, no PowerMock — the seam makes the test deterministic and runs headless.

**Green.**

- `ScenePanel.MouseListener` — on click, `findHotspotAt(e.getX(), e.getY())`. If non-null, call back into `MainFrame.onHotspotClicked(hotspot)` via a constructor-injected callback.
- `MainFrame.onHotspotClicked(Hotspot)` — switches on `hotspot.type`. SCENERY → `client.examine(gameId, hotspot.objectId)`. EXIT → `client.move(gameId, hotspot.objectId)` (where `objectId` holds the target room ID for exits). ITEM → `client.pickup(...)`. PUZZLE → defer (handled in M13).
- All API calls wrapped in `try { ... renderState(dto); } catch (ApiException e) { showError(e); }`.
- `ErrorReporter` — small interface with a single method `void report(ApiException e)`. Production impl `JOptionPaneErrorReporter` (holds a parent `Component`) calls `JOptionPane.showMessageDialog(parent, e.getError().getMessage(), "Action failed (" + e.getError().getErrorCode() + ")", JOptionPane.WARNING_MESSAGE)`. `MainFrame` takes an `ErrorReporter` via its constructor (`EscapeRoomApp.main` wires `new JOptionPaneErrorReporter(mainFrame)`); `MainFrame.showError(ApiException e)` is a one-liner that delegates to `errorReporter.report(e)`. The seam exists for testability; production behavior is identical to the original `JOptionPane.showMessageDialog` call.
- `GameApiClient` gains `examine`, `move`, `pickup` methods.

**Refactor.** Hotspot dispatching is a tiny switch; if it grows past 4 cases, introduce a `HotspotActionStrategy` map — but for Phase 1, the switch is clearer.

**Acceptance.**

- Automated: 3 new tests pass.
- Manual demo: launch both processes. From the foyer:
  - Click "Wall Clock" hotspot → dialogue panel shows the clock description (the riddle preview). Note: clicking the clock should open the riddle dialog in M13; for M12 only examine works.
  - Click "→ Reading Hall" → scene transitions; status bar updates; new hotspots render.
  - Click "Display Case" in reading hall → dialogue shows the case description (puzzle dialog not yet wired).
  - Try a contrived bad move via the JOptionPane fallback (e.g., temporarily edit `world.json` to remove the connection) → friendly error dialog, no crash.

**Risks / fallbacks.** If `MouseListener` fires on EDT but `HttpClient.send()` blocks the EDT noticeably for any reason, accept it per `idea.md §12` resolved decision (no `SwingWorker` in Phase 1). Document the freeze if it's perceptible.

---

### M13 — Puzzle Dialogs + Win Screen

**NORTH STAR CHECKPOINT** — at the end of this milestone, `idea.md §3` plays end-to-end with clicks only. This is the central UX milestone.

**Goal.** Each puzzle type opens its own dialog with the right inputs. Submitting the dialog calls `attempt-puzzle` (or `use-item` for ITEM_USE) and renders the response. On the final puzzle, the win screen appears.

**North-star tie.** Closes §20 box 7 (full golden path completes from clicks). Enables `idea.md §3` steps 3, 6, 7, 8, 10, 11, 12, 13.

**Pre-conditions.** M12 complete.

**Red.**

- `CombinationPuzzleDialogInputTest`: programmatically construct dialog with `digitCount=3`, set spinners to 3, 8, 4, call `getInputs()`. Assert returned map is `{"code":"384"}`.
- `RiddlePuzzleDialogInputTest`: construct dialog, set text field to `"11:47"`, call `getInputs()`. Assert `{"answer":"11:47"}`.
- `SequencePuzzleDialogInputTest`: construct with 6 items, reorder programmatically to the target sequence, call `getInputs()`. Assert `{"sequence":"510,520,610,621,720,810"}`.
- `WinScreenFiresTest` (Swing unit-ish): given a mocked `GameApiClient` that returns a `GameStateDTO` with `gameStatus = COMPLETE`, calling `MainFrame.renderState(dto)` triggers `JOptionPane.showMessageDialog` (capture via test double).

**Green.**

- `PuzzleDialog` (abstract `JDialog` with confirm/cancel buttons, abstract `getInputs()`).
- `CombinationPuzzleDialog` — row of `JSpinner` widgets (one per digit).
- `RiddlePuzzleDialog` — `JLabel` for question + `JTextField` for answer.
- `SequencePuzzleDialog` — `JList<String>` + `TransferHandler` for drag-and-drop, OR (fallback) move-up/move-down buttons with `DefaultListModel`. Build drag-and-drop first; if it fights us, swap `puzzle_bookshelf` in `world.json` from `SEQUENCE` to `RIDDLE` per `idea.md §12` resolved decision (frontend code unchanged; backend already handles the `RIDDLE` type).
- `MainFrame.onHotspotClicked` for `PUZZLE` type — reads `hotspot.puzzleType` (from `RoomObjectDTO`), instantiates the matching dialog, on dialog confirm calls `client.attemptPuzzle(gameId, puzzleId, getInputs())`.
- ITEM-on-hotspot interaction: if `InventoryPanel.getSelectedItemId() != null` AND the clicked hotspot is `SCENERY` or `PUZZLE` type → call `client.useItem(gameId, selectedItemId, hotspot.objectId)` instead of examine. Inventory selection is cleared after use.
- Win screen: in `MainFrame.renderState`, after dispatching to panels, check `dto.gameStatus == GameStatus.COMPLETE`. If so and we haven't shown it yet (a `boolean winShown` flag), invoke `JOptionPane.showMessageDialog(this, dto.dialogueMessage, "You're free!", JOptionPane.INFORMATION_MESSAGE)`.
- `GameApiClient` gains `attemptPuzzle`, `useItem` methods.

**Refactor.** If puzzle-type → dialog-class wiring becomes ugly, introduce a small factory:
```java
PuzzleDialog dialog = switch (objectDto.puzzleType) {
    case "COMBINATION" -> new CombinationPuzzleDialog(...);
    case "RIDDLE"      -> new RiddlePuzzleDialog(...);
    ...
};
```
Keep it inline if it stays short.

**Acceptance.**

- Automated: 4 new tests pass.
- Manual demo: walk the full `idea.md §3` golden path (steps 1–13) using clicks only. The win dialog appears on step 13.

**Risks / fallbacks.** **Drag-and-drop is the known timeline risk.** If we burn more than half a day on `TransferHandler` plumbing, immediately invoke the fallback: change `world.json` `puzzle_bookshelf.type` from `SEQUENCE` to `RIDDLE`, set `expectedAnswer = "510,520,610,621,720,810"`, restart backend. Zero frontend code change. Re-run the golden path. The drag-and-drop dialog goes to the Phase 2 backlog per `idea.md §11`.

---

### M14 — Save / Load UI + Phase 1 Acceptance

**Goal.** Save / Load / New Game buttons in the status bar are wired. The full `design.md §20` acceptance checklist passes.

**North-star tie.** Closes §20 boxes 8 ("Save creates JSON file"), 9 ("Load restores"), 12 ("editing world.json honored"), 13 ("AP CS rubric matrix"). This is the **Phase 1 shipped** moment.

**Pre-conditions.** M13 complete.

**Red.**

- `StatusBarSaveButtonTest`: press Save button → `client.save(gameId)` called → confirmation dialog shown with filename.
- `StatusBarLoadFlowTest`: press Load → `client.listSaves(gameId)` called → `JOptionPane.showInputDialog` (mock) returns the user's pick → `client.load(gameId, filename)` called → `renderState` invoked with the loaded state.
- `StatusBarNewGameTest`: press New Game → confirmation dialog → `client.newGame()` → new state rendered.

**Green.**

- `StatusBar` buttons wired to `MainFrame.onSavePressed`, `onLoadPressed`, `onNewGamePressed`.
- `onSavePressed` — calls `client.save(gameId)`, shows `JOptionPane` with the filename.
- `onLoadPressed` — calls `client.listSaves(gameId)`. If empty, info dialog "No saves yet." Otherwise, `JOptionPane.showInputDialog` with the file list. On selection, calls `client.load(gameId, filename)` and renders.
- `onNewGamePressed` — `JOptionPane.showConfirmDialog` ("Start a new game?"); on YES, calls `client.newGame()` and renders.
- Polish pass: dialogue panel auto-scrolls to bottom on `append()`. Inventory selection visual highlight. Status bar gets a "Solved: 3/6" counter. Confirm `gameStatus = COMPLETE` disables the Save button (game is over).
- Confirm composite-asset-manager wrap (`FileAssetManager` → `PlaceholderAssetManager` fallback) is in place — even though no PNGs exist in Phase 1, the wiring is correct for Phase 2 art drop-in.

**Refactor.** This is the polish milestone — actively look for duplication, dead branches, leftover stubs. Run any IDE inspection. Re-read every TODO and either resolve or document.

**Acceptance — the full §20 checklist:**

- [ ] `mvn clean install` from repo root builds both modules with zero errors and zero test failures.
- [ ] Backend starts via `mvn -pl backend spring-boot:run` and binds to `127.0.0.1:8080`.
- [ ] `GET http://127.0.0.1:8080/api/health` returns `{"status":"ok"}`.
- [ ] `http://127.0.0.1:8080/h2-console` lists all 17 tables from `design.md §3a`.
- [ ] Inspection in H2 console after first boot shows: 3 rows in `ROOM`, 6 rows in `PUZZLE` distributed across the four child tables as **2 in `RIDDLE_PUZZLE`** (`puzzle_clock`, `puzzle_iron_chest`), **1 in `COMBINATION_PUZZLE`** (`puzzle_display_case`), **1 in `SEQUENCE_PUZZLE`** (`puzzle_bookshelf`), **2 in `ITEM_USE_PUZZLE`** (`puzzle_cipher_wheel`, `puzzle_terminal`) — totaling 6. **5 rows in `INVENTORY_ITEM`**.
- [ ] Frontend launches via `mvn -pl frontend exec:java` and the Swing window displays the foyer scene with hotspot rectangles labeled.
- [ ] The full golden path from `idea.md §3` (steps 1–13) completes end-to-end purely from clicks; the win screen `JOptionPane` appears with the closing dialogue text.
- [ ] Pressing "Save" creates `./saves/<gameId>-<timestamp>.json`. The file is human-readable and contains the `GameSnapshotDTO` fields.
- [ ] Restarting the backend, then pressing "Load" and selecting the saved file, restores the player to the same room with the same inventory and the same solved puzzles.
- [ ] All backend unit tests pass — the cluster catalogued in `§3e` (expected count ≈ 15 in Phase 1; the exact number rises as edge-case tests are added during M2–M9 and is not load-bearing).
- [ ] All 3 backend integration tests pass.
- [ ] Editing `world.json` (e.g., change `puzzle_clock.expectedAnswer` from `"11:47"` to `"11:48"`), **deleting `./data/` to clear the seeded H2 file** (`WorldSeedService` only seeds an empty DB by design), restarting the backend, and attempting the clock puzzle confirms the new answer is required — no recompile.
- [ ] AP CS rubric matrix from `idea.md §9`: for every concept, Abhishri can name the class/method that demonstrates it without consulting notes.

**Risks / fallbacks.** None — at this milestone the only blockers are forgotten boxes in the §20 checklist. Walk it slowly.

---

## 7. Risk Hotspots (project-wide, lifted from `design.md §19`)

| Risk                                                              | Mitigation                                                                                          | Watch in milestone |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|---------------------|
| `@ElementCollection` ordering loss (`SequencePuzzle.expectedSequence`) | `@OrderColumn(name = "POSITION")`; verify with `SequencePuzzleOrderTest`                            | M2                  |
| H2 file-mode single-writer lock (two backends started by accident)| README troubleshooting guide; startup banner logs absolute DB path                                  | M1, M9              |
| Jackson polymorphic seed typo silently breaks startup             | Loud failure intended; `WorldSeedReferentialIntegrityTest` ensures bad data crashes loudly          | M3                  |
| `@OneToOne` insertion order (GameSession ↔ PlayerInventory)        | Save inventory first, then attach, then save session; comment in `GameSessionService.createNewGame` | M2, M4              |
| `schemaVersion` mismatch silently corrupting old saves            | `LoadSchemaVersionTest` enforces refusal of non-1 versions                                          | M9                  |
| `SequencePuzzleDialog` drag-and-drop slips                        | Fallback: change `world.json` puzzle type to RIDDLE — no code change needed                          | M13                 |
| `javax.persistence` accidentally imported from old tutorials      | `design.md §3c` documented; `mvn compile` fails loudly on missing imports                            | Every backend milestone |
| Swing `HttpClient.send()` on EDT freezes UI                       | Acknowledged per `idea.md §12`; deferred to Phase 2 (`SwingWorker`); localhost latency makes it imperceptible | M11–M14    |

---

## 8. North Star Traceability — `design.md §20` → Milestone

| § 20 Acceptance Box                                                                                 | Closed by |
|------------------------------------------------------------------------------------------------------|-----------|
| 1. `mvn clean install` builds both modules with zero errors and zero test failures.                  | M0 → M14  |
| 2. Backend starts via `mvn -pl backend spring-boot:run` and binds to `127.0.0.1:8080`.               | M1        |
| 3. `GET /api/health` returns `{"status":"ok"}`.                                                       | M1        |
| 4. H2 console lists all 17 tables from `design.md §3a`.                                              | M2        |
| 5. H2 console shows 3 rooms, 6 puzzles (2 RIDDLE + 1 COMBINATION + 1 SEQUENCE + 2 ITEM_USE = 6), 5 items after seeding. | M3        |
| 6. Frontend launches and the Swing window displays the foyer with labeled hotspots.                   | M10 + M11 |
| 7. Full golden path from `idea.md §3` completes purely from clicks; win screen fires.                 | M13       |
| 8. Pressing "Save" creates a readable `./saves/...json`.                                              | M9 + M14  |
| 9. Restart + Load restores the same room, inventory, and solved puzzles.                              | M9 + M14  |
| 10. All backend unit tests in `§3e` pass (≈15 in Phase 1; exact count is not load-bearing).           | M7 + M8   |
| 11. All 3 backend integration tests pass.                                                             | M8 + M9   |
| 12. Editing `world.json`, deleting `./data/` to clear the seeded H2 file, and restarting picks up the change — no recompile. | M3        |
| 13. AP CS rubric matrix: every concept maps to a named class/method Abhishri can point at.            | M14 (review) |

---

## 9. Rubric Concept → Milestone where it first appears

(For Abhishri's grading conversation — every AP CS concept demonstrated by Phase 1 surfaces in a specific milestone she built.)

| Concept       | First demonstrated in | Where to point                                                                   |
|---------------|-----------------------|----------------------------------------------------------------------------------|
| Classes & Objects | M2                | Every entity in `com.abhishri.escape.domain`                                     |
| Inheritance       | M2                | `Puzzle` (abstract) → 4 concrete subclasses, JOINED in H2                        |
| ArrayLists        | M2                | `PlayerInventory.heldItemIds`, `Room.connectedRoomIds`, `SequencePuzzle.expectedSequence` |
| Loops             | M8                | `SequencePuzzle.attempt()` iterates expected vs. submitted sequence              |
| Conditionals      | M5                | Room adjacency check in `GameSessionService.move()`                              |
| File I/O          | M3 + M9           | M3: `WorldSeedService` reads `world.json` via `ObjectMapper.readValue(File, ...)`. M9: `SaveLoadService` writes/reads `GameSnapshotDTO` via Jackson + `java.nio.file.Files`. H2 file mode is itself disk-backed file I/O. |
| GUI               | M10 → M14         | Every class in `com.abhishri.escape.ui` — `MainFrame`, `ScenePanel`, all `PuzzleDialog` subclasses |

---

## 10. How to use this document during build

1. **At the start of each milestone**, re-read its section in full. Confirm pre-conditions.
2. **Write the Red tests first.** Don't write production code until you can see them fail.
3. **Green minimally**, then **Refactor**.
4. **Run the acceptance gate** — automated `mvn clean install` from root, then the manual demo. Both must pass.
5. **Walk the §20 traceability** — tick the boxes this milestone closed.
6. **Commit** with a message naming the milestone (`feat(M3): world seeding from world.json`).
7. **Mentor review** at the end of every milestone.

If anything in the design fails to match reality during a milestone — a JPA annotation refuses to compile, an endpoint shape changes — that's a flag for the mentor, not an in-flight pivot. Update `design.md` first, then resume.

---

## 11. Out of scope for this plan

- Phase 2 backlog (timer, hints, sound, multiple saves, React port, accounts) — see `idea.md §11`.
- Real CC-licensed art — `idea.md §11` defers this; M14 confirms the asset-manager fallback wiring works so the swap-in is a Phase 2 drop-in.
- CI/CD, Docker, deployment — single-laptop only per `design.md §18`.
- Performance tuning — `design.md §17` confirms defaults are fine at single-player scale.

---

**End of plan.md.** Mentor sign-off required before M0 begins.
