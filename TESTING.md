# Verification

Current scenario guide, reviewed 2026-10-25. Run `./gradlew build` and
`LC_ALL=C.UTF-8 ./gradlew test`; the current suite contains **749 tests**.
Steps below are regression scenarios, not claims that every combination passed.
Only dated observation paragraphs record actual game checks; their test counts and
artifact hashes describe that historical build. Use disposable copies and the
same current Plus build on both sides, except explicit client-only cases.
`<evidence-dir>` denotes private, local validation artifacts outside the repository;
these are scenario labels, not distributed files or public download links.

## Survival printer destruction

- Enable `destroyBlocks` with `printBreakWrongBlock`, `printBreakExtraBlock` and
  `printBreakWrongStateBlock` disabled. Survival printing must remove nonmatching
  solid blocks, including extra blocks where the schematic has air. Matching
  blocks, fluids and unbreakable blocks must remain intact.
- Without server assistance, or with server fast breaking disabled, keep native
  mining progress across ticks. With fast breaking available, use the existing
  queued harvesting path and its client/server limits. Check tool wear, harvest
  drops, tool selection and protection cancellation in survival.
- Repeat in creative mode with and without server assistance. Tool durability must
  remain unchanged, including when a durability reserve exceeds the tool's remaining
  uses. Preserve native creative harvesting and the existing `destroyInstantly`
  path. That option must not promise instant survival mining without server support.
- Check all six existing `destroyBlocks` translations and the generated config
  description: none must still label it creative-only. English and Chinese wrong-
  block mining descriptions must distinguish normal and server-assisted speed.

Observed 2026-10-25: build, Checkstyle and 749 JUnit tests pass. The native Forge
development client passes dispatch checks for unassisted, assisted-normal and
assisted-fast survival with only `destroyBlocks` enabled, fluid/bedrock exclusions,
the disabled toggle and retained creative paths. These checks call the actual
client target dispatcher; they do not simulate the complete multiplayer exchange.
The dedicated-server printer fixture separately harvests 64 stone blocks in
survival with 64 tool-damage points and 64 cobblestone drops, then breaks 64 in
creative with unchanged tool damage and no harvest drops. Existing protection,
inventory, durability-reserve and queue-quota checks also pass. Evidence:
`<evidence-dir>/destroy-survival-client.log` and `destroy-survival-server.log`.

Observed 2026-10-25 in the supplied GTNH instance: `schematica_plus.cfg` has
`destroyBlocks`, all three break-mismatch switches, auto-tool and 64 break targets
enabled, with a 45-block work range and an 8 ms iteration budget. The saved `bruh`
placement is 3x11x49 at the recorded player position and is inside that range.
The saved world still contains 463 extra and 332 wrong non-air cells, concentrated
in the higher layers; `latest.log` has no printer exception. This is consistent
with stopping a bottom-up pass before the placement completed. The inventory audit
also found low-durability ordinary tools, and GTNH IguanaTweaks is configured to
reduce regular-tool break speed to zero; auto-tool selection now keeps a usable
tool ahead of an empty hand in that situation. The supplied world and schematic
were read only; no original files were changed.

## Background verification and translucent sorting

The second optimization batch keeps world reads, mod callbacks and OpenGL on the
client thread. Up to 4,096 captured cells per session are compared on a bounded
worker pool, then merged on the client thread. Multiple thin columns and vertical
sections share a batch. The shared snapshot/merge budget is four milliseconds per
tick; this is a soft budget, not a deadline that can interrupt a mod callback.

- Verify a large placement, including thin schematics and partially unloaded
  chunks. Counts, pending-chunk indicators, ignored pairs and error markers must
  agree with a complete scan. World access must never occur on the worker threads.
- Change two distant blocks, including blocks at different heights in one column.
  Repeated notifications must deduplicate into affected 16x16x16 sections, without
  scanning the intervening region. More than 4,096 dirty columns falls back to a
  bounded full invalidation instead of growing the notification map indefinitely.
- Move, rotate, edit or unload a placement while comparison is pending. Old results
  must not enter the current scan. Pause/resume and leaving the world must cancel
  outstanding work and release its queue slots. Changes to comparison settings
  invalidate snapshots. A changed/unloaded column rejects only its own batch parts.
- View colored glass from opposite sides with schematic alpha both off and on.
  Sorting must change the order of cached quads without calling block renderers
  again. Each quad must retain all vertex attributes and deterministic tie order.
- Explicitly dirty an unchanged section: identical geometry must retain its sorted
  display list. Unused native vertex fields are normalized before equality checks.
  Edit the source, hide the section and remove the placement: stale jobs must be
  cancelled, replaced geometry discarded and retained memory released.
- Repeat with native GTNH renderers and shaders. The new raw-vertex replay path
  excludes custom/tile-bearing geometry, nonstandard Tessellators/formats, active
  Iris/OptiFine shaders, OptiFine multitexture and Angelica terrain meshing.
  Missing optional APIs are harmless; an installed but incompatible API retains
  ordinary rendering. See the dated source audit in `compatibility/render-audit.md`.

Sorting shares two workers and at most eight owned tasks/results with verification,
reserving two slots from render submissions. Retained raw geometry/order metadata
has a 32 MiB cap; each capture is at most 4 MiB. Completed sorting uploads have an
8 MiB / three-millisecond soft frame budget, checked between sections. Invisible
work releases its slot; oversized/unsupported geometry uses ordinary rendering.
Equal sorted orders avoid another upload. This does not move native model callbacks
to workers or deduplicate the ordinary fallback display-list compilation.

Observed 2026-10-24: build, Checkstyle and 749 UTF-8 JUnit tests passed. A sparse
update fixture rereads 512 of 5,120 cells, preserving the untouched counts. Unit
tests cover worker isolation, cancellation, stale/partially valid batches, thin
column batching, queue bounds, sort attributes and optional renderer API failures.
The native `tools/headless/render_smoke` client also checks 4,000 comparisons
against the original rules, real verification counts and changes, move/edit
cancellation, pause/resume, world cleanup, opposite-camera sorting without model
rebuilds, identical geometry reuse and offscreen cancellation. Evidence is under
`<evidence-dir>/remaining-client.log` and `render-client/render-smoke-result.txt`.
The previous translation/pixel tests run in the same fixture. These development
client checks do not establish new full-pack/shader acceptance or an FPS gain.

## Translation caches and indexed render updates

The first rendering optimization batch uses
[LitematicaFastMove](https://github.com/hotpad100c/litematicafastmove/tree/c58102a017d6685920b6fd09b0023ce074d5777e)
as a behavior reference. It retains the 1.7.10 display-list renderer and synchronous
world access. That first batch did not add delayed logical movement, background
model building or asynchronous verification; the latter is covered above.

- Move a vanilla projection repeatedly through loaded empty sections with fixed
  lighting. Reuse is allowed only when the compiled and destination regions,
  including a one-block neighbor halo, are proven empty. The check conservatively
  examines entire world sections and rejects unloaded/placeholder chunks.
- Repeat across existing blocks, unloaded terrain, world-lighting changes,
  fixed-light-level changes, world slices and local layers. Invalidated sections
  must rebuild. Custom block classes and tile-bearing blocks retain normal builds.
- Change one real block on a projection's section boundary. Both affected sections
  must update after the existing neighbor halo is applied; unrelated sections must
  remain clean. Repeat after camera movement changes draw ordering and at negative
  world coordinates.
- Enable a repeat grid and move its base. Retain copies and render data for grid
  cells still in range, update their coordinates in the same client tick, remove
  departed cells and create newly entered cells. Content, rotation, mirror and
  grid-setting changes must still rebuild the appropriate copies.
- Check selection and printer targeting immediately after movement. The logical
  placement origin has no settling delay. Rendering retains its shared three-build
  and four-millisecond soft frame budget; cache reuse does not consume build slots.
  Clean scans resume across frames so a long clean prefix cannot starve dirty work.

Observed 2026-10-24: the full build and 735 UTF-8 JUnit tests passed. New unit
coverage includes inclusive section boundaries, neighbor halos, disjoint and
inverted ranges, large coordinate differences, stable indexing after sorting,
1,000 seeded intersection comparisons, reuse budgets and cross-frame fairness.
The separate `tools/headless/render_smoke` fixture passed in a real Minecraft
1.7.10 / Forge 10.13.4.1614 development client with MCP stable_12, Java 8 and
Xvfb/Mesa. After one three-pass compilation, 64 air translations produced zero
additional section builds. Reused and freshly rebuilt complete/sliced projections
matched pixel for pixel in a nonempty 128x128 framebuffer. Obstruction, lighting,
layers, edits, unloaded regions, custom/tile block fallbacks, indexed updates and
grid identity/lifecycle checks passed. The client and integrated server stopped
cleanly. Evidence: `<evidence-dir>/fastmove-client.log` and
`<evidence-dir>/render-client/render-smoke-result.txt`.

These are native renderer/coordinate assertions, not mouse-driven printer tests.
This round does not establish full GTNH 2.8.4/2.9.0-RC-2 acceptance, arbitrary
renderer/coremod compatibility or an FPS gain. Use the scenarios above for those
pack checks; custom mod geometry deliberately keeps the normal rebuild path.

## Ordered printer actions and browser follow-up

Toolbar follow-up (2026-10-24): the favorite and recent graphics occupy 8x8 GUI
pixels, centered within the existing 12x13 click areas and vertically aligned with
the directory buttons. The favorite uses a native 8x8 pixel shape padded to a
12x12 frame; the recent sprite scales to 8x8. The full-size star sprites remain
available to other controls. Build and 726 tests passed. In the Forge development client
at GUI scale 2 and 854x480, the smaller sprites, hover outlines/tooltips and clicks
near the button edges were checked through the actual browser. Both collection
buttons opened their lists and returned to the directory. Evidence:
`<evidence-dir>/ui-small-toolbar.png` and `ui-toolbar-recent-hit.png`.
When adjusting toolbar graphics, check their size against the directory icons and
confirm that reducing the graphic does not shrink the clickable area.

Star palette follow-up (2026-10-24): the filled and outlined stars use the directory
icon's yellow (`#EAE816`) and shading (`#DCBB07`), with a brighter yellow on hover.
The 36x12 sprite sheets use gray disabled frames.
Build and 726 tests passed. The sprite sheets were inspected in all three states;
the Forge development client confirmed the toolbar's normal and hover colors at
GUI scale 2. Evidence: `<evidence-dir>/ui-star-palette.png` and
`ui-star-palette-hover.png`. Compare the stars with the adjacent directory icons
and check filled, outlined, hovered and disabled states after palette changes.

Star symmetry follow-up (2026-10-24): the full-size filled/outlined sprites and the
native 8x8 toolbar shape were checked pixel by pixel against their horizontal
mirrors, including RGBA colors in disabled, normal and hover states. In the Forge
development client at GUI scale 2, the toolbar star occupied 16x16 screen pixels;
normal and hover screenshots also passed the pixel symmetry check. Keep tooltips
clear of the icon when checking screenshots. Build and 726 tests passed. Evidence:
`<evidence-dir>/ui-star-symmetric.png` and `ui-star-symmetric-hover.png`.

- Install the same Plus build on both sides. Mix near and remote targets, alternate
  items and directional states, and increase client placement/break limits to 64.
  Requests must retain their order, held-item identity and orientation even when
  the inventory moves before execution. Stale clicked/target block states must be
  rejected and synchronized back to the client. Test lower independent server
  quotas, range changes, disconnects and server fast breaking disabled at 0.
- Check native harvesting, actual tool damage, durability reserve, drops and
  protection cancellation. Survival must not create a missing requested item.
  Without server support, retain normal reach and mining speed. Server assistance
  corrects supported placement orientations; protection events still apply.
- M must put Shared Schematic Placements directly below Server Schematics.
  Chinese sharing actions must use the shared-placement terminology. Check the
  browser clock, favorite star, file badge and material-list favorite toggle.
- Click the path bar or magnifier, type a filter, and press Escape to clear it.
  Typing without a focused text field must also open search. Check text fields,
  modal dialogs and the focused 3D preview retain their own keyboard handling.
- Set Preview must show a centered square, including with F1 hiding the HUD.
  F2 must save the centered 120x120 image without the frame and dismiss the frame.
  Reopen the manager to verify the saved preview. Check portrait and landscape
  dimensions, cancellation and GUI scales separately when changing rendering.
- The work-radius slider must use integers; old decimal settings round and clamp.
  Fresh placement/break limits are 64, and pasteWithoutUpdates defaults to true.
  Existing saved values remain intact. Server-assisted placement offers Auto,
  Slabs only and None; obsolete v2/v3 settings migrate to Auto.

Observed 2026-10-20--21 on Minecraft 1.7.10, Forge 10.13.4.1614, MCP stable_12:
the build and all 726 UTF-8 tests passed. The separate
`tools/headless/printer_smoke` test mod ran in a real dedicated-server world with
a Forge fake player and native interaction handlers. One queue pass harvested 64
stone blocks, used 64 iron-pickaxe durability and produced 64 cobblestone drops.
A durability reserve stopped subsequent queued breaks. Interaction/BreakEvent
cancellation preserved blocks/tools, and survival bedrock stayed unbreakable.
Another pass placed 64 stairs in request order with the requested metadata and
consumed 64 items after their stack moved to another inventory slot. The player's
orientation was restored. Stale targets, mixed near/remote FIFO order, separate
per-tick quotas and disabled fast breaking passed. The fixture JAR is excluded
from production artifacts. This is native server execution, not client input or
a simulated multiplayer latency test; arbitrary mod blocks and protection plugins
were not revalidated in this round.

A registered fixture pickaxe also changed its NBT from native harvesting callbacks.
All 64 queued breaks completed with both 64 durability damage and 64 recorded NBT
uses; another pickaxe with different NBT stayed untouched. Queued tool identities
follow changes made by the server's preceding harvest. This fixture establishes
the NBT-update path, not compatibility with every powered mod tool. Double-slab
completion consumed two slab items; with one item it left a single slab. A separate
development server without the fixture mod reached Done and stopped cleanly.

Development-client mouse/keyboard checks used Chinese, GUI scale 2, an 854x480
window, a disposable creative superflat world and Xvfb/Mesa software rendering.
The shared-placement menu position/name, visible clock/star sprites, material
favorite toggle, path-click search, direct typing and Escape passed. The integer
radius slider moved from 66 to 67. Auto/Slabs only/None and the Plus-specific help
were inspected. Set Preview drew a centered square with and without F1; F2 wrote
14,400 preview pixels, and independent NBT readback found no white frame pixels
on the image edges. The manager displayed the saved preview. Evidence includes
`<evidence-dir>/ui-search.png`, `ui-material-star.png`, `ui-protocol-help.png`,
`ui-integer-range-next.png` and `ui-preview-frame-f1.png`.

The upstream comparison used Litematica 26.3 commit
`64e74592f631cfa5b084bedb0eda5dac8a3b3cb8` and MaLiLib 26.3 commit
`b043d04c21009a9ac1d19078e56c501d63d56ec8`. The inspected search widget toggles
through its icon and supports typing to search; activating it by clicking the path
bar is a Plus addition. The centered preview frame follows the upstream behavior.

## Issue report entry and introduction

- In GTNH 2.8.4 and 2.9.0-RC-2, enter a copied test world and press M. Check the
  20x20 report button at the top right, its 26.3 bug icon and localized tooltip.
  Open the configuration page and check the same entry above the tabs. Also open
  configuration from Forge's Mods screen without joining a world.
- In Chinese, require the exact maintainer-supplied introduction headed
  `HackerRouter 的话`; English must show the corresponding translation. Check GUI
  scales 1/2/Auto, Unicode font, a small window and resizing while the dialog is
  open. The title, checkbox and OK/Cancel buttons must remain readable. Wheel,
  scrollbar dragging and focused Up/Down/PageUp/PageDown/Home/End must expose the
  entire message without scrolling the controls or interacting with the menu
  behind the dialog.
- Align the report button's top with the version/title text and its right edge
  with the configuration list's scrollbar. At 640x480 and 1280x720, verify that
  the tabs remain below the button and the title never overlaps it.
- Check the shared confirmation footer: OK followed by Cancel at the bottom
  right, with Don't show again vertically centered on their row at the bottom
  left. If a translation cannot fit beside the buttons, the checkbox may use
  the row immediately above without overlapping them. Check the same right
  alignment in the actual blocks-only NBT paste prompt, which has no checkbox.
- In Chinese, read every paragraph at both window sizes: wrapping should use
  available width after embedded English words, keep `Pull Request` together,
  retain exactly one blank line between paragraphs, and show the complete text.
  Check English word boundaries, scrollbar access to the final line, and resize
  the open dialog at GUI scales 1/2/Auto.
- With `skip_issue_report_introduction` absent or false in
  `config/schematica_plus/UiState.json`, Cancel and Escape must close only the
  introduction, launch no browser and leave the preference unchanged, even if
  the checkbox was selected. OK without the checkbox must open
  `https://github.com/HackerRouter/Schematica-Plus/issues` and show the introduction
  again on the next report click.
- Select Don't show this message again and OK. Reopen the entry from both menus,
  then restart the client: each explicit report click should open the same URL
  directly. Merely opening M/configuration or starting the game must never launch
  a browser. Only the introduction preference is saved; no report, log or other
  player data is uploaded automatically.
- The default browser handles external links; tab/window behavior follows its
  settings. If browser opening fails, require a visible fallback dialog whose
  Copy link button places the exact URL on the clipboard. Cancel must dismiss it.
- Recheck the existing blocks-only NBT paste confirmation after this shared
  dialog change: no report checkbox, Cancel/Escape performs no paste, and OK
  keeps the existing consent flow. Long confirmation text must remain scrollable.

Cloud observations (2026-10-12): both native GTNH clients passed the Chinese and
English introduction views, scales 1/2/Auto, 640x480 and odd 1001x563 windows,
resizing an open dialog, wheel/keyboard/scrollbar navigation, checked Cancel and
Escape, confirmation and the preference surviving a full client restart. The
final build also opened the report entry from configuration without a world and
from the in-world menu, and Chromium showed new tabs at the correct Issues URL.
The environment could fetch the page HTML but not all GitHub stylesheets; this
does not establish Windows/macOS browser behavior or force a browser's tab policy.
The browser-failure Copy link path was checked in 2.8.4 with an intentionally
failing system launcher; the 2.9 native fallback could still open the browser.
In both packs, the actual blocks-only NBT prompt retained its existing controls:
Cancel/Escape left a synthetic destination empty, while OK
created an empty chest. Saved region files independently confirmed the result,
and the input schematic still contained seven diamonds. Build, Checkstyle and
603 tests passed. Evidence: `<evidence-dir>/issue-report`, especially
`screenshots/issue-*-final-full-zh.png`, `issue-browser-tabs.png`, final acceptance
logs and `nbt*.log`. Game checks used Xvfb/Mesa software rendering on copied test
worlds, not user saves or a physical GPU.

Layout follow-up (2026-10-12): the updated entry alignment and confirmation
footer were seen in native GTNH 2.8.4 and 2.9.0-RC-2. Both clients passed 13
introduction layouts covering Chinese/English, 640x480/1280x720, scales 1/2/Auto
and resizing an open dialog. Runtime measurements confirmed complete text and
exactly four paragraph gaps; screenshots showed the previously broken mixed
lines filled normally. Checkbox clicks, Cancel, Escape and scrolling remained
usable. Actual NBT prompts had right-aligned buttons; Cancel/Escape left fresh
destinations empty, and mouse confirmation produced empty chests while the
source schematic retained its seven diamonds. During the first 2.8.4 NBT run,
the X11 cursor and the client's cached cursor differed after opening the dialog.
Moving away and back before clicking synchronized them; the complete sequence
then passed at a new destination. Keep this input-driver detail separate from a
mod failure when repeating headless mouse tests. Build, Checkstyle and 609 tests
passed. Evidence: `<evidence-dir>/issue-report-layout`, including
`screenshots/layout-*-main-dialog-zh.png`, `layout-*-nbt-confirm.png`, acceptance
logs, `input-sync284.txt` and `nbt*.log`. These checks used copied synthetic
worlds and software rendering; browser/cold-start results above belong to the
preceding feature validation.

## Version support scope (2026-10-11)

Use the current Schematica Plus version for acceptance. Older Plus releases,
mixed-version connections and legacy-peer protocol compatibility are outside the
support and follow-up test scope. Historical results below do not extend it.

- Install the same current Plus JAR on client and server; verify their loaded
  versions, join, download a schematic and perform a remote paste with NBT.
- Separately test the current client against a server without Plus: verify the
  documented command permissions and blocks-only consent for client-only paste.
- Cover GTNH 2.8.4 and 2.9.0-RC-2 with their native dependency versions. This
  support-scope change concerns Plus releases, not the two target modpacks.

This is a documentation change; it records no new in-game test result.

## NEI recipe-button event dispatch

- In GTNH 2.8.4 and 2.9.0-RC-2, enter a world and open the crafting-table recipe
  before exporting a material list. The recipe page must render without an
  `IllegalAccessError` from `ASMEventHandler` / `NeiIntegration`.
- After NEI finishes loading its items, export a material list to its bookmark
  group. Reopen a recipe, hover the S button, and click it. The recipe must appear
  in the exported group. Check another recipe tab and return to the inventory.
- Restart and open recipes before exporting another group. A missing group must
  hide the S button without breaking NEI's own recipe buttons.

## Dedicated-server save queue and release gate

- `ServerSaveQueueLoadingTest` loads the shared save queue with Minecraft client,
  Plus client and LWJGL classes unavailable, including event-method reflection.
  It also constructs `MessageDownloadEnd` and reflects its methods under the same
  restrictions, covering packet registration on a dedicated server.
- Start `gradlew runServer` in a fresh test directory with the EULA accepted.
  Require the normal `Done (...)!` startup message and clean shutdown after `stop`.
  Initialization must not load EntityClientPlayerMP, WorldClient or Minecraft.
- On a dedicated server, run a server-side schematic save with a player connected.
  Use an ordinary filename without an icon prefix, then an unknown `mod:item;name`
  prefix: both must use the default grass icon without loading `WorldClient` or
  failing the command. A valid `minecraft:diamond;name` prefix keeps its item icon.
  Download and reload the resulting files to exercise the NBT icon fallback too.
  In singleplayer, save an area, clone it to memory and save a project version;
  client callbacks must still run on the client thread after capture completes.
- The tag release workflow runs the same build/tests/server smoke check before
  publishing. Keep that gate enabled and publish only the main runtime JAR.

2026-10-20: The download-message loading regression reproduced
`NoClassDefFoundError: EntityClientPlayerMP` before changing the file-link chat
recipient to `ICommandSender`. After the change, `build` and all 718 tests passed.
A fresh Forge 1614 development dedicated server on Java 8u504 reached `Done` and
exited successfully after `stop`. This check did not exercise client downloads or
native GTNH pack gameplay.

Run `gradlew build` for compilation, packaging, Checkstyle and unit tests. The tests
exercise bounded file/NBT inputs and pure transformation logic without starting
Minecraft. Forge world initialization requires LaunchWrapper and is not exercised
by the plain JUnit process.

## Shared production UI and removed test entry

- In both target packs, open M and Configuration > Hotkeys. Search `uiDemo`,
  `UI component test` and `UI 控件测试`; no registered action or launch button
  may appear. An old unknown JSON entry must not launch a page or break loading.
- Exercise the actual load/placement lists, configuration fields and report dialog
  in English/Chinese, GUI scales 1/2/3/Auto, Unicode font, resized/odd windows and
  a 320x240 scaled viewport. Text, clipping and hit targets must agree.
- Filter real file/placement rows, clear search, select rows, use keyboard navigation,
  double-click and drag scrollbars outside their bounds. Scroll clamps and a filtered
  row cannot activate. Release a held row after filtering without acting on another.
- Use Tab/Shift+Tab, selection and clipboard in configuration/rename fields. Test
  empty/minus/out-of-range numeric drafts, Enter, focus loss and spinner controls.
- Open a report or deletion confirmation: background controls must not react,
  Escape dismisses the dialog, and confirm/cancel have distinct effects. Resize
  and return to the parent without losing its drafts, selection or scroll position.
- Close the page and inspect world/NEI/inventory textures. No scissor, tint, blend,
  depth or keyboard-repeat state should leak.

Observed on 2026-10-12: both native clients (GTNH 2.8.4 / lwjgl3ify 2.1.16
and 2.9.0-RC-2 / lwjgl3ify 3.0.37) cold-started with the rebuilt main JAR.
The configuration screen and main menu were opened through the runtime inspection
helper without loading a world. A native mouse click selected Hotkeys in both;
each listed 80 actions. Runtime checks found no `uiDemo` registration and no
`UiDemoScreen` class. Searching the internal ID and English label yielded zero
rows. The normal hotkey page, empty search and main-menu captures were inspected.
This is menu acceptance, not a new world-edit or gameplay regression run. Build,
Checkstyle and 609 automated tests passed. Evidence label: `<evidence-dir>/docs-refresh`.

Headless UI tests cover input routing, focus/capture/modal isolation, clipping
math and list state/visible-row behavior. They do not validate native GL drawing,
font/IME behavior or modpack rendering integrations; those require the checks above.

Before a release, use a disposable 1.7.10 world to check:

- Open M, load a schematic through a nested folder, search by name, double-click
  a folder, refresh, and use the keyboard. Check `.schematic`, `.schemplus`, `.litematic`, `.nbt` and
  `.schem`, Unicode/spaced filenames, empty folders, and a corrupt schematic.
  Back must never load; a failed load must leave a useful message on the page.
- Load the same path twice: keep one source row, with a new placement only when
  Create Placement is checked. Create two placements, return through M and verify
  their independent transforms/session restoration. Check the direct load binding
  and retention of a customized M binding.
- Test the new menu/load/save pages at 320x240 scaled size, larger windows,
  Unicode font and English/Chinese. Long names, paths and messages have tooltips.
  Return from source/placement/material/config pages to the main menu.
- Open the area editor from M, edit corner coordinates, commit with Enter and
  move each point to the player. Open Save (Ctrl+Alt+S by default), enter a name,
  select a subfolder, return and resize: draft name, destination and format survive.
- Save a small selection in each output format; also paste an existing suffix in
  the name field and ensure it is not doubled. Save high block IDs while selecting
  `.schematic` and verify the automatic `.schemplus` name reported in chat.
- Save over an existing file: ordinary Save refuses without changing it; Shift+Save
  explicitly overwrites. Escape before submission must not write a file.
  Check invalid names, removed output folders, out-of-range Y, oversized selections,
  repeated saves during a pending job, and server-disallowed load/save actions.
  A queued message must not be mistaken for completion. After submission, change
  the NBT/entity toggles and confirm that the queued capture retains its options.

- Run the compatibility scenarios in both GTNH 2.8.4 and 2.9.0-RC-2. Use each pack's native versions. `compatibility/gtnh-mods.json` records the earlier
  2.8.4/RC-1 source investigation; it is not an RC-2 installation manifest or an
  in-game validation claim.
- Save/reload Forestry machines and apiaries, Railcraft tanks/signals and
  BuildCraft pipes with wires, facades and gates, in singleplayer and multiplayer.
  Compare connections, fluid levels, active textures and covers. Loading a file
  from a different protocol version must skip its opaque update and retain NBT.
- Save OpenComputers screens/holograms away from the origin; verify they rebuild
  at schematic coordinates. Save, move and resave a JABBA barrel, including an
  ender-linked barrel; its real-world BSpace registration must remain unchanged.

- Start the client and dedicated server without LunatriusCore; also start with it
  installed for another mod. Open the load, save, placement configuration and materials screens.
- Load a two-layer schematic, display one layer, rotate/mirror, then display all
  layers. Verify both layers and entity positions survive.
- Load two differently colored/oriented schematics; switching or unloading one
  must not change the other instance.
- Use two schematics wider/taller than 32 blocks. Switch repeatedly, then hide the
  selected instance and move the other one. Its chunks must continue rebuilding.
- From M, open Schematic Placements, select another placement and configure it.
  Coordinates, movement and rotation must affect that placement. Rotate a 17x33x49
  schematic around each axis and check the final partial chunks are present.
- Move one schematic away from matching world blocks across X/Z=15/16 and -1/0;
  its previously hidden blocks must appear without rebuilding unrelated instances.
- Walk around a large schematic across subchunk boundaries and view it beyond
  160 blocks with sufficient render distance. Enable Freecam, fly away from the
  player and rotate the view; blocks, entities and selection boxes must stay fixed.
- Apply Y then X rotations, reconnect, and compare the restored result. Repeat in
  another dimension and on a differently addressed server with the same display name.
- Load different blueprints in the Overworld and Nether, remove the last one in
  the Nether, then travel back and forth twice and reconnect. The removed instance
  must remain absent and the Overworld instance must keep its own position.
- Paste over a chest, paste luminous blocks and a door, save/reload the world, and
  check inventories, lighting and block behavior. Reject pastes crossing Y=0/256.
- Download a schematic containing inventories, an item frame and a mob. Verify
  that contents survive regardless of the local save-dialog options, and that a
  repeated chunk does not duplicate entities. Interrupt/reconnect during download.
- Start a queued save, change the save-dialog NBT/entity options, and verify the
  queued save uses its original options.
- Save adjacent mod blocks with different ID high bytes in both formats. Load
  them after changing the format preference and verify names and metadata. Import
  a WorldEdit schematic, including one with an odd block count. Open a saved
  `.schematic` with adjacent IDs above 255 in GTNH Schematica and check every block.
  Check old files made with either nibble order against their saved name mappings.
- With the extended-format preference off, save blocks with IDs above 4095 from
  the GUI and save command, and download them from a server. The output must use
  `.schemplus` and the completion message must show its actual filename. Repeat
  with an existing `.schemplus` of the same name: keep that file and use a numbered
  name. Selections containing only IDs up to 4095 must keep `.schematic`.
  Download a server file by its full name and by its base name with the opposite
  format preference selected. When both extensions exist, each list download link
  and removal confirmation must target the exact listed file. Include a name with spaces.
- With NBT saving enabled, save GregTech pipes/cables with bends and junctions,
  plus ProjectRed full lamps and multipart lights with microblocks. Compare local
  and remote-server saves, then reload the files and check connections, colors,
  powered/inverted state and parts. Client saves cannot recover unsynchronized
  inventories or machine data; a server save is required for those.
- Save an active GT machine and another mod's tile using a standard S35 update,
  with NBT enabled. Compare facing, active texture, color, covers and connections
  in singleplayer and multiplayer. Rotate, save again and reload; verify the
  renderer's update data survives and uses schematic coordinates. Paste the
  singleplayer copy and verify original inventories/energy were not replaced by
  the smaller client update. Loading a preview must not start machine sounds.
- Check animated textures and a TESR driven by world time while moving, rotating,
  pausing and using Freecam. Check multiple instances and nearby real machines.
  Preview world time and frame interpolation now advance; arbitrary tile logic
  is deliberately not ticked, so animations driven by private tick counters need
  a mod-specific adapter. Private packets, absolute positions embedded in opaque
  payloads, neighbors outside the selection and biome-dependent rendering still
  need separate compatibility work; this is not a universal visual snapshot.
- Inspect an empty and filled cauldron from all sides with alpha enabled/disabled,
  alongside mod blocks that use both render passes. Check multipart glows and
  ensure rendering them does not change nearby blocks, entities or the HUD.
- In GTNH 2.8.4 and 2.9.0-RC-2, capture isolated normal/trapped/ender chests, a
  double chest and IronChest iron/gold/diamond/crystal/obsidian variants with their
  native tile NBT. Move the preview into air and inspect the front, back, sides,
  top and bottom, especially the strip just below the lid. Compare with the real
  source chests: no gap, rear latch or internal face should cover the exterior.
  Repeat with overlays on/off and schematic alpha on/off. The 2.8.4 regression
  showed triangular holes under closed lids when the overlay's disabled depth
  writes leaked into the opaque tile pass. Put 64 diamonds in a crystal chest and
  capture it again: both the transparent frame and the rotating contents must show.
  Crystal chest models must retain transparent depth handling. Watch a complete
  item rotation cycle and compare with the real crystal chest; record intermittent
  disappearance separately from missing chest faces. Check that other pass-1
  renderers still work, and real blocks in front still occlude the preview.
  Rendering a preview must not change the source chest's lid or contents.
- After chest depth changes, compare real blocks with their moved previews in
  GTNH 2.8.4 and 2.9.0-RC-2. Include a beacon, enchanting table, skull, sign,
  brewing stand, hopper, filled cauldron and anvil; Baby Chest, EnderStorage,
  AE2 sky chests, Galacticraft treasure/parachests, hungry/bound chests and Forestry
  chests; BuildCraft/EnderIO/EnderStorage tanks, Thaumcraft jars/brain jars,
  Botania pools/pylons, Forestry/BuildCraft engines, JABBA and StorageDrawers.
  Compare opposite views, not just a single screenshot. Also check glass/panes,
  stained glass, slabs/stairs, fences/bars, GT machines/pipes, ArchitectureCraft
  shapes and ProjectRed multipart wires. Default empty/inactive fixtures do not
  validate every active machine, orientation, upgrade or animation.
- Specifically capture all three TConstruct `LavaTank` metadata variants, empty
  and half filled through native fluid insertion, and GT tinted industrial glass
  (`gregtech:gt.blocktintedglass`). On old Angelica, changing only Forge's static
  pass leaves `getWorldRenderPass()` stuck at zero: tank shells and industrial
  glass disappear from the preview. Both passes must reach the renderer, and
  entering/exiting the schematic pass must restore the previous Forge and legacy
  thread-local values independently. Check overlays and alpha on/off, then inspect
  real terrain and containers again. Repeat on 2.9 where the legacy classes are
  absent; no optional renderer classes may become a hard dependency.
- Print water/lava and a registered mod fluid bucket in survival and creative,
  with the bucket in the hotbar and main inventory. Check consumption/empty bucket,
  restored view/slot/sneaking, source-versus-flow behavior, reach, occlusion, and
  server-denied placement. Verify a different fluid or solid block is not replaced.
  Printing water in the Nether must not waste buckets on repeated evaporation.
- Print with a GregTech volumetric flask and IC2 universal fluid cells, including
  stacked cells and a flask holding more than 1000 mB. Verify each source costs
  1000 mB and the remaining contents survive. Try a partly filled flask below
  1000 mB, a nearby tank, an obstructing source block and denied server interaction.
  Test Forestry buckets separately; Forestry cans/capsules and drink containers
  must not be selected. Forestry non-lava buckets must be skipped in the Nether.

Session keys now use save-folder/server-address plus dimension. Legacy entries
keyed only by display name remain in the JSON files but are not automatically
assigned to a world, because that assignment is ambiguous. Reload/place schematics
once to establish the new keys. Ordered transforms apply to newly saved sessions.
# GTNH client-only visual state

- Thaumic Exploration: save differently colored bound chests/jars on a remote server. Their seals and chest lids must survive without changing the linked real storage's color or contents.
- Binnie: reopen multicolored/mature/wilted flowers and running analyser/splicer machines. Check flower type, all three colors, section and visible machine items.
- IronChest: reopen crystal chests with several visible item types and open lids. Stored inventory and displayed top stacks must remain separate.
- MalisisDoors: save an opened door and a door mid-animation; reopening must preserve state and allow the visual transition to finish, without playing save/load sounds or operating neighboring real doors.
- Steve's Addons: save RF nodes with different input/output sides; their side indicators must survive reopening.

- LogisticsPipes: reopen routed pipes with power indicators, mixed BuildCraft connections and pluggables. Check the old and new pipe renderers.
- EnderStorage/Translocators: reopen filled ender tanks, their redstone-controlled valves, open ender chests and upgraded translocator attachments. Visual interpolation must continue without changing storage contents or sending placement/transfer packets.

- GT: check formed turbine/air-filter overlays and custom machine indicators, including single-player server capture and multiplayer client capture.
- IC2 2.2.828: save planted crop sticks at different growth stages, including crops from GTNH addons. Reopen, move and rotate the preview across chunk boundaries; crops must use their plant textures rather than missing textures, and pasting must retain the crop and growth stage. Test empty and crossbreeding sticks too.
- GT pipes: rotate bends/junctions through all axes, mirror twice and resave. Connection arms, covers, per-side redstone and blocked fluid inputs must follow the pipe. Four turns must recover the original appearance. Other mods' custom orientation encodings still need individual checks.
- EnderIO conduits: rotate/mirror mixed bundles with internal and machine connections. Verify input/output modes, filters/upgrades, channel colors, redstone settings and fluid round-robin sides. Resave/reload and paste; directions must match the preview, with stored energy and filter contents retained. Include empty side maps and disabled faces.
- Tool settings: `pasteWithoutUpdates` defaults off and `pasteReplaceBehavior` defaults to None (air-only). In that mode, paste across air, stone, water, plants and filled chests; only air may change, including during multi-tick pastes. In command mode, commands must use `keep` so the server also checks the target.
- Mod identity: verify `schematica_plus`, the Schematica Plus name and HackerRouter attribution in the mod list. Install original or GTNH Schematica alongside Plus, in either jar order: the client must show the incompatibility screen and the dedicated server must stop with a clear message. Replace the old jar and check that existing Plus settings are retained and config labels/textures still work.
- Mod details: select Schematica Plus in the Forge mod list. Credits must honor line breaks and wrap with the panel width, with Authors, URL and description positioned below them. Check different GUI scales, switch to other mods, and return from Config/Done without losing the expected navigation.
- With block updates disabled in singleplayer, paste floating sand/gravel, redstone, fluids, machines and containers across chunk/section boundaries. Placement must not call block added/broken or neighbor notification hooks; lighting and client display must update and the world must reload correctly. Normal ticks, existing scheduled ticks and mod tile lifecycle hooks remain active. Cancel a large paste and check its completed portion. Command mode must reject this option before sending edits, since vanilla 1.7.10 commands cannot suppress updates.
- BuildCraft: save filled fluid pipes, powered kinesis pipes, gates and facades. Verify the pipe contents and per-side power display after reopening.
- Multipart: test ProjectRed lamps, framed/unframed wires, gates, microblocks and AE2 parts sharing a multipart tile. Check part count, lamp state, covers and connections after reopening. Repeat with a different mod version to check binary payload rejection.
- Printer: place Railcraft fluids using its own bucket alongside GT volumetric flasks, IC2 universal cells, Forestry buckets, EnderIO buckets and Tinkers' Construct buckets. Confirm survival consumes fluid and returns the native empty container.

- Test GTNH 2.8.4 and 2.9.0-RC-2 separately: save powered AE2 smart/dense cables, terminals, storage/crafting monitors, drives and AE2 Fluid Crafting parts. Check connections, channel stripes, colors, indicators and displayed stacks after reopening. The original live network must keep its state after saving.
- Include an AE2 monitor away from local origin (0,0,0): its dynamic tile replacement must preserve its coordinates and saved NBT without interrupting loading of other tiles.
- Save Galacticraft colored pipes, filled machines, solar panels and linked beam receivers/telepads. Check synchronized fields and rebased link coordinates after loading at another position.
- Load an AE2/GT stream captured with a different mod version: incompatible binary state must be skipped while canonical NBT remains available. Named visual fields are separate from version-specific streams.


## UI: main menu and file loading

- Compare M with the supplied Litematica at the same scaled resolution, in English
  and Chinese. Check the two button groups, icon states, title, bottom tool selector
  and disabled-entry tooltips. Test GUI scales 1/2/3 and resizing while a child is open.
  Vanilla button skins/font glyphs are expected to follow the 1.7.10 resource pack.
- Open both Loaded Schematics and Schematic Placements, including when empty.
  With many instances, search, drag the scrollbar, toggle a row and remove first,
  middle, last and active instances. Clicking a row button must not select a different
  row. Filter or scroll while holding a button; releasing must not act on an old row.
  Verify both lists reflect the same current backend and session reload retains changes.
- Check the load browser's root/up/search icons, 14-pixel file rows, side panel,
  bottom checkbox and action order. Search, Enter/double-click, F5 refresh and
  Backspace navigation must work. Check every supported extension and file deletion
  between selection and Load. Check metadata, thumbnail and 3D preview refresh
  without publishing stale data from the previous selection.
- In Area Editor, edit A/B coordinates, use the +/- icons and Move to player, then
  Save Schematic. The save page must use a file browser rather than coordinate fields.
  Select a file to fill its name, type a new name, navigate directories, open Options,
  switch format/NBT, resize and return; the draft and destination must survive.
- Confirm saving with Ignore entities on/off, disabled selection, invalid bounds,
  illegal paths/names, a full queue and explicit Shift overwrite. Check each output
  format; .schematic automatically upgrades where required and .schem is import-only.
- Check the explicit Subregion Slice placeholder explains its limitation. Open
  placement configuration and Forge Config through their actual navigation paths.


## UI: placement configuration

- In M > Schematic Placements > Configure, compare English/Chinese against the
  supplied Litematica at the same scaled resolution, especially heights below and
  above 328. Check the rename field, right-side origin controls, +/- icons, lock
  checkboxes, named independent region rows, magnifier and the two footer arrangements.
- Configure inactive placement B while A is selected/printing. Rename, move,
  transform, toggle visibility/entities and open Materials. Only B may change;
  its material list must describe B and A must stay selected. Reopen/relog: B's
  display name and transform must persist, with its source filename unchanged.
- Type a new name, resize, visit Materials and return: retain the unsubmitted
  draft. Empty rename must show feedback. Load an old session without displayName;
  its filename-based name must remain intact.
- Use an asymmetric 2x3x5 test schematic at positive and negative coordinates.
  Record the origin shown in the UI. Cycle rotation four times, reverse once,
  cycle mirrors both directions, and alternate rotation/mirror. The original
  corner must stay at that origin. Four turns or three mirror cycles must recover
  the original result. Include directional blocks, entities, GT pipes and EnderIO
  conduits; save/reload the session and paste to compare NBT directions.
- Move to player while facing each direction. The origin must equal the player's
  feet block, without the old facing-dependent placement offset. Test coordinate
  typing, arrows/wheel, left/right nudges, Shift (8), Alt (4) and both (32), including
  coordinate limits. Invalid proposed minima must leave the placement unchanged.
- Load a disposable session fixture with an X/Z tilt or vertical mirror; the
  placement page must show CUSTOM instead of incorrect planar labels. Verify
  retained non-planar geometry after reopening and session restoration.
- While the page has a pending coordinate edit, unload the instance or change
  dimension/disconnect. Releasing a held button or closing the screen must not
  modify an instance belonging to the previous world. Navigation must still work.
- Configure independent regions and coordinate locks. Toggle rendering, enabled
  state and enclosing boxes separately, and open the verifier. Disabled controls
  must not mutate state; check the dedicated subregion scenarios below.


## UI: configuration

- Open Config from the Forge mod list with no world loaded, and M > Configuration
  in a world. Done/Escape must return to the correct parent. Compare English and
  Chinese tabs, search positions, rows, reset buttons and numeric-toggle icons with
  the supplied upstream. Resize at GUI scales 1/2/3; narrow layouts must wrap tabs
  without hiding reachable controls. Exercise advanced key contexts/triggers and
  matching settings, plus Render Layers and Colors.
- Edit a boolean, printer delay, alpha, directory and tool item. Change tabs, search,
  scroll offscreen and resize while editing; drafts must survive. Done must persist
  changes across restarting the game. Render changes must rebuild previews. Local
  server options must not imply that remote server permissions have changed.
- Try empty/minus numeric drafts, letters, NaN, infinity and out-of-range values.
  Invalid fields must be red, with a warning count; close must keep those properties'
  previous applied values while saving other valid edits. Reset must repair invalid
  input. Check numeric reset, text/slider toggling, mouse drag outside the slider,
  left/right arrows, and each of the nine printer-slot defaults after several edits.
- Search English/internal names, Chinese labels, categories and modified. Toggle the
  magnifier and type outside an editor to open search. Escape hides search first;
  Shift+Escape closes the page. Hold a row control while filtering/scrolling: release
  must not edit a removed row. Closing without changes must not rewrite settings.
- Edit extraAirBlocks from empty and populated lists: insert, type, reset, move,
  delete first/middle/last, scroll and resize. Escape closes only the dialog. Close
  Config, restart and confirm order/values. Reset the entire list and confirm empty
  brackets and defaults. Clipboard paste and selection must work inside fields.
- In Hotkeys, bind a normal key, Tab and a mouse button; Escape during capture must
  unbind without closing. Try a conflicting vanilla/mod key, then reset. Click and
  keyboard activation must both begin capture without leaking the key into search.
  Saved keys must work after reopening the world/game. Exercise All's key filter,
  clear it with Escape, and scroll a capturing row out of view to cancel capture.


## UI: placement materials

- Open Materials from placement A's configuration while B is the active printer
  placement. All counts must describe A; neither the active placement nor the
  printer may switch. Repeat through the main-menu material entry. Escape returns to the parent;
  Main Menu opens M. Empty/unloaded placements must not crash the page.
- Compare English/Chinese with the supplied upstream at wide and narrow scaled
  resolutions: top actions, multiplier, info icon, four sortable headers, alternating
  rows, icons, Ignore and footer. Check GUI scales 1/2/3, long mod names and 16-digit
  counts. Columns/actions must not overlap. Hover must show full counts and actual
  stack limits (1, 16, 64); resource-pack fonts and mod item icons remain native.
- Use one completed block, one air position, one incorrect real-world block and
  one unloaded chunk. Totals/missing/available colors and progress must distinguish
  these cases; unverified positions must not appear completed. Move/rotate during a
  large scan, then unload or change dimension: restart/cancel without exporting a
  mixed or incomplete snapshot. Verify the UI remains responsive during scans.
- Compare two items with identical IDs/metadata but different NBT, including GTNH
  mod variants. Inventory and list entries must remain separate. Do not treat block
  ID/metadata progress as proof of correct machine NBT. Review skipped blocks and
  generic pick-block mapping limits in the info tooltip.
- Click every header twice, search by translated name and registry ID, ignore a
  row, refresh, then Clear ignored. Toggle Hide available, change inventory while
  the page remains open, and confirm filtering updates. Reopen to check saved sort
  order, ignored rows and multiplier for that placement. Search remains page state.
  Test the magnifier,
  ordinary typing to search, Escape to hide it, and Shift+Escape to close.
- Change multiplier to 2, back to 1, and Integer.MAX_VALUE. Counts must not overflow;
  larger multipliers must require full copies rather than crediting the current
  placement repeatedly. Test text edits, Enter, focus loss, resize and arrows/wheel.
- Compare All with Render layers, both with the placement layer filter enabled and
  disabled. Counting scope must not change preview or printer settings.
- Export TXT, Shift+CSV and Alt+JSON with filters/ignored rows active. Verify displayed
  order/counts, Unicode, CSV quotes/commas and JSON variant NBT. Repeated exports must
  create new UTF-8 files under dumps and retain old files. Test an unwritable directory
  and confirm error feedback. Export stays disabled during scanning or after world
  invalidation. Exercise HUD/cache/Raw Materials using their dedicated checks below.
- Render enchanted items, 3-D blocks and custom GTNH item renderers, then open other
  UI pages and return to the world. Check lighting/depth/scissor state, tooltip
  layering and clipped rows while scrolling. This requires native game testing.


## render layers (native checks)

- Open M > Configuration > Render Layers. Compare English/Chinese with the supplied
  Litematica/MaLiLib editor: six tabs without All, mode/axis at (10,60), coordinate
  fields at y=86, maximum first, minimum 23 pixels below, plus/minus icons, Hotkey
  checkboxes and Set Here. Test GUI scales 1/2/3 and narrow windows; tabs must wrap
  without overlapping the editor. Change a config draft before switching tabs;
  preserve it, and return through Escape to the correct parent.
- Test all five modes on all three axes, negative X/Z, inclusive endpoints, equal
  endpoints, a range entirely outside the schematic and an empty intersection with
  a legacy local Y layer. Left/right mode/axis cycling must reverse. Edit fields,
  use Enter/focus loss, resize, and try integer limits. Test plus/minus, Shift, Ctrl
  and Shift+Ctrl. Set Here must use the active camera base, including Freecam, and
  match the expected world Y rather than the player's 1.7.10 eye-height offset.
- Assign the existing next/previous layer bindings. Neither range checkbox chooses
  the nearest endpoint; one chooses that endpoint; both move the entire interval.
  At integer limits preserve range width. With global All, legacy local-Y hotkeys
  must still work. Disable the legacy local filter when testing global ranges alone.
- Load multiple placements at different heights. Move, rotate and switch the active
  placement while slicing X/Y/Z: each preview must stay clipped in world coordinates,
  with bounded rebuilds completing. Check opaque cube cut faces, cauldrons, GT pipes,
  Ender IO conduits, animated TESRs and entities. Hidden neighbors must remain
  available to connection logic. Test picking through hidden blocks onto visible
  layers. TESR/entity visibility uses the anchor; their geometry can extend beyond
  a slice. Custom mod renderers may require further cut-face adapters.
- Compare material All and Render Layers with both global and legacy filters.
  All must count the entire placement. Move/change layers during a scan and check
  that refresh/export never publishes mixed results. The printer must place/check
  only the visible range. Save/rotate/full Paste must still contain all blocks;
  the UI tooltip explicitly distinguishes full Paste from printer layer filtering.
- Set different ranges in overworld/nether, disconnect, reload, and visit another
  server/save. Check RenderLayers.json isolation and restoration, including an All
  mode reset. With a backed-up deliberately malformed settings file, saving must
  log a failure and preserve it; restoration must fall back to All. Restore the
  backup after testing.


## colors and HSV editor (native checks)

- Open M > Configuration > Colors and the Forge mod-list Config entry. Compare
  English/Chinese list order, 100-pixel option area, 18x18 indicators and Reset
  positions with upstream. Search in Colors/All; scroll and resize with a color
  draft in progress. Supported color rows must edit/reset and update their
  corresponding overlays, including REBUILD targeting colors.
- Click a supported swatch. Compare the centered 300x180 dialog, square, vertical
  hue strip, seven component rows, hex field, current-color preview and markers.
  Test GUI scales 1/2/3 and supported resource packs. Mouse capture must clamp at
  edges and stop on release; keyboard Tab/arrows must stay inside the dialog.
  Escape closes the dialog, a second Escape leaves Configuration and saves.
- Edit H/S/V and R/G/B, alpha 0/1/128/255, black and grayscale. HSV hue must survive
  setting V=0, editing alpha and restoring brightness. Verify vertical hue runs in
  the upstream direction, square left is black, top-right is the selected saturated
  hue and bottom-right is white. Swatches/previews are intentionally opaque like
  upstream; use the A bar/value to inspect opacity.
- Test #AARRGGBB, #RRGGBB, lowercase, 0x-prefixed input, empty and partial values,
  invalid letters and overlong values. Invalid drafts must not replace a valid
  color. Check Enter/focus loss on numeric components, Reset and reopening. A bad
  picker hex draft retains its last valid result when closed. A bad outer-row draft
  stays invalid and is not saved. Test a backed-up config containing an invalid
  color; runtime uses its default and the row allows repair without erasing the
  invalid persisted text automatically.
- Place missing, extra, wrong-ID and wrong-metadata blocks inside a schematic.
  Give each category an unmistakable color and alpha, close Configuration, and
  verify faces/lines after bounded display-list rebuilds. Metadata matching does
  not verify tile NBT. Test highlight, highlightAir, drawQuads and drawLines switches.
  Change areaSelectionBoxSideColor and check selection faces; edges/point colors
  remain as before. Defaults must match the documented Litematica palette.
- Open item/material screens after closing the picker, then return to the world.
  Check blend, lighting, alpha test, shade model and clipping for leaked GL state.


## schematic manager and file dialogs (native checks)

- Use disposable copies in a test schematics folder. Open M > Schematic Manager;
  compare the (10,24) browser, footer order, labels and source icons in English and
  Chinese. Select each of .schematic/.schemplus/.litematic and a directory. Check
  GUI scales 1/2/3, narrow windows and resize: groups wrap without covering Main
  Menu. Exercise metadata editing, import and export with their supported formats;
  Sponge .schem must not become an export choice.
- Cycle the File selector with left/right clicks. Rename/copy each format; verify
  file hashes and retained extension, then load the result. Existing names and
  attempts to change extensions must fail, including Shift-click. Errors keep
  the input editable. Test Chinese names, spaces, invalid paths/reserved names,
  Enter, Reset, Cancel, Escape and Tab focus. Load > Rename File must work too.
- Open Create Directory from loading, saving and directory selection. Create a
  nested folder by navigating one level at a time. F5, Backspace, wheel and clicks
  outside an open modal must not change the underlying directory or selection.
- Load multiple instances, including custom names and transforms. Save sessions
  in two dimensions, then rename their source file through the manager. Current
  previews and positions must stay intact; revisit both dimensions and reconnect
  to verify source references. Other source files/sessions must remain untouched.
- With a backed-up malformed LoadedSchematics.json, rename must fail while both
  the original source and JSON remain intact. Restore the backup after the check.
  Test read-only files/folders and a selected file edited/deleted externally while
  its dialog is open. Errors must be visible and must not affect a different file.
- Delete only disposable copies. Confirm the green OK/red Cancel positions and
  default focus on Cancel. Cancel/Escape must leave the file intact; confirmation
  removes only that file. Already loaded previews remain until unloaded, but a
  deleted source cannot restore after leaving the world. Check explicit Shift+Save
  overwrite separately; it does not use a confirmation dialog.

## loaded source / placement backend (native checks)

- Back up LoadedSchematics.json and use test schematic copies. Uncheck Create
  Placement on Load. The source appears in Loaded Schematics, no placement or
  preview appears, and an unrelated active placement stays selected. Repeated
  loading of the same path must keep one source row. Checked loading adds a new
  placement. Create Placement on the Loaded row adds one at the player.
- Create two placements of a GT/Ender IO pipe schematic with tile NBT and entities.
  Move, rotate, mirror and hide one. The other and a newly created third placement
  must retain the original data. Remove every placement: the source row remains.
  Unload that source: all its placements disappear, and other sources stay loaded.
- Give two placements different names, origins, transforms, render/entity/NBT
  flags and local layers. Replace their disk source with a valid different-size
  test schematic, then Reload. Origins, transforms, order and active selection
  must survive; local layers clamp to the new height. Check renderer buffers and
  printer selection. Reloading an unrelated source must not reset the printer.
- Replace only a disposable source file with invalid/truncated NBT and Reload.
  The source cache and all existing previews must survive the failed read. Also
  check a new source whose entities cannot accept a previous three-axis rotation:
  failure must retain all old placements, not publish a partially rebuilt group.
- Delete a loaded test source outside the game. Create Placement and Save to File
  must still work from memory. Compare saved bytes/hashes with the backed-up
  original for .schematic/.schemplus/.litematic, including raw visual-state NBT and
  long arrays. It saves source data, not a placement's rotations or local edits.
  Changing the extension must fail; overwriting requires Shift+Save. Cancelling
  or switching world while the save dialog is open must not write a stale source.
- Keep both a source with no placements and one with multiple placements. Visit
  two dimensions, reconnect and reopen the lists. Restore old session files too.
  Source-only records must not create previews; missing files and invalid entries
  must not block unrelated entries. Deselect every placement before disconnecting
  and verify it stays deselected. Unload a source, leave and return: it must stay
  unloaded in that dimension. Check removal from the current source/placement lists.
- Rename both a placed and an unplaced source through Schematic Manager. Verify
  current library paths, Reload, future Create Placement, and session restoration.
  Saving a copy must not change the source path. Restore backups after testing.
## upstream translations (native checks)

- Switch between English, Simplified/Traditional Chinese, French, Japanese,
  Turkish and Literary Chinese in Minecraft's language menu. Reopen the main
  menu, placement/source lists, browser, area editor and configuration. Upstream
  buttons and tool-mode names should use that language; custom Plus messages
  and absent translations may use English. Check long labels at GUI scales 1–3.
- Hover plus/minus controls and color settings. Original multiline descriptions
  should break into lines and preserve colors instead of displaying backslash-n.
  Check directory up/root/create and string-list add/remove/move tooltips too.
- Override an upstream key in a resource pack at
  assets/schematica_plus_litematica/lang/<locale>.lang and reload resources. Both
  buttons and tooltips should use the override. Escape newlines as backslash-n
  and literal backslashes as doubled backslashes for upstream UI templates.
- Check that legacy Schematica screens, key bindings and command messages still
  translate. Adding the upstream catalogs must not enable unavailable features.

## area selection library (native checks)

- Back up AreaSelection.json. Enter a world with an old-format saved selection:
  its corners and outline must be unchanged. Open Area Selection browser, create
  two selections, rename each and give their boxes different names. Change their
  corners using the editor and tool. Switch entries and reconnect; all names,
  corners, visibility and the active entry must remain independent.
- Copy an area, move one corner of the copy and toggle its outline. The original
  must not change. Search by selection or box name. Rename to an existing name
  (including different letter case) and try an empty name: the operation must
  fail without changing either entry. Remove only disposable entries: the area
  manager's red minus removes immediately, without a confirmation dialog.
- Unselect the active entry, reconnect and verify no outline reappears. Saving
  and area-based fill/delete/replace must refuse to run with no selected area.
  Delete the active entry and then the last entry; returning to this dimension
  must not recreate either. Other dimensions and servers keep their own lists.
- Move/rotate a placement and use From Placement. Preserve its enabled named
  regions, including the last block on each axis, and exclude gaps between them.
  Export through the save screen and inspect the result. Out-of-world bounds must
  be rejected without clipping silently or creating a partial selection.
- Enter a coordinate and press Enter, click another field, resize the window,
  and return from the save screen. Values must not reset or leak into another
  selection. World/dimension changes while a browser, editor, name dialog or
  save dialog is open must disable actions from the old session.
- On disposable settings, test an unreadable file and an external edit to the
  current session. The mod must retain disk data and show a persistence failure;
  reconnect to load the external edit. Restore backups after testing.


## multiple subregions (native checks)

- Back up AreaSelection.json and open a legacy or version=2 selection. In Area
  Editor, verify its first subregion retains both corners and its box name.
  Create two separated boxes and an overlapping box. Rename, select/unselect,
  remove, copy the whole area, and reconnect across two dimensions. Copies must
  stay independent; deselected/deleted boxes must not reappear. Delete all boxes
  and verify save/edit is refused until another is created.
- Check Normal and subregion editors in English/Chinese and at GUI scales 1-3.
  Compare button order, coordinates, list selection, tooltips and plus/minus icons
  against the reference UI. Check short windows and long names. Coordinate typing,
  nudging and Move to player must affect the configured box only. A stale editor
  after a world change must not change the new session.
- Display all boxes and switch the selected row. Red/blue corners must follow
  only that box, without a phantom box at 0,0,0 when no box is selected. Left/right
  tool clicks update only the selected box. With no selected box they refuse;
  saving the union of existing boxes remains possible.
- Place distinctive blocks, tile entities and entities in the selected boxes and
  in the gap. Save with NBT/entities enabled in singleplayer and on a server.
  Load the file: selected contents remain, gap contents are absent, and overlaps
  have no duplicate entities. Request .schematic and confirm a multi-box save
  produces a new .schemplus file without overwriting an existing alternate file.
  A normal one-box low-ID .schematic save/read must continue to work.
- Rotate/mirror the loaded preview around each axis. Check bounds, selected
  contents, gap highlights and the result of From Placement. Repeat with a
  .litematic containing disjoint regions and negative sizes. From Placement must
  reject out-of-world coordinates without leaving a partial new selection.
- In a disposable creative world, place blocks in the gaps. Fill, replace,
  delete and paste: no gap block may change. Repeat the printer test with creative
  destruction enabled, and test multiplayer command edits on a disposable server.
  Overlapping selected positions must only be processed once. Test cancellation,
  single-block boxes, boxes crossing chunks, and saving with no active box.
- Try two boxes whose enclosing bounds exceed the existing allocation limits;
  save/edit must fail before queuing work.


## manual origins (native checks)

- Enable Manual Origin in Normal Area Editor. It starts at your feet. Edit XYZ,
  nudge, move it to the player, and select its checkbox. Both tool clicks should
  now move only the origin. Select/configure another subregion and verify tool
  clicks edit that box again. Watch the orange/cyan origin marker and HUD.
- Put the origin outside two separated boxes; save, reload and paste. The file
  must keep its small enclosing box and gaps, with no extra volume out to the
  origin. Check that entering a placement origin aligns the designated original
  reference point. Test .schematic upgrade to .schemplus for a nonzero offset,
  and ordinary legacy files with zero offsets.
- Toggle Manual Origin off, reconnect, copy a selection, delete a selected box
  and switch dimensions. Automatic origins must follow the remaining minimum;
  manual origins and their selection state must remain independent. An empty
  area with a manual origin must still refuse saving or area edits.
- Rotate/mirror around X/Y/Z from the legacy controls and around Y from the
  Litematica placement editor. The world origin must stay fixed. Four rotations
  or two matching mirrors should restore contents and position. Include pipes,
  tile entities and entities; test negative offsets and non-square dimensions.
- Import a .litematic with regions on both sides of its origin, then use From
  Placement after rotation. Region bounds and origin should match the preview.
  Check invalid coordinates reject the whole import without creating an area.
- Reconnect with rotated legacy LoadedSchematics.json entries and restore old
  Coordinates.json bookmarks. Their minimum coordinates must remain unchanged.
  Save new sessions, change a source's origin/dimensions, and use Reload or
  reconnect: new records keep the world origin. Back up source/settings files.
- Check origin controls at English/Chinese GUI scales 1-3, input focus, resize,
  returning from another screen and changing worlds while the editor is open.
  Hidden/disabled controls must not modify state.
- Use /schematic download with the same current Plus version on server and client
  for a file with two separated regions and an outside origin. Reload it and check
  gaps, origin and rotation. Repeat a plain flat file with the same current builds;
  it should still download without acquiring independent-region metadata.

## independent placement subregions (native checks)

- Load a two-region schematic with an outside origin. Verify the placement list
  has both names, search works, and Configure opens each region's own state.
  Check English/Chinese at GUI scales 1-3, resize, keyboard activation, click
  sounds and returning from the subregion page. Main screen state must refresh.
- Move one region by XYZ, nudge and Move to player; rotate and mirror it. Its
  original region anchor stays at the entered position, and the other
  region stays unchanged. Test four turns, double mirrors, reset and reload.
  Repeat under global Y and legacy X/Z rotations and mirrors.
- Select a row, close the GUI and use Move. Only that region should move; its
  name/origin appears in the HUD and its outline is cyan. Deselect the row to
  move the whole placement. Lock coordinates and verify locked relative axes
  are preserved, including under global rotation.
- Toggle enabled off/on, All off/on and Reset. Disabled contents must be absent
  from preview, materials, paste and printer (including creative destruction).
  An all-disabled placement must refuse Paste and From Placement. Re-enabling
  must recover its original data. Rendering off alone hides preview/printing
  but keeps enabled contents in material counts and paste.
- Include GT/Ender IO pipes, tile entities with distinct NBT and animated visuals,
  entities and hanging entities. Verify previews and pasted orientations after
  local/global transforms. Ignore entities must only affect that region. X/Z
  transformations unsupported for hanging entities must fail without losing data.
- Move regions into overlapping positions and apart. Later source regions win
  blocks including air; overwritten tile entities must disappear, then return
  correctly when moved apart. An entity in overlapping source boxes occurs only
  once and belongs to the first source region. Original files remain unchanged.
- Reconnect and switch dimensions with transforms, flags and selection saved.
  Reload a changed source: matching names keep overrides, new names use defaults,
  removed names clear selection. Other placements of the same source are independent.
- Attempt positions whose combined dimensions exceed allocation limits. The
  rejected geometry must leave the previous placement usable. See the Litematic
  source-preservation checks for signed pivots and overlapping region payloads.


## Litematic source preservation and file icons (native checks)

- Browse .litematic, .schematic and .schemplus files (including uppercase suffixes).
  Check L, S and the separate schemplus.png icon in browsers, loaded lists and
  placement rows at GUI scales 1-3. Replace the 12x12 RGBA schemplus.png, rebuild
  or reload resources, and confirm only the Schemplus icon changes.
- Import a .litematic with overlapping named regions containing different blocks,
  chest/sign NBT and air at the same positions. Sorted later names win the initial
  preview. Disable or move the winning region and check the earlier original data
  returns. Move both apart, rotate/mirror, reset, reload and reconnect. Repeated
  edits and multiple placements must not contaminate each other's source data.
- Use regions whose Size is negative on X, Y and Z, separately and together, with
  a nonzero Position and a global origin outside the enclosing box. Subregion UI,
  Move tool and outlines must use the saved Position anchor. Test all local Y
  rotations and mirrors, followed by global transforms. Reset restores the source.
- Include hanging entities and distinct entities in overlapping source regions.
  Check entity positions and anchors before/after edits and pasted placement.
  Ignore entities excludes only that region's entries; repeated UUIDs remain
  deduplicated in the combined preview. Test both v1 wrappers and v2-7 flat NBT.
- Back up LoadedSchematics.json and restore an old version 1 subregions record
  with moved/rotated negative-size regions. Blocks must stay at the old positions;
  the displayed anchor coordinates can change. Saving upgrades the record to v2.
- Save Source and compare unedited .litematic bytes with the original. Export
  independent regions to .schemplus and download them from the current Plus server:
  verify distinct payloads and signed anchors survive. Flat files stay flat; format
  conversion does not promise retention of arbitrary unknown Litematic fields.

## placement activation, locks and enclosing boxes (native checks)

- In the placement list and configuration page, disable a placement. Its blocks,
  entities and outlines must disappear; pick-block and printing stop, material
  refresh is empty, and Paste/From Placement refuse it. Re-enable to recover its
  data and prior rendering preference. Other placements remain unchanged.
- Toggle R off while enabled: previews/entities and printing stop, outlines remain,
  materials still count and Paste still works. Toggle the enclosing-box icon with
  separated regions and verify only the combined outline changes. Check hover and
  disabled states at English/Chinese GUI scales 1-3. New enclosing boxes default off.
- Lock the placement. Main/subregion XYZ, Move, rotate, mirror and reset must be
  unavailable. Attempt Move with a selected subregion and with the entire placement;
  the upstream locked message should appear and no geometry changes. Repeat from
  the legacy controls. Visibility/entity flags and subregion enable switches remain
  usable. Unlock and verify geometry edits resume.
- Test each main coordinate lock and combinations, using field edits, nudges, Move
  to player, the Move tool and legacy minimum-coordinate controls. Locked world
  origin coordinates stay fixed, including after global rotations and with an
  outside origin. Coordinate locks do not prevent rotating around that origin.
- Reload the source, reconnect and switch dimensions with locked/disabled placements,
  mixed subregion transforms and coordinate locks. Geometry must restore before
  locks apply. Old entries without placementSettings stay enabled, retain their
  visible preference and have the enclosing outline enabled. New state is isolated
  per placement; unreadable settings must not restore a partially configured entry.
- Hold Shift when loading with Create Placement checked, or clicking Create Placement
  in the loaded list. The new placement is disabled and can later be enabled. Normal
  creation remains enabled. Saving/loading only a source must not create a placement.
- Pause an active printer by disabling or hiding the placement, then restore it.
  No blocks are placed and no completion notification occurs while paused. Resuming
  continues the existing job. Already queued world edits retain their captured data.
## Selection operations

- In the main menu, alternate Normal/Simple and edit different coordinates, names
  and origins. Neither selection should overwrite the other; reconnect and switch
  dimensions to verify session isolation. Existing v2-v4 selection files must load.
- In Simple, edit both corners directly and toggle the manual origin. The single
  box remains selected. Open the selection browser and configure a Normal area;
  this returns to Normal mode without losing the Simple box.
- Hold the tool in Area Selection mode. Corners mode: left/right set corner 1/2.
  Expand mode: right-click starts a single-block box, left-click grows it in any
  direction, clicking inside leaves its size unchanged. Sneak offsets to the hit
  face. Selecting the manual origin makes both mouse buttons move only the origin.
- Middle-click each corner, the box body, an origin and empty sky. Alt + wheel
  moves the selected corner, whole box or origin along the nearest viewing axis.
  The chosen corner turns cyan. Check overlapping origin/corner hits, occlusion,
  reversed corners, camera/freecam aiming and y=0/255 bounds. Invalid moves must
  leave both endpoints unchanged. Ctrl + wheel still cycles tool modes.
- Save and Fill/Delete/Replace from each mode: they must use only the active
  selection. Verify all native checks in Minecraft; headless tests cannot exercise
  GUI drawing, mouse dispatch or Forge world behavior.

### Area analysis

- Open Analyze Area from both modes. Select overlapping boxes, an air gap between
  boxes, mod machines/pipes with different picked-item metadata/NBT, and ordinary
  blocks. A real world position must count once; gaps and air must not become items.
  This counts the block's picked item, not recipes or inventory contents.
- Switch All/Render Layers for X/Y/Z and each range mode, including an empty range.
  Analyze bounds use absolute world coordinates. Search, sort, ignore/reset,
  multiplier and player inventory counts should work as on placement materials.
- Include unloaded chunks; their positions appear in the incomplete-result count.
  They must not be read as air or trigger client chunk loading. Load them and
  refresh. Blocks whose pick hook throws/returns no usable item should be skipped
  with a count, while the remaining scan finishes.
- Export TXT, Shift+export CSV and Alt+export JSON. All include unloaded/skipped
  counts; partial JSON sets complete=false. Missing/mismatch columns are zero for
  analysis of existing blocks, not a comparison against a blueprint.
- Close a large scan and reopen; it starts afresh. Change dimensions/disconnect
  during scanning; old-world results must not be delivered or exported. Invalid
  or oversized selections should show the size-limit message without retry spam.
- Simple: verify both corner checkboxes and coordinate inputs, manual-origin
  controls, Save/Analyze positions and the guide toggle with origin enabled.
  Escape returns to the parent menu.

## Icon button hover feedback

- Hover search, parent/root/new-directory, coordinate +/- and enclosing-box icons.
  Enabled buttons show the MaLiLib light-gray overlay and white outline inside
  their click area; moving away removes it. Tab focus provides the same cue.
- Disabled buttons do not highlight. Cropped list icons and icons behind a modal
  must not react outside the visible area. File/type icons and passive info icons
  are unchanged. Compare native screenshots rather than inferring appearance from
  the framework tests.

## Existing feature translation alignment

- Switch English/Chinese, then reopen configuration, load/save, loaded schematics,
  placement controls and area selection screens. Shared labels, visual settings,
  success/error messages and the tool HUD use upstream Litematica/MaLiLib keys.
  Plus-specific behavior keeps its own keys; test the 1.7.10 NBT paste warning,
  printer completion and server permission settings in both languages.
- Existing config values and Minecraft key bindings must survive the upgrade.
  The translated config labels change; serialized property/binding IDs do not.
- Export an area material list in Chinese as TXT/CSV/JSON. Text headers and partial
  scan labels follow the language; JSON field names and item IDs remain stable.
- Test invalid load paths and filenames containing percent signs or backslashes.
  Error tooltips must show the path intact, with no raw key or Format error.
- Simple-mode help must describe its independent selection, without claiming it
  is unavailable. Early mod-conflict and download capability errors
  retain readable English fallback when language resources are unavailable.
- Automated coverage checks source-referenced keys in English/Chinese and formats
  active templates across all 14 bundled locales with English fallback. These
  checks do not replace in-game text-width, hover or language-switch checks.

## Block info lines HUD

- Reference: Litematica OverlayRenderer.renderHoverInfo/updateBlockInfoLines and
  MaLiLib RenderUtils.renderText. The independent HUD defaults to top right,
  scale 0.5, offsets 4/4, enabled, fluids excluded. All six controls appear under
  Info Overlays using upstream translation keys; close the page to apply/save.
- Look at a visible ghost block within 10 blocks without holding the tool. Check
  the schematic title, registry name and metadata. The background is gray and
  translucent, each line right-aligned, with no text shadow. Minecraft 1.7.10
  metadata replaces modern block-state properties; tile NBT is not compared.
- At the same position, test wrong blocks and different metadata: both Schematic
  and Client sections should appear. Equal states show one schematic section;
  unrelated real blocks in front must occlude the ghost, without displaying
  information about a different position behind the wall.
- Target an unselected placement, overlapping placements, shifted/rotated regions,
  negative world coordinates, disabled regions, hidden placements and all render
  layer modes. Only visible regions inside the current range can supply HUD data.
- Move the Freecam camera while leaving the player still; target and reach must
  follow the camera. Check non-full shapes (slabs, stairs, GT pipes), vanilla and
  mod fluids. The fluid toggle includes flowing liquid blocks as well as sources.
- Try all alignments, offsets, font scale 0/0.5/1/2 and GUI scales 1-3; reopen the
  game to verify persistence. F1 and an open GUI suppress the HUD. Disconnect or
  change dimensions and confirm the previous target never appears in the new
  world. Other HUDs, item lighting and world rendering must remain unaffected.
- Automated tests cover voxel traversal, shape hits, range, occlusion, overlapping
  placements, coordinate translation, mutable trace inputs, state comparison,
  configuration persistence/validation and scaled alignment.

## Task manager

- Reference: Litematica GuiTaskManager, WidgetListTasks and WidgetTaskEntry.
  Open Main Menu > Task Manager. Check the 22-pixel alternating rows, hover
  background, right-aligned Remove buttons and bottom-right Main Menu button.
  Escape returns to the parent; Tab/Enter and button sounds remain available.
- Start a large area save, reopen the menu and check the filename in its tooltip,
  capture progress and automatic removal on completion. Cancel during capture:
  no new file should appear and an existing destination must remain unchanged.
  Once writing begins, Remove is disabled until the task completes or fails.
- Start singleplayer paste, fill, replace and delete operations. Check task names,
  phase-local progress, coordinates and affected block/entity counts. Removing a
  task stops placement, finishes the update pass and preserves changes already
  made. Repeated tool-key cancellation must not start a replacement job.
- Test normal and no-update paste, multi-region gaps and entity placement. Cancel
  early and during the update pass; no-update paste must still honor its setting.
  An old row/button reference must never cancel a newly submitted task.
- On multiplayer, command jobs wait for native server replies and track confirmed
  results. Missing replies stop the queue instead of implying completion. Remove
  stops further sends, including during the delay; already applied blocks remain.
  Permission/syntax errors, disconnects and world changes terminate the task.
- Cancel, finish and fail saves/edits, then start another. Change dimension or
  disconnect during capture/editing; old jobs must disappear and release their
  world references. Other players' tasks must not appear in this player's list.
  Client and integrated-server capture must run on their owning tick thread.
- Scope: this page tracks queued area saves, world edits and verifier scans in this process.
  Remote edit tasks receive progress/results for the requesting player; this is
  not a browser of every remote server task. Material scans still belong to
  their material screen; synchronous source-file saves are not queued tasks.
- Automated tests cover immutable progress publication, owner/dimension filters,
  stale handles, cancellation/write races, edit phases, command preflight/busy
  rejection and queue cleanup.


## Schematic verifier: backend and results UI

- Reference: Litematica GuiSchematicVerifier, WidgetListSchematicVerificationResults,
  WidgetSchematicVerificationResult and SchematicVerifier. Open a placement's
  configuration, then Schematic Verifier. Compare the two control rows, category
  colors, 22-pixel rows, expected/found item columns, count sorting and Ignore.
  Narrow screens wrap controls; row text clips and full IDs/metadata appear in
  tooltips. Shared labels and HUD options use the upstream translation keys.
- Start with a small blueprint containing correct, missing, extra, wrong-block
  and wrong-metadata positions. Air/air must not count as correct material. All
  shows non-ignored errors; each filter shows its own category. Click the three
  headers to sort and reverse. Ignore one exact expected/found pair; another
  metadata pair remains visible. Reset ignored must restore current results.
- Scan a large placement, close the screen and open Task Manager. Work continues
  on client ticks with a shared 4 ms budget. Remove pauses verification while
  retaining its data. Reopen the verifier and Resume; no double counts. Reset
  data removes the task and releases the scan. Completed scans leave Task Manager.
- Use shifted/rotated/mirrored placements, negative coordinates, overlapping and
  disabled subregions, and separated regions with unloaded chunks in the gaps.
  Check All and Render Layers for X/Y/Z and the legacy placement layer. Only
  included cells require loaded chunks. No client chunk should be force-loaded;
  unloaded included cells remain pending, not missing or correct.
- Load an empty real chunk and an unloaded one. The former is valid air data;
  the latter must wait for actual data. A block whose read hook fails increments
  the unreadable/incomplete count and logs at most three details per scan.
- Fix errors while scanning and after completion. Client world update events
  queue affected chunks for bounded rechecks; old counts are replaced, including
  unreadable positions that become readable. Pause, change blocks and Resume.
- Move, rotate, reload, disable or edit the placement, or change its layer range.
  Old results must be cleared with a restart notice. Unload the placement, change
  dimensions and disconnect; tasks/results must not survive into another world.
- Lock/unlock a placement or select a different subregion without modifying it;
  these UI operations must preserve verification data.
- Toggle Info HUD. It defaults to bottom right, scale 1, offsets 1/1. Configure
  infoHudAlignment/Scale/OffsetX/OffsetY in Info Overlays, restart and verify they
  persist. Check F1, open menus and all GUI scales; other HUD/GL state must remain
  intact. The existing top-right Block Info Lines settings remain independent.
- Scope: compares registry IDs and metadata. Tile NBT, entity contents and modern
  block-tag equivalence are not compared. Results and marker hover panels show registry IDs and metadata only.
  Results update from client world notifications, not server NBT queries.
- Automated coverage checks classifications, exact-pair ignores, cropped bounds,
  negative/chunk coordinates, finite work budgets, unloaded chunks and gaps,
  replacement counting, updates during partial scans, failed reads, allocation
  limits and HUD configuration/translation persistence.


## Schematic verifier: selected markers and comparison overlays

- Select the placement whose verification results should be displayed. In its
  verifier, click a category to mark every non-ignored pair in that category, or
  click individual rows to select several pairs. Selecting a category clears its
  individual selections; selecting an individual clears that category selection.
  Other categories stay selected. Correct-state rows cannot produce markers.
  Filter/sort operations preserve selections. Ignore must not also select a row;
  mouse release outside a row cancels the click. Tab/Enter/Space and click sounds
  should follow the existing button behavior. Selected rows have a gray fill and
  light border. Disabled overlays display a configuration hint on selection.
- Close the GUI: missing/correctable errors have colored, through-wall outlines
  and translucent faces. The nearest 1000 selected positions are shown by default.
  `verifierErrorHilightMaxPositions` accepts 1..10000 in this 1.7.10 port.
  Search work is incremental (up to 16384 cells / 1 ms per client tick), separate
  from verification's 4 ms allowance. Already correct chunks are skipped, and
  distant chunks are pruned once enough closer markers have been found.
  Very large selections may take several ticks to refresh after camera movement.
- Set renderErrorMarkerSides, renderErrorMarkerConnections and
  verifierErrorHilightAlpha. Outlines remain visible when faces are disabled or
  alpha is zero. Marker RGB follows the corresponding existing overlay colors.
  Aim at a marker: its outline thickens. Hold renderInfoOverlay (default I) for
  the Expected/Found item, registry ID and metadata comparison panel. Remap the
  key to a mouse button and then unbind it; an unbound key disables the panel.
  The panel does not query server NBT or preview inventory contents.
- Hover a verifier result for the same comparison panel. Check long mod IDs,
  blocks without items or broken item render hooks, both languages, all GUI
  scales and narrow windows. Check blockInfoOverlayAlignment (top center/center)
  and blockInfoOverlayOffsetY, including negative offsets. Info HUD toggling only
  affects status/coordinate lines, not the marker hover panel. infoHudMaxLines
  limits the nearby coordinate list; screen height additionally bounds it.
- Move through negative coordinates and chunk boundaries, then enable Freecam.
  Boxes must remain on world blocks; ray targeting and nearest ordering must
  follow the camera. Test near large world coordinates for float jitter.
  Switch selected placements; only the selected placement's markers/HUD appear.
  Fix a marked block, ignore a pair, reset ignored, pause/resume the verifier,
  move/rotate/unload a placement, change dimension, disconnect and reconnect.
  Corrected/ignored/invalid results must not leave ghost markers or hover panels.
- F1 hides markers and HUD. Menus hide the in-world comparison panel. Check
  world translucency, other mods' overlays, lightmaps and item rendering afterward
  for OpenGL state leaks. Keep the existing top-right Block Info Lines enabled.
- Automated coverage: category/entry selection transitions, brute-force nearest
  comparison across cropped negative chunks, deadlines/work caps, sparse results,
  correction/ignore invalidation, changes during partial searches, camera/limit
  changes, world-scan replacement, immutable output and selected-cell ray hits.
  Config defaults, normalization, persistence and dynamic upstream keys are also
  tested.


## Branding, Forge logo and pick-block fallback

- Switch between all shipped languages. The English main-menu button reads
  "Schematica+ menu"; menu/config titles and functional descriptions use the
  local Schematica+ branding. Upstream translation keys, .litematic format names,
  file conversion messages and attribution remain intact. Regenerating with
  tools/import_translations.py must preserve these display adaptations.
- Open Forge's Mods screen from an installed release JAR and select Schematica
  Plus. The existing schematic preview logo should appear. mcmod.info.logoFile
  must exactly match its JAR entry, without a leading slash: FMLFileResourcePack
  passes this value directly to ZipFile.getEntry.
- In creative, select an enabled schematic. Middle-click a real block outside
  it, a real block closer than a ghost block, a ghost block closer than the real
  block, and a real block occupying the same position as the schematic block.
  Vanilla handles real targets; the schematic only consumes successful picks.
  Try actual entities, misses, disabled rendering/subregions and render layers.
  Move/rotate/switch the placement immediately before picking to check that the
  handler traces current geometry rather than using the previous rendered frame.
- Repeat with negative placement coordinates, Freecam and a keyboard binding for
  pick block. A mod hook that returns false or throws must leave one pending
  vanilla action, not swallow or duplicate the click. Existing survival behavior
  should be preserved. Check slab/snow special handling and modded pick hooks.
- Source references: generated Forge 1.7.10 / 10.13.4.1614 sources, MCP stable 12:
  Minecraft.runTick and func_147112_ai, FMLFileResourcePack.getPackImage and
  FileResourcePack.getInputStreamByName. Upstream Litematica 0.27.8 (MC 26.1.2)
  WorldUtils.doSchematicWorldPickBlock returns false for no schematic target and
  uses RayTraceUtils.getSchematicWorldTraceIfClosestNoFluids for nearest picking.
- Automated regressions cover input replay, failed/throwing picks, misses, nearest
  world/schematic/entity targets, negative offsets and coincident blocks. Native
  Forge Mods UI, creative inventory and Freecam checks remain unexecuted here.


## Advanced hotkeys and tool input

- Release M to open the menu; M+C/M+S/M+P/M+L/M+V opens the corresponding screen
  without opening the menu after releasing the chord. Ctrl+Alt+S opens Save.
  Execute Operation is unbound by default. Modified legacy single-key bindings
  migrate once from options.txt into config/schematica_plus_hotkeys.json.
- Hotkey rows capture ordered keyboard/mouse combinations. Escape clears an
  untouched binding or finishes an edited chord; clicking outside also finishes.
  Advanced settings use the original MaLiLib seven-option dialog and icons;
  right-click its icon to restore settings. Check persistence and reset.
- Exercise PRESS/RELEASE/BOTH, INGAME/GUI/ANY, order, extras, exclusive and cancel.
  Test focus loss, opening/closing GUIs with keys held, mouse releases outside
  the window, OS repeat and conflicts with vanilla controls. Empty modifiers
  are active only when Allow Empty is explicitly enabled.
- Hold the tool: Ctrl+wheel cycles modes; Alt+wheel nudges the selected element.
  Left/right position corners or placement origins. Area clicks offset when
  sneaking; placement clicks offset when not sneaking. Middle selects; configured
  grab modifier + middle grabs/releases an area element or selects a placement
  subregion. Grab+wheel changes distance. Alt/Shift+middle select primary/secondary
  block state for modes that use it, including schematic states and air.
- Verify normal middle-click still picks real blocks outside/closer than the
  schematic; check custom keyboard bindings, disabled tools and pick toggle.
- Test an installed JAR: its FMLCorePlugin manifest entry must load InputPlugin.
  Only Minecraft and GuiScreen LWJGL next() calls are redirected. Native game
  interaction has not been exercised by the headless test suite.


## World move tool

- In creative MOVE mode, bind selectionGrabModifier. Modifier + either corner
  key moves the selected world area to the targeted adjacent face (sneak: inside).
  Modifier + wheel moves by one block along the camera direction when no origin
  is selected/grabbed. Move Entire Selection uses player position.
- Test singleplayer and a dedicated server with this version on both sides.
  Require creative and permission level 2. Old/unmodded servers are refused;
  no client-NBT copy/delete fallback is attempted. Select at most 1,048,576 cells
  in the enclosing box and load source and target chunks before moving.
- Include overlapping source/target, multiple boxes with gaps, chests/inventory,
  GT pipe NBT, redstone, entities/paintings, negative coordinates and height edges.
  Check that successful completion moves the area coordinates only if the area
  still exists and its geometry was not edited while the job ran.
- Cancel before capture, during clear, placement or updates; disconnect/change
  dimension and stop the server mid-move. Changed block snapshots should restore
  on cancellation/failure; normal shutdown drains rollback before saving. Runtime
  mod callbacks, entities, abrupt process termination and crash recovery still
  require native testing; no disk-backed transaction log is provided.

## Edit Schematic (REBUILD) tool mode

- Select Edit Schematic with Ctrl+wheel while holding the tool. Without the tool,
  left-click a rendered schematic block that is nearer than any real block: it is
  removed from the schematic only. Right-click places the held block item against
  the targeted face (stairs/logs/pistons/furnaces orientation, slabs, GT machines and
  other mod ItemBlocks with their placement data); with an empty hand the picked
  primary block (Alt+middle) is used. Non-block items, misses and nearer real blocks
  must fall through to vanilla. The real world must never change.
- Bind the six schematicEdit* modifiers. Check break/place/replace in the targeted
  direction (center quarter = into the block for break/replace), break/replace all
  identical, break all except the targeted state, replace block type (metadata kept)
  and fill air. Bulk edits need the targeted placement selected (or one of its
  subregions selected) and stay inside the render layer range; otherwise the
  original warning appears and, as in Litematica, the click falls through to
  vanilla (a real block behind the preview within reach is attacked/used).
  Successful edits consume the click; refused ones never do.
- Repeat with rotated/mirrored placements, rotated subregions, signed anchors,
  overlapping and disabled/hidden subregions, independent .litematic regions, flat
  .schematic sources and several placements of one source: every placement of the
  source must update consistently, other sources must not. Bulk edits on large
  sources continue over several ticks without freezing the client, appear in the
  Task Manager and can be removed there (changes made so far remain); a second
  bulk edit while one runs is refused. Edits over 4096 cells
  recompose the placements instead of patching cells; check selection, layers,
  verifier and material list afterwards.
- While a modifier is held, the targeted schematic face shows the upstream overlay
  (break/place green, break-except red, replace orange; editable in Colors). The
  directional overlay highlights the center/edge zone that decides the direction,
  for all six faces and all four player facings, through blocks.
- schematicEditReplaceSelection copies loaded real blocks that differ inside the
  area selection (no size limit; large selections run as a task) into the
  schematic(s) shown there; the success message appears when the task completes.
- Edited sources show an orange name and notice icon in Loaded Schematics until
  saved; Save writes a new .schemplus file (the original file is untouched),
  overlapping independent regions are refused, and Reload/Unload ask before
  discarding unsaved edits. Edits are memory-only and are lost when the world is
  left without saving. Tile entities of newly placed blocks are default instances;
  real-world tile data is not copied.

## Rendering toggles and render-state hotkeys

- M+R toggles all rendering (schematics, overlays, placement/area boxes, verifier
  markers, rebuild overlay, HUDs and tool use) and M+G schematic rendering; the
  action bar prints "Toggled <name> ON/OFF". Bind and test the other toggles:
  schematic blocks (tile entities/entities stay), overlay, overlay outlines,
  overlay sides, translucent blocks, area selection boxes, placement boxes,
  block info overlay and verifier overlay. Each change is saved to the config
  and visible in the Visuals/Info Overlays tabs.
- Hold invertGhostBlockRenderState / invertOverlayRenderState: schematic
  blocks/overlay flip visibility only while held, and schematic blocks cannot be
  targeted (pick block, Edit Schematic, info lines) while hidden.
- Hold Right Ctrl (renderOverlayThroughBlocks) or enable
  schematicOverlayRenderThroughBlocks: overlay faces/lines draw through terrain.
- Hold I without a verifier marker under the crosshair: the block info overlay
  shows the client block, the schematic block, or both side by side when they
  differ, at the configured overlay alignment/offset. Inventory previews are not
  ported.

## Clone, in-memory schematics, placement rotation/mirror and pick block

- cloneSelection captures the selected area (same capture task as Save, so a
  pending save blocks it) into an in-memory schematic, creates and selects a
  placement at the targeted block (adjacent face unless sneaking), or at the area
  origin with cloneAtOriginalPosition, and switches creative players to Paste.
- saveAreaAsInMemorySchematic asks for a name and adds an in-memory source without
  a placement. In-memory sources show "IN-MEMORY ONLY" in Loaded Schematics, cannot
  be reloaded, are not restored in the next session, and can be saved to a file.
- schematicPlacementRotation / schematicPlacementMirror rotate the selected
  placement clockwise / cycle its mirror with the upstream action bar message;
  locked placements show the locked message.
- pickBlockFirst (middle click) picks the nearest rendered schematic block of any
  placement when it is not farther than the real block; pickBlockLast picks the
  farthest schematic block in front of the targeted real block, or the schematic
  block in the empty space against its face. Bound to the use key, it picks before
  the vanilla use action. Both are off while holding the tool or when rendering
  or schematic rendering is disabled.

## Tool behavior parity (Execute, scroll, selection mode)

- A second Execute while an edit runs reports busy and does not cancel it; remove
  the task in the Task Manager instead. Fill without a picked block and Replace
  without both blocks do nothing. With a selected box, Fill/Replace/Delete affect
  only that box; otherwise all boxes. The integrated-server message is the
  upstream "scheduled task added".
- selectionModeCycle (Ctrl+M): area modes cycle Corners/Expand; in Delete it
  switches the target between the area and the selected placement (Tool HUD shows
  "Delete target mode"; tool clicks then move placements); in Paste it toggles the
  replace mode None/All (Tool HUD "Replace blocks").
- Grow modifier + wheel always consumes the scroll and reports missing area/box;
  nudge modifier + wheel only consumes it when something moved; mode change
  modifier + wheel honors reverseOperationModeDirection.
- addSelectionBox selects the new box's first corner and prints the position;
  deleteSelectionBox removes a selected manual origin first, otherwise the selected
  box, with the upstream messages. toolEnabledToggle and pickBlockToggle print the
  toggle message.

## Easy Place, Placement Restriction, Sign Text Paste and Status Info HUD

- easyPlaceToggle (or the Generic config) enables Easy Place. Right click
  (easyPlaceUseKey) on a schematic block picks its item (hotbar/inventory swap, or
  set in creative) and places it at that position; holding the key places every
  block the crosshair moves over. easyPlaceFirst (hotkey toggle) places the nearest
  schematic block; off, the farthest one before the real block (layers "at once").
  Real blocks in front, occupied positions, wrong items and repeated clicks within
  2 s show "Action prevented by the Easy Place mode" per placementRestrictionWarn.
- Check torches/levers/ladders (need a solid side; refused otherwise), logs, slabs
  (upper/lower and completing a double slab), stairs, repeaters (extra clicks),
  doors, carpets, GT/mod blocks, signs, buckets/cells for fluids, survival and
  creative, easyPlaceSwapInterval with high ping, easyPlaceVanillaReach on servers.
  Without a Plus server (or with easyPlaceProtocolVersion = None), known facing-based
  blocks use the placement registry's player/piston direction rules and restore the view.
- In both GTNH 2.8.4 and 2.9.0-RC-2, use survival with protocol None and a deliberately
  different player facing. Place upper oak stairs with metadata 6 and a south-facing bed
  (foot metadata 2, head 10). Enable Post-Rewrite and Click Adjacent, then place white glazed
  terracotta with metadata 3 beside a solid support. Each action must consume one item,
  match the schematic, restore yaw/pitch and refuse a repeat without further consumption.
  Save and inspect the real blocks with the preview hidden. Test missing inventory and
  blocked positions too: failed clicks must not report success.
- For doors in GTNH, capture real doors with complete tile NBT: MalisisDoors replaces the
  vanilla door and depends on that tile for its bounding boxes. A template containing only
  the block ID and metadata cannot validate full door support. The ordinary direction pass
  covers stairs, beds and glazed terracotta; other door variants still need separate checks.
- togglePlacementRestriction: using items is blocked where the schematic has air
  near its regions, outside the layer range, into occupied positions, with the
  wrong item, or (for blocks with known orientation rules) with the wrong
  orientation; the warning follows placementRestrictionWarn.
- toggleSignTextPaste: placing a sign on a schematic sign opens the editor filled
  with the schematic text (first 15 characters per line); closing sends it.
- statusInfoHud shows Easy Place / restriction state, layer mode and renderer
  switches at the bottom left; with statusInfoHudAuto it appears for 10 s after
  creating a placement or toggling a render switch while something is hidden.

## Plus translation keys and save behavior

- Save Area as schematic and Save loaded source: saving onto an existing name now
  fails with the Litematica "file already exists" message; hold Shift while
  clicking Save to overwrite (hover shows the Litematica hint). No overwrite
  confirmation dialog is expected.
- Area Selection browser: the red "-" removes the selection immediately, as in
  Litematica. Re-test that the removed selection does not return after reconnecting.
- Area corner, origin and sub-region coordinate plus/minus buttons: right click
  decreases, Shift ×8, Alt ×4, both ×32; hover shows the MaLiLib plus/minus tip.
- File operations: an invalid name shows the MaLiLib illegal-characters message
  with the name; an existing target shows the MaLiLib "already exists" message.
  Multi-line MaLiLib messages appear on one status line and fully in its hover.
- Paste/Fill/Delete/Replace finish messages are the Litematica ones (Schematic
  pasted in world / Area filled / Area cleared, and the failure variants after a
  cancel or error). Command paste waits for native server replies before reporting
  completion; permission failures and no-op edits must not announce successful
  placement. Queueing shows "Scheduled task added...", not completion. A disabled
  placement reports the rendering-disabled message.
- Verifier: before starting or while paused the status line is empty (Litematica
  shows nothing); counts appear only after a finished pass. Clicking an error
  category with overlays off shows the Litematica warning naming Info Overlays,
  the toggle hotkey and its keys. Metadata lines in the verifier and the block info
  overlay use MaLiLib's integer property format (`metadata = 3` / `metadata: 3`).
- Config: the search field hover is MaLiLib's search/hotkey hint; capturing a key
  filter shows `> NONE <` until a key is pressed. Reset, slider and keybind buttons
  have no hover text, as upstream.
- Ukrainian: paste-by-command and data-restore HUD texts show their numbers
  instead of "Format error".
- Check the removed hint tooltips did not leave blank hover boxes (render layers,
  main menu mode buttons, area editor, placement visibility/rotation, search icons).

## Paste replace behavior

- Existing config with `pasteOnlyAir=true` loads as None, `false` as With non-air;
  the old key disappears from the file. A fresh config starts at None.
- With selectionModeCycle in Paste mode, the HUD cycles None (red) → All → With
  non-air (orange); the Generic config row shows the same names and cycles on click.
- Paste a schematic with air pockets over stone in each mode, singleplayer and on a
  server via commands: None fills only air, All replaces everything and clears
  stone where the schematic is air (only inside enabled sub-regions), With non-air
  replaces with the schematic's blocks but keeps world blocks under schematic air.
  Clearing must drop no items from replaced chests and keep neighbor updates and
  "Paste without block updates" behavior.
- placementRestrictionWarn also shows as a cycling None/Message/Actionbar button.

## Schematic VCS (projects)

- With `unhideSchematicVCS` off, the projects hotkey prints the hidden warning and the
  main menu has no Schematic VCS button. Turn it on; the button shows the upstream warning.
- Projects browser: create a project (illegal and existing names are refused), see it in
  the list with the project info panel, load it, close it, delete a project JSON (the open
  project closes first). Reconnect: the open project and its current version return.
- While a project is open: the area browser button/hotkey is disabled with the upstream
  hover/message, the area editor title and its save button change to the project ones,
  tool clicks edit the project area in every tool mode, switching Normal/Simple works,
  and closing the project brings back the world's selections unchanged.
- Save Version (manager, area editor or saveAreaAsSchematicToFile): enter name and
  description; a numbered `<project>_00001.schemplus` appears beside the project JSON, the
  version is checked out as a placement at origin + area offset, and the chat shows the
  version message. A second save while one is pending is refused. A cancelled or failed save
  re-allows saving.
- Version list: click versions to switch the placement; hover shows number, name,
  timestamp and description. schematicVersionCycleNext/Previous and the cycle modifier +
  scroll switch versions; the Tool HUD shows project, version, date and origin.
- Move to player moves the origin outline, both selections and the current placement.
- Place to world / Execute (creative): with schematicVcsDeleteMode Entire Volume the
  current version's area is cleared first and then pasted; other modes paste directly and
  print the upstream "No previous pasted version known" note on the first paste.
- Delete Area clears the last seen area. schematicVCSDeleteBlockByPlacement removes
  matching / non-matching / any / no-schematic / all world blocks inside the current version
  placement within the render layer range and reports "Deleted N blocks". Test both
  singleplayer and a server with command permission.
- Version placements are not written to LoadedSchematics.json; a schematic file that was
  already loaded by the user stays loaded when the project closes.

### Project disk recovery and narrow layouts

- Save a project with automatic origins in both normal/simple selections, multiple
  versions and a multiline description. Quit the world and the client completely.
  Reopen with VCS enabled; require the current version, origin, selection bounds and
  paste history to return. Compare the schematic files' hashes and complete NBT.
- Load an existing project written without explicit null origin/selectedBox/selected
  fields. Automatic origins and unselected states must recover; a selected manual
  origin without coordinates must still be rejected. Loading must not rewrite the
  input just to repair its in-memory representation.
- At 1280x960, check GUI scales 1, 2, 4 and Auto (scale 4 really gives 320x240).
  Require every manager/browser action to stay on-screen and outside the version
  list. Test English and Chinese; open Save Version, cancel it, select versions,
  and exercise the browser with all action buttons visible.
- In command mode with ENTIRE_VOLUME, put a gold sentinel in the destination.
  Cancel blocks-only confirmation and verify no clearing/history update. Accept
  and require acknowledged clear-then-paste, empty inventories and blank signs.
  Slow the queue, switch the selected version while a paste is running, and verify
  the old task cannot mark the newly selected version as pasted.
- For command grid paste, use a base plus one repeat. Cancel the base confirmation
  and inspect both destinations; accept the base but cancel the repeat and require
  only the base; finally accept both and check both complete block layouts.

## VCS follow-ups: origin outline, multi-line description, entity deletion

- With a project open, turn off area selection box rendering: the magenta 4 px project
  origin outline stays visible (depth tested). Area box lines are thicker in project mode
  and thinner (1.5 px) outside it.
- Save Version description box: type several lines with Enter, wrap long text, scroll with
  the wheel, select with mouse drag / Shift+arrows, Ctrl+A/C/X/V, Ctrl+Backspace/Delete,
  Home/End and Ctrl+Home/End, Page Up/Down. More than 8 lines or 512 characters is refused.
  The saved description keeps its line breaks in the version hover and the info panel.
- Singleplayer: Delete tool (area and placement target), VCS Delete Area, Entire Volume
  pasting and delete-by-placement remove mobs, items, item frames, paintings and minecarts
  inside the deleted boxes but never players; Fill and Replace keep entities. On a server
  using commands, the chat says entities are kept.

## Server protocols: accurate placement and remote full-NBT edits

Needs a dedicated server (and a LAN-opened singleplayer world) running this build;
compare with a server that does not have the mod and with each server option off.

- Development-build login: install the same untagged main JAR on a dedicated server
  and client, including a git-hash/dirty version. Join and require the world to open
  without a `Mod rejections [FMLMod:schematica_plus{...}]` screen. Check the client's
  `Server capabilities{...}` log after joining. Matching development identifiers
  must be accepted; test a server without Plus separately. This does not extend
  the support scope to older or mixed Plus versions.
- Download completion: download an ordinary single-region `.schematic` and a flat
  `.schemplus` with extended metadata. Both must finish, create usable client files
  and leave the dedicated server running. These files have no independent-region
  payload even when the client supports one. `DownloadCompletionTest` also checks
  independent-region slice ordering and verifies that encoding failures cannot send
  a successful end packet.
- Accurate placement: with easyPlaceProtocolVersion Auto/V3/V2, Easy Place and the
  printer place stairs (all facings and upside down), slabs (top/bottom), logs/pillars
  (all axes, quartz pillars), pistons/dispensers/droppers/hoppers (all six facings),
  furnaces/chests/ender chests, ladders, torches, levers and buttons, fence gates and
  trapdoors (open and top), rails (curves, slopes), repeaters (delay) and comparators
  (subtract mode), pumpkins, anvils, cocoa, tripwire hooks, end portal frames and
  standing signs exactly as in the schematic, whatever the player faces. Slabs Only:
  only slabs are corrected. None: the old facing behavior. Powered levers/buttons,
  lit/powered states, crop ages and portal frame eyes are not copied; doors and beds
  keep vanilla orientation. In survival the item count and drops are unchanged.
  Turn accuratePlacementEnabled off on the server: after reconnecting the client
  falls back to the facing behavior. Check high ping and placeInstantly with several
  blocks per tick.
- Remote edits (creative + op): Paste on a remote Plus server keeps chest/furnace/sign/
  spawner/GT machine NBT, pastes entities, honors pasteWithoutUpdates and
  pasteReplaceBehavior, and Delete removes entities inside the boxes. Fill, Replace,
  Delete, delete-by-placement and VCS Place to world/Delete Area run on the server too.
  The Task Manager shows the Upload stage, then the server stages and counts; removing
  the task during upload or the structure pass cancels it on the server. The chat shows
  the Litematica finish message. Non-op or survival: "permissions" message, nothing
  changes. remoteEditsEnabled off: rejection message; the client must not fall back to
  commands silently. Large pastes (millions of blocks) upload without disconnect;
  changing dimension or disconnecting during upload ends the task with the lost message.
  A second edit while one runs (from this or another player) is refused as busy.

## .litematic export (1.12.2 format), import/export and schematic metadata

- Save Area as Schematic → Options: the format button cycles .schematic / .schemplus / .litematic.
  Save a mixed build (logs in all axes, stairs, slabs top/bottom, doors, fences, rails, redstone,
  chests with items, signs with text, a spawner, item frames with items, a horse/donkey/mule, a wither
  skeleton, armour stands are not in 1.7.10) as .litematic and load it back: blocks, orientations, chest
  items, sign text, spawner mob and entities must match. Repeat with GT/modded blocks and machines.
- Copy the .litematic into Minecraft 1.12.2 with Litematica (LiteLoader) and into a modern Litematica
  (1.20+/26.x): it must load with the same vanilla blocks and orientations (modern Litematica converts
  1.12 states). Check stairs shapes, fence/pane connections, double plants, doors, chests with items,
  signs, the spawner and entities. Modded blocks are expected to be missing in other packs.
- Load a .litematic made by 1.12.2 Litematica and one made by modern Litematica in Plus.
- Schematic Manager on a writable schematic: Schematic Edit → Rename Schematic / Change Author update the info
  panel (name, author, Modified time) without changing blocks or the version line; Set Preview closes
  the GUI, the next screenshot key sets the preview (chat success), right click on the Edit button
  cancels a pending preview, Ctrl+Alt+Shift click uses thumb.png from the same folder. The preview
  appears in the browser info panels (manager and load screen), scaled down on small windows.
- Import on .schematic / .schemplus / .litematic / .nbt files: saves a .litematic (Ignore entities
  works; existing name refused unless Shift is held). Export As on a .litematic: Schematic
  (.schematic, or .schemplus when IDs/regions need it), V4 (1.12.2) Litematic, Vanilla Structure (.nbt).
  Load the exported .nbt in Plus and in 1.12.2 with a structure block.
- The info panel shows Litematic Version / Vanilla Structure and the Minecraft version from the data
  version for .litematic and .nbt files; other files keep the name, size and date view.

## Load browser material list and save options

- Load screen: Material List on a file opens "Material List for schematic '<name>' (n of m regions)"
  without loading it; totals equal Missing, Available follows the inventory. Shift+click on a
  multi-region file asks for the sub-regions first (OK disabled until one is ticked). Rename Schematic
  changes the metadata name shown in the info panel for writable formats; Sponge files remain import-only.
- Save Area as Schematic: Save from schematic world with one or two placements overlapping the area
  saves the placement blocks, chests with contents and entities instead of the world (also with the
  real world empty there). Visible blocks only on a solid 5x5x5 cube saves only the shell; with
  Include support blocks, blocks under hidden repeaters/carpets/snow and under visible sand/gravel
  columns are kept too. Check all three with .schematic, .schemplus and .litematic outputs.

## Grid / repeated placements (ported from 1.12.2 Litematica)

- Placement configuration → Grid/Repeat button opens the grid screen; Shift+click only toggles.
  Grid Size cannot go below the placement size; Reset Size restores it. Set repeat counts for
  -x/+x/-z/+z (and Y) and enable: copies appear at size steps, rendered like the placement, with
  correct missing/wrong-block overlays for their own positions and the render layer range.
- Walk far away: copies outside render distance + 1 chunk disappear and new ones appear ahead.
  Move, rotate, mirror or edit sub-regions of the base: copies follow. Disable the placement or
  the grid, or remove the placement: copies vanish. Reconnect: grid settings are restored.
- Copies are targetable by Easy Place, the block info overlay, pick block and placement
  restriction, but are not listed in Loaded/Placements lists and are not selectable or saved.
- Tool mode Grid Paste (creative): Execute pastes the base and every existing copy in turn (watch
  the Task Manager); with the grid off it pastes only the placement and prints the note. The paste
  replace mode is shown in the HUD and cycled with selectionModeCycle as for Paste.
- Hotkey openPlacementGridSettingsScreen (unbound by default) opens the screen for the selected
  placement.

## Independent overlapping regions in .schemplus and downloads

- Load a .litematic with two overlapping regions of different blocks, edit a block in each with the
  rebuild tool and save the source as .schemplus: the save succeeds; reload it and check both regions
  keep their own blocks, chests and entities, and moving one sub-region shows the other's blocks.
- Old Schematica / Plus builds load the same .schemplus as one flat schematic.
- Server download (/schematic download) of such a file from a dedicated server to this client: the
  saved file restores both regions. An older Plus client gets the update-client message.

## File-based area selections (selection manager folders)

- First join of a world that had selections: they appear unchanged; after saving, each is a JSON file
  in schematics/area_selections_per_world/<world>/area_selections and AreaSelection.json no longer lists
  them. The selected selection, simple mode and corner mode are kept per dimension.
- Selection manager: create a directory, enter it (click), create / copy / rename / remove selections
  there; root and up buttons return. Files and folders appear on disk accordingly; renaming renames the
  file. Copy a Litematica area selection .json into the folder (F5 not needed after reopening): it loads
  with its boxes, current box and origin. Copy a Plus file into Litematica's area_selections: it loads.
- areaSelectionsPerWorld off: all worlds share schematics/area_selections after reconnecting.
- Schematic projects still keep their own selections (the manager stays disabled in project mode).


## Material list HUD, cache and Raw Materials

- Placement configuration → Material List; close it; the Material List hotkey reopens the same list
  (ignored entries, multiplier and Hide available kept). Select another placement: the hotkey now opens
  that placement's list (recounted). Load browser Material List and Analyze Area also become the list
  the hotkey opens. Reconnect: multiplier, sort, Hide available and Show (render layers) are restored.
- Info HUD: ON draws "Material List" with the missing items at the info HUD corner (default bottom
  right), below verifier HUD lines if those are shown. Counts drop by what is in the inventory within
  2 seconds; covered items disappear. Multiplier 2 shows the doubled totals. materialListHudMaxLines,
  materialListHudScale and materialListHudItemCountsColor change it; renderMaterialListInGuis off
  hides it in the inventory and other screens. Turning on the HUD of another list turns this one off.
- Counts: a door, bed, tall flower, double slab, 4-layer snow, still water/lava source and a flower pot
  with a flower count 1 door, 1 bed, 1 flower, 2 slabs, 4 snow, 1 bucket each, pot + flower; flowing
  water, piston heads and portals count nothing. GregTech machines and AE2 cables keep their own items.
  materialListIgnoreState on: rotated stairs in the world count as done.
- Clear cache shows "Material Cache cleared" and the next refresh still counts the same items.
- highlightBlockInInventory on: look at a schematic block, open the inventory or a chest: slots with
  that item are tinted in hightlightBlockInInventoryColor.
- Raw Materials writes three JSON files to the dumps folder (two with materialListRecipeDetails off):
  a ladder/planks build resolves to logs; iron bars to iron ingots, packed into blocks plus remainder in
  the simplified file; glass to sand via smelting. Shift writes the _missing_only files for items not yet
  in the inventory. Check a GTNH build to see that it finishes and that the chosen recipes are sensible.

### Material names after changing language

- Open a persistent placement material list in English, enable its HUD and record
  missing/available counts. Give part of a required stack and require the HUD to
  subtract it; covering the requirement removes that item within the update interval.
- Change to Chinese using the language screen, reopen the same list without pressing
  Refresh, and require translated item names in the list and HUD. Include a material
  replacement, ignored item, custom sort and multiplier; keep those settings/counts.
  Switch back to English and repeat. This checks the persistent list, not a new scan
  constructed only for the test.
- Toggle renderMaterialListInGuis and verify visibility inside a screen, then close
  it and require the world HUD to remain usable. Check narrow GUI scales too.

## Different blocks, inventory previews, Easy Place post-rewrite, NBT and Sponge import

- enableDifferentBlocks on: oak stairs in the world where the schematic has stone stairs with the same
  facing, orange wool for white wool, birch logs for oak logs of the same axis show the yellow
  "different block" overlay (schematicOverlayColorDiffBlock) and count as Diff Blocks in the verifier
  (button, status line); a different facing stays Wrong State. Off: they are Wrong Block/State as before.
  schematicOverlayTypeMissing/WrongBlock/WrongState/DiffBlock hide their overlays; overlay line width
  follows schematicOverlayOutlineWidth, and schematicOverlayOutlineWidthThrough while rendering through.
- Hold the info overlay key on a chest/furnace/dispenser/hopper in a placement and in the world: the
  schematic's contents left, the world's right (single player shows the real items; double chests
  joined), the block info panel below them for top center. Same on a verifier marker.
- easyPlacePostRewrite on: single top/bottom slabs are placed as the schematic's half, also next to an
  existing slab of the same kind without merging into it; double slabs are placed in one click;
  easyPlaceClickAdjacent places plain blocks only against an existing block.
- Pick block / Easy Place with the item in the main inventory: it is swapped into one of
  pickBlockableSlots (empty slot first); tools and damaged items are kept with pickBlockAvoidTools /
  pickBlockAvoidDamageable; an empty pickBlockableSlots shows the warning.
- REBUILD replace selection over a chest with items, a sign and a spawner: the edited schematic keeps
  their contents/text/mob (single player; on a server what the client knows).
- Export a .litematic with potions (normal/splash), spawn eggs and a skeleton riding a spider; load it in
  1.12.2 Litematica and re-import in Plus: items and the rider are kept.
- Load .schem files from WorldEdit 7 (v2) and 7.3+ (v3) and from FAWE: blocks, chests with items, signs
  and entities appear; the placement origin matches where the copy was made from (//copy position).

## Modern vanilla Litematic import

Use copies of files and disposable worlds. This section tests import and preview;
it does not require pasting into the world or changing the export format.

1. In native GTNH 2.8.4 and 2.9.0-RC-2, import vanilla files with pre-component
   items, 1.20.5+ item components, and 1.21.5+ NBT text/equipment. Record the root
   `Version`, `SubVersion` and `MinecraftDataVersion`; container version alone does
   not identify the item or text layout. Keep source hashes before/after loading.
2. Include independent regions, negative dimensions, explicit air and a palette
   large enough to cross packed-long boundaries. Check bounds, region origins,
   positions and metadata for stairs, slabs, doors, trapdoors, beds, redstone,
   logs, quartz pillars and giant mushroom caps/stems.
3. Include a named chest with colored/wooden block items, half slabs, damaged
   anvils, repeaters/comparators, an enchanted damaged tool, dyed armor, a splash
   potion and a written book. Check item registry IDs, metadata, counts, slots,
   display names, enchantment IDs/levels, dye color and readable book pages.
4. Check old JSON sign text and new front-text compounds, including Chinese text.
   Verify furnace timers and synthesized note-block pitch, skull type/rotation
   and potted-plant data when the modern file has no corresponding block entity.
5. Import an equipped named zombie using both `HandItems`/`ArmorItems` and the new
   `equipment` compound. Check main hand/head and empty intermediate slots,
   local entity coordinates and custom name. Do not infer passenger support.
6. Include an unknown item, unsupported component, sign back text, a new note
   instrument and offhand equipment. Loading must retain supported content and
   report omissions; it must not silently replace missing items with stone.
7. Inspect the preview and material list in each pack. Preserve screenshots and
   the imported NBT checks separately. Constructed files test their encoded
   layouts, not every block/entity or an actual upstream client's export path.

Limits: conversion targets representable 1.7.10 vanilla content. New gameplay,
rich text, back-side sign rendering, arbitrary riders and unknown mod content are
not covered. Read compatibility/README.md before treating a warning as a crash or
assuming that a successfully loaded file preserved every modern feature.

2026-10-13 automated and native observations:

- Build and UTF-8 JUnit run passed: **637 tests**, including 22 new regressions
  for item forms/components, text, equipment/timers and mushroom faces.
- Constructed gzip files with real packed long arrays covered these layouts in
  both native packs: 1.13.2 (data version 1631/container 5), 1.20.4 (3700/6),
  1.20.6 (3839/6), 1.21.5 (4325/7), 26.1.2 (4790/7) and 26.3 (5023/7).
  These are versioned fixtures, not files exported by actual modern clients.
- Native file-loader checks passed for both regions/origins, 26 representative
  block states, container data, plain/Chinese sign text, note/skull/pot data,
  old/new equipment layouts and normalized entity positions. Extra inventory
  cases checked eight slab/anvil/redstone/wood item variants. Source hashes stayed
  unchanged; no world paste was performed.
- The omission fixture retained supported data and reported one removed
  netherite item plus unsupported trim, sign back text, note instrument and
  offhand equipment. The warnings appeared in native chat.
- Inspected preview and material-list screenshots in both packs. In 2.8.4,
  copied imported stacks were also displayed in native inventory/book screens;
  the page showed `Imported page`. These screens used a detached inventory and
  stack copies, not an interaction with a placed chest.
- Runtime artifact SHA-256:
  `556bfc5ee7e87892c2483a1f4990d389e5564b830a2fdf9c78cc79f268014300`.
  Evidence: `<evidence-dir>/modern-litematic-import/`, including `final-284-*.log`,
  `final-290-*.log`, versioned fixtures and `screenshots/`. An RC-2 startup attempt
  hit a BetterLoadingScreen/Angelica mixin initialization race before import;
  restarting the same artifact completed the acceptance checks.
- Actual modern-client exports and every vanilla block/entity combination remain
  unverified. No modern mod-content or new export compatibility is claimed.

## Public upstream import fixtures

Use unchanged copies of public files, record their source commit and SHA-256,
and compare hashes again after loading. Run in native GTNH 2.8.4 and 2.9.0-RC-2.

1. From [Litemapy](https://github.com/SmylerMC/litemapy/tree/e830fcd6c4c518adc097ac215e3390470a5c4686/tests/litematics/valid),
   import `Lite.litematic`, `SimpleHouse.litematic`, `Subversion.litematic` and
   `ArmorStand.litematic`. These cover v5/data version 2586 and v6/subversion 1/data
   version 3465, negative region sizes and multiple regions. Expect respectively
   122/78/8/9 non-air blocks and 4/1/1/1 regions. The armor stand has no vanilla
   1.7.10 counterpart: require an omission warning and retain the nine stone blocks.
2. Inspect the house preview and material list, not just the block count. Its
   large fern must stay a large fern: lower block metadata 3, upper metadata 8,
   exactly one `double_plant` item with damage 3. Stairs, slabs and the door must
   retain their shapes. Do not paste or re-export these Litematic fixtures.
3. From [FAWE](https://github.com/IntellectualSites/FastAsyncWorldEdit/tree/fbf5c91c5de175eb3f55ef8521a3e09c56950778/worldedit-core/src/test/resources/fastasyncworldedit/schematics),
   import `sponge2.schem` and `sponge3.schem`. Their metadata identifies FAWE
   `2.13.2-SNAPSHOT`, Bukkit-Official and data version 4440. Both are 2x1x2 and
   contain gravel, grass block, granite and sandstone. In a disposable creative
   world with a supporting floor, paste each and inspect the actual blocks with
   schematic rendering disabled. Compare target registry names/metadata; modern
   granite uses the existing stone fallback rather than a new 1.7.10 block.
4. The same FAWE directory's `sponge1.schem` is a format-detection placeholder
   with an empty palette/stream. Do not treat it as a valid complete volume or
   weaken stream validation to accept it.

Native observations on 2026-10-13:

- An external harness invoked native loading, material scans and server paste;
  screenshots supplied separate visual checks. Six unchanged public files passed
  import in both packs. The four Litematic imports retained the expected block/region
  counts, reported the unsupported armor stand, and were not pasted or re-exported.
- The house exposed a real regression: the large fern became a sunflower in both
  preview and materials. `BlockStateTranslator` now supplies the lower-half species
  while keeping upper-half metadata 8. Automated tests cover all six species with
  lower/upper states, propertyless names and ignored properties. Cold native clients
  passed exact fern block/material assertions; both corrected preview/material
  screens were inspected.
- Both FAWE v2/v3 files passed full server paste and target block/metadata checks
  in each pack. Actual world views were inspected with previews disabled. This
  verifies these four-block files, which contain no tile or entity data.
- Build and UTF-8 tests passed: **675 tests**, no failures or skips. Evidence:
  `<evidence-dir>/upstream-exports/` (`provenance.json`, `accept-{284,290}.log`,
  unchanged `fixtures/`, before/after screenshots). Runtime JAR SHA-256:
  `fcbaaedd98314dc884e85063529386d920c4d9e45cac926c0097e96416aff68f`.

Litemapy samples are public upstream fixtures, not exports produced by a modern
Litematica client launched in this environment. A fresh WorldEdit 7.3.8 source
build stopped at unresolved `fabric-loom:1.6.12`; dependency resolution prevented that
exporter run. Fresh modern-client exports with inventories/entities and a complete
real Sponge v1 export remain unverified. No new export support was added.

## Sponge block-stream validation

- In both target packs, load valid Sponge v1/v2/v3 files with asymmetric stone,
  stairs and air positions. Compare dimensions, copy origin, block positions and
  orientations; rotate and reload. Keep valid files and worlds unchanged.
- On disposable file copies, remove all block bytes, leave one complete index,
  or truncate after a complete multi-byte index before the declared volume ends.
  Loading must report an error without adding a source or placement. Existing
  loaded sources and placements must remain usable.
- Repeat with extra trailing indices/bytes, an unfinished or overflowing varint,
  an unknown index, negative/oversized/non-integer palette indices and duplicate
  palette IDs. All must fail explicitly; then load a valid file successfully.
- Use the browser's import/conversion action on the same malformed copies. It must
  not produce a normal-looking partial output file with missing cells filled by air.

On 2026-10-12, six new automated regressions failed against the preceding reader.
The fixed reader passes eight Sponge tests spanning v1/v2/v3 and all 615 project
tests; build and Checkstyle pass. These are automated fixtures, not a claim of
native WorldEdit/FAWE export or game acceptance.

The same day's running-client reader probes in both target packs also checked
12 constructed NBT cases each: v1/v2/v3 declared four stone cells, with block
streams containing zero, one, four or five indices. Four indices loaded correctly;
short and trailing streams were rejected. This exercises native game classes,
but does not replace browser, placement or real WorldEdit/FAWE file acceptance.

File-level probes then exercised `SchematicGuiLoader.load` in both native games.
For each version, a compressed four-cell stone/air file loaded into a placement;
empty, short, trailing and overflowing streams failed without changing the
loaded-source count, placement count or active placement. All 15 input files
retained their original checksums. These are constructed files and direct loader
calls, not mouse-driven browser/conversion checks or real WorldEdit/FAWE exports.
Evidence: `<evidence-dir>/sponge-nei/sponge-files290.log` and
`cold-verify284-final.log`.

## Tool HUD, new generic/visuals options and boolean toggle hotkeys

- Hold the tool: the Tool HUD text is in the bottom left with a dark background per line and shadowed
  text like Litematica (block, mode, selection/placement lines, `Mode [n/9]`); toolHudAlignment,
  offsets and scale move/resize it; it hides while a screen is open or F1 is active.
- Visuals tab: rows such as Enable Schematic Rendering or Schematic Overlay Type Missing show a value
  button, a keybind button and the keybind settings button. Bind one, press it in game: the option flips,
  the action bar shows the MaLiLib toggle message and the schematic re-renders; Reset clears the binding
  and the value. These hotkeys are not listed in the Hotkeys tab.
- Bind schematicEditReplaceSelection with another key held: it still fires (modifier settings) and the
  other key keeps working.

## Verifier parity follow-ups

- Verify a placement over water/lava with ignoreExistingFluids on: water where the schematic has air is
  not listed as Extra; blocks named in extraAirBlocks likewise.
- Verify with Render Layers range, then change the layer (or enable layerModeFollowsPlayer and walk):
  the results stay; moving the placement resets with "The placement changed".
- With two placements verifying and both HUDs on, the info HUD shows "Schematic Verifier, remaining
  chunks (n)" and the closest chunk lines for each; after finishing, selecting one category shows its
  name as the title, several categories show "Schematic Verifier errors"; the lines have the dark
  per-line background like the Tool HUD.

## Printer switch and server permission

- Single player: bind printerWorkingSwitch (Hotkeys tab, unbound by default) and press it with a placement visible: the action bar shows the
  printer toggle message and blocks are placed around you; press again to stop. Changing the selected
  placement keeps printing (with the new placement).
- Turn off Allow Printer (printerEnabled) in the config screen while printing: printing stops with
  "The printer is disabled on this server"; pressing the key again only shows that message. Turn it on
  again and the key works.
- Dedicated server with printerEnabled=false: the key shows the disabled message; on a server without
  Schematica Plus the printer works.
- Die while printing: the printer turns off with a message. Reconnecting always starts with it off.
- Single player now receives the server capabilities: Easy Place uses the accurate placement protocol
  (stairs/pistons get the schematic facing without turning).

## Extended printer distance

- With a fresh server configuration, or after removing only `printerMaxRange`,
  start the server and check that the generated value and advertised cap are 128.
  An existing explicit value such as 32 must survive a restart unchanged.
  The default changed to 128 on 2026-10-15; build and 705 automated tests passed.
  No new game run was requested for this default-only change.
- Use the same current JAR on both sides. In Hotkeys, bind the printer working
  switch by searching `printer`, close/reopen configuration, and confirm the
  binding survives. Also start with an existing `workingSwitch` chord and advanced
  settings: `printerWorkingSwitch` must retain both after saving/reopening. Check
  that its advanced matching/reset controls work and GUI typing does not print.
- Leave `printerWorkRange=0`: ordinary reach remains the default. Set it to 128
  with server `printerMaxRange=128` and a loaded schematic extending past 128 blocks.
  Keep the target chunks loaded on both sides. From a fixed position, print survival
  blocks at 32, 64, 96 and 127 blocks; confirm
  server blocks, exact inventory consumption, and rejection beyond the limit.
- Reduce server `printerMaxRange` to 16 while connected. The client must receive
  the limit and leave farther blocks untouched. Disable `printerEnabled` while
  printing; it must stop, and direct requests must also be rejected.
- Include a wrong block at 12 blocks: enable breaking and confirm normal mining
  progress, drops/tool wear and replacement. Toggle off mid-dig, walk outside the
  limit, and check cancellation. Ordinary clicks must retain normal reach.
- Reject out-of-range, unloaded-chunk and protected-block requests; exercise native
  placement/break event cancellation with installed protection mods where available.
  No inventory item or block NBT comes from the printer request.
- Set a 1 ms scan budget and use the 128-block cube: distant offsets must eventually
  be visited. Try top-down/axis-reversed traversal, chunks loading after a scan,
  reconnecting and changing dimensions. Large scans resume over successive passes;
  layer sorting applies to each collected batch.
- Without Plus on the server, or with server `printerMaxRange=0`, the effective
  printer radius stays at normal reach. Fluid containers continue using native
  ray tracing and normal reach, including on a server allowing extended printing.

Observed 2026-10-15 in native GTNH 2.8.4 and 2.9.0-RC-2 clients connected to
matching dedicated servers, with the same current JAR on both sides (SHA-256
`ff982fc917fbf2afbdd374f0879c745621bc761bb7ac573bafb803ff1eaa5342`):

- The Hotkeys page showed the printer working switch; a real mouse/keyboard F8
  binding survived closing configuration and a cold client restart. F8 started
  and stopped printing in the world.
- Generated survival fixtures placed stone at 2, 12, 24 and 31 blocks while the
  33-block target stayed air. A 20-block stair retained metadata 6. Server readback
  and client inventory agreed: stone 64 to 60, stairs 8 to 7. A wrong dirt block
  at 12 blocks was mined and replaced, consuming one further stone and wearing
  the selected tool. The ordinary server reach returned to 5 after requests.
- A live server limit of 16 reached the client and rejected a direct 24-block
  request. Disabling the printer stopped it and rejected a direct 12-block
  request. Server limit 0 restored the client's ordinary 4.5-block radius.
- With stone selected, an ordinary native placement packet at 12 blocks was
  rejected while the printer request succeeded. A synthetic Forge interaction
  cancellation fixture refused placement without consuming stone; interaction
  and break-event cancellation also kept the protected wrong block intact.
- At a 1 ms scan budget and cube shape, every in-range stone target was eventually
  visited while the 33-block target stayed air. F8 in chat did not start printing.
  Fixtures were prepared through runtime APIs; key capture/toggling used native
  mouse/keyboard input. Screenshots were inspected and results read from the
  actual dedicated worlds. This is not acceptance of every protection plugin.
- An iron hoe refused both ordinary nearby native use and the extended request
  in these packs; dirt stayed dirt. No successful tilling claim or workaround
  follows from this fixture. Fluids and arbitrary mod-private reach checks remain
  outside the extended-distance acceptance.

Observed 2026-10-15 for the 128-block update in both native packs, using matching
current clients and dedicated servers (JAR SHA-256
`e22561693592fb4be7cfa7ff9932fb62aa537b40b184231c74a1f660a3e40f57`):

- Servers loaded `printerMaxRange=128` from configuration and advertised 128.
  Clients still default to ordinary reach unless configured.
- Existing `workingSwitch=F8` files loaded as `printerWorkingSwitch=F8`. Native
  GUI input found the binding by searching `printer` in Chinese; saving retained
  F8 under the new name. Native F8 input toggled printing, and F8 in chat did not.
  Screenshots of the search results were inspected in both packs.
- With target chunks loaded, a generated survival fixture and a 1 ms cube scan
  placed stone at horizontal offsets 2, 32, 64, 96 and 127; the 129-block target
  stayed air. The 20-block stair retained metadata 6. Actual server readback and
  client inventory matched: stone 64 to 59, stairs 8 to 7. Ordinary server reach
  returned to 5. Screenshots of the resulting worlds were inspected.
- Reducing the live server cap to 64 updated the client and rejected a direct
  request at 127 without consuming stone. Restoring 128 allowed it; a direct
  129-block request was still refused. Cap 0 restored ordinary client reach (4.5).
- In a reset stone-only fixture with printing stopped, one explicitly invoked
  pass at `placeBlocksPerTick=3` placed exactly three targets and consumed three
  stone. A second pass at limit 1 added exactly one target and consumed one more.
  These are per-pass checks, not a measurement of tick rate or a strict limit on
  multi-block structures. Fixture setup and readback used native runtime APIs.

Build, Checkstyle and all 705 tests pass, including lazy 128-radius indexing,
shape/order/reversal clipping and saved-binding migration. Initial launcher-free
logins required reconnects after timeouts; acceptance used established connections.
Evidence: `<evidence-dir>/printer128` (`accept-290.log`, `accept-284-final.log`,
`hotkey-*` and `printed-*` screenshots). This round does not extend the earlier
fluid, private item-distance or protection-plugin coverage.

## Printer pass (litematica-printer style)

- Printer tab: printer options and swap slots; the printerWorkingSwitch binding is in Hotkeys.
- Default settings: standing next to a placement the printer places one block per tick, bottom layer first,
  only within reach; several visible placements are all printed.
- Stairs, chests, furnaces, pumpkins, repeaters, doors and horizontal pistons/dispensers get the schematic
  facing without turning (also on a vanilla server); the view does not jump. Upside-down stairs, top slabs,
  open-top trapdoors and logs on all axes are right, also floating in the air (Place In Air on).
- Place In Air off: floating blocks are skipped. Torches/ladders/buttons without support and sand over air
  are skipped; with Falling Block Check off sand is placed and falls.
- Iteration Order YXZ / reversal, Work Area Shape and Work Range 2 change which blocks go first and how far.
- Print Range: Area selection prints only inside the selected area; Below/Above the player limits by feet.
- Skip Blocks with minecraft:stone: stone is never placed. Always Sneak: no container opens when clicked.
- Lag check: on a server, freeze the connection (e.g. suspend the server process): printing pauses after
  about a second and continues afterwards. Pause While Moving stops printing while walking.
- `placeBlocksPerTick` accepts 1..64 targets per pass (default 1). `placeDelay`
  is the interval in client ticks; 0/1 runs each tick, 2 runs every other tick.
  With printing stopped and a fixture of simple stone targets, invoke one printer
  pass at limit 3: exactly three targets should be attempted successfully, with
  matching server blocks and item consumption. Repeat at limit 1. Double slabs
  may require an extra click, and doors/plants can create multiple blocks from
  one target; this is not a strict count of world cells or network packets.
  Position Cooldown keeps failed spots from being retried every tick.

## Menu focus, first frame, pick block on entities, data folder

- Open the main menu with Schematic VCS hidden: no VCS button flashes. Open any menu: no button is
  highlighted until the mouse is over it; Tab still moves keyboard focus; screens with a text field focus it.
- Creative, a schematic block behind a cow: middle click on the cow gives a cow spawn egg.
- The schematics folder no longer shows area_selections/area_selections_per_world; they, AreaSelection.json,
  LoadedSchematics.json, RenderLayers.json and SchematicProjects.json are in config/schematica_plus and
  existing ones are moved there on first use (selections, loaded placements and layers are kept).

## Selection and placement boxes like Litematica

- Area selection: the selected box has red/green/blue edges along X/Y/Z, other boxes white edges (1.5 px,
  3 px with a project open); corner 1 red and corner 2 blue block outlines (2 px), the selected corner cyan
  with translucent sides; both corners at the same block: a red/blue/magenta outline. All are hidden behind
  blocks (depth tested) like Litematica. The manual origin is orange, cyan when selected.
- Placements: each placement gets its own color (Litematica's Kelly palette, kept after restarting); the
  selected placement's boxes are cyan; 1 px edges, corner outlines and a 2 px origin outline; disabled
  sub-regions show no box; the enclosing box uses the placement color.

## Remembered UI state

- Change the config tab, the "Create placement" checkbox of the load screen and the directory of the load,
  save and manager browsers; restart the game: all come back (config/schematica_plus/UiState.json).
- Switch the tool mode, leave and rejoin the world (or restart): the mode is restored; another world keeps its own.
- A new Simple mode selection is named "Unnamed" like Litematica.

## Printer breaking and highlights

- Break Extra Blocks: survival, dirt inside a placement where the schematic has air: the printer mines it
  at normal speed (crack animation), then continues; water/lava are left alone. Walking out of reach stops.
- Break Wrong Blocks: stone where the schematic wants planks is mined, then planks are placed; tall grass
  and snow layers are simply replaced; a bottom slab under a schematic double slab is completed, not mined.
- Break Wrong State Blocks: a wrongly rotated stair is mined and placed again with the right facing.
- Highlight Printer Blocks: placed blocks flash white, mined red, unplaceable (no item/no side) gray, fading
  over Highlight Fade Time; Highlight Through Walls shows them behind blocks.
- Turning the printer off while mining stops the mining.

## Container verifier and Fill button

- Paste or load a schematic with filled chests, a double chest, a furnace, a hopper and a dispenser; place the
  empty blocks in the world and open them: empty slots show the schematic's items faded with a light blue
  outline, items the schematic does not have a magenta outline, other items red, the right item with another
  count or NBT orange; hovering a slot shows "Schematic: 64 x Cobblestone" (or empty). Correct slots stay plain.
- Fill (right of the GUI, left of it when there is no room): items from the main inventory (then the hotbar)
  are moved in; partial stacks are split, the rest goes back; wrong items stay; the action bar shows how many
  slots match. With an item on the cursor it asks to put it down first. Works on servers (normal window clicks).
- Both features can be turned off in the Generic tab (Container Verifier, Container Fill Button). Containers that
  are not in a visible placement, the player inventory and the creative inventory are unaffected.
- Every schematic browser (load, manager, save, projects) has a folder icon after "create directory" that
  opens the current folder in the system file manager (Windows Explorer, macOS Finder, Linux file manager).
- Bind refreshMaterialList: pressing it recounts the last viewed material list (else the selected
  placement's) and the action bar says so; the material list HUD updates.

## Material list: marks, replacements, storage, all placements

- Each row has ☆ (mark), Replace and Ignore. Marked rows (★) stay on top whatever the sort.
- Hold birch planks and click Replace on oak planks: the oak row disappears and its counts are added to a
  birch planks row (available = birch in the inventory); the tooltip lists "Includes: Oak Wood Planks". Right
  click Replace on the birch row (or click with an empty hand) to undo. The HUD, Write to file and raw
  material export use the replaced rows.
- Hover a row: Total and Missing with stacks, Storage "0.19 chests / 0.09 double chests".
- Marks, replacements and ignored rows of a placement's list survive closing the screen, reloading and
  restarting (saved with the placement); Clear ignored clears the ignored ones.
- With two or more placements, "All placements" opens one list adding up every enabled placement; it
  recounts when a placement is moved, transformed, added or removed.

## NEI (GTNH) material demands and bookmark groups

Use GTNH 2.8.4 / NEI 2.8.44 and GTNH 2.9.0-RC-2 / NEI 2.8.155. The former has
bookmark crafting chains; the latter also has the graphical crafting tree.

- Cold-start the client and send a material list before NEI finishes loading its bookmarks. The notice
  asks to retry shortly, without creating or saving a temporary group. Retry after loading: the new
  group stays present when a recipe screen is opened. Existing bookmarks must remain intact.
- Material list -> Send to NEI creates a saved demand represented by a named book. This is a planning
  node, not a craftable item or supplied inventory. Its ingredients are the missing block amounts;
  Shift sends totals. Check a partially built placement, a multiplier greater than one, ignored rows
  and a material replacement. Zero demand must not create an empty group; excessive amounts must
  report the export limit instead of silently truncating the list.
- Send materials from both the file browser and a placement. The book must use the source filename,
  including `.schematic`, `.schemplus`, `.litematic`, `.schem` or `.nbt`; include a Chinese filename.
  Renaming a placement must not replace the source filename with the placement name. Lists without
  a single schematic source use their list name. Save/reload bookmarks and reopen the demand recipe:
  the book name, extension and original material counts must survive. Send identical materials from
  differently named files and confirm that each saved group retains its own book name.
- Use 17 iron bars and three iron blocks as a fixture. Select the shaped iron-bar recipe producing
  three bars from six rods. Click S: the chain must request 36 rods and produce 18 bars (six batches),
  while still requesting the three iron blocks. Repeated S must not duplicate the recipe.
- In 2.9, open the group's crafting tree, click its iron-bar node and use the native recipe-tree arrow.
  Repeat on its rod node with the one-rod-from-one-ingot recipe. The tree must expand to 36 ingots,
  retain the three iron blocks and show one surplus bar. Tool choices and alternatives follow NEI.
- Send a second list with different counts but the same material types; both plans must retain their
  own counts. Save/reload NEI bookmarks and restart the client; requests, recipes and calculated
  amounts must survive. Open the saved demand book's NEI recipe to inspect the original quantities.
- Switch bookmark namespaces while an old S button is still on screen. It must not add to another
  group with the same numeric ID. Return to the original namespace and retry. Delete the group and
  create another with a reused ID: the old S button must not target that replacement. Leaving the
  world clears the last-send reference. Saved groups remain usable through NEI's own controls;
  send a new material list to bind S again.
- Export a demand containing modded items in 2.8.4 and read the snapshot in 2.9.0-RC-2. Registry names,
  metadata, item NBT and amounts must survive changed numeric item IDs. Missing named items must
  not be mistaken for unrelated items occupying the old IDs.
- Without GTNH NEI, the optional button must not appear and normal materials/recipe screens must
  remain usable. This integration selects recipes manually; it does not choose a GT machine route,
  perform crafts or replace NEI's native tool, fluid, byproduct and ore-dictionary calculations.

### NEI demand book filenames

On 2026-10-13, both native clients passed file-browser exports for `.schematic`,
`.schemplus`, `.litematic`, `.schem` and `.nbt`, including `原理图.schemplus`.
Native mouse clicks on Send to NEI created seven groups per pack: five file
formats, a renamed placement and a custom list. Each retained the expected name
and three-stone demand after native bookmark-file save/reload. Reopening demand
recipes succeeded; later groups did not change earlier books with identical
materials. Both Chinese book tooltips were visually inspected. Source-less custom
lists used their list name. Previously saved generic books need to be sent again
to acquire a source filename; they did not store one when created.

Build, Checkstyle and all 615 automated tests passed. Tested runtime JAR SHA-256:
`1920a276611387c6992f52ae3f75fc74e5e7e3bb82608690e73efbff2b97f228`.
Evidence: `<evidence-dir>/nei-book-name/verify*.log`, `post*.log`, saved bookmarks
and `screenshots/nei-*-book-tooltip.png`. This run reloaded native bookmark files;
the preceding complex-chain run separately covered full client restarts.

### Complex NEI chains in both native packs

Use separate disposable bookmark files in each pack. Compare each Plus demand
with an ordinary NEI bookmark group containing the same production recipes and
batch counts. Recipe availability and probability handling differ between packs.

- Send 17 iron plates from the material GUI and click the real S button on the
  fluid-solidifier recipe. Expect 2448 mB molten iron and a zero-consumption plate
  mold. The mold is still required equipment; zero consumption does not supply it.
- Repeat with the metal bender (17 ingots, circuit 1 with zero consumption),
  extruder (17 ingots, zero-consumption plate shape), forge hammer (27 ingots,
  18 plates) and cutting saw (two iron blocks, 104 mB water, 18 plates).
  In 2.9, switch the saw's fluid alternative to lubricant using NEI and compare
  the recalculated amount with a native group.
- Select the hand-hammer recipe, then change its single-use hammer alternative
  to the generic GT hammer. Compare tool counts with native NEI; a generic tool
  placeholder does not establish the durability of a particular crafted tool.
- Request 17 iron dust through brown-limonite centrifuging. Preserve all outputs:
  17 iron dust, 34 oxygen cells and 17000 mB hydrogen, using 68 brown-limonite dust
  and 34 empty cells. Also request iron dust as the secondary, probabilistic
  output of impure-magnetite centrifuging; compare with each version's native math.
- Chain iron plates through the bender, iron-dust smelting and brown-limonite
  centrifuging. Check intermediate amounts, zero-consumption circuit, fluid and
  cell byproducts. Inspect the expanded graphical tree in 2.9.
- Add both gold-nugget/ingot alloy-smelter conversions to a 17-ingot demand.
  Recalculation must terminate, retain the seed material requirement, and match
  the native group rather than supplying free material from the cycle.
- Reload handler information and reopen the demand recipe. Its classification
  must identify Schematica Plus with a book icon, without an Unknown label or
  crafting-overlay button. Save bookmarks, cold-restart and reopen the world;
  quantities, alternatives and recipe lookup must survive. S must remain unbound
  until a new material list is sent.

On 2026-10-12, both native clients completed ten saved groups: fluid solidifier,
bender, extruder, forge hammer, saw, hand/generic hammer, brown limonite,
probabilistic magnetite byproduct, gold conversion cycle and the three-stage
plate chain. Quantities matched ordinary NEI control groups. Native mouse clicks
exercised material export and S; further cases used the real handlers, bookmarks
and chain math on the running client's main thread. Demand/recipe screens in both
packs and the 2.9 fluid and multi-stage trees were visually inspected. Handler-info
reload preserved the book icon and classification, without a crafting overlay.

In 2.9 the saw's lubricant alternative used 26 mB for two batches instead of
104 mB water. The 11.11% magnetite byproduct needed 154 batches for an expected
17 iron dust; 153 yielded a displayed 16. In 2.8.4 native NEI instead used 17
batches, ignoring that probability. Both native versions displayed 17 generic
hammer placeholders for the 17-craft hand recipe. Plus matched those native
limits; this does not validate concrete-tool durability or guarantee random yields.
The gold cycle terminated and retained a 17-ingot seed requirement. Bookmark
file reloads preserved all ten groups and resolved each demand recipe.

Cold client restarts in both packs retained every group's amounts, alternatives
and recipe lookup. NEI's native quantity edit doubled the multi-stage request to
136 brown-limonite dust and 68 empty cells; reverting it restored the entire
previous calculation. S was correctly unbound after rejoining. The restored
demand pages and 2.9 multi-stage/cyclic trees were inspected, including a native
cyclic control group. Startup probes waited for both the player and NEI bookmark
loading; recipe-handler readiness alone does not establish bookmark readiness.

Build, Checkstyle and all 615 automated tests passed. Tested runtime JAR SHA-256:
`c86b8f52a56fb398b96ccd71177a756b06bb9020ae29ca6ae18714a8921f2417`.
Evidence: `<evidence-dir>/sponge-nei/final-suite284.log`,
`final-suite290-retry.log`, `checks284.json`, `checks290.json` and inspected
`screenshots/nei-*-final-*.png`; cold-restart evidence is in
`cold-verify284-final.log`, `cold-verify290-final.log` (through its cold-restart
pass marker), `acceptance.json` and
`screenshots/nei-*-cold-*.png`. These are named planning fixtures, not exhaustive
coverage of every GT machine recipe or automated execution of the planned crafts.

## 3D schematic preview in the browsers

1. In each target GTNH pack, select a supported schematic with an embedded image.
   The Image/3D slider must switch by clicking either half or dragging the thumb;
   keyboard Left/Right and Space work only when the switch has focus. With no
   embedded image the switch is absent and 3D is used. The existing replacement
   option determines the initial choice, not whether switching is possible.
2. Click the model to select it and pause automatic rotation. Left-drag rotates;
   right-drag pans with the cursor. Wheel zoom works only over the model; wheel
   outside it still scrolls the information panel or file list. Test a partially
   scrolled viewport: controls and model must stay within the panel's clipping.
3. With the viewport selected, hold W/S to move forward/back, A/D to move left/right,
   Space to move up and either Shift to move down. Movement is relative to the
   view. Inspect an asymmetric hollow fixture at close range. Observe the actual
   player position before and after: only the preview may move.
4. Click the search field and type WASD/Space: the field receives text and the model
   stops moving. Repeat while a rename/confirmation modal is open, after switching
   to Image, and after leaving the screen or changing files. Release held keys
   before selecting a viewport; returning focus must not resume an old movement.
5. Pause freezes the current angle; Rotate resumes it. Reset View restores the
   original position, zoom and angles while keeping the current pause/rotation
   setting. Move beyond the model, then reset to recover it.
6. Expand opens a full-window 3D view with the same camera and controls. Test
   movement, drag, zoom, reset and image switching there. Escape/Back returns to
   the browser with the camera preserved; it must not close the browser or unload
   a placement. Repeated opening/closing must leave other menus and textures intact.
7. Repeat English/Chinese, small and resized windows, and GUI scales 1/2/Auto.
   Check all controls remain accessible, the model is not stretched, and input
   outside the viewport does not activate its navigation. Refresh with F5, switch
   files quickly and close/reopen the screen; no stale preview or GL errors.
8. The existing performance limits remain: files over 4 million enclosing cells
   or 32 MB do not get a 3D view. Disabling 3D keeps an embedded image available.
   Chests/signs and other blocks drawn only by tile entity renderers remain absent
   in this lightweight browser renderer. This is separate from placed holograms.

Dated observations (2026-10-15): build, Checkstyle and all **694 tests** pass.
Ten new regressions cover view-relative navigation, diagonal speed, bounded zoom
and travel, reset/pause behavior, complete perspective framing in landscape and
portrait viewports, and slider capture/focus/modal isolation.

Native GTNH 2.8.4 and 2.9.0-RC-2 were cold-started with runtime JAR SHA-256
`701fa80358ba700290cd7d99a645f818afe2333b27d62f83b3f120185fdca16b`.
The fixture is a constructed 17x9x17 `.schemplus` wool/glass enclosure with a
stone-brick floor and gold pillar, saved with and without an embedded image.
Real mouse/key input passed W/S/A/D, Space, both Shift keys, left rotation,
right-drag pan, wheel zoom, pause/resume and reset. The actual player coordinates
stayed unchanged during preview navigation. Expanded views retained camera state
on return. Native image-slider dragging, no-image files, search typing and a real
file-rename modal passed; cancelling the modal wrote no file. Repeated expand/back,
F5, Chinese/English controls, GUI scales 1/2/Auto and 640x480/854x480/1280x720
windows passed. File hashes stayed unchanged, and ordinary world movement and the
M menu worked after closing the preview. Screenshots of the model, pan, expanded
views and Chinese layouts were inspected. These runs used disposable world copies,
Java 21/LWJGL 3, software GL and disabled shaders; they do not establish additional
OptiFine, shader or physical-GPU coverage.

Both packs were then cold-started with final runtime JAR SHA-256
`ea129ce23171d566ae76344d02d81a95be1ab2cf044f3190a351d726ade02e5d`, which
adjusts the embedded control margins and fits the initial perspective view around
the entire model. Native checks confirmed every control stays inside the info
panel at all three window sizes, full-model framing after reset, movement into
the enclosure, right-drag pan, return from the expanded view, image/no-image
switching and unchanged schematic bytes. Final Chinese screenshots were inspected,
including `checked-image-640-290.png`, `checked-model-290.png`,
`checked-fullscreen-284.png` and both `checked-closeup` images. Evidence is under
`<evidence-dir>/preview-navigation`; a virtual window extending beyond the display
and a test-helper reload race were corrected before the successful final reruns.

## Printer missing material HUD and auto tool

- Survival, printer on, a placement needing stone and glass with only stone in the inventory: in the info HUD
  corner (below the material list HUD if shown) "Missing: 1 types" with a glass icon appears while the
  printer works; picking up glass makes it disappear about 3 seconds later. Open a chest: the list shows
  there too. Turn the printer off: the list goes away at once. More than 10 kinds: "...and N more".
- A water source in the schematic without a water bucket: Water Bucket is listed.
- "Missing Material HUD" off: nothing shown.
- With "Break Wrong Blocks" on, a stone block where the schematic has dirt, a pickaxe in hotbar slot 5 and a
  block held in slot 1: the printer switches to the pickaxe, mines the stone at pickaxe speed (no restart of
  the crack animation), then goes back to slot 1 and places the dirt. Dirt where the schematic wants stone:
  a shovel is picked over the pickaxe. Obsidian with an iron and a diamond pickaxe: the diamond one.
- Pickaxe only in the main inventory with swap slots configured: it is moved into a swap slot and used;
  with no swap slots it is not used.
- A pickaxe with 10 or fewer uses left is skipped (mined by hand or another tool); with the protection at 0
  it is used. "Auto Tool Selection" off: the held item is kept.

## Unknown blocks on import

- Load a .litematic or .schem from a newer version or with blocks of a mod that is not installed: chat shows
  "<file>: N block types this game does not have were replaced:" and up to 8 lines "name[state] -> minecraft:stone"
  (more: "...and N more"), the log has the full list. A .schematic/.schemplus saved with a mod that is missing
  now: its names are listed with "-> minecraft:air".
- Loading the same file again while it is loaded (a second placement): no repeat. Reload from the loaded
  schematics list: listed again. A file with only known blocks: no message.

## Align to Map

- Load a 128 x 128 (one layer) map art, open its placement configuration and click "#" next to Move to player:
  the placement covers exactly one map (check with a map item: the border of the drawn area matches the
  schematic box). A 128 x 129 map art: the extra north row lies one block north of the map.
- Rotate the placement 90 degrees and align again: still aligned. With X locked only Z moves; with X and Z
  locked or the placement locked the button is disabled. Y never changes.

## Doors, beds and double plants

- Printer, survival, no Plus server: a schematic with doors in all four facings, beds in all four directions
  and sunflowers/tall grass/lilacs: each is placed once from its lower block/foot (never a door whose lower
  half sits at the upper half's position), doors face the schematic direction, beds point the right way,
  sunflowers face the schematic direction. A door next to a chest: the chest does not open (sneaking click).
- Same without a server: door hinges may differ from the schematic and open doors are placed closed; with
  "Break Wrong State Blocks" on, these doors are NOT broken again and again.
- Plus server with accurate placement: doors also get the schematic hinge and open state, for both halves.
- Easy Place: aim at a door's upper half or a bed head and use: the door/bed is placed from below/the foot;
  facing follows your own view (on a Plus server the door facing/hinge/open follow the schematic).
- A bed whose head position is blocked or has no solid ground: not placed, no item lost.

## ignoreCropAge (Litematica 0.27.12)

- Schematic with fully grown wheat, carrots, pumpkin stems, nether wart, cocoa and sugar cane; plant them
  young. Off: wrong state overlay (orange), verifier lists wrong states, material list counts them missing.
  Turn "ignoreCropAge" on and save: overlays disappear, the verifier (re-run) counts them correct, the material
  list (refresh) no longer counts them. A cocoa pod facing the wrong way is still a wrong state.
- With "Break Wrong State Blocks" on and ignoreCropAge on, the printer does not break young crops.

## Custom material lists (Litematica 0.27.12)

- Material list of a placement -> Export: a save screen listing .json/.txt files opens with "<name>.json";
  Save writes it into the shown folder (chat link opens it), saving again without Shift says it exists, Shift
  overwrites. The file has "name" and "items" with "id", "count" and "damage" for colored wool etc.
- Load screen: the .json appears in the list; selecting it shows "Material List: JSON / Name / Item Count"
  instead of schematic info, Load and Rename Schematic are disabled. Material List (or double click) opens
  "Custom Material List: <name> (<file>)" with every item missing; Available follows the inventory; the info
  HUD toggle works. Edit the file and press Refresh: the list follows the file.
- A text file "minecraft:cooked_beef 64" / "# comment" / "minecraft:wool@14 3" opens the same way; an upstream
  (1.21) JSON without damage opens with damage 0; unknown items are left out (incomplete hint shows them).
- A schematic project .json shows plain file info; Material List says it failed to load the custom list.

## Container scan and cache order (Litematica 0.27.12)

- New material lists sort by name. Turn on materialListContainerScan, open a chest with cobblestone and glass,
  close it, open the material list: cobblestone and glass are on top (the last opened container first).
- Clicking the Item column header cycles name, cache order, cache order reversed (arrow on the Item column).

## Verifier and disabled sub-regions (Litematica 0.27.12)

- Placement with two sub-regions, one disabled, built in the world (also where the disabled one is): run the
  verifier: nothing inside the disabled sub-region is listed (no "extra" blocks there); unloaded chunks that
  only the disabled sub-region touches are not waited for. Enable it again: the verification restarts and
  covers it.

## Rotation and mirroring of vanilla blocks

- Build a test schematic with stairs (normal and upside-down, all facings), slabs (top/bottom), logs on every
  axis, quartz pillars, hay, pistons/dispensers/droppers/hoppers in all directions, furnaces, chests, ladders,
  wall and standing signs, wall and floor skulls, torches, buttons, levers (wall, floor, ceiling), rails (straight,
  curved, ascending, powered), doors (both hinges, open), trapdoors (top/bottom/open), vines, a huge mushroom,
  beds, fence gates, pumpkins, repeaters, comparators, cocoa, tripwire hooks, end portal frames, anvils,
  sunflowers. Rotate the placement 90/180/270 degrees and mirror it front-back and left-right: every block keeps
  its relation to its neighbors (a torch stays on its wall, rails stay connected, a door keeps its hinge side
  next to the same wall, beds stay whole). Paste it and compare; REBUILD edits on a rotated/mirrored placement
  change the right block in the source.

## GT machines under rotation and mirroring (GT5U 5.09.54.205)

- Save a schematic with single-block machines (output side set with the wrench), a few multiblocks (EBF,
  Large Chemical Reactor, an assembly line, a turbine; one controller turned with the wrench to a rotated/
  flipped ExtendedFacing), hatches incl. a maintenance hatch, an item distributor with per-side counts, covers
  (conveyor, pump, redstone covers) and strong redstone on some sides. Rotate the placement 90/180 degrees and
  mirror it: controller fronts, output sides, covers and multiblock structure previews (the in-world hologram
  after paste) all follow; pasted multiblocks form without re-wrenching.

## AE2 under rotation and mirroring (rv3-beta-1080-GTNH)

- Schematic with ME drives, interfaces, molecular assemblers, a controller, chests and a quartz growth
  accelerator in various orientations; cable buses with export/import buses, storage buses, P2P tunnels,
  terminals on walls, floors and ceilings (wrenched to different spins), facades on some sides. Rotate and
  mirror: devices keep facing their neighbors, parts stay on the side that faces the same neighbor, terminals
  on floors keep pointing the same way relative to the build; the preview cables connect to the moved parts.
  Paste: everything works without re-placing parts.

## Mod tile facings under rotation and mirroring (TileFacingAdapter)

- For each mod: place a few blocks facing different ways, save, rotate 90/180 and mirror the placement, paste:
  IC2 machines/generators/solar panels, EnderIO machines with IO modes set per side (input/output colors stay on
  the side facing the same neighbor), cap banks with IO and display faces, Iron Chests, Storage Drawers (incl.
  controller), Jabba barrels (also on walls/ceilings, with side upgrades), Avaritia compressor, Draconic chest,
  Forestry machines, Nuclear Control panels, Railcraft engines/loaders/detectors/signals, Tinkers' smeltery
  controller/drains/faucets/casting tables, OpenComputers cases/screens (also upside-down screens with y mirror).
  Each block faces the same neighbor as before in the preview and after paste.

## ForgeMultipart / ProjectRed under rotation and mirroring

- Microblock covers, panels, slabs, hollow covers, corners (nooks), edges (strips) and posts in every position;
  ProjectRed gates on floors, walls and ceilings pointing different ways, red alloy/insulated/bundled wires on
  several faces, lamps/fixtures on walls. Rotate and mirror: every microblock stays in the same place relative
  to the build, gates keep their input/output toward the same neighbors, wires stay on the same face. Paste:
  circuits work (wires reconnect after placement).

## Schematic migration across packs (ItemIdMaps / PortableSchematicData)

- Install the current Plus version in both packs. Work in disposable world copies and transfer schematic files,
  rather than opening one pack's world in another pack. Prefer `.schemplus` for complete 1.7.10 mod data.
- Save in `.schemplus`, `.schematic`, `.litematic` and structure `.nbt`, with tile/entity data enabled.
  An Alpha export requiring extended block IDs is automatically saved as `.schemplus`; use a vanilla chest
  holding mod items for the classic `.schematic` case. Include GT machines holding items in their inventories/output slots, covers, an EnderIO conduit
  with a filter, Storage Drawers, chests and a flower pot. Load it in a different pack (or a new world with other
  ids): every container shows the same items as before, no foreign items appear. Check counts, damage, nested
  item NBT, GT cover side/damage, BC pipe type, and the drawer prototype/count. Paste, save/reopen the world,
  and check again. Resave in the recipient pack and return that file to the source pack; repeat in both directions.
- Specifically migrate StorageDrawers 2.2 in GTNH 2.9.0-RC-2 back to StorageDrawers 2.1 in 2.8.4:
  populated drawers, counts above 64, named/tagged prototypes and empty locked drawers must retain their data.
  The newer nested `ItemStack` layout must not become an empty drawer in the older pack.
- Include a custom-display minecart, an Enderman carrying a block, and falling-block/moving-piston NBT with
  mod block references. Their block IDs must use block names, independently of item IDs. A missing block becomes
  air with a warning. First confirm the native entity can retain the reference without a schematic round trip;
  mapping does not lift that entity's own ID limits. Unknown mod-private numbers such as material indexes are
  outside this mapping.
- Remove a mapped item from a constructed copy, including the held item before a helmet in an equipment list.
  Expect a missing-item report, an empty held slot and the helmet in its original slot. Empty slots do not warn.
- With different block/item/fluid registries, inspect a file containing `SchematicaVisualState`: opaque binary
  adapters and tile update packets are omitted with a report; canonical NBT and named visual fields remain.
  Matching registries retain their visual cache, subject to the existing mod-version checks.
- A file without the item mapping, captured in another pack: load it in the recipient pack. Expected: a chat line
  says how many stacks were removed and names `config/schematica_plus/id_maps`; no baked potatoes or other wrong
  items. Copy the source world's `level.dat` into that folder and reload the file: a chat line names the
  mapping and the resolvable items are restored. An archived `world-*.dat` from joining that world with the current
  Plus version serves the same purpose. The source block mapping must identify the source registry; a vanilla-only
  palette cannot identify arbitrary mod item IDs. This fallback does not recover opaque mod-private indexes.
- Reloading the same file again in the same session does not change the items again.
- An existing world whose Simple selection was named "Simple selection": it now shows "Unnamed" (selection and
  box); a new Normal mode world creates "Unnamed".

**2026-10-13 — native cross-pack acceptance.** Both packs ran the same current JAR
under Xvfb/Mesa, with their native mod versions and disposable world copies.
Native server APIs prepared the source fixtures; actual Plus capture, file loading,
export and paste queues performed the migration. This was automated game execution,
with the resulting world screenshots inspected, rather than a manual build session.

| Check | 2.8.4 → 2.9.0-RC-2 | 2.9.0-RC-2 → 2.8.4 |
| --- | --- | --- |
| Capture and reload `.schemplus`, `.litematic`, structure `.nbt`, chest-only `.schematic` | Passed | Passed |
| Import and paste all four files into the native recipient world | Passed | Passed |
| Resave in the recipient pack, then reload in the source pack | Passed | Passed |
| Read saved region files and check inventories, drawer data, covers and pipe type | Passed | Passed |
| Compare every pasted block name and metadata against the source file | 86/86 cells | 86/86 cells |
| Fully stop/restart the client, reopen the world and recheck the source and four pasted structures | Passed | Passed |

The full fixture contained a chest, Storage Drawer, GT MV input bus with an UP-side
LV conveyor cover, BC cobblestone item pipe and the pack's registered chest minecart.
Checks retained chest stack counts/damage, a complete TConstruct steel pickaxe tag,
the drawer's 64 IC2 ingots and named prototype, the bus's 13 GT items and NORTH facing,
the cover item/damage, pipe type and one chest minecart. The classic Alpha case used
a vanilla chest holding the same six item types; the full RC-2 sample requires
extended block IDs and upgrades an Alpha export to `.schemplus`.
Actual registry changes included GT metaitem `7495 → 7638`, IC2 ingot `4111 → 4112`,
BC pipe item `6612 → 6753` and drawer block `3954 → 10717`. Source file hashes remained
unchanged during recipient imports and return checks.

Additional constructed files loaded in both native clients: raw StorageDrawers 2.2
`ItemStack` slots retained their item/name/count through the 2.1-compatible layout;
a missing IC2 registry name emptied the chest slot and drawer with a report;
a falling-block entity carrying white glazed terracotta retained the recipient's
block ID (`2268` or `2505`). The entity case checks native import/NBT, not a falling
entity's later world simulation. Cross-registry BC binary visual data and tile update
packets were omitted with warnings, while canonical pipe/inventory data survived.

The automated suite passes **656 tests**, including 19 new cases for nested layouts,
incomplete mappings, unsigned IDs, equipment positions, GT cover filters, typed block
references, cache validation and drawer conversion. Counts above 64, empty locked
drawers and the other typed block-reference fields have unit coverage in this change;
they are not additional native-world acceptance claims. The 2.8.4 native Enderman control
also loses the sampled high block ID without schematic loading; mapping does not fix
that native limitation. Other packs, renamed registries, arbitrary mod-private material
or machine indexes, and whole-world migration remain outside this acceptance.

Evidence: `<evidence-dir>/cross-pack-migration/` contains source/returned files,
registry dumps, acceptance/disk/reopen logs, and inspected
`screenshots/*-migrated.png` / `screenshots/*-reopened.png`. Original local settings,
placements and bookmarks were restored after the clients stopped.
Tested JAR SHA-256: `5d0cde842ce38e5a9ddd880f5cc4e938859e633af9a2d7344139b20b977fed13`.

## Fluid containers and nested filters across packs

- In disposable copies of both target packs, prepare a BuildCraft tank with 8,000 mB water,
  an EnderIO tank with 4,000 mB lava, a chest with a 1,000 mB IC2 universal fluid cell,
  and an EnderIO item conduit with an advanced filter. Also put a configured filter
  upgrade inside the chest. Include a named IC2 ingot in filter slot 0, an empty slot 1,
  and a GT item with damage 32001 in slot 2; enable NBT matching.
- Save the full group as `.schemplus`, `.litematic` and structure `.nbt`; save the chest
  alone as classic `.schematic`. Import each in the other pack, paste it, resave it,
  and return the resaved file to its original pack. Check fluid names/amounts, item
  names/damage, named-item NBT and the empty filter slot. Check the native filter API
  accepts the named ingot and rejects both an unnamed ingot and a diamond.
- Save, unload and reopen both worlds, then verify every pasted group again. Inspect
  the actual world display separately from server inventory/NBT checks.
- Repeat with a universal cell filled through its native fluid API with 1,000 mB of
  registered mod oil. Transfer that chest in `.schemplus` in both directions.
- In a separate constructed copy, replace only the EnderIO tank's `FluidName` with an
  unregistered name. Loading must report the unavailable fluid. Paste and confirm the
  native tank is empty while the BC water tank, cell and filters remain intact. Keep
  the original file unchanged; adding a missing fluid later requires reloading it.

Observed 2026-10-13: all eight four-format cross-pack cases passed native loading,
paste, recipient resave/reload and return to the source pack. Server-side reads
confirmed tank/cell amounts, nested filter templates (including native count-zero
stacks), empty slot 1 and actual filter acceptance/rejection. Item IDs differed:
IC2 fluid cell 4098/4099, IC2 ingot 4111/4112, filter upgrade 5270/5265 and GT metaitem
7495/7638. Both oil-cell migration directions passed. Constructed missing-fluid
copies produced the chat warning and empty EnderIO tanks in both packs; unrelated
water/cell/filter data remained intact. No replacement fluid was invented.

The runtime versions were BuildCraft 7.1.44/7.1.64, EnderIO 2.9.28/2.10.48 and IC2
2.2.828 in GTNH 2.8.4/2.9.0-RC-2. Source and pasted world views were inspected. RC-2
passed world unload/reopen; 2.8.4 passed a full Java-process restart and world reopen.
An earlier scripted 2.8.4 reopen began while FML was still `SERVER_STOPPING`; that
failed harness sequence is not recorded as a native menu or product failure. Tests
used API-prepared fixtures and real in-game capture/import/paste operations; the
filter checks used the mod's live server objects, not a simulated filter.

Evidence: `<evidence-dir>/complex-migration` contains the source/returned files,
`prepare-*`, `accept-*`, `post-accept-*`, `finish-290`, `cold-verify-284`, `oil-*` and
`negatives-*` logs, plus a native display screenshot. Additional world screenshots
are in `<evidence-dir>/cross-pack-migration/screenshots`. Tested JAR SHA-256:
`e801925ab973ef8737b772d5c7ed0b332c40b9226b6876a3150c8a88e0077c38`.
Build and all 667 automated tests passed. Mod-private machine/material indexes,
renamed fluid registries and shared world-owned storage are separate boundaries.

## Plain Forge, Java 8 and optional NEI

1. Use a fresh Forge 10.13.4.1614 client with Java 8 and LWJGL 2.9.4, with the current
   Plus JAR as its only mod. Use the launcher libraries for Minecraft 1.7.10; do not
   copy a Java 21/lwjgl3ify launch classpath unchanged. Create a disposable world.
2. Import `.schemplus`, `.schematic`, `.litematic` and structure `.nbt` captured in a
   different mod selection. Include a vanilla chest with named/tagged items, a chest
   minecart and missing mod blocks/items. Check the established format-specific
   missing-block fallback, retained inventory slots and the omission report.
3. Paste each format with tile/entity data enabled, export/reload it, and capture
   a pasted region again. Check actual server blocks, tile inventories and entity
   counts. Save, close the Java process, reopen the world and repeat those checks.
4. Inspect the preview and pasted containers. Use native right/left clicks to place
   and break a block, M to open the main menu, and E to open the vanilla inventory.
   For the installed original-NEI case, follow the additional checks below: normal
   NEI recipes must load, while unsupported bookmark-demand integration stays off.

Observed 2026-10-13: Java 8u504, Forge 1614 and LWJGL 2.9.4 passed steps 1–4 without
NEI, GTNHLib, UniMixins or lwjgl3ify. Four source-B selective-mod files passed native
import/paste/export/reload, recapture and cold world reopen. The chest retained seven
diamonds and the written book text; the chest minecart retained eleven emeralds,
with one cart per destination. Missing StorageDrawers/JABBA data used the previously
recorded fallback. Preview, pasted-world, main-menu and inventory screenshots were
inspected. Native mouse edits were checked against server block state; native M/E
opened the expected screens. Fixtures and API-triggered schematic operations were
prepared by an external harness; menu entry and input checks used native events.
Headless audio was unavailable and was not tested. This first run covered the
absent-NEI case only; installed original NEI is covered separately below.
No product change was needed for the tested configuration.

Evidence: `<evidence-dir>/plain-forge/{acceptance-final,native-input,cold-verify}.log`
and its `screenshots` directory. Tested JAR SHA-256:
`d99db0218dfc86d54ef7f1da46c40d5018e66458d3302502895a4f03d3ddecbb`.
Build and all 664 automated tests passed.

### Installed original NEI

1. In a separate copy of the plain Forge/Java 8/LWJGL2 instance, install the current
   Plus build with `NotEnoughItems-1.7.10-1.0.5.120-universal.jar` and
   `CodeChickenCore-1.7.10-1.0.7.47-universal.jar` in `mods/`. Put
   `CodeChickenLib-1.7.10-1.1.3.138-universal.jar` in `mods/1.7.10/`. Preserve uploaded
   originals, record their hashes and use copies. Do not replace NEI inside a GTNH
   instance to create this test; this checks the separately installed original mod.
2. Enter a disposable world, open survival inventory and wait for NEI's item list.
   Hover a crafting table and press R; require its four-plank recipe. Hover an iron
   ingot and press U; require native crafting uses. Press R on that ingot, switch
   recipe categories and inspect its furnace recipe. Check that no Plus S group
   button or demand handler is registered.
3. Type `redstone` into NEI's search box and verify that the item panel filters.
   Right-click the search box to clear it and require the full item list again.
   Leave inventory and import the public `SimpleHouse.litematic`
   fixture described above; the material list must have 76 items and no Send to NEI
   button. Missing bookmark/tree APIs must not prevent opening or using the list.
4. In creative mode, place and break a block with actual mouse input, checking
   server block state. Use native M/E to open the Plus menu and vanilla inventory.
5. Save and close the world, stop the Java process, restart and reopen the world.
   Repeat recipe lookup/category navigation and material-list checks. Inspect the
   actual screens as well as native handler/item counts; retain startup logs.

Observed 2026-10-13: both initial startup and a full Java-process restart passed
with the three supplied original JARs, Forge 1614, Java 8u504 and LWJGL 2.9.4.
NEI indexed 619 items; the unfiltered visible panel contained 589. Native R opened
the workbench recipe; U on an iron ingot
returned 25 shaped uses and one shapeless use. R on the ingot included its shaped
and smelting recipes; native category navigation displayed the furnace recipe.
Searching `redstone` returned seven items; clearing it restored the same 589 visible
item identities. The material list retained all 76 required items; Send to NEI was absent, the bookmark bridge stayed disabled and neither the
demand handler nor the S group button registered. Native placement/breaking and
M/E checks passed, with block changes independently checked on the integrated server.
The fixture harness prepared inventory/world state and opened the material list;
recipe/search and input checks used native keyboard/mouse events. Recipe, search,
material and menu/inventory screenshots were inspected. No product-code change
was needed. This verifies the named original-NEI combination, not GTNH crafting
chains or other NEI forks; the earlier GTNH acceptance remains separate.

Build and all **675 automated tests** passed. Evidence:
`<evidence-dir>/original-nei/{acceptance,cold-verify,search-reset}.log`,
`uploads.json` (original/installed hashes) and `screenshots/`. Uploaded originals
remained unchanged and were not added to the repository. Tested Plus JAR SHA-256:
`ae9394250250892aee684ac2bc090ad0352f706493fe08418c42f177b73206ab`.

## Schematic migration between individually selected mods

- Use separate Forge instances with fresh configs and disposable worlds. Install the current Plus JAR
  in each; a complete GTNH pack is not required. Use two versions of a shared storage mod, a storage mod
  present only on each side, and a third instance without any of those mods.
- Capture a vanilla chest containing shared and missing mod items, a written book, a populated drawer,
  another mod container and a mod inventory entity. Save all four writable formats with NBT/entities.
  Deliberately include a numeric ID used by different registry names in the two instances. Import by name:
  a missing IronChest must never become JABBA just because both have ID 165.
- Import and paste in both directions and into the instance without the storage mods. Check surviving
  block/item names, counts, damage, book pages and drawer prototypes; check missing-content reports.
  Unsupported entities are skipped with a report. Missing blocks use the selected format's existing
  fallback; neither air nor a non-tile placeholder may retain orphaned container NBT.
- Export the converted result to a new file and reload it. Already cleared inventory slots must not
  cause another missing-item or missing-ID-map warning. Save, stop the client, restart and inspect the
  actual pasted world again, including entities. Inspect saved region files independently.
- Keep the source files unchanged. Reload them in their original instance to verify their complete
  contents still exist. A converted export cannot recover omitted content after installing a missing mod;
  return to the original source. Unedited Save Source's byte-preserving behavior is separate from conversion.
- Check a constructed Sponge v2 input with equivalent native mod data in an instance with the mods and
  one without them. This is import coverage, not a real WorldEdit/FAWE export or modern mod backport claim.
- Unit fixtures additionally cover a missing optional tile API and lowercase namespaced mod entity/tile
  IDs with private NBT. Those IDs must not undergo vanilla naming/text/health conversion.

**2026-10-13 — selective-mod native acceptance.** Three fresh Forge 1614 instances
ran under Xvfb/Mesa with Java 21, UniMixins 0.1.23 and lwjgl3ify 2.1.16. A and B
also used GTNHLib 0.7.10. These were selected JARs with fresh configs, without NEI,
Angelica, GregTech, Backhand or a complete pack installation.

| Instance | Gameplay mods |
| --- | --- |
| A | StorageDrawers 2.1.10-GTNH, IronChest 6.1.6, IronChestMinecarts 1.2.0 |
| B | StorageDrawers 2.2.31-GTNH, JABBA 1.5.10 |
| C | None; the final checks also omit GTNHLib |

Native server APIs prepared fixtures; actual Plus capture, loading, export and paste
queues performed the migrations. Each direction B → A, A → B, A → C and B → C
passed `.schemplus`, classic `.schematic`, `.litematic` and structure `.nbt` import,
world paste and recipient export/reload: **16 cases**. Unlike the full-pack fixture,
the selected block IDs fit classic Alpha, so these `.schematic` files did not upgrade.
The source files remained unchanged and reloaded with all contents in their original
instances. A constructed Sponge v2 file additionally passed native loading in A and C;
it was not a real WorldEdit/FAWE export or an additional Sponge world-paste test.

The shared drawer retained 64 iron ingots with a named prototype in both directions;
its upgrade item moved between IDs `4116` and `4105`, retaining count 5 and damage 2.
The vanilla chest retained seven diamonds and the written book's title/pages.
ID `165` meant IronChest in A and JABBA in B: the absent registry name yielded missing
content, never the unrelated local block/item. A's missing mod minecart was reported
and omitted; B's vanilla chest minecart retained eleven emeralds in A and C.
Missing blocks became air in Alpha formats and stone under the existing default
fallback in named formats. Neither placeholder retained orphaned tile NBT.

All 16 recipient exports reloaded with zero additional removed-item warnings,
including empty slots left by missing items. Independent saved-region checks matched
all **1,280 block/metadata cells** (A: 320, B: 320, C: 640), chest inventories and
surviving drawer data. All three clients were then fully stopped and restarted;
native world checks passed for all 16 pasted structures, inventories and nearby
minecart counts. C also reloaded all eight original inputs without GTNHLib.
World views before and after restarting were inspected. Build, Checkstyle and **664 automated tests** pass, including
eight added regressions for orphan tile NBT, missing entity/tile types, unavailable
optional APIs, unchanged native mod identifiers/private NBT and cleared-slot reloads.

This covers the named selected-mod combinations, not every Forge mod or every storage
feature. Drawer counts above 64 and locked slots retain the earlier unit-only status.
Mod registry renames, arbitrary private IDs/schema changes and whole-world migration
still need specific handling. Missing content is not recoverable from a converted
export; retain the original schematic.

Evidence: `<evidence-dir>/selective-mods/` contains native source/recipient files,
registry dumps, `acceptance-final-*.log`, `extras-final-*.log`, `cold-final.log`,
`reopen-final-*.log`, source hashes and inspected `screenshots/*-accepted.png` /
`screenshots/*-reopened.png` views. Fixtures were generated
in new disposable instances; original pack settings and user worlds were not changed.
Tested JAR SHA-256: `b1ec8425c66721151bb9ac1e103af62d89b3619cd859ad0961fbf736473f8b62`.

## Printer build order (printerBuildOrder)

- Layers (default): print a small house with torches on the walls, rails and flowers on the floor, standing
  inside it. The printer fills the lowest layer in reach first, puts the walls' full blocks before glass panes,
  slabs and stairs, and torches/rails/flowers after the blocks they hang on; it does not place blocks next to
  your feet and head while other positions are left, so you are not boxed in. With Y Axis Reverse it starts at
  the top.
- Iterator: the order follows the iteration order and axis settings as before.

## Ignore survival states (ignoreSurvivalStates)

- Off by default: the blocks below show as wrong state/wrong block until the option is turned on.
- Option on. Save a natural tree, a wheat farm, a wired redstone circuit with buttons, pressure plates, powered rails, a
  lit furnace and a redstone lamp. Build it by hand: placed leaves (no-decay bit), younger crops, unpowered
  plates/rails/wire, unlit furnace and lamp show no wrong-state or wrong-block overlay, the verifier counts them
  as correct, the material list does not list them as missing, and the printer neither re-places nor breaks
  them. Wrong wood types of leaves still show as different blocks; a stone block where the furnace should be
  still shows as wrong.
- With the option off, the same blocks show as wrong state/wrong block like before.

## Material list replacements while building

- Open a placement's material list, replace oak planks with spruce planks and oak stairs with spruce stairs.
  Printer: it places spruce planks and spruce stairs (facing like the schematic) where oak is wanted, and with
  "break wrong blocks" on it does not break them afterwards. Easy Place: clicking an oak plank ghost holds and
  places spruce planks. Placement Restriction lets you place spruce planks there by hand but not stone.
- Clear the replacement: the printer goes back to oak planks.

## Placement by simulation (printerPlacementSolver)

- Schematic with mod blocks whose facing is in their metadata and that the printer did not orient before
  (for example logs/pillars, machines or chests from mods that set metadata from the player's direction), in all
  directions. Printer and Easy Place: each block gets the schematic's facing; the player's view turns briefly for
  the placement. A block that cannot be placed facing the right way next to the available neighbors is skipped
  (red failed highlight) instead of being placed wrong.
- GregTech machines (facing kept in the tile entity) are placed as before.
- With the option off, the behavior is the same as before.

## Auto align

- Build (or paste) a schematic in the world, then load it again as a new placement somewhere else, rotated and
  mirrored. Stand near the built copy, open the placement configuration and press "A": after a few seconds chat
  says the placement moved and how much matches; the placement lies exactly on the build with the right rotation
  and mirror. Pressing it again says it is already at the best spot.
- A half-built copy (about half the blocks): it is still found.
- Far away from any copy: chat says it was not found; the placement does not move.
- A locked placement: the locked message, nothing moves. Locked X/Z coordinates: the message says locked
  coordinates kept it from moving all the way.
- Large GTNH builds: the game does not stutter while it searches.

## Render distance and FPS guard

- Load a very large GTNH schematic. With Schematic Render Distance 64: only schematic chunks within about 64
  blocks are drawn; walking along the build draws the next chunks. 0 draws everything.
- Adaptive Rendering Min FPS 30 with a placement that makes the frame rate drop below 30: within a few seconds the
  action bar says the render distance was reduced and the frame rate recovers; far parts disappear. Looking away
  or moving to a lighter area grows it back. 0 turns this off.

## Asynchronous loading

- Load a large schematic (tens of MB) from the load browser: the game keeps running (the status line says it is
  being read), then the placement appears and the status says it was read. Pressing load again while it is read
  says it is still being read. Do not start a duplicate parse or freeze input; record
  load time for the actual file and machine rather than assuming a fixed speedup.
- Leave the world while a file is read: nothing appears in the next world.

## Printer mining in survival

- Survival, printer on with "break wrong blocks" and "break extra blocks": wrong stone and dirt blocks inside the
  placement are mined to the end (crack animation, then the block breaks and drops), one after another, with
  the best tool picked by Auto Tool; the previous slot is held again afterwards. Before this change blocks that
  are not broken instantly were never finished.

## Destroy assist (destroyAssist)

- Survival, option on. Press and hold left click on an extra block (where the schematic has air) of a placement:
  it is mined, then the next extra/wrong blocks in reach follow while the button stays down; correct blocks and
  blocks outside placements are never mined. Releasing stops at once. Left click on a correct block or outside a
  placement mines normally. Attack bound to a keyboard key: no assist.

## Next block hint (highlightNextBlock)

- Option on, survival build in progress: a pulsing cyan box marks the nearest missing block that has a solid
  neighbor; it moves on after placing it. Floating blocks without neighbors are not marked; nothing is marked
  when everything near is built.

## Printer tools (printUseTools)

- Schematic with a wheat field on farmland and a nether portal. Over grass: the printer tills the grass under the
  field with a hoe from the hotbar (or one moved up from the inventory), then plants. Over air: it places dirt,
  then tills it. With the obsidian frame built it lights the portal once with flint and steel. Without a hoe or
  flint and steel the missing-material HUD lists it. Easy Place on a farmland ghost over dirt tills it.
- With "break wrong blocks" on, the dirt under farmland is not mined.

## Favorite and recent schematics (load browser)

- Select a schematic, click the grey star at the top right of the info panel: it turns yellow and the file shows
  a yellow ★ in the list. The ★ button next to the folder icons lists the favorites from all folders; the star
  removes one again. ⌚ lists the last loaded schematics, newest first. Pressing the same button, Up or Backspace
  returns to the folder. Lists survive a restart; deleted files disappear from them. Other browsers (save,
  manager) have no such buttons.

## Container labels (containerLabels)

- Schematic with filled chests (also a double chest), a furnace with fuel and a hopper. Build the containers
  empty: within 12 blocks each shows "Wanted" in grey with up to three of its items and counts, facing you.
  Open one: its label turns amber; fill it (Fill button or by hand) and close: the label disappears. A double
  chest has one label. Containers whose schematic copy is empty and blocks other than the schematic's get none.
  After rejoining the contents are unknown again: the labels are grey until the containers are opened.

## World fingerprinting (worldFingerprinting)

- On a server that runs several worlds behind one address (or two single-player worlds saved under the same
  folder name, if possible): put a placement in world A and wait two seconds. Switch to world B: a chat line says
  the placement was placed in another world and it is not drawn, printed or listed for Easy Place. Back in A it
  shows again. Moving a shown placement elsewhere in A takes a new fingerprint there.
- Superflat worlds (no random bedrock): placements are never hidden. Option off: never hidden.

## Easy Place supports first (easyPlacePrerequisites)

- Schematic with a torch on a wall, a rail on a block and sand on a block, all of them floating with nothing
  built yet. Easy Place on the torch ghost places the wall block first, the next click the torch; the same for
  the rail and the sand (the block below first). Aimed blocks that can stand are placed directly. Option off:
  clicking such a ghost places nothing, as before.

## Carpenter's Blocks under rotation and mirroring

- Build with Carpenter's slopes (wedges, corners, oblique, prisms), stairs, slabs in all directions, a full block
  with different covers on each face and chiseled sides, buttons, levers on walls and floors, torches, ladders,
  hatches open and closed (low and high), doors with left and right hinges, gates open both ways, beds, safes,
  garage doors, daylight sensors and collapsible blocks with uneven corners. Rotate the placement 90/180 degrees
  and mirror it on X/Z: every shape, cover and side design faces the same neighbor as before in the preview and
  after paste; doors keep opening correctly (hinge side swapped by mirrors). Upside-down mirror: wedges, stairs
  and slabs flip; prisms stay.

## BuildCraft pipes under rotation and mirroring

- Pipe network with wooden extraction pipes, iron pipes pointing different ways, diamond item and fluid pipes with
  filters on several sides, gates with wires and expansions, facades and plugs on pipe sides, a powered wooden
  kinesis pipe. Rotate and mirror: extraction/output sides, gates, facades and plugs stay on the side facing the
  same neighbor; diamond filters follow their sides. After paste the network works without reconfiguring.
- Loading such a schematic in another pack: the pipes keep their pipe type (pipe item ids are remapped).

## Logistics Pipes under rotation and mirroring

- Chassis pipes pointing at inventories on different sides, with provider and extractor modules set to sneaky
  sides. Rotate and mirror: each chassis still points at its inventory, the sneaky sides follow; after paste the
  network requests and extracts as before.

## Railcraft tracks under rotation and mirroring

- Line with Railcraft boosters (powered and not), one-way tracks, direction detectors, control tracks, gated
  tracks, buffer stops, boarding/holding tracks, switches with switch motors, curves and slopes. Rotate 90/180 and
  mirror: straight boosters stay straight (not turned into curves), one-way and direction tracks still pass carts
  the same way relative to the line, buffer stops face the same end, switches lead to the same branch. Vanilla
  powered and detector rails as well.

## Botania under rotation and mirroring

- Mana spreaders aimed at pools (also upwards and downwards, one on a turntable), bellows on furnaces, mana pumps,
  incense plates, avatars and red string containers facing different ways. Rotate and mirror: every spreader
  still hits its pool after paste, the turntable turns the spreader the mirrored way, the other blocks face the
  same neighbor.

## Galacticraft Mars and asteroid machines

- Terraformers, cryogenic chambers, launch controllers, an astro miner base and beam receivers on different
  sides. Rotate and mirror: each faces the same neighbor after paste; the miner base still forms and its dock
  side follows.

## Facings kept in tile NBT (second sweep)

- OpenBlocks: block breaker/placer (six ways), fan, target, grave, XP shower, paint mixer, drawing table, bear trap,
  sprinkler, rotating elevator, guide; ArchitectureCraft shapes placed on walls, floors and ceilings with different
  turns, sawbench; a Stargate with DHD and interfaces; a Blood Magic ritual set up facing east; Ender IO reservoir
  (2x2); Draconic flow gate; Thaumic Horizons node monitor/stabilizer; Gadomancy sticky jar on a wall; Botanic
  Horizons automation block; Thaumic Energistics essentia provider; Mechworks signal bus; Steve's Factory Manager
  breaker. Rotate and mirror: each keeps its orientation relative to the build; the ritual still activates after
  paste; shapes still meet at the same edges (mirroring is covered in "ArchitectureCraft mirrors").

## Other GTNH blocks found by the facing sweep

- One of each, facing different ways where they can: Et Futurum glazed terracotta, loom, stonecutter, bee nest,
  pink petals, observer, barrel, end rod, glow lichen, amethyst cluster, chain, standing and wall banners; Cooking for
  Blockheads counters/oven/fridge/sink/tool rack; Chisel present (double) and snakestone; Botania fel pumpkin, tiny
  potato, quartz pillars; Tinkers' drying racks (floor and wall), tool station, battlesign, stone torch, landmine,
  conveyor and slime pad (diagonal too); Twilight Forest fireflies/cicadas, trophies, shields, spiral bricks; Thaumic
  Tinkerer tablet, repairer, R-placer, dislocator with a transvector target; HEE void chest, obsidian pillars; Malisis
  mixer, door factory, rusty hatch, forcefield door; Draconic generator, potentiometer, teleporter stand; Galacticraft
  dish, lamps, torches, spin thrusters, treasure chests; FloodLights; OpenPrinter, OpenSecurity, Steve's Factory
  Manager cluster/relay/sign/breaker. Rotate and mirror: each faces the same neighbor; multi-block doors still open as
  one piece after paste.
- Et Futurum lanterns standing and hanging, a dripstone stalactite and stalagmite (several blocks long). Flip the
  schematic upside down (y mirror): lanterns hang from what they stood on, dripstone points the other way with its tip
  still at the end. Horizontal turns and mirrors leave them as they were.

## Catwalks, HarvestCraft, Galaxy Space and Automagy under rotation and mirroring

- Catwalks with some sides opened, caged ladders facing all four ways with opened sides, support columns on all three
  axes; HarvestCraft oven, churn, quern, presser and grinder; Galaxy Space machines and solar panels; Automagy
  hourglass, chests, golem taskmaster/inhibitor, unseen scribe with pointer, requisition tome, remote comparator, vis
  reader, tallies, hungry maws, redcrystal lines, an inventarium with nodes. Rotate and mirror: everything faces the
  same neighbor, ladders become the ladder block of the turned side with the same openings, redcrystal still connects
  along the line, the inventarium and the linked devices still find their partners after paste elsewhere.
- Galaxy Space and Galacticraft solar panels in the preview tilt toward the sun (flat at night or in rain).

## Thaumcraft display state in the preview

- Save and load: an active infusion matrix (should hover and turn as in the world), a closed and an open essentia
  valve (handle down / up), an arcane bore with a focus (focus shown), an essentia reservoir holding two aspects
  (glass color cycles between them), a crystallizer working on an aspect (tinted), an ethereal bloom (fully grown).

## Links to other blocks in tile NBT

- Build with a Botania light relay chain, a daybloom bound to a spreader and a hopperhock bound to a pool, a
  Draconic energy relay linked to two receivers, a BuildCraft quarry with its area and markers, Thaumcraft linked
  mirrors and a Galacticraft beam reflector chain. Save it, paste it elsewhere (also rotated and mirrored, and as a
  grid copy): every copy links to its own partners, not to the original build; the quarry digs its own area.
- Paste a schematic saved by an older version with such tiles: the links keep the old positions (unchanged).
- Move the build with the move tool: links follow.
- A Nuclear Control screen of several panels and extenders, and a lit Galacticraft arc lamp: save, paste elsewhere
  rotated. The screen shows its text across all its panels at the copy; switching the copied lamp off and breaking it
  leaves no lit air at the copy and touches nothing at the original.
- Nuclear Control info panel with a reactor/energy sensor card for a reactor or storage in the build, a range trigger
  and a remote thermometer with cards: save, paste elsewhere (also rotated). The copies show and react to the copied
  machines, not the original ones; cards pointing outside the saved area point at the same offset from the copy.
- Railcraft block signals paired along a track, a distant signal paired to a signal box controller, and a switch
  motor paired to a receiver box: save, paste elsewhere (also rotated and mirrored). The copied signals change aspect
  for carts on the copied track and the copied motor follows the copied box; the originals are unaffected. Pairings
  into another dimension are left as they were.
- A Tinkers' smeltery with drains, seared tanks, faucets and casting channels branching in several directions, molten
  metal inside: save, paste elsewhere rotated and mirrored. The copy forms without breaking a block, its drains and
  tanks belong to the copied controller, molten metal renders inside the copied walls, channels keep their open
  outputs and contents turned with the build. Also a Draconic portal (dislocator receptacle frame), a deployed Adventure
  Backpack sleeping bag, a Railcraft world anchor, a Galacticraft launch controller and Storage Drawers with a
  controller and controller slaves: links follow the copy (the copied slaves serve the copied controller).

### Cross-dimension links and world-owned storage

1. In disposable GTNH 2.8.4 and 2.9.0-RC-2 worlds, place and link two
   Thaumcraft mirrors on supported walls in the Overworld. Include the supports,
   an owned EnderStorage chest and a vanilla Ender Chest in a full-NBT capture.
2. Save `.schemplus`, `.litematic` and structure `.nbt`. Paste each into a cleared
   Nether location (or explicitly select replacement of all blocks). Both mirrors
   must target the copied partner in dimension -1. Send items through a copied
   mirror and verify the other mirror actually receives them.
3. Put different contents in the same owner's EnderStorage frequency and personal
   Ender Chest in each world. Transfer the schematic between packs. Frequency and
   owner should remain intact, while the recipient world's shared and personal
   inventories remain its own. Schematics do not contain those world-owned stores.
4. Preserve references originally pointing into a different dimension, unbound
   links and unmarked tags. Numeric mod-dimension identities can differ between
   packs; inspect and manually rebind those references where necessary.

Native checks on 2026-10-13 used the same current Plus build in both packs.
Before the fix, a real Nether paste moved mirror coordinates but retained
`linkDim=0`; native mirrors invalidated the links. After the fix, all three full-NBT
formats in both packs produced valid native links with `linkDim=-1`. Calling each
copied mirror's native transport with three diamonds populated the partner's
buffer; the emitted items and both Nether scenes were inspected. Cross-pack
pastes retained frequency 1725 and the test player's owner while preserving the
recipient's distinct shared and personal inventories. Saved region data was also
checked. Foreign-dimension/list-encoded links and zero-offset dimension changes
are covered by automated tests; arbitrary third-party link formats remain outside
these native checks.

Evidence: `<evidence-dir>/world-links/{baseline-290,accept-290,accept-284,nether-accept-284,scene-284,scene-290}.log`,
`saved-regions.log`, and `screenshots/nether-{284,290}.png`.
Tested JAR SHA-256:
`d6a80fbebaaccb8853707ab6c602a29e552226bb1d88fb7a7d6ed3621b74ff55`.
Build and all 671 automated tests passed.

## Thaumcraft, BiblioCraft, Witchery and Extra Utilities under rotation and mirroring

- Thaumcraft: alembics on a crucible, essentia jars with labels, centrifuge, thaumatorium, reservoir and crystallizer,
  tubes with closed sides plus a one-way tube and a valve, a buffer with choked sides, vis relays on walls, bellows on
  an infernal furnace, arcane lamps and an arcane bore on walls, banners standing and on walls, mirrors on walls and an
  arcane door. Rotate and mirror: everything faces the same neighbor, tubes still connect the same way, banners turn.
- BiblioCraft: bookcases, shelves, potion shelves, labels, weapon racks/cases (one laid flat), writing desk, clock,
  fancy sign, map frame, painting, seats, dinner plate, armor stand, printing press, typesetting table. Each faces the
  same way relative to the build after paste, the flat weapon case stays flat.
- Witchery: coffin, mirror (two tall), distillery, kettle, spinning wheel, witches' oven, alluring skull and wolf head
  on the floor and on walls. Same check; the coffin is not split into two halves pointing apart.
- Extra Utilities: generators of several kinds, conveyor line, item and liquid transfer nodes on pipes, spikes on
  walls. Same check; the nodes keep pulling into their pipe network.

## Backhand offhand with Easy Place, pick block and the printer

- With Backhand installed, hold a torch in the offhand and blocks of a schematic in the hotbar. Easy Place and the
  printer place the schematic blocks; no torch is placed by the offhand.
- Put the only stack of a schematic block in the offhand (none in the main inventory) and empty a pick-blockable
  hotbar slot. Pick block on that schematic block and use Easy Place: the stack moves from the offhand into the hotbar
  slot and is placed; the item in hotbar slot 1 is not touched.
- With a swap slot queue configured and printAutoTool on, keep a pickaxe only in the offhand and break a wrong block
  with the printer: the pickaxe is swapped into the queued hotbar slot. Reopen the inventory: no item is duplicated or
  lost, on a server too.

## Snakestone, nagastone, bloodwood, arc lamps and Nuclear Control panels

- Chisel snakestone and Twilight Forest nagastone: a snake with a head, straight runs along x, z and y, and corners
  going up and down (also the naga courtyard as generated), plus etched nagastone on every side. Rotate and mirror: the
  body still runs through the same blocks, heads point away from the body, no piece shows a wrong corner texture.
  Tilt it (X/Z): straight runs and vertical corners turn; a corner that would join two horizontal sides keeps its
  texture until a neighbor update rebuilds it after paste.
- Natura bloodwood: a grown 2x2 bloodwood trunk. Rotate and mirror: the end grain on top still forms one ring across
  the four logs.
- Galacticraft arc lamps on the floor, the ceiling and each wall, each lit toward a different side. Rotate, mirror and
  paste: each lights the same part of the build as before (check with the lamp on, at night).
- Nuclear Control info panels and advanced info panels on the floor, the ceiling and walls, with text, some turned
  with the wrench (screen rotation), an advanced one tilted left and up and one with text rotation. Rotate: floor and
  ceiling screens read the same way relative to the build. Mirror: screens still read upright, the left/right tilt of
  the advanced panel flips to the other side, the up/down tilt stays.

## ArchitectureCraft mirrors

- Run the following in both GTNH 2.8.4 and 2.9.0-RC-2. Save with tile NBT enabled, load the result with schematic and block rendering enabled, and open its material list. Roof outer/inner corners and small triangular shapes must appear as models and contribute to the total; GTNH 2.8.4 may use the native generic Architectural Block item name.
- After original and mirrored pastes, capture both copies again. Compare `Shape`, `BaseName`, `BaseData`, `side` and `turn` with the preview and inspect the saved region data; omitted zero-valued orientation fields are equivalent to zero. A file without the shape tile NBT is an incomplete sample and cannot establish whether the mirror calculation is correct.
- Place every shape from the sawbench at least once, the corner ones (roof, overhang, bevelled, round, sphere, stair,
  balustrade, banister and window corners, Ionic and Corinthian capitals, plain balustrades) with two different
  turns, LH and RH gable overhangs and cornices (and their ends), a banister with a placement offset, and a window
  with a connection turned off with the hammer. Mirror along x and z and paste.
- Each shape is the mirror image of the original: corners fill the mirrored corner, LH pieces become RH and the other
  way round, the banister sits on the other side of its block, the disconnected window side is the mirrored one.
  Triglyph and architrave corners are not quite symmetric in their carving; their outline must match, the carving
  may differ slightly.

## Extended block metadata (EndlessIDs) and multiblocks under rotation and mirroring

- In GTNH 2.9, build or find a GT multiblock that uses frames (for example a Precise Auto-Assembler MT-3662 with
  Tungstensteel frames) and save a box that holds the whole structure: the file is saved as `.schemplus`. Paste it
  rotated (CW_90) and mirrored (CW_180 + LEFT_RIGHT): the frames keep their material (WAILA shows the same frame
  name), the controller forms ("Running Fine"), a mirrored controller shows its front texture mirrored.
- Same with an AE2 build mirrored: cables connect to the mirrored sides, controllers light up, Assembly Lines form.
- Multiplayer: download the schematic from a server (`/schematic download`) and paste it through the remote edit
  path; the frames keep their material.

Dedicated GTNH server fixture (fresh disposable world, creative + op):

- Build a north-facing Precise Auto-Assembler at `(0,80,0)` (`mID=32018`, not the
  display model number 3662). Creative sneak-right-click with two Hologram Projectors
  in one stack builds the shell with EV glass; replace five front-bottom casings with
  energy, input bus, output bus, maintenance and muffler hatches. Require the controller
  to form normally. Its 12 Tungstensteel frames have metadata **316**.
- Include a named chest with seven diamonds, a four-line sign and red wool beside
  the machine. Face the chest south, then put the wool immediately west of it;
  this catches the chest placement callback auto-facing east and skipping saved NBT.
  Keep `pasteWithoutUpdates=false` and require the saved chest facing, custom name
  and all seven diamonds after pasting. Save `(-12,79,-1)..(4,84,4)` using
  `/schematic save net_meta316 -12 79 -1 4 84 4`, then download it with
  `/schematic download net_meta316.schemplus`. Do not copy the file between instances.
  The 17-block width puts six extended-metadata frames in each of two download chunks.
  Compare block names, full metadata and tile NBT in the two files; the frame bytes
  must combine as `60 + (1 << 8) = 316`, never 60 or 12.
- Load the downloaded file and execute Paste while holding the tool. Test separate
  placements at origins `(52,79,-1)` with NONE/NONE, `(128,79,64)` with CW_90/NONE,
  and `(192,79,64)` with NONE/LEFT_RIGHT. Keep block NBT and inventories enabled.
  After each operation check the real controller and frames in game, then issue
  `save-all` on the server console and inspect its region files with
  `tools/headless/regions.py`. Require 12 metadata-316 frames, preserved chest/sign
  contents and controller/hatch facings NORTH, EAST and SOUTH respectively. Account
  for the chest/sign metadata rotating too; normal formation does not prove a powered
  recipe was run.
- Disable `remoteEditsEnabled`, restart the server and reconnect. Paste a single stone
  block into known empty space: require the server's remote-edit rejection and an
  unchanged destination. Using a block without tile NBT ensures a command fallback
  cannot be concealed by a separate NBT validation error. Re-enable and reconnect;
  the same operation must succeed. Test the documented command fallback separately
  against a server without Plus.

Adjacent double-chest placement callbacks (normal updates):

- In a fresh test area, place two chests side by side facing NORTH (metadata 2), then
  add a wall north of both. For console construction, leave the north side clear,
  temporarily put stone south of both, and create each chest once with its full NBT.
  Remove the south stones and add the north wall. Do not use a second `setblock`
  to change chest metadata: in 1.7.10 it can break the chest and drop its inventory.
- Give the west half the name `Double A Diamonds` and seven diamonds in local slot 0;
  give the east half `Double B Emeralds` and eleven emeralds in local slot 26.
  Save and download the area through the dedicated server. Keep normal updates,
  block NBT and inventories enabled; paste into empty destinations.
- Test NONE/NONE, CW_90/NONE and NONE/FRONT_BACK. Require both metadata values to
  be 2/2, 5/5 and 2/2 respectively after the entire paste and a world save. The X
  mirror must move each half's name and inventory with that half. Before the fix,
  the second placement callback changed the first half from 2 to 3 in a plain paste.
- Open both halves in every copy. Each must expose the same six-row, 54-slot
  inventory. Plain/CW_90: diamonds in global slot 0, emeralds in slot 53, title A.
  FRONT_BACK: diamonds in slot 27, emeralds in slot 26, title B. No items may be
  duplicated, lost or reassigned between halves.
- The extra metadata pass must only repair surviving chest blocks. Test a long
  paste alongside flowing water/lava and other ticking blocks: normal world changes
  must not cause a new metadata-restoration failure or prevent remaining updates
  and entities. Unit regressions separately cover cancellation, skipped cells,
  silent placement, rejected chest metadata writes and progress across both scans.


### Dedicated-server edit lifecycle and download boundaries

Use a fresh disposable world, matching client/server JARs and real network commands.
Save the world before reading region files; retain inputs, logs and screenshots.

- With remote edits enabled, paste a one-stone file into air as a creative non-OP.
  Require the server permission rejection and an unchanged destination. Restore OP,
  switch to survival and repeat: this is the client's Creative-only rejection,
  not proof of the server check. Restore creative mode and require the same paste
  to succeed. Separately change to survival while a large task is already writing:
  the server must stop further writes and allow a new task after permissions return.
- Use a 256x16x128 wool schematic in a new empty destination, hide its preview,
  and wait for a SERVER task with Structure progress. Open Task Manager and Remove
  it. The already pasted subset is retained; no rollback is promised for PASTE.
  Compare two saved-world snapshots: the nonempty subset must stop growing below
  524288 cells. Wait for the task to disappear, then require a new small paste to
  succeed. Repeat at another destination, disconnecting during Structure instead.
- Exercise cancellation during upload, decoding and queued server start as well.
  Deterministic lifecycle tests pause these stages; a GUI run that only cancels
  Structure does not prove it hit the shorter decoding window. After cancel A,
  start B and release A's delayed work: A must neither start nor cancel/remove B.
  Change dimension during an incomplete upload; a new-dimension task must not
  remain busy, and late old-dimension slices must not disturb it.
- Download a separate 128-chunk file and disconnect after transfer begins. The
  client must not save a partial file. Reconnect, download a small file successfully,
  then retry the complete large download and compare all block states and NBT.
  Keep the client filename distinct from any local large-paste input.
- Download an explicit independent-region fixture with overlapping A/B regions,
  a distant C region and an origin outside the enclosing box. Compare every nested
  schematic and region definition. Load the actual downloaded file, move B aside
  in the subregion GUI and verify that A's previously hidden blocks/NBT reappear.
  Paste with Replace All: air inside a region must clear a preplaced marker, while
  a sentinel in the gap between regions must survive. Check all three inventories.
- Build a source spanning a download chunk boundary with two named chest minecarts
  containing distinct items, and two item frames holding differently rotated items.
  Place frames through the normal held-item action and wait for their surface check;
  do not rely on `/summon` to preserve a hanging entity's thin collision box. Save
  through the server, download, and compare all four entities without duplication.
  Enable placement entities and paste to a fresh area. Verify exact item contents,
  rotations, relative positions, hanging anchors and newly assigned UUIDs; wait
  over five seconds to check that the frames remain attached. Repeat with Ignore
  Entities enabled and require zero copied entities.

- Connect two independent clients with different player UUIDs. Give each private
  server directory a same-named, differently shaped/colored large file. Start both
  downloads together and compare every block, metadata and NBT to its own input.
- Start a large edit for A; submit B while A is writing. Require the busy rejection
  and no queued B writes. A cancellation packet from B using A's operation ID must
  not stop A. Once A finishes, B must work. Cancel another edit using A's Task
  Manager, compare stable partial-world snapshots, then require B to work again.

Two-client scheduling/download checks and live legacy-capability emulation passed
on 2026-10-08; HANDOUT.md records their scope. Legacy-capability emulation is now
an archived result, outside future acceptance work. Two-client scheduling/download
checks remain required for current matching builds; arbitrary server permission
plugins are not covered by these results.


### GTNH 2.8.4 mouse input with lwjgl3ify 2.1.16

- Use an installed runtime JAR with the pack's original lwjgl3ify, fresh Plus
  configuration, Creative mode and no loaded schematic. In a disposable world,
  break a stone block with an empty hand, place a colored wool block and open a
  chest. Check the actual saved blocks as well as the screen. Repeat after closing
  an inventory and regaining window focus. Inventory left/right clicks must work.
- The public `1.0.0-beta.1` JAR reproduces blocked attack/use on this pack. Compare
  against the fix with the same server and player; the server need not install Plus.
  Use OP outside protected areas so permissions cannot mask input behavior.
- Hold the configured tool (default stick), with rendering and tool enabled:
  left/right clicks must still control selection/placement instead of breaking or
  placing real blocks. Switch to an empty slot and require normal interaction again.
- Bind a keyboard shortcut, open/close the main menu, scroll the tool mode and repeat
  clicks. Recheck a vanilla development client and other lwjgl3ify versions when
  changing the hook. A successful 2.8.4 run alone does not cover every input coremod.


### Client-only command paste with NBT

The core confirmation, omission, permission, recovery and cancellation checks
passed in the GTNH 2.8.4 dedicated server without Plus on 2026-10-08. GTNH
2.9.0-RC-2 also passed the blocks-only result through forced integrated-server
commands. Project/grid and the remaining context combinations below need separate
coverage; section 6 of HANDOUT.md records the exact completed scenarios.

- Connect with only the client installing Plus. Use Creative mode and server
  `/setblock` permission. Load a fresh schematic containing stone, colored wool,
  a chest with seven diamonds, a sign with text and an entity. Keep default NBT
  and entity settings. Record the source file hash and an empty destination.
- Execute Paste. Require the blocks-only explanation before any command or task
  starts. Cancel and repeat using Escape: save the server world and require an
  unchanged destination. Confirm on the next attempt; require the block layout
  and supported metadata, an empty chest, blank sign and the entity summoned at
  its position and facing. Item frames and paintings must keep their anchor and
  direction and stay attached for five seconds. Rename `commandNameSummon` and
  require the paste to use the new command name.
  The source file hash and loaded schematic's complete NBT must remain unchanged.
- Paste a block-only file: no omission dialog is needed. Explicitly ignoring
  block entities must not silently drop entities that are still included. With
  update suppression enabled, reject before sending commands or offering consent.
- Remove command permission and repeat; require the server's rejection and an
  unchanged destination. Restore permission and require a new task to work. Do
  not describe commands sent as acknowledged successful server edits.
- Disconnect/change dimension or leave Creative mode while the dialog is open;
  its old snapshot must not execute in a different player/world context. Remove
  a running COMMANDS task and check that further commands stop.
- Project ENTIRE_VOLUME mode must confirm data omission before clearing the old
  area. Cancel and require the original blocks and paste history to remain. After
  accepting, require clear-then-paste order. Changing project/version while work
  is pending must not mark the new version pasted. Check grid copies separately.
- With Plus on the server (and in normal singleplayer), repeat with full NBT:
  no blocks-only prompt, inventories/signs/entities preserved. A server that
  advertises the protocol but rejects remote editing must not fall back to chat
  commands to evade that server policy.

### Complete data requests when saving

- With Plus on a dedicated server, save an area containing a filled chest, a sign,
  a GregTech machine and an item frame. The saved file must contain the server's
  inventory, text, machine data and frame item, not only the client's view.
- Set `nbtRequestPermissionLevel` above the player's level, or disable
  `nbtRequestsEnabled`, and repeat: the save must finish with the client data and
  show the incomplete-data warning. Save an area larger than 64 chunks and one with
  unloaded chunks; both must finish without stalling.

### Command paste replies and permissions

- On a dedicated server without Plus, whitelist a creative player but remove OP.
  Accept the blocks-only dialog for a file with more than ten non-air blocks.
  Require one rejected command, no completed-paste message or completion callback,
  an unchanged destination, no remaining task and no spam disconnect. Restore OP
  and repeat at a fresh, loaded destination; inspect every block and metadata.
- The first command must receive a native success reply before another batch is
  sent. Completion, including a project's clear-then-paste continuation and paste
  history, must wait for the last reply. Test a one-block file too: it must not
  finish before a delayed permission or out-of-world error arrives.
- Repeat an already matching block-only paste. Matching loaded cells need no
  command; they must not trigger a vanilla no-change error. For other commands,
  no-change, syntax and block errors stop the task without a success callback.
- Remove a task while it awaits a reply, leave creative mode, change dimension or
  disconnect. No further commands should be sent. Already sent commands cannot
  be recalled; an unrelated player's admin broadcast must not acknowledge them.
- If a custom server command suppresses or replaces native setblock replies, the
  queue must stop with the unconfirmed-result message after the reply timeout.
  It must not assume completion or advance a project's history. Automated queue
  tests exercise delayed, absent, wrapped and unrelated replies independently
  of the native-game checks above.

## ProjectRed Easy Place and multipart materials

Run in GTNH 2.8.4 and 2.9.0-RC-2, including a server without Plus:

- Capture OR gates at all four floor rotations and on a wall and ceiling, with tile NBT enabled. Keep the backing blocks, move the placement to an empty copy, enter survival with the correct gates in inventory, and use Easy Place. Check each native gate's facing, the inventory decrease and its saved `orient` value. Repeated use on a complete gate must not consume another item.
- Capture two red-alloy wires on different faces of the same cell. Its material list must count two wires. Build one face, refresh the list (one missing), then build the other (zero missing); both parts must remain present. A matching host block and metadata alone must not mark the second wire as built.
- With Ignore State disabled, a differently oriented gate remains missing. With it enabled, orientation is ignored, but a different gate item or missing second part remains missing. Unsupported/unknown picked items must be reported as skipped rather than silently counted as complete.
- With no matching item in survival, or with the required backing block removed, Easy Place must fail without placing or consuming anything. Gate configurations that cannot be produced by normal placement still require the native screwdriver/configuration tools; this does not copy powered state, timers or arbitrary NBT.
- On a Plus server with the accurate-placement protocol enabled, test a non-whitelisted mod block whose placement solver supports its orientation. The client must continue computing its placement direction instead of assuming the server will rewrite it. Recheck vanilla stairs, and repeat with protocol None.

Project and material follow-up acceptance (2026-10-08): cold GTNH 2.9.0-RC-2 recovered an older two-version project, retained chest/sign NBT and Chinese descriptions, and restored the selected second version after leaving/re-entering the world. Opening alone left all three source file hashes unchanged. GTNH 2.8.4 opened a copied legacy project. Both packs refreshed an already-open English material list to Chinese automatically. In 2.8.4 the two-times multiplier, ignored material and gold-block replacement survived; verify the Chinese inventory HUD appears with `renderMaterialListInGuis` on and disappears with it off. These checks used the combined project/layout/material fix, with 580 automated tests passing.

### Connected states, camera and dimension follow-up

Observed in both GTNH 2.8.4 and 2.9.0-RC-2 on 2026-10-08:

1. Power a lamp/piston, fuel a furnace, join clear/coloured panes, fences and iron bars, stack three BuildCraft tanks and connect GT pipes. Capture the actual world, and compare opposite preview views; verify saved source states include the lit furnace/lamp and extended piston.
2. Place the preview across x=671/672 and at minimum (-17,150,-17), crossing negative chunk boundaries. Compare fast/AO-off and fancy/AO-on; move the camera across the chunk and view the same geometry again.
3. Enter the Nether and move clear of the arrival portal before testing. Confirm dimension -1, then load the fixture above the roof and inspect it. In 2.8.4, load a uniquely named placement, remove it, leave and return; it must remain absent while retained placements restore.
4. In 2.9, enable the installed Freecam with F4, wait, and move forward while watching the connected EnderIO fixture. The real player must remain fixed, the camera coordinates must change, and the preview must remain fixed in world space. Disable Freecam afterward. This run does not cover a separately installed Freecam on 2.8.4.
5. Compare stone against a real gold block, red wool against white wool, and schematic air against a real stone block. Enable overlays and inspect distinct wrong-block, wrong-state and extra colours in both packs.
6. Capture seven actual EnderIO bundles (item/power curve and item T) with five endpoint chests. Check source connections, mode arrays, side filters and inventories from a saved-world copy before comparing both preview views. A stale in-memory source must be reloaded after replacing its file; the 2.9 final inspection used a fresh complete-file name. Transform/transport checks are separate.

No external shader pack was installed in that run; no shader or universal machine-animation result is claimed for it. Screenshots, source snapshots and native command records: `<evidence-dir>/followup`. External shader checks were added separately on 2026-10-12 below.

### Failed writes and edits with no changes

1. On a Plus server using unloaded-chunk placeholders, paste a single stone far outside loaded chunks. It must fail with the target coordinates and must not show the successful-paste message. Travel there and verify the actual target is still air; load the area and retry, then verify stone was written.
2. Repeat the same ordinary block paste. Expect a no-changes notice, not a successful-paste message. Check an air-only/replacement-filtered empty job too.
3. Paste a long row through loaded, unavailable and loaded chunks. Record refused writes, continue to the later writable cells, and finish with a partial-result summary. Failed cells must not increase the successful-block count. Do not describe the operation as atomic or rolled back. Verify a subsequent small edit can run.
4. Use forced commands in single player against identical blocks. The queue must send zero commands and report no changes. An empty project clear stage may continue to its following paste; a denied/failed stage must not.
5. Overwrite a matching chest block with different saved contents/name. The tile data must still be restored. Repeat normal adjacent-chest/extended-metadata tests to ensure the no-op check did not skip tile restoration.

The pre-fix dedicated 2.9 baseline at (4096,100,256) reported success while the real block remained air. Fixed cold-runtime checks passed: 2.9 remote zero-write failure, load/retry, no-op, stable 112-block partial prefix with an air failed cell, subsequent full-NBT recovery and matching-chest NBT replacement; 2.8.4 native/forced-command no-op and a following acknowledged one-block command paste. The remaining ungenerated chunks were not counted as air. HANDOUT section 6 records the exact scope; 586 automated tests pass.

### Crystal chest without Plus

The 2026-10-08 GTNH 2.9.0-RC-2 control used a copied world and a cold client with the Plus JAR removed. Confirm the mod is absent, keep IronChest contents rendering enabled, and view a real crystal chest containing 64 diamonds from a fixed nearby camera over a full rotation cycle. The icon was visible, absent for several successive captures, then visible again without any input; opening the native inventory still showed 64 diamonds. Leave/reload its chunk and inspect the reverse side. The original world-region hash must remain unchanged; restore the JAR afterward. This reproduced the observation without Plus, so no Plus change was made. Evidence: `<evidence-dir>/followup/screenshots/crystal-no-plus-*`.


### Historical transforms and Multipart synchronization

1. In each target pack, capture native EnderIO curve/T networks with item and power conduits, per-face filters, channels, priorities, extraction modes, a disabled endpoint and stocked endpoint chests. Test all four Y rotations with and without a horizontal mirror. Export/reload and paste into loaded chunks; independently compare positions, ordinary block metadata and complete conduit/chest NBT. Rotate subregions and the whole placement together and repeat. Change extraction only on a pasted copy: filtered diamonds must reach the intended outputs, iron must remain at the inputs and the disabled endpoint must stay empty.
2. Capture normal/inverted ProjectRed full lamps with powered/unpowered supports and four small lights: fixture, cage, inverted cage and lantern. Record real registry IDs; the tested packs assigned full lamps IDs 10713 and 11145. Transfer native files both ways and verify registry names, colours, inversion and power, including metadata above 15. Do not use vanilla `/setblock` arguments above 15 to prepare the source: use native item placement.
3. Paste the lamps into a loaded, previously empty area and disable schematic rendering. All four Multipart lights must appear immediately in the real world, without leaving/re-entering the chunk. Compare the client tile descriptions and saved server NBT. Repeat with `pasteWithoutUpdates`, replacing existing parts and with the world-move tool; confirm cancellation never sends a description for an unplaced tile. Restore the option afterward. The pre-fix failure left complete server tiles but no client tiles until a chunk reload.
4. Capture IC2 wheat at growth stages 1/4/7, coffee at 1/5, mature redwheat and empty crop sticks. Inspect real/preview textures and paste. Save and copy the result immediately before comparing crop owner/name, growth stage and stats: a live crop can subsequently grow or die, especially on unwatered farmland. Check preview loading with no purple-black textures.
5. In 2.9, rotate/mirror a native GT metadata-316 frame fixture, save/reload and paste. Check every saved block name/metadata and inspect the frame textures. This representative case does not resolve a report involving an unavailable original schematic.

The EnderIO matrix/NBT/transport cases passed both packs on 2026-10-08. The 2.9 GT fixture passed all 612 cells including twelve metadata-316 frames. Full ProjectRed lamps retained their type and state across both packs; the separate Multipart client-description omission was reproduced and repaired. Two automated installation/cancellation tests fail without the repair; all 588 tests pass with it. Both cold clients passed ordinary/silent/cross-pack paste and world move without a chunk reload, with client descriptions and saved tile NBT matching. Each pack also passed all seven immediately saved crop states and preview/pasted texture checks. A matching cold 2.9 dedicated server passed remote paste and replacement of existing Multipart lights with different colours/inversion; the new client appearance and ten saved tiles matched. Final screenshots were inspected. Runtime JAR SHA-256: `9dd76a64c17cdb26724e0ac5e6e36321bbadfaf3d06a9ade716bf9f11e6556df`. Evidence is under `<evidence-dir>/followup/multipart-final-*` and `multipart-dedicated` (saved-world check: `multipart-dedicated-saved.log`).

### Input and Backhand release regression (1.0.0.beta.2)

1. Use each installed runtime JAR in GTNH 2.8.4 (Backhand 1.7.7, Bogo Sorter 1.2.68) and 2.9.0-RC-2 (Backhand 1.8.16, Bogo Sorter 1.3.54). Start from a cold client and verify the version shown by Forge. In survival, disable Easy Place/the printer and test normal mouse placement and breaking, including after opening/closing inventory. Check native offhand torch placement as a control.
2. Release M to open the menu; press M+C, release both and verify the settings screen remains. Rebind vanilla attack/use to otherwise unused keyboard keys, test both, then restore the bindings. Resolve native pack conflicts first: 2.9's guide already uses G. Hold M while changing window focus, release it outside, return and verify there is no spurious menu action and the next M press works. Hold use while opening a GUI, release there, close it and verify attack/use are no longer held and still work.
3. In the real inventory, split eight dirt with right click, deposit with left click and merge them back. Verify eight items and an empty cursor on both client and server. Recheck after automatic swaps; neither GUI click path nor Bogo Sorter shortcuts should remain disabled.
4. Before opening a container GUI, put seven sticks in hotbar slot 1, eight stone only in the offhand and leave hotbar slot 5 empty. Configure pick-blockable slot 5. Middle-click a nearby stone schematic, then use Easy Place: client and server must agree on the moved stack, seven remaining stone, empty offhand and untouched seven sticks. Repeat with direct Easy Place, ordinary main-inventory source stacks, and a nonempty destination slot. The client's Bogo Sorter shortcut guard must return to its previous value.
5. Put eight torches in the offhand and eight stone in the hotbar. Easy Place and the printer must place stone, consume one stone and leave all torches. Run the printer with stone only in the offhand. Then configure swap slot 5 and auto-tool, put the only usable pickaxe in the offhand (a native TConstruct steel pickaxe in GTNH), and ask the printer to remove a wrong stone block. Verify the tool moves to slot 5, mines the block and remains a single tool with the expected durability change. Check an offhand water bucket and its returned empty bucket too.
6. Repeat offhand block/tool swaps on a dedicated server. Save/reopen the inventory and inspect server-side quantities; a client picture alone cannot rule out ghost stacks. Test creative middle pick against a schematic, a closer real block, a coincident real block and hidden rendering. Release all input and restore test settings afterward.

On 2026-10-09, cold clients in both target packs passed these input, inventory, creative pick, offhand material, occupied destination, tool and bucket cases under Xvfb/Mesa. Native mouse/key events drove the operations; client and integrated-server inventory inspection confirmed counts. A matching cold 2.9 dedicated server additionally passed offhand pick, direct Easy Place, printer stone and steel-pickaxe swaps. Saved player NBT confirmed seven sticks were preserved, eight stone became seven after one placement, and the single steel tool retained its materials with one point of damage after mining. The vanilla diamond pickaxe was not used for acceptance: the pack's native mining strength for it is zero. Forge loaded version `1.0.0.beta.2` in both clients. Build, Checkstyle and all 592 tests passed. Tested runtime JAR SHA-256: `e8864e976a2ad7cff6296567405c541871e15091faaf45c6b332004859545f3d`. Logs and fixtures are in `<evidence-dir>/input-backhand`; inspected screenshots are `followup/screenshots/input-*`. This covers the named pack versions and cases, not every hotkey chord or third-party input mod.

### Powered complex structures: GregTech and AE2

1. Use synthetic fixtures in disposable worlds in GTNH 2.8.4 and 2.9.0-RC-2. Enable native paste, inventory copying and neighbor updates. Build a working 3 x 4 x 3 Electric Blast Furnace with two Cupronickel coil rings, MV input/output buses and energy hatch, fluid hatches, maintenance hatch and top muffler. Fix maintenance and power it with a native TecTech Debug Power Generator at 128 V / 2 A. Run one cobalt dust plus programmed circuit 1 through the native 120 EU/t, 1750-tick recipe; require one cobalt ingot in the output bus. Do not fabricate the output or force the controller's formed flag.
2. Disable the source controller, refill one recipe and empty the output. Capture the furnace and generator together. Separately rotate 90 degrees around Y, mirror Z, and rotate Y then mirror X. Save/reload each transform and paste into empty loaded chunks. Verify the copied input, controller/hatch/generator directions and maintenance before enabling each controller. Require normal structure formation and an actual cobalt ingot from every copy without rebuilding or refilling it.
3. Build an AE2 network with creative energy cell, controller, six smart cables and a drive. Attach a north-facing crafting terminal, an upward storage monitor with spin 1, a downward terminal with spin 2, a south storage bus, north export bus and south import bus. Connect three chests; put 64 diamonds behind the storage bus, configure the monitor to diamonds after channels settle, and filter the export bus to gold. Import 16 gold through the third chest and wait for all 16 at the export chest. Check powered terminals, channel lines and the monitor's 64-item display.
4. Capture the stable AE2 network. Apply the same three transforms, inspect every part's direction, power/channel state and floor/ceiling spin, and compare material totals with the original. Each host must retain one center cable and exactly its original number of side parts. Look specifically for a terminal or bus left on the old side alongside an unpowered copy on the new side. Save and reload each transformed file and repeat these checks.
5. Paste each AE2 copy with schematic rendering disabled. Inspect the real network, import another 16 gold and require 32 at its export chest, an empty import chest and unchanged 64 diamonds. Save/reopen the world; recheck inventories, parts and active channels. Read saved region NBT independently to compare tile positions, part definitions, directions, filters and GT machine IDs with the transformed files. These cases cover native horizontal transforms of the named structures; they do not establish every machine, grid link, P2P tunnel or autocrafting multiblock.
6. Repeat at least 20 clear-to-air/paste cycles of the complete GT fixture, inspecting all nine machine IDs after each completed paste. Give synthetic controllers normal player ownership first: the 2.9 power-failure tracker cannot remove an artificially ownerless controller. Test fresh placement as well as replacement: pasting onto identical blocks alone does not exercise `onBlockAdded`. The 2.8.4 native async machine scan uses `GTMod.proxy.TICK_LOCK`; queued mutations and shutdown rollback must hold that same lock, restoring its previous hold count even after an exception. Verify edits still work without GregTech and with the 2.9 main-thread machine updater.

The pre-fix 2.8.4 run produced an empty muffler (`mID=0`) in one transformed furnace. A separate 20-cycle clear/paste run reproduced empty input and output buses together on cycle 8; a 20-cycle replacement-only control did not reproduce it. The schematic still contained the correct IDs. Native queue inspection found the GT tick lock released at edit time, and the pack's async scanner can create a default tile while an edit is installing its saved tile. `MachineUpdateLock` now guards queued changes and shutdown rollback; unit tests cover competing access, reentrant exception cleanup and absence of the optional mod. AE2's separate preview bug was reproduced in both packs: moved parts left an old-side copy and lost their power flags. Two regressions cover removal, state migration, spin preservation and repeated transforms.

On 2026-10-11, cold integrated clients passed these three transforms in GTNH 2.8.4 (GT 5.09.51.482, AE2 rv3-beta-695-GTNH) and 2.9.0-RC-2 (GT 5.09.54.205, AE2 rv3-beta-1080-GTNH). Synthetic fixtures used native game APIs and recipes. Every transformed furnace formed and produced one cobalt ingot; every AE2 copy retained its filters and 64 diamonds and exported the additional 16 gold, reaching 32. Preview parts, power, monitor counts, spin, material totals and exported/reloaded files matched; inspected real GUIs showed the cobalt output, gold chest and diamond terminal. Both worlds reopened successfully with those quantities and active networks. With the native GT lock confirmed held during queued work, all nine machine IDs survived each of 40 clear/paste cycles in 2.8.4 and 20 in 2.9. The latter stress fixture was given normal player ownership after an initial native cleanup failure on an ownerless synthetic controller; that fixture correction is not a Plus fix. Saved-world checks passed 225 cells and 63 tile positions/definitions/directions per pack. The reader now handles the big-endian `Blocks16/Data16` arrays used by 2.8.4 as well as the existing 2.9/vanilla formats. Build, Checkstyle and all 597 tests passed. Tested runtime JAR SHA-256: `eed5646d2a702b2b8dd5dc2c001d0cd80ca504c305a84fca7119e002bc5e44dc`. Evidence: `<evidence-dir>/complex-structures` (`final*-check`, `stress*-final`, `recheck*-final`, `verify*-final`) and inspected `followup/screenshots/complex-*`. Dedicated-server versions of these new fixtures, AE2 autocrafting/P2P and other active multiblocks remain untested by this round.

### AE2 cable-host materials and partial builds

1. Count the original powered AE2 fixture above: require 11 material rows and 18 items (six smart cables, six mounted parts, three chests, energy cell, controller and drive). A cable item must not stand in for its attached parts. Stored diamonds, filtered gold and upgrades are not construction materials.
2. Add a second, ordinary terminal to the crafting terminal host's south side, a stone facade on the monitor host's west side, and a glass facade on the downward terminal host's east side. Capture the whole network. Require 13 rows and 21 items: six cables, seven side parts, two distinct facade materials, three chests and three devices. The two ordinary terminals share one row with quantity two; facade items retain their distinct NBT.
3. Put one normally constructed stone facade and one glass facade in the player inventory. Both material rows must show one available item even though the native client facade stream omits the `modid/itemname` fields present on crafted stacks. Align the placement with the complete source or a pasted copy: missing must be zero. Move it to an empty area: all 21 items must be missing. In a disposable copy, remove the north crafting terminal and stone facade, change the downward terminal's spin from 2 to 0, and move the south import bus north. With state comparison enabled, require four missing items and zero missing cables. With Ignore block state enabled, require only the absent crafting terminal and stone facade; an existing part cannot satisfy multiple required parts.
4. Repeat Y rotation, Z mirror and Y-then-X-mirror; export/reload each file. Require the same 13 rows/21 items and correct facade materials, sides and terminal spin. Paste each copy and require zero missing items. Press Refresh in the material GUI after changing the world, inspect the displayed counts and transformed previews, then reopen the world and repeat complete/partial counts.

The original source-versus-transform comparison only established invariance: both lists omitted the six AE2 side parts, reporting five rows instead of eleven. `AE2Items` now reads native part pick stacks and facade stacks, counts each attached item, and matches partial hosts by item/NBT, installation side and panel spin. Ignore-state comparisons retain item identity and multiplicity. Native facade construction normalizes the redundant registry fields for material keys and part matching; this preserves material/other NBT and does not mutate the native stacks. An unnormalized client-stream facade and crafted facade reproduced unequal material keys in the 2.9 game, and a rotated/pasted 2.8 network incorrectly reported both facades missing. A regression covers equivalent keys, facade matching, distinct materials and unchanged source tags.

On 2026-10-11, cold clients in both target packs passed the original 11-row/18-item and expanded 13-row/21-item checks with no skipped cells. Complete sources/copies reported zero missing items, empty placements reported 21, and the partial copies reported four with state comparison or two with Ignore State. Native crafted stone/glass facades each showed one available item. Both refreshed material GUIs were inspected and matched the scan results. All three transformed previews and exported/reloaded files retained the expected counts; pasted copies had zero missing items, including their facades. Reopening both worlds retained the complete and partial results. Independent region reads matched all 15 cable/side-part/facade definitions and their expected transformed sides per case, 90 across both packs. The disposable fixture setup was corrected to request actual facade items (`createFacadeForItem(..., false)`) and use native wrench activation for an already active panel; these setup corrections are not production fixes. Already-loaded files were given a new source name after recapture to avoid the intentional source cache. Build, Checkstyle and all 603 tests passed. Tested runtime JAR SHA-256: `6adb6251462d853c66265e620d0f5a74f09bcb7981cfe83c4869ea81f5df836f`. Evidence: `<evidence-dir>/complex-structures/materials*-final.log`, `materials*-saved.log` and inspected `followup/screenshots/complex-ae-*-materials-*.png`. These material tests cover native cable hosts, not hosts converted to ForgeMultipart or automatic AE2 part placement; no new NEI recipe-tree or dedicated-server claim is made by this round.


### Dedicated powered structures and Multipart initialization

Use separate disposable GTNH 2.8.4 and 2.9.0-RC-2 dedicated servers and clients,
with the identical current Plus JAR on both sides. This does not test older Plus
network interoperability.

1. Build a powered EBF with Cupronickel coils, MV input/output buses and hatches,
   maintenance and a muffler. Run an available native recipe before capture.
2. Build an AE2 controller, powered cell, drive with a storage cell, interface with
   a valid crafting pattern, molecular assembler and crafting CPU. Request a job
   and check the actual input consumption and output, not just the lit controller.
3. Build a separate powered ME-P2P carrier connecting a storage network to a remote
   export bus and chest. Put a stone microblock cover on a cable in the carrier;
   verify that its host becomes ForgeMultipart and still transfers 17 diamonds.
4. Use `/schematic save` from the connected player, then `/schematic download` for
   the server file. A local client capture cannot recover unsynchronized disks,
   patterns or machine inventories, even when the server runs Plus.
5. Paste ordinary, Y-rotated and X-mirrored copies. Each EBF must form and process
   a new native recipe. Each AE2 copy must execute a new crafting job; each P2P copy
   must transfer new input to its own output chest without re-pairing tunnels.
6. Paste the Multipart network with `pasteWithoutUpdates` enabled, then move that
   copy as a whole. Both copies must work immediately. Inspect the world view,
   save the world and check the resulting tiles/parts on disk.

Native observations on 2026-10-13:

- Both packs reproduced a disconnected pasted ME-P2P carrier at the converted
  AE2 cable/stone-cover host. NBT and appearance alone did not expose the missing
  network registration. Invoking the native world-join callback restored the RC-2
  control and delivered all 17 input diamonds.
- With the repair installed in cold clients and servers, each pack passed three
  server-downloaded crafting-network pastes (ordinary/Y rotation/X mirror), four
  P2P/Multipart pastes (ordinary/Y rotation/X mirror/suppressed updates), and a
  whole-network move. Crafting consumed one of four logs and produced two planks;
  P2P delivered 17 newly supplied diamonds to each copy's own output chest.
- Powered native EBF source processing passed. All three downloaded/transformed
  EBF copies formed and produced the expected native recipe output in both packs.
  AE2 disks and encoded patterns also passed server-side automatic crafting after
  transferring the downloaded file in both directions between packs. Import
  reports discarded incompatible client update packets while preserving canonical
  disk/pattern inventories.
- Final world views were inspected with server-authoritative camera positions.
  Saved region checks confirmed cable/cover parts, output-chest contents, cleared
  move sources and native GT products. Test-control save/camera calls were corrected
  separately; their initial errors were not attributed to Plus.
- Versions: GT 5.09.51.482 / 5.09.54.205, AE2 rv3-beta-695 / rv3-beta-1080,
  ForgeMultipart 1.7.2 / 1.7.16 in 2.8.4 / RC-2 respectively.

The repaired `ForgeMultipart.onPlaced` invokes native chunk-load initialization
once after installing the tile and before sending its description. Both normal
and silent placement use it; the move tool shares the silent writer. Automated
checks cover ordering, cancellation without repeated initialization and a failed
initialization not counting or synchronizing a successful paste.

Evidence: `<evidence-dir>/dedicated-complex/`, including `baseline-{284,290}.log`,
`fmp-join-control-290.log`, `accept-{284,290}.log`, `gt-{284,290}.log`, downloaded
files, `saved-regions.log` and `screenshots/`. Tested JAR SHA-256:
`a6eddc00feec9f2ba0e3949d6b679197219df2308fe15dd9d2a6452c82201498`.
Build and all 672 automated tests passed. This covers the EBF and ME-P2P fixture,
not every GT controller, P2P type or third-party multipart.

### External Angelica shader packs

Use disposable copies of a generated world. Keep each pack's native Angelica
(2.8.4: 1.0.0-beta66b; 2.9.0-RC-2: 2.2.28). Do not install OptiFine alongside it.
Use unmodified Complementary Reimagined r5.2.2 in both packs; additionally test
r5.9.2 in 2.9. Select VERYLOW, which retains terrain shadows. Exact upstream
commits, archive hashes and environment details are in
[the shader audit](compatibility/render-audit.md#external-shader-packs-2026-10-12).

1. With shaders disabled, build paired source/empty-preview areas on the same
   floor: single/double/ender chests, an empty crystal chest, clear/stained glass,
   stairs, slabs, leaves, GT industrial glass, a filled TConstruct seared tank,
   contained water, stone and glowstone. Capture the source and move its preview
   beside it. Inspect front and reverse views with overlays disabled.
2. Enable the shader pack. Verify a visibly changed sky/lighting and active
   shader pipeline; merely selecting the pack is insufficient. When diagnosing,
   check IrisApi.isShaderPackInUse and whether the pipeline is deferred or the
   fixed-function fallback. Repeat the same cameras on real blocks and previews.
3. Toggle schematic alpha and missing-block overlays; show/hide placement boxes
   and all schematic rendering. Check the real scene after hiding the preview.
   Compare apparently missing transparent faces against a dark real backdrop,
   with shaders off/on and alpha off/on, before classifying a missing-face bug.
4. Toggle shaders off and on again. Save, exit the world, wait until the integrated
   server has fully stopped, and reopen it. Also verify a cold client with the
   shader pack selected. Inspect representative mod geometry separately; this
   does not validate machine operation or shader support for every renderer.
5. Record failed pack loads separately. In 2.8.4, r5.9.2 was rejected during
   Angelica's shadow-program transformation and fell back to ordinary rendering;
   this case must never be counted as successful shader rendering.

On 2026-10-12, actual Overworld views passed the representative shell/face checks
with r5.2.2 in both native clients and r5.9.2 in 2.9. All successful cases reported
an active deferred shader pipeline. Close black-background comparisons disproved
an initial suspicion of missing translucent glass: its faces/textures were still
present, but contrast was low against the bright floor. Preview lighting/water
appearance differs from real blocks; no complete material/reflection/shadow parity
is claimed. No new block/tile rendering exception was observed in the successful
runs. The 2.9 world reopened with r5.9.2 active. A 2.8 scripted reentry attempted
to start a server during SERVER_STOPPING; that attempt is invalid as a shader
transition check. A complete 2.8 client restart with r5.2.2 already selected then
reopened the world successfully, with shaders active and the inspected preview
intact. Both packs also passed front/reverse preview checks for the existing
18-shape ArchitectureCraft fixture, AE2 parts/facades and GT Electric Blast Furnace
with alpha disabled; machine operation and material counts were not retested by
these additional views. Evidence is in `<evidence-dir>/shader-validation`.
Build, Checkstyle and all 603 tests passed; no production code changed.
The later OptiFine checks are recorded below. Real GPUs, other shader families,
higher presets and Nether/End shader effects remain untested.

### Separate OptiFine shader validation

On 2026-10-12, independent copies of both target packs and the synthetic shader
worlds were prepared under `<evidence-dir>/optifine-validation`.
Angelica was removed from these copies; the original instances were preserved.
Hodgepodge's `removeOptifineGLErrors` was disabled in the copies so diagnostics
remain visible. Current Plus was installed, with its exact filename and checksum
in `preparation.json`. Launch argument paths and missing-OptiFine guards were
checked. The initial official download attempt was blocked by CONNECT 403;
access succeeded on retry. The actual runtime was OptiFine 1.7.10 HD U E7,
which includes ShadersMod. Exact binaries, hashes and results are recorded in
`compatibility/render-audit.md` and the external directory's `runtime.json`.

1. Obtain a verified official Minecraft 1.7.10 OptiFine runtime, recording its
   version and hash. Verify whether that release needs a separate compatible
   ShadersMod, and record its version/source if needed. A dummy compile-time JAR
   or modern-version documentation cannot establish runtime compatibility.
2. Start each isolated pack with its native libraries and shaders disabled.
   Verify that OptiFine actually loaded and Angelica is absent. Open only the
   copied `Optifine284` / `Optifine290` world and capture a rendering baseline.
3. Enable a shader pack supported by the actual installed shader loader. Verify
   active shader rendering through native loader state/logs and visible effects;
   do not reuse the Angelica/Iris activation check or count a fallback as a pass.
4. Repeat the preceding shader section's paired block/preview checks, opposite
   views, dark glass backdrops, alpha/overlays and representative mod structures.
   Record any required configuration changes and keep native rendering errors.
5. Check shader off/on, a completed integrated-server shutdown before reentry,
   and cold startup. If either startup or rendering fails, compare a further
   disposable copy without Plus before attributing the failure to this mod.
6. Record successful, failed and unexecuted combinations separately. Angelica
   results do not establish OptiFine support. Wait for native terrain to finish
   rebuilding after a shader toggle before judging a screenshot: the first
   2.8 post-toggle capture lacked terrain, while later settled views were intact.

Actual 2.8.4 checks with E7 and Complementary r5.2.2 entered the copied world,
initialized nonzero native shader programs and visibly rendered terrain shadows.
Inspected paired real/preview chests, glass against black backdrops, GT glass,
filled tank/water, alpha/overlays and hide/show. ArchitectureCraft's 18 shapes,
AE2 parts/facades and the GT Electric Blast Furnace were inspected from both
sides with alpha disabled. These samples showed no new missing face. Shader
off/on returned to an active initialized pack and intact settled previews.
A full cold client with r5.2.2 selected reopened the world with active shaders
and intact inspected previews. Native state confirmed complete integrated-server
shutdown before the restart. The cold run also logged repeated
`GL error 0x0500: Invalid enum at pre-final` diagnostics.
A cold control with only Plus removed, on a further copy of that world, activated
the same shader pack and reproduced this diagnostic. Keep GL-error suppression
disabled in both cases when repeating the comparison. The precise upstream/driver
cause is unassigned; the error is not specific to Plus.
Preview lighting/water remains different from real blocks; E7 also logs
unsupported modern pack options/block mappings, so this is not full material
or shader-feature parity.

Native 2.9.0-RC-2 failed before the main menu with E7, even with shaders OFF.
Removing only Plus reproduced Hodgepodge 2.7.215's
`MixinFontRenderer_FallbackShadow` error: missing `@Shadow field_78286_d` in
OptiFine `FontRenderer`. This is a pack/OptiFine startup conflict. No world or
shader rendering was tested in that combination; no third-party mod was patched
or downgraded. Recheck both with and without Plus when the upstream conflict is
resolved. r5.9.2 on OptiFine, other shader families, real GPUs, higher settings
and Nether/End remain untested.
Build, main/test Checkstyle and a fresh run of all 603 tests passed with no
failures, errors or skips; no production code changed.

### Release 1.0.0-beta.4 verification

The current release uses the Git tag as the version source for generated
`SchematicaVersion.VERSION`, `mcmod.info` and the runtime JAR name. Run
`./gradlew build` and `LC_ALL=C.UTF-8 ./gradlew test`, then repeat the build
on tag `1.0.0-beta.4` and check `schematica_plus-1.0.0-beta.4.jar`.
Run the dedicated-server startup gate and require normal startup and clean stop.
Push the tag to trigger `.github/workflows/release-tags.yml`; its verification
job must pass before the runtime JAR is published as a GitHub prerelease.

For a game version check, install that runtime JAR in a disposable instance,
open Mods and confirm Schematica Plus reports `1.0.0-beta.4`. Existing dated
pack acceptance remains scoped to its recorded builds and fixtures; a
version-only release does not imply those game scenarios were rerun.

### Release 1.0.0-beta.3 verification

The release uses the Git tag as the version source for generated
`SchematicaVersion.VERSION`, `mcmod.info` and the runtime JAR name. Run
`./gradlew build` and `LC_ALL=C.UTF-8 ./gradlew test`, then repeat the build
on tag `1.0.0-beta.3` and check `schematica_plus-1.0.0-beta.3.jar`.
Run the dedicated-server startup gate and require normal startup and clean stop.
Push the tag to trigger `.github/workflows/release-tags.yml`; its verification
job must pass before the runtime JAR is published as a GitHub prerelease.

For a game version check, install that runtime JAR in a disposable instance,
open Mods and confirm Schematica Plus reports `1.0.0-beta.3`. Existing dated
pack acceptance above remains scoped to its recorded builds and fixtures;
a version-only release does not imply those game scenarios were rerun.

## Descriptive metadata across writable formats

1. In both native target packs, save a small vanilla selection as `.schematic`,
   `.schemplus` and `.litematic`; use Manager conversion for structure `.nbt`.
   Check the saving player's author name, file-stem title, creation/modification
   time, dimensions and an initially absent preview. Keep classic Alpha fixtures
   within its block-ID/region limits so unrelated format upgrades are distinguishable.
2. In Manager, change the author and metadata name. Add a Chinese/English multiline
   description, exercise Reset/Cancel, then confirm. Reopen the file and inspect
   the info panel and the stored tags. At large GUI scale, scroll the info panel to
   inspect the entire thumbnail; 3D preview zoom must still work over the model.
   File names and block data must be unchanged.
3. Set Preview, close the GUI and press the normal screenshot key. Reopen Manager
   and inspect the embedded image. Repeat using a non-square `thumb.png` beside the
   file and Ctrl+Alt+Shift-click Set Preview. Require a centered 120x120 image; move
   the PNG away and confirm the embedded preview remains. Remove Preview and verify
   attribution/description survive. Right-click Edit cancels a pending screenshot.
4. Load a source before editing its metadata, then edit blocks with REBUILD and
   save to `.schemplus`. Both edits must survive, including the original creation
   time and author. Metadata-only changes must not mark a source's blocks as edited.
   An untouched source saved to another file must retain the original bytes.
5. Convert each writable format into the others. Preserve name, author, description,
   creation time and preview; advance modification time and recompute dimensions
   and available format statistics.
   Existing extended-ID/independent-region upgrades still apply. Check `.nbt` native
   `author` agrees with its optional metadata. Reopen after a complete client restart.
6. Download a decorated file from a matching current server, including a flat file
   without independent regions. Repeat with explicit `.litematic` and `.nbt`
   command arguments. Check the final downloaded file's description and
   preview, not just a successful download message. Incomplete payloads must not save.
7. For `.schematic`, compare block/metadata arrays and encoding before/after editing;
   no `.schemplus` file may be created solely for descriptive metadata. Preserve
   unrelated root/extension tags. For modern `.litematic`, preserve data version and
   long block-state arrays during metadata-only edits. Invalid/non-square/oversized
   thumbnail arrays must not create a browser texture.

Automated codec tests exercise all 16 writable-format pairs, new-save defaults,
classic block-array preservation, metadata copy isolation, loaded-source REBUILD
saves, unknown long-array preservation, native structure-author synchronization,
flat download payloads and preview validation. These checks do not establish
that external schematic editors retain the optional metadata.

Dated observations (2026-10-15): `./gradlew build` and
`LC_ALL=C.UTF-8 ./gradlew test` pass all **684 tests**, with no failures, errors or
skips. Native GTNH 2.8.4 and 2.9.0-RC-2 runs use their own mod sets,
Java 21/LWJGL 3 and software GL on disposable instances.

The native fixtures contain a 3x2x2 stone grid. Game-thread calls exercised the
public save/convert APIs, while real mouse/keyboard input exercised metadata
name/author editing, Chinese/English multiline descriptions, Reset/Cancel and
PNG import. All 16 conversion pairs per pack retained the descriptive fields and
embedded pixels. An export to classic Alpha that needs independent regions still
uses the existing `.schemplus` upgrade. SourceEditor REBUILD changes to both flat
and Litematic region sources retained metadata and the changed gold block on readback;
this exercised the actual working-copy API, not a manual REBUILD-tool click test.
The 160x120 PNG's centered 120x120 crop was verified pixel by pixel.

Both clients and matching dedicated servers were cold-started with runtime JAR
SHA-256 `e90ed86db95136641c1a55463043c34df11cefae47530411efcf9a37a53b1980`.
Eight downloads, one per writable format per pack, retained names, authors,
multiline descriptions, creation times and all embedded preview pixels. The
explicit `.litematic`/`.nbt` command cases exposed and verified the filename lookup
fix. Initial test connection failures came from bypassing Forge's multiplayer
initialization; the successful runs use its normal connection entry.

Both clients were then cold-started with the final runtime JAR SHA-256
`2918c054a949f53a36ddb03274d1b761d9c60d84f7b8dbcfea1f0dd19aeed2e8`.
This includes native structure-author synchronization and the 3D preview scissor
intersection. Earlier live replacement of graphics classes bypassed the packs'
OpenGL transformation and is excluded from graphical acceptance. The final cold
runs verify the actual pack-transformed classes.

In both final clients, real `/schematic save` captures and subsequent downloads
write the saving player, file-stem title, creation time and empty description;
previews remain absent until explicitly set. Native structure-author edits made
outside the optional namespace survive a subsequent description change. The
scrolled information panel, partial/full 3D preview and wheel zoom were visually
checked in both packs. Both runs also exercised native preview removal, PNG import
and right-click cancellation, including Chinese labels. GTNH 2.8.4 had cloud
connection read timeouts; its remaining file-only manager checks were completed
from the menu. These timeouts are not reported as fixed by this change.

F2 capture was also verified visually in the final clients: GTNH 2.9.0-RC-2 while
connected to the dedicated server, and GTNH 2.8.4 in a disposable local-world copy
after its connection timed out. Reopening the browser displayed the captured world
thumbnail. File readback confirmed 120x120 pixels with the existing author and
multiline description preserved. Evidence includes `verified-f2-290.png` and
`verified-local-f2-284.png`; the earlier PNG import, removal and cancellation checks
were performed separately from these F2 captures.

### Paste continuation and Tile Entity diagnostics

Use disposable world copies and copies of reproduction schematics. Run these
cases with each pack's native mods in GTNH 2.8.4 and 2.9.0-RC-2. Use the same
current Plus build on both sides of any dedicated-server test.

1. Paste a structure containing unsupported standing torches and crops, followed
   by ordinary blocks and machines. In normal mode, a successful write that turns
   back into air during its placement callback must not interrupt later cells or
   count as a successful placement. Plants may also disappear during later native
   updates. Repeat with suitable soil/support and check the resulting blocks and
   tile entities independently of the chat message.
2. In a disposable captured job, replace one early tile's NBT `id` with a nonexistent
   test identifier. Test normal and no-update pastes. The bad tile must be reported
   with coordinates, block metadata and NBT ID; later tiles must still be restored.
   Finish with a partial-result summary, without counting the failed tile as a
   successful block. Keep the original schematic untouched.
3. Force one block write, metadata restoration, tile initialization, neighbor
   callback or silent light/client refresh to fail. Continue other positions and
   count each failed position once. Restore temporary snapshot/drop-suppression
   flags even after exceptions. A failed entity spawn must not prevent later
   entity attempts. Non-paste edit and MOVE rollback behavior retain their rules.
4. Check a job where every position fails, one where all placed blocks immediately
   remove themselves, replacement-filtered jobs and ordinary repeats. Distinguish
   a partial/error result from a no-changes result; neither may claim all blocks
   were successfully placed.
5. Force command paste and return a native error for one position, then success
   for subsequent commands. The queue must continue and count acknowledged writes,
   including mixed replies in the last batch. Command construction/send errors
   must not leave phantom pending replies. Missing replies, lost permission,
   disconnects and explicit cancellation still end the task.
6. Make an early grid copy partially fail and verify later copies run. Repeat with
   the current server protocol and its partial-result status. Project stages that
   require a successful paste must not advance on a partial result. Cancellation
   must not trigger the next grid copy.

Automated coverage includes placement/readback refusals, self-removal, missing
saved/default tile factories, callback exceptions, metadata and neighbor failures,
failed entity attempts, silent refresh continuation, command failures and replies,
partial-result serialization and grid continuation/cancellation policy. These
checks alone do not establish compatibility with every mod's callbacks.

Dated observations (2026-10-16): build and all **719 automated tests** pass with
zero failures, errors or skips. Both native packs were cold-started with runtime
JAR SHA-256 `c4a23f1d60a109fbcb26d20da9cacbcdee26c006cf729c4f14d3fc2d200e7ed7`.
Tests used generated world copies and an unmodified private schematic copy; no
private input or ownership fields are included in the repository. Game-thread
fixture setup and the actual paste queues were used; this is not a keyboard-only
workflow test or a claim that every pasted machine was functionally exercised.

The reported file has 674 non-air cells and 322 tile entities. On the baseline
whose paste implementation matches beta.3, normal pasting into unsupported air
stopped at a standing torch after 570 counted blocks. All 322 saved tile entities
could be read and recreated in both packs. Suitable ground/support allowed all
674 blocks and 322 tiles to remain, so the separate report that every tile entity
fails to create was not reproduced by this file.

With the changed build, both packs produced these results:

| Case | Observed result |
| --- | --- |
| Normal paste without external support | Traversed the whole schematic; 649 writes counted, excluding 25 torches removed immediately by placement callbacks. Native crop updates later left 451 matching blocks and 288 tiles. This is not a guarantee of unsupported plants surviving. |
| Normal and no-update paste with suitable support | 674 matching blocks and 322 tiles; no recorded errors. |
| No-update paste without support | Traversed all cells and counted 674 initial writes; later native plant updates still apply. |
| One early tile given an invalid NBT ID in a captured job, normal and no-update modes | Traversal completed with 673 successful blocks and one failed tile position. Chat showed the partial result, coordinates, block/metadata and invalid ID. The block's default tile can remain at the failed position; its presence is not counted as successful NBT restoration. |
| Two grid jobs, with the first containing the invalid tile | First job reported 673 successful positions and one failure; the second still ran and retained all 674 blocks and 322 tiles. |

The Chinese partial-result chat and resulting structures were inspected in both
clients. Evidence names include `fixed-284-normal-bad-tile.png` and
`fixed-290-normal-bad-tile.png`. The dedicated-server partial-result packet and
client continuation policy have automated coverage; this round's native paste
and grid runs used integrated servers.

Independent saved-region readback using `tools/headless/regions.py` also matched
all 674 non-air block names and all 322 tile IDs for the supported normal paste,
supported no-update paste and successful second grid copy in each pack (six saved
structures). This confirms persistence of those blocks and tile types, not every
machine's operating state or every mod-private NBT field.

Forced-command acceptance also passed in both native packs after cold restart
with the same JAR. A temporary test command returned `commands.setblock.noChange`
for the first cell and delegated the following two cells to native `setblock`.
The first remained air, the next two became stone, and chat reported two successful
blocks and one failure with traversal complete. Actual reply packets drove the
queue; success was not simulated in the client. Screenshots include
`fixed-284-command-partial.png` and `fixed-290-command-partial.png`.
Earlier incomplete test-command interface implementations caused test-instance
crashes and are excluded from acceptance. The corrected cases used fresh generated
world copies and were saved and closed normally. No corresponding product-code
change was needed. Standalone server-without-Plus acceptance was not repeated in
this round; the forced-command tests exercised the same client fallback queue.

## Server schematics and shared placements

Development client, integrated server, superflat creative world, GUI scale 3:

- `/schematic save tower <x1 y1 z1 x2 y2 z2>` wrote `tower.schematic`.
- Server Schematics opened from the main menu with My Schematics and Public at the
  top level. Double-clicking a folder entered it; the upload screen listed
  `tower.schematic` with its metadata and the target folder. Upload stored the file
  under the save's `schematica_plus/cloud/<uuid>/private`, refreshed the list, marked
  the row as downloaded and showed the success message. Make Public copied it to
  `public` with an owner entry; storage counted both copies. Create placement loaded
  the cached copy.
- In Schematic Placements, Share without Shift showed `Press shift to share`; with
  Shift the file was uploaded under its SHA-256 name in `cloud/shared`, the placement
  was registered in `schematica_plus/shared_placements.json`, the Share button was
  disabled and the server badge appeared.
- View Syncmatics listed the placement with its details; Unload and Load switched the
  button label and placement. In the placement configuration, unlocking showed the
  modification-accepted message and locking again published version 1.

Not yet tested: dedicated server with two clients (live updates, automatic loading
on join and dimension change, concurrent modification denial), uploads near the
size and quota limits, `/schematic shared list|remove`, and the narrow-screen
fallback of the View Syncmatics button.
