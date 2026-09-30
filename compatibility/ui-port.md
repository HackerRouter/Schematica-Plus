# Litematica UI adaptation

Target: Minecraft 1.7.10, Forge 10.13.4.1614, MCP stable_12, Java 8 bytecode.
The UI is internal to Schematica Plus; it does not register a MaLiLib mod or expose
binary compatibility with modern MaLiLib.

## Source investigation (2026-09-30)

| Project | Relevant findings | Decision |
| --- | --- | --- |
| [ModularUI](https://github.com/GTNewHorizons/ModularUI) | Existing 1.7.10 library; README lists GTNHMixins; LGPL-3.0 | Useful precedent, no new dependency |
| [ModularUI2](https://github.com/GTNewHorizons/ModularUI2/tree/40fdbdabab7767999b84fda0dc5e9ff47e7edb0b) | Widget tree, focus and capture, separate screen wrapper; GTNHLib/NEI dependencies; LGPL-3.0 | Adopt the separation of responsibilities, implement locally |
| [OpenModsLib](https://github.com/GTNewHorizons/OpenModsLib/tree/e5e8177f2dd2297d5aea6bb733955a67ffa63c95) | BaseComposite demonstrates small reusable controls and snapshots when callbacks can mutate children | Keep the component layer independent of game/world data |
| [MaLiLib 26.1.2-0.28.8](https://github.com/sakura-ryoko/malilib/tree/d194eb689d60f13ba39df4ab525dca64b1cf4478) | GuiBase, ButtonGeneric, list/search controls provide the target interaction model; modern Screen/GuiGraphicsExtractor/GLFW cannot be used in 1.7.10 | Adapt screen lifecycle, drawing and input at the boundary |

The supplied Litematica 26.1.2-0.27.8 requires MaLiLib >=0.28.8, while the supplied
MaLiLib snapshot was 0.28.2. A separate checkout of tag 26.1.2-0.28.8 was obtained
for this work. The supplied directories were not modified.

ModularUI2's Stencil supports transformed/holographic interfaces. Our axis-aligned
screens need only nested scissors, avoiding a stencil-buffer requirement. Its
render helpers also reinforce the need to restore texture/blend state after draws.
OpenModsLib's broad child event dispatch is not reused: only the topmost hit and
the focused control receive input here, and a modal blocks the underlying layer.

Phase 1 independently implemented the adapter and primitives. Phase 3 below
introduces attributed LGPL UI layouts, translations and the upstream icon atlas.
See THIRD_PARTY_NOTICES.md for the file-level license exceptions.

## Internal contract

- `UiBounds`, `UiInput` and `UiListModel` are game-independent and covered by tests.
- `UiPanel` owns its children. Coordinates are absolute, in vanilla scaled GUI
  pixels, matching MaLiLib's explicit positioning. Every ancestor clips rendering
  and input. Add order is draw order; hit tests run backwards.
- Focus is unique. Tab traverses visible/enabled controls; mouse capture survives
  leaving a control and is cancelled on removal, disable, screen close or modal
  opening. Modal layers restore earlier focus when dismissed.
- `UiDraw` is the rendering boundary. The 1.7.10 implementation owns GL state and
  converts scissors using ScaledResolution's actual scale, including odd window
  dimensions. Controls never use framebuffer mouse coordinates.
- Layout updates existing controls on resize; typing, selection and scroll state
  belong to the controls/models, not the layout pass.
- Lists filter with Locale.ROOT, retain selection by entry, clamp scrolling and
  draw only visible rows. An entry filtered out cannot be activated with Enter.

## Visual direction

The target is the supplied Litematica UI, including positions, text-derived button
widths, icons, grouping and row actions. The initial compact custom layout was a
foundation, not the visual specification. Match upstream scaled GUI coordinates;
do not redistribute the buttons into equal-width rows or substitute text for icons.
The screen overlay is #B0000000, list/info outlines #FF999999 and titles white at
(20, 10). Use the unmodified Litematica atlas. MaLiLib's vanilla button background
is resolved through the running 1.7.10 resource pack, so its skin and the font
rasterization can differ from Minecraft 26.1.2. Keyboard focus retains a light border.

Phase 1 excludes business screens, hotkey chords, subregion data and schematic
backend changes. Phase 2 below builds on this foundation.

## Minecraft adapter and controls

`UiScreen` bridges GuiScreen lifecycle and LWJGL2 input. It owns repeat-key state,
parent navigation, modal confirmation, delayed wrapped tooltips and relayout.
`MinecraftUiDraw` scopes GL attributes/modelview and preserves an existing scissor;
its texture method accepts explicit source/atlas dimensions for later icon sheets.
The initial icons are drawn with primitives and do not require upstream assets.

Controls include left/right-click buttons, a boolean toggle, vanilla-backed text
editing (selection/clipboard), bounded integer input, labels and a searchable,
virtualized list with scrollbar dragging and keyboard navigation. Integer edits
commit on Enter/focus loss; intermediate empty/minus text is allowed. Committed
values clamp to their configured range.

In Controls, bind **UI component test** in the Schematica category, then press
it in a world. The binding defaults to unbound. The page uses 200 synthetic entries
and does not load, save or change schematics/worlds. Child pages exercise return
navigation with state retained in the parent.

## Phase 2 (superseded layout): main menu and file loading

The existing M binding now opens `GuiSchematicMainMenu`. Its binding identifier is
preserved, so customized keys remain assigned. Direct controls and direct loading
have separate unbound bindings; N still opens the save screen. Menu navigation
follows Litematica's compact grouped layout and bottom tool-mode selector, with
only implemented features exposed. Instances, controls, materials and configuration
currently open the existing screens; their UI/data-model ports are later work.

`GuiSchematicBrowser` provides the shared directory toolbar, search, virtual list,
file size/time information, refresh, file-manager shortcut and action/status rows.
`SchematicBrowserModel` handles directory bounds and sorting without Minecraft or
NBT parsing. Opening a folder no longer decodes every schematic for its icon.
Double-click/Enter opens folders or activates files; filtering cannot activate a
hidden selection. Back never loads a file. The footer has an explicit Load action.

The load screen uses the existing three format readers, creates a new instance,
restores saved coordinates/transforms when present and otherwise uses the existing
look-target/player placement behavior. It refreshes rendering and saves the session.
Failures stay on the page with feedback. Success stays on the page to allow another
load; Esc returns to the menu, then to the world. File parsing itself remains
synchronous, as in the previous loader; directory listing no longer parses NBT.

## Phase 2 (superseded layout): area selection and saving

`GuiSchematicSave` now uses the same component layer. It edits the existing A/B
points, supports moving either point to the player, controls guide/NBT/entity
settings and displays selection validation. Coordinate edits commit on Enter or
focus loss. Resizing or returning from the folder picker retains filename and
format choices, and the existing points remain the source of selection state.

`GuiSchematicDirectory` reuses the browser in directory-only mode. Destinations
stay within the configured schematic directory. The output format initially uses
the configuration default, but can be changed for this save without changing that
global preference. Known suffixes in the name field are normalized to the selected
format, avoiding doubled extensions. `.litematic` remains import-only.

`SchematicSaveTarget` validates names/destinations without writing anything. An
existing target prompts for confirmation. The existing save queue captures the
selection and NBT/entity options; no capture or file-format backend was replaced.
UI feedback reports acceptance into that queue, with completion/automatic format
upgrade reported by the existing chat messages. Queue limits and server loading/
saving restrictions still apply.

Headless checks cover directory navigation, deleted files/folders, metadata refresh,
hidden selection, format normalization and invalid/save-target paths. Native UI,
world capture and preview behavior need the phase 2 scenarios in `TESTING.md`.

## Phase 3: source-aligned layouts and instance lists

- Main menu: the upstream x=12, y=30/52/74 and 118/140/162 left groups,
  second column separated by 20 pixels, configuration at y=30 and manager/task
  entries at y=118/140, plus the tool selector at height-26. Widths include every
  upstream menu label and both selection modes. Projects remain hidden as in the
  upstream default. The mod name/version retain the Schematica Plus identity.
- Loaded schematics and placements: 22-pixel rows, expandable search at (14,34),
  upstream right-aligned row actions and bottom navigation. Rows are virtualized;
  rebuilding filtered rows cancels captured actions on removed controls. Placement
  selection, render toggling, configuration and removal use the existing backend.
- Browser/load: icon navigation at y=28, 14-pixel entries, 170-pixel information
  panel, create-placement checkbox at height-40 and upstream action order at
  height-26. Search toggles with the magnifier; F5 refreshes and Backspace navigates
  up unless editing text. Existing readers and bounded directory handling remain.
- Simple area editor: selection/subregion name rows and the two 68-pixel coordinate
  columns follow the upstream simple editor. Coordinate nudges and move-to-player
  use the existing A/B selection. The selection visibility checkbox at (250,48)
  is a Plus addition. Names are screen-local drafts, not persistent named selections.
- Saving: filename at (10,32), four checkbox positions at y=28/40/52/64, save at
  (10,54), browser at (10,80). Existing format/NBT/selection-visibility options are
  in a separate Options page beside Save. File selection fills the name; typing,
  directory navigation and returning from Options retain the draft. Bounds checks,
  overwrite confirmation, queue limits and world identity checks remain unchanged.

This is an incremental UI port. The loaded-source/placement backend is still a
single SchematicWorld per instance; both lists therefore show the same instances,
and removing/unloading either removes that instance. The tooltip states this.
Separate loaded sources, new placements from memory, reload/export from memory,
file renaming, area libraries, multiple boxes/manual origins, area analysis, task
management and the three advanced capture options remain disabled with explanatory
hover text. At the end of phase 3, configuration, placement Configure and materials still
used their existing screens. Phases 4 and 5 below replace placement Configure and
global configuration. The information panel currently reports file attributes, not
schematic metadata, region details or preview images. These are the next UI/data
adapters, not completed parity. No modern MaLiLib runtime dependency is introduced.

Automated verification covers row virtualization, clipping and cancellation when
filtering during a held click, alongside existing input/browser/save-path tests.
The atlas is copied byte-for-byte. Native game appearance, resource-pack behavior,
GUI scaling and interaction with real loaded schematics still need the phase 3
manual checks. Compilation does not establish pixel-for-pixel visual parity.

## Phase 4: placement configuration

`GuiPlacementConfiguration` now opens from a placement row's Configure button.
It follows the supplied Litematica screen's rename row, sub-region list at (10,62),
120-pixel right column, coordinate fields/nudges/checkboxes, rotation/mirror controls
and the height-328 breakpoint for the material/verifier/navigation buttons.
The atlas is unchanged; this phase adds the existing enclosing-box icon mapping.
All edits target the instance used to open the screen. Configuring an inactive
instance and viewing its materials do not switch the printer/tool's active instance.

Available: display-name rename, origin coordinates, +/- with Shift x8 and Alt x4,
Move to player (feet block), Y rotation, horizontal mirror cycling, preview visibility,
Ignore entities and the existing material list. Rename persists as an optional
`displayName` in LoadedSchematics.json; older session entries keep the filename-based
name. It neither renames the source file nor changes its contents.

The existing renderer/paste backend stores the transformed minimum corner, whereas
Litematica edits a placement origin. `PlacementTransform` recovers the original
corner from ordered transforms and dimensions. The GUI edits that world-space
origin and adjusts the stored minimum after rotating/mirroring, keeping the origin
fixed. The block/entity/NBT transform pipeline is reused. Mirrors use rotated local
axes, not unordered X/Z flip counters. Three-dimensional legacy orientations that
cannot be represented by this page show CUSTOM and disable the two planar controls;
the optional direct Controls binding retains the earlier X/Y/Z transform UI.

A merged region row is shown because the current loader collapses sub-regions.
Independent sub-region toggles/configuration/reset, placement/axis locks, separate
rendering, enclosing-box control and the verifier remain unavailable in their
upstream locations. Placement still controls existing preview visibility; it is
not yet a separate enable switch for the tool/backend. Ignore entities uses the
existing shared preview/paste entity option. These limits are identified in tooltips.

The page guards mutations with the opened client-world identity and instance
membership. Leaving that world or unloading the instance disables its controls.
Resize/child-page return retains the name draft; coordinate fields synchronize
only when the underlying geometry changes. Persistence, materials, NBT orientation,
resource-pack appearance and native game interactions require the phase 4 checks
in TESTING.md; automated geometry checks do not establish full visual parity.


## Phase 5: configuration

Both M > Configuration and the Forge mod-list Config button now open the ported
configuration page. It follows GuiConfigs' seven tabs at (10,26), browser origin
(10,50), 22-pixel config rows, measured label column, tab-specific option widths
(180/140/204), reset buttons and expandable search with a 140-pixel key filter.
The string-list dialog uses the centered 400-pixel MaLiLib layout, numbered rows,
insert/remove/move icons and row resets. The MaLiLib atlas is copied unchanged.
At narrow scaled resolutions the tabs wrap and the columns shrink to keep controls
reachable. A Done button is provided for returning to the Forge parent screen.

Existing Forge properties remain in schematica_plus.cfg. General, printer, hotbar
slots, tool and local server settings are in Generic; debug information is in Info
Overlays; rendering options are in Visuals. All also includes the mod's existing
Minecraft key bindings. Hidden properties such as material sort order remain hidden.
Properties are sorted by their internal names and can be searched by translated
label, name, category or the word modified. Numeric rows support validated text
and bounded sliders; unbounded numbers retain text input. Invalid values are marked
red and do not replace the last applied property on close. Drafts survive tab,
filter, scroll and resize changes. Closing applies valid drafts through the existing
configuration handler, writes settings and refreshes schematic render chunks.

The Hotkeys tab edits existing single-key or mouse-button bindings, shows conflicts,
resets to Minecraft defaults and saves options.txt on close. During capture, Escape
unbinds and Tab is assignable; captured input cannot close the screen, navigate focus
or enter the search field. The search key filter uses the same native key codes.
Escape first closes the string-list dialog or hides search; Shift+Escape bypasses
search dismissal. Ordinary text typing outside an editor opens search.

At phase 5, Colors, Render Layers and advanced key settings retained their upstream locations
with disabled controls and explanatory tooltips. Phases 7 and 8 below enable Render
Layers and Colors. That stage did not yet
provide Litematica's color options, global render-layer model, key chords or trigger
settings. No cross-mod config switcher is shown: there is no MaLiLib config registry
in this 1.7.10 port. This phase ports the config presentation and existing Plus
settings, not the entire modern Litematica option set. Phase 6 below replaces the
material list; other pending pages still need their ports. Native font, scaling and visual
comparisons remain manual checks; passing tests does not establish visual parity.


## Phase 6: placement materials

The placement Materials button and the legacy Controls Materials button now open
GuiSchematicMaterials in the ported UI. It follows GuiMaterialList's top actions at
(12,24), right-aligned multiplier, info icon, browser at (10,44), 22-pixel header and
rows, four sortable columns, right-aligned Ignore buttons and width-dependent
footer. Names/counts determine column widths. Small windows wrap actions and shrink
columns to keep controls reachable; hover details retain full counts and item stack
sizes. Search replaces the header text, with the original magnifier on the right.
The original Litematica atlas supplies the info/sort icons without new image edits.
Minecraft 1.7.10 item rendering is scoped so lighting, depth and matrix state return
to the surrounding UI after each icon. This does not establish native visual parity.

The backend counts one pick-block item per non-air schematic position, preserving
the old material estimate convention. Item identity now includes item NBT, so
variants with identical item IDs/metadata are not merged and cannot borrow each
other's inventory counts. Inventory covers main slots, including the hotbar; it
does not unpack bags, cells or other containers. Creative mode shows actual item
counts rather than an unlimited-inventory marker. Existing blocks are still checked
by block ID and metadata, not full tile NBT. Multipart counts, paired/multi-block
items, fluid quantities and recipe decomposition are not guaranteed by this generic
pick-block mapping. The info tooltip states these limits.

Scanning runs on the client thread in batches of at most 4096 positions or roughly
4 ms between calls; a slow individual mod pick-block hook can exceed that budget.
Only a completed snapshot is shown/exported. Moving, transforming or replacing the
placement restarts the scan, while closing the page, unloading the placement or
leaving the captured client world cancels it. Rendering-layer scope uses the
placement's layer filter without altering that filter (extended to global ranges in phase 7). Unloaded
real-world chunks/out-of-height positions stay unverified and count conservatively
as missing; they are not included as completed positions. Failed/unavailable pick
items are counted separately. World progress is a scan-time snapshot; inventory
counts refresh once per second while the page is open.

Available: Refresh/F5, All/Render layers, current-inventory Hide available, Ignore,
Clear ignored, text/registry search, all four column sorts, multiplier and UTF-8
exports. At multiplier 1, Missing subtracts placed blocks; at larger multipliers it
uses the full multiplied total as upstream does. Arithmetic uses long integers.
Ignore is local to the page and survives rescans; hiding available items follows
current counts rather than upstream's remembered resource-gathering behavior.
The old NAME_ASC/NAME_DESC/SIZE_ASC/SIZE_DESC preference is read; new sort criteria
persist through the existing hidden sortType property.

Write to file exports exactly the visible, sorted rows to a new file in dumps:
plain text by default, Shift for CSV, Alt for JSON. Existing dumps are not overwritten;
failures produce feedback. JSON is Schematica Plus material-list schema version 1,
including registry IDs, serialized item variant NBT and counts, not a modern
Litematica recipe-cache format. Raw Materials, Info HUD and Clear cache remain
unavailable with tooltips. Phase 7 below adds global render layers; other pending UI pages are
unchanged. See TESTING.md for game checks, including mod icons and NBT variants.


## Phase 7: render layers

Configuration > Render Layers follows Litematica GuiRenderLayer and MaLiLib
GuiRenderLayerEditBase. It uses the six source tabs at (10,26), without the All
config tab on this page, mode and axis controls at (10,60), 60x20 coordinate fields
at y=86, maximum above minimum with a 23-pixel gap, the original 16x16 plus/minus
icons, range hotkey checkboxes and Set Here below the fields. Tabs wrap at narrow
widths and move the editor down accordingly. The existing configuration screen
hosts the editor so changing tabs retains unsaved configuration drafts and the
correct Forge/main-menu parent. Escape returns to that parent.

All, Single Layer, Layer Range, All Below and All Above cycle in upstream order;
right click reverses mode/axis cycling. Both range endpoints are inclusive. The
range uses absolute world coordinates on X/Y/Z and applies to all loaded placements.
Each mode retains its own coordinate values. Endpoints cannot cross, and arithmetic
saturates rather than wrapping at integer limits. Fields commit on Enter/focus loss.
Plus/minus uses left +1, right -1, Shift x16 and Ctrl x64, with both modifiers x1024.
Set Here uses the active camera entity, subtracting the 1.7.10 yOffset on Y to match
modern entity base coordinates; in Range mode both endpoints become that position.

Existing next/previous-layer bindings move the global range whenever it is active.
Range checkboxes choose the moved endpoint, both checked move the whole interval,
and neither checked chooses the nearest endpoint (ties choose maximum). Whole-range
movement preserves width at integer limits. When global mode is All, the bindings
retain the active placement's legacy local-Y behavior. Legacy local layers remain
available and intersect the global range; the mode tooltip explains this.

The same bounds filter block/highlight compilation, tile/entity preview anchors,
preview ray picking, printer placement/completion and material Render Layers scans.
Changing world coordinates or range state dirties display lists through the existing
bounded rebuild scheduler. Updates can therefore take several frames on large
placements. Entity/TESR geometry is not sliced geometrically: its anchor selects
whether the renderer runs. Standard boundary blocks request all faces so cut faces
stay visible; custom mod renderers that ignore RenderBlocks.renderAllFaces still
need in-game verification. Placement/selection outlines remain full size.

SchematicWorld block reads now retain the complete neighborhood for connection
models, material All counts and transforms. Only preview ray tracing temporarily
filters reads, restoring state in finally. Rendering filters do not mutate schematic
data or affect full-placement Paste/save operations. Material scope uses a clipped
volume snapshot; moving/rotating the placement or changing the range restarts the
scan before results can be exported.

RenderLayers.json uses the existing save/server-and-dimension session keys, resets
when leaving a session, and restores before subsequent previews. Saves are atomic;
a corrupt existing file is preserved instead of overwritten. Closing Configuration
or saving/leaving the current world session persists changes. No extra MaLiLib
runtime is required. Automated checks cover axes, inclusive bounds, clipping,
legacy intersection, overflow, hotkey endpoint selection and persistence isolation.
Native layout and GTNH render checks remain manual in TESTING.md.


## Phase 8: colors and HSV editor

Configuration > Colors now follows the upstream list of 11 color options in source
order. Each row has the 100-pixel option area, a hex field, the original 18x18 color
indicator and Reset. All-tab search also includes these options. The indicator uses
an opaque RGB preview inside white/black borders, as WidgetColorIndicator does;
alpha is edited numerically or in the picker. The internal controls keep keyboard
focus visible. Unavailable features retain their rows with disabled editors and
explanatory tooltips.

Clicking a color opens the 300x180 MaLiLib GuiColorEditorHSV dialog centered over the
configuration page. The layout retains the 102x102 square at (+6,+24), 16-pixel
vertical hue strip, H/S/V/R/G/B/A rows starting at (+148,+24), 90x12 sliders,
32x12 numeric fields and the hex field at (+160,+151). The square uses horizontal
value and reversed vertical saturation, matching this upstream version. The lower
left 32x32 preview is opaque like upstream. Escape closes the dialog and retains
its valid edits in the configuration draft; closing Configuration applies/saves
valid drafts and refreshes schematic display lists. Tab, arrow keys and captured
mouse drags work inside the dialog. Clicking outside does not dismiss it.

A batched vertex-color grid replaces the modern texture/render-pipeline code.
The hue strips use six segments; the square uses one strip per row, interpolating
RGB at the current hue. No dynamic textures, frame-by-frame texture allocations,
new image assets or MaLiLib runtime are needed. The drawing adapter scopes blend,
alpha test and shade-model state, with the surrounding widget's scissor intact.
Native Minecraft 1.7.10 font and text-field rendering still apply.

Hex input accepts RRGGBB or AARRGGBB with optional # or 0x; six digits are opaque,
eight digits start with alpha. Applied edits are written as #AARRGGBB. Incomplete or
invalid input never overwrites the last valid color. The outer config row remains
marked invalid until repaired/reset; inside the picker a bad hex draft stays red
and the last valid picker result is retained on closing. Component fields apply on
Enter/focus loss and clamp H to 0..360, S/V to 0..100 and RGBA to 0..255. HSV edits
preserve the chosen hue at black, and alpha-only edits do not change RGB/HSV.

Five options are active: areaSelectionBoxSideColor, schematicOverlayColorExtra,
schematicOverlayColorMissing, schematicOverlayColorWrongBlock and
schematicOverlayColorWrongState. Their initial/default values now match Litematica
(#30FFFFFF, #4CFF4CE6, #2C33B3E6, #4CFF3333 and #4CFF9010 respectively), replacing
previous hard-coded overlay colors. Selection edges/endpoints keep their existing
colors. The overlay colors affect both the existing face and outline buffers;
Wrong State compares metadata, not tile entity NBT. Existing highlight/face/line
visibility switches still apply.

Inventory highlight, material HUD, the three rebuild selection overlays and
pre-defined equivalent-block highlighting are pending. Their color rows remain
unavailable; no unrelated existing renderer is presented as those features. The
Forge config stores colors in its colors category, preserving malformed on-disk
values for repair while using defaults at runtime. Automated checks cover ARGB
parsing/channels, 10,000 RGB/HSV round trips, alpha retention, validation/reset,
Forge persistence, drag capture and keyboard adjustment. In-game appearance and
resource-pack interactions remain manual checks.


## Phase 9: schematic manager and file dialogs

M > Schematic Manager opens the source-layout browser at (10,24), with height H-60.
Selecting a .litematic shows Schematic Edit + edit selector, Import, Export As +
export selector, then File + file-operation selector. Legacy .schematic and Plus
.schemplus show Import and File operations. No file selection hides these actions.
Buttons start at (10,H-26), use text width +10 with 4-pixel gaps, and retain the
right-aligned Main Menu button. If the row cannot fit, whole operation groups wrap
upward and the browser shrinks; controls within an oversized group trim their text.
Left/right click cycles selectors in opposite directions. Metadata editing, author,
preview generation, import and export are pending: their execution buttons remain
disabled. Their selectors display the upstream choices with a pending tooltip.

Rename, Copy and Delete operate on all three supported extensions. Rename File on
the Load screen is also connected. The shared browser Create Directory toolbar
button now works on loading, saving and directory-picker screens. Text dialogs adapt
MaLiLib GuiTextInputBase's centered 260x80 layout, title at (+10,+4), 240x20 field at
(+12,+20), and OK/Reset/Cancel row at (+10,+50). Failed operations keep the dialog
and input open with an expanded error area. Enter submits from the field; Reset
restores the original value; Escape cancels. Confirmation dialogs use the upstream
400-pixel width, green OK on the left and red Cancel beside it, retaining initial
keyboard focus on Cancel. They also serve the existing save-overwrite dialog.
Root screen shortcuts such as F5 and Backspace do not run beneath a modal.

Names must be single path components, without Windows reserved characters/names,
trailing spaces/dots or a changed extension. No operation overwrites an existing
file (including Shift-click); the port's tooltips describe that behavior. Copy uses
a temporary sibling and preserves the file bytes without NBT conversion. Rename
also preserves bytes. File mutations validate the selected file's identity, size
and full-precision modification time again; stale selections and linked files are
rejected. Directories are created one level at a time, cannot escape the browser
root, and cannot be renamed/deleted through these file-only actions.

Rename updates matching LoadedSchematics.json references across sessions/dimensions
and matching in-memory instances, retaining display names, transforms and positions.
Unknown JSON properties are preserved by this operation. An unreadable session
index prevents rename; a changed index is not overwritten, and a write failure
attempts to restore the original filename. These checks protect normal local use,
not concurrent hostile changes to filesystem links. Old Coordinates.json defaults,
external scripts and unrelated third-party references are not rewritten.

Delete requires confirmation and removes only the selected file. Already loaded
previews and session records are retained, as with deleting a source externally;
they cannot reload that missing source later. Copy/rename select the resulting
file after refresh. Creating a directory selects it; opening it is a separate action.
Read-only browsing and file operations do not require an active world. File info
still shows filename, size and date, not parsed metadata or screenshot previews.
Automated checks cover preserved bytes, no overwrite/format change, invalid names,
stale/deleted targets, directory refusal and cross-session reference preservation.
Live-game rendering, scaling and modpack interactions remain manual checks.


## Phase 10: loaded sources and placements

Loaded Schematics and Schematic Placements now have separate backends. The library
owns one source per canonical file path and zero or more independent SchematicWorld
placements. Loading the same path reuses its cached source; use Reload to read
changed disk contents. The Load screen's Create Placement checkbox is operational.
Unchecked loading neither creates render data nor changes the active placement.
The Loaded list exposes Create Placement, Reload, Save to File and Unload. The
Placements list and legacy controls remove individual placements while retaining
their source. Unload in the Loaded list removes that source and all its placements.

The source holds an immutable snapshot of the original file bytes, validated through
the existing format reader. Each placement decodes that snapshot into separate
blocks, metadata, tile entities and entities. Transforms therefore cannot mutate
another placement or the cached source. This preserves original visual-state NBT
and modern long arrays without a lossy round trip through the legacy writer. It
also preserves fields the current reader does not understand for subsequent Save
to File. Existing rendering/conversion limitations of each reader still apply.
Snapshots are bounded to 128 MiB of file bytes in addition to the existing decoded
NBT/volume limits. Parsing and creation remain synchronous; placements retain their
own full mutable geometry, rather than sharing transformed block arrays.

Create Placement from the Loaded list uses the player's position and selects the
new placement. Creation from the file browser retains the previous saved-coordinate
or look-target behavior. Placement names are unique without reusing an existing
numeric suffix. The cached source can create placements or be saved even after
its disk file is removed. No new .litematic encoder is introduced: Save to File
copies the cached source bytes in their original extension, including entities,
NBT and metadata, without applying placement transforms. It uses the existing
browser pattern, contained output paths, an explicit overwrite dialog and sibling
temporary files. Saving a copy does not retarget the loaded source.

Reload reads/validates a new snapshot and prepares all dependent placements and
render data before publishing replacements. A preparation failure keeps the old
snapshot and placements and releases prepared render buffers. Successful reload
retains placement order, names, world-space origins, ordered three-axis transforms,
visibility, entity/NBT flags and legacy layer settings (clamped to the new height).
Origin retention also handles changed source dimensions. The active placement
follows its replacement; reloading another source does not reset the active printer.
Reload replaces placement block/entity data with source-file data; it does not
merge local edits. Open child pages holding replaced placements become unavailable
through their existing membership checks.

LoadedSchematics.json keeps the existing per-world/server/dimension arrays. Regular
entries remain placement records; a source with no placements gets sourceOnly=true.
Old records without that field are restored as placements and share their source
by canonical path. Source-only records never create a preview on restore. Local
layer settings now persist too. Explicitly having no active placement survives a
session reload. One malformed/missing entry no longer prevents later entries from
restoring. World transitions clear both sources and placements, and the file manager
renames current source paths plus matching persisted records, including unplaced
sources. A rename cannot take a path already owned by another loaded source,
even if that source's disk file was deleted. Session persistence references files,
not embedded snapshot bytes;
a deleted source cannot restore after leaving that session.

Automated coverage includes canonical-path deduplication, independent placement
factories, removal vs. unload, stale actions, staged reload rollback, unrelated
placement order, no-placement sources, retained unknown NBT, fresh modern long
arrays, byte-identical saving, no overwrite/format relabeling, save path checks,
renamed source-only records and changed-size origin calculations. Native GTNH
rendering and world-session behavior still require the checks in TESTING.md.
Area libraries, subregions, verifier/tasks, advanced input and format conversion
remain separate backend work; this phase does not complete those features.

## Phase 11: complete upstream translation catalogs

Litematica 26.1.2-0.27.8 is the translation-key baseline for the UI port. All 13
upstream language catalogs are bundled, including keys for features not yet
ported, block names and translator credits. The English catalog has 1,149 keys;
the other catalogs each have 1,150, including an additional shader warning that
is absent from upstream English. MaLiLib 26.1.2-0.28.8 contributes its complete
455-key catalogs for 12 languages. It supplies no Turkish catalog; those labels
use Minecraft's normal English fallback. No translations are synthesized.

The merged catalogs remain in assets/schematica_plus_litematica/lang. Original
key spelling, case, placeholders, formatting codes and text are retained, including
upstream trailing spaces (exempted from Git's check for these generated files).
Only the storage format changes from JSON to UTF-8 .lang, and region suffixes change
case for 1.7.10 (en_us -> en_US). pack.mcmeta registers lzh (Literary Chinese),
which vanilla 1.7.10 does not list. Vanilla and other mods' untranslated text in
that locale falls back to English; this does not provide new game translations
or fonts. Available glyphs still depend on the active 1.7.10 font/resource pack.

Ported screens use UiTranslations for upstream strings. Minecraft loads and
selects the language normally, including resource-pack overrides. UiTranslations
decodes escaped newlines, tabs, carriage returns and backslashes in the template
before formatting, so inserted paths such as D:\new\test are never decoded.
Native 1.7.10 numeric-placeholder normalization still applies. Original literal
escapes are retained, including the literal backslash-n in the upstream Turkish
commandUseStrict description. Tool modes, area corner mode, directory navigation,
list-editor actions and the cached-source save title now use their original keys.

The existing Schematica catalogs are retained for legacy controls, printer/server
settings and commands. Plus-specific behavior and implementation-limit messages
keep their own keys, including the extra save-options page and file operations
whose overwrite behavior differs from upstream. The main menu retains the
Schematica Plus product name. Adding a translation does not enable its feature.
New ported functions should use the upstream key whenever the behavior matches,
and should not edit generated upstream translations to describe Plus differences.

Regenerate from the two reference source roots using Python 3:

```text
python tools/import_translations.py --litematica <litematica-source-root> --malilib <malilib-source-root>
```

Add --check to compare every generated file against the sources without writing.
The importer rejects duplicate/conflicting keys and preserves all source entries.
It needs no additional Python packages and is not a build-time dependency.
Automated tests load catalogs with Minecraft's language parser, check complete
key coverage and fallback, and exercise multiline formatting with user paths.
Native language switching, font rendering and layout remain manual checks.

## Phase 12: persistent single-box area library

The Area Selection browser is operational. Each world/server/dimension owns a
list of named selections and an explicitly selected entry, or none. Entries
keep independent corner coordinates, box names and outline visibility. The
browser supports creating, copying, renaming, configuring, selecting/unselecting,
searching and confirmed deletion. Its top action bar and row button order follow
Litematica's GuiAreaSelectionManager and WidgetAreaSelectionEntry. Configure
selects the entry before opening the existing Simple editor. From Placement
creates one box from that placement's current transformed bounds, not its
original untransformed source. New selections start at the player's feet.

The editor's selection and box names now persist independently. Coordinate
edits are bounded to Minecraft 1.7.10 coordinates. Numeric field synchronization
does not publish intermediate values. The active entry supplies the existing
corner vectors, outline renderer, tool selection and schematic save operation.
Tool clicks persist the changed corners. The save screen defaults to the area
name; the tool HUD shows the selected area's name or an explicit no-selection
message. The save screen checks that its original world/session/selection is
still active, including after overwrite confirmation. Deselecting/deleting the active area
clears its outline and blocks saving and area-based world edits. It does not
select an unrelated entry automatically. Existing single-block edit restrictions
and server save/edit permissions are unchanged.

AreaSelection.json remains keyed by the existing session identity. Each session
now has version=2, a selections array with stable UUIDs, and a nullable selected
ID. Old pointA/pointB-style records migrate to one entry named Selection with
their coordinates and visibility intact. A previously unseen session starts
with the same hidden zero-coordinate selection as before. A persisted empty
library or explicit deselection stays empty/deselected on reconnect. Unknown
JSON fields and other sessions survive writes. Malformed data, unsupported
versions, duplicate IDs/names and invalid coordinates block restoration without
overwriting that data. Saves use atomic replacement and reject stale changes to
the current session on disk; changes to other sessions are preserved. This is
normal-use conflict detection, not a lock against concurrent filesystem writers.
Reload external edits by reconnecting/re-entering the session. Failed writes are
logged and surfaced in the UI; in-memory changes may then be lost on exit.
Limits are 4,096 entries per session and 16 MiB for the settings file.

This stage retains the Simple, one-box editing/saving model for every entry.
The list is a per-session library, not Litematica's filesystem directory browser.
Multiple boxes within one selection, directory organization/import/export,
manual origins, corner selection/expand mode and multi-region placement editing
remain unported. Normal mode remains unavailable. From Placement includes the
whole bounding box, including gaps in an already merged schematic. No sparse
multi-box saving or file-format change is introduced here. Existing upstream
translation keys are used where behavior matches; Plus-specific limitations and
persistence errors have separate English/Chinese keys.

Automated coverage includes legacy migration, independent copies, switching and
round-trip restoration, deselection/deletion without resurrection, dimension
isolation, stale objects, coordinate rejection and corrupt/externally modified
settings. Native GUI, tool interactions and rendered outlines remain manual tests.


## Phase 13: multi-box selections and sparse capture

This phase supersedes Phase 12's single-box limitations. The area editor now uses
Litematica's Normal layout: mode/corner controls at y=24, selection name at y=59,
New sub-region / Manual Origin / Save at y=81, and subregion rows at y=116.
Rows use the original Configure / Rename / Remove order and selection highlight.
Configure opens the subregion coordinate page with the original two-corner layout
and plus/minus icons. Original translation keys are retained. Mode switching,
manual origin, corner targeting/expand mode and area analysis remain unavailable;
the controls disclose that status. The extra outline toggle and error line are
Plus controls. Native rendering and layout have not been tested in-game.

Each selection contains up to 256 named boxes, an explicitly selected box or none,
and its existing guide flag. Tool clicks only modify the selected box. Saving,
fill, replace and delete use the union of all boxes, even if no box is selected.
An empty selection cannot save/edit; single-block selections can now be edited.
Removing a box does not implicitly select another. All boxes render separately;
only the active box gets the red/blue corner markers. The HUD shows both area and
subregion names, or an explicit missing-subregion message. Search includes every
box name. From Placement retains known subregions with their current transformed
coordinates; sources without region data produce one bounding box. Import is
validated before adding any selection to the library.

AreaSelection.json writes version=3 with per-area boxes and nullable selectedBox.
Both version=2 single-box entries and unversioned legacy entries migrate. Copies
have independent boxes, coordinates and selected-box state. Explicit deselection,
empty selections, other sessions and unknown fields continue to survive writes.
Existing atomic-write, stale-session and external-edit safeguards still apply.

SchematicRegion is part of the public schematic API. Schematics keep immutable
region bounds plus a union mask. Capture skips unselected blocks, tile entities
and entities whose position lies outside the union. Overlap is captured once.
The queued save snapshots bounds when submitted, including before an overwrite
confirmation. Area edits use one masked job; both integrated-server operations
and the command fallback skip the gaps. Renderer highlights, material checks and
the printer (including creative destruction) exclude unselected positions.
Rotations and mirrors transform region bounds along with schematic contents.

Alpha files store SchematicaPlusRegionsVersion=1 and SchematicaPlusRegions, a list
of Name and six-integer Bounds values in schematic-local coordinates. Multiple
or partial boxes force extended block encoding and the existing safe .schemplus
filename upgrade. A single complete box can still use legacy .schematic; ordinary
.schematic files remain readable as full boxes. Malformed or unsupported region
data fails loading instead of being treated as a full rectangle. .litematic import
retains each region's name and bounds, including negative sizes, while still
merging block contents into one preview. No .litematic writer or independently
editable placement subregions is provided. Older mod versions do not understand
the new bounds tag, so use this version when loading these multi-box saves.

Storage remains a dense bounding volume with a mask. Existing dimension and
16-million-block limits therefore apply to the enclosing box, not just the sum
of selected boxes. Widely separated small boxes can exceed those limits.
The capture queue still walks enclosing chunks, although excluded cells are not
read from the world. Multiplayer NBT availability and visual adapters are unchanged.
Automated tests cover migration, atomic placement import, explicit box deselection,
copy isolation, bounds/overlap/gaps, all six transforms, file conversion/round-trip,
and skipping edit gaps before accessing the world. Full Forge world/client/GL
integration requires the native checks in TESTING.md.


## Phase 14: manual origins

Normal mode now supports the upstream Manual Origin toggle, origin selection
checkbox, XYZ fields, plus/minus icons and Move to player control. Enabling the
origin starts it at the player's feet, matching upstream. Selecting its checkbox
makes both tool clicks move the origin without modifying box corners. Selecting
or adding a subregion returns tool targeting to the box. Disabling the manual
origin uses the minimum corner of all boxes again. The origin can lie outside
all boxes, within 1.7.10 world coordinate limits; it does not expand the captured
bounding volume. It is shown with the upstream orange outline, or cyan outline
and translucent orange sides when selected. The HUD and area browser show the
actual origin. From Placement imports both current region bounds and world origin.

AreaSelection.json writes version=4, adding nullable origin and originSelected.
Versions 2 and 3 and unversioned selections migrate with automatic origins. Copy,
restore, deselection and validation preserve independent origin state. An origin
alone does not make an empty selection saveable. Queued captures use an immutable
origin offset together with their region snapshot.

The schematic API exposes immutable SchematicOrigin coordinates relative to its
normalized minimum corner. Alpha stores nonzero offsets in the three-integer
SchematicaPlusOrigin tag with SchematicaPlusOriginVersion=1. Such offsets require
.schemplus even for a single box; a zero offset does not force an upgrade. Old
files without the tag use zero. Malformed/incomplete/future origin tags fail
loading rather than silently relocating the structure. Litematic import uses
its original (0,0,0) reference, offset by the merged region minimum; it no longer
loses that reference when regions extend into negative coordinates.

Rotations/mirrors now preserve the world origin, including the legacy control
screen. The dense minimum moves to compensate, and blocks, regions and entities
keep using normalized coordinates internally. The Litematica placement editor
shows the stored origin and checks the planned transform against coordinate
limits. Move to player and look-target placement align this origin. The old
control screen still displays the dense minimum. Existing Coordinates.json
bookmarks retain their old minimum-corner meaning and apply it after transforms.
LoadedSchematics.json adds a world-origin tuple; restoration uses it after the
source transforms so an updated source origin does not move the world anchor.
Entries lacking the tuple retain their saved minimum. Reload also keeps the
world origin when the source dimensions or origin change.

Server downloads now carry the origin and named region bounds in a bounded,
versioned header. The client installs the mask before receiving block chunks.
The begin acknowledgement advertises support; a new server cancels downloads
with this metadata when an old client cannot preserve it. Plain legacy downloads
remain compatible. Both ends must be updated to preserve metadata: an old server
does not send it, so a new client cannot recover it from those downloads.

Automated coverage includes origin isolation/copy, targeted tool changes,
version migration, out-of-box capture without volume growth, complete and mixed
3D transforms, legacy/new placement coordinate meanings and file round-trips.
Download checks cover old/new headers, support negotiation, Unicode region names,
the 256-region limit, malformed headers and invalid bounds before allocation.
Native GUI, rendering, modded block orientation and world placement are still
manual checks. Simple-mode switching, expand/corner targeting, area analysis and
independent transforms of placed subregions remain unported.

## Phase 15: independent placement subregions

Placement subregions now have immutable per-region positions, Y rotation,
horizontal mirroring, enabled/rendering flags, entity exclusion and coordinate
locks. Geometry uses coordinates relative to the schematic origin; global
transforms are applied afterwards. World-coordinate input is inverted through
the global transform before updating a region. Changes compose a fresh preview
from the cached source snapshot and are committed only after composition succeeds.
The original file and other placements remain unchanged. Disabled regions are
excluded from the combined bounds and mask; all-disabled placements have an empty
mask. Rendering-only exclusion keeps block data available for material/paste use.
The printer also observes the rendering mask, as it does for render layers.

LoadedSchematics.json stores a versioned subregions object containing named
overrides and optional selection. Old entries use defaults. Reload carries over
overrides for matching names, gives new regions defaults and drops removed names.
The enclosing volume still has the existing allocation limits. Legacy schematics
without region metadata expose one full region named Region.

This operates on the currently imported merged source. If original Litematic
regions overlapped with different block data, that data was already flattened
by the importer and cannot be recovered here. Region pivots use normalized minimum
corners; original signed-size Litematic region pivots are not yet retained.
Overlapping placed regions resolve blocks in source region order, later regions
winning including air. Source entities belong to the first original containing
region, preventing duplicate entities from overlapping capture boxes. Entity and
tile-entity transforms reuse the existing 1.7.10 adapters. Native mod rendering
and world placement still require in-game verification.

The placement configuration lists real named subregions with upstream Configure,
enabled buttons and modified markers. All on/off and Reset are active. The new
subregion configuration page follows the upstream right-column positions, icons
and translation keys: XYZ world coordinates, nudge, coordinate locks, Move to
player, Y rotation, horizontal mirror, enabled/rendering/entity flags and reset.
The upstream placeholder Slice control remains unavailable. Main placement locks
and separate main placement/rendering flags remain as previously documented.

Clicking a region row selects or deselects it for the Move tool. Selection is
persisted, shown in the HUD and outlined in cyan, including a selected disabled
region. Other enabled/rendered regions receive individual outlines. From Placement
uses the composed enabled bounds; it and Paste refuse an all-disabled placement.
Material counting uses enabled regions and the existing layer range, independently
of the rendering-only switch. Unmodified session restoration avoids rebuilding.

Headless tests cover independent state, all horizontal region orientations composed
with global 3D transforms, inverse coordinates, all-disabled geometry, locks,
visibility, legacy full regions, JSON round-trips, changed-source names and invalid
saved data/allocation bounds. They do not execute Forge block/TE/GL composition.
