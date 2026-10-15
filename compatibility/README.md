# GTNH compatibility scope

Runtime support targets: Minecraft 1.7.10, GTNH **2.8.4** and **2.9.0-RC-2**,
with each pack's native dependency versions. The adapter tables describe
implementation coverage, not a claim that every block renders identically.

Schematica Plus support targets the current version. When installed on both sides,
use the same current version on client and server. Older Plus releases,
mixed-version interoperation and legacy peer protocols are not support or test
targets. Client-only use on servers without Plus remains supported within the
command/NBT limits below. Both target GTNH packs and their native dependency
versions remain in scope.

The earlier source investigation used the official manifests [2.8.4](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.8.4/README.md)
and [2.9.0-RC-1](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.9.0-RC-1/README.md).
[gtnh-mods.json](gtnh-mods.json) retains their 231 and 242 entries, respectively,
with a 256-entry union that includes renamed projects. `source_refs` records exact
release tags and commit IDs for 90 repositories. A recorded revision means the
source was acquired and version-pinned; it does not mean every class was reviewed.
This registry is historical source provenance, not the RC-2 installation manifest.
Use the exact official RC-2 distribution for that runtime.
The source investigation followed tile persistence, client synchronization,
render-state writers/readers and fluid-container placement paths.

## Implemented adapters

All implementations are optional, under
[`compat`](../src/main/java/com/github/lunatrius/schematica/compat).
None of these mods becomes a required dependency.

| Family | Captured/restored state and source entry points |
| --- | --- |
| GregTech and inherited machines | Standard tile update plus `getUpdateData`/`onValueUpdate`; client pipe connections, turbine/filter formed flags. Pipe rotations/mirrors also transform connection masks, covers, redstone sides and disabled fluid inputs. |
| AE2 / AE2 Fluid Crafting | Client cable connections/channels, power flags, monitor/terminal state and machine indicators. Uses explicit visual fields instead of querying the client's absent server grid. Rotation/mirroring removes vacated preview parts and moves their client state to the new side while retaining the transformed NBT spin. Handles dynamic `TileCableBus` replacement without losing coordinates or saved NBT. |
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

Queued server edits and shutdown rollback also use GregTech's native `TICK_LOCK`
when present. GTNH 2.8.4 runs machine scans asynchronously; without that lock, a
scan can install an empty default tile over a machine being pasted. This guard
does not disable machine updates and remains optional when GregTech is absent.

Powered Electric Blast Furnaces and AE2 storage/bus networks passed native game
checks in both target packs on 2026-10-11: Y rotation, Z mirror, combined Y/X
transform, preview/export/reload, real processing/transfer and world reopening.
See [TESTING.md](../TESTING.md#powered-complex-structures-gregtech-and-ae2) for the
fixtures, exact versions and limits. AE2 autocrafting/P2P and arbitrary active
multiblocks are not covered by these samples.

AE2 cable-host material lists use each native part's pick item and each facade's
item, including multiple parts on one host and the facade's material NBT.
Facade material keys normalize the registry fields omitted by native client
streams so crafted inventory items and placed facades compare equally.
Partial builds are matched by item, mounting side and panel spin. Ignoring block
state relaxes side/spin comparisons but still requires every item and the correct
facade material. Network inventories and installed upgrades are not building
materials. This material counting does not add automatic AE2 part assembly to
the printer.

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

## NEI material planning

Original ChickenBones NEI 1.0.5.120 can run alongside Plus with CodeChickenCore
1.0.7.47 and CodeChickenLib 1.1.3.138. Native plain-Forge/Java 8 checks passed
ordinary recipe/usage lookup, search and the material list, including cold restart.
It lacks the GTNH bookmark-group APIs, so Send to NEI, demand recipes and the S
button stay disabled. This combination does not provide the crafting-chain/tree
integration described below.

GTNH 2.8.4 (NEI 2.8.44) supports bookmark crafting chains; GTNH 2.9.0-RC-2
(NEI 2.8.155) also supports the graphical crafting tree. Sending a material list
creates a saved demand node, shown as a book named after the source schematic file,
including its extension. Lists without a single schematic source use their list name.
Its ingredients are required materials, so they are not mistaken for existing
inventory. Select production
recipes with S or the native tree controls; NEI determines batches and further
ingredients. A request for 17 iron bars with a three-bar recipe requires six
batches, not one. The book is an informational plan, with no crafting overlay.

Snapshots retain item registry names, metadata, NBT and amounts. Sending waits
for NEI's own bookmark loading to finish. S targets the last sent group in the
current world and bookmark namespace; deleted groups and reused numeric IDs do
not acquire that reference. Saved plans survive through NEI's normal storage;
after restarting, use NEI's own controls or send a new list to bind S again.

The demand handler registers its own NEI classification and book icon, including
after handler-info reloads. Its page has no crafting-overlay button. Production
recipes retain native NEI alternatives, multiple outputs, fluids and
zero-consumption equipment. Plus does not replace NEI's recipe-chain arithmetic.

Probability and generic tool placeholders need care. For the inspected impure
magnetite recipe, 2.8.44 plans the displayed iron-dust stack without accounting for
its chance; 2.8.155 uses the 11.11% chance in its batch calculation. The latter is
a planning estimate, not a guaranteed yield. The generic GT hammer alternative
has no concrete tool material/durability and can appear as one tool per craft in
both versions. Ordinary NEI groups behave the same way. These are native planning
limits, not automatic machine operation or a promise of exact tool wear.

## Multipart Easy Place

ProjectRed Easy Place uses native multipart item factories and ordinary item use to
match the schematic's mounting side and gate rotation. Materials count each part,
including multiple parts in one cell. OR gates and red-alloy wires were checked in
GTNH 2.8.4 and 2.9.0-RC-2; client-only installation was also checked against a
2.8.4 dedicated server without Plus. The inventory and backing block are required
in survival. Gate settings that require a screwdriver remain manual; this does
not restore arbitrary NBT or promise automatic printer support for every part.

## Backhand and inventory input

Pick block, Easy Place and printer swaps resolve inventory indices through the
player's container, including Backhand's appended offhand slot. With Inventory
Bogo Sorter installed, each automated swap temporarily enables its client click
prediction and restores the previous guard afterward. Native slot restrictions
still apply. Printer material/tool searches include the offhand after ordinary
inventory slots; a stack is not treated as selected until the local swap succeeds.

The target combinations are Backhand 1.7.7 / Bogo Sorter 1.2.68 in GTNH 2.8.4 and
Backhand 1.8.16 / Bogo Sorter 1.3.54 in GTNH 2.9.0-RC-2. Input and item-count
acceptance steps and observations are recorded in [TESTING.md](../TESTING.md).

## Printer distance

The current client/server protocol supports a printer radius up to 32 blocks,
subject to server `printerMaxRange` and `printerEnabled`. Default client radius 0
keeps normal reach; a server without Plus cannot grant the extension. Requests
use native server placement/mining handlers and actual inventory items. Temporary
printer reach does not extend ordinary mouse interactions. Protection events,
spawn restrictions and loaded-chunk checks remain in force. Mod items with their
own distance checks may still refuse extended interactions.

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

Sponge `.schem` v1-v3 is import-only; biomes are ignored. The other supported
formats are `.schematic`, `.schemplus`, `.litematic` and structure `.nbt`. Modern
block/NBT conversion is partial; inspect import warnings and test converted copies.

Modern vanilla `.litematic` import understands the existing v1-v7 container,
including independent regions, negative sizes and packed block-state arrays.
The [26.3 format writer](https://github.com/sakura-ryoko/litematica/blob/64e74592f631cfa5b084bedb0eda5dac8a3b3cb8/src/main/java/fi/dy/masa/litematica/schematic/LitematicaSchematic.java)
still writes v7/subversion 1. Import uses `MinecraftDataVersion` to distinguish
JSON text from 1.21.5+ NBT text. This work does not add a modern export format.

Representable vanilla content includes flattened block/item variants, quartz
pillar axes, native mushroom faces and all six double-plant species/halves,
note pitch and skull state, sign front text, container names/inventories and
furnace timers. Item conversion supports legacy named enchantments and 1.20.5+
components for damage, names, lore, leather color, repair cost, unbreakable state,
native enchantments, potions and books.
Slabs, anvils and redstone components use item metadata/IDs rather than placed
block metadata/IDs. Modern hand/armor lists and 1.21.5+ equipment compounds retain
the native five equipment slots and drop chances.

This is partial conversion to 1.7.10 content, not a backport of modern gameplay.
New blocks/items/entities, unsupported enchantments/effects/components, offhand
and other new equipment slots cannot be reconstructed universally. Missing items
are removed with a warning; omitted components and sign back text are reported.
Unrepresentable mushroom faces use a warned approximation. Text becomes plain
text; sign lines retain the native 15-character limit. Note instruments remain
dependent on the 1.7.10 supporting block. Rich text, arbitrary riding hierarchies,
modern entity behavior, biomes and scheduled ticks are outside this coverage.
Shared structure/Sponge import converters also receive their file's data version.
See TESTING for constructed-file checks, public upstream samples and the
remaining fresh-export limitations.

- Enable NBT saving and recapture the original build. Old schematics cannot
  recreate client fields that were never saved.
- Singleplayer uses the integrated server world. Remote capture can preserve
  synchronized visual data, but cannot recover inventory/genetics/private machine
  data that the server never sent.
- Client-only creative paste uses server commands and requires `/setblock` permission.
  When saved tile/entity data is present, an explicit blocks-only confirmation
  explains that inventories, machine data, sign text and entities will be omitted.
  The source schematic is preserved. Complete NBT needs singleplayer or Plus on
  the server; command-mode update suppression remains unsupported. The consent
  flow was checked in both native packs; see the dated results in TESTING.md.
- Canonical NBT and visual state are separate. Loading a smaller client update
  must not overwrite unsynchronized inventories when saving again or pasting.
- Binary adapters are version-guarded. Multipart/LogisticsPipes streams also use
  the loaded-mod version fingerprint. New schematic saves also record a block/item/fluid
  registry fingerprint. A changed registry discards tile update packets and binary visual
  adapters with an import warning, keeping canonical NBT and named visual fields.
  Opaque payloads are not rewritten; their exact preview may require recapture in the target pack.
- Cross-pack schematic saves carry item names in all four writable formats, including
  numeric GT covers, BC pipe IDs and Storage Drawers slots inside `.litematic`/structure `.nbt`.
  Vanilla piston/falling-block/Enderman/minecart NBT block references have a separate block map.
  Missing items are omitted with a report and positional equipment slots remain in place.
  StorageDrawers 2.2 `ItemStack` slots are normalized to the layout also understood by
  2.1, preserving item tags, full counts and empty locked prototypes for reverse migration.
  Prefer `.schemplus` and the current Plus version in both packs; save from a world copy,
  transfer the file, inspect the import report and paste into a recipient world copy.
  Arbitrary mod-private material indexes, machine IDs and opaque canonical payloads are
  not universally portable. Registry-name changes require a specific adapter or manual replacement.
- Standard Forge fluid stacks use `FluidName`, so matching registered names retain their
  amount and tags even when numeric fluid IDs differ. Unknown nonempty named fluid stacks
  are reported on import before the container's native reader can discard them. This
  does not invent replacement fluids or translate mod-private numeric fluid encodings.
- Migration resolves the target's Forge registry, so it also applies to individually
  selected mods without the GTNH pack. Missing content follows the existing block
  fallback and item-removal reports; mapped names take precedence over coincidentally
  equal numeric IDs. Unsupported entities and block entities are reported, and NBT is not attached
  to a replacement block that cannot host a tile entity. A missing optional API in one
  imported tile/entity does not abort the other supported content.
  Non-vanilla entity/tile identifiers and private NBT bypass vanilla modern-format
  conversion. This preserves native mod names; it does not translate arbitrary modern
  mod data into an older mod's schema. Keep the original schematic: exporting a
  partially supported import saves the converted contents, including omissions and
  placeholders. Installing the missing mod later requires reloading the original file.
- Captured supported links within the source dimension follow the paste destination's
  coordinates and dimension. Links originally targeting another dimension remain
  unchanged. Mod-dimension numeric IDs are not a portable identity across packs;
  inspect and rebind affected external links manually. Ownership and shared-storage
  frequencies are preserved, not reassigned. EnderStorage and personal Ender Chest
  contents belong to the recipient world/player save and are not cloned by a schematic.
- World time, frame interpolation and selected safe visual updates advance.
  Full machine ticking is not enabled. Private tick counters, transient transport
  particles and entity-targeted effects are not universally reproduced.
- Placed and moved Multipart tiles run native chunk-load initialization after
  installation and before their native description packet. This restores network
  registration for converted AE2 cable hosts. Both native packs passed dedicated
  ME-P2P transfers with a microblock cover, including rotation, mirroring, suppressed
  updates and a whole-network move; other multipart networks need concrete fixtures.
- Native block rotations remain in use, with explicit transforms for supported
  EnderIO/GT sides, GT extended facings, AE2 parts, ArchitectureCraft shapes and
  other named adapters. Unknown directional encodings and absolute links inside
  opaque NBT/streams still need per-mod handling. Supported samples are listed in
  TESTING.md; representable transforms do not cover every machine or shape.
- Neighbors outside the selection, layer-filtered neighbor queries, biome tint,
  original lighting and shader-specific output can still change the appearance.
- During the source investigation, public source URLs for Catwalks-2,
  Galaxy-Space-GTNH, harvestcraft and Automagy-GTNH were unavailable. Binary-only
  or restricted projects are not source-verified implementations. Later format,
  transform and rendering fixtures cover selected examples in these families and
  Thaumcraft, Witchery, Extra Utilities and BiblioCraft; they are not a complete
  tile-render audit. Restricted source is not copied into Plus.

Client-only command editing requires native server command replies. The queue
waits for these replies and stops on permission errors or an unconfirmed result;
a server plugin that suppresses or replaces vanilla replies cannot be assumed to
have completed the edit. Blocks-only consent preserves the loaded source NBT but
does not transfer inventories, sign text or entities to a server without Plus.

## Verification

`./gradlew build` compiles, reobfuscates, runs Checkstyle and executes the unit
tests; also run `LC_ALL=C.UTF-8 ./gradlew test`. Offline mode requires a populated
dependency cache. The tests cover bounded codecs, field restoration,
client/server distinctions, snapshot isolation, stream framing and transformations.
Small test fixtures do not reproduce entire upstream mods and are not packaged.

Selected pack-level results and their limits are recorded in
[HANDOUT.md](../HANDOUT.md) and the [render audit](render-audit.md). The latter
checks the build after the chest depth fix and tracks the additional old-Angelica
render-pass fix for TConstruct container shells and GT industrial glass. It also
records actual Complementary Reimagined shader tests on both native Angelica
versions, including the old loader's r5.9.2 fallback and the limited tested presets.
Separate OptiFine E7/r5.2.2 views passed the sampled 2.8.4 face checks. Native
2.9.0-RC-2 fails before the menu because of a Hodgepodge/OptiFine font mixin
conflict, reproduced with Plus removed; its OptiFine rendering remains untested.
Follow [TESTING.md](../TESTING.md) for repeatable regression scenarios and dated
results. Multiplayer, chunk boundaries, transforms, dimensions and round trips
have representative coverage. Remaining work includes new powered structures on
dedicated servers, 2.8.4 Freecam, longer transition sequences, opaque cross-pack
data and additional shader/driver combinations; see HANDOUT section 11. A passing
fixture does not establish complete compatibility for its entire mod.

## Descriptive schematic metadata

Names, authors, multiline descriptions, creation/modification times and embedded
previews are preserved between `.schematic`, `.schemplus`, `.litematic` and structure
`.nbt`, including source copies, REBUILD saves and current-protocol downloads.
Litematic uses its native Metadata fields. The other formats carry optional data
under `ExtendedMetadata.SchematicaPlus`; `.nbt` also writes native `author`.
This does not modify their block encodings or trigger a format upgrade by itself.
It is not a standard author/thumbnail extension understood by every external editor;
other tools may ignore or discard those fields. Sponge `.schem` stays import-only.

Pure metadata editing retains the original data version, block arrays/palettes and
unknown tags, and refuses files whose NBT cannot be rewritten safely. Conversion
still performs the normal content conversion and is not a lossless rewrite of
unsupported modern/modded content. Preview images are embedded, not linked to the
source PNG. Browser textures accept square images up to 512x512; newly set images
use the Litematica-style centered 120x120 crop. Invalid previews are not rendered.

The browser can switch between an embedded image and a navigable 3D model, expand
the view, pause rotation and reset the camera. Its movement input is scoped to the
focused viewport and never edits a placement or moves the game player. The
lightweight model renderer uses a separate perspective viewport and restores the
GUI's matrices, viewport and clipping. It keeps the existing 4-million-cell/32-MB
limits and excludes blocks that require tile entity renderers; those limits do
not describe the separate world hologram renderer.
