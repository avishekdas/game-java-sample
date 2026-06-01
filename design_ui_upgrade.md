# UI Upgrade Design — Thornwick Visual Overhaul

**Branch:** `ui-improvement`  
**Status:** Architecture-reviewed — fixes applied  
**Scope:** Swing frontend only. Zero backend changes. Zero AP CS rubric removals.

---

## 1. North Star Alignment

The north star for this project is a **pedagogically complete AP CS final project** that also feels like a real game. Every design decision in this document is tested against two questions:

1. **Does it preserve or strengthen AP CS rubric coverage?** (Classes & Objects, Inheritance, ArrayLists, Loops, Conditionals, File I/O, GUI)
2. **Does it stay within AP CS-level Java concepts?** (No concurrency frameworks, no reactive streams, no dependency injection beyond what's already present)

The visual overhaul advances the second dimension — "feels like a real game" — without touching the first. `idea.md §11` explicitly lists art assets and UI polish as Phase 2 work. This document executes that backlog item.

**Guardrails that cannot be crossed:**
- `javax.swing.*` remains the GUI framework — this IS the rubric demonstration
- No new Maven dependencies (all improvements use Java standard library only: `java.awt.*`, `javax.swing.*`, `javax.imageio.*`, `javax.sound.sampled.*`)
- `FileAssetManager` and `PlaceholderAssetManager` remain the art abstraction — no art logic enters game-logic classes
- All 102 existing tests must still pass after this work

---

## 2. Design Goals

| Goal | Measure |
|------|---------|
| Atmospheric immersion | Dark Gothic library theme applied to every surface |
| Visual feedback | Player always knows what is clickable, hovered, solved, or in "use mode" |
| Art-ready architecture | Room backgrounds + item icons load from PNG files; absent files fall to styled placeholders |
| Readable at a glance | Puzzle progress, current room, and selected inventory item all visible without reading labels |
| AP CS showcase | More custom `paintComponent()` code = stronger GUI rubric evidence |

---

## 3. Visual Direction

### 3.1 Theme

Gothic Victorian library at midnight. Warm amber candlelight fighting cold stone. Aged brass fittings. Parchment and ink. The player is trapped — the atmosphere should feel both beautiful and slightly threatening.

### 3.2 Color Palette

All colors defined as named constants in `ThemeConstants.java` — no hardcoded hex values anywhere else.

```
Name              Hex        Usage
─────────────────────────────────────────────────────────────────
NIGHT_BLACK       #0D0B07    Deepest background (window, scene panel default)
DARK_WOOD         #1A1408    Panel fills (inventory, status bar, dialogue frame)
AGED_BRASS        #8B6914    Primary accent — borders, hotspot stroke, scroll bar
BRASS_GLOW        #C8A84B    Hover / active state highlight
PARCHMENT         #F0DFA0    Dialogue text area background
PARCHMENT_TEXT    #2A1F0A    Text rendered on parchment
CANDLE_TEXT       #E8D5A3    Text on dark backgrounds
DIM_TEXT          #706040    Secondary / disabled labels
SOLVED_OVERLAY    #1A3A1A    Green-tint overlay on solved PUZZLE hotspots (50% alpha)
EXIT_OVERLAY      #0A1A2A    Blue-tint overlay on EXIT hotspots (40% alpha)
SELECTION_GLOW    #C8A84B    Inventory selection border
ERROR_TEXT        #C84040    Wrong-answer flash in dialogue
```

### 3.3 Typography

All fonts defined as constants in `ThemeConstants.java`.

```
Name              Value                               Usage
──────────────────────────────────────────────────────────────────────────
FONT_TITLE        Georgia, BOLD, 15                  Room name in status bar
FONT_BODY         Georgia, PLAIN, 13                 Dialogue text area
FONT_LABEL        SansSerif, BOLD, 11                Hotspot labels (readability over atmosphere)
FONT_BUTTON       Georgia, BOLD, 12                  All buttons
FONT_INVENTORY    SansSerif, PLAIN, 12               Inventory item names
FONT_SMALL        SansSerif, PLAIN, 10               Secondary labels, puzzle counter
```

Georgia is a system font available on macOS, Windows, and most Linux installs. No font file bundling needed.

### 3.4 Window Layout

Overall window size increases from ~1020×740 to ~1100×780 to accommodate richer panels. Layout structure (BorderLayout) is unchanged.

```
┌──────────────────────────────────────────────────────────────────────┐
│  StatusBar  [NIGHT_BLACK bg]                                   NORTH  │
│  ◀ The Archives        ● ● ● ○ ○ ○      [Save] [Load] [New Game]     │
├────────────────────────────────────────────────┬─────────────────────┤
│                                                │                     │
│   ScenePanel  [room background PNG]    CENTER  │  InventoryPanel     │
│                                                │  [DARK_WOOD bg]     │
│   Hotspot buttons float over scene             │                     │
│   Exit arrows at bottom edge                   │  ┌───────────────┐  │
│                                                │  │ [icon] item   │  │
│                                                │  │ [icon] item ← │← selected (brass glow)
│                                                │  └───────────────┘  │
│                                                │  EAST  200px        │
├────────────────────────────────────────────────┴─────────────────────┤
│  DialoguePanel  [DARK_WOOD frame + PARCHMENT textarea]         SOUTH  │
│  "A compartment pops open beneath the reception desk..."              │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Specifications

### 4.1 `ThemeConstants.java` (NEW)

A new utility class — no instance, all `static final` fields. Referenced by every UI component. This is the single place to adjust the palette or fonts.

```
frontend/src/main/java/com/abhishri/escape/ui/ThemeConstants.java
```

Contents: Color constants (§3.2) + Font constants (§3.3) + a utility method `applyDarkButton(JButton)` that applies the standard dark-background / brass-border / Georgia-font styling so all buttons are styled from one call.

### 4.2 `ScenePanel.java` (REWORK)

**`paintComponent()` changes:**
1. Draw room background PNG (existing behavior via `assetManager.getBackground(currentRoomId)`)
2. For each `Hotspot`, paint a styled rectangle:
   - Base fill: semi-transparent (`alpha = 80`) tinted overlay based on type:
     - `PUZZLE` / `ITEM` / `SCENERY`: `AGED_BRASS` tint
     - `EXIT`: `EXIT_OVERLAY`
     - `PUZZLE` + solved: `SOLVED_OVERLAY` (dimmed — player has already interacted)
   - Stroke: `AGED_BRASS`, 1.5px. On hover: `BRASS_GLOW`, 2.5px
   - Corner arc: `RoundRectangle2D`, arc 8
   - Label: `FONT_LABEL`, `CANDLE_TEXT`, centered in hotspot bounds. Solved hotspots append ` ✓`
3. On hover over any hotspot: change cursor to `Cursor.HAND_CURSOR`. Off all hotspots: `Cursor.DEFAULT_CURSOR`

**New fields:**
- `private String hoveredHotspotId` — updated by `MouseMotionListener`

**No `solvedPuzzleIds` field on `ScenePanel`.** Solved state is carried on each `Hotspot` (§4.4). `paintComponent()` calls `hotspot.isSolved()` directly. `MainFrame.buildHotspots()` is the single place that computes solved state and stamps it onto the `Hotspot` objects before passing the list to `ScenePanel.setHotspots()`. This keeps `ScenePanel` purely a renderer with no game-state knowledge.

**`MouseMotionListener`** added in constructor:
- `mouseMoved(e)` → finds hotspot under cursor, updates `hoveredHotspotId`, updates cursor, calls `repaint()`

**`Graphics2D` discipline:** `paintComponent()` must open with `Graphics2D g2 = (Graphics2D) g.create()` and close with `g2.dispose()`. All painting (background, gradients, `AlphaComposite`, hotspot shapes, labels) is done through `g2`, never through the original `g`. This prevents composite and stroke state from leaking into subsequent Swing painting passes.

No threading. No `SwingWorker`. All Swing EDT.

### 4.3 Room Crossfade Animation

**Same-room guard (required):** `setCurrentRoomId()` must compare against the existing `currentRoomId` and do nothing if the value is unchanged. Without this guard, every puzzle solve / examine / pickup in the same room re-triggers the fade, which makes the scene flash on every action.

```
setCurrentRoomId(String newRoomId):
  if newRoomId.equals(this.currentRoomId) → return immediately
  this.currentRoomId = newRoomId
  if fadeTimer.isRunning() → fadeTimer.stop()   // discard any in-flight fade
  fadeAlpha = 0.0f
  fadeTimer.start()
```

**Fade mechanics:**
- `private float fadeAlpha = 1.0f` — starts fully opaque
- `private final javax.swing.Timer fadeTimer` — created once in constructor at 16ms interval (never recreated)
- Each timer tick: `fadeAlpha = Math.min(1.0f, fadeAlpha + 0.08f)`. When `fadeAlpha >= 1.0f`, stop timer
- In `paintComponent()` (within the `g2.create()` scope per §4.2): apply `AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha)` before drawing background image; reset composite to `AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)` before drawing hotspots — hotspots always paint at full opacity regardless of fade state

Duration: ~200ms (≈12 ticks × 16ms). `javax.swing.Timer` fires on EDT — no threading concern. One timer field, reused across all room transitions.

### 4.4 `Hotspot.java` (MINOR CHANGE)

Add `private final boolean solved` field. New constructor overload:

```
Hotspot(String id, String type, String label, Rectangle bounds, String objectId, boolean solved)
```

Existing 5-arg constructor delegates to new one with `solved = false` — backward compatible for all existing tests.

`ScenePanel.paintComponent()` calls `hotspot.isSolved()` to choose overlay color. This is the only solved-state check; `ScenePanel` holds no solved-puzzle set of its own.

`MainFrame.buildHotspots()` sets `solved = true` for PUZZLE-type hotspots using the explicit match:
```
solved = "PUZZLE".equals(type) && solvedIds.contains(obj.getPuzzleId())
```
where `solvedIds` is a null-safe `Set<String>` derived from `state.getSolvedPuzzleIds()` (see §4.9). `obj.getPuzzleId()` is the puzzle id (e.g. `"puzzle_clock"`); `solvedIds` contains the same ids from `GameStateDTO.solvedPuzzleIds`. The hotspot's own `id`/`objectId` (e.g. `"wall_clock"`) is never used for the solved lookup — only `obj.getPuzzleId()` is.

### 4.5 `InventoryPanel.java` (REWORK)

**Background:** `DARK_WOOD`. Border: `LineBorder(AGED_BRASS, 1)` with a titled label "INVENTORY" in `CANDLE_TEXT`.

**Icon cache (required):** `InventoryPanel` holds `private final Map<String, Icon> iconCache = new HashMap<>()`. The renderer must never call `assetManager.getItemIcon()` directly on every cell paint — `ProceduralAssetManager.getItemIcon()` performs full Java 2D rendering on each call, which would fire dozens of times per second on hover/scroll/repaint. Cache is populated lazily:
```
iconCache.computeIfAbsent(item.getAssetKey(), key ->
    new ImageIcon(assetManager.getItemIcon(key)
        .getScaledInstance(40, 40, Image.SCALE_SMOOTH)))
```
`assetManager.getItemIcon()` returns `java.awt.Image` (64×64). It is scaled to 40×40 via `getScaledInstance` and wrapped in `ImageIcon` for use in a `JLabel`. This scaling and wrapping happens once per asset key, not per render pass.

**Custom `ListCellRenderer`** — inner class `ItemCellRenderer extends JPanel implements ListCellRenderer<InventoryItemDTO>`:
- Layout: `BorderLayout`
- WEST: `JLabel` with the cached `ImageIcon` (40×40)
- CENTER: `JLabel` with item name in `FONT_INVENTORY`, `CANDLE_TEXT`
- Padding: `EmptyBorder(4, 6, 4, 6)`
- Selected state: background `DARK_WOOD` + `LineBorder(BRASS_GLOW, 2)`; unselected: background `DARK_WOOD`, no extra border
- Cell height: 52px

**"Use mode" indicator:** When an item is selected in inventory, the panel title changes to "USING: [item name]" in `BRASS_GLOW` color. When selection cleared, reverts to "INVENTORY".

`InventoryPanel` constructor updated to accept `AssetManager`. `MainFrame` passes `assetManager` when constructing `InventoryPanel`. No-arg constructor removed (no test constructs `InventoryPanel` directly — confirmed by review).

### 4.6 `StatusBar.java` (REWORK)

Background: `NIGHT_BLACK`. Layout: `BorderLayout`.

**Left section:** `JLabel` for room name. Font: `FONT_TITLE`. Color: `CANDLE_TEXT`. Prefix: "◀ " (indicates current location).

**Center section:** Puzzle progress rendered as a custom `JPanel` that overrides `paintComponent()` — draws filled dots (●) for solved puzzles and hollow dots (○) for unsolved. Dot color: `BRASS_GLOW` for solved, `DIM_TEXT` for unsolved. Dot size: 10px, spacing: 16px.

`PuzzleDotsPanel` stores `private int solved` and `private int total` as fields. `setSolvedCount(int solved, int total)` updates both fields and calls `repaint()`. The dot-drawing loop must iterate over `total` (not just over `solved`) and branch per index:
```
for (int i = 0; i < total; i++) {
    if (i < solved) { fillOval in BRASS_GLOW }
    else            { drawOval in DIM_TEXT   }
}
```
This is the Loops + Conditionals rubric demonstration. `total` comes from `GameStateDTO.getTotalPuzzles()` (already used in `MainFrame` line 87); a different puzzle count does not require a code change.

**Right section:** `JPanel` with buttons. `applyDarkButton()` from `ThemeConstants` applied to all three buttons. The getter methods `getNewButton()`, `getSaveButton()`, `getLoadButton()` return the same `JButton` instances as before — no rename or replacement — so all existing `StatusBar*` tests that wire listeners via those getters continue to work.

### 4.7 `DialoguePanel.java` (REWORK)

**Outer panel:** `DARK_WOOD` background. `CompoundBorder`: outer `EmptyBorder(4,4,4,4)` + inner `LineBorder(AGED_BRASS, 1)`.

**`JTextArea`:** Background: `PARCHMENT`. Foreground: `PARCHMENT_TEXT`. Font: `FONT_BODY`. Line wrap, word wrap on. Caret color: `PARCHMENT_TEXT`.

**`JScrollPane`:** Background: `DARK_WOOD`. ScrollBar styled with `AGED_BRASS` track by subclassing `BasicScrollBarUI` and calling `scrollBar.setUI(new ThornwickScrollBarUI())` — this is component-scoped, not global. `UIManager.put()` must NOT be used here: it is a global JVM-wide change, not scoped to a single component. `ThornwickScrollBarUI` is a private static inner class of `DialoguePanel` that overrides `paintTrack` and `paintThumb` to use `DARK_WOOD` and `AGED_BRASS` respectively.

Welcome text remains but is set in `PARCHMENT_TEXT` on `PARCHMENT` background, giving an aged journal feel.

### 4.8 `PuzzleDialog.java` base class (REWORK)

`PuzzleDialog extends JDialog` is the base for all three puzzle dialogs. Styling applied here cascades to all subclasses.

**Dialog background:** `DARK_WOOD`.

**`initLayout(JPanel inputPanel)` updated to:**
1. Set dialog background to `DARK_WOOD`
2. Style `inputPanel` background to `DARK_WOOD`
3. Apply dark theme to containers and labels via `applyThemeRecursively(Component c)`
4. Style OK and Cancel buttons via `ThemeConstants.applyDarkButton()`

**`applyThemeRecursively(Component c)` — exclusion rules (critical):**
This helper applies `DARK_WOOD` background and `CANDLE_TEXT` foreground to `JPanel` and `JLabel` instances only. It **must skip** the following types entirely — do not touch their background, foreground, font, or model:
- `JTextField` and `JFormattedTextField`
- `JSpinner` and `JSpinner.DefaultEditor`
- `JButton`
- `JList`

These components are styled explicitly by the subclass constructors (see below). The recursive helper touching them would overwrite subclass parchment styling.

**Subclass styling order — all subclasses follow this pattern:**
1. Create input widgets (text field, spinners, list, etc.)
2. Immediately style them with explicit color/font calls (parchment or dark, as specified below)
3. Add to `inputPanel`
4. Call `initLayout(inputPanel)` — recursive helper runs but skips the already-styled input types

This ordering means parchment styling is applied by the subclass before `initLayout()` runs, and `applyThemeRecursively` skips those types, leaving the colors untouched.

**Subclass-specific styling (applied in step 2 above):**
- `RiddlePuzzleDialog`: question text `JLabel` → no change (picked up by recursive pass). Answer `JTextField` → `setBackground(PARCHMENT)`, `setForeground(PARCHMENT_TEXT)`, `setBorder(LineBorder(AGED_BRASS, 1))`
- `CombinationPuzzleDialog`: each `JSpinner` → `setBackground(PARCHMENT)`, `setForeground(PARCHMENT_TEXT)`; spinner's editor text field → same (access via `((JSpinner.DefaultEditor) s.getEditor()).getTextField()`). Digit `JLabel` prefixes → picked up by recursive pass
- `SequencePuzzleDialog`: `JList` → `setBackground(DARK_WOOD)`, `setForeground(CANDLE_TEXT)`, `setSelectionBackground(BRASS_GLOW)`, `setSelectionForeground(NIGHT_BLACK)`. Move-up/down `JButton`s → `applyDarkButton()` called directly by subclass

### 4.9 `MainFrame.java` (MINOR CHANGES ONLY)

1. Pass `assetManager` to `InventoryPanel` constructor (§4.5)
2. **Null-safe solved set** — computed once in `applyState()` and passed into `buildHotspots()`:
   ```
   Set<String> solvedIds = state.getSolvedPuzzleIds() != null
       ? new HashSet<>(state.getSolvedPuzzleIds())
       : Collections.emptySet();
   ```
   `state.getSolvedPuzzleIds()` is null on new-game responses (confirmed by `StatusBarNewGameTest`). Defaulting to empty set prevents NPE. **No call to `scenePanel.setSolvedPuzzleIds()`** — that method does not exist. The set flows into `buildHotspots(solvedIds)`.
3. `buildHotspots(Set<String> solvedIds)` — signature updated to accept the solved set. PUZZLE hotspots stamped `solved = solvedIds.contains(obj.getPuzzleId())` as specified in §4.4. All other hotspot construction logic unchanged.
4. Window preferred size set to `1100×780`

No other logic changes. All dispatch logic (EXIT / ITEM / use-item / PUZZLE / examine) unchanged.

---

## 5. Art Assets — `ProceduralAssetManager` (CONFIRMED: Track B)

**Confirmed approach:** All room backgrounds and item icons are rendered at runtime using `ProceduralAssetManager`, a new class built during the coding sprint. No external PNG files are required. The game ships complete and fully styled without any image files.

`FileAssetManager` retains its existing PNG-first logic — if a file exists at `/art/{roomId}.png` or `/art/{assetKey}.png`, it is used instead. This means real PNG art can be dropped in at any time in the future as a zero-code-change upgrade (see §5.4 for optional upgrade path). For this sprint, procedural art is the deliverable.

### 5.1 `ProceduralAssetManager.java` (NEW — primary art renderer)

```
frontend/src/main/java/com/abhishri/escape/ui/ProceduralAssetManager.java
```

Implements `AssetManager`. Wired into `FileAssetManager` as its fallback, replacing `PlaceholderAssetManager`. `PlaceholderAssetManager` stays in the codebase unchanged (used by existing tests).

#### `getBackground(String roomId)` — rendering algorithm

Produces a `BufferedImage(800, 500, TYPE_INT_RGB)` per room. All color references are `ThemeConstants` constants.

**Layer 1 — base fill:** Solid `NIGHT_BLACK` fill.

**Layer 2 — ambient candlelight gradient:** Two overlapping `RadialGradientPaint` passes using `Graphics2D.setPaint()`:
- Primary glow: center (400, 380), radius 280px. Colors: `#5A3A10` (warm amber, fraction 0.0) → `#2A1A08` (fraction 0.5) → `NIGHT_BLACK` (fraction 1.0). Simulates floor candle or lamp.
- Secondary glow: center (160, 160), radius 160px. Colors: `#3A2808` (fraction 0.0) → `NIGHT_BLACK` (fraction 1.0). Softer ceiling scatter.

**Layer 3 — structural silhouettes:** Filled shapes in `#1A0F04` (darkest brown, slightly lighter than `NIGHT_BLACK` so shapes read as depth, not black holes). Room-specific:

| Room | Shapes drawn |
|------|-------------|
| `room_foyer` | Two stone pillar rects (x=60,y=80 and x=660,y=80, w=80, h=420); arched doorframe connecting them (filled arc at top, `Arc2D`); rectangular door panel inset (x=180,y=150, w=440,h=350) in slightly lighter brown; wall clock silhouette (filled oval + two thin rect hands) at (230,100); reception desk rect (x=80,y=340, w=200,h=80) |
| `room_reading_hall` | Five bookshelf column rects evenly spaced across width (w=100,h=360, y=50); fireplace opening (arch rect, x=300,y=280, w=180,h=200) in very dark grey; reading lamp pole (thin rect) + cone of light (`GradientPaint` from amber to transparent, triangle shape) |
| `room_archives` | Four filing cabinet rects (w=90,h=260, y=120) with horizontal line dividers (drawer faces); cipher wheel circle outline on right wall (x=660,y=180, diameter=120, `Ellipse2D` stroke only in `AGED_BRASS`); iron chest rect (x=320,y=320, w=150,h=100) with domed top arc |

**Layer 4 — vignette:** Two `GradientPaint` overlay rectangles (using `AlphaComposite.SRC_OVER`):
- Top: `NIGHT_BLACK` at alpha 200 → transparent, height 120px
- Bottom: `NIGHT_BLACK` at alpha 160 → transparent, height 80px from bottom

**Layer 5 — room label:** Room name string in bottom-left (x=12, y=488) in `DIM_TEXT`, `FONT_SMALL`.

#### `getItemIcon(String assetKey)` — rendering algorithm

Produces a `BufferedImage(64, 64, TYPE_INT_ARGB)` per item (transparent background).

**Base chip:** Filled `RoundRectangle2D` (w=58,h=58, arc=8, centered, i.e. `new RoundRectangle2D.Float(3,3,58,58,8,8)`) in `DARK_WOOD`. Stroke `AGED_BRASS` 1px.

**Symbol per asset key** (all shapes in `AGED_BRASS` unless noted):

| Asset key | Shapes |
|-----------|--------|
| `item_key` | Filled oval head (x=8,y=20, w=20,h=20); filled rect shaft (x=26,y=28, w=26,h=8); two small rect teeth at shaft end (x=44,y=36,w=6,h=6 and x=38,y=36,w=5,h=5) |
| `item_lens` | Circle stroke (x=8,y=8, w=32,h=32) 3px; filled circle interior `DARK_WOOD` 80% alpha; diagonal line handle from (38,38) to (56,56) 4px stroke |
| `item_scrap` | `PARCHMENT`-filled rect (x=12,y=10,w=38,h=44); torn-top polygon overlay in `DARK_WOOD` (ragged clip at y=10–16); three thin horizontal lines in `PARCHMENT_TEXT` (ruled lines) |
| `item_manuscript` | `PARCHMENT`-filled rect (x=10,y=14,w=42,h=40); torn-top polygon in `DARK_WOOD` (jagged edge, y=14–20); four thin ruled lines in `PARCHMENT_TEXT`; small wax-seal filled circle (x=34,y=42,w=12,h=12) in `#8B1A1A` (dark red) |
| `item_token` | Filled outer circle (x=10,y=10,w=44,h=44) `AGED_BRASS`; filled inner circle (x=18,y=18,w=28,h=28) `DARK_WOOD`; star/seal path or concentric ring in `BRASS_GLOW` |

**Draw order for all icons (back-to-front):**
1. Enable antialiasing: `g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)`
2. Fill base chip (`RoundRectangle2D`, `DARK_WOOD`)
3. Stroke chip border (`AGED_BRASS`, 1px)
4. Draw symbol shapes specific to the asset key (see table above)

For `item_lens`: fill interior circle (`DARK_WOOD` at 80% alpha via `AlphaComposite`) before stroking the circle border, then draw handle line — ensures handle does not appear behind the interior fill.

### 5.2 `FileAssetManager.java` (MINOR CHANGE)

Change the `fallback` field from `new PlaceholderAssetManager()` to `new ProceduralAssetManager()`. One line change.

### 5.3 PNG Upgrade Path (optional, future)

PNG files can be added to `frontend/src/main/resources/art/` at any time to override procedural art per room or item. No code changes required. Useful if Abhishri later generates or sources real artwork.

**File naming:** `room_foyer.png`, `room_reading_hall.png`, `room_archives.png` (800×500); `item_key.png`, `item_lens.png`, `item_scrap.png`, `item_manuscript.png`, `item_token.png` (64×64).

If AI-generated art is desired later, the following prompts produce consistent results:

- **Foyer:** `Interior of a Victorian gothic library entrance hall at midnight, marble floor, locked double doors, stopped wall clock, candlelight glow, no people, painted game art style, dark atmospheric, 16:10`
- **Reading Hall:** `Victorian gothic library reading room at night, towering bookshelves, glass display case, stone fireplace, reading lamp, no people, painted game art style, dark atmospheric, 16:10`
- **Archives:** `Gothic library archive vault at night, steel filing cabinets, brass cipher wheel on wall, iron chest, pneumatic tube terminal, no people, painted game art style, dark atmospheric, 16:10`
- **Icons (all):** `[item description], Victorian antique style, fantasy RPG inventory icon, transparent background, 64x64px, warm brass tones, detailed illustration`

This is significantly better than the current solid-color oval. It is also a clean AP CS GUI demonstration: a `switch` over `assetKey` (Conditionals), shape drawing primitives (custom graphics), and implements the `AssetManager` interface (Inheritance/polymorphism).

---

## 6. Files Changed vs. Not Changed

### Changed (frontend only)

| File | Change Type |
|------|-------------|
| `ThemeConstants.java` | **NEW** — color + font constants + `applyDarkButton()` |
| `ProceduralAssetManager.java` | **NEW** — Java 2D fallback renderer; replaces `PlaceholderAssetManager` as `FileAssetManager`'s fallback |
| `ScenePanel.java` | Rework `paintComponent()`, add hover tracking, fade animation, `setSolvedPuzzleIds()` |
| `Hotspot.java` | Add `solved` field + new 6-arg constructor (5-arg delegates, backward compat) |
| `InventoryPanel.java` | Accept `AssetManager`, custom cell renderer, dark theme |
| `StatusBar.java` | Dark theme, `PuzzleDotsPanel` inner class, brass button styling |
| `DialoguePanel.java` | Parchment textarea, dark outer frame, serif font |
| `PuzzleDialog.java` | Dark background, `applyThemeRecursively()` helper |
| `CombinationPuzzleDialog.java` | Inherit dialog theme, styled spinners |
| `RiddlePuzzleDialog.java` | Inherit dialog theme, styled text field |
| `SequencePuzzleDialog.java` | Inherit dialog theme, styled list + buttons |
| `MainFrame.java` | Pass `assetManager` to `InventoryPanel`; null-safe `solvedIds` set; `buildHotspots(solvedIds)` signature; window size |
| `FileAssetManager.java` | One-line change: fallback field `new PlaceholderAssetManager()` → `new ProceduralAssetManager()` |

### NOT Changed

| File | Reason |
|------|--------|
| All `backend/` source | Backend is complete; zero backend changes this phase |
| `GameApiClient.java` | HTTP client, no UI concern |
| `AssetManager.java` | Interface is correct and sufficient |
| `PlaceholderAssetManager.java` | Fallback still needed by tests; left as-is |
| `EscapeRoomApp.java` | Entry point, no change |
| All `dto/` classes | Data contracts, no change |
| All test files | Must pass unchanged (except `HotspotTest` if it needs new constructor) |

---

## 7. AP CS Rubric Preservation Check

| Rubric Item | Current Demonstration | After Upgrade |
|-------------|----------------------|---------------|
| Classes & Objects | All UI classes, domain entities | Adds `ThemeConstants`, `PuzzleDotsPanel`, `ItemCellRenderer` — more classes |
| Inheritance | `Puzzle` hierarchy; `PuzzleDialog` → subclasses | Unchanged. `ItemCellRenderer extends JPanel` adds one more example |
| ArrayLists | Inventory, solved puzzle IDs, hotspot list | Unchanged |
| Loops | Hotspot rendering loop, world seeding | Dot-drawing loop in `PuzzleDotsPanel` adds a clean new demo |
| Conditionals | Puzzle evaluation, game-state branching | Hover/solved state branching in `ScenePanel.paintComponent()` adds more |
| File I/O | H2 file DB, JSON saves, PNG loading via `ImageIO.read()` | PNG art loading via `ImageIO` adds a visible File I/O demo |
| GUI | All Swing components | Richer `paintComponent()` override, custom renderer, Swing Timer — strengthens this item |

**Net result: rubric coverage is strengthened, not diluted.**

---

## 8. Out of Scope (defer to later)

- Sound / ambient audio (was in Phase 2 backlog — keep there; no `javax.sound.sampled` code this sprint)
- `SwingWorker` async HTTP calls (stays Phase 2 per `idea.md §11` and CLAUDE.md)
- Drag-and-drop sequence dialog upgrade (stays Phase 2)
- Win screen animation beyond existing `JOptionPane`
- Any new gameplay mechanics or puzzle changes

---

## 9. Acceptance Criteria

**Functional / regression**
- [ ] All 102 existing tests pass with no modifications to test logic
- [ ] `StatusBarNewGameTest`, `StatusBarSaveButtonTest`, `StatusBarLoadFlowTest` pass — these construct `MainFrame` and call `applyState()` with DTOs that have `null` `solvedPuzzleIds` and `null` `currentRoom`; must not NPE
- [ ] `WinScreenFiresTest` passes — `MainFrame.showWinDialog()` still fires on `COMPLETE` status
- [ ] `StatusBar` getter methods `getNewButton()`, `getSaveButton()`, `getLoadButton()` return the same `JButton` instances (no rename); listener wiring in existing tests must work unchanged

**Visual — theme**
- [ ] Dark Gothic theme (§3.2 palette, §3.3 fonts) applied consistently across StatusBar, ScenePanel, InventoryPanel, DialoguePanel, all three puzzle dialogs
- [ ] Parchment input fields remain parchment (`PARCHMENT` background, `PARCHMENT_TEXT` foreground) after `initLayout()` / `applyThemeRecursively()` runs — not overwritten to `DARK_WOOD`
- [ ] All puzzle dialogs use dark background + parchment input fields (`JTextField`, spinners, `JList` correctly styled per §4.8)

**Visual — scene and interaction**
- [ ] Hotspot hover: cursor changes to `HAND_CURSOR`, fill brightens to `BRASS_GLOW` on mouse-over
- [ ] Hotspot solved state: PUZZLE hotspots whose `puzzleId` appears in `solvedPuzzleIds` render with `SOLVED_OVERLAY` and ` ✓` label suffix
- [ ] Room crossfade: entering a new room triggers ~200ms alpha fade-in of the background image
- [ ] Crossfade does NOT re-trigger when performing an action (examine, pickup, puzzle solve) in the same room — only triggers on actual room change

**Visual — inventory and status**
- [ ] Inventory icons: unique 40×40 scaled icon per item in each cell (shape varies per item — not uniform colored oval)
- [ ] Icon cache: `assetManager.getItemIcon()` called at most once per asset key across all repaints — not on every cell render
- [ ] Selected inventory item shows "USING: [name]" in panel title; reverts to "INVENTORY" when deselected
- [ ] Puzzle progress rendered as ●●○○○○ dots in status bar, not as "2/6" text
- [ ] `PuzzleDotsPanel` renders exactly `total` dots (hollow or filled), not a hardcoded 6; updates immediately when `setSolvedCount()` is called

**Art**
- [ ] `ProceduralAssetManager` produces visually distinct atmospheric backgrounds for all three rooms (two-pass radial candlelight gradient + room-specific structural silhouettes + vignette — clearly different per room, not solid color)
- [ ] `FileAssetManager` falls through to `ProceduralAssetManager` on PNG miss; a PNG placed in `/art/` overrides procedural art for that room or item

**Constraints**
- [ ] No new Maven dependencies added to any `pom.xml`
- [ ] No backend source files modified
- [ ] `PlaceholderAssetManager` unchanged (tests depend on it)
