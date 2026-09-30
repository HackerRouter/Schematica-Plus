# Verification

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
  original minimum-corner pivot stays at the entered position, and the other
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
  rejected geometry must leave the previous placement usable. Signed-size region
  pivots and distinct overlapping Litematic payloads are still importer limitations.
  These native game checks have not been run by the agent.
