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

Colors, Render Layers and advanced key settings retain their upstream locations
with disabled controls and explanatory tooltips. The current backend does not yet
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
placement's existing single-layer filter without altering that filter. Unloaded
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
unavailable with tooltips. Global render layers and the other pending UI pages are
unchanged. See TESTING.md for game checks, including mod icons and NBT variants.
