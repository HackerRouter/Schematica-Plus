# Verification

## Dedicated-server save queue and release gate

- `ServerSaveQueueLoadingTest` loads the shared save queue with Minecraft client,
  Plus client and LWJGL classes unavailable, including event-method reflection.
- Start `gradlew runServer` in a fresh test directory with the EULA accepted.
  Require the normal `Done (...)!` startup message and clean shutdown after `stop`.
  Initialization must not load EntityClientPlayerMP, WorldClient or Minecraft.
- On a dedicated server, run a server-side schematic save with a player connected.
  In singleplayer, save an area, clone it to memory and save a project version;
  client callbacks must still run on the client thread after capture completes.
- The tag release workflow runs the same build/tests/server smoke check before
  publishing. Keep that gate enabled and publish only the main runtime JAR.

Run `gradlew build` for compilation, packaging, Checkstyle and unit tests. The tests
exercise bounded file/NBT inputs and pure transformation logic without starting
Minecraft. Forge world initialization requires LaunchWrapper and is not exercised
by the plain JUnit process.

For the new UI foundation, bind `UI component test` in Controls (unbound by
default), then open it in a world. This page only uses synthetic data.

- In both GTNH 2.8.4 and 2.9.0-RC-1, test English/Chinese, GUI scales 1/2/3/Auto,
  Unicode font, window resizing, an odd window size, and the minimum scaled
  viewport (320x240). Text, clipping and hit targets must agree.
- Search `019`, clear, select rows, use arrows/Home/End/PageUp/PageDown/Enter,
  double-click a row, scroll and drag the scrollbar outside its bounds. Scroll
  must clamp after filtering, and only visible rows should render.
- Use Tab/Shift+Tab, select text with Shift/arrows or mouse drag, and copy/paste.
  Edit the number to an empty string, minus sign and out-of-range values, then
  press Enter or move focus. Step with arrows/the focused mouse wheel.
- Disable the list: it must not receive mouse/keyboard input. Open the reset
  dialog: background controls must not react. Escape dismisses only the dialog;
  Tab stays inside it. Confirm and cancel must have distinct effects.
- Open a child page and return. Search, numeric value, selection and scrolling
  must persist in the parent, including after resizing and reopening a dialog.
- Close the page and inspect the world, NEI/inventory item textures and other
  mod screens. No scissor, tint, blend, depth or keyboard-repeat state should leak.

Headless UI tests cover input routing, focus/capture/modal isolation, clipping
math and list state/visible-row behavior. They do not validate native GL drawing,
font/IME behavior or modpack rendering integrations; those require the checks above.

Before a release, use a disposable 1.7.10 world to check:

- Open M, load a schematic through a nested folder, search by name, double-click
  a folder, refresh, and use the keyboard. Check `.schematic`, `.schemplus` and
  `.litematic`, Unicode/spaced filenames, empty folders, and a corrupt schematic.
  Back must never load; a failed load must leave a useful message on the page.
- Load the same file twice, return through the main menu, and verify the two
  instances and previous placement/rotation restoration. Try the direct load and
  controls bindings. Confirm that an existing customized M binding is retained.
- Test the new menu/load/save pages at 320x240 scaled size, larger windows,
  Unicode font and English/Chinese. Long names, paths and messages have tooltips.
  Return from legacy instances/control/materials/config pages to the new menu.
- Open N, edit both corner coordinates, commit with Enter and by clicking Save,
  and move each point to the player. Turn on Selection, enter a name, select a
  subfolder, return and resize: the name, coordinates and format must survive.
- Save a small selection in each output format; also paste an existing suffix in
  the name field and ensure it is not doubled. Save high block IDs while selecting
  `.schematic` and verify the automatic `.schemplus` name reported in chat.
- Save over an existing file, first cancel/Escape (file unchanged), then confirm.
  Check invalid names, removed output folders, out-of-range Y, oversized selections,
  repeated saves during a pending job, and server-disallowed load/save actions.
  A queued message must not be mistaken for completion. After submission, change
  the NBT/entity toggles and confirm that the queued capture retains its options.

- Run the compatibility scenarios in both GTNH 2.8.4 and 2.9.0-RC-1. Exact
  upstream versions and inspected source tags are recorded in
  `compatibility/gtnh-mods.json`; a source audit is not an in-game test.
- Save/reload Forestry machines and apiaries, Railcraft tanks/signals and
  BuildCraft pipes with wires, facades and gates, in singleplayer and multiplayer.
  Compare connections, fluid levels, active textures and covers. Loading a file
  from a different protocol version must skip its opaque update and retain NBT.
- Save OpenComputers screens/holograms away from the origin; verify they rebuild
  at schematic coordinates. Save, move and resave a JABBA barrel, including an
  ender-linked barrel; its real-world BSpace registration must remain unchanged.

- Start the client and dedicated server without LunatriusCore; also start with it
  installed for another mod. Open the load, save, control and materials screens.
- Load a two-layer schematic, display one layer, rotate/mirror, then display all
  layers. Verify both layers and entity positions survive.
- Load two differently colored/oriented schematics; switching or unloading one
  must not change the other instance.
- Use two schematics wider/taller than 32 blocks. Switch repeatedly, then hide the
  selected instance and move the other one. Its chunks must continue rebuilding.
- From Controls, open Instances, select another schematic and return. Coordinates,
  movement and rotation must affect the newly selected instance. Rotate a 17x33x49
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
  a WorldEdit schematic, including one with an odd block count. Check old files
  made with reversed nibbles against their saved name mappings.
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
- Tool settings: both paste options must default off. With air-only enabled, paste across air, stone, water, plants and filled chests; only air may change, including during multi-tick pastes. In command mode, commands must use `keep` so the server also checks the target.
- Mod identity: verify `schematica_plus`, the Schematica Plus name and HackerRouter attribution in the mod list. Install original or GTNH Schematica alongside Plus, in either jar order: the client must show the incompatibility screen and the dedicated server must stop with a clear message. Replace the old jar and check that legacy configuration is copied once, existing Plus settings are retained, and config labels/textures still work.
- Mod details: select Schematica Plus in the Forge mod list. Credits must honor line breaks and wrap with the panel width, with Authors, URL and description positioned below them. Check different GUI scales, switch to other mods, and return from Config/Done without losing the expected navigation.
- With block updates disabled in singleplayer, paste floating sand/gravel, redstone, fluids, machines and containers across chunk/section boundaries. Placement must not call block added/broken or neighbor notification hooks; lighting and client display must update and the world must reload correctly. Normal ticks, existing scheduled ticks and mod tile lifecycle hooks remain active. Cancel a large paste and check its completed portion. Command mode must reject this option before sending edits, since vanilla 1.7.10 commands cannot suppress updates.
- BuildCraft: save filled fluid pipes, powered kinesis pipes, gates and facades. Verify the pipe contents and per-side power display after reopening.
- Multipart: test ProjectRed lamps, framed/unframed wires, gates, microblocks and AE2 parts sharing a multipart tile. Check part count, lamp state, covers and connections after reopening. Repeat with a different mod version to check binary payload rejection.
- Printer: place Railcraft fluids using its own bucket alongside GT volumetric flasks, IC2 universal cells, Forestry buckets, EnderIO buckets and Tinkers' Construct buckets. Confirm survival consumes fluid and returns the native empty container.

- Test GTNH 2.8.4 and 2.9.0-RC-1 separately: save powered AE2 smart/dense cables, terminals, storage/crafting monitors, drives and AE2 Fluid Crafting parts. Check connections, channel stripes, colors, indicators and displayed stacks after reopening. The original live network must keep its state after saving.
- Include an AE2 monitor away from local origin (0,0,0): its dynamic tile replacement must preserve its coordinates and saved NBT without interrupting loading of other tiles.
- Save Galacticraft colored pipes, filled machines, solar panels and linked beam receivers/telepads. Check synchronized fields and rebased link coordinates after loading at another position.
- Load an AE2/GT stream captured with a different mod version: incompatible binary state must be skipped while canonical NBT remains available. Named visual fields are separate from version-specific streams.


## Litematica UI phase 3

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
  between selection and Load. Metadata/preview and disabled operations are pending.
- In Area Editor, edit A/B coordinates, use the +/- icons and Move to player, then
  Save Schematic. The save page must use a file browser rather than coordinate fields.
  Select a file to fill its name, type a new name, navigate directories, open Options,
  switch format/NBT, resize and return; the draft and destination must survive.
- Confirm saving with Ignore entities on/off, disabled selection, invalid bounds,
  illegal paths/names, a full queue and overwrite cancellation/confirmation. The
  output still uses .schematic/.schemplus, with automatic upgrade where required.
- Check that unavailable operations stay in the source UI positions, show a clear
  tooltip and cannot run. Configure and Forge Config are superseded by phases 4 and 5 below.


## Litematica UI phase 4: placement configuration

- In M > Schematic Placements > Configure, compare English/Chinese against the
  supplied Litematica at the same scaled resolution, especially heights below and
  above 328. Check the rename field, right-side origin controls, +/- icons, lock
  checkboxes, merged region row, magnifier and the two footer arrangements.
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
- Open legacy Controls and apply an X/Z tilt or vertical mirror; reopening the
  new page must show CUSTOM instead of incorrect planar labels. The legacy binding
  remains available. Four X turns that restore a planar orientation should allow
  planar controls again.
- While the page has a pending coordinate edit, unload the instance or change
  dimension/disconnect. Releasing a held button or closing the screen must not
  modify an instance belonging to the previous world. Navigation must still work.
- The single merged region and unavailable locks, sub-region operations, independent
  rendering/enclosing-box options and verifier must not imply supported behavior.
  Hover text must explain the limitation; disabled controls must not mutate state.


## Litematica UI phase 5: configuration

- Open Config from the Forge mod list with no world loaded, and M > Configuration
  in a world. Done/Escape must return to the correct parent. Compare English and
  Chinese tabs, search positions, rows, reset buttons and numeric-toggle icons with
  the supplied upstream. Resize at GUI scales 1/2/3; narrow layouts must wrap tabs
  without hiding reachable controls. Advanced key settings remain unavailable and explain why.
  Render Layers and Colors are enabled in phases 7 and 8 below.
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


## Litematica UI phase 6: placement materials

- Open Materials from placement A's configuration while B is the active printer
  placement. All counts must describe A; neither the active placement nor the
  printer may switch. Repeat through legacy Controls. Escape returns to the parent;
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
  order; ignore/search/multiplier state is local to this page. Test the magnifier,
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
  invalidation. HUD/cache/raw-material actions must remain disabled with explanations.
- Render enchanted items, 3-D blocks and custom GTNH item renderers, then open other
  UI pages and return to the world. Check lighting/depth/scissor state, tooltip
  layering and clipped rows while scrolling. This requires native game testing.


## Phase 7: render layers (native checks)

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
  backup after testing. These checks have not been run in a live game by the agent.


## Phase 8: colors and HSV editor (native checks)

- Open M > Configuration > Colors and the Forge mod-list Config entry. Compare
  English/Chinese list order, 100-pixel option area, 18x18 indicators and Reset
  positions with upstream. Search in Colors/All; scroll and resize with a color
  draft in progress. The six pending-feature rows must be disabled with tooltips.
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
  No live-game visual validation has been performed by the agent.


## Phase 9: schematic manager and file dialogs (native checks)

- Use disposable copies in a test schematics folder. Open M > Schematic Manager;
  compare the (10,24) browser, footer order, labels and source icons in English and
  Chinese. Select each of .schematic/.schemplus/.litematic and a directory. Check
  GUI scales 1/2/3, narrow windows and resize: groups wrap without covering Main
  Menu. Metadata/import/export execute buttons must remain disabled with tooltips.
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
  deleted source cannot restore after leaving the world. Check the existing save
  overwrite confirmation with its updated layout as well.
- These native GUI and gameplay checks have not been run by the agent.


## Phase 10: loaded source / placement backend (native checks)

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
  Changing the extension must fail; overwriting requires confirmation. Cancelling
  or switching world while the save dialog is open must not write a stale source.
- Keep both a source with no placements and one with multiple placements. Visit
  two dimensions, reconnect and reopen the lists. Restore old session files too.
  Source-only records must not create previews; missing files and invalid entries
  must not block unrelated entries. Deselect every placement before disconnecting
  and verify it stays deselected. Unload a source, leave and return: it must stay
  unloaded in that dimension. Check the legacy instance/control removal buttons.
- Rename both a placed and an unplaced source through Schematic Manager. Verify
  current library paths, Reload, future Create Placement, and session restoration.
  Saving a copy must not change the source path. Restore backups after testing.
- The agent has not run these game/client/GL checks; automated tests are headless.

## Phase 11: upstream translations (native checks)

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

## Phase 12: area selection library (native checks)

- Back up AreaSelection.json. Enter a world with an old-format saved selection:
  its corners and outline must be unchanged. Open Area Selection browser, create
  two selections, rename each and give their boxes different names. Change their
  corners using the editor and tool. Switch entries and reconnect; all names,
  corners, visibility and the active entry must remain independent.
- Copy an area, move one corner of the copy and toggle its outline. The original
  must not change. Search by selection or box name. Rename to an existing name
  (including different letter case) and try an empty name: the operation must
  fail without changing either entry. Cancel a delete confirmation too.
- Unselect the active entry, reconnect and verify no outline reappears. Saving
  and area-based fill/delete/replace must refuse to run with no selected area.
  Delete the active entry and then the last entry; returning to this dimension
  must not recreate either. Other dimensions and servers keep their own lists.
- Move and rotate a placement, then use From Placement. The new selection should
  match its current bounding box, including the last block on each axis. Export
  the selected area through the existing save screen and inspect the result.
  It is one full box; gaps in a merged schematic are included. Placement bounds
  extending outside Y=0..255 must be rejected, not clipped silently.
- Enter a coordinate and press Enter, click another field, resize the window,
  and return from the save screen. Values must not reset or leak into another
  selection. World/dimension changes while a browser, editor, name dialog or
  overwrite confirmation is open must disable actions from the old session.
- On disposable settings, test an unreadable file and an external edit to the
  current session. The mod must retain disk data and show a persistence failure;
  reconnect to load the external edit. Restore backups after testing. Native
  client/GL behavior has not been exercised by the agent.


## Phase 13: multiple subregions (native checks)

These checks supersede Phase 12's Simple-editor and bounding-box expectations.

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
  save/edit must fail before queuing work. The agent has not run these native
  game checks; automated checks do not prove rendered appearance or modded NBT.


## Phase 14: manual origins (native checks)

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
  Hidden/disabled controls must not modify state. No native game tests have been
  run by the agent; source and headless test checks do not replace these checks.
- Use /schematicaDownload on an updated server and client for a file with two
  separated regions and an outside origin. Reload the downloaded file and check
  gaps, origin and rotation. Repeat a plain legacy file with an older client;
  it should still download. An older client requesting a metadata-bearing file
  from a new server must receive an update notice and no incomplete saved file.
  Older servers cannot transmit the origin or region metadata; update both ends.

## Phase 15: independent placement subregions (native checks)

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
  rejected geometry must leave the previous placement usable. See Phase 16 for
  signed-size region pivots and distinct overlapping Litematic payloads.
  These native game checks have not been run by the agent.


## Phase 16: Litematic source preservation and file icons (native checks)

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
- Save Source and compare the .litematic bytes with the original. Server download
  and flat .schemplus exports do not preserve independent overlapping payloads or
  signed anchors yet; do not use these as lossless Litematic source backups.
- Headless build/tests cover data and geometry; the agent has not run these native
  GUI, Forge rendering or paste checks in Minecraft.


## Phase 17: placement activation, locks and enclosing boxes (native checks)

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
- The agent has verified headless tests/builds, not these native Minecraft checks.

## Selection operations (phase 18)

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
  Escape returns to the parent menu. Native checks remain unexecuted by the agent.

## Icon button hover feedback

- Hover search, parent/root/new-directory, coordinate +/- and enclosing-box icons.
  Enabled buttons show the MaLiLib light-gray overlay and white outline inside
  their click area; moving away removes it. Tab focus provides the same cue.
- Disabled buttons do not highlight. Cropped list icons and icons behind a modal
  must not react outside the visible area. File/type icons and passive info icons
  are unchanged. This visual change still requires an in-game check.

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
  is unavailable. Early mod-conflict errors and old-client download rejection
  retain readable English fallback when language resources are unavailable.
- Automated coverage checks source-referenced keys in English/Chinese and formats
  active templates across all 13 bundled locales with English fallback. These
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
  configuration persistence/validation and scaled alignment. Native visual and
  mod-compatibility checks above remain unexecuted by the agent.

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
- On multiplayer, command jobs show scanned positions and commands sent, without
  claiming server execution. Remove stops further sends, including during the
  send delay. Permission/syntax errors, disconnects and world changes clear them.
- Cancel, finish and fail saves/edits, then start another. Change dimension or
  disconnect during capture/editing; old jobs must disappear and release their
  world references. Other players' tasks must not appear in this player's list.
  Client and integrated-server capture must run on their owning tick thread.
- Scope: this page tracks queued area saves, world edits and verifier scans in this process.
  It does not query tasks on a remote server. Material scans still belong to
  their material screen; synchronous source-file saves are not queued tasks.
- Automated tests cover immutable progress publication, owner/dimension filters,
  stale handles, cancellation/write races, edit phases, command preflight/busy
  rejection and queue cleanup. Native GUI, actual world writes and save-file
  cancellation checks above remain unexecuted by the agent.


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
  limits and HUD configuration/translation persistence. Native GUI, lifecycle
  integration and GTNH in-game checks above remain unexecuted by the agent.


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
  tested. Native OpenGL, GUI and GTNH/Freecam checks remain unexecuted by the agent.


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
  Without a Plus server (or with easyPlaceProtocolVersion = None), facing-based blocks
  (furnaces, stairs, pistons) take the player's facing.
- togglePlacementRestriction: using items is blocked where the schematic has air
  near its regions, outside the layer range, into occupied positions, with the
  wrong item, or (for blocks with known orientation rules) with the wrong
  orientation; the warning follows placementRestrictionWarn.
- toggleSignTextPaste: placing a sign on a schematic sign opens the editor filled
  with the schematic text (first 15 characters per line); closing sends it.
- statusInfoHud shows Easy Place / restriction state, layer mode and renderer
  switches at the bottom left; with statusInfoHudAuto it appears for 10 s after
  creating a placement or toggling a render switch while something is hidden.

## Plus translation key audit (supersedes older confirm-dialog steps)

- Save Area as schematic and Save loaded source: saving onto an existing name now
  fails with the Litematica "file already exists" message; hold Shift while
  clicking Save to overwrite (hover shows the Litematica hint). The old overwrite
  confirmation dialogs (phases 3, 9, 10, 13) no longer exist.
- Area Selection browser: the red "-" removes the selection immediately, as in
  Litematica. Re-test that the removed selection does not return after reconnecting.
- Area corner, origin and sub-region coordinate plus/minus buttons: right click
  decreases, Shift ×8, Alt ×4, both ×32; hover shows the MaLiLib plus/minus tip.
- File operations: an invalid name shows the MaLiLib illegal-characters message
  with the name; an existing target shows the MaLiLib "already exists" message.
  Multi-line MaLiLib messages appear on one status line and fully in its hover.
- Paste/Fill/Delete/Replace finish messages are the Litematica ones (Schematic
  pasted in world / Area filled / Area cleared, and the failure variants after a
  cancel or error). Multiplayer paste by commands reports "pasted using N setblock
  commands"; queueing in multiplayer shows "Scheduled task added...". A paste of a
  disabled placement reports the Litematica rendering-disabled message.
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
- Schematic Manager on a .litematic: Schematic Edit → Rename Schematic / Change Author update the info
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
  changes the .litematic name shown in the info panel; other file types show the edit error.
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
- Server download (/schematicaDownload) of such a file from a dedicated server to this client: the
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
