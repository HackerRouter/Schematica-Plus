# Litematica UI adaptation

Current architecture and maintenance guide, reviewed 2026-10-12. Target:
Minecraft 1.7.10, Forge 10.13.4.1614, MCP stable_12 and Java 8 bytecode.
Behavior and translations follow Litematica **26.1.2-0.27.12** and MaLiLib
**26.1.2-0.28.11**. The UI is internal to Schematica Plus; it does not register a
MaLiLib mod or offer binary compatibility with modern MaLiLib.

The earlier staged implementation notes are retained in Git history. Use this
page for current contracts, [TESTING](../TESTING.md) for acceptance scenarios,
and [HANDOUT](../HANDOUT.md) for outstanding work and dated results.

## Sources and attribution

[ModularUI](https://github.com/GTNewHorizons/ModularUI),
[ModularUI2](https://github.com/GTNewHorizons/ModularUI2/tree/40fdbdabab7767999b84fda0dc5e9ff47e7edb0b)
and [OpenModsLib](https://github.com/GTNewHorizons/OpenModsLib/tree/e5e8177f2dd2297d5aea6bb733955a67ffa63c95)
provided architectural references for component ownership, rendering boundaries
and focus/capture. None becomes a new runtime dependency.
Litematica/MaLiLib layouts, icon atlases and translations retain their original
attribution and LGPL notices. The original asset imports predate the current
behavior reference; see [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md) for provenance.

## Component and input contract

- `UiBounds`, `UiInput` and `UiListModel` are game-independent, tested models.
- `UiPanel` owns its children. Coordinates are absolute scaled GUI pixels; every
  ancestor clips both drawing and input. Add order is draw order, and hit tests
  run backwards so only the topmost eligible control handles an event.
- Focus is unique. Tab traverses visible/enabled controls. Mouse capture survives
  moving outside a control, and cancels on removal, disable, close or modal opening.
  A modal blocks underlying input and restores the earlier focus when dismissed.
- `UiScreen` bridges the GuiScreen lifecycle and native input. It owns keyboard
  repeat, parent navigation, wrapped tooltips, modal handling and relayout.
- `UiDraw` is the rendering boundary. `MinecraftUiDraw` scopes GL state, preserves
  prior scissors and converts clipping through the actual `ScaledResolution`,
  including odd window dimensions. Controls never consume framebuffer coordinates.
- Resize updates existing controls. Draft text, selection and scroll position
  belong to controls/models, not to a fresh layout pass.
- Lists filter with `Locale.ROOT`, retain selection by entry, clamp scroll and
  draw visible rows only. Enter cannot activate an entry filtered out of view.
  Releasing a captured row after filtering must not act on its replacement.
- Text editing supports selection and clipboard. Integer drafts may temporarily
  be empty or a minus sign; commit on Enter/focus loss and validate at the boundary.
  Invalid configuration drafts retain the last applied value, with visible feedback.
- Hotkey settings support contexts, press/release/both, key order, extra keys,
  exclusivity and cancellation. Modal/key capture must not leak input to search or
  the world. Closing or losing focus must not leave vanilla keys pressed.

The input transformer intercepts Minecraft/GuiScreen event polling at the boundary;
ordinary native attack/use and inventory/Backhand interactions must remain intact.
M opens the menu on release. Releasing a consumed M chord does not also open it.
The production hotkey registry no longer exposes a synthetic UI demonstration page.
Use real screens and the framework tests for regression checks.

## Drawing and layout

Match upstream scaled coordinates, text-derived button widths, grouping and icon
states. Titles start at (20, 10), the screen overlay is `#B0000000` and list/info
outlines use `#FF999999`. Vanilla button backgrounds and fonts come from the active
1.7.10 resource pack; they need not rasterize identically to modern Minecraft.
Keyboard focus retains a visible border. Axis-aligned interfaces use nested
scissors without adding a stencil-buffer requirement.

The report icon on the main/config screens aligns with the version/header region
and right content edge. It opens the author message with a persistent skip option.
Confirmation dialogs share a right-aligned bottom action group; optional checkboxes
sit at the lower left without overlapping those actions. Long content scrolls and
wraps using Unicode-font-aware widths, preserving ordinary Latin word groups where
space permits. Test Chinese paragraphs, `Pull Request`, mixed text, GUI scales and
narrow windows through the actual report and client-only paste dialogs.

Icon atlases retain hover/active states. Rendering controls, material item icons,
3D previews and tooltips must restore texture, tint, blend, lighting, depth and
scissor state before returning to other mod screens or the world.

## Current screens and data ownership

Paths are relative to `src/main/java/com/github/lunatrius/schematica/client/gui/`.

| Area | Entry points and implemented behavior |
| --- | --- |
| Main/config | `GuiSchematicMainMenu`, `GuiModConfig`, `config/`; grouped navigation, tool selector, report entry, searchable option/hotkey/color/layer tabs |
| Browser/manager | `browser/`, `load/`; directory bounds, search, file actions, metadata, thumbnails/3D preview, asynchronous loading, import/export |
| Sources/placements | `placement/`; separate source and placement lists, origin/rotation/mirror, named independent subregions, coordinate locks, render/enabled/entity flags, grid settings |
| Selections/save | `save/`; Normal multi-box and Simple selection, named boxes, manual origins, file-backed selection folders, capture options and explicit Shift overwrite |
| Materials | `control/GuiSchematicMaterials`, `material/`; bounded scans, inventory/partial counts, sorting/ignore/multiplier, replacement/container support, exports, HUD/cache and native NEI planning |
| Verifier | `GuiSchematicVerifier`; categories, ignored pairs, pause/resume/recheck and selected markers/comparison overlays |
| Tasks/projects | `GuiTaskManager`, `projects/`; bounded-work cancellation, project versions/origins and browsing |
| Shared dialogs | `GuiConfirmAction`, `GuiTextPrompt`, `framework/`; validation, wrapped content, focus/capture and parent navigation |

### Sources and placements

Repeated loading of a path reuses its source; Create Placement controls whether
another placement is added. Unloading a source removes its placements; removing a
placement leaves the source. Configuring inactive placement B cannot change active
placement A. Local subregion transforms compose with global transforms around
preserved signed anchors. The last enabled overlapping region supplies the visible
block, including air. Reset restores source geometry, not an already transformed copy.

Save Source preserves an unedited file's original bytes. REBUILD edits shared source
data in memory; edited saves use `.schemplus`, with explicit limits for overlapping
independent payloads. Dirty-source indicators/discard confirmation must remain.
Supported formats are `.schematic`, `.schemplus`, `.litematic`, `.nbt` and import-only
Sponge `.schem`. An unknown modern state is not evidence that its 1.7.10 conversion
is complete; retain the import warning/report.

### Persistence and work in progress

Per-world/server/dimension session identity isolates sources, placements, selections
and layer state. Normal and Simple selection state remain independent. Preserve
compatible unknown fields and do not overwrite unreadable/future schema files.
`UiState` retains the implemented page/search/sort preferences; configuration values
and placement-specific material settings use their owning stores.

Background parsing/scans validate their generation and world/source identity before
publishing results. Changing worlds, unloading or reloading a source must invalidate
stale callbacks. A queued operation is not complete, and an incomplete scan cannot
be exported as final results. Coordinate/rename drafts survive resize/navigation
without mutating another placement or a previous world's objects.

## Translation maintenance

Fourteen locale files are bundled from 14 Litematica and 13 MaLiLib catalogs.
`tools/import_translations.py` converts JSON to the 1.7.10 format, retains keys and
translator credits, applies selected Schematica+ branding and repairs legacy format
placeholders. Check against the pinned references using the command in HANDOUT.
Use upstream keys where the meaning matches. Plus-only keys belong in both English
and Chinese files under `assets/schematica/lang` in the resources tree.

Resource-pack overrides use `assets/schematica_plus_litematica/lang/<locale>.lang`.
Test descriptions, escaped newlines, colors and mixed CJK/Latin text. Upstream
translator/author credits are not branding labels and must not be rewritten.

## Limits and acceptance

Subregion Slice remains an explicit unavailable upstream placeholder. Existing
unavailable entries must explain their actual limitation; do not use obsolete
phase labels to disable features that now have implementations.

Framework tests cover focus/capture, clipping math, list state, layout and wrapping;
they do not establish native GL, IME or resource-pack correctness. TESTING contains
production-screen scenarios and separately dated native observations, including the
report/paste footer and Chinese wrapping checks in both target packs. Pixel parity
for every locale, font, scale and renderer remains outside the verified coverage.
