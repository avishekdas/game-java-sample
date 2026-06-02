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
| `design_ui_upgrade.md` | UI upgrade mechanics: color palette, typography, component specs, procedural art algorithm, visual hint system (§10) |
| `plan_ui_upgrade.md` | UI upgrade TDD milestones UI-M1–UI-M7, Red/Green/Acceptance per milestone |
| `README.md` | Operator quickstart, running instructions, manual test checklist, how-to-play |

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

### Running the game manually
```bash
# Terminal 1 — backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn -pl backend spring-boot:run
# Wait for: Tomcat started on port 8080

# Terminal 2 — frontend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp

# H2 console: http://127.0.0.1:8080/h2-console  JDBC: jdbc:h2:file:./data/escaperoom  user: sa  pw: (blank)
# Health:     curl http://127.0.0.1:8080/api/health
```

---

## 5. Milestone Status

### Phase 1 — Backend + Swing MVP (M0–M14)

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
| M8 — Sequence + ItemUse + Win | ✅ DONE | `POST /use-item`, win condition, 75 backend tests |
| M9 — Save / Load | ✅ DONE | `SaveLoadService`, File I/O rubric, 86 backend tests |
| M10 — Swing Skeleton | ✅ DONE | `EscapeRoomApp`, `MainFrame` (4 panels), `ScenePanel`, `AssetManager`, 88 total tests |
| M11 — GameApiClient + Hotspot Clicks | ✅ DONE | All HTTP methods, `MouseListener`, `applyState`, 94 total tests |
| M12 — Puzzle Dialogs + Win Screen | ✅ DONE | `PuzzleDialog` → 3 subclasses, win dialog, item-use shortcut, 98 total tests |
| M13 — Save/Load UI + Phase 1 Acceptance | ✅ DONE | `listSaves` flow, `confirmNewGame`, solved counter, 102 total tests |
| M14 — Polish + Phase 1 Ship | ✅ DONE | `FileAssetManager` (Inheritance rubric), README, 105 total tests — **Phase 1 shipped** |

### Phase 2 — UI Visual Overhaul (UI-M1–UI-M5)  ✅ Complete

| Milestone | Status | Key outputs |
|-----------|--------|-------------|
| UI-M1 — ThemeConstants + ProceduralAssetManager | ✅ DONE | 12 color + 6 font constants; runtime procedural room art + item icons |
| UI-M2 — Hotspot + MainFrame plumbing | ✅ DONE | `Hotspot.solved` field; null-safe `solvedIds` set; `buildHotspots(solvedIds)` |
| UI-M3 — ScenePanel visual rework | ✅ DONE | Styled hotspots; hover (hand cursor + glow); room crossfade; `FileAssetManager` → `ProceduralAssetManager` fallback |
| UI-M4 — Panels | ✅ DONE | Dark theme: `StatusBar` (dots), `DialoguePanel` (parchment), `InventoryPanel` (icons + use-mode) |
| UI-M5 — Puzzle Dialogs + Final Gate | ✅ DONE | All dialogs dark/parchment styled; `design_ui_upgrade.md §9` acceptance gate complete |

### Phase 2 — Visual Hint System (UI-M6–UI-M7)  ✅ Complete

| Milestone | Status | Key outputs |
|-----------|--------|-------------|
| UI-M6 — Room Art Counting Clues | ✅ DONE | 8 bookshelves in Reading Hall (up from 5); 3 AGED_BRASS nail circles in Foyer doorframe |
| UI-M7 — Examination Hint Cards | ✅ DONE | `getHintCard()` default on `AssetManager`; 4 procedural 320×200 cards; `ScenePanel` overlay with hold+fade timer; `MainFrame` trigger logic (PUZZLE delay + SCENERY examine) |

**Additional polish:** auto-exit on win (`dispose()` + `System.exit(0)` in `showWinDialog`); wall clock card shows every click (not once-per-session); clock hands render actual answer time 11:47.

**Test count — all phases complete:** 83 backend + 82 frontend = **165 tests**, all green. Branch `ui-improvement` merged to `main`.

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
│       ├── CombinationPuzzle.java, RiddlePuzzle.java, SequencePuzzle.java, ItemUsePuzzle.java
├── dto/  (GameStateDTO, RoomDTO, RoomObjectDTO, InventoryItemDTO, all request/response DTOs)
├── exception/ (ApiErrorCode, GlobalExceptionHandler, domain exceptions)
├── repository/ (5 Spring Data repositories)
└── service/
    ├── GameSessionService.java, InventoryService.java
    ├── PuzzleEvaluationService.java, SaveLoadService.java
    └── WorldSeedService.java + WorldSeedValidator.java

backend/src/main/resources/
├── application.properties
└── world.json                          (3 rooms, 6 puzzles, 5 items — unchanged in Phase 2)

frontend/src/main/java/com/abhishri/escape/ui/
├── dto/
│   ├── GameStateDTO.java, RoomDTO.java, InventoryItemDTO.java, RoomObjectDTO.java
│   └── LastActionResult.java, GameStatus.java, ObjectType.java (enums)
├── AssetManager.java              (interface: getBackground, getItemIcon — Phase 2 adds getHintCard default)
├── PlaceholderAssetManager.java   (simple colored rects — used by tests only; unchanged)
├── FileAssetManager.java          (PNG-first → ProceduralAssetManager fallback — Phase 2 changed fallback)
├── ProceduralAssetManager.java    ★ NEW Phase 2 — Java 2D runtime art: room backgrounds + item icons
├── ThemeConstants.java            ★ NEW Phase 2 — all Color + Font constants + applyDarkButton()
├── Hotspot.java                   (id, type, label, bounds, objectId, solved — Phase 2 added solved field)
├── ScenePanel.java                (Phase 2 rework: styled overlays, hover, crossfade fade timer)
├── InventoryPanel.java            (Phase 2 rework: ItemCellRenderer, icon cache, use-mode title)
├── StatusBar.java                 (Phase 2 rework: PuzzleDotsPanel inner class, dark theme)
├── DialoguePanel.java             (Phase 2 rework: parchment textarea, ThornwickScrollBarUI)
├── PuzzleDialog.java              (Phase 2 rework: dark theme, applyThemeRecursively helper)
├── CombinationPuzzleDialog.java   (Phase 2: parchment spinners)
├── RiddlePuzzleDialog.java        (Phase 2: parchment text field)
├── SequencePuzzleDialog.java      (Phase 2: dark list + styled buttons, list promoted to field)
├── GameApiClient.java             (all HTTP methods: newGame, move, examine, pickup, etc.)
├── MainFrame.java                 (BorderLayout orchestrator; hotspot click dispatch; applyState)
└── EscapeRoomApp.java             (main() — SwingUtilities.invokeLater, wires FileAssetManager)
```

---

## 7. Project Status

**All milestones complete. Branch `ui-improvement` merged to `main`.**

165 tests pass (`mvn --offline clean test` from repo root). Zero backend changes across Phase 2.

The project is ready for AP CS submission. To play the finished game:
1. Start the backend (`mvn -pl backend spring-boot:run`)
2. Start the frontend (`mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp`)
3. Click **New Game** and play through all six puzzles
4. On win, click OK — the application exits automatically

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
| 9 | `@DataJpaTest` context-sharing with `@SpringBootTest` classes | Use `@ActiveProfiles("test")` consistently; `application-test.properties` isolates |
| 10 | `world.json` used `objectIds` (strings); replaced with `objects` (RoomObjectSeed) | `RoomSeed` now has `List<RoomObjectSeed> objects`; `Room` entity has `List<RoomObject> objects` |
| 11 | Lambda captures variable that is later reassigned — compile error | Use a new variable for the saved result: `GameSession saved = repo.save(session)` |
| 12 | `UnnecessaryStubbing` with Mockito STRICT_STUBS | Put stubs that are test-specific into individual test methods, not `@BeforeEach` |
| 13 | `WorldSeedServiceTest` count assertions break when `world-test.json` is extended | Update count expectations every time test fixture gains new entities |
| 14 | **AssertJ not in frontend `pom.xml`** — frontend tests that import `assertThat` fail to compile | Frontend test assertions must use `org.junit.jupiter.api.Assertions` only (`assertNotNull`, `assertEquals`, etc.) |
| 15 | **`javax.swing.Timer` self-reference in lambda** — `fadeTimer.stop()` inside the timer's own `ActionListener` causes "blank final field may not have been initialized" | Use `((Timer) e.getSource()).stop()` instead of referencing the field by name |
| 16 | **`assetManager` is a constructor parameter, not a field, in `MainFrame`** | Both `ScenePanel` and `InventoryPanel` are constructed inside the constructor where the parameter is in scope — do not add a field |
| 17 | **Solved lookup uses `obj.getPuzzleId()` not `obj.getId()`** | `solvedIds` contains puzzle IDs (e.g. `"puzzle_clock"`); hotspot object ID is `"wall_clock"`. Using `getId()` for the solved check silently never matches |
| 18 | **`applyThemeRecursively` ordering** — applying theme AFTER `initLayout()` overwrites subclass parchment colors | Subclasses style `JTextField`/`JSpinner`/`JList` BEFORE calling `initLayout()`; the recursive helper skips those types |
| 19 | **`currentRoomId` initialized to `null`, not `"foyer"`** — first room set must not trigger crossfade | `setCurrentRoomId` uses `Objects.equals` + `previousId != null` guard; test `ScenePanelFadeGuardTest` pins this |
| 20 | **Reading Hall had 5 bookshelf silhouettes** — the combination digit is 8 | UI-M6 changed the loop from 5 to 8 (`shelfW=72, gap=24`); pixel test at **(750, 200)** verifies shelf 8 (old plan used 696,200 which was inside old shelf 5 — vacuously true) |
| 21 | **Foyer nail pixel tests: "NOT NIGHT_BLACK" is vacuously true** — the nail coordinates sit inside the arch PIE fill (silhouette color, not gradient) | Use `assertEquals(AGED_BRASS.getRGB(), ...)` instead of `assertNotEquals(NIGHT_BLACK, ...)` for nail pixel assertions |
| 22 | **Two clock hands at any angle read as a specific time** — "10:15", "3:35" etc. | Use three hands (120° apart) to make it unreadable, OR draw actual answer time (11:47: hour at 353.5°, minute at 282°) |
| 23 | **Visual rendering claims need visual verification** — pixel math alone is insufficient | Write a temp test that saves `getHintCard()` to `/tmp/*.png`, run it, read the PNG with the image viewer, then delete the test before committing |

---

## 9. Git State

```
branch:      main
last commit: merge: ui-improvement → main — Phase 2 visual overhaul + hint system
remote:      git@github.com:avishekdas/game-java-sample.git
```

All work is on `main`. The `ui-improvement` branch has been merged and can be deleted if desired:
```bash
git branch -d ui-improvement
```

---

## 10. AP CS Rubric Coverage

| Concept | Phase 1 demonstration | Phase 2 additions |
|---------|----------------------|-------------------|
| Classes & Objects | Every entity in `com.abhishri.escape.domain` | `ThemeConstants`, `PuzzleDotsPanel` (inner class), `ItemCellRenderer` (inner class), `ThornwickScrollBarUI` (inner class) |
| Inheritance | `Puzzle` → 4 subclasses (JOINED, polymorphic `attempt()`); `AssetManager` → `FileAssetManager` + `PlaceholderAssetManager` | `ItemCellRenderer extends JPanel implements ListCellRenderer`; `ProceduralAssetManager implements AssetManager` adds third impl |
| ArrayLists | `solvedPuzzleIds`, `heldItemIds`, `connectedRoomIds`, `expectedSequence` | `hotspots` list in `ScenePanel`; `items` list in `InventoryPanel` |
| Loops | `SequencePuzzle.attempt()` iterates `expectedSequence` | `PuzzleDotsPanel.paintComponent()` loop over total dots; `ProceduralAssetManager` loops over items and rooms |
| Conditionals | Room adjacency check in `GameSessionService.move()` | Solved/hover state branching in `ScenePanel.paintComponent()`; puzzle type dispatch in `MainFrame.createPuzzleDialog()` |
| File I/O | `WorldSeedService` reads `world.json`; `SaveLoadService` writes/reads JSON save files | `FileAssetManager.getBackground()` reads PNG via `ImageIO.read()` from classpath |
| GUI | `MainFrame` (BorderLayout, 4 panels), `ScenePanel` (`paintComponent`, `MouseListener`), `StatusBar` (3 JButtons), `AssetManager` interface | `Graphics2D` with `RadialGradientPaint`, `AlphaComposite`, `RoundRectangle2D`; `javax.swing.Timer` for crossfade + hint card; `BasicScrollBarUI` subclass; custom `ListCellRenderer`; 4 procedural hint card images |
