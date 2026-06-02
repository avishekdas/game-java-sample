# Thornwick UI Upgrade — `plan_ui_upgrade.md`

> Companion to `design_ui_upgrade.md` (what & how). This document is the **execution plan**: a TDD-driven, milestone-based sequence of work that converts the visual overhaul design into shipped code on the `ui-improvement` branch. Every milestone produces a testable build slice. No milestone is finished until its acceptance gate passes.

---

## 1. North Star

The UI upgrade is "done" when **every checkbox** in `design_ui_upgrade.md §9` is green and the full golden path plays through the visually upgraded game.

Restated as a single sentence:

> A player launches the Swing app, sees a dark Gothic atmospheric scene with styled hotspots, navigates three rooms with a smooth crossfade, solves six puzzles in styled dialogs with parchment-colored inputs, watches a procedurally drawn cipher wheel get revealed by the magnifying glass icon, sees ●●●●●● turn gold one by one in the status bar, and wins — all backed by the unchanged Spring Boot backend, with all 102 existing tests still green.

Every decision during build is tested against: **does this advance us toward that sentence without erasing any AP CS rubric demonstration?**

---

## 2. Guiding Principles

1. **Test-first, always.** No production class changes before the test that asserts what it must do. The Red → Green → Refactor loop applies to every behavior.
2. **Milestones ship a visible slice.** Each milestone produces something a human can see on screen or a test can verify. No "invisible prep" milestones.
3. **Zero backend changes.** Every milestone gate verifies this: the `backend/` directory is untouched.
4. **Done = automated + manual.** A milestone is complete when (a) all new tests pass, (b) all 102 prior tests still pass, (c) the manual demo produces the documented result.
5. **AP CS rubric is load-bearing.** Any refactor that erases a rubric demonstration (removes `paintComponent`, removes a custom class, simplifies a loop away) is a regression, not a cleanup.
6. **No new Maven dependencies.** All APIs used are standard JDK: `java.awt.*`, `javax.swing.*`, `javax.imageio.*`. Maven build files are not touched.

---

## 3. TDD Approach for UI Work

Swing's paint/render path is not unit-testable at the pixel level. The following tiered approach is used, matching the existing Phase 1 frontend TDD convention:

**Tier 1 — Strict TDD (pure Java, no Swing rendering):**
- `ThemeConstants` — constant values, font/color non-null assertions
- `ProceduralAssetManager` — image dimensions, non-null returns, distinct outputs per room/item
- `Hotspot` — constructor backward compat, `isSolved()` correctness
- `MainFrame.buildHotspots()` / `applyState()` — null-safety, solved-flag stamping
- `InventoryPanel` icon cache — call-count verification via a counting stub `AssetManager`
- `PuzzleDialog` input extraction — existing tests must continue to pass unchanged (this is the C3 regression gate)

**Tier 2 — Smoke + behavioral assertion (Swing EDT, `SwingUtilities.invokeAndWait`):**
- `ScenePanel` hover state — construct panel, inject hotspots, call internal `findHotspotAt()`, assert `hoveredHotspotId`
- `ScenePanel` same-room fade guard — call `setCurrentRoomId()` twice with same value, assert timer not restarted
- `StatusBar` dot panel — `setSolvedCount()` call, verify both values stored
- `DialoguePanel` parchment colors — `textArea.getBackground()` matches `ThemeConstants.PARCHMENT`
- `InventoryPanel` use-mode indicator — select item, assert title text contains "USING:"
- `PuzzleDialog` theme — content pane background is `ThemeConstants.DARK_WOOD`; parchment input fields are `ThemeConstants.PARCHMENT` after construction

**Tier 3 — Manual demo only:**
- Visual appearance of all panels (color fidelity to `design_ui_upgrade.md §3.2`)
- Crossfade animation smoothness (~200ms, visible between rooms)
- Cursor change to hand on hotspot hover
- Hotspot overlay and ` ✓` suffix on solved puzzles
- Procedural room background visual distinctness per room
- PuzzleDotsPanel rendering (filled vs hollow dots)
- Icon shapes in inventory

---

## 4. Milestone Map

7 milestones. UI-M1–M5 execute the visual overhaul; UI-M6–M7 add the visual hint system (Option A room art clues + Option B examination cards). Build order: foundation → structural plumbing → scene → panels → dialogs → room art counting → hint card overlays.

| #     | Name                                         | Primary output                                               |
|-------|----------------------------------------------|--------------------------------------------------------------|
| UI-M1 | Foundation — ThemeConstants + ProceduralAssetManager | Color/font palette live; procedural room art and item icons rendering |
| UI-M2 | Scaffolding — Hotspot + MainFrame plumbing   | `Hotspot.solved` field; null-safe solved-state flow in MainFrame |
| UI-M3 | Scene — ScenePanel visual rework             | Styled hotspots, hover, solved overlay, crossfade, `FileAssetManager` fallback swap |
| UI-M4 | Panels — StatusBar, DialoguePanel, InventoryPanel | Dark theme on all non-dialog panels; icon cache; dot progress; parchment dialogue |
| UI-M5 | Dialogs + Final Gate                         | Dark-themed puzzle dialogs; full `design_ui_upgrade.md §9` acceptance gate green |
| UI-M6 | Room Art Counting Clues (Option A)           | 8 bookshelves in Reading Hall; 3 brass nail circles in Foyer doorframe |
| UI-M7 | Examination Hint Cards (Option B)            | `getHintCard()` on `AssetManager`; 4 procedural card images; `ScenePanel` overlay; `MainFrame` trigger logic |

---

## 5. Universal Definition of Done (every milestone)

Before a milestone is marked complete:

- [ ] All new tests pass
- [ ] All 102 prior tests still pass (`mvn --offline clean test` from repo root exits 0)
- [ ] No new entries in `backend/` diff (`git diff --name-only` shows only `frontend/` and doc files)
- [ ] No new Maven dependencies in any `pom.xml`
- [ ] Manual demo produces the documented visual result on the running game
- [ ] No hardcoded hex/RGB values in source — all colors reference `ThemeConstants`

---

## 6. Milestones

---

### UI-M1 — Foundation: `ThemeConstants` + `ProceduralAssetManager`

**Goal.** The color palette, fonts, and procedural art renderer exist as tested, standalone classes. No existing class is touched. All subsequent milestones import `ThemeConstants` for colors rather than hardcoding values.

**North-star tie.** Everything visual in milestones UI-M2 through UI-M5 depends on `ThemeConstants` for correctness and `ProceduralAssetManager` for atmospheric room art. Building and testing this first isolates the palette from component layout concerns.

**Pre-conditions.** `design_ui_upgrade.md` is architecture-reviewed (status field updated). `ui-improvement` branch checked out.

**Red — write these tests first.**

`ThemeConstantsTest.java` (new):
- All 12 color constants (`NIGHT_BLACK`, `DARK_WOOD`, `AGED_BRASS`, `BRASS_GLOW`, `PARCHMENT`, `PARCHMENT_TEXT`, `CANDLE_TEXT`, `DIM_TEXT`, `SOLVED_OVERLAY`, `EXIT_OVERLAY`, `SELECTION_GLOW`, `ERROR_TEXT`) are non-null `java.awt.Color` instances
- All 6 font constants (`FONT_TITLE`, `FONT_BODY`, `FONT_LABEL`, `FONT_BUTTON`, `FONT_INVENTORY`, `FONT_SMALL`) are non-null `java.awt.Font` instances
- `applyDarkButton(JButton)` sets the button's font to `FONT_BUTTON`, background to `DARK_WOOD`, foreground to `CANDLE_TEXT`

`ProceduralAssetManagerTest.java` (new):
- `getBackground("room_foyer")` returns a non-null `java.awt.Image`, cast to `BufferedImage`, width 800, height 500, type `TYPE_INT_RGB`
- Same assertions for `"room_reading_hall"` and `"room_archives"`
- The three room backgrounds are not pixel-identical: at least one pixel in center column differs between foyer and archives (structural silhouettes place different shapes)
- `getItemIcon("item_key")` returns non-null, `BufferedImage`, 64×64, type `TYPE_INT_ARGB`
- Same dimension assertions for all five asset keys: `item_key`, `item_lens`, `item_scrap`, `item_manuscript`, `item_token`
- The five item icons are not all identical: key icon and lens icon differ at the center pixel
- `getItemIcon("unknown_key")` returns a non-null 64×64 image (graceful unknown-key fallback — the base chip with no symbol drawn)
- `getBackground("unknown_room")` returns a non-null 800×500 image (graceful fallback — gradient with no structural shapes)

`FileAssetManagerFallbackTest.java` (existing test — must still pass; no changes needed, just run it to confirm the swap in UI-M3 will keep it green).

**Green.**

`ThemeConstants.java` (new):
- Package: `com.abhishri.escape.ui`
- All `public static final Color` fields from `design_ui_upgrade.md §3.2`
- All `public static final Font` fields from `design_ui_upgrade.md §3.3`
- `public static void applyDarkButton(JButton b)` — sets background, foreground, font, border (`LineBorder(AGED_BRASS, 1)`), focus-painted false, opaque true

`ProceduralAssetManager.java` (new):
- Package: `com.abhishri.escape.ui`
- Implements `AssetManager`
- `getBackground(String roomId)` — full layer algorithm from `design_ui_upgrade.md §5.1`: base fill → two-pass `RadialGradientPaint` → room-specific structural silhouettes via `switch(roomId)` → vignette → room label
- `getItemIcon(String assetKey)` — `RoundRectangle2D` base chip → symbol via `switch(assetKey)` per per-icon shape table in `design_ui_upgrade.md §5.1`; unknown key returns chip with no symbol; antialiasing on; draw order: fill chip → stroke chip → fill symbol → stroke symbol (lens interior before outer stroke)
- All color references via `ThemeConstants.*`, no hardcoded hex

**Refactor.** If the structural-silhouette drawing for each room grows past 20 lines per case, extract private helpers `paintFoyerSilhouettes(Graphics2D)` etc. to keep the switch readable.

**Acceptance.**

- Automated: `ThemeConstantsTest` (3 assertions) + `ProceduralAssetManagerTest` (14 assertions) all pass. `mvn --offline clean test` exits 0.
- Manual demo: write a standalone `main()` in a scratch class (deleted after demo), instantiate `ProceduralAssetManager`, call `getBackground("room_foyer")`, render in a `JFrame`. Confirm: dark background, two amber gradient glows, two pillar silhouettes, arched door shape. Repeat for `room_archives` — confirm different shapes (cabinets, cipher wheel circle). Confirm `getItemIcon("item_key")` shows a key shape. Delete scratch class before committing.

---

### UI-M2 — Scaffolding: `Hotspot` + `MainFrame` plumbing

**Goal.** `Hotspot` gains a `solved` boolean field with full backward compatibility. `MainFrame.applyState()` gains a null-safe `solvedIds` computation, and `buildHotspots()` stamps the solved flag via `solvedIds.contains(obj.getPuzzleId())`. No visual change yet — this milestone exists purely to make the data correct before the visual work begins.

**North-star tie.** `ScenePanel.paintComponent()` (UI-M3) reads `hotspot.isSolved()` to choose the overlay color. If the flag is wrong or the null guard is missing, every puzzle-solve state will render incorrectly or NPE on new-game. Establishing correct data flow here prevents regressions in all later milestones.

**Pre-conditions.** UI-M1 complete.

**Red — write these tests first.**

`HotspotSolvedFieldTest.java` (new — no existing `HotspotTest` to extend):
- 6-arg constructor `Hotspot(id, type, label, bounds, objectId, true)` → `isSolved()` returns `true`; all other getters (`getId()`, `getType()`, `getLabel()`, `getBounds()`, `getObjectId()`) return the values passed to the constructor unchanged
- 5-arg constructor `Hotspot(id, type, label, bounds, objectId)` → `isSolved()` returns `false`
- All existing `Hotspot` construction sites in test code (`ScenePanelInteractionTest` etc.) use the 5-arg form and must continue to compile and pass without modification

`MainFrameApplyStateNullSafetyTest.java` (new — regression-pinning test; the existing null-guards on lines 73/86 of `MainFrame.java` already handle these cases, so this test passes before any Green code; its purpose is to prevent future regressions, not to drive new production code):
- Construct `MainFrame` with `PlaceholderAssetManager` and a no-op `GameApiClient` stub
- Call `applyState()` with a `GameStateDTO` where `getSolvedPuzzleIds()` returns `null` and `getCurrentRoom()` returns `null` — must complete without throwing `NullPointerException`
- Call `applyState()` with a DTO where `getSolvedPuzzleIds()` returns `null` but `getCurrentRoom()` is a valid `RoomDTO` with objects — must complete without NPE

`MainFrameBuildHotspotsSolvedTest.java` (new — the primary Red driver for UI-M2; this test **fails** today because `buildHotspots()` does not accept `solvedIds` and `ScenePanel` has no `getHotspots()` accessor):
- Construct `MainFrame`, call `applyState()` with a `GameStateDTO` containing:
  - `currentRoom` with one `RoomObjectDTO` of type `PUZZLE`, `puzzleId = "puzzle_clock"`, `id = "wall_clock"`
  - `solvedPuzzleIds = ["puzzle_clock"]`
- Retrieve the hotspot list from `scenePanel.getHotspots()` — the hotspot for `wall_clock` must have `isSolved() == true`
- Repeat with `solvedPuzzleIds = []` — same hotspot must have `isSolved() == false`
- Repeat with `solvedPuzzleIds = null` — same hotspot must have `isSolved() == false` (null treated as empty, no NPE)

**Green.**

`Hotspot.java` — add field and constructor:
```java
private final boolean solved;

// New 6-arg constructor (canonical)
public Hotspot(String id, String type, String label, Rectangle bounds, String objectId, boolean solved)

// Existing 5-arg delegates — backward compat
public Hotspot(String id, String type, String label, Rectangle bounds, String objectId) {
    this(id, type, label, bounds, objectId, false);
}

public boolean isSolved() { return solved; }
```

`ScenePanel.java` — one addition only (no visual changes yet):
- Add a package-private accessor required by `MainFrameBuildHotspotsSolvedTest`:
  ```java
  List<Hotspot> getHotspots() { return hotspots; }
  ```
  This exposes the existing private `hotspots` field for test assertion. No other `ScenePanel` change in this milestone.

`MainFrame.java` — four targeted changes only:
1. `applyState()`: compute null-safe solved set once, before both the `currentRoom` block and the `solvedPuzzleIds` block:
   ```java
   Set<String> solvedIds = state.getSolvedPuzzleIds() != null
       ? new HashSet<>(state.getSolvedPuzzleIds())
       : Collections.emptySet();
   ```
   Reuse `solvedIds` in the existing `if (state.getSolvedPuzzleIds() != null)` guard — replace that guard with `if (!solvedIds.isEmpty())` to avoid double null-check. The existing `setSolvedCount()` call in that block is preserved.
2. `buildHotspots()` signature: `private List<Hotspot> buildHotspots(RoomDTO room, Set<String> solvedIds)`
3. Inside `buildHotspots()`, for PUZZLE-type objects:
   ```java
   boolean solved = "PUZZLE".equals(type) && solvedIds.contains(obj.getPuzzleId());
   hotspots.add(new Hotspot(obj.getId(), type, obj.getLabel(), bounds, obj.getId(), solved));
   ```
   **⚠ Important:** the solved lookup uses `obj.getPuzzleId()` (e.g. `"puzzle_clock"`), NOT `obj.getId()` (e.g. `"wall_clock"`). The surrounding code uses `getId()` everywhere for the hotspot's own id; the solved check is the deliberate exception. See `design_ui_upgrade.md §4.4` — the design is explicit on this point.
   EXIT and non-PUZZLE objects continue to use the 5-arg constructor (solved defaults to false).
4. Update the `buildHotspots()` call sites inside `applyState()` to pass `solvedIds`: `scenePanel.setHotspots(buildHotspots(state.getCurrentRoom(), solvedIds))`

No visual change. No ThemeConstants references. No other method touched.

**Refactor.** None — the changes are already minimal.

**Acceptance.**

- Automated: 3 new test classes pass (all assertions in `HotspotSolvedFieldTest`, `MainFrameApplyStateNullSafetyTest`, `MainFrameBuildHotspotsSolvedTest`). All prior tests pass. `mvn --offline clean test` exits 0.
- Manual demo: launch both backend and frontend. Solve the wall clock puzzle (`11:47`). Confirm the app does not crash (NPE guard works). Visual change not yet expected — the hotspot still renders in yellow (old `ScenePanel` code). That is correct for this milestone.

---

### UI-M3 — Scene: `ScenePanel` visual rework

**Goal.** `ScenePanel.paintComponent()` renders styled hotspots (rounded brass-bordered overlay, hover highlight, solved dimming), mouse hover tracking (hand cursor, `hoveredHotspotId`), room crossfade animation (same-room guard + timer lifecycle), and uses `Graphics2D.create()`/`dispose()` discipline. `FileAssetManager` fallback is swapped to `ProceduralAssetManager` so rooms render atmospheric backgrounds.

**North-star tie.** This is the highest-impact visual change in the entire sprint. After this milestone the game looks like a Gothic library game, not a colored-rectangle prototype. Enables visual verification of solved-state and hover feedback — both core to the "player always knows what is clickable" goal.

**Pre-conditions.** UI-M2 complete (so `Hotspot.isSolved()` exists and solved data flows correctly into the hotspot list).

**Red — write these tests first.**

`ScenePanelHoverTest.java` (new — the `findHotspotAt` assertions duplicate existing `ScenePanelInteractionTest`; that duplication is intentional as a regression pin; the **genuinely Red** assertions are the hover-state ones that require the new `hoveredHotspotId` field and `getHoveredHotspotId()` accessor):
- Construct `ScenePanel` with `PlaceholderAssetManager`; set a list of two hotspots with known non-overlapping bounds (existing behavior, already passes)
- Call `findHotspotAt(x, y)` inside hotspot 1's bounds — confirm returned hotspot id (existing behavior)
- Call `findHotspotAt` in empty space — confirm null (existing behavior)
- **(Red)** Call `updateHover(x, y)` with coordinates inside hotspot 1's bounds — assert `getHoveredHotspotId()` equals hotspot 1's id
- **(Red)** Call `updateHover(x, y)` in empty space — assert `getHoveredHotspotId()` is null

`ScenePanelFadeGuardTest.java` (new — tests the new `setCurrentRoomId` logic; all four assertions are Red because the current code has no fade timer or same-room guard):
- Construct `ScenePanel`; initial `currentRoomId` is `null` (see Green below); call `setCurrentRoomId("room_foyer")` — `isFadeRunning()` returns `false` (first-load rule: no fade from null initial state)
- Call `setCurrentRoomId("room_reading_hall")` (genuine room change) — `isFadeRunning()` returns `true` immediately after
- Call `setCurrentRoomId("room_reading_hall")` again (same room) — timer state unchanged; same-room guard fires, no restart
- Call `setCurrentRoomId("room_archives")` while fade still running — `isFadeRunning()` is still `true` (timer restarted cleanly; no double-timer stack)

**Green.**

`ScenePanel.java` — full rework of `paintComponent()` and new supporting fields/methods per `design_ui_upgrade.md §4.2` and `§4.3`:

**Field changes:**
- Change `private String currentRoomId = "foyer"` to `private String currentRoomId = null` — this is the first-load guard; `setCurrentRoomId()` fires the fade only when transitioning between non-null values
- Add `private String hoveredHotspotId = null`
- Add `private float fadeAlpha = 1.0f`
- Add `final javax.swing.Timer fadeTimer` (package-private, not `private` — required by `ScenePanelFadeGuardTest`)

**New package-private accessors (required by tests):**
```java
String getHoveredHotspotId() { return hoveredHotspotId; }
boolean isFadeRunning() { return fadeTimer.isRunning(); }
```

**Constructor additions:**
- Init `fadeTimer` at 16ms interval; tick body: `fadeAlpha = Math.min(1.0f, fadeAlpha + 0.08f); if (fadeAlpha >= 1.0f) fadeTimer.stop(); repaint()`
- Add `MouseMotionListener` via `addMouseMotionListener(new MouseMotionAdapter() { mouseMoved → updateHover(e.getX(), e.getY()) })`

**`setCurrentRoomId(String newId)` — replace existing implementation:**
```java
public void setCurrentRoomId(String newId) {
    if (newId.equals(this.currentRoomId)) return;  // same-room guard (also handles null==null on first call — but newId is never null)
    this.currentRoomId = newId;
    if (currentRoomId == null) { repaint(); return; }  // first load: assign but don't fade
    // at this point, previous value was null (first real room) OR was a different room ID
    if (this.currentRoomId != null && fadeTimer != null) {
        if (fadeTimer.isRunning()) fadeTimer.stop();
        fadeAlpha = 0.0f;
        fadeTimer.start();
    }
    repaint();
}
```
**First-load rule:** when `currentRoomId` transitions from `null` → first room ID, the equals-guard fires on the `null.equals()` call — which would NPE. Use a null-safe equals: `if (newId.equals(this.currentRoomId))` will NPE when `currentRoomId` is null. Replace with `java.util.Objects.equals(newId, this.currentRoomId)` to be safe, then after setting: `if (this.currentRoomId != null && previousId != null)` to decide whether to start the fade. Concretely:
```java
public void setCurrentRoomId(String newId) {
    if (Objects.equals(newId, this.currentRoomId)) return;
    String previousId = this.currentRoomId;
    this.currentRoomId = newId;
    if (previousId != null) {          // skip fade on first load (null → first room)
        if (fadeTimer.isRunning()) fadeTimer.stop();
        fadeAlpha = 0.0f;
        fadeTimer.start();
    }
    repaint();
}
```

**`updateHover(int x, int y)` (package-private):**
- Call `findHotspotAt(x, y)`; update `hoveredHotspotId` to the found hotspot's id or null; set cursor to `HAND_CURSOR` if non-null, `DEFAULT_CURSOR` otherwise; call `repaint()`

**`paintComponent(Graphics g)` — full rewrite using `Graphics2D g2 = (Graphics2D) g.create()`:**
1. Super call
2. Apply `AlphaComposite.SRC_OVER, fadeAlpha` → draw background image scaled to panel size
3. Reset to `AlphaComposite.SRC_OVER, 1.0f`
4. For each hotspot:
   - Determine fill color by type and solved state (see design §4.2 overlay mapping, all colors from `ThemeConstants`)
   - Determine stroke: `AGED_BRASS` 1.5px default; if `hotspot.getId().equals(hoveredHotspotId)`, use `BRASS_GLOW` 2.5px
   - Draw `RoundRectangle2D.Float(bounds, arc=8)` fill with 80-alpha overlay, then stroke
   - Draw label centered in bounds, `FONT_LABEL`, `CANDLE_TEXT`; append ` ✓` if `hotspot.isSolved()`
5. `g2.dispose()`

`FileAssetManager.java` — two-line change: (1) change the fallback **field type** declaration from `PlaceholderAssetManager` to `AssetManager`; (2) change the field init from `new PlaceholderAssetManager()` to `new ProceduralAssetManager()`. Both lines must change or the code will not compile.

**Refactor.** If the per-hotspot paint block exceeds 15 lines, extract `private void paintHotspot(Graphics2D g2, Hotspot h)`. Keep `paintComponent` as a clear top-level sequence.

**Acceptance.**

- Automated: `ScenePanelHoverTest` + `ScenePanelFadeGuardTest` pass. All 102 prior tests pass. `mvn --offline clean test` exits 0.
- Manual demo (requires backend running):
  - Launch game, start new game: foyer background is procedural art (dark with amber glow + pillar shapes), not a solid blue rectangle
  - Hotspots render as rounded brass-bordered overlays, not plain yellow rectangles
  - Move mouse over "Wall Clock" hotspot — cursor changes to hand, hotspot fill brightens
  - Move off — cursor reverts, brightness reverts
  - Solve wall clock (`11:47`) — that hotspot re-renders with `SOLVED_OVERLAY` (green tint) and ` ✓` in label
  - Click "→ Reading Hall" — ~200ms alpha fade-in plays as the scene transitions
  - Click examine on any object while already in the reading hall — no fade (same-room guard works)
  - Navigate to Archives — different structural silhouette (cabinets, cipher wheel circle) visible in background

---

### UI-M4 — Panels: `StatusBar`, `DialoguePanel`, `InventoryPanel`

**Goal.** All three non-dialog peripheral panels apply the dark Gothic theme. `StatusBar` gains `PuzzleDotsPanel` for progress display. `DialoguePanel` gains parchment textarea and component-scoped scrollbar. `InventoryPanel` gains a custom cell renderer with icon cache and "use mode" indicator. `MainFrame` is updated to pass `assetManager` to `InventoryPanel`.

**North-star tie.** After this milestone, every surface the player sees (except puzzle dialogs) is fully themed. The "readable at a glance" goal (dots, use-mode indicator) is now met.

**Pre-conditions.** UI-M3 complete.

**Red — write these tests first.**

`StatusBarDotsTest.java` (new — all assertions are Red because `PuzzleDotsPanel` and its accessors don't exist yet):
- Construct `StatusBar`; call `setSolvedCount(3, 6)` — access the dots panel via `statusBar.getDotsPanel()` and assert `dotsPanel.getSolved() == 3` and `dotsPanel.getTotal() == 6`
- Call `setSolvedCount(0, 6)` — assert `getSolved() == 0`, `getTotal() == 6`
- Verify button getters: `getNewButton()`, `getSaveButton()`, `getLoadButton()` all return non-null `JButton` instances after the rework
- All three existing `StatusBar*` tests (`StatusBarSaveButtonTest`, `StatusBarLoadFlowTest`, `StatusBarNewGameTest`) must continue to pass — run them; do not modify them

`DialoguePanelThemeTest.java` (new):
- Construct `DialoguePanel` on the EDT via `SwingUtilities.invokeAndWait`
- Assert `textArea.getBackground().equals(ThemeConstants.PARCHMENT)`
- Assert `textArea.getForeground().equals(ThemeConstants.PARCHMENT_TEXT)`
- Assert `textArea.getFont().equals(ThemeConstants.FONT_BODY)`
- Existing `DialoguePanelTest` (if present) must still pass

`InventoryIconCacheTest.java` (new):
- Implement a counting stub `AssetManager` that tracks how many times `getItemIcon()` is called per key
- Construct `InventoryPanel(countingStub)`
- Set 4 items: two with `assetKey="item_key"`, two with `assetKey="item_lens"`
- Trigger cell rendering for all 4 items (call `getCellRenderer().getListCellRendererComponent()` for each)
- Assert `getItemIcon("item_key")` was called exactly once; `getItemIcon("item_lens")` was called exactly once (cache hit on second call for each key)

`InventoryUseModeTest.java` (new):
- Construct `InventoryPanel`, set two items, select the first item programmatically (`itemList.setSelectedIndex(0)`)
- Trigger the panel title update (may require calling a package-private method or firing a `ListSelectionListener`)
- Assert panel title text contains `"USING:"`
- Call `clearSelection()` — assert panel title reverts to `"INVENTORY"`

**Green.**

`StatusBar.java` — rework:
- Background `ThemeConstants.NIGHT_BLACK`; layout `BorderLayout`
- Left: `roomLabel` with `FONT_TITLE`, `CANDLE_TEXT`
- Center: `PuzzleDotsPanel` inner class extending `JPanel` — package-private fields `int solved`, `int total`; `void setSolvedCount(int s, int t)` stores both values and calls `repaint()`; `paintComponent()` loops `for (int i = 0; i < total; i++)`, fills oval in `BRASS_GLOW` when `i < solved`, draws oval stroke in `DIM_TEXT` otherwise. **Required package-private accessors for tests:** `int getSolved()` and `int getTotal()`.
- Right: button panel; `ThemeConstants.applyDarkButton()` on all three buttons
- Getters `getNewButton()`, `getSaveButton()`, `getLoadButton()` return same `JButton` field instances (no rename)
- `setSolvedCount(int solved, int total)` on `StatusBar` delegates to `PuzzleDotsPanel.setSolvedCount()`; `setRoomName()` updated accordingly; `setSaveEnabled()` unchanged
- **Required accessor for tests:** `PuzzleDotsPanel getDotsPanel()` on `StatusBar` — returns the inner panel instance so tests can read `getSolved()`/`getTotal()`

`DialoguePanel.java` — rework:
- Outer panel background `DARK_WOOD`; `CompoundBorder(EmptyBorder(4,4,4,4), LineBorder(AGED_BRASS,1))`
- `textArea`: background `PARCHMENT`, foreground `PARCHMENT_TEXT`, font `FONT_BODY`, caret color `PARCHMENT_TEXT`
- `ThornwickScrollBarUI` — private static inner class extending `BasicScrollBarUI`; overrides `paintTrack()` (fill `DARK_WOOD`) and `paintThumb()` (fill `AGED_BRASS`); applied via `scrollBar.setUI(new ThornwickScrollBarUI())` on the scroll pane's vertical and horizontal scroll bars — component-scoped, not global

`InventoryPanel.java` — rework:
- Constructor updated to `InventoryPanel(AssetManager assetManager)`
- Field `private final Map<String, Icon> iconCache = new HashMap<>()`
- `ItemCellRenderer` inner class: `extends JPanel implements ListCellRenderer<InventoryItemDTO>`; `BorderLayout`; WEST `JLabel` with cached icon; CENTER `JLabel` with item name `FONT_INVENTORY`, `CANDLE_TEXT`; padding `EmptyBorder(4,6,4,6)`; selected state: `LineBorder(BRASS_GLOW, 2)`; cell height `setPreferredSize(new Dimension(0, 52))`
- Icon loaded lazily: `iconCache.computeIfAbsent(key, k -> new ImageIcon(assetManager.getItemIcon(k).getScaledInstance(40, 40, Image.SCALE_SMOOTH)))`
- Use-mode: `ListSelectionListener` on `itemList` — on selection change, update panel title text; `clearSelection()` also triggers title revert
- Border updated from `TitledBorder` to a styled `CompoundBorder` with custom title rendering (or a titled border with `CANDLE_TEXT` color and `AGED_BRASS` line)

`MainFrame.java` — one additional change: on the existing line that constructs `InventoryPanel` (currently `new InventoryPanel()`), pass the constructor parameter `assetManager`:
```java
inventoryPanel = new InventoryPanel(assetManager);
```
Note: `assetManager` is a **constructor parameter** of `MainFrame(AssetManager assetManager, ...)`, not a stored field. Both `InventoryPanel` and `ScenePanel` are constructed inside that constructor where the parameter is in scope, so the change compiles without adding a field.

**Refactor.** If `InventoryPanel`'s `setItems()` call clears the model and the selection listener fires spuriously (firing "deselected" on every refresh), add a guard flag `private boolean updatingItems` toggled around the `model.clear()` / `model.addElement()` calls so the use-mode indicator only responds to genuine user selection changes.

**Acceptance.**

- Automated: `StatusBarDotsTest` + `DialoguePanelThemeTest` + `InventoryIconCacheTest` + `InventoryUseModeTest` pass. All 102 prior tests pass (especially the three `StatusBar*` tests). `mvn --offline clean test` exits 0.
- Manual demo (requires backend running):
  - Status bar: dark background, room name in serif, three brass-bordered buttons
  - Start new game: 6 hollow circles (○○○○○○) in center of status bar
  - Solve wall clock: first circle turns gold (●○○○○○)
  - Solve display case: ●●○○○○
  - Pick up magnifying glass: inventory cell shows 40×40 lens icon alongside item name; icon is NOT a solid oval
  - Select magnifying glass: panel title changes to "USING: Brass Magnifying Glass"
  - Deselect: title reverts to "INVENTORY"
  - Dialogue panel: parchment-colored text area, dark outer border, serif font
  - Scrollbar in dialogue panel is brass-colored, not default grey

---

### UI-M5 — Puzzle Dialogs + Final Acceptance Gate

**Goal.** All three puzzle dialog subclasses apply the dark Gothic theme with parchment-colored input fields. The existing dialog input tests continue to pass unchanged (regression gate for the `applyThemeRecursively` exclusion rule). Full `design_ui_upgrade.md §9` acceptance checklist is walked and all boxes ticked.

**North-star tie.** Completes the visual overhaul. After this milestone, the upgrade is fully shipped on the `ui-improvement` branch.

**Pre-conditions.** UI-M4 complete.

**Red — write these tests first.**

`PuzzleDialogBaseThemeTest.java` (new):
- Construct each of the three dialog subclasses on the EDT
- For each: assert `getContentPane().getBackground().equals(ThemeConstants.DARK_WOOD)`

`RiddleDialogFieldThemeTest.java` (new):
- Construct `RiddlePuzzleDialog`; locate the `JTextField` (`getAnswerField()`)
- Assert `answerField.getBackground().equals(ThemeConstants.PARCHMENT)`
- Assert `answerField.getForeground().equals(ThemeConstants.PARCHMENT_TEXT)`
- Assert existing `RiddlePuzzleDialogInputTest` still passes unchanged (call `answerField.setText("11:47")`, assert `getInputs()` = `{"answer":"11:47"}`)

`CombinationDialogFieldThemeTest.java` (new):
- Construct `CombinationPuzzleDialog(owner, "puzzle_display_case", 3, "Test")`; access `getSpinners()`
- For each `JSpinner`: assert `getBackground().equals(ThemeConstants.PARCHMENT)`
- Assert existing `CombinationPuzzleDialogInputTest` still passes unchanged

`SequenceDialogFieldThemeTest.java` (new):
- Construct `SequencePuzzleDialog`; locate the `JList` via the **new** `getList()` accessor (see Green — `getListModel()` returns only the `DefaultListModel`, not the `JList`)
- Assert `getList().getBackground().equals(ThemeConstants.DARK_WOOD)`
- Assert `getList().getSelectionBackground().equals(ThemeConstants.BRASS_GLOW)`
- Assert existing `SequencePuzzleDialogInputTest` still passes unchanged

**Green.**

`PuzzleDialog.java` — update `initLayout(JPanel inputPanel)`:
1. `getContentPane().setBackground(ThemeConstants.DARK_WOOD)`
2. `inputPanel.setBackground(ThemeConstants.DARK_WOOD)`
3. Call `applyThemeRecursively(inputPanel)`
4. Style OK and Cancel buttons inline, right after the existing lines that create them inside `initLayout`. The buttons are local variables in `initLayout` — **do not promote them to fields**. Apply styling in place:
   ```java
   JButton okButton = new JButton("OK");
   ThemeConstants.applyDarkButton(okButton);
   JButton cancelButton = new JButton("Cancel");
   ThemeConstants.applyDarkButton(cancelButton);
   ```
   (The exact variable names may differ in the existing code — apply `applyDarkButton()` immediately after each button is created.)

`applyThemeRecursively(Component c)` — private helper:
```java
private void applyThemeRecursively(Component c) {
    // Skip input widgets — styled explicitly by subclasses
    if (c instanceof JTextField || c instanceof JFormattedTextField
            || c instanceof JSpinner || c instanceof JButton || c instanceof JList) {
        return;
    }
    c.setBackground(ThemeConstants.DARK_WOOD);
    c.setForeground(ThemeConstants.CANDLE_TEXT);
    if (c instanceof Container) {
        for (Component child : ((Container) c).getComponents()) {
            applyThemeRecursively(child);
        }
    }
}
```

`RiddlePuzzleDialog.java` — before calling `initLayout(inputPanel)`:
```java
answerField.setBackground(ThemeConstants.PARCHMENT);
answerField.setForeground(ThemeConstants.PARCHMENT_TEXT);
answerField.setBorder(BorderFactory.createLineBorder(ThemeConstants.AGED_BRASS, 1));
```
Question `JLabel` — no explicit change needed; `applyThemeRecursively` applies `CANDLE_TEXT`.

`CombinationPuzzleDialog.java` — immediately after creating each spinner, before adding to panel:
```java
spinner.setBackground(ThemeConstants.PARCHMENT);
spinner.setForeground(ThemeConstants.PARCHMENT_TEXT);
((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setBackground(ThemeConstants.PARCHMENT);
((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setForeground(ThemeConstants.PARCHMENT_TEXT);
```

`SequencePuzzleDialog.java` — two changes:
1. Promote `list` from a local variable to a field (`private JList<String> list`). This is required both for the theme styling below and for the new `getList()` accessor.
2. After creating the `JList`, before adding to panel:
```java
list.setBackground(ThemeConstants.DARK_WOOD);
list.setForeground(ThemeConstants.CANDLE_TEXT);
list.setSelectionBackground(ThemeConstants.BRASS_GLOW);
list.setSelectionForeground(ThemeConstants.NIGHT_BLACK);
```
3. Add package-private accessor: `JList<String> getList() { return list; }`
4. Move-up/down buttons: call `ThemeConstants.applyDarkButton(moveUp)` and `ThemeConstants.applyDarkButton(moveDown)` immediately after creating them.

**Refactor.** Walk all changed files for any remaining hardcoded hex values — replace with `ThemeConstants` references. Look for any `new Color(...)` literals outside `ThemeConstants.java` and `ProceduralAssetManager.java` (where structural art colors are acceptable as local values if they are not part of the named palette).

**Final Acceptance Gate — walk `design_ui_upgrade.md §9` fully.**

- [ ] All 102 existing tests pass + all new tests pass: `mvn --offline clean test` exits 0, zero failures
- [ ] `StatusBarNewGameTest`, `StatusBarSaveButtonTest`, `StatusBarLoadFlowTest` pass (null-safety + button getter regression)
- [ ] `WinScreenFiresTest` passes
- [ ] `StatusBar` button getters return same instances (listener wiring intact)
- [ ] Dark Gothic theme applied consistently (manual: every surface is dark wood, brass, or parchment)
- [ ] Parchment input fields stay parchment after `initLayout()` (manual: open riddle dialog, text field is light parchment color, not dark)
- [ ] All puzzle dialogs: dark background + correct field theming (manual: open each dialog type)
- [ ] Hotspot hover: cursor changes, fill brightens (manual: move mouse over hotspots)
- [ ] Hotspot solved state: green overlay + ` ✓` on solved puzzles (manual: solve two puzzles, navigate back to their rooms)
- [ ] Room crossfade: ~200ms fade on room change (manual: navigate rooms, observe smooth transition)
- [ ] Crossfade does NOT fire on same-room action (manual: examine objects without leaving room — no flash)
- [ ] Inventory icons: unique shape per item, not uniform oval (manual: acquire all 5 items, inspect each cell)
- [ ] Icon cache: no stutter on inventory repaint (manual: move mouse rapidly over inventory while items loaded)
- [ ] "USING: [name]" title on item select; reverts on deselect (manual)
- [ ] Puzzle progress as ●●○○○○ dots, not "2/6" text (manual)
- [ ] `PuzzleDotsPanel` renders correct dot count (manual: 6 dots always present, correct filled count after each solve)
- [ ] `ProceduralAssetManager`: visually distinct backgrounds per room (manual: rooms clearly look different)
- [ ] PNG override: place any 800×500 PNG at `frontend/src/main/resources/art/room_foyer.png`, restart frontend — that room shows the PNG instead of procedural art; remove the file, restart — procedural art returns
- [ ] No new Maven dependencies: `git diff main -- '*/pom.xml'` is empty
- [ ] No backend changes: `git diff main -- backend/` is empty

**Manual demo — full golden path with upgraded UI:**

Play through the complete `idea.md §3` golden path from click 1 to the win screen. At each step, confirm:
1. New Game → foyer renders with Gothic art, 6 hollow dots, dark panels
2. Solve wall clock (`11:47`) → first dot turns gold, clock hotspot shows ` ✓`
3. Move to Reading Hall → crossfade plays, 2 exits visible at bottom of scene
4. Solve display case (`384`) → second dot turns gold; magnifying glass appears in inventory with icon
5. Select magnifying glass → title shows "USING: Brass Magnifying Glass"
6. Solve book cart (`510, 520, 610, 621, 720, 810`) → third dot gold, scrap in inventory
7. Move to Archives → procedural art shows cabinets and cipher wheel silhouette
8. Solve cipher wheel (mag glass + wheel) → fourth dot gold; "THORNWICK" revealed in dialogue
9. Solve iron chest (`THORNWICK`) → fifth dot gold; manuscript in inventory
10. Select manuscript, click terminal → sixth dot gold, all dots filled, win dialog fires

---

### UI-M6 — Room Art: Counting Clues (Option A)

**Goal.** `ProceduralAssetManager` draws exactly 8 bookshelf silhouettes in the Reading Hall (up from 5) and 3 small brass nail circles in the Foyer doorframe. Archives already have 4 cabinet silhouettes — no change needed. These changes embed combination-puzzle digit clues visually in the room art without stating the answer.

**North-star tie.** A player exploring rooms before opening the combination dialog can count objects in the art to discover the three digits (3, 8, 4). This is the parallel visual discovery path described in `design_ui_upgrade.md §10.1`. Neither hint path (visual or text) is sufficient alone — the player still needs to connect three separate rooms to the three-dial combination.

**Pre-conditions.** UI-M5 complete. All 150 tests passing.

**Red — write these tests first.**

`ProceduralAssetManagerCountingCluesTest.java` (new):
- Cast `getBackground("room_foyer")` to `BufferedImage`; assert pixel at (207, 140) is NOT `ThemeConstants.NIGHT_BLACK.getRGB()` — confirms a nail circle exists at that point (the circle is `AGED_BRASS`; the baseline background at that coordinate is dark gradient)
- Same assertion for nail coordinates (192, 145) and (222, 148) — all three nail centers must be non-NIGHT_BLACK
- Cast `getBackground("room_reading_hall")` to `BufferedImage`; assert pixel at (696, 200) is NOT `ThemeConstants.NIGHT_BLACK.getRGB()` — this coordinate falls inside bookshelf 8's area; with 5 bookshelves it was empty background (gradient only)
- Existing `ProceduralAssetManagerTest` (dimensions, distinctness assertions) must still pass — regression pin

All four new assertions are Red today: the foyer has no nail circles (those coordinates are currently dark gradient), and the reading hall 8th shelf slot is currently unoccupied.

**Green.**

`ProceduralAssetManager.java` — two targeted changes inside the reading hall and foyer silhouette drawing blocks:

1. **Reading Hall — 8 bookshelves** (replaces the existing 5-shelf loop):
```java
int shelfW = 72;
int gap = (800 - 8 * shelfW) / 9;   // ≈ 24px
for (int i = 0; i < 8; i++) {
    int sx = gap + i * (shelfW + gap);
    g2.fillRect(sx, 50, shelfW, 360);
}
```

2. **Foyer — 3 brass nail heads** (added after existing doorframe and reception desk drawing):
```java
g2.setColor(ThemeConstants.AGED_BRASS);
int[] nailX = {192, 207, 222};
int[] nailY = {145, 140, 148};
for (int i = 0; i < 3; i++) {
    g2.fillOval(nailX[i] - 2, nailY[i] - 2, 5, 5);
}
```

No other changes to `ProceduralAssetManager`. No other files touched in this milestone.

**Refactor.** None — the changes are already minimal and self-contained.

**Acceptance.**

- Automated: `ProceduralAssetManagerCountingCluesTest` (4 assertions) passes. All 150 prior tests pass. `mvn --offline clean test` exits 0.
- Manual demo: launch frontend, start new game.
  - Navigate to Reading Hall: count the bookshelf silhouettes — there are exactly 8.
  - Navigate to Entry Foyer: look at the arched doorframe area — 3 small brass dots are visible near the arch top, subtly placed but individually countable.
  - Navigate to Archives: 4 cabinet silhouettes unchanged; filing cabinet drawer faces unchanged.

---

### UI-M7 — Examination Hint Cards (Option B)

**Goal.** Four object interactions display a centered `BufferedImage` overlay ("hint card") on `ScenePanel` that auto-fades after ~2 seconds. `AssetManager` gains a backward-compatible `getHintCard()` default method. `ProceduralAssetManager` renders four distinct cards. `ScenePanel` gains hint card overlay infrastructure (image, alpha, hold-then-fade timer). `MainFrame` wires all four triggers: `wall_clock` first-click shows the clock-face card and delays the riddle dialog by 1.5s; SCENERY objects (`reception_desk`, `reading_lamp`, `filing_cabinets`) show their card alongside the server's dialogue text on every examine.

**North-star tie.** Completes the visual hint system. After this milestone a player who examines objects sees a close-up procedural drawing that confirms they have found something countable (or, for the clock, shows the HH:MM format). The challenge is preserved: the clock card never shows the time; the counting cards have no label.

**Pre-conditions.** UI-M6 complete.

**Red — write these tests first.**

`ScenePanelHintCardTest.java` (new):
- Construct `ScenePanel` with `PlaceholderAssetManager`; call `showHintCard(new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB))` — assert `isHintCardVisible()` returns `true` immediately
- Assert `getHintCardAlpha()` returns `1.0f` immediately after `showHintCard`
- Assert `hintCardTimer.isRunning()` returns `true` immediately after `showHintCard`
- Call `showHintCard` twice in a row — second call resets state cleanly; `hintCardTimer.isRunning()` is `true` (timer stopped-and-restarted, not doubled)

`ProceduralAssetManagerHintCardTest.java` (new):
- `getHintCard("wall_clock")` returns non-null, casts to `BufferedImage`, width 320, height 200, type `TYPE_INT_ARGB`
- Same assertions for `"reception_desk"`, `"reading_lamp"`, `"filing_cabinets"` (4 cards total)
- Clock card and nails card are not pixel-identical: center pixel (160, 100) differs between the two
- `getHintCard("unknown_object")` returns non-null 320×200 ARGB image — graceful fallback, no exception

`MainFrameHintCardPreRenderTest.java` (new):
- Construct `MainFrame` with `ProceduralAssetManager` and a no-op `GameApiClient` stub
- Access `getHintCardImages()` (package-private accessor) — assert map contains exactly 4 keys: `"wall_clock"`, `"reception_desk"`, `"reading_lamp"`, `"filing_cabinets"`
- Access `getShownHintCards()` (package-private accessor) — assert set is empty at construction

**Green.**

`AssetManager.java` — add one `default` method. All existing implementations require zero changes:
```java
default java.awt.Image getHintCard(String objectId) {
    return new java.awt.image.BufferedImage(1, 1,
            java.awt.image.BufferedImage.TYPE_INT_ARGB);
}
```

`ProceduralAssetManager.java` — add `@Override public java.awt.Image getHintCard(String objectId)`:
- `switch(objectId)` with cases `"wall_clock"`, `"reception_desk"`, `"reading_lamp"`, `"filing_cabinets"` per card specs in `design_ui_upgrade.md §10.3`
- Default: return blank 320×200 `TYPE_INT_ARGB` image
- All colors via `ThemeConstants.*`; `g2.create()/dispose()`; antialiasing on

`FileAssetManager.java` — add one override that delegates:
```java
@Override
public java.awt.Image getHintCard(String objectId) {
    return fallback.getHintCard(objectId);
}
```

`ScenePanel.java` — add hint card overlay per `design_ui_upgrade.md §10.7`:
- Fields: `private BufferedImage hintCardImage = null`, `private float hintCardAlpha = 0.0f`, `private int hintCardTicks = 0`, `final Timer hintCardTimer` (package-private)
- `hintCardTimer` (16ms): tick body increments `hintCardTicks`; once `> 125` (≈2s hold), decrements α by 0.05f per tick; at α ≤ 0 nulls image and stops timer; uses `((Timer) e.getSource()).stop()` pattern (no self-reference)
- `public void showHintCard(BufferedImage image)`: assigns image, sets α=1.0f, resets ticks=0, stops-then-starts timer
- `paintComponent()`: after hotspot loop, before `g2.dispose()`, draw centered 320×200 card if visible
- Package-private accessors: `float getHintCardAlpha()`, `boolean isHintCardVisible()`

`MainFrame.java` — add trigger logic per `design_ui_upgrade.md §10.8`:
- Fields: `private final Map<String, BufferedImage> hintCardImages` (built in constructor from `assetManager.getHintCard()` for all 4 IDs); `private final Set<String> shownHintCards = new HashSet<>()`
- Package-private accessors for tests: `Map<String, BufferedImage> getHintCardImages()`, `Set<String> getShownHintCards()`
- PUZZLE branch in `handleHotspotClick`: if `!shownHintCards.contains(oid) && hintCardImages.containsKey(oid)` → add to `shownHintCards`, call `scenePanel.showHintCard(...)`, start one-shot `Timer(1500, …)` with `setRepeats(false)` that calls `handlePuzzleClick(capturedObj)`, return
- After `applyState(client.examine(…))`: if `hintCardImages.containsKey(hotspot.getObjectId())` → call `scenePanel.showHintCard(...)`
- `shownHintCards.clear()` in `handleNewGame()` and `handleLoad()` success paths

**Refactor.** If any hint card drawing case exceeds 20 lines, extract a private helper (`paintWallClockCard(Graphics2D)` etc.) to keep `getHintCard`'s `switch` readable. All helpers remain private to `ProceduralAssetManager`.

**Acceptance.**

- Automated: `ScenePanelHintCardTest` (4 assertions) + `ProceduralAssetManagerHintCardTest` (6 assertions) + `MainFrameHintCardPreRenderTest` (3 assertions) pass. All prior tests pass. `mvn --offline clean test` exits 0.
- Manual demo (backend running):
  - Examine `reception_desk`: server dialogue text appears in dialogue panel AND a centered card overlay appears in the scene with 3 brass circles in a doorframe; card auto-fades after ~2s
  - Examine `reading_lamp`: card shows 8 book-spine rectangles side-by-side; count them
  - Examine `filing_cabinets`: card shows 4 distinct drawer-row sections with brass handles
  - Click `wall_clock` (first time in session): clock face card appears showing Roman numeral ticks and two blurred non-directional hand smears; after ~1.5s the riddle dialog opens automatically; no readable specific time on the card
  - Click `wall_clock` (second time, same session): no card — riddle dialog opens immediately
  - Solve the wall clock, start a New Game, click `wall_clock` again → card reappears (shownHintCards cleared on new game)
  - Load a save, click `wall_clock` → card reappears (shownHintCards cleared on load)

**Final hint system acceptance gate** — walk `design_ui_upgrade.md §9 "Visual — hint system"` checklist fully:
- [ ] All 11 hint system acceptance items green
- [ ] `mvn --offline clean test` exits 0 with all tests (150 + ~15 new) passing
- [ ] No backend files changed: `git diff main -- backend/` is empty
- [ ] No `world.json` changes: `git diff main -- backend/src/main/resources/world.json` is empty
- [ ] No new Maven dependencies: `git diff main -- '*/pom.xml'` is empty

---

## 7. Risk Register

| Risk | Mitigation | Milestone |
|------|-----------|-----------|
| `applyThemeRecursively` overwrites parchment fields | Exclusion list in `PuzzleDialog` helper; regression gate: existing dialog input tests must pass unchanged | UI-M5 |
| `javax.swing.Timer` fade stacks if room changes rapidly | Stop-before-restart in `setCurrentRoomId()`; `ScenePanelFadeGuardTest` covers this | UI-M3 |
| First-load fade fires on initial room set | `currentRoomId` initialised to `null`; `setCurrentRoomId` uses `Objects.equals` + `previousId != null` guard; `ScenePanelFadeGuardTest` first assertion covers this | UI-M3 |
| `setCurrentRoomId` NPE on `null.equals(newId)` | `Objects.equals(newId, currentRoomId)` used instead of `newId.equals(currentRoomId)` | UI-M3 |
| Icon cache not hit: renderer called outside cache path | `InventoryIconCacheTest` verifies call count ≤ 1 per key | UI-M4 |
| `PuzzleDotsPanel` getters not exposed for test | `getSolved()`, `getTotal()`, `getDotsPanel()` are package-private and specified in Green | UI-M4 |
| `SequencePuzzleDialog.getList()` doesn't exist | `list` promoted from local to field; `getList()` accessor added in Green | UI-M5 |
| `ScenePanel.getHotspots()` doesn't exist | Package-private `getHotspots()` added to `ScenePanel` Green in UI-M2 | UI-M2 |
| `MainFrame.assetManager` field claimed but absent | `assetManager` is a constructor parameter, not a field; `InventoryPanel` construction is inside the constructor where parameter is in scope | UI-M4 |
| `FileAssetManager` fallback field type mismatch | Both type declaration (`AssetManager`) and init (`new ProceduralAssetManager()`) changed in UI-M3 Green | UI-M3 |
| `InventoryPanel` constructor change breaks callers | Only `MainFrame` constructs `InventoryPanel`; no test builds it directly (confirmed by review) | UI-M4 |
| `PuzzleDotsPanel` not repainting on `setSolvedCount` | Test asserts `repaint()` contract via stored field; manual demo visually confirms | UI-M4 |
| Null `solvedPuzzleIds` NPE on new-game response | `MainFrameApplyStateNullSafetyTest` covers this; null-guard in `applyState()` | UI-M2 |
| `ProceduralAssetManager` structural coordinates misaligned with hotspot positions (M3) | Known and accepted: backgrounds are 800×500 and stretched; hotspots are dynamically laid out; silhouettes are decorative, not precise. No acceptance criterion requires pixel alignment. | UI-M3 |
| Nail circle test coordinate is inside gradient glow, not NIGHT_BLACK, making the "not NIGHT_BLACK" assertion vacuously true | Pick coordinates in the nail circle center (≈(207,140)) rather than the outer doorframe; verify the pixel is specifically `AGED_BRASS` rather than just "not NIGHT_BLACK" if the assertion proves flaky | UI-M6 |
| Bookshelf 8 test coordinate is inside the ambient gradient (also not NIGHT_BLACK) | Use a coordinate in the mid-height of shelf 8 (y≈200) where the gradient is near-black; assert the pixel is darker than `#2A1A08` (gradient max) — bookshelf fill `#1A0F04` is noticeably darker | UI-M6 |
| `AssetManager.getHintCard()` default method breaks test stubs that explicitly implement every method | Default method provides a fallback automatically — stubs compile without changes. Verified by running all existing tests in Green step before committing. | UI-M7 |
| Hint card timer and crossfade timer fire simultaneously (both timers running during room change + hint card show) | Independent timers; each controls its own alpha field and repaint. `paintComponent` draws them in distinct layers — crossfade affects background alpha; card is drawn after hotspots at full opacity. No shared state. | UI-M7 |
| `wall_clock` one-shot delay timer fires after the modal dialog has already been dismissed by another action | One-shot timer is scoped to a single click; `capturedObj` is effectively final; `handlePuzzleClick` is idempotent — calling it after the dialog was already shown just opens another dialog. Acceptable: player pressed the hotspot intentionally. | UI-M7 |
| `hintCardImages` map build fails if `assetManager.getHintCard()` returns non-`BufferedImage` | `instanceof BufferedImage bi` pattern match silently skips non-BufferedImage returns; `PlaceholderAssetManager` default returns a `BufferedImage`; `ProceduralAssetManager` always returns `BufferedImage`. No runtime failure path. | UI-M7 |

---

## 8. File Change Summary

| File | Changed in | Change type |
|------|-----------|-------------|
| `ThemeConstants.java` | UI-M1 | **NEW** |
| `ProceduralAssetManager.java` | UI-M1 | **NEW** |
| `FileAssetManager.java` | UI-M3 | Minor: fallback swap |
| `Hotspot.java` | UI-M2 | Minor: `solved` field + 6-arg constructor |
| `MainFrame.java` | UI-M2 + UI-M4 | `buildHotspots()` signature, `applyState()` null-guard, `InventoryPanel` constructor |
| `ScenePanel.java` | UI-M2 + UI-M3 | UI-M2: `getHotspots()` accessor; UI-M3: full `paintComponent()` rework + hover + fade + `getHoveredHotspotId()` + `isFadeRunning()` + `currentRoomId` init change |
| `StatusBar.java` | UI-M4 | Dark theme + `PuzzleDotsPanel` inner class |
| `DialoguePanel.java` | UI-M4 | Parchment textarea + `ThornwickScrollBarUI` inner class |
| `InventoryPanel.java` | UI-M4 | Dark theme + `ItemCellRenderer` + icon cache |
| `PuzzleDialog.java` | UI-M5 | `initLayout()` + `applyThemeRecursively()` |
| `CombinationPuzzleDialog.java` | UI-M5 | Parchment spinner styling |
| `RiddlePuzzleDialog.java` | UI-M5 | Parchment text field styling |
| `SequencePuzzleDialog.java` | UI-M5 | Promote `list` to field; add `getList()` accessor; dark list styling + styled buttons |
| `AssetManager.java` | UI-M7 | Minor: add `default getHintCard(String)` method (backward-compatible) |
| `ProceduralAssetManager.java` | UI-M6 + UI-M7 | UI-M6: 8 bookshelves + 3 nail circles; UI-M7: `getHintCard()` for 4 object IDs |
| `FileAssetManager.java` | UI-M7 | Minor: override `getHintCard()` to delegate to fallback |
| `ScenePanel.java` | UI-M7 | Hint card overlay fields + `showHintCard()` + `hintCardTimer` + `paintComponent()` card layer |
| `MainFrame.java` | UI-M7 | `hintCardImages` map + `shownHintCards` set + PUZZLE first-click delay + SCENERY examine trigger + clear on new-game/load |

Zero backend files changed. Zero `pom.xml` files changed. Zero `world.json` changes.

---

## 9. How to use this document during build

1. **At the start of each milestone**, re-read its section. Confirm pre-conditions.
2. **Write the Red tests first.** Do not write production code until you see at least one test fail.
3. **Green minimally**, then **Refactor**.
4. **Run the acceptance gate**: `mvn --offline clean test` from repo root must exit 0.
5. **Run the manual demo** documented for that milestone.
6. **Commit** with the milestone tag: `feat(UI-M1): ThemeConstants + ProceduralAssetManager`.
7. After UI-M7, merge `ui-improvement` into `main`.

---

**End of plan_ui_upgrade.md.** UI-M1–M5 complete. UI-M6–M7 awaiting plan review approval before build begins.
