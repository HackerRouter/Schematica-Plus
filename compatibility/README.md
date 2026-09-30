# GTNH compatibility scope

Targets: Minecraft 1.7.10, GTNH **2.8.4** and **2.9.0-RC-1**. These are source-backed
adapters awaiting pack-level game tests, not a claim that every block renders identically.

The official manifests are [2.8.4](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.8.4/README.md)
and [2.9.0-RC-1](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.9.0-RC-1/README.md).
[gtnh-mods.json](gtnh-mods.json) retains their 231 and 242 entries, respectively,
with a 256-entry union that includes renamed projects. `source_refs` records exact
release tags and commit IDs for 90 repositories. A recorded revision means the
source was acquired and version-pinned; it does not mean every class was reviewed.
The source investigation followed tile persistence, client synchronization,
render-state writers/readers and fluid-container placement paths.

## Implemented adapters

All implementations are optional, under
[`compat`](../src/main/java/com/github/lunatrius/schematica/compat).
None of these mods becomes a required dependency.

| Family | Captured/restored state and source entry points |
| --- | --- |
| GregTech and inherited machines | Standard tile update plus `getUpdateData`/`onValueUpdate`; client pipe connections, turbine/filter formed flags. Pipe rotations/mirrors also transform connection masks, covers, redstone sides and disabled fluid inputs. |
| AE2 / AE2 Fluid Crafting | Client cable connections/channels, power flags, monitor/terminal state and machine indicators. Uses explicit visual fields instead of querying the client's absent server grid. Handles dynamic `TileCableBus` replacement without losing coordinates or saved NBT. |
| ForgeMultipart / ProjectRed / AE2 multipart | Native part factories and per-part `writeDesc`/`readDesc`; AE2 parts use the client-field adapter. Preserves lamp state, wires and multipart render state in matching environments. |
| Forestry | `IStreamable.writeData/readData`, including inherited machines. |
| Railcraft | `RailcraftTileEntity.writePacketData/readPacketData`. |
| BuildCraft | `TileBuildCraft` streams and pipe render/pluggable/pipe state; fluid render cache and kinesis power display. Does not call the pipe packet-sending method. |
| LogisticsPipes | `LogisticsTileGenericPipe` render state, BuildCraft pluggables and pipe streams. |
| Galacticraft | `TileEntityAdvanced` fields annotated for client synchronization; supported primitives, tanks, energy storage and relative `BlockVec3` links. Inherited add-ons receive the same field handling. |
| OpenComputers | Preserves root tile identity and coordinates omitted by client NBT writers; retains native synchronized NBT. |
| JABBA | Captures barrel NBT without invoking the writer's world-position registration. |
| EnderStorage / Translocators | Tank fluid/pressure, chest lids and attachment visual fields. Advances selected client-only interpolation without ticking transfers. |
| IronChest | Crystal chest `topStacks`, lid position and viewer count, separately from stored inventory. |
| Binnie | Machine `syncToNBT/syncFromNBT`; flower `RenderInfo` colors, type, age, section and wilt/flower flags. |
| MalisisDoors | Door state and animation timer, with visual completion that avoids operating neighboring doors. |
| Steve's Addons | RF node input/output-side indicators. |
| Thaumic Exploration | Bound chest/jar client seal colors and access/lid state without calling the shared-storage color setter. |

## Native NBT and standard tile updates

The generic path preserves canonical tile NBT and, where implemented, the S35
description packet. The following inspected examples already serialize their
relevant state through that path; adding a duplicate adapter is unnecessary:

| Family | Inspected native path |
| --- | --- |
| EnderIO | Conduit NBT connections, external connections, modes and active state; `TileConduitBundle` S35. Rotations/mirrors transform connection directions, per-side modes, filters/upgrades, colors, redstone settings and fluid round-robin masks in both preview and saved NBT. |
| StorageDrawers | Tile NBT/S35. |
| ArchitectureCraft | Shape, material, side and turn in NBT/S35. |
| Carpenter's Blocks | `TEBase` NBT/S35. |
| LittleTiles | `TileEntityLittleTiles` tile-list NBT and S35 rebuild. |
| OpenBlocks / OpenModsLib | `SyncedTileEntity` persistence includes its sync map. |
| bdlib inheritors, including ae2stuff/gendustry | `TileExtended` and world-persistent data slots. |
| FloodLights / OpenModularTurrets | Light state/orientation/color and turret camouflage stack in NBT. |
| Botania / Botanic Horizons | Custom NBT/S35, including automation facing, mana and enabled state. Transient spark events are outside this path. |
| Amazing Trophies / NewHorizonsCoreMod baby chests | Trophy properties and chest synchronized NBT. |
| IC2 Experimental | `TileEntityBlock` active/facing are persisted in NBT; checked against the 2.2.828 development artifact used by both manifests. |

These are evidence for the named persistence paths, not blanket validation of every
tile, add-on, special renderer or animation in those projects. Block-only mods use
the existing block-name/metadata storage. Libraries, UI, recipe and entity-only
mods do not each need a tile-visual adapter.

## Fluid printer

Supported routes are native `ItemBucket` placement (including EnderIO/Tinkers'
Construct subclasses), Railcraft buckets, Forestry buckets, GregTech volumetric
flasks and IC2 universal fluid cells. Native item use handles consumption, return
containers and server interaction. The printer requires a source-placeable fluid
and at least 1000 mB; it does not fabricate placement for a storage-only item.

Forestry cans/capsules, Galacticraft canisters and Botania's fluid-removing open
bucket do not supply the required native source-placement behavior. They are not
accepted merely because they contain or interact with fluid.

## File behavior and limits

- Enable NBT saving and recapture the original build. Old schematics cannot
  recreate client fields that were never saved.
- Singleplayer uses the integrated server world. Remote capture can preserve
  synchronized visual data, but cannot recover inventory/genetics/private machine
  data that the server never sent.
- Canonical NBT and visual state are separate. Loading a smaller client update
  must not overwrite unsynchronized inventories when saving again or pasting.
- Binary adapters are version-guarded. Multipart/LogisticsPipes streams also use
  the loaded-mod version fingerprint. A mismatch skips that visual payload and
  keeps canonical NBT. This is not a general numeric-registry migration mechanism;
  cross-pack/world ID remapping inside opaque mod payloads remains unverified.
- World time, frame interpolation and selected safe visual updates advance.
  Full machine ticking is not enabled. Private tick counters, transient transport
  particles and entity-targeted effects are not universally reproduced.
- Native block rotations remain in use. The added side-state transform covers EnderIO conduits and GT
  pipes; AE2 multipart orientations, GT machine extended facings and other custom
  encodings are not all transformed. Absolute links inside opaque NBT/streams,
  including link rotation, need additional per-mod handling.
- Neighbors outside the selection, layer-filtered neighbor queries, biome tint,
  original lighting and shader-specific output can still change the appearance.
- As of 2026-09-30, the public source URLs for Catwalks-2, Galaxy-Space-GTNH,
  harvestcraft and Automagy-GTNH were unavailable. They are not source-verified.
  The manifests' binary-only entries such as Thaumcraft, Witchery, Extra Utilities
  and BiblioCraft have not received a complete tile-render audit.

## Verification

`gradlew build --offline --no-daemon` compiles, reobfuscates, runs Checkstyle and
executes the unit tests. The tests cover bounded codecs, field restoration,
client/server distinctions, snapshot isolation, stream framing and transformations.
Small test fixtures do not reproduce entire upstream mods and are not packaged.

No pack-level game test has been run for this change. Follow [TESTING.md](../TESTING.md)
in both target packs, using singleplayer and a remote server. Include Angelica,
Freecam, multiple schematic instances, chunk boundaries, rotations, dimension
changes and a save/reload round trip before treating the build as release-ready.
