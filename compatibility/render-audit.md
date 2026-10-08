# Render audit after the chest depth fix

Date: 2026-10-08. Baseline: `8b285a9`, with the chest depth fix already applied.
Targets: GTNH 2.8.4 / Angelica 1.0.0-beta66b and GTNH 2.9.0-RC-2 / Angelica 2.2.28.
Actual clients ran under Xvfb and Mesa llvmpipe, using generated test worlds.
No uploaded world or schematic was used.

## Findings

The chest fix covers opaque tile renderers, but it did not fix every source of
missing geometry. Two failures were observed in the current 2.8.4 preview:

- TConstruct seared tanks, windows and glass (all three `LavaTank` metadata
  variants): empty containers disappeared; filled containers showed only fluid.
- GT tinted industrial glass (`gregtech:gt.blocktintedglass`): the preview was
  invisible, although the real source block was visible.

Old Angelica replaces `ForgeHooksClient.getWorldRenderPass()` with a thread-local
value from `ChunkRenderManager`. Updating Forge's field alone left the effective
pass at zero while pass one was being compiled. Both real game probes and isolated
regression tests confirmed this mismatch. TConstruct 1.13.57-GTNH draws the fluid
in pass zero and the container in pass one. The 2.9 samples did not show these
missing-shell failures before the additional fix.

`SchematicRenderPass` now also enters and restores the optional legacy renderer
pass, while preserving Forge's raw field separately from its overridden getter.
The overlay pass does not select a nonexistent third legacy pass. New Angelica
and clients without that legacy API retain the normal Forge path. No optional
renderer dependency or mod implementation is bundled.

## Observed coverage

The initial sweep compared source blocks with front and reverse preview views
for 40 block/state cases in each pack. Additional fixtures checked all six
empty/filled TConstruct tank variants, a bronze GT macerator, unconnected GT fluid
pipes, 18 ArchitectureCraft shapes and two ProjectRed wires sharing one cell.

| Family | Samples | Baseline observation |
| --- | --- | --- |
| Vanilla registry IDs | Beacon, enchanting table, player skull, sign, brewing stand, hopper, filled cauldron, anvil | No chest-like missing faces observed; native pack replacements retained |
| Other chests | Baby Chest, EnderStorage chest, AE2 sky chest, Galacticraft treasure/parachest, Thaumcraft hungry chest, Thaumic Exploration bound chest, Forestry apiarist chest | Complete shells in both packs |
| Transparent contents | Filled BuildCraft, EnderIO and EnderStorage tanks; empty/brain/bound jars; half-filled Botania pool | Container and contents visible in both packs |
| TConstruct containers | Three `LavaTank` variants, empty and half filled | Shells missing in 2.8.4; visible in 2.9 |
| Other machines/models | JABBA barrel, StorageDrawers, AE2 drive/inscriber, BuildCraft/Forestry engines, EnderIO SAG mill, Botania pylon | No new missing-face defect relative to real source fixtures |
| Ordinary geometry | Glass, panes, stained glass, upper slab/stair, fence, iron bars | No new missing-face defect relative to real source fixtures |
| GT industrial glass | Tinted industrial glass | Invisible in 2.8.4; visible in 2.9 |
| Additional geometry | GT macerator/pipes, ArchitectureCraft shapes, ProjectRed multipart wires | No new missing-face defect observed |

## Validation and limits

Build and Checkstyle pass. All 570 tests pass; three added tests cover legacy pass
switching, independent restoration, nested/failed drawing, overlay handling and
absence of the optional API. The two legacy scenarios fail against the preceding
implementation. The test fixtures are not included in the runtime JAR.

Both clients were fully restarted with the rebuilt runtime JAR (SHA-256
`0ad47e7a153ca16b7d8f0e5c86a7d35f984a28b16c59aa26edfa6400fd39ea9c`).
In 2.8.4, the effective world pass now changes to one when requested; all three
empty/filled TConstruct container shells and GT industrial glass are visible.
The same fixtures remain visible in 2.9. Opposite views and alpha/overlay toggles
were checked for the failing models. Selected vanilla, chest, fluid, machine,
GT, ArchitectureCraft and multipart fixtures were checked again after the restart.
No block/tile render failures were logged during these final runs.

These are representative fixtures, not an exhaustive test of every mod block.
Empty/inactive machines do not cover all upgrades, inventories, orientations,
active animations, connected networks or formed multiblocks. The unconnected GT
pipe fixture does not validate every connection shape. Lighting and schematic
alpha can differ from real blocks. Angelica shaders, other graphics drivers,
resource packs and the user's own schematics remain untested here. The intermittent
crystal-chest item visibility was subsequently reproduced in
a cold 2.9 client with Plus removed, on a copied world; the native inventory
still contained all 64 diamonds. No Plus change was made for that observation.

Follow the render checks in [TESTING.md](../TESTING.md). Scripts, native captures,
pass readings, screenshots and logs are outside the repository at
`/workspace/release-issues/render-audit`. Source references used to identify the
pass API and tank behavior were Angelica `7059ee0eecbee9cc66a3b9a301bf42d7242669b3`
and TConstruct `ba672fb6af0f71678a5c7aac2b2c4c05abf1e2de`; no upstream rendering
implementation was copied.

## Expanded states and cameras

A later 2026-10-08 run on the project/material fix added native powered lamps,
burning furnaces, extended pistons, connected clear/coloured panes, bars and
fences, stacked BuildCraft tanks and connected GT pipes in both packs. Source
saved-world states and opposite preview views matched. Fast/fancy graphics with
AO off/on, positive/negative chunk boundaries, actual Nether previews and distinct
wrong-block/wrong-state/extra colours were inspected without another missing-face
defect. A removed Nether placement stayed removed after a 2.8.4 round trip.
Native Freecam 1.0.12 in 2.9 moved independently of the player while the preview
kept its world position. Seven-bundle EnderIO curve/T fixtures with item/power
conduits rendered completely in both packs after checking native source NBT.

These expand the earlier coverage; they do not establish all machine animations,
formed multiblocks, 2.8 Freecam or external shader compatibility. The 2.9 software
renderer showed ground dithering even with schematic rendering disabled; this
was not classified as a newly observed missing-face defect. Evidence is under
`/workspace/release-issues/followup`; see the follow-up steps in TESTING.md.


## Historical fixtures and pasted Multipart tiles

The follow-up also inspected all eight horizontal EnderIO transforms and a
subregion/global composition in both packs. Saved conduit settings, ordinary
block states and filtered item transport matched independent expectations.
Seven IC2 crop-stick states rendered without missing textures and retained their
crop/stage data in immediate post-paste snapshots. A mirrored/rotated, reloaded
2.9 GT frame fixture retained metadata 316 in all twelve frame cells.

ProjectRed full lamps had real registry IDs above 4095 in both packs and kept
their identity/state across pack exchange. Four small-light types exposed a
different problem: previews were complete and server tiles existed after paste,
but clients did not receive Multipart descriptions until a chunk reload.
`ForgeMultipart.sendDescription` now calls the native helper after normal or
silent tile installation, including world moves. Both cold clients passed
ordinary/silent/cross-pack paste and move without reloading chunks. A cold 2.9
dedicated server also passed remote paste and replacing existing parts with new
colours/inversion. Inspected client appearances and saved tile data matched.
Build, Checkstyle and 588 automated tests pass; two added placement/cancellation
regressions fail without the synchronization calls.

These checks use constructed native fixtures. The original GT invalid-texture
and EnderIO report files remain unavailable and are not declared fixed by this
coverage. See HANDOUT section 6 and TESTING for steps and evidence locations.
