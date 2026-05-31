# Mystery Escape Room (Season 3) — `design.md`

> Companion to `idea.md`. This document is the implementation blueprint: DDL, DTOs, sequence diagrams, error model, configuration, and acceptance criteria. Nothing here re-opens a decision recorded in `idea.md §12`.

---

## 1. Document Purpose & Reading Guide

`idea.md` answers **what** we are building and **why** the decisions look the way they do — narrative, scope, rubric mapping, resolved architectural choices. `design.md` answers **how**, at the level of every field, every column, every status code, and every file format the implementation must honor. If `idea.md` says "use H2 in file mode," `design.md` says "the DDL is exactly this; the connection string is exactly that." When the two documents disagree, `idea.md` wins on intent; `design.md` wins on mechanics.

**Reading guide for Abhishri while implementing:**

- Building entities and repositories → §3 (DDL), §7 (Backend Class Catalog).
- Writing `world.json` and the seed loader → §4.
- Designing the REST surface → §5 (DTOs), §6 (API Reference), §11 (Error model).
- Building the Swing UI → §8 (Frontend Class Catalog), §9 (Sequence diagrams 2, 3, 5).
- Configuring the project for the first build → §13 (config files), §14 (run/build), §20 (acceptance).
- Writing tests → §16.
- Debugging a "why doesn't the win screen fire?" moment → §9 (diagram 4), §10 (state machine).

---

## 2. System Context Diagram

```
+---------------------------------------+        +---------------------------------------+
|  Swing JVM  (process A)               |        |  Spring Boot JVM  (process B)         |
|                                       |        |                                       |
|  EscapeRoomApp -> MainFrame           |  HTTP  |  GameController                       |
|     ScenePanel / InventoryPanel       | <----> |     -> GameSessionService             |
|     DialoguePanel / StatusBar         |  JSON  |     -> PuzzleEvaluationService        |
|     PuzzleDialog(s)                   |  :8080 |     -> InventoryService               |
|                                       |        |     -> SaveLoadService                |
|  GameApiClient (java.net.http)        |        |  WorldSeedService (@PostConstruct)    |
|     Jackson ObjectMapper              |        |  GlobalExceptionHandler               |
+----------------+----------------------+        +-----------------+---------------------+
                 |                                                 |
                 | reads/writes                                    | JDBC
                 v                                                 v
+---------------------------------------+        +---------------------------------------+
|  Filesystem (shared workstation)      |        |  H2 Embedded DB (file mode)           |
|                                       |        |                                       |
|  ./saves/<gameId>-<ts>.json   (SLS)   |  <--   |  ./data/escaperoom.mv.db              |
|  ./backend/.../world.json    (seed)   |        |  ./data/escaperoom.trace.db           |
|  ./logs/escaperoom.log     (logback)  |        |  (file lock held by process B)        |
+---------------------------------------+        +---------------------------------------+
```

- **Swing JVM** owns rendering and input only; it holds zero game rules.
- **Spring Boot JVM** owns all rules, all persistence, and all save/load. It binds to `localhost:8080` only (see §18).
- **H2 file DB** is the source of truth for live state. It is local to the backend process; the Swing process never touches it.
- **Filesystem artifacts** are: the seed (`world.json`, read-only at runtime), the H2 file DB (read-write), JSON saves (write on save, read on load), and the log file (append-only).

---

## 3. Database Schema (DDL)

### 3a. Full DDL

These statements reflect what Hibernate emits under `spring.jpa.hibernate.ddl-auto=update` for the entities described in `idea.md §5a`, plus the new exception/error infrastructure introduced in this design. H2 will create them on first boot. Types are H2-compatible.

```sql
-- ============================================================
-- Root aggregate: one row per player session
-- ============================================================
CREATE TABLE GAME_SESSION (
    ID                  UUID         NOT NULL,
    CURRENT_ROOM_ID     VARCHAR(64)  NOT NULL,
    STATUS              VARCHAR(16)  NOT NULL,        -- IN_PROGRESS | COMPLETE | FAILED
    CREATED_AT          TIMESTAMP    NOT NULL,
    LAST_UPDATED_AT     TIMESTAMP    NOT NULL,
    INVENTORY_ID        UUID         NULL,            -- FK -> PLAYER_INVENTORY.ID (OneToOne, optional)
    CONSTRAINT PK_GAME_SESSION PRIMARY KEY (ID),
    CONSTRAINT FK_GAME_SESSION_INVENTORY
        FOREIGN KEY (INVENTORY_ID) REFERENCES PLAYER_INVENTORY(ID)
);

-- @ElementCollection List<String> solvedPuzzleIds
CREATE TABLE GAME_SESSION_SOLVED_PUZZLES (
    GAME_SESSION_ID     UUID         NOT NULL,
    SOLVED_PUZZLE_ID    VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_SOLVED_GAME
        FOREIGN KEY (GAME_SESSION_ID) REFERENCES GAME_SESSION(ID)
);
-- No PK; Hibernate treats this as a bag. Idempotent inserts enforced in service code.

-- ============================================================
-- Player inventory: one per game session
-- ============================================================
CREATE TABLE PLAYER_INVENTORY (
    ID                  UUID         NOT NULL,
    GAME_SESSION_ID     UUID         NULL,            -- back-reference, nullable to break cycle on insert
    CONSTRAINT PK_PLAYER_INVENTORY PRIMARY KEY (ID)
);

-- @ElementCollection List<String> heldItemIds
CREATE TABLE PLAYER_INVENTORY_HELD_ITEMS (
    PLAYER_INVENTORY_ID UUID         NOT NULL,
    HELD_ITEM_ID        VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_HELD_INVENTORY
        FOREIGN KEY (PLAYER_INVENTORY_ID) REFERENCES PLAYER_INVENTORY(ID)
);

-- ============================================================
-- World data: ROOM and its element collections
-- ============================================================
CREATE TABLE ROOM (
    ID                  VARCHAR(64)  NOT NULL,
    NAME                VARCHAR(128) NOT NULL,
    DESCRIPTION         CLOB         NULL,
    CONSTRAINT PK_ROOM PRIMARY KEY (ID)
);

CREATE TABLE ROOM_CONNECTED_ROOMS (
    ROOM_ID             VARCHAR(64)  NOT NULL,
    CONNECTED_ROOM_ID   VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_CONNECTED_ROOM
        FOREIGN KEY (ROOM_ID) REFERENCES ROOM(ID)
);

CREATE TABLE ROOM_OBJECT_IDS (
    ROOM_ID             VARCHAR(64)  NOT NULL,
    OBJECT_ID           VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_ROOM_OBJECT
        FOREIGN KEY (ROOM_ID) REFERENCES ROOM(ID)
);

CREATE TABLE ROOM_PUZZLE_IDS (
    ROOM_ID             VARCHAR(64)  NOT NULL,
    PUZZLE_ID           VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_ROOM_PUZZLE
        FOREIGN KEY (ROOM_ID) REFERENCES ROOM(ID)
);

-- ============================================================
-- Inventory item definitions (immutable world data)
-- ============================================================
CREATE TABLE INVENTORY_ITEM (
    ID                  VARCHAR(64)  NOT NULL,
    NAME                VARCHAR(128) NOT NULL,
    DESCRIPTION         CLOB         NULL,
    ASSET_KEY           VARCHAR(64)  NOT NULL,
    CONSTRAINT PK_INVENTORY_ITEM PRIMARY KEY (ID)
);

-- ============================================================
-- Puzzle parent table (JOINED inheritance)
-- DTYPE column omitted: under JOINED, Hibernate disambiguates by row presence
-- in the matching child table; no discriminator column is required.
-- ============================================================
CREATE TABLE PUZZLE (
    ID                  VARCHAR(64)  NOT NULL,
    ROOM_ID             VARCHAR(64)  NOT NULL,
    DESCRIPTION         CLOB         NULL,
    REWARD_ITEM_ID      VARCHAR(64)  NULL,
    CONSTRAINT PK_PUZZLE PRIMARY KEY (ID)
);

CREATE TABLE PUZZLE_PREREQUISITE_IDS (
    PUZZLE_ID           VARCHAR(64)  NOT NULL,
    PREREQUISITE_ID     VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_PUZZLE_PREREQ
        FOREIGN KEY (PUZZLE_ID) REFERENCES PUZZLE(ID)
);

-- Child: CombinationPuzzle
CREATE TABLE COMBINATION_PUZZLE (
    ID                  VARCHAR(64)  NOT NULL,
    EXPECTED_CODE       VARCHAR(32)  NOT NULL,
    DIGIT_COUNT         INT          NOT NULL,
    CONSTRAINT PK_COMBINATION_PUZZLE PRIMARY KEY (ID),
    CONSTRAINT FK_COMBINATION_PUZZLE_PARENT
        FOREIGN KEY (ID) REFERENCES PUZZLE(ID)
);

-- Child: RiddlePuzzle
CREATE TABLE RIDDLE_PUZZLE (
    ID                  VARCHAR(64)  NOT NULL,
    QUESTION_TEXT       CLOB         NOT NULL,
    EXPECTED_ANSWER     VARCHAR(255) NOT NULL,
    CASE_SENSITIVE      BOOLEAN      NOT NULL,
    CONSTRAINT PK_RIDDLE_PUZZLE PRIMARY KEY (ID),
    CONSTRAINT FK_RIDDLE_PUZZLE_PARENT
        FOREIGN KEY (ID) REFERENCES PUZZLE(ID)
);

-- Child: SequencePuzzle (no scalar columns; both lists are @ElementCollection)
CREATE TABLE SEQUENCE_PUZZLE (
    ID                  VARCHAR(64)  NOT NULL,
    CONSTRAINT PK_SEQUENCE_PUZZLE PRIMARY KEY (ID),
    CONSTRAINT FK_SEQUENCE_PUZZLE_PARENT
        FOREIGN KEY (ID) REFERENCES PUZZLE(ID)
);

-- Ordered list: expected sequence (must use @OrderColumn; see §19)
CREATE TABLE SEQUENCE_PUZZLE_EXPECTED (
    SEQUENCE_PUZZLE_ID  VARCHAR(64)  NOT NULL,
    EXPECTED_ITEM       VARCHAR(64)  NOT NULL,
    POSITION            INT          NOT NULL,
    CONSTRAINT PK_SEQUENCE_PUZZLE_EXPECTED PRIMARY KEY (SEQUENCE_PUZZLE_ID, POSITION),
    CONSTRAINT FK_SPE_PARENT
        FOREIGN KEY (SEQUENCE_PUZZLE_ID) REFERENCES SEQUENCE_PUZZLE(ID)
);

-- Unordered: items the player can choose from
CREATE TABLE SEQUENCE_PUZZLE_AVAILABLE (
    SEQUENCE_PUZZLE_ID  VARCHAR(64)  NOT NULL,
    AVAILABLE_ITEM      VARCHAR(64)  NOT NULL,
    CONSTRAINT FK_SPA_PARENT
        FOREIGN KEY (SEQUENCE_PUZZLE_ID) REFERENCES SEQUENCE_PUZZLE(ID)
);

-- Child: ItemUsePuzzle
CREATE TABLE ITEM_USE_PUZZLE (
    ID                  VARCHAR(64)  NOT NULL,
    REQUIRED_ITEM_ID    VARCHAR(64)  NOT NULL,
    TARGET_OBJECT_ID    VARCHAR(64)  NOT NULL,
    OUTCOME_MESSAGE     CLOB         NULL,
    CONSTRAINT PK_ITEM_USE_PUZZLE PRIMARY KEY (ID),
    CONSTRAINT FK_ITEM_USE_PUZZLE_PARENT
        FOREIGN KEY (ID) REFERENCES PUZZLE(ID)
);
```

**Table count: 17.** (`@ElementCollection` join tables push the count higher than a casual eyeball suggests; the precise list above is the source of truth.)

### 3b. ER Diagram

```
                            +----------------+
                            |     ROOM       |
                            +----------------+
                            | PK ID          |
                            |    NAME        |
                            |    DESCRIPTION |
                            +----------------+
                              |    |    |
                              |    |    +-----> ROOM_PUZZLE_IDS (ROOM_ID -> PUZZLE.ID, soft)
                              |    +----------> ROOM_OBJECT_IDS  (ROOM_ID, OBJECT_ID)
                              +---------------> ROOM_CONNECTED_ROOMS (ROOM_ID, CONNECTED_ROOM_ID)

  +----------------+    1..1     +-----------------------+
  |  GAME_SESSION  |<----------->|   PLAYER_INVENTORY    |
  +----------------+             +-----------------------+
  | PK ID          |             | PK ID                 |
  |    CURRENT_ROOM_ID (-> ROOM) |    GAME_SESSION_ID    |
  |    STATUS                    +-----------------------+
  |    CREATED_AT                            |
  |    LAST_UPDATED_AT                       v
  |    INVENTORY_ID (FK)         PLAYER_INVENTORY_HELD_ITEMS
  +----------------+                  (PLAYER_INVENTORY_ID, HELD_ITEM_ID)
         |
         v
  GAME_SESSION_SOLVED_PUZZLES
   (GAME_SESSION_ID, SOLVED_PUZZLE_ID)

                       +--------+
                       | PUZZLE |  (parent, JOINED)
                       +--------+
                       | PK ID  |
                       | ROOM_ID|
                       | DESCRIPTION |
                       | REWARD_ITEM_ID -> INVENTORY_ITEM.ID
                       +--------+
                          | | | |
                          | | | +--> PUZZLE_PREREQUISITE_IDS (PUZZLE_ID, PREREQUISITE_ID)
              JOINED-PK   | | |
   +----------------------+ | +----------------------+
   |                        |                        |
   v                        v                        v
COMBINATION_PUZZLE   RIDDLE_PUZZLE          ITEM_USE_PUZZLE
(ID, EXPECTED_CODE,  (ID, QUESTION_TEXT,    (ID, REQUIRED_ITEM_ID,
 DIGIT_COUNT)         EXPECTED_ANSWER,        TARGET_OBJECT_ID,
                      CASE_SENSITIVE)         OUTCOME_MESSAGE)
                                            v
                                        SEQUENCE_PUZZLE (ID)
                                          |
                                          +--> SEQUENCE_PUZZLE_EXPECTED (..., POSITION)
                                          +--> SEQUENCE_PUZZLE_AVAILABLE

INVENTORY_ITEM (ID, NAME, DESCRIPTION, ASSET_KEY) -- referenced by ID from many places (string FK only)
```

Note: Room/puzzle/item IDs are referenced as plain `VARCHAR` foreign keys (no JPA relationship), keeping the model simple and the SQL flat — this is the same trade-off `idea.md §5a` makes for `connectedRoomIds`.

### 3c. JPA → DDL Mapping Notes

- **`@Inheritance(strategy = InheritanceType.JOINED)`** on `Puzzle` produces one parent table (`PUZZLE`) plus four child tables, each child's PK being also an FK to `PUZZLE.ID`. The `@DiscriminatorColumn` annotation is *not used* under JOINED — Hibernate disambiguates by which child table the row appears in. A SELECT for a `Puzzle` by ID issues a `LEFT JOIN` against all four child tables; for a known subclass query, only one child table is joined. Acceptable cost at our scale.
- **`@ElementCollection List<String>`** generates a separate "bag" table with two columns: the owner FK and the value. Unordered by default — see `@OrderColumn` discussion below for `SEQUENCE_PUZZLE_EXPECTED` (the only collection where order matters).
- **`@OneToOne`** between `GameSession` and `PlayerInventory` is unidirectional with `@JoinColumn(name = "INVENTORY_ID")` on `GameSession`. Cascade is `ALL`; orphan removal is `true`. The reverse FK column on `PLAYER_INVENTORY.GAME_SESSION_ID` is purely informational and nullable to avoid an insertion-order chicken/egg.
- **`UUID`** types map to H2's `UUID` column (native 16-byte type). No string conversion needed; Hibernate handles it.
- **`spring.jpa.hibernate.ddl-auto=update`** lets H2 generate/alter the schema on every backend boot during Phase 1. Implication: dropping a column from an entity does *not* drop it from H2 — `update` is additive. If schema diverges meaningfully, delete `./data/escaperoom.mv.db` and restart. Phase 2 should switch to `validate` + Flyway/Liquibase migrations.
- **Use `jakarta.persistence.*` imports, NOT `javax.persistence.*`.** Spring Boot 3.x runs on Jakarta EE 9+, which renamed the JPA package from `javax.persistence` to `jakarta.persistence`. Every annotation in this design (`@Entity`, `@Id`, `@OneToOne`, `@JoinColumn`, `@ElementCollection`, `@OrderColumn`, `@Inheritance`, `@InheritanceType`, `@MappedSuperclass`) comes from the `jakarta.persistence` package. Most Spring Boot tutorials older than 2023 still show `javax.persistence`; copy-pasting those imports will not compile against Boot 3.x. IntelliJ and VS Code will offer to auto-import — pick the `jakarta` option. The same rename applies to validation (`jakarta.validation.constraints.NotBlank`, not `javax.validation...`) and servlets (`jakarta.servlet.http.HttpServletRequest`, used in §11b).

---

## 4. `world.json` Schema and Phase 1 Content

### 4a. JSON Schema (informal)

Top-level object:

| Key | Type | Required | Notes |
|---|---|---|---|
| `rooms` | array of `Room` | yes | Must contain at least one room with `id` equal to `escape.world.starting-room` (default `room_foyer`). |
| `items` | array of `Item` | yes | Every ID referenced by a puzzle's `rewardItemId` or `requiredItemId` must exist here. |
| `puzzles` | array of `Puzzle` | yes | Each entry has a `type` discriminator (see below). |

**`Room`:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `id` | string | yes | Snake-case `room_*`; unique. |
| `name` | string | yes | Display name. |
| `description` | string | yes | Flavor text. |
| `connectedRoomIds` | array of string | yes | Must reference existing room IDs. |
| `objectIds` | array of string | yes | Logical IDs of clickable scenery (e.g., `wall_clock`). |
| `puzzleIds` | array of string | yes | Must reference puzzle IDs in this file. |

**`Item`:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `id` | string | yes | Snake-case; unique. |
| `name` | string | yes | |
| `description` | string | yes | |
| `assetKey` | string | yes | Key the frontend uses to resolve an icon (`item_key`, `item_lens`, ...). |

**`Puzzle` (common):**

| Field | Type | Required | Validation |
|---|---|---|---|
| `type` | enum string | yes | One of `COMBINATION`, `RIDDLE`, `SEQUENCE`, `ITEM_USE`. |
| `id` | string | yes | Snake-case `puzzle_*`; unique. |
| `roomId` | string | yes | Must reference existing room. |
| `description` | string | yes | |
| `rewardItemId` | string | no | Must reference an item, or null. |
| `prerequisitePuzzleIds` | array of string | no | Defaults to `[]`. Cycle detection at seed time. |

**Type-specific fields:**

| `type` | Extra fields | Validation |
|---|---|---|
| `COMBINATION` | `expectedCode` (string), `digitCount` (int) | `expectedCode` must be numeric and `expectedCode.length() == digitCount`. `digitCount >= 1`. |
| `RIDDLE` | `questionText` (string), `expectedAnswer` (string), `caseSensitive` (boolean) | Non-empty answer. |
| `SEQUENCE` | `expectedSequence` (array of string), `availableItems` (array of string) | `expectedSequence` non-empty; every entry in `expectedSequence` must also appear in `availableItems`. |
| `ITEM_USE` | `requiredItemId` (string), `targetObjectId` (string), `outcomeMessage` (string) | `requiredItemId` must reference an item. |

### 4b. Complete Phase 1 `world.json` (ship this file verbatim)

```json
{
  "rooms": [
    {
      "id": "room_foyer",
      "name": "The Entry Foyer",
      "description": "You wake on the cold marble of Thornwick Library's foyer. The front door is sealed by an electromagnetic lock. A wall clock above the reception desk is stopped at 11:47. A welcome mat curls at your feet; a battered umbrella stand leans against the wall. There are 3 brass nails set into the door frame.",
      "connectedRoomIds": ["room_reading_hall"],
      "objectIds": ["wall_clock", "reception_desk", "welcome_mat", "umbrella_stand", "front_door"],
      "puzzleIds": ["puzzle_clock"]
    },
    {
      "id": "room_reading_hall",
      "name": "The Reading Hall",
      "description": "Eight tall bookshelves climb to a vaulted ceiling. A librarian's rolling cart sits half-shelved beside the 800-section. A glass display case glints in the lamplight, locked with a three-digit dial. A fireplace yawns cold against the north wall; the iron grate is loose.",
      "connectedRoomIds": ["room_foyer", "room_archives"],
      "objectIds": ["display_case", "book_cart", "fireplace", "reading_lamp"],
      "puzzleIds": ["puzzle_display_case", "puzzle_bookshelf"]
    },
    {
      "id": "room_archives",
      "name": "The Archives",
      "description": "Steel filing cabinets line the walls in 4 long rows. A brass cipher wheel is bolted to the south wall, its outer ring etched with letters. An iron chest sits dead-center, its lid stamped with a single keyhole. The pneumatic-tube terminal hisses softly in the corner, its tube empty and waiting. The manuscript pedestal stands bare.",
      "connectedRoomIds": ["room_reading_hall"],
      "objectIds": ["cipher_wheel", "iron_chest", "pneumatic_tube_terminal", "manuscript_pedestal", "filing_cabinets"],
      "puzzleIds": ["puzzle_cipher_wheel", "puzzle_iron_chest", "puzzle_terminal"]
    }
  ],

  "items": [
    {
      "id": "desk_key",
      "name": "Desk Key",
      "description": "A small brass key, warm to the touch. Fits a desk drawer.",
      "assetKey": "item_key"
    },
    {
      "id": "brass_magnifying_glass",
      "name": "Brass Magnifying Glass",
      "description": "Heavier than it looks. The lens is etched with hairline grid lines.",
      "assetKey": "item_lens"
    },
    {
      "id": "fireplace_scrap",
      "name": "Scrap of Paper",
      "description": "A charred scrap, partially legible: '...the second digit is 8...'",
      "assetKey": "item_scrap"
    },
    {
      "id": "manuscript_page",
      "name": "Torn Manuscript Page",
      "description": "A handwritten page in old library script. It names a thief.",
      "assetKey": "item_manuscript"
    },
    {
      "id": "evidence_token",
      "name": "Evidence Token",
      "description": "Proof transmitted. The lock is open. You won.",
      "assetKey": "item_token"
    }
  ],

  "puzzles": [
    {
      "type": "RIDDLE",
      "id": "puzzle_clock",
      "roomId": "room_foyer",
      "description": "A riddle is carved into the clock frame.",
      "rewardItemId": "desk_key",
      "prerequisitePuzzleIds": [],
      "questionText": "The clock's hands are frozen. The frame reads: 'When does the library's silence begin? Answer in HH:MM.'",
      "expectedAnswer": "11:47",
      "caseSensitive": false
    },
    {
      "type": "COMBINATION",
      "id": "puzzle_display_case",
      "roomId": "room_reading_hall",
      "description": "A three-dial combination lock guards the display case. The digits are hidden in the room descriptions: 3 brass nails in the foyer, 8 bookshelves in the reading hall, 4 rows of filing cabinets in the archives.",
      "rewardItemId": "brass_magnifying_glass",
      "prerequisitePuzzleIds": [],
      "expectedCode": "384",
      "digitCount": 3
    },
    {
      "type": "SEQUENCE",
      "id": "puzzle_bookshelf",
      "roomId": "room_reading_hall",
      "description": "Six books from the librarian's cart must be reshelved in Dewey Decimal order. Smallest number first.",
      "rewardItemId": "fireplace_scrap",
      "prerequisitePuzzleIds": [],
      "expectedSequence": ["510", "520", "610", "621", "720", "810"],
      "availableItems": ["810", "510", "720", "520", "621", "610"]
    },
    {
      "type": "ITEM_USE",
      "id": "puzzle_cipher_wheel",
      "roomId": "room_archives",
      "description": "The cipher wheel's letters are too small to read by lamplight alone.",
      "rewardItemId": null,
      "prerequisitePuzzleIds": ["puzzle_display_case"],
      "requiredItemId": "brass_magnifying_glass",
      "targetObjectId": "cipher_wheel",
      "outcomeMessage": "Under the lens, etched letters resolve into a single word: THORNWICK."
    },
    {
      "type": "RIDDLE",
      "id": "puzzle_iron_chest",
      "roomId": "room_archives",
      "description": "The iron chest demands a passphrase.",
      "rewardItemId": "manuscript_page",
      "prerequisitePuzzleIds": ["puzzle_cipher_wheel"],
      "questionText": "The chest's brass plate reads: 'Speak the name of this place to open me.'",
      "expectedAnswer": "THORNWICK",
      "caseSensitive": false
    },
    {
      "type": "ITEM_USE",
      "id": "puzzle_terminal",
      "roomId": "room_archives",
      "description": "The pneumatic-tube terminal waits for evidence to transmit.",
      "rewardItemId": "evidence_token",
      "prerequisitePuzzleIds": ["puzzle_clock", "puzzle_display_case", "puzzle_bookshelf", "puzzle_cipher_wheel", "puzzle_iron_chest"],
      "requiredItemId": "manuscript_page",
      "targetObjectId": "pneumatic_tube_terminal",
      "outcomeMessage": "The tube hisses. Evidence received. The front door's electromagnetic lock disengages. You are free."
    }
  ]
}
```

Notes embedded in this content:

- The Phase 1 win condition is "all six puzzles solved." `puzzle_terminal` carries every other puzzle as a prerequisite, so it cannot succeed before the rest are done — this also drives the win-condition check (§10).
- The combination `384` is justified in-fiction by counted nouns across rooms — the three "3 brass nails," "8 tall bookshelves," "4 long rows" phrases.
- The Dewey sequence is canonical (510 Math, 520 Astronomy, 610 Medicine, 621 Electrical Eng., 720 Architecture, 810 American Literature) — Abhishri can swap in any monotonically increasing set.

### 4c. Seed POJO Classes

Place under `com.abhishri.escape.config.seed`. These are *not* JPA entities — they are deserialization targets.

```java
public class WorldSeed {
    public List<RoomSeed> rooms;
    public List<ItemSeed> items;
    public List<PuzzleSeed> puzzles;
}

public class RoomSeed {
    public String id;
    public String name;
    public String description;
    public List<String> connectedRoomIds;
    public List<String> objectIds;
    public List<String> puzzleIds;
}

public class ItemSeed {
    public String id;
    public String name;
    public String description;
    public String assetKey;
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CombinationPuzzleSeed.class, name = "COMBINATION"),
    @JsonSubTypes.Type(value = RiddlePuzzleSeed.class,      name = "RIDDLE"),
    @JsonSubTypes.Type(value = SequencePuzzleSeed.class,    name = "SEQUENCE"),
    @JsonSubTypes.Type(value = ItemUsePuzzleSeed.class,     name = "ITEM_USE")
})
public abstract class PuzzleSeed {
    public String id;
    public String roomId;
    public String description;
    public String rewardItemId;            // nullable
    public List<String> prerequisitePuzzleIds = new ArrayList<>();
}

public class CombinationPuzzleSeed extends PuzzleSeed {
    public String expectedCode;
    public int digitCount;
}

public class RiddlePuzzleSeed extends PuzzleSeed {
    public String questionText;
    public String expectedAnswer;
    public boolean caseSensitive;
}

public class SequencePuzzleSeed extends PuzzleSeed {
    public List<String> expectedSequence;
    public List<String> availableItems;
}

public class ItemUsePuzzleSeed extends PuzzleSeed {
    public String requiredItemId;
    public String targetObjectId;
    public String outcomeMessage;
}
```

`WorldSeedService` reads `world.json`, deserializes into `WorldSeed`, validates referential integrity (every `rewardItemId` exists, no prereq cycles, every `puzzleIds` entry resolves), then maps each seed to its JPA entity and saves through the repositories.

---

## 5. DTO Specifications

All DTOs live in two places per `idea.md §8`: `com.abhishri.escape.dto` on the backend, `com.abhishri.escape.ui.api.dto` on the frontend. The shapes are identical; the duplication is intentional. JSON names use `camelCase` (Jackson default).

### `GameStateDTO`

| Field | Type | Nullable | Validation | JSON |
|---|---|---|---|---|
| `gameId` | `UUID` | no | — | `gameId` |
| `gameStatus` | enum (`IN_PROGRESS`/`COMPLETE`/`FAILED`) | no | Identical to the `GameStatus` entity enum. The frontend detects the win moment by checking `gameStatus == COMPLETE` after any state-changing response — no separate `WIN` value is needed. | `gameStatus` |
| `currentRoom` | `RoomDTO` | no | — | `currentRoom` |
| `inventory` | `List<InventoryItemDTO>` | no | empty list allowed | `inventory` |
| `solvedPuzzleIds` | `List<String>` | no | — | `solvedPuzzleIds` |
| `dialogueMessage` | `String` | yes | — | `dialogueMessage` |
| `lastActionResult` | enum (`MOVE_SUCCESS`, `EXAMINE_OK`, `PICKUP_OK`, `PUZZLE_SOLVED`, `PUZZLE_FAILED`, `INVALID_MOVE`, `NEW_GAME`, `LOADED`, `SAVED`) | yes | — | `lastActionResult` |

Sample:
```json
{
  "gameId": "a3f7c2d1-9e44-4f6a-9a0b-2d2b3a3e3a8c",
  "gameStatus": "IN_PROGRESS",
  "currentRoom": { "id": "room_reading_hall", "name": "The Reading Hall", "description": "...", "objects": [], "exits": ["room_foyer","room_archives"] },
  "inventory": [{ "id": "desk_key", "name": "Desk Key", "description": "...", "assetKey": "item_key" }],
  "solvedPuzzleIds": ["puzzle_clock"],
  "dialogueMessage": "The case clicks open.",
  "lastActionResult": "PUZZLE_SOLVED"
}
```

### `RoomDTO`

| Field | Type | Nullable | JSON |
|---|---|---|---|
| `id` | `String` | no | `id` |
| `name` | `String` | no | `name` |
| `description` | `String` | no | `description` |
| `objects` | `List<RoomObjectDTO>` | no | `objects` |
| `exits` | `List<String>` | no | `exits` |

### `RoomObjectDTO`

| Field | Type | Nullable | JSON |
|---|---|---|---|
| `id` | `String` | no | `id` |
| `label` | `String` | no | `label` |
| `interactable` | `boolean` | no | `interactable` |
| `puzzleId` | `String` | yes | `puzzleId` |
| `pickupItemId` | `String` | yes | `pickupItemId` |
| `objectType` | enum (`SCENERY`, `PUZZLE`, `ITEM`, `EXIT`) | no | `objectType` |

Sample:
```json
{ "id": "display_case", "label": "Display Case", "interactable": true, "puzzleId": "puzzle_display_case", "pickupItemId": null, "objectType": "PUZZLE" }
```

### `InventoryItemDTO`

| Field | Type | Nullable |
|---|---|---|
| `id` | `String` | no |
| `name` | `String` | no |
| `description` | `String` | yes |
| `assetKey` | `String` | no |

### `MoveRequest`

| Field | Type | Validation | JSON |
|---|---|---|---|
| `targetRoomId` | `String` | `@NotBlank`, must be in current room's `connectedRoomIds` | `targetRoomId` |

```json
{ "targetRoomId": "room_archives" }
```

### `ExamineRequest`

| Field | Type | Validation |
|---|---|---|
| `objectId` | `String` | `@NotBlank` |

```json
{ "objectId": "wall_clock" }
```

### `PickupRequest`

| Field | Type | Validation |
|---|---|---|
| `objectId` | `String` | `@NotBlank` — the object the player clicked, server resolves to a pickup item |

```json
{ "objectId": "desk_key" }
```

### `UseItemRequest`

| Field | Type | Validation |
|---|---|---|
| `itemId` | `String` | `@NotBlank`, must be in inventory |
| `targetObjectId` | `String` | `@NotBlank`, must exist in current room |

```json
{ "itemId": "brass_magnifying_glass", "targetObjectId": "cipher_wheel" }
```

### `AttemptPuzzleRequest`

| Field | Type | Validation |
|---|---|---|
| `puzzleId` | `String` | `@NotBlank` |
| `inputs` | `Map<String,String>` | `@NotNull` (may be empty for some puzzle types) |

```json
{ "puzzleId": "puzzle_display_case", "inputs": { "code": "384" } }
```

### `LoadRequest`

| Field | Type | Validation |
|---|---|---|
| `filename` | `String` | `@NotBlank`, must end in `.json`, must not contain path separators |

```json
{ "filename": "a3f7c2d1-9e44-4f6a-9a0b-2d2b3a3e3a8c-20260531T143200.json" }
```

### `SaveConfirmationDTO`

| Field | Type | Nullable |
|---|---|---|
| `filename` | `String` | no |
| `savedAt` | `Instant` (ISO-8601 string) | no |
| `sizeBytes` | `long` | no |

```json
{ "filename": "a3f7c2d1-...-20260531T143200.json", "savedAt": "2026-05-31T18:32:00Z", "sizeBytes": 412 }
```

### `SaveMetadataDTO`

| Field | Type | Nullable |
|---|---|---|
| `filename` | `String` | no |
| `gameId` | `UUID` | no |
| `savedAt` | `Instant` | no |
| `currentRoomId` | `String` | no |
| `solvedPuzzleCount` | `int` | no |

### `GameSnapshotDTO`

Mutable session state only — see `idea.md §5e`.

| Field | Type | Nullable |
|---|---|---|
| `gameId` | `UUID` | no |
| `currentRoomId` | `String` | no |
| `status` | `GameStatus` | no |
| `createdAt` | `LocalDateTime` | no |
| `lastUpdatedAt` | `LocalDateTime` | no |
| `solvedPuzzleIds` | `List<String>` | no |
| `heldItemIds` | `List<String>` | no |
| `schemaVersion` | `int` | no (always `1` in Phase 1) |

```json
{
  "schemaVersion": 1,
  "gameId": "a3f7c2d1-9e44-4f6a-9a0b-2d2b3a3e3a8c",
  "currentRoomId": "room_archives",
  "status": "IN_PROGRESS",
  "createdAt": "2026-05-31T18:00:12.345",
  "lastUpdatedAt": "2026-05-31T18:31:55.012",
  "solvedPuzzleIds": ["puzzle_clock","puzzle_display_case","puzzle_bookshelf","puzzle_cipher_wheel"],
  "heldItemIds": ["desk_key","brass_magnifying_glass","fireplace_scrap","manuscript_page"]
}
```

### `ErrorResponseDTO`  *(new in this design)*

| Field | Type | Nullable | JSON |
|---|---|---|---|
| `timestamp` | `Instant` | no | `timestamp` |
| `status` | `int` (HTTP status code) | no | `status` |
| `error` | `String` (HTTP reason phrase) | no | `error` |
| `message` | `String` (user-readable, safe to surface in UI) | no | `message` |
| `path` | `String` (request URI) | no | `path` |
| `errorCode` | enum `ApiErrorCode` | no | `errorCode` |

`ApiErrorCode` values: `GAME_NOT_FOUND`, `INVALID_MOVE`, `PUZZLE_NOT_FOUND`, `PREREQUISITE_NOT_MET`, `WRONG_ANSWER`, `ITEM_NOT_IN_INVENTORY`, `INVALID_REQUEST`, `SAVE_FAILED`, `LOAD_FAILED`, `INTERNAL_ERROR`.

```json
{
  "timestamp": "2026-05-31T18:32:11.221Z",
  "status": 404,
  "error": "Not Found",
  "message": "No game session with id a3f7c2d1-...",
  "path": "/api/game/a3f7c2d1-.../move",
  "errorCode": "GAME_NOT_FOUND"
}
```

---

## 6. REST API Reference

### HTTP status code policy (decision)

- `200 OK` — successful state-changing actions; the response carries the new state.
- `201 Created` — `POST /api/game/new` only.
- `400 Bad Request` — `@Valid` body validation failure; malformed JSON.
- `404 Not Found` — unknown `gameId`, unknown `puzzleId`, unknown room, unknown save file.
- `409 Conflict` — prerequisite not met, item not in inventory, illegal room move (the request is well-formed but violates game state).
- `500 Internal Server Error` — unhandled exception.

**Wrong puzzle answer = `200 OK` with `lastActionResult: "PUZZLE_FAILED"`** (decided). A wrong answer is a legitimate gameplay event, not an HTTP error; the client needs the full updated state anyway (to re-render the dialog with a hint, to show the dialogue message, etc.). Forcing `422` would force the client to special-case parsing — `200 + result enum` keeps every response identically shaped, which simplifies `GameApiClient` and `MainFrame.renderState` enormously.

### Endpoints

#### `POST /api/game/new`

- **Body:** empty.
- **Success:** `201 Created` + `GameStateDTO` (`gameStatus = IN_PROGRESS`, player at `room_foyer`, empty inventory, `lastActionResult = NEW_GAME`).
- **Errors:** `500 INTERNAL_ERROR` if seed data missing.
- **Side effects:** inserts one row into `GAME_SESSION` and one into `PLAYER_INVENTORY`.
- **Idempotency:** not idempotent (each call creates a new session).

#### `GET /api/game/{gameId}`

- **Path param:** `gameId` UUID.
- **Body:** none.
- **Success:** `200 OK` + `GameStateDTO`.
- **Errors:** `404 GAME_NOT_FOUND`.
- **Side effects:** none.
- **Idempotency:** safe and idempotent.

#### `POST /api/game/{gameId}/move`

- **Body:** `MoveRequest`.
- **Success:** `200 OK` + `GameStateDTO` (`lastActionResult = MOVE_SUCCESS`).
- **Errors:** `404 GAME_NOT_FOUND`; `400 INVALID_REQUEST` (blank field); `409 INVALID_MOVE` if `targetRoomId` not in current room's `connectedRoomIds`.
- **Side effects:** updates `GAME_SESSION.CURRENT_ROOM_ID` and `LAST_UPDATED_AT`.
- **Idempotency:** idempotent within the same room (moving to the room you are already in is a no-op success); otherwise state-changing.

#### `POST /api/game/{gameId}/examine`

- **Body:** `ExamineRequest`.
- **Success:** `200 OK` + `GameStateDTO` (`lastActionResult = EXAMINE_OK`, `dialogueMessage` set to object's flavor text).
- **Errors:** `404 GAME_NOT_FOUND`; `400 INVALID_REQUEST`; `409 INVALID_MOVE` if `objectId` is not present in the current room's `objectIds` (state violation, not a missing resource — aligns with the exception list in §11a).
- **Side effects:** updates `LAST_UPDATED_AT`.
- **Idempotency:** safe (no state change beyond timestamp).

#### `POST /api/game/{gameId}/pickup`

- **Body:** `PickupRequest`.
- **Success:** `200 OK` + `GameStateDTO` (`lastActionResult = PICKUP_OK`; item appears in `inventory`).
- **Errors:** `404 GAME_NOT_FOUND`; `409 INVALID_MOVE` if object is not pickupable or has been picked up already.
- **Side effects:** inserts one row into `PLAYER_INVENTORY_HELD_ITEMS`.
- **Idempotency:** repeated calls return `409` on the second attempt — item already in inventory.

#### `POST /api/game/{gameId}/use-item`

- **Body:** `UseItemRequest`.
- **Success:** `200 OK` + `GameStateDTO` (`lastActionResult = PUZZLE_SOLVED` if it triggered an `ItemUsePuzzle` correctly; `EXAMINE_OK` with a "nothing happens" message otherwise).
- **Errors:** `404 GAME_NOT_FOUND`; `409 ITEM_NOT_IN_INVENTORY` if `itemId` not held; `404 PUZZLE_NOT_FOUND` if there is no matching `ItemUsePuzzle`; `409 PREREQUISITE_NOT_MET`.
- **Side effects:** may update `GAME_SESSION_SOLVED_PUZZLES`, `PLAYER_INVENTORY_HELD_ITEMS`.
- **Idempotency:** second successful call is a no-op (puzzle already solved).

#### `POST /api/game/{gameId}/attempt-puzzle`

- **Body:** `AttemptPuzzleRequest`.
- **Success:** `200 OK` + `GameStateDTO`.
  - On correct answer: `lastActionResult = PUZZLE_SOLVED`; reward item appended.
  - On wrong answer: `lastActionResult = PUZZLE_FAILED`; `dialogueMessage` carries the hint; no state change beyond `LAST_UPDATED_AT`.
- **Errors:** `404 GAME_NOT_FOUND`; `404 PUZZLE_NOT_FOUND`; `409 PREREQUISITE_NOT_MET`; `400 INVALID_REQUEST` for missing `inputs.code`/`inputs.answer`/`inputs.sequence`.
- **Side effects:** on success, inserts into `GAME_SESSION_SOLVED_PUZZLES`, possibly `PLAYER_INVENTORY_HELD_ITEMS`.
- **Idempotency:** correct answer on an already-solved puzzle is a no-op success.

#### `POST /api/game/{gameId}/save`

- **Body:** empty.
- **Success:** `200 OK` + `SaveConfirmationDTO`.
- **Errors:** `404 GAME_NOT_FOUND`; `500 SAVE_FAILED` if filesystem write fails.
- **Side effects:** writes `./saves/{gameId}-{timestamp}.json`.
- **Idempotency:** each call writes a new file (timestamped); never overwrites.

#### `GET /api/game/{gameId}/saves`

- **Success:** `200 OK` + `List<SaveMetadataDTO>` for files whose name starts with `{gameId}-`.
- **Errors:** `404 GAME_NOT_FOUND`.
- **Side effects:** none.

#### `POST /api/game/{gameId}/load`

- **Body:** `LoadRequest`.
- **Success:** `200 OK` + `GameStateDTO` (`lastActionResult = LOADED`).
- **Errors:** `404 GAME_NOT_FOUND`; `404 LOAD_FAILED` if file missing; `500 LOAD_FAILED` on parse error.
- **Side effects:** overwrites the `GAME_SESSION` and `PLAYER_INVENTORY` rows for `gameId` with the snapshot's mutable state.
- **Idempotency:** loading the same file twice produces the same state.

#### `GET /api/health`

- **Success:** `200 OK` + `{"status":"ok"}`.
- **Errors:** none (if the JVM is alive, this returns).
- **Side effects:** none.

---

## 7. Backend Class Catalog

Package root: `com.abhishri.escape`.

| Class | Package | Type | Responsibility | Public methods |
|---|---|---|---|---|
| `EscapeRoomApplication` | `.` | `@SpringBootApplication` | Boot entry point. | `public static void main(String[] args)` |
| `BackendConfig` | `.config` | `@Configuration`, `WebMvcConfigurer` | CORS, ObjectMapper bean. | `public void addCorsMappings(CorsRegistry registry)`; `public ObjectMapper objectMapper()` |
| `WorldSeedService` | `.service` | `@Service` | Reads `world.json` on `@PostConstruct`, seeds H2 if empty. | `public void seedIfEmpty()` (also `@PostConstruct`-annotated `init()`) |
| `GameSessionService` | `.service` | `@Service` | New-game creation; state assembly; room transitions; win check. | `public GameStateDTO createNewGame()`; `public GameStateDTO getState(UUID id)`; `public GameStateDTO move(UUID id, MoveRequest req)`; `public GameStateDTO examine(UUID id, ExamineRequest req)`; `public GameStateDTO pickup(UUID id, PickupRequest req)`; `public GameStateDTO buildStateDTO(GameSession s, String msg, LastActionResult result)` |
| `PuzzleEvaluationService` | `.service` | `@Service` | Loads puzzle, checks prereqs, calls `attempt()`, awards reward. | `public GameStateDTO attempt(UUID id, AttemptPuzzleRequest req)`; `public GameStateDTO useItem(UUID id, UseItemRequest req)` |
| `InventoryService` | `.service` | `@Service` | Inventory adds/removes/validates. | `public void addItem(GameSession s, String itemId)`; `public boolean hasItem(GameSession s, String itemId)`; `public List<InventoryItemDTO> snapshot(GameSession s)` |
| `SaveLoadService` | `.service` | `@Service` | JSON snapshot read/write. | `public SaveConfirmationDTO save(UUID id)`; `public GameStateDTO load(UUID id, String filename)`; `public List<SaveMetadataDTO> listSaves(UUID id)` |
| `GameController` | `.controller` | `@RestController` | HTTP entry point for all `/api/game/*`. | One method per endpoint in §6; see §6 signatures. |
| `HealthController` | `.controller` | `@RestController` | `/api/health`. | `public Map<String,String> health()` |
| `GameSessionRepository` | `.repository` | `@Repository` interface | `JpaRepository<GameSession, UUID>`. | inherited + `Optional<GameSession> findByStatus(GameStatus s)` |
| `RoomRepository` | `.repository` | `@Repository` interface | `JpaRepository<Room, String>`. | inherited |
| `PuzzleRepository` | `.repository` | `@Repository` interface | `JpaRepository<Puzzle, String>`. | inherited + `List<Puzzle> findByRoomId(String roomId)` |
| `PlayerInventoryRepository` | `.repository` | `@Repository` interface | `JpaRepository<PlayerInventory, UUID>`. | inherited |
| `InventoryItemRepository` | `.repository` | `@Repository` interface | `JpaRepository<InventoryItem, String>`. | inherited |
| `GameSession` | `.domain` | `@Entity` | Root aggregate. | getters/setters; `boolean isComplete()` |
| `PlayerInventory` | `.domain` | `@Entity` | Held items. | getters/setters; `boolean contains(String itemId)` |
| `Room` | `.domain` | `@Entity` | Room definition. | getters/setters; `boolean isConnectedTo(String roomId)` |
| `InventoryItem` | `.domain` | `@Entity` | Item definition. | getters/setters |
| `Puzzle` | `.domain.puzzle` | abstract `@Entity`, `@Inheritance(JOINED)` | Base for all puzzles. | `public abstract boolean attempt(Map<String,String> inputs)` + getters/setters |
| `CombinationPuzzle` | `.domain.puzzle` | `@Entity` | 3-digit combo. | `public boolean attempt(Map<String,String> inputs)` |
| `RiddlePuzzle` | `.domain.puzzle` | `@Entity` | Text answer. | `public boolean attempt(Map<String,String> inputs)` |
| `SequencePuzzle` | `.domain.puzzle` | `@Entity` | Ordered list. | `public boolean attempt(Map<String,String> inputs)` |
| `ItemUsePuzzle` | `.domain.puzzle` | `@Entity` | Held-item + target. | `public boolean attempt(Map<String,String> inputs)` |
| `GameStatus` | `.domain` | enum | `IN_PROGRESS`, `COMPLETE`, `FAILED`. | — |
| `LastActionResult` | `.dto` | enum | Action result reported on every response. | — |
| `ApiErrorCode` | `.exception` | enum | See §5 list. | — |
| `GlobalExceptionHandler` | `.exception` | `@ControllerAdvice` | Converts exceptions → `ErrorResponseDTO`. | one handler per exception; see §11b. |
| `GameNotFoundException` | `.exception` | `RuntimeException` | Thrown when `gameId` unknown. | constructor(UUID id) |
| `InvalidMoveException` | `.exception` | `RuntimeException` | Illegal room transition, illegal pickup. | constructor(String message) |
| `PuzzleNotFoundException` | `.exception` | `RuntimeException` | Unknown `puzzleId`. | constructor(String id) |
| `PrerequisiteNotMetException` | `.exception` | `RuntimeException` | Puzzle prereq fails. | constructor(String puzzleId, String missing) |
| `WrongAnswerException` | `.exception` | `RuntimeException` | *Not thrown* in Phase 1 — wrong answers return `200 + PUZZLE_FAILED`. Class exists for Phase 2 reuse. | constructor(String puzzleId) |
| `SaveLoadException` | `.exception` | `RuntimeException` | Filesystem read/write failures. | constructor(String message, Throwable cause) |
| `WorldSeed`, `RoomSeed`, `ItemSeed`, `PuzzleSeed` (+4 subclasses) | `.config.seed` | POJO | See §4c. | public fields, no methods |
| All DTOs in §5 | `.dto` | POJO | JSON I/O. | getters/setters; `@Valid`-annotated fields. |

---

## 8. Frontend Class Catalog

Package root: `com.abhishri.escape.ui`.

| Class | Extends | Responsibility | Public methods |
|---|---|---|---|
| `EscapeRoomApp` | — | `main()` entry; instantiates `MainFrame` on EDT via `SwingUtilities.invokeLater`. | `public static void main(String[] args)` |
| `MainFrame` | `JFrame` | Top-level window; holds `GameApiClient`, current `GameStateDTO`; routes events; renders state to child panels. | `public MainFrame(GameApiClient client, AssetManager assets)`; `public void renderState(GameStateDTO dto)`; `public void onHotspotClicked(Hotspot h)`; `public void onSavePressed()`; `public void onLoadPressed()`; `public void onNewGamePressed()` |
| `ScenePanel` | `JPanel` | `paintComponent` draws background + hotspot rects; mouse listener fires `onHotspotClicked`. | `public ScenePanel(AssetManager a)`; `public void setRoom(RoomDTO r)`; `public List<Hotspot> getHotspots()`; `protected void paintComponent(Graphics g)` |
| `InventoryPanel` | `JPanel` | Vertical icon+label list; tracks selected item. | `public InventoryPanel(AssetManager a)`; `public void setItems(List<InventoryItemDTO> items)`; `public String getSelectedItemId()`; `public void clearSelection()` |
| `DialoguePanel` | `JPanel` | `JTextArea` + `JScrollPane`. | `public void append(String message)`; `public void clear()` |
| `StatusBar` | `JPanel` | Room name label + Save/Load/New buttons. | `public StatusBar(MainFrame frame)`; `public void update(GameStateDTO dto)` |
| `Hotspot` | — | Data class: `Rectangle bounds`, `String objectId`, `ObjectType type`, optional `String puzzleId`. | constructor + getters |
| `PuzzleDialog` | abstract `JDialog` | Common confirm/cancel; subclasses build inputs. | `public abstract Map<String,String> getInputs()`; `protected void onConfirm()`; `protected void onCancel()` |
| `CombinationPuzzleDialog` | `PuzzleDialog` | Row of `JSpinner` per digit. | `public CombinationPuzzleDialog(int digitCount, String description)`; overrides `getInputs()` |
| `RiddlePuzzleDialog` | `PuzzleDialog` | `JLabel` + `JTextField`. | `public RiddlePuzzleDialog(String questionText)`; overrides |
| `SequencePuzzleDialog` | `PuzzleDialog` | `JList` + `TransferHandler` for drag-and-drop. Riddle-string fallback per `idea.md §12`. | `public SequencePuzzleDialog(List<String> availableItems, String description)`; overrides |
| `GameApiClient` | — | `java.net.http.HttpClient` wrapper; Jackson deserialization; throws `ApiException` on non-2xx. | `public GameStateDTO newGame()`; `public GameStateDTO getState(UUID id)`; `public GameStateDTO move(UUID id, String target)`; `public GameStateDTO examine(UUID id, String objectId)`; `public GameStateDTO pickup(UUID id, String objectId)`; `public GameStateDTO useItem(UUID id, String item, String target)`; `public GameStateDTO attemptPuzzle(UUID id, String puzzleId, Map<String,String> inputs)`; `public SaveConfirmationDTO save(UUID id)`; `public GameStateDTO load(UUID id, String filename)`; `public List<SaveMetadataDTO> listSaves(UUID id)` |
| `ApiException` | `RuntimeException` | Wraps an `ErrorResponseDTO`. | `public ErrorResponseDTO getError()` |
| `AssetManager` | interface | Asset abstraction. | `ImageIcon getRoomBackground(String roomId)`; `ImageIcon getItemIcon(String assetKey)`; `ImageIcon getHotspotOverlay(String objectId)` |
| `FileAssetManager` | implements `AssetManager` | Loads PNGs from `art/<theme>/...`. | implements interface |
| `PlaceholderAssetManager` | implements `AssetManager` | Colored rectangles labeled with the ID. | implements interface |

All DTOs from §5 are duplicated under `com.abhishri.escape.ui.api.dto`.

### Swing component lifecycle

`EscapeRoomApp.main` calls `SwingUtilities.invokeLater(() -> new MainFrame(...).setVisible(true))`. The `MainFrame` constructor (running on the EDT) instantiates `ScenePanel`, `InventoryPanel`, `DialoguePanel`, `StatusBar`, places them in a `BorderLayout`, wires action listeners (`StatusBar` Save button → `MainFrame.onSavePressed`; `ScenePanel` `MouseListener` → `MainFrame.onHotspotClicked`; `InventoryPanel` `ListSelectionListener` → updates internal state only), then calls `gameApiClient.newGame()` and pushes the returned DTO through `renderState(dto)`. `renderState` sets the current DTO field, then calls `scenePanel.setRoom(dto.getCurrentRoom())`, `inventoryPanel.setItems(dto.getInventory())`, `statusBar.update(dto)`, `dialoguePanel.append(dto.getDialogueMessage())`, and finally checks `dto.gameStatus == COMPLETE` to show the win dialog via `JOptionPane.showMessageDialog`. Every subsequent API response from a click handler ends with `renderState(dto)`. All of this runs on the EDT (handlers fire on the EDT, the synchronous `HttpClient.send()` runs inline per `idea.md §12`).

---

## 9. Sequence Diagrams

### 9.1 New game

```
EscapeRoomApp  MainFrame  GameApiClient   GameController   GameSessionService   Repos          H2
     |             |            |                |                |              |             |
 main|--invokeLater>|            |                |                |              |             |
     |             |new(client) |                |                |              |             |
     |             |--newGame()-->|                |                |              |             |
     |             |            |POST /api/game/new                |              |             |
     |             |            |-------------->| createNewGame()  |              |             |
     |             |            |               |---------------->| save(session) |             |
     |             |            |               |                 |--save(s)----->|             |
     |             |            |               |                 |               |INSERT GAME_SESSION
     |             |            |               |                 |--save(inv)--->|INSERT PLAYER_INVENTORY
     |             |            |               |                 |               |UPDATE GAME_SESSION SET INVENTORY_ID
     |             |            |               |<--GameStateDTO--|               |             |
     |             |            |<--201 JSON----|                                                |
     |             |<--DTO------|                                                                |
     |             |renderState(dto)                                                             |
```

SQL emitted (rough):
```
INSERT INTO PLAYER_INVENTORY (ID, GAME_SESSION_ID) VALUES (?, NULL);
INSERT INTO GAME_SESSION (ID, CURRENT_ROOM_ID, STATUS, CREATED_AT, LAST_UPDATED_AT, INVENTORY_ID) VALUES (?, 'room_foyer', 'IN_PROGRESS', ?, ?, ?);
SELECT * FROM ROOM WHERE ID = 'room_foyer';
-- Plus selects against ROOM_OBJECT_IDS, ROOM_CONNECTED_ROOMS, ROOM_PUZZLE_IDS to build the RoomDTO.
```

### 9.2 Examine + Pick up (clock riddle → desk_key)

```
User  ScenePanel  MainFrame  RiddleDialog  GameApiClient  GameController  PuzzleEvalSvc  Repos   H2
 |       |           |            |             |               |               |          |     |
click--->|onClick    |            |             |               |               |          |     |
         |---onHot-->|            |             |               |               |          |     |
         |           |--examine?->|             |               |               |          |     |
         |           |       (hotspot.puzzleId != null -> open dialog)         |          |     |
         |           |--new RiddleDialog-->|    |               |               |          |     |
                                  |confirm |    |               |               |          |     |
                                  |-input->|    |               |               |          |     |
                                  |attemptPuzzle("puzzle_clock",{answer:"11:47"})           |     |
                                  |             |--POST attempt->|               |          |     |
                                  |             |                |--attempt()-->|           |     |
                                  |             |                |              |--find--->|SELECT GAME_SESSION
                                  |             |                |              |          |SELECT PUZZLE LEFT JOIN children
                                  |             |                |              |--solved->|INSERT GAME_SESSION_SOLVED_PUZZLES
                                  |             |                |              |--reward->|INSERT PLAYER_INVENTORY_HELD_ITEMS
                                  |             |                |<--StateDTO---|           |     |
                                  |             |<--200 JSON-----|                                |
                                  |<--DTO-------|                                                 |
         |           |renderState(dto)                                                            |
         |           |  scenePanel.setRoom(...)  inventoryPanel.setItems(...)  dialogue.append(...)
```

SQL emitted:
```
SELECT * FROM GAME_SESSION WHERE ID = ?;
SELECT * FROM PUZZLE p LEFT JOIN RIDDLE_PUZZLE r ON r.ID = p.ID
                       LEFT JOIN COMBINATION_PUZZLE c ON c.ID = p.ID
                       LEFT JOIN SEQUENCE_PUZZLE s ON s.ID = p.ID
                       LEFT JOIN ITEM_USE_PUZZLE i ON i.ID = p.ID
 WHERE p.ID = 'puzzle_clock';
SELECT PREREQUISITE_ID FROM PUZZLE_PREREQUISITE_IDS WHERE PUZZLE_ID = 'puzzle_clock';
SELECT SOLVED_PUZZLE_ID FROM GAME_SESSION_SOLVED_PUZZLES WHERE GAME_SESSION_ID = ?;
INSERT INTO GAME_SESSION_SOLVED_PUZZLES VALUES (?, 'puzzle_clock');
SELECT * FROM PLAYER_INVENTORY WHERE ID = ?;
INSERT INTO PLAYER_INVENTORY_HELD_ITEMS VALUES (?, 'desk_key');
UPDATE GAME_SESSION SET LAST_UPDATED_AT = ? WHERE ID = ?;
```

### 9.3 Item use puzzle (magnifying glass on cipher wheel)

```
User  InvPanel  ScenePanel  MainFrame  GameApiClient  GameController  PuzzleEvalSvc  Repos   H2
 |    select----|            |            |               |               |          |     |
 |              |  (getSelectedItemId() = "brass_magnifying_glass")        |          |     |
 |              click cipher_wheel hotspot |               |               |          |     |
 |              |--onClick-->|             |               |               |          |     |
 |                           |-- selectedItem != null AND hotspot.objectType in {SCENERY,PUZZLE}
 |                           |--useItem("brass_magnifying_glass","cipher_wheel")             |
 |                           |             |--POST use-item->|              |          |     |
 |                           |             |                 |--useItem()->|           |     |
 |                           |             |                 |             |--find--->|SELECT GAME_SESSION
 |                           |             |                 |             |--checkInv|SELECT held items
 |                           |             |                 |             |--findPuz-|SELECT ITEM_USE_PUZZLE WHERE TARGET = 'cipher_wheel' AND REQUIRED = 'brass...'
 |                           |             |                 |             |--prereqs-|SELECT prereqs (puzzle_display_case must be solved)
 |                           |             |                 |             |--solved->|INSERT GAME_SESSION_SOLVED_PUZZLES
 |                           |             |                 |<--StateDTO--|           |     |
 |                           |             |<--200 JSON------|                               |
 |                           |<--DTO-------|                                                 |
 |                           |renderState(dto)  dialogue.append(outcomeMessage)              |
```

### 9.4 Win condition (final pneumatic-tube puzzle)

```
User -> ScenePanel.click(pneumatic_tube_terminal)
     -> MainFrame.onHotspotClicked -> useItem("manuscript_page","pneumatic_tube_terminal")
        |
        v
GameController -> PuzzleEvaluationService.useItem(...)
        |
        |-- load session  (SELECT GAME_SESSION ...)
        |-- check inventory (manuscript_page held? yes)
        |-- locate ItemUsePuzzle by (target='pneumatic_tube_terminal', required='manuscript_page')
        |   -> puzzle_terminal
        |-- check prereqs: all 5 others solved? yes
        |-- puzzle.attempt(...) -> true
        |-- session.solvedPuzzleIds.add("puzzle_terminal")
        |-- award reward 'evidence_token'  (INSERT PLAYER_INVENTORY_HELD_ITEMS)
        |
GameSessionService.buildStateDTO(session, outcomeMessage, PUZZLE_SOLVED):
        |
        |-- if (session.solvedPuzzleIds.containsAll(allPuzzleIds)) {
        |       session.status = COMPLETE;
        |       gameSessionRepository.save(session);   -- UPDATE GAME_SESSION SET STATUS='COMPLETE'
        |       dto.gameStatus = COMPLETE;
        |   }
        |
        v
MainFrame.renderState(dto):
        |
        |-- if (dto.gameStatus == COMPLETE) JOptionPane.showMessageDialog("You're free.");
```

SQL on win:
```
UPDATE GAME_SESSION SET STATUS='COMPLETE', LAST_UPDATED_AT=? WHERE ID=?;
```

### 9.5 Save then Load (restart in between)

```
-- Save phase --
User -> StatusBar.save -> MainFrame.onSavePressed -> GameApiClient.save(gameId)
   POST /api/game/{gameId}/save
GameController.save -> SaveLoadService.save(gameId)
   SELECT * FROM GAME_SESSION WHERE ID = ?;
   SELECT * FROM PLAYER_INVENTORY WHERE ID = ?;
   SELECT HELD_ITEM_ID FROM PLAYER_INVENTORY_HELD_ITEMS WHERE PLAYER_INVENTORY_ID = ?;
   SELECT SOLVED_PUZZLE_ID FROM GAME_SESSION_SOLVED_PUZZLES WHERE GAME_SESSION_ID = ?;
   ObjectMapper.writeValue(File("./saves/{gameId}-20260531T143200.json"), snapshot);
   -> SaveConfirmationDTO{filename, savedAt, sizeBytes}

-- Restart backend (Ctrl+C, mvn spring-boot:run) --
   H2 file is reopened; world data and old session are still in H2 because file-mode persists.
   (The save would still work if H2 had been wiped — the snapshot carries enough mutable state.)

-- Load phase --
User -> StatusBar.load -> MainFrame.onLoadPressed
   -> GameApiClient.listSaves(gameId)  GET /api/game/{gameId}/saves
   user picks filename in a JOptionPane.showInputDialog
   -> GameApiClient.load(gameId, filename)  POST /api/game/{gameId}/load
GameController.load -> SaveLoadService.load(gameId, filename)
   ObjectMapper.readValue(File, GameSnapshotDTO.class);
   UPDATE GAME_SESSION SET CURRENT_ROOM_ID=?, STATUS=?, LAST_UPDATED_AT=? WHERE ID=?;
   DELETE FROM GAME_SESSION_SOLVED_PUZZLES WHERE GAME_SESSION_ID = ?;
   INSERT INTO GAME_SESSION_SOLVED_PUZZLES VALUES (?, ?)...;
   DELETE FROM PLAYER_INVENTORY_HELD_ITEMS WHERE PLAYER_INVENTORY_ID = ?;
   INSERT INTO PLAYER_INVENTORY_HELD_ITEMS VALUES (?, ?)...;
   -> GameStateDTO -> renderState(dto)
```

---

## 10. `GameSession` State Machine

```
              createNewGame()
                  |
                  v
        +-------- NEW ---------+
        |  (transient: immediately becomes IN_PROGRESS
        |   after the first save in the controller)
        |
        | first JPA save with status='IN_PROGRESS'
        v
  +--> IN_PROGRESS ---- buildStateDTO detects winCondition --> COMPLETE
  |        |
  |        | (any state-changing endpoint)
  +--------+
                         (reserved, unused in Phase 1)
                                |
                                v
                              FAILED
                       (Phase 2: timer expires)
```

| Transition | Trigger | Guard | Side effects |
|---|---|---|---|
| (none) -> NEW | `createNewGame()` constructs a `GameSession` in memory with status `IN_PROGRESS` immediately (NEW is a logical, in-method state). | — | `GameSession` and `PlayerInventory` instances created. |
| NEW -> IN_PROGRESS | First `gameSessionRepository.save()`. | — | `INSERT INTO GAME_SESSION`. |
| IN_PROGRESS -> IN_PROGRESS | Any move/examine/pickup/attempt that doesn't satisfy the win guard. | not all puzzles solved | `UPDATE LAST_UPDATED_AT`; possible inventory/solved-puzzle inserts. |
| IN_PROGRESS -> COMPLETE | `GameSessionService.buildStateDTO` is called with a freshly mutated session. | `session.solvedPuzzleIds.containsAll(allPuzzleIds)` where `allPuzzleIds = puzzleRepository.findAll().stream().map(Puzzle::getId).toList()`. Computed once at startup and cached. | `UPDATE GAME_SESSION SET STATUS='COMPLETE'`; DTO carries `gameStatus=COMPLETE`. |
| IN_PROGRESS -> FAILED | (Phase 2) timer expires. | — | — |
| COMPLETE -> (terminal) | — | — | further state-changing endpoints return `409 INVALID_MOVE` with message "Game already complete." |

---

## 11. Error Handling Strategy

### 11a. Exception hierarchy

All custom exceptions extend `RuntimeException` (unchecked) so service methods stay clean.

| Exception | Thrown when | HTTP | `ApiErrorCode` |
|---|---|---|---|
| `GameNotFoundException` | `gameSessionRepository.findById(id).orElseThrow(...)` returns empty. | 404 | `GAME_NOT_FOUND` |
| `InvalidMoveException` | Target room not adjacent; object not in room; pickup of non-item. | 409 | `INVALID_MOVE` |
| `PuzzleNotFoundException` | `puzzleRepository.findById(id)` empty, or no `ItemUsePuzzle` matches the (item, target) pair. | 404 | `PUZZLE_NOT_FOUND` |
| `PrerequisiteNotMetException` | Puzzle's `prerequisitePuzzleIds` not all in `solvedPuzzleIds`. | 409 | `PREREQUISITE_NOT_MET` |
| `WrongAnswerException` | *Not thrown in Phase 1* — wrong answers return `200 + PUZZLE_FAILED`. Reserved for Phase 2 strict mode. | 422 | `WRONG_ANSWER` |
| `ItemNotInInventoryException` | Use-item request references an item the player isn't holding. | 409 | `ITEM_NOT_IN_INVENTORY` |
| `SaveLoadException` | `ObjectMapper.writeValue`/`readValue` throws `IOException`. | 500 | `SAVE_FAILED` or `LOAD_FAILED` |
| `MethodArgumentNotValidException` (Spring built-in) | `@Valid` body fails. | 400 | `INVALID_REQUEST` |
| `Throwable` (catch-all) | Anything else. | 500 | `INTERNAL_ERROR` |

### 11b. `@ControllerAdvice`

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleGameNotFound(GameNotFoundException ex, HttpServletRequest req);

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidMove(InvalidMoveException ex, HttpServletRequest req);

    @ExceptionHandler(PuzzleNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlePuzzleNotFound(PuzzleNotFoundException ex, HttpServletRequest req);

    @ExceptionHandler(PrerequisiteNotMetException.class)
    public ResponseEntity<ErrorResponseDTO> handlePrereq(PrerequisiteNotMetException ex, HttpServletRequest req);

    @ExceptionHandler(ItemNotInInventoryException.class)
    public ResponseEntity<ErrorResponseDTO> handleItemMissing(ItemNotInInventoryException ex, HttpServletRequest req);

    @ExceptionHandler(SaveLoadException.class)
    public ResponseEntity<ErrorResponseDTO> handleSaveLoad(SaveLoadException ex, HttpServletRequest req);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req);

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponseDTO> handleEverythingElse(Throwable ex, HttpServletRequest req);
}
```

Each handler returns `ResponseEntity.status(...).body(new ErrorResponseDTO(...))`. Logging at WARN for 4xx, ERROR with stack trace for 5xx.

### 11c. Frontend error handling

`GameApiClient` after `HttpClient.send()`:

```java
if (response.statusCode() >= 400) {
    ErrorResponseDTO err = objectMapper.readValue(response.body(), ErrorResponseDTO.class);
    throw new ApiException(err);
}
return objectMapper.readValue(response.body(), GameStateDTO.class);
```

`MainFrame` action handlers wrap every API call in a try/catch:

```java
try {
    GameStateDTO dto = client.move(gameId, targetRoomId);
    renderState(dto);
} catch (ApiException e) {
    JOptionPane.showMessageDialog(this,
        e.getError().getMessage(),
        "Action failed (" + e.getError().getErrorCode() + ")",
        JOptionPane.WARNING_MESSAGE);
}
```

The UI never crashes on backend errors; the player sees a dialog and keeps playing.

---

## 12. Logging Strategy

Spring Boot's default SLF4J + Logback. No custom `logback-spring.xml` required for Phase 1; everything is configured via `application.properties`.

**Levels per package:**

| Package | Level | Why |
|---|---|---|
| `root` | INFO | Spring Boot startup output, request/response milestones. |
| `com.abhishri.escape.controller` | INFO | One line per request: `POST /api/game/{id}/attempt-puzzle -> 200 PUZZLE_SOLVED`. |
| `com.abhishri.escape.service` | DEBUG | Decisions: prereq check, win condition met, save filename. |
| `com.abhishri.escape.repository` | WARN | Silent unless something breaks. |
| `org.hibernate.SQL` | INFO (off in prod) | `spring.jpa.show-sql=true` covers it in dev. |
| `org.springframework.web` | INFO | Mapping registration; CORS denials. |

**Format:** `%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`

**Events worth a log line:**

- `INFO  GameSessionService - New game created id=...`
- `DEBUG PuzzleEvaluationService - Attempt puzzle=puzzle_display_case correct=true reward=brass_magnifying_glass`
- `INFO  PuzzleEvaluationService - Win condition reached for game=...`
- `INFO  SaveLoadService - Saved game=... to file=... bytes=412`
- `INFO  SaveLoadService - Loaded game=... from file=...`
- `WARN  GlobalExceptionHandler - 4xx error code=PREREQUISITE_NOT_MET path=...`
- `ERROR GlobalExceptionHandler - Unhandled exception` + stack trace.

**PII:** none. The "user" is a single player on their own laptop; the game stores no real names or contact data. File logging to `./logs/escaperoom.log` (rolling not configured for Phase 1; revisit if logs exceed ~10 MB during demos).

---

## 13. Configuration Files

### 13a. `backend/src/main/resources/application.properties`

```properties
# --- Application identity ---
spring.application.name=escaperoom-backend

# --- HTTP server ---
server.port=8080
server.address=127.0.0.1
# Bind to loopback only; do not expose on LAN (see design.md section 18).

# --- Datasource (H2 file mode) ---
spring.datasource.url=jdbc:h2:file:./data/escaperoom;DB_CLOSE_ON_EXIT=FALSE
# File-mode H2: data persists across restarts. DB_CLOSE_ON_EXIT=FALSE lets Spring control shutdown.
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
# Empty password: local file DB, no security boundary; matches AP CS scope.

# --- JPA / Hibernate ---
spring.jpa.hibernate.ddl-auto=update
# 'update' is additive: adds new tables/columns, never drops. Wipe ./data/ to reset schema.
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
# Pretty-prints SQL in the console for debugging.

# --- H2 web console ---
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
# Browse to http://localhost:8080/h2-console (JDBC URL above, user 'sa', no password).
# Disable in Phase 2 if the app ever leaves the laptop.

# --- Logging ---
logging.level.root=INFO
logging.level.com.abhishri.escape=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=./logs/escaperoom.log
# Append-only log; rolling not configured in Phase 1.

# --- Game-specific config ---
escape.world.seed-file=classpath:world.json
# Read at startup by WorldSeedService.
escape.world.starting-room=room_foyer
# First room the player enters on POST /api/game/new.
escape.saves.directory=./saves
# Where SaveLoadService writes JSON snapshots. Created on first save.

# --- Jackson ---
spring.jackson.serialization.indent-output=true
# Pretty-print JSON responses in dev (makes save files readable too).
spring.jackson.default-property-inclusion=non_null
# Don't emit nulls in JSON; cleaner DTO output.
```

### 13b. `backend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.abhishri.escape</groupId>
        <artifactId>escaperoom-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>escaperoom-backend</artifactId>
    <packaging>jar</packaging>
    <name>Escape Room Backend</name>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.2.5</spring.boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
                <executions>
                    <execution>
                        <goals><goal>repackage</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 13c. `frontend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.abhishri.escape</groupId>
        <artifactId>escaperoom-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>escaperoom-frontend</artifactId>
    <packaging>jar</packaging>
    <name>Escape Room Swing Frontend</name>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <main.class>com.abhishri.escape.ui.EscapeRoomApp</main.class>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.17.0</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>2.17.0</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>${main.class}</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 13d. Parent `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.abhishri.escape</groupId>
    <artifactId>escaperoom-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Mystery Escape Room (parent)</name>

    <modules>
        <module>backend</module>
        <module>frontend</module>
    </modules>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

---

## 14. Build, Run, and Project Layout

### 14a. Final directory tree

```
game-java-dev/
├── pom.xml                          # parent aggregator, modules: backend, frontend
├── idea.md                          # vision & scope
├── design.md                        # this file
├── README.md                        # quickstart, points at design.md sections
├── .gitignore                       # see 14c
│
├── backend/
│   ├── pom.xml                      # Spring Boot 3.2.x, JPA, H2, Jackson
│   └── src/
│       ├── main/
│       │   ├── java/com/abhishri/escape/
│       │   │   ├── EscapeRoomApplication.java                # @SpringBootApplication main
│       │   │   ├── config/
│       │   │   │   ├── BackendConfig.java                    # CORS, ObjectMapper bean
│       │   │   │   └── seed/
│       │   │   │       ├── WorldSeed.java                    # root POJO
│       │   │   │       ├── RoomSeed.java
│       │   │   │       ├── ItemSeed.java
│       │   │   │       ├── PuzzleSeed.java                   # @JsonTypeInfo
│       │   │   │       ├── CombinationPuzzleSeed.java
│       │   │   │       ├── RiddlePuzzleSeed.java
│       │   │   │       ├── SequencePuzzleSeed.java
│       │   │   │       └── ItemUsePuzzleSeed.java
│       │   │   ├── controller/
│       │   │   │   ├── GameController.java                   # 10 endpoints
│       │   │   │   └── HealthController.java                 # /api/health
│       │   │   ├── service/
│       │   │   │   ├── GameSessionService.java
│       │   │   │   ├── PuzzleEvaluationService.java
│       │   │   │   ├── InventoryService.java
│       │   │   │   ├── SaveLoadService.java
│       │   │   │   └── WorldSeedService.java                 # @PostConstruct seeder
│       │   │   ├── domain/
│       │   │   │   ├── GameSession.java                      # @Entity root
│       │   │   │   ├── GameStatus.java                       # enum
│       │   │   │   ├── Room.java                             # @Entity
│       │   │   │   ├── InventoryItem.java                    # @Entity
│       │   │   │   ├── PlayerInventory.java                  # @Entity
│       │   │   │   └── puzzle/
│       │   │   │       ├── Puzzle.java                       # abstract @Entity JOINED
│       │   │   │       ├── CombinationPuzzle.java
│       │   │   │       ├── RiddlePuzzle.java
│       │   │   │       ├── SequencePuzzle.java               # @OrderColumn on expected
│       │   │   │       └── ItemUsePuzzle.java
│       │   │   ├── repository/
│       │   │   │   ├── GameSessionRepository.java
│       │   │   │   ├── RoomRepository.java
│       │   │   │   ├── PuzzleRepository.java                 # findByRoomId
│       │   │   │   ├── PlayerInventoryRepository.java
│       │   │   │   └── InventoryItemRepository.java
│       │   │   ├── dto/
│       │   │   │   ├── GameStateDTO.java
│       │   │   │   ├── RoomDTO.java
│       │   │   │   ├── RoomObjectDTO.java
│       │   │   │   ├── InventoryItemDTO.java
│       │   │   │   ├── MoveRequest.java
│       │   │   │   ├── ExamineRequest.java
│       │   │   │   ├── PickupRequest.java
│       │   │   │   ├── UseItemRequest.java
│       │   │   │   ├── AttemptPuzzleRequest.java
│       │   │   │   ├── LoadRequest.java
│       │   │   │   ├── SaveConfirmationDTO.java
│       │   │   │   ├── SaveMetadataDTO.java
│       │   │   │   ├── GameSnapshotDTO.java
│       │   │   │   ├── ErrorResponseDTO.java
│       │   │   │   └── LastActionResult.java                 # enum
│       │   │   └── exception/
│       │   │       ├── ApiErrorCode.java                     # enum
│       │   │       ├── GlobalExceptionHandler.java           # @ControllerAdvice
│       │   │       ├── GameNotFoundException.java
│       │   │       ├── InvalidMoveException.java
│       │   │       ├── PuzzleNotFoundException.java
│       │   │       ├── PrerequisiteNotMetException.java
│       │   │       ├── WrongAnswerException.java             # reserved for Phase 2
│       │   │       ├── ItemNotInInventoryException.java
│       │   │       └── SaveLoadException.java
│       │   └── resources/
│       │       ├── application.properties                    # full file in 13a
│       │       └── world.json                                # full content in 4b
│       └── test/
│           ├── java/com/abhishri/escape/
│           │   ├── PuzzleAttemptTests.java                   # JUnit 5 unit tests
│           │   ├── InventoryServiceTests.java                # mocked repos
│           │   ├── GameFlowIntegrationTest.java              # @SpringBootTest golden path
│           │   └── SaveLoadIntegrationTest.java
│           └── resources/
│               ├── application-test.properties               # in-memory H2
│               └── world-test.json                           # 1 room, 1 puzzle
│
├── frontend/
│   ├── pom.xml                      # shade plugin, exec plugin, Jackson
│   └── src/main/
│       ├── java/com/abhishri/escape/ui/
│       │   ├── EscapeRoomApp.java                            # main()
│       │   ├── MainFrame.java
│       │   ├── ScenePanel.java
│       │   ├── InventoryPanel.java
│       │   ├── DialoguePanel.java
│       │   ├── StatusBar.java
│       │   ├── hotspot/
│       │   │   ├── Hotspot.java
│       │   │   └── ObjectType.java                           # enum
│       │   ├── dialog/
│       │   │   ├── PuzzleDialog.java
│       │   │   ├── CombinationPuzzleDialog.java
│       │   │   ├── RiddlePuzzleDialog.java
│       │   │   └── SequencePuzzleDialog.java
│       │   ├── api/
│       │   │   ├── GameApiClient.java
│       │   │   ├── ApiException.java
│       │   │   └── dto/                                       # duplicated from backend dto/
│       │   │       └── (same 15 DTOs)
│       │   └── asset/
│       │       ├── AssetManager.java
│       │       ├── FileAssetManager.java
│       │       └── PlaceholderAssetManager.java
│       └── resources/
│           └── art/
│               ├── CREDITS.md                                # asset licensing log
│               └── thornwick/                                # empty in Phase 1
│
├── data/                            # H2 file DB (runtime, git-ignored)
├── saves/                           # JSON snapshots (runtime, git-ignored)
└── logs/                            # logback output (runtime, git-ignored)
```

### 14b. How to run

```text
1.  cd <project-root>                    # the directory containing this design.md (the parent pom.xml)
2.  mvn clean install                    # builds both modules, runs all tests
3.  In terminal A:  mvn -pl backend spring-boot:run
    -> Backend listens on http://127.0.0.1:8080
    -> H2 console at  http://127.0.0.1:8080/h2-console
       JDBC URL:  jdbc:h2:file:./data/escaperoom
       User:      sa     Password: (empty)
4.  In terminal B:  mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp
    -> Swing window opens; foyer scene appears.
5.  Smoke test:  curl http://127.0.0.1:8080/api/health   ->   {"status":"ok"}

Packaged JARs after `mvn clean install`:
    backend/target/escaperoom-backend-1.0.0-SNAPSHOT.jar
    frontend/target/escaperoom-frontend-1.0.0-SNAPSHOT.jar

Run from JAR:
    java -jar backend/target/escaperoom-backend-1.0.0-SNAPSHOT.jar
    java -jar frontend/target/escaperoom-frontend-1.0.0-SNAPSHOT.jar
```

### 14c. `.gitignore`

```gitignore
# --- Maven build output ---
target/
**/target/

# --- Runtime data (game state, saves, logs) ---
data/
saves/
logs/
*.mv.db
*.trace.db
*.lock.db

# --- IDE files ---
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# --- Java / OS ---
*.class
hs_err_pid*.log
Thumbs.db
.DS_Store

# --- Environment ---
*.env
*.local
```

---

## 15. Asset Specification

| Aspect | Contract |
|---|---|
| **Layout** | `frontend/src/main/resources/art/<theme>/rooms/<roomId>.png` for backgrounds; `art/<theme>/items/<assetKey>.png` for icons; `art/<theme>/hotspots/<objectId>.png` for optional overlays. |
| **Theme directory** | Phase 1 uses `thornwick`. New themes drop into a new folder; `FileAssetManager` takes the theme as a constructor argument. |
| **Room background size** | 1024 x 640 px, PNG, opaque or with alpha. `ScenePanel` is sized to match; window grows to fit. |
| **Item icon size** | 64 x 64 px PNG with alpha. `InventoryPanel` renders them at native size. |
| **Hotspot overlay** | Optional 1024 x 640 transparent PNG aligned to the background. Used for hover highlight effects in Phase 2. |
| **File format** | PNG only (24-bit + alpha). No JPG (lossy), no SVG (not natively supported). |
| **Naming** | `roomId` and `assetKey` come from `world.json` — never duplicated, never derived. Renaming a room in `world.json` requires renaming the PNG. |
| **Fallback chain** | `MainFrame` instantiates a `FileAssetManager(theme)` wrapped by a `PlaceholderAssetManager` fallback (composite pattern). `FileAssetManager.getX(key)` returns `null` if the file is missing; the wrapper then asks the placeholder. Phase 1 ships with the file directory empty, so every call falls through to placeholders. |
| **License tracking** | `frontend/src/main/resources/art/CREDITS.md` lists every imported asset: file path, source URL, license (must be CC0, CC-BY 3.0/4.0, or original work), creator name. Adding a PNG without a CREDITS.md entry is a code-review block. |

---

## 16. Test Strategy (Phase 1)

Pitched at AP CS level: enough to demonstrate correctness on the golden path, not industrial-grade coverage.

### Backend unit tests (JUnit 5, no Spring context — ~15 tests)

File: `backend/src/test/java/com/abhishri/escape/PuzzleAttemptTests.java`

| Test | Verifies |
|---|---|
| `combinationPuzzle_correctCode_returnsTrue` | `CombinationPuzzle.attempt(Map.of("code","384"))` is `true`. |
| `combinationPuzzle_wrongCode_returnsFalse` | Wrong digits return `false`. |
| `combinationPuzzle_missingInput_returnsFalse` | Empty map returns `false`. |
| `riddlePuzzle_correctAnswer_caseInsensitive` | "thornwick" matches "THORNWICK" when `caseSensitive=false`. |
| `riddlePuzzle_correctAnswer_caseSensitive` | "thornwick" does *not* match when `caseSensitive=true`. |
| `riddlePuzzle_whitespaceTrimmed` | "  11:47  " matches "11:47". |
| `sequencePuzzle_correctOrder_returnsTrue` | Comma-separated input matches `expectedSequence`. |
| `sequencePuzzle_wrongOrder_returnsFalse` | Reversed sequence returns `false`. |
| `sequencePuzzle_extraItems_returnsFalse` | Length mismatch returns `false`. |
| `itemUsePuzzle_correctItemAndTarget_returnsTrue` | Both match. |
| `itemUsePuzzle_wrongItem_returnsFalse` | |
| `itemUsePuzzle_wrongTarget_returnsFalse` | |

File: `backend/src/test/java/com/abhishri/escape/InventoryServiceTests.java` (Mockito-mocked repos)

| Test | Verifies |
|---|---|
| `addItem_appendsToHeldItemIds` | |
| `addItem_duplicateRejected` | Calling twice does not duplicate. |
| `hasItem_trueWhenHeld` | |

### Backend integration tests (`@SpringBootTest` + `application-test.properties` with in-memory H2 — ~3 tests)

File: `backend/src/test/java/com/abhishri/escape/GameFlowIntegrationTest.java`

| Test | Verifies |
|---|---|
| `goldenPath_solveAllPuzzles_winConditionFires` | POST /new, then walk through every puzzle via the API, assert final `gameStatus == COMPLETE`. |
| `invalidMove_returns409` | Attempting to move to a non-adjacent room returns 409 + `INVALID_MOVE`. |

File: `backend/src/test/java/com/abhishri/escape/SaveLoadIntegrationTest.java`

| Test | Verifies |
|---|---|
| `saveThenLoad_restoresState` | Save mid-game, mutate further, load, assert state matches the saved snapshot. |

### Frontend tests

None in Phase 1. Manual smoke test = golden-path walkthrough from `idea.md §3`. Recorded as a checklist in §20.

### Test data

`backend/src/test/resources/world-test.json` — one room (`test_room`), one `RiddlePuzzle` with answer "yes". Used only by integration tests via `@ActiveProfiles("test")` and `escape.world.seed-file=classpath:world-test.json`.

### Dependencies

`spring-boot-starter-test` (already in §13b) brings JUnit 5, AssertJ, Mockito, MockMvc, `@SpringBootTest` support. Nothing else needed.

---

## 17. Performance and Threading Notes

- **Concurrency:** the backend serves one player. No `@Transactional` tuning, no isolation level overrides, no connection pool sizing — Spring's defaults are abundant.
- **JPA session lifecycle:** Spring's `OpenEntityManagerInView` filter is left on (default). Lazy collections render fine through DTO assembly.
- **Swing threading:** every `MainFrame.renderState` call occurs inside an event handler that the EDT invoked (`ActionListener`, `MouseListener`, or `SwingUtilities.invokeLater` from `main`). No state mutates from another thread.
- **HTTP latency budget:** `HttpClient.send()` against `localhost:8080` is consistently sub-10 ms for these payloads (largest response is `GameStateDTO` with 6 inventory items ≈ 2 KB JSON). Synchronous EDT calls are imperceptible. See `idea.md §12` for the deferred async work.
- **Memory:** H2 file DB at Phase 1 scale (3 rooms, 6 puzzles, ~5 items, single session) is under 100 KB on disk. JVM heap for the backend stays under 200 MB. Swing process under 150 MB.

---

## 18. Security Posture (Phase 1)

This game has no security boundary. Treat as a local-only application:

- Backend binds to `127.0.0.1:8080` (`server.address=127.0.0.1`) — explicitly *not* `0.0.0.0`. The app cannot be reached from another machine on the LAN without reconfiguration.
- No authentication, no authorization. Anyone with localhost access has full game control. Acceptable: the only user is the player on the same machine.
- No PII. The save files contain UUIDs and game state. No real names, emails, addresses anywhere.
- No secrets in source. H2 uses the default `sa`/empty credentials.
- H2 console (`/h2-console`) is enabled because it is genuinely useful for debugging and grading discussion. **Phase 2: set `spring.h2.console.enabled=false` before any deployment beyond a single laptop.**
- CORS allows `http://localhost:3000` and `http://localhost:5173` only — narrow allowlist for the future React port. No wildcard origins.
- Save filename validation: `LoadRequest.filename` is rejected if it contains `/`, `\`, or `..` to prevent path traversal into `./saves/`.

---

## 19. Open Implementation Risks

Real risks surfaced by this design pass — not items already resolved in `idea.md §12`.

1. **`@ElementCollection` ordering loss.** Plain `List<String>` columns are stored as bags by Hibernate; iteration order is undefined. For `SequencePuzzle.expectedSequence` this is fatal — the sequence is the puzzle. We must annotate it with `@OrderColumn(name = "POSITION")`, which produces the `SEQUENCE_PUZZLE_EXPECTED.POSITION` column shown in §3a. Easy to forget; verify in H2 console after first seed.
2. **H2 file-mode single-writer lock.** H2 in file mode holds an exclusive OS file lock. If Abhishri accidentally runs `mvn spring-boot:run` twice (two terminals), the second backend fails at startup with `Database may be already in use`. Document this loudly in the README troubleshooting section and consider adding a startup banner that prints the absolute DB path.
3. **Jackson polymorphic seed deserialization.** The `@JsonSubTypes` enum names in `PuzzleSeed.java` (`COMBINATION`, `RIDDLE`, `SEQUENCE`, `ITEM_USE`) must exactly match the `type` strings in `world.json`. A typo causes `WorldSeedService` to throw `JsonMappingException` at startup, which aborts the boot — loud failure, easy to fix, but Abhishri should know to look in the stack trace for the offending puzzle ID.
4. **`@OneToOne` insertion order.** `GameSession` owns the FK to `PlayerInventory`. We must `inventoryRepository.save(inv)` first, then attach to the `GameSession`, then save the session. Doing it backwards yields a transient-instance exception. Documented in `GameSessionService.createNewGame()` as a code comment.
5. **Save-file forward compatibility.** `GameSnapshotDTO.schemaVersion` is hardcoded to `1`. If a Phase 2 change adds a field, old saves still load (Jackson tolerates missing fields), but a Phase 2 *rename* will silently null out the old field. The `schemaVersion` field exists so `SaveLoadService.load` can detect and refuse incompatible versions before silent corruption.

---

## 20. Phase 1 Acceptance Criteria

Mentor walks this list at the end of Phase 1. Every box checked = done.

- [ ] `mvn clean install` from the repo root builds both modules with zero errors and zero test failures.
- [ ] Backend starts via `mvn -pl backend spring-boot:run` and binds to `127.0.0.1:8080`.
- [ ] `GET http://127.0.0.1:8080/api/health` returns `{"status":"ok"}`.
- [ ] `http://127.0.0.1:8080/h2-console` lists all 17 tables from §3a (use the H2 console "Tables" panel).
- [ ] Inspection in H2 console after first boot shows: 3 rows in `ROOM`, 6 rows in `PUZZLE` distributed across the four child tables as **2 in `RIDDLE_PUZZLE`** (`puzzle_clock`, `puzzle_iron_chest`), **1 in `COMBINATION_PUZZLE`** (`puzzle_display_case`), **1 in `SEQUENCE_PUZZLE`** (`puzzle_bookshelf`), **2 in `ITEM_USE_PUZZLE`** (`puzzle_cipher_wheel`, `puzzle_terminal`) — totaling 6. **5 rows in `INVENTORY_ITEM`**.
- [ ] Frontend launches via `mvn -pl frontend exec:java` and the Swing window displays the foyer scene with hotspot rectangles labeled.
- [ ] The full golden path from `idea.md §3` (steps 1-13) completes end-to-end purely from clicks; the win screen `JOptionPane` appears with the closing dialogue text.
- [ ] Pressing "Save" creates `./saves/<gameId>-<timestamp>.json`. The file is human-readable and contains the `GameSnapshotDTO` fields.
- [ ] Restarting the backend, then pressing "Load" and selecting the saved file, restores the player to the same room with the same inventory and the same solved puzzles.
- [ ] All ~15 backend unit tests pass.
- [ ] All 3 backend integration tests pass.
- [ ] Editing `world.json` (e.g., change `puzzle_clock.expectedAnswer` from `"11:47"` to `"11:48"`), **deleting `./data/` to clear the seeded H2 file** (`WorldSeedService` only seeds an empty DB by design), restarting the backend, and attempting the clock puzzle confirms the new answer is required — no recompile.
- [ ] AP CS rubric matrix from `idea.md §9`: for every concept, Abhishri can name the class/method that demonstrates it without consulting notes.