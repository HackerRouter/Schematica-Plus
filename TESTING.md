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
  tooltip and cannot run. Configure/Forge config still open the earlier screens.
