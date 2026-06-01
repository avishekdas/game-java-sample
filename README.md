# Mystery Escape Room (Season 3)

Single-player point-and-click escape room game. Java 17 + Spring Boot 3 backend, Java Swing frontend, H2 embedded database. AP Computer Science final project — student: Abhishri Das.

## Status

Phase 1 (MVP) — **complete**. Phase 2 (UI upgrade, branch `ui-improvement`) — **complete**.
150 tests pass (86 backend + 64 frontend). Zero backend changes in the UI upgrade.

## Documentation

- **[`idea.md`](./idea.md)** — vision, scope, theme, AP CS rubric mapping, resolved architectural decisions, MVP build order.
- **[`design.md`](./design.md)** — implementation-ready blueprint: DDL, DTOs, REST API reference, sequence diagrams, config files, acceptance criteria.
- **[`design_ui_upgrade.md`](./design_ui_upgrade.md)** — UI upgrade design: color palette, typography, component specs, procedural art algorithm.
- **[`plan_ui_upgrade.md`](./plan_ui_upgrade.md)** — UI upgrade TDD milestone plan (UI-M1 through UI-M5).

## Prerequisites

- JDK 17 (Temurin recommended). **Important on macOS:** the system default JVM may be older. Set `JAVA_HOME` explicitly before every Maven command:
  ```
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
  ```
- Maven 3.8+
- (Optional) An IDE that understands Maven multi-module projects: IntelliJ IDEA, Eclipse, or VS Code with the Java extension pack.

## Running the Game (Manual Testing)

You need **two terminals** open at the repo root. Keep both running while you play.

### Step 1 — Run all tests (optional sanity check)

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
  mvn --offline clean test
```

Expected: `BUILD SUCCESS`, 150 tests, 0 failures.

### Step 2 — Start the backend (Terminal 1)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn -pl backend spring-boot:run
```

Wait for this line in the log before starting the frontend:
```
Tomcat started on port 8080
```

Verify the backend is up (in any terminal):
```bash
curl http://127.0.0.1:8080/api/health
```
Expected response: `{"status":"UP"}` (or similar).

### Step 3 — Start the frontend (Terminal 2)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp
```

The Swing window opens. Click **New Game** in the top toolbar to begin.

### Step 4 — Stop

Press `Ctrl+C` in Terminal 1 to stop the backend. Closing the Swing window stops the frontend.

## What to Verify (UI Upgrade Checklist)

After clicking New Game, walk through these checks before playing the full golden path:

**Theme**
- [ ] Window background is dark (near-black), not grey
- [ ] Status bar is dark with brass-bordered buttons in a serif font
- [ ] Dialogue panel has a parchment-coloured text area with a thin brass border
- [ ] Inventory panel is dark-background with a brass-outlined titled border

**Scene panel**
- [ ] Entry Foyer background is atmospheric dark art (amber gradient glow + pillar silhouettes), not a solid blue rectangle
- [ ] Hotspots are rounded brass-outlined overlays, not plain yellow rectangles
- [ ] Moving the mouse over a hotspot changes the cursor to a hand and brightens the border
- [ ] Moving off a hotspot reverts the cursor and border
- [ ] Clicking "→ Reading Hall" shows a ~200ms crossfade as the scene transitions
- [ ] Performing an action (examine, pickup) in the same room does **not** trigger a flash

**Progress dots**
- [ ] Status bar centre shows 6 hollow dots (○○○○○○) at game start
- [ ] Each solved puzzle fills one dot gold (●○○○○○ after first solve)

**Dialogs**
- [ ] Opening the Wall Clock riddle dialog: background is dark, text field is parchment-coloured
- [ ] Opening the Display Case combination dialog: spinners are parchment-coloured
- [ ] OK and Cancel buttons in all dialogs are dark with brass borders

**Inventory**
- [ ] After picking up an item, the inventory cell shows a small icon (unique shape per item)
- [ ] Clicking an inventory item changes the panel title to `USING: [item name]`
- [ ] Clicking elsewhere (or using the item) reverts the title to `INVENTORY`

**Solved state**
- [ ] After solving the Wall Clock, navigate away and back — that hotspot shows `Wall Clock ✓` with a green-tint overlay

## Project Layout

See `design.md` §14 for the full tree. Quick reference:

```
game-java-dev/
├── backend/                  Spring Boot REST API + JPA + H2
├── frontend/                 Swing client + HTTP client
├── data/                     H2 file DB (created at runtime; gitignored)
├── saves/                    JSON save snapshots (created at runtime; gitignored)
├── logs/                     Application logs (gitignored)
├── idea.md                   What and why
├── design.md                 How (implementation blueprint)
└── README.md                 This file
```

## Troubleshooting

### "Database may be already in use" on backend startup

H2 in file mode holds an exclusive OS-level lock on `./data/escaperoom.mv.db`. If you accidentally launch a second backend instance (a second `mvn spring-boot:run` in another terminal, or a leftover process from a previous run), the second one fails to start with this error.

**Fix:**

1. Check for a stray `java` process holding the lock:
   - Windows: `Get-Process java` in PowerShell, or check Task Manager.
   - macOS/Linux: `ps aux | grep java`.
2. Stop the orphaned process.
3. If H2 still complains after the process is gone, delete the lock file: `./data/escaperoom.mv.db.lock.db` (only the `.lock.db` file — never the main `.mv.db`).
4. Restart the backend.

### Frontend opens but shows "Connection refused" or blank scene

The backend is not running, or it's running on a different port. Confirm with `curl http://127.0.0.1:8080/api/health`. If that fails, restart the backend before re-launching the frontend.

### Changing `world.json` doesn't take effect

`WorldSeedService` only seeds the H2 database **if a row is missing**. To force a re-seed after editing `world.json`:

1. Stop the backend.
2. Delete the `./backend/data/` directory (this is where H2 writes when launched via `mvn -pl backend spring-boot:run`).
3. Restart the backend. The new `world.json` content is loaded into a fresh database.

### Tests fail with "Port 8080 already in use"

An integration test is trying to start the backend while a manually-launched backend is still running on `:8080`. Stop the manual backend before running `mvn test`.

### H2 console shows no tables

The backend hasn't finished its first startup yet (Spring needs a moment to create the schema). Wait until the console log shows `Tomcat started on port 8080` and refresh `http://127.0.0.1:8080/h2-console`. Login JDBC URL must be `jdbc:h2:file:./data/escaperoom` — leave username `sa`, password blank.

## How to Play

### Story

You are Abhishri, and you've woken up on the cold marble floor of Thornwick Municipal Library at midnight. The front door is sealed by an electromagnetic lock. Head Librarian Mira has vanished, leaving only a note: *"The answer is always in the books."* Reconstruct the timeline of a stolen manuscript, assemble the evidence, and transmit it via the pneumatic-tube terminal in the archives to escape.

### Controls

The game is fully mouse-driven. Every interactive object in the scene is a labeled hotspot rectangle you click with the left mouse button.

- **Click a hotspot** to examine it, pick up an item, or open a puzzle dialog.
- **Click an exit label** (e.g., "→ Reading Hall") to move to the adjacent room.
- **Select an item in the Inventory panel** (right side) before clicking a hotspot to use that item on the object.
- **Dialogue panel** (bottom) shows the server's response text after every action — read it for clues.
- **Status bar** (top) shows the current room name, how many puzzles you've solved, and the Save / Load / New Game buttons.

### Starting a game

Click **New Game** in the status bar. A game ID is assigned and the Entry Foyer scene loads. If you already have a save, click **Load** and pick the slot from the list.

### Room map

```
Entry Foyer  ──────  Reading Hall  ──────  The Archives
```

You start in the Entry Foyer. The Reading Hall is accessible immediately. The Archives open from the Reading Hall.

### Puzzle walkthrough (golden path)

Work through the puzzles in this order. You can explore non-linearly, but prerequisites must be met before a puzzle unlocks.

**1. Wall Clock — Entry Foyer (Riddle)**

Click the **wall clock** hotspot. A riddle dialog opens:
> *"The clock's hands are frozen. The frame reads: 'When does the library's silence begin? Answer in HH:MM.'"*

Enter `11:47` and click OK. A compartment opens under the reception desk.

**Reward:** the `Desk Key` is added to your inventory.

---

**2. Display Case — Reading Hall (Combination)**

Navigate to the **Reading Hall** and click the **Display Case** hotspot. A three-dial combination dialog opens.

The three digits are hidden in the room flavor text across all three rooms:
- Foyer: *"3 brass nails"* → **3**
- Reading Hall: *"Eight tall bookshelves"* → **8**
- Archives: *"4 long rows"* → **4**

Enter `384` and click OK.

**Reward:** `Brass Magnifying Glass` added to inventory.

---

**3. Book Cart — Reading Hall (Sequence)**

Click the **Book Cart** hotspot. A sequence dialog opens listing six books by Dewey Decimal number.

Arrange them in ascending Dewey order: `510, 520, 610, 621, 720, 810` and click OK. The fireplace compartment unlocks.

**Reward:** `Fireplace Scrap` added to inventory. (This is a clue that confirms the second digit of the combination above — you already have it by now.)

---

**4. Cipher Wheel — The Archives (Item Use)**

Navigate to **The Archives**. First, make sure `Brass Magnifying Glass` is selected in your inventory panel.

Click the **Cipher Wheel** hotspot. The server automatically uses the magnifying glass on the wheel.

> *"Under the lens, etched letters resolve into a single word: THORNWICK."*

No item is added — you learn the passphrase.

*Prerequisite: Display Case (puzzle 2) must be solved first.*

---

**5. Iron Chest — The Archives (Riddle)**

Click the **Iron Chest** hotspot. A passphrase dialog opens:
> *"The chest's brass plate reads: 'Speak the name of this place to open me.'"*

Enter `THORNWICK` (not case-sensitive) and click OK.

**Reward:** `Manuscript Page` added to inventory.

*Prerequisite: Cipher Wheel (puzzle 4) must be solved first.*

---

**6. Pneumatic-Tube Terminal — The Archives (Item Use) — WIN CONDITION**

Select `Manuscript Page` in the inventory panel, then click the **Pneumatic-Tube Terminal** hotspot.

> *"The tube hisses. Evidence received. The front door's electromagnetic lock disengages. You are free."*

The game ends with a win dialog. All six puzzles must be solved before this step succeeds.

---

### Saving and loading

- Click **Save** at any time to write a snapshot to `./saves/`. The slot name includes a timestamp.
- Click **Load** to see your save slots and restore one. Your room, inventory, and solved puzzles are all restored.
- The Save button is disabled after you win.

### Quick-reference answer sheet

| Puzzle | Type | Answer |
|--------|------|--------|
| Wall Clock | Riddle | `11:47` |
| Display Case | Combination | `384` |
| Book Cart | Sequence | `510, 520, 610, 621, 720, 810` |
| Cipher Wheel | Item Use | select Magnifying Glass, click wheel |
| Iron Chest | Riddle | `THORNWICK` |
| Pneumatic Terminal | Item Use | select Manuscript Page, click terminal |

## License

Project code is for educational use as part of an AP Computer Science final project. Any third-party art assets are tracked with their original license in `frontend/src/main/resources/art/CREDITS.md` (added in Phase 2 when real art replaces placeholders).
