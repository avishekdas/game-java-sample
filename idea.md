# Mystery Escape Room — Season 3: `idea.md`

---

## 1. Vision & Goals

Mystery Escape Room (Season 3) is a single-player point-and-click puzzle game where the player explores interconnected rooms, collects items, solves puzzles, and works toward a win condition — all from a desktop GUI. The gameplay follows the classic escape room loop: examine the environment, collect clues, combine knowledge, unlock the next area. The MVP targets three rooms, five to six puzzles, a persistent inventory, save/load support, and a clear win condition.

The pedagogical angle is the real point of this project. Rather than building a single monolithic Java program, this project deliberately separates concerns the way professional software does: a Spring Boot backend that owns all game logic, a Swing frontend that only renders state and sends player actions, and a clean JSON API boundary between them. A student who builds this project will have written a real HTTP client, a real REST API, JPA entities, and a multi-window Swing application — not a toy. Every pattern here exists in industry codebases; the difference is the complexity ceiling is deliberately held at AP CS level.

The AP CS rubric maps to this project naturally and completely. Classes and Objects appear everywhere. The `Puzzle` class hierarchy is the canonical Inheritance demonstration. ArrayLists appear in inventory management and room object listings. Loops appear in world-seeding, inventory traversal, and hotspot rendering. Conditionals appear in puzzle evaluation and game-state branching. File I/O is demonstrated twice: H2 embedded database writes to disk in file mode (backing the JPA entities), and `SaveLoadService` writes JSON snapshots to `./saves/` using Jackson, giving a human-readable, graded artifact. The Swing frontend is the GUI requirement.

---

## 2. Theme & Narrative

**Theme: "The Vanishing Librarian"**

Abhishri wakes up at midnight on the floor of Thornwick Municipal Library. The lights are dimmed, the front door is sealed with an electromagnetic lock, and Head Librarian Mira has vanished — leaving behind a note that reads only: *"The answer is always in the books."* As Abhishri explores the library, she discovers that Mira has been systematically hiding evidence of a rare manuscript's theft, and the only way out is to reconstruct the theft's timeline from the clues Mira left scattered across three rooms. The electromagnetic lock releases only when all evidence is assembled and the theft is reported via the library's antique pneumatic-tube terminal in the archives. No one is coming to help. The books are watching.

**The Three Rooms:**

| Room ID | Name | Contents |
|---|---|---|
| `room_foyer` | The Entry Foyer | Main entrance, sealed front door (win-condition lock), reception desk with locked drawer, umbrella stand, dusty welcome mat with a faded symbol, wall clock stopped at 11:47 |
| `room_reading_hall` | The Reading Hall | Rows of bookshelves, a librarian's rolling cart with books out of order, a combination-locked display case holding a brass magnifying glass, a reading lamp with a loose base, fireplace with cold ash |
| `room_archives` | The Archives | Filing cabinets, a pneumatic-tube terminal (win condition), a cipher wheel on the wall, a locked iron chest, a manuscript pedestal — empty |

**The Five Puzzles:**

| Puzzle ID | Type | One-line description |
|---|---|---|
| `puzzle_clock` | RiddlePuzzle | The stopped wall clock shows 11:47 — the answer to "what time does a library close?" unlocks a hidden compartment under the reception desk |
| `puzzle_bookshelf` | SequencePuzzle | Books on the cart must be reshelved in Dewey Decimal order; correct sequence opens the reading hall's fireplace compartment |
| `puzzle_display_case` | CombinationPuzzle | Three-digit combination lock on the display case; digits are hidden across room descriptions; correct combo yields the brass magnifying glass |
| `puzzle_cipher_wheel` | ItemUsePuzzle | Use the brass magnifying glass on the cipher wheel to reveal a decoded message; decoded message is the passphrase for the iron chest |
| `puzzle_iron_chest` | RiddlePuzzle | Enter the passphrase decoded from the cipher wheel; chest contains the manuscript page that proves the theft |
| `puzzle_terminal` | ItemUsePuzzle | Use the manuscript page on the pneumatic-tube terminal to transmit the evidence; this is the win condition |

Mentor can swap flavor — room names, puzzle text, item names — without changing mechanics. The engine is theme-agnostic below the seed data layer.

---

## 3. Player Experience (Golden Path Walkthrough)

1. Player launches the Swing application. The main window opens showing the Entry Foyer scene. The dialogue panel reads: *"You wake up on the cold floor. The clock on the wall reads 11:47. The front door is sealed."*

2. Player clicks the **wall clock** hotspot. The dialogue panel updates: *"The clock is stopped. The hands point to 11:47. A riddle is carved into the frame: 'When does the library's silence begin?'"* The riddle puzzle dialog opens.

3. Player types `11:47` (or the answer phrase configured in seed data) into the riddle dialog and submits. The server validates the answer, marks `puzzle_clock` as solved, and returns the updated game state. The dialogue panel reads: *"A compartment pops open beneath the reception desk. Something glints inside."*

4. Player clicks the **reception desk** hotspot (now unlocked). Server returns a `PICK_UP` action result. The item `desk_key` is added to the player's inventory. The inventory panel on the right refreshes to show the key icon.

5. Player clicks the doorway to **The Reading Hall**. The server processes the room-transition request and returns the Reading Hall state. The scene panel redraws with the Reading Hall background (or placeholder colored rectangle in early dev).

6. Player clicks the **display case** hotspot. The combination puzzle dialog opens with three digit spinners. Player has been reading room descriptions and noted the digits embedded in the flavor text. Player enters `3`, `8`, `4` and submits.

7. Server validates the combination, marks `puzzle_display_case` as solved. Response includes a new inventory item: `brass_magnifying_glass`. Dialogue reads: *"The case clicks open. The magnifying glass feels unusually heavy."*

8. Player clicks the **book cart** hotspot. The sequence puzzle dialog opens showing six book titles with Dewey numbers. Player drags them into correct order and submits. Server validates the sequence, unlocks the fireplace compartment. Dialogue: *"The books slide into place. Behind the fireplace grate, a loose brick reveals a scrap of paper: the first digit of a code."* (This is one of the three combination digits the player needed earlier — golden path assumes player does things in this order; non-linear exploration works too because the server tracks all state.)

9. Player navigates to **The Archives**. Scene shows the cipher wheel, the iron chest, the pneumatic-tube terminal (currently locked/inactive), and the manuscript pedestal (empty).

10. Player clicks the **cipher wheel** hotspot while `brass_magnifying_glass` is selected in the inventory panel. The server processes an `USE_ITEM` action with `{"itemId":"brass_magnifying_glass","targetObjectId":"cipher_wheel"}`. Server marks `puzzle_cipher_wheel` as solved and returns a clue: `passphrase = "THORNWICK"`.

11. Player clicks the **iron chest**. The riddle/passphrase dialog opens. Player types `THORNWICK`. Server validates, marks `puzzle_iron_chest` solved, adds `manuscript_page` to inventory. Dialogue: *"The chest opens. A torn manuscript page accuses someone by name."*

12. Player selects `manuscript_page` in the inventory panel, then clicks the **pneumatic-tube terminal** hotspot. Server processes `USE_ITEM` with manuscript page on terminal. This is the final puzzle. Server checks: all five previous puzzles solved, manuscript page in inventory — win condition met.

13. Server sets `GameSession.status = COMPLETE`. Response DTO includes `"gameStatus":"WIN"`. The Swing client detects the WIN status and opens a congratulations dialog: *"The tube hisses. Evidence received. The front door's electromagnetic lock disengages. Mira's secret is exposed. You're free."*

14. Player closes the app or chooses "New Game." If they had previously used the save menu, their progress was serialized to `./saves/<gameId>-<timestamp>.json` and can be reloaded.

---

## 4. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SWING CLIENT (frontend/)                     │
│                                                                     │
│   MainFrame                                                         │
│   ├── ScenePanel        (renders room state, clickable hotspots)    │
│   ├── InventoryPanel    (renders player inventory)                  │
│   ├── DialoguePanel     (shows server messages / flavor text)       │
│   └── PuzzleDialog(s)   (per-puzzle input dialogs)                  │
│                                                                     │
│   GameApiClient         (java.net.http.HttpClient + Jackson)        │
└───────────────────────┬─────────────────────────────────────────────┘
                        │  HTTP/JSON  (port 8080)
                        │  POST/GET requests  ←  all state is in body/path
                        │  No cookies, no HTTP session
                        │
┌───────────────────────▼─────────────────────────────────────────────┐
│                  SPRING BOOT SERVER (backend/)                      │
│                                                                     │
│   @RestController(s)    (receive HTTP, delegate to services)        │
│   @Service(s)           (business logic: puzzle eval, inventory...) │
│   @Repository(s)        (Spring Data JPA interfaces)                │
│   @Entity classes       (GameSession, Room, Puzzle subtypes, ...)   │
└───────────────────────┬─────────────────────────────────────────────┘
                        │  JPA / JDBC
                        │
┌───────────────────────▼─────────────────────────────────────────────┐
│              H2 EMBEDDED DATABASE (file mode)                       │
│              ./data/escaperoom  (writes to disk)                    │
│              + ./saves/<gameId>-<timestamp>.json  (JSON snapshots)  │
└─────────────────────────────────────────────────────────────────────┘
```

**Swing Client layer** renders game state received from the server and sends player actions (clicks, dialog submissions) as JSON over HTTP. It holds no game logic. It does not know puzzle rules, win conditions, or item effects — it only knows how to draw what the server sends and which API endpoint to call for each player gesture.

**Spring Boot Server layer** owns everything: room definitions, puzzle rules, inventory state, win-condition checking, and save/load. Every game state change is initiated by an HTTP request, validated here, and persisted before a response is sent. The server is stateless per-request — there is no HTTP session object; the `gameId` in every request is the only identity token.

**H2 + JPA layer** persists all entities to `./data/escaperoom.mv.db` on disk. Spring Data JPA (a library that generates SQL for you based on Java interfaces) makes this nearly invisible from service-layer code, which is appropriate for AP CS level. The important teaching point is that the data survives a server restart.

**Future React boundary:** Any client that can make HTTP requests and parse JSON can replace the Swing frontend with zero backend changes. The API contract is the entire interface. A React SPA running on port 3000 would call the same endpoints with the same JSON bodies. This boundary is enforced by a rule: no `javax.swing` or AWT type ever enters a DTO, service, or controller class.

---

## 5. Backend Design

### 5a. Domain Model (JPA Entities & Class Hierarchy)

**Inheritance hierarchy (the AP CS Inheritance requirement):**

```
Puzzle  (abstract @Entity, @Inheritance(strategy=JOINED))
├── CombinationPuzzle    (digits, expectedCode)
├── RiddlePuzzle         (questionText, expectedAnswer, caseSensitive)
├── SequencePuzzle       (itemIds: ArrayList<String>, expectedSequence: ArrayList<String>)
└── ItemUsePuzzle        (requiredItemId, targetObjectId, outcomeMessage)
```

`Puzzle` is an abstract class. It cannot be instantiated directly. Each subclass adds the fields specific to its puzzle type. `PuzzleEvaluationService` calls `puzzle.attempt(PlayerAction action)` and each subclass implements that method differently — this is polymorphism, the practical payoff of inheritance.

**JPA inheritance strategy: `JOINED` (decided).** H2 generates one `PUZZLE` parent table plus four child tables (`COMBINATION_PUZZLE`, `RIDDLE_PUZZLE`, `SEQUENCE_PUZZLE`, `ITEM_USE_PUZZLE`), each child storing only its subclass-specific columns and joined back to the parent on the shared primary key. This costs one extra `JOIN` per query but produces a schema where the inheritance hierarchy is immediately visible in the database — useful for the grading discussion and for Abhishri to point at when explaining the Inheritance concept.

---

**`GameSession`** — represents one player's play session, the root aggregate.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key, generated on `POST /api/game/new` |
| `currentRoomId` | `String` | FK reference to `Room.id` |
| `status` | `GameStatus` (enum) | `IN_PROGRESS`, `COMPLETE`, `FAILED` |
| `createdAt` | `LocalDateTime` | |
| `lastUpdatedAt` | `LocalDateTime` | |
| `inventory` | `@OneToOne PlayerInventory` | The player's held items |
| `solvedPuzzleIds` | `@ElementCollection List<String>` | IDs of solved puzzles (ArrayList rubric item) |

*Rubric: Classes & Objects, ArrayLists (`solvedPuzzleIds`), Conditionals (status checks)*

---

**`Room`** — a room definition, shared across all game sessions (immutable world data seeded once).

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | e.g., `"room_foyer"` |
| `name` | `String` | Display name |
| `description` | `String` | Flavor text shown on room entry |
| `connectedRoomIds` | `@ElementCollection List<String>` | Navigable exits |
| `objectIds` | `@ElementCollection List<String>` | Examinable/interactable object IDs in this room |
| `puzzleIds` | `@ElementCollection List<String>` | Puzzles available in this room |

*Rubric: Classes & Objects, ArrayLists (`connectedRoomIds`, `objectIds`)*

---

**`Puzzle`** (abstract `@Entity` with `@Inheritance(strategy = InheritanceType.JOINED)`)

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | e.g., `"puzzle_clock"` |
| `roomId` | `String` | Which room this puzzle lives in |
| `description` | `String` | Shown to player when they interact |
| `rewardItemId` | `String` | Item added to inventory on solve (nullable) |
| `prerequisitePuzzleIds` | `@ElementCollection List<String>` | Must be solved first (ArrayList rubric item) |

Abstract method: `boolean attempt(Map<String, String> playerInput)` — each subclass overrides this.

*Rubric: Inheritance (`Puzzle` → subclasses), Classes & Objects, Conditionals inside `attempt()`*

---

**`CombinationPuzzle extends Puzzle`**

| Field | Type | Notes |
|---|---|---|
| `expectedCode` | `String` | e.g., `"384"` |
| `digitCount` | `int` | |

`attempt()`: checks if `playerInput.get("code").equals(expectedCode)`.

---

**`RiddlePuzzle extends Puzzle`**

| Field | Type | Notes |
|---|---|---|
| `questionText` | `String` | |
| `expectedAnswer` | `String` | |
| `caseSensitive` | `boolean` | |

`attempt()`: normalizes and compares the player's submitted text.

---

**`SequencePuzzle extends Puzzle`**

| Field | Type | Notes |
|---|---|---|
| `expectedSequence` | `@ElementCollection List<String>` | Ordered list of item/book IDs |
| `availableItems` | `@ElementCollection List<String>` | What the player can arrange |

`attempt()`: iterates `expectedSequence` against submitted list with a loop — explicit rubric Loops demonstration.

---

**`ItemUsePuzzle extends Puzzle`**

| Field | Type | Notes |
|---|---|---|
| `requiredItemId` | `String` | Item that must be in inventory |
| `targetObjectId` | `String` | Object being used on |
| `outcomeMessage` | `String` | Flavor text shown on success |

`attempt()`: checks inventory contains `requiredItemId` AND target matches.

---

**`InventoryItem`** — an item definition (immutable world data).

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | e.g., `"brass_magnifying_glass"` |
| `name` | `String` | Display name |
| `description` | `String` | Shown when examined |
| `assetKey` | `String` | Key into `AssetManager` for the icon |

---

**`PlayerInventory`** — a player's current held items, tied to one `GameSession`.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key |
| `gameSessionId` | `UUID` | FK to `GameSession` |
| `heldItemIds` | `@ElementCollection List<String>` | Item IDs currently held |

*Rubric: ArrayLists (`heldItemIds`), Loops (iterating items to render/check)*

---

### 5b. Repository Layer

Each interface extends `JpaRepository<Entity, IdType>`. Spring Data JPA generates the SQL implementation at runtime — nothing to code, just declare the interface.

| Interface | Manages |
|---|---|
| `GameSessionRepository` | `GameSession` by UUID |
| `RoomRepository` | `Room` by String ID |
| `PuzzleRepository` | `Puzzle` (and all subclasses) by String ID |
| `PlayerInventoryRepository` | `PlayerInventory` by UUID |
| `InventoryItemRepository` | `InventoryItem` by String ID |

Custom finder examples worth adding:
- `PuzzleRepository.findByRoomId(String roomId)` — returns all puzzles in a room
- `GameSessionRepository.findByStatus(GameStatus status)` — utility for admin/debug

---

### 5c. Service Layer

| Service | Responsibility |
|---|---|
| `GameSessionService` | Creates new sessions, fetches current state, validates room transitions, assembles the `GameStateDTO` response that every endpoint returns |
| `PuzzleEvaluationService` | Loads the correct `Puzzle` subclass, calls `puzzle.attempt()`, checks prerequisites, marks puzzle solved in `GameSession.solvedPuzzleIds`, awards reward items |
| `InventoryService` | Adds/removes items from `PlayerInventory`, validates item existence, returns current inventory list |
| `SaveLoadService` | Serializes a full `GameSession` snapshot (entities + inventory) to `./saves/<gameId>-<timestamp>.json` using Jackson `ObjectMapper`; can deserialize and rehydrate the session into H2 |
| `WorldSeedService` | Runs once on application startup (`@PostConstruct`); reads `src/main/resources/world.json` via Jackson `ObjectMapper`, then inserts all Room, Puzzle, and InventoryItem definitions into H2 if they don't already exist. Editing `world.json` (riddle text, combination codes, item names) lets Abhishri iterate on content without recompiling. |

---

### 5d. REST API

All requests and responses are `Content-Type: application/json`. The `gameId` is a UUID string. All endpoints return a `GameStateDTO` (or an error DTO) — the client always gets a full picture of the current state, not a partial delta.

**Endpoint table:**

| Method | Path | Request Body | Response | Purpose |
|---|---|---|---|---|
| `POST` | `/api/game/new` | *(empty)* | `GameStateDTO` | Creates new session, seeds inventory, returns initial state |
| `GET` | `/api/game/{gameId}` | — | `GameStateDTO` | Fetch current state (for load / reconnect) |
| `POST` | `/api/game/{gameId}/move` | `MoveRequest` | `GameStateDTO` | Move player to an adjacent room |
| `POST` | `/api/game/{gameId}/examine` | `ExamineRequest` | `GameStateDTO` | Examine an object; returns flavor text + any triggered events |
| `POST` | `/api/game/{gameId}/pickup` | `PickupRequest` | `GameStateDTO` | Add item to inventory |
| `POST` | `/api/game/{gameId}/use-item` | `UseItemRequest` | `GameStateDTO` | Use inventory item on a target object (triggers `ItemUsePuzzle` if applicable) |
| `POST` | `/api/game/{gameId}/attempt-puzzle` | `AttemptPuzzleRequest` | `GameStateDTO` | Submit puzzle answer; returns success/failure + updated state |
| `POST` | `/api/game/{gameId}/save` | *(empty)* | `SaveConfirmationDTO` | Write JSON snapshot to `./saves/` |
| `GET` | `/api/game/{gameId}/saves` | — | `List<SaveMetadataDTO>` | List available save files for this session |
| `POST` | `/api/game/{gameId}/load` | `LoadRequest` | `GameStateDTO` | Rehydrate session from a named save file |
| `GET` | `/api/health` | — | `{"status":"ok"}` | Verify server is up (first smoke test) |

---

**`GameStateDTO`** — the universal response. The Swing client renders from this.

```json
{
  "gameId": "a3f7c2d1-...",
  "gameStatus": "IN_PROGRESS",
  "currentRoom": {
    "id": "room_reading_hall",
    "name": "The Reading Hall",
    "description": "Rows of shelves reach the ceiling...",
    "objects": [
      { "id": "display_case", "label": "Display Case", "interactable": true, "puzzleId": "puzzle_display_case" },
      { "id": "book_cart",    "label": "Book Cart",    "interactable": true, "puzzleId": "puzzle_bookshelf" }
    ],
    "exits": ["room_foyer", "room_archives"]
  },
  "inventory": [
    { "id": "desk_key", "name": "Desk Key", "description": "A small brass key.", "assetKey": "item_key" }
  ],
  "solvedPuzzleIds": ["puzzle_clock"],
  "dialogueMessage": "The reading hall is cold. Somewhere, a book falls off a shelf.",
  "lastActionResult": "MOVE_SUCCESS"
}
```

---

**`AttemptPuzzleRequest`** — for combination and riddle puzzles:

```json
{
  "puzzleId": "puzzle_display_case",
  "inputs": {
    "code": "384"
  }
}
```

For a sequence puzzle:
```json
{
  "puzzleId": "puzzle_bookshelf",
  "inputs": {
    "sequence": "510,520,610,621,720,810"
  }
}
```

---

**`UseItemRequest`** — for ItemUsePuzzle:

```json
{
  "itemId": "brass_magnifying_glass",
  "targetObjectId": "cipher_wheel"
}
```

Server response on success would include `lastActionResult: "PUZZLE_SOLVED"`, the reward item in `inventory`, and flavor text in `dialogueMessage`.

---

### 5e. Save/Load and File I/O

`SaveLoadService` is the explicit File I/O rubric demonstration. When `POST /api/game/{gameId}/save` is called:

1. `GameSessionService` fetches the full `GameSession` aggregate from JPA (session + inventory + solved puzzles).
2. `SaveLoadService.save(GameSession session)` constructs a `GameSnapshotDTO` (a plain Java object mirroring the entity state).
3. Jackson's `ObjectMapper.writeValue(file, snapshot)` writes the object to `./saves/<gameId>-<timestamp>.json`. The directory is created with `Files.createDirectories()` if it does not exist.
4. The filename is returned in `SaveConfirmationDTO.filename`.

On load, `SaveLoadService.load(String filename)` reads the file with `ObjectMapper.readValue(file, GameSnapshotDTO.class)`, then writes each field back into the H2 database via the repositories. The player's session resumes exactly where it was.

The saves directory contents are human-readable JSON files — this is the graded File I/O artifact. The file writing uses `java.io.File` and `java.nio.file.Files`, which are the standard AP CS File I/O classes.

H2 in file mode (`spring.datasource.url=jdbc:h2:file:./data/escaperoom`) independently persists entity data to `./data/escaperoom.mv.db` on disk. This means the game survives a server restart without needing a manual save. The JSON snapshot is a portable, human-readable backup layer on top.

**`GameSnapshotDTO` scope (confirmed).** The snapshot captures only **mutable session state** — `gameId`, `currentRoomId`, `status`, `createdAt`, `lastUpdatedAt`, `solvedPuzzleIds`, and `heldItemIds`. **Immutable world data** (Rooms, Puzzles, InventoryItems) is *not* serialized — it is reseeded from `world.json` into H2 at startup and remains identical for every save. On load, the snapshot's mutable fields are written back into H2, and the world data already there is sufficient to render the room and inventory correctly. This keeps save files small and human-readable, and means an old save still loads against an updated `world.json` as long as the puzzle/item/room IDs referenced in the save still exist.

---

### 5f. CORS and Future-React Note

In a `WebMvcConfigurer` bean in `BackendConfig.java`:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:3000", "http://localhost:5173")
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*");
```

This allows a React development server (Vite default: 5173, CRA default: 3000) to call the backend from a browser without CORS errors. The Swing client, making direct `HttpClient` calls from a JVM process, is not subject to CORS — the configuration costs nothing for the Swing phase and unblocks the React phase entirely. No other backend changes are required to support a React frontend.

---

## 6. Frontend Design (Swing)

### 6a. Main Window Layout (ASCII)

```
┌─────────────────────────────────────────────────────────────────────┐
│  THORNWICK LIBRARY  [Room: The Reading Hall]   [Save] [Load] [New]  │  ← StatusBar (JPanel)
├──────────────────────────────────────────┬──────────────────────────┤
│                                          │  INVENTORY               │
│                                          │  ┌────────┐ ┌────────┐  │
│          SCENE PANEL                     │  │  Key   │ │  Lens  │  │
│     (paintComponent draws room           │  └────────┘ └────────┘  │
│      background image or colored         │                          │
│      placeholder rectangle)             │  ┌────────┐              │
│                                          │  │  ...   │              │
│   [clickable hotspot rectangles          │  └────────┘              │
│    labeled: "Display Case",              │                          │
│    "Book Cart", "Exit → Foyer"]          │  Selected:               │
│                                          │  Brass Magnifying Glass  │
│                                          │  [Use on Target]         │
├──────────────────────────────────────────┴──────────────────────────┤
│  DIALOGUE PANEL                                                     │
│  "The case clicks open. The magnifying glass feels unusually        │
│   heavy. It is added to your inventory."                            │
└─────────────────────────────────────────────────────────────────────┘
```

The window is a single `JFrame` (`MainFrame`) using `BorderLayout`. The status bar sits at `NORTH`. The scene panel sits at `CENTER`. The inventory panel sits at `EAST` with a fixed preferred width of ~200px. The dialogue panel sits at `SOUTH` as a scrollable text area.

---

### 6b. Swing Class Structure

| Class | Extends | Responsibility |
|---|---|---|
| `MainFrame` | `JFrame` | Top-level window; assembles all panels; holds `GameApiClient`; dispatches server responses to child panels via `renderState(GameStateDTO)` |
| `ScenePanel` | `JPanel` | Overrides `paintComponent` to draw room background + hotspot rectangles; maintains a `List<Hotspot>` for click detection; fires action callbacks to `MainFrame` on click |
| `InventoryPanel` | `JPanel` | Renders inventory items as a vertical list of icon+label buttons; tracks selected item; exposes `getSelectedItemId()` |
| `DialoguePanel` | `JPanel` | Wraps a `JTextArea` in a `JScrollPane`; exposes `append(String message)` |
| `StatusBar` | `JPanel` | `JLabel` showing current room name + game status |
| `PuzzleDialog` | `JDialog` (abstract) | Base dialog with confirm/cancel; each subclass builds its specific input UI |
| `CombinationPuzzleDialog` | `PuzzleDialog` | Row of `JSpinner` widgets (one per digit) |
| `RiddlePuzzleDialog` | `PuzzleDialog` | `JLabel` (question) + `JTextField` (answer) |
| `SequencePuzzleDialog` | `PuzzleDialog` | **Phase 1 target:** drag-and-drop list (`JList` + `TransferHandler`) or `DefaultListModel<String>` with move-up/move-down buttons. **Fallback if it blocks timeline:** simplify `puzzle_bookshelf` to a `RiddlePuzzle` whose `expectedAnswer` is the correct Dewey sequence as a comma-separated string (e.g., `"510,520,610,621,720,810"`). Mechanically equivalent, no new dialog class needed. Switch is server-side only (swap the puzzle's `DTYPE` in `world.json`); frontend already supports `RiddlePuzzleDialog`. |
| `ItemUsePuzzleDialog` | Not needed — fired automatically when player uses a selected item on a hotspot | (No dialog; `MainFrame` calls `GameApiClient.useItem()` directly) |
| `GameApiClient` | — | Encapsulates all HTTP communication; exposes methods `newGame()`, `getState(UUID)`, `move(UUID, String)`, `examine(UUID, String)`, `pickup(UUID, String)`, `useItem(UUID, String, String)`, `attemptPuzzle(UUID, String, Map)`, `save(UUID)`, `load(UUID, String)`; each method returns a `GameStateDTO` |

`GameApiClient` uses `java.net.http.HttpClient` (standard library, Java 11+) to send HTTP requests and Jackson `ObjectMapper` to deserialize responses. No third-party HTTP library is needed.

**Threading (decided):** Phase 1 calls `HttpClient.send()` synchronously on the Swing Event Dispatch Thread. Against `localhost:8080` the round-trip is sub-millisecond and the UI freeze is imperceptible. `SwingWorker` and async background calls are explicitly deferred to Phase 2 — they would add concurrency concepts beyond AP CS scope and provide no user-visible benefit on localhost.

---

### 6c. Asset Loader

`AssetManager` is an interface:

```java
public interface AssetManager {
    ImageIcon getRoomBackground(String roomId);
    ImageIcon getItemIcon(String assetKey);
    ImageIcon getHotspotOverlay(String objectId);
}
```

`FileAssetManager implements AssetManager` loads PNG files from `/resources/art/<theme>/<filename>.png` using `ImageIO.read()`. If a file is missing, it falls through to the placeholder.

`PlaceholderAssetManager implements AssetManager` ignores file loading entirely. `getRoomBackground()` returns a colored `BufferedImage` with the room name painted in large text. `getItemIcon()` returns a small colored rectangle with the item name. This class is used during all of Phase 1 development. Swapping in real art is a single line change in `MainFrame` where the `AssetManager` is instantiated.

`MainFrame` holds a reference typed as `AssetManager` (the interface), not as either concrete class. `ScenePanel` and `InventoryPanel` receive the interface reference. Neither panel ever calls a method that only exists on one implementation. This is the abstraction that isolates art from game code — changing art assets requires no changes to any game logic class.

---

### 6d. Action to API Call Mapping

| Player gesture | Swing event | API call | Server response used |
|---|---|---|---|
| Click "Examine display case" hotspot | `ScenePanel` mouse click detected on hotspot rect | `POST /api/game/{gameId}/examine` `{"objectId":"display_case"}` | `dialogueMessage` shown in `DialoguePanel` |
| Click "Pick up desk key" hotspot | Same as above, object type is ITEM | `POST /api/game/{gameId}/pickup` `{"objectId":"desk_key"}` | `inventory` refreshes `InventoryPanel` |
| Click exit hotspot "→ Archives" | Hotspot type is EXIT | `POST /api/game/{gameId}/move` `{"targetRoomId":"room_archives"}` | `currentRoom` rerenders `ScenePanel` |
| Click puzzle hotspot (display case) | Opens matching `PuzzleDialog` subclass | *(on dialog confirm)* `POST /api/game/{gameId}/attempt-puzzle` | `solvedPuzzleIds` updated, `inventory` updated, `dialogueMessage` set |
| Select item in inventory, click object hotspot | `InventoryPanel.getSelectedItemId()` non-null, hotspot clicked | `POST /api/game/{gameId}/use-item` `{"itemId":"...","targetObjectId":"..."}` | Same full `GameStateDTO` re-render |
| Click "Save" in status bar | `JButton` `ActionListener` | `POST /api/game/{gameId}/save` | `dialogueMessage` shows save filename confirmation |

After every API call, `MainFrame.renderState(GameStateDTO dto)` is called. It pushes the new state to all child panels: `ScenePanel.setRoom(dto.currentRoom)`, `InventoryPanel.setItems(dto.inventory)`, `DialoguePanel.append(dto.dialogueMessage)`, `StatusBar.update(dto)`. This one method is the entire "re-render" logic.

---

## 7. Data Flow Example: The Combination Lock Puzzle

The player is in the Reading Hall. They click the **Display Case** hotspot. `ScenePanel` detects the click, checks the hotspot's metadata (`puzzleId = "puzzle_display_case"`, `puzzleType = "COMBINATION"`), and opens a `CombinationPuzzleDialog`.

The dialog shows three `JSpinner` widgets. The player sets them to `3`, `8`, `4` and clicks Confirm.

**Swing → API:**
```
GameApiClient.attemptPuzzle(gameId, "puzzle_display_case", {"code": "384"})
→ POST /api/game/{gameId}/attempt-puzzle
  Body: { "puzzleId": "puzzle_display_case", "inputs": { "code": "384" } }
```

**Controller:**
```java
// GameController.java
@PostMapping("/{gameId}/attempt-puzzle")
public ResponseEntity<GameStateDTO> attemptPuzzle(
        @PathVariable UUID gameId,
        @RequestBody AttemptPuzzleRequest request) {
    GameStateDTO state = puzzleEvaluationService.evaluate(gameId, request);
    return ResponseEntity.ok(state);
}
```

**Service:**
```java
// PuzzleEvaluationService.java
GameSession session = gameSessionRepository.findById(gameId)  // load from H2
Puzzle puzzle = puzzleRepository.findById(request.puzzleId)   // load CombinationPuzzle
// check prerequisites: session.solvedPuzzleIds.containsAll(puzzle.prerequisitePuzzleIds)
boolean solved = puzzle.attempt(request.inputs)               // "384".equals(expectedCode)
if (solved) {
    session.solvedPuzzleIds.add(puzzle.id)                    // ArrayList.add()
    inventoryService.addItem(session, puzzle.rewardItemId)    // adds brass_magnifying_glass
    session.lastUpdatedAt = LocalDateTime.now()
    gameSessionRepository.save(session)                       // JPA persists to H2
}
return gameSessionService.buildStateDTO(session)              // assemble full response
```

**`CombinationPuzzle.attempt()`:**
```java
@Override
public boolean attempt(Map<String, String> inputs) {
    String submitted = inputs.get("code");
    if (submitted == null) return false;
    return this.expectedCode.equals(submitted);   // Conditional
}
```

**Response DTO (JSON):**
```json
{
  "gameId": "a3f7c2d1-...",
  "gameStatus": "IN_PROGRESS",
  "currentRoom": { "id": "room_reading_hall", ... },
  "inventory": [
    { "id": "desk_key", ... },
    { "id": "brass_magnifying_glass", "name": "Brass Magnifying Glass", "assetKey": "item_lens" }
  ],
  "solvedPuzzleIds": ["puzzle_clock", "puzzle_display_case"],
  "dialogueMessage": "The case clicks open. The magnifying glass feels unusually heavy.",
  "lastActionResult": "PUZZLE_SOLVED"
}
```

**Swing re-render:**
`GameApiClient` deserializes the JSON into `GameStateDTO`. `MainFrame.renderState()` is called. `InventoryPanel` iterates `dto.inventory` (a loop over an ArrayList) and repaints — the magnifying glass icon appears. `DialoguePanel.append()` adds the flavor text. `CombinationPuzzleDialog` closes. The hotspot for the display case is marked visually as inactive (server included the solved state).

---

## 8. Project Structure

```
game-java-dev/
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/abhishri/escape/
│           │   ├── EscapeRoomApplication.java       (Spring Boot main)
│           │   ├── config/
│           │   │   └── BackendConfig.java            (CORS, seed trigger)
│           │   ├── controller/
│           │   │   └── GameController.java
│           │   ├── service/
│           │   │   ├── GameSessionService.java
│           │   │   ├── PuzzleEvaluationService.java
│           │   │   ├── InventoryService.java
│           │   │   ├── SaveLoadService.java
│           │   │   └── WorldSeedService.java
│           │   ├── domain/
│           │   │   ├── GameSession.java              (@Entity)
│           │   │   ├── Room.java                     (@Entity)
│           │   │   ├── puzzle/
│           │   │   │   ├── Puzzle.java               (abstract @Entity)
│           │   │   │   ├── CombinationPuzzle.java
│           │   │   │   ├── RiddlePuzzle.java
│           │   │   │   ├── SequencePuzzle.java
│           │   │   │   └── ItemUsePuzzle.java
│           │   │   ├── InventoryItem.java            (@Entity)
│           │   │   └── PlayerInventory.java          (@Entity)
│           │   ├── repository/
│           │   │   ├── GameSessionRepository.java
│           │   │   ├── RoomRepository.java
│           │   │   ├── PuzzleRepository.java
│           │   │   ├── PlayerInventoryRepository.java
│           │   │   └── InventoryItemRepository.java
│           │   └── dto/
│           │       ├── GameStateDTO.java
│           │       ├── RoomDTO.java
│           │       ├── RoomObjectDTO.java
│           │       ├── InventoryItemDTO.java
│           │       ├── AttemptPuzzleRequest.java
│           │       ├── MoveRequest.java
│           │       ├── ExamineRequest.java
│           │       ├── PickupRequest.java
│           │       ├── UseItemRequest.java
│           │       ├── LoadRequest.java
│           │       ├── SaveConfirmationDTO.java
│           │       ├── SaveMetadataDTO.java
│           │       └── GameSnapshotDTO.java          (for file serialization)
│           └── resources/
│               └── application.properties
│
├── frontend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/abhishri/escape/ui/
│           │   ├── EscapeRoomApp.java                (main() entry point)
│           │   ├── MainFrame.java
│           │   ├── ScenePanel.java
│           │   ├── InventoryPanel.java
│           │   ├── DialoguePanel.java
│           │   ├── StatusBar.java
│           │   ├── hotspot/
│           │   │   └── Hotspot.java                  (data class: bounds, objectId, type)
│           │   ├── dialog/
│           │   │   ├── PuzzleDialog.java             (abstract JDialog)
│           │   │   ├── CombinationPuzzleDialog.java
│           │   │   ├── RiddlePuzzleDialog.java
│           │   │   └── SequencePuzzleDialog.java
│           │   ├── api/
│           │   │   └── GameApiClient.java
│           │   └── asset/
│           │       ├── AssetManager.java             (interface)
│           │       ├── FileAssetManager.java
│           │       └── PlaceholderAssetManager.java
│           └── resources/
│               └── art/
│                   └── thornwick/                    (placeholder: empty, filled in Phase 2)
│
├── saves/                                            (runtime, git-ignored)
├── data/                                             (H2 file DB, git-ignored)
├── idea.md
└── README.md
```

**Build tool: Maven.** Gradle is more powerful but Maven's `pom.xml` is more common in AP CS coursework, Spring Boot's official start.spring.io generates Maven by default, and the XML structure (despite being verbose) is easier to explain to a student line by line. Multi-module Maven (`parent pom.xml` with `backend` and `frontend` as child modules) keeps one `mvn install` from the root building both.

**Shared DTO module: duplicate, do not create a `shared-dto` module.** A third Maven module introduces multi-module project mechanics (parent POMs, inter-module dependencies) that add cognitive overhead for no Phase 1 benefit. Duplicate the DTO classes in `backend/dto/` and `frontend/api/dto/`. Keep them identical by hand during Phase 1. If they drift — or when the React port happens — revisit. The rule "frontend is a thin client" means the DTO surface area stays small anyway.

---

## 9. AP CS Rubric Coverage Matrix

| Concept | Where it shows up |
|---|---|
| Classes & Objects | Every file in the project; most concretely: `GameSession`, `Room`, `InventoryItem`, `Hotspot`, `GameApiClient` — each is a class instantiated into objects with state and behavior |
| Inheritance | `Puzzle` (abstract) → `CombinationPuzzle`, `RiddlePuzzle`, `SequencePuzzle`, `ItemUsePuzzle`; `PuzzleDialog` (abstract `JDialog`) → `CombinationPuzzleDialog`, `RiddlePuzzleDialog`, `SequencePuzzleDialog`; `AssetManager` interface → `FileAssetManager`, `PlaceholderAssetManager` |
| ArrayLists | `GameSession.solvedPuzzleIds` (`List<String>`); `PlayerInventory.heldItemIds` (`List<String>`); `Room.connectedRoomIds`, `Room.objectIds`; `SequencePuzzle.expectedSequence`; `ScenePanel`'s `List<Hotspot>`; inventory iteration in `InventoryPanel` |
| Loops | `SequencePuzzle.attempt()` iterates expected vs submitted sequence; `WorldSeedService` loops over seed data lists to insert rooms/puzzles; `InventoryPanel` loops over `dto.inventory` to render item buttons; `PuzzleEvaluationService` loops over `prerequisitePuzzleIds` to check prerequisites |
| Conditionals | `Puzzle.attempt()` implementations in every subclass; `GameSessionService` win-condition check (`if allPuzzlesSolved → status = COMPLETE`); `PuzzleEvaluationService` prerequisite gate; `MainFrame.renderState()` branching on `gameStatus` to show win screen |
| File I/O | H2 file mode (`jdbc:h2:file:./data/escaperoom`) persists all JPA entities to disk on every write; `SaveLoadService` writes human-readable JSON snapshots to `./saves/<gameId>-<timestamp>.json` using `ObjectMapper.writeValue(File, Object)` and reads with `ObjectMapper.readValue(File, Class)` using `java.io.File` and `java.nio.file.Files.createDirectories()` |
| GUI | All `javax.swing.*` classes in `frontend/`: `MainFrame` (`JFrame`), `ScenePanel` (`JPanel` + `paintComponent`), `InventoryPanel` (`JPanel` + `JButton` grid), `DialoguePanel` (`JTextArea` + `JScrollPane`), `CombinationPuzzleDialog` (`JDialog` + `JSpinner`), `RiddlePuzzleDialog` (`JDialog` + `JTextField`), `SequencePuzzleDialog` (`JDialog` + `JList`) |

---

## 10. Phase 1 (MVP) Build Order

1. **Initialize backend Maven project.** Use start.spring.io with Spring Web, Spring Data JPA, H2. Verify `GET /api/health` returns `{"status":"ok"}`. Confirm H2 console accessible at `/h2-console`.

2. **Define `GameSession` entity and `GameSessionRepository`.** Configure `application.properties` for H2 file mode (`jdbc:h2:file:./data/escaperoom`). Verify a test record persists and survives server restart.

3. **Define `Room` entity and `RoomRepository`.** No service logic yet — just confirm JPA mapping compiles and H2 schema generates.

4. **Define `Puzzle` abstract entity and all four concrete subclasses** with `@Inheritance(strategy = InheritanceType.JOINED)`. Verify JPA generates five tables in H2: a parent `PUZZLE` table holding the shared columns plus four child tables (`COMBINATION_PUZZLE`, `RIDDLE_PUZZLE`, `SEQUENCE_PUZZLE`, `ITEM_USE_PUZZLE`) joined on the shared primary key.

5. **Define `InventoryItem` and `PlayerInventory` entities** with repositories. Verify the `@ElementCollection` list mapping for `heldItemIds` generates a join table in H2.

6. **Implement `WorldSeedService` driven by `world.json`.** Create `src/main/resources/world.json` containing the three Thornwick rooms, six puzzles (with `type` discriminator: `COMBINATION` / `RIDDLE` / `SEQUENCE` / `ITEM_USE`), and all inventory item definitions. On `@PostConstruct`, load the file via Jackson `ObjectMapper`, deserialize into seed POJOs, and insert into H2 if not present. Verify via H2 console that seed data appears on startup. Verify editing `world.json` (e.g., changing a riddle answer) and restarting the server picks up the change — without recompiling.

7. **Implement `GameSessionService.createNewGame()` and `POST /api/game/new`.** Returns a `GameStateDTO` with the player starting in `room_foyer` with an empty inventory. Test with `curl` or Postman.

8. **Implement `POST /api/game/{gameId}/move`.** `GameSessionService` validates the target room is adjacent, updates `currentRoomId`, returns updated `GameStateDTO`. Test all valid and invalid room transitions manually.

9. **Implement `POST /api/game/{gameId}/examine` and `POST /api/game/{gameId}/pickup`.** Examine returns flavor text. Pickup adds item to `PlayerInventory` via `InventoryService`. Test the `desk_key` pickup flow.

10. **Implement `PuzzleEvaluationService` and `POST /api/game/{gameId}/attempt-puzzle`.** Start with `CombinationPuzzle` and `RiddlePuzzle` only. Verify: correct answer marks puzzle solved, adds reward item to inventory, persists. Verify: wrong answer returns failure message, state unchanged.

11. **Implement `ItemUsePuzzle` evaluation in `PuzzleEvaluationService` and `POST /api/game/{gameId}/use-item`.** Test the magnifying-glass-on-cipher-wheel interaction. Implement `SequencePuzzle` evaluation. Test book-cart ordering.

12. **Implement win condition check** in `GameSessionService.buildStateDTO()`: if `solvedPuzzleIds` contains all six puzzle IDs, set `GameStatus.COMPLETE`. Test by solving all puzzles via API calls in sequence.

13. **Implement `SaveLoadService` and `POST /api/game/{gameId}/save` + load endpoints.** Verify a save file appears in `./saves/`. Verify a loaded session restores correct room, inventory, and solved puzzles. This is the File I/O grading checkpoint.

14. **Configure CORS** in `BackendConfig.java`. Test from a browser `fetch()` call (or Postman browser agent) to confirm no CORS errors.

15. **Initialize frontend Maven project.** Single `main()` launches `MainFrame` with `PlaceholderAssetManager`. Verify the window opens with correctly sized panels and colored placeholder rectangles for the foyer.

16. **Implement `GameApiClient`** using `java.net.http.HttpClient`. Implement `newGame()` first. Call it from `MainFrame` constructor and render the returned `GameStateDTO`. Verify room name appears in status bar and placeholder rectangle draws.

17. **Implement hotspot click → examine/pickup/move flow end-to-end.** Hard-code the foyer hotspots for this step. Verify clicking the clock hotspot triggers the API call and `DialoguePanel` updates.

18. **Implement all `PuzzleDialog` subclasses** and wire them to hotspot clicks. Test `CombinationPuzzleDialog` → submit → inventory updates → `InventoryPanel` redraws.

19. **Implement full golden-path playthrough from Swing UI.** All three rooms navigable, all six puzzles solvable through the UI, win screen appears on final puzzle. Save and load work from the menu. This is the end-to-end acceptance test.

---

## 11. Out of Scope for Phase 1 (Phase 2+ Backlog)

- Countdown timer with visual pressure mechanic
- Hint system (tiered hints per puzzle, penalty tracking)
- Sound effects and ambient audio (`javax.sound.sampled`)
- Animated scene transitions between rooms
- Multiple save slots with a save-selection screen
- Real CC-licensed art assets from OpenGameArt.org replacing placeholders
- Leaderboard (fastest solve times, stored in H2)
- Additional rooms beyond the three MVP rooms
- Additional puzzle types (e.g., `MastermindPuzzle`, `MazePuzzle`)
- Player accounts and login (would require Spring Security)
- React frontend port (no backend changes required; this is the Phase 2 client swap)
- Multiplayer / co-op mode
- Difficulty settings (altering puzzle solutions or adding red herrings)
- In-game journal / notebook accumulating discovered clues
- **`SwingWorker` async API calls** (defer all backend calls to a background thread so the EDT never blocks — Phase 1 accepts the imperceptible localhost freeze)
- **`SequencePuzzle` drag-and-drop UI** if Phase 1 hits the riddle-string fallback (carry the full drag-and-drop dialog over to Phase 2)

---

## 12. Resolved Decisions (mentor-approved)

The questions in this section have been answered by the mentor; recording the decisions here so the rationale survives into implementation.

- **JPA inheritance strategy for `Puzzle`: `@Inheritance(strategy = InheritanceType.JOINED)`.** Generates one parent `PUZZLE` table plus four child tables in H2. The schema makes the class hierarchy visible at the database level — chosen specifically to serve the grading discussion and give Abhishri a concrete artifact to point at when explaining Inheritance. The extra JOIN per query is acceptable at this scale.

- **Swing threading: synchronous `HttpClient.send()` on the EDT for Phase 1; defer `SwingWorker` to Phase 2.** Localhost round-trip is sub-millisecond, freeze is imperceptible, and concurrency concepts are out of AP CS scope. Phase 2 will introduce `SwingWorker` if/when the Swing client ever talks to a non-local server.

- **`SequencePuzzle` UI: build the drag-and-drop list first; fall back to riddle-string answer if it blocks the timeline.** *Note carried forward:* the drag-and-drop dialog (`JList` + `TransferHandler` or `DefaultListModel` with reorder buttons) is the Phase 1 target. If implementation slips, swap `puzzle_bookshelf`'s `type` in `world.json` from `SEQUENCE` to `RIDDLE` with `expectedAnswer` set to the correct Dewey sequence as a comma-separated string. This is a server-side configuration change only — the frontend already supports `RiddlePuzzleDialog`, so no UI work is wasted by the fallback. Revisit and complete the proper drag-and-drop dialog in Phase 2.

- **World seed format: `src/main/resources/world.json`, loaded by `WorldSeedService` at startup.** Lets Abhishri tweak riddles, combination codes, room descriptions, and item names without recompiling. Adds one Jackson `ObjectMapper.readValue()` call and a small set of seed POJOs (`RoomSeed`, `PuzzleSeed`, `ItemSeed`) keyed by a `type` discriminator that maps to the puzzle subclass.

- **`GameSnapshotDTO` scope: mutable session state only (`gameId`, `currentRoomId`, `status`, `createdAt`, `lastUpdatedAt`, `solvedPuzzleIds`, `heldItemIds`).** Immutable world data is *not* serialized — it is reseeded from `world.json` at startup and is the same for every game. Saves are small, human-readable, and forward-compatible with `world.json` edits as long as referenced puzzle/item/room IDs still exist. Fixed for the life of Phase 1; any format change after release breaks existing saves.

- **Java version target: Java 17 + Spring Boot 3.x.** Modern toolchain, latest LTS, full `java.net.http.HttpClient` and modern Spring Boot features available. `pom.xml` will set `<java.version>17</java.version>` and depend on Spring Boot 3.x BOM.
