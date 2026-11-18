# Active repository compatibility screen

Static triage against Schematica Plus `0002f57` for the [organization inventory](organization-screen.md). Each entry links to the default-branch revision actually inspected. Counts refer to files with at least one matching declaration. Source file names and package declarations are used for class lookup and collision candidates. Shaded sources, inheritance, renamed methods, binary-only implementations and bytecode patches can change the result. A match identifies a path to exercise and does not establish a runtime pass or failure.

The checks follow Plus tile NBT capture, Forge block and tile rendering, material picking, right-click placement, interaction event handlers, and mixins targeting selected world, player-controller and renderer classes. Existing optional integrations are cross-referenced by literal class name. The target pack runtime observations are recorded in [TESTING.md](../TESTING.md).

### Advanced-Botany

Source [79196942e3](https://github.com/GTNewHorizons/Advanced-Botany/tree/79196942e3468c433bc7e6204c1607088807992b) · 160 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileManaCharger.readFromNBT](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/tile/TileManaCharger.java#L236).
- Check preview rendering against `RendererSchematicChunk`. 15 source file(s) contain matching declarations, including [RenderBlockABSpreader.renderWorldBlock](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/client/render/block/RenderBlockABSpreader.java#L26).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockLebethronWood.getPickBlock](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/BlockLebethronWood.java#L121).
- Check printer placement against `SchematicPrinter`. 6 source file(s) contain matching declarations, including [BlockABSpreader.onBlockPlacedBy](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/BlockABSpreader.java#L45).
- [BlockABSpreader.onBlockPlacedBy](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/BlockABSpreader.java#L45) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BlockLebethronWood.getPickBlock](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/BlockLebethronWood.java#L121) queries world state when selecting an item. Compare the materials list against the placed variant.
- [TileManaCharger.readFromNBT](https://github.com/GTNewHorizons/Advanced-Botany/blob/79196942e3468c433bc7e6204c1607088807992b/src/main/java/ab/common/block/tile/TileManaCharger.java#L236) accesses `requestUpdate`. Compare those fields after capture, transform and restore.

### AdventureBackpack2

Source [93791644f1](https://github.com/GTNewHorizons/AdventureBackpack2/tree/93791644f15006ec0afa3a06833c2feee2aba5ec) · 173 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [com.darkona.adventurebackpack.block.BlockAdventureBackpack](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/BlockAdventureBackpack.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileAdventureBackpack.readFromNBT](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/TileAdventureBackpack.java#L113).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [RendererAdventureBackpackBlock.renderTileEntityAt](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/client/render/RendererAdventureBackpackBlock.java#L23).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockAdventureBackpack.getPickBlock](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/BlockAdventureBackpack.java#L214).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [BlockAdventureBackpack.onBlockPlacedBy](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/BlockAdventureBackpack.java#L256).
- [BlockAdventureBackpack.onBlockPlacedBy](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/BlockAdventureBackpack.java#L256) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileAdventureBackpack.readFromNBT](https://github.com/GTNewHorizons/AdventureBackpack2/blob/93791644f15006ec0afa3a06833c2feee2aba5ec/src/main/java/com/darkona/adventurebackpack/block/TileAdventureBackpack.java#L113) accesses `sleepingbag`, `sbx`, `sby`, `sbz`. Compare those fields after capture, transform and restore.

### AE2FluidCraft-Rework

Source [a950c8f23b](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/tree/a950c8f23b0f10151744cb582432cfc80a6ceabd) · 230 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [com.glodblock.github.common.block.BlockWalrus](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/block/BlockWalrus.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 19 source file(s) contain matching declarations, including [PartFluidConversionMonitor.readFromNBT](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/parts/PartFluidConversionMonitor.java#L17).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [RenderBlockCertusQuartzTank.renderTileEntityAt](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/client/render/RenderBlockCertusQuartzTank.java#L18).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockCertusQuartzTank.getPickBlock](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/block/BlockCertusQuartzTank.java#L91).
- Check printer placement against `SchematicPrinter`. 10 source file(s) contain matching declarations, including [BlockWalrus.onBlockPlacedBy](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/block/BlockWalrus.java#L54).
- [BlockWalrus.onBlockPlacedBy](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/block/BlockWalrus.java#L54) depends on placement orientation. Exercise the printer with a rotated schematic.
- [PartFluidConversionMonitor.readFromNBT](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/a950c8f23b0f10151744cb582432cfc80a6ceabd/src/main/java/com/glodblock/github/common/parts/PartFluidConversionMonitor.java#L17) accesses `configuredItem`, `StackType`. Compare those fields after capture, transform and restore.

### AFSU

Source [4e734bb6ab](https://github.com/GTNewHorizons/AFSU/tree/4e734bb6ab4cd3dc12bf86806f10513abd35f5ff) · 9 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [AFSUBlock.onBlockPlacedBy](https://github.com/GTNewHorizons/AFSU/blob/4e734bb6ab4cd3dc12bf86806f10513abd35f5ff/src/main/java/xbony2/afsu/blocks/AFSUBlock.java#L184).
- [AFSUBlock.onBlockPlacedBy](https://github.com/GTNewHorizons/AFSU/blob/4e734bb6ab4cd3dc12bf86806f10513abd35f5ff/src/main/java/xbony2/afsu/blocks/AFSUBlock.java#L184) depends on placement orientation. Exercise the printer with a rotated schematic.
- AFSUBlock.onBlockPlacedBy restores energy from the held stack and chooses facing from the placing entity. A matching item alone cannot establish that the printed facing matches the schematic.

### AkashicTome

Source [da141abd9a](https://github.com/GTNewHorizons/AkashicTome/tree/da141abd9a385dad3a8608b15cd020c2685b76cd) · 20 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemTome.onItemUse](https://github.com/GTNewHorizons/AkashicTome/blob/da141abd9a385dad3a8608b15cd020c2685b76cd/src/main/java/vazkii/akashictome/ItemTome.java#L31).

### AlchemyGrate

Source [221d8f97fb](https://github.com/GTNewHorizons/AlchemyGrate/tree/221d8f97fbc8a0b5a77148b6bea6634c0a5deccb) · 6 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileChestGrate.writeToNBT](https://github.com/GTNewHorizons/AlchemyGrate/blob/221d8f97fbc8a0b5a77148b6bea6634c0a5deccb/src/main/java/com/technicianlp/chestgrate/TileChestGrate.java#L130).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockChestGrate.onBlockPlacedBy](https://github.com/GTNewHorizons/AlchemyGrate/blob/221d8f97fbc8a0b5a77148b6bea6634c0a5deccb/src/main/java/com/technicianlp/chestgrate/BlockChestGrate.java#L133).
- [TileChestGrate.writeToNBT](https://github.com/GTNewHorizons/AlchemyGrate/blob/221d8f97fbc8a0b5a77148b6bea6634c0a5deccb/src/main/java/com/technicianlp/chestgrate/TileChestGrate.java#L130) accesses `Slot`, `Items`, `CustomName`. Compare those fields after capture, transform and restore.

### Amazing-Trophies

Source [5c1a32cce3](https://github.com/GTNewHorizons/Amazing-Trophies/tree/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f) · 48 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileEntityTrophy.readFromNBT](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/TileEntityTrophy.java#L140).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RendererTrophy.renderTileEntityAt](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/RendererTrophy.java#L20).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockTrophy.getPickBlock](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/BlockTrophy.java#L56).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemBlockTrophy.placeBlockAt](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/ItemBlockTrophy.java#L85).
- [BlockTrophy.getPickBlock](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/BlockTrophy.java#L56) queries world state when selecting an item. Compare the materials list against the placed variant.
- [ItemBlockTrophy.placeBlockAt](https://github.com/GTNewHorizons/Amazing-Trophies/blob/5c1a32cce3c6e7bea27fa8224c418b7c4bc4462f/src/main/java/glowredman/amazingtrophies/trophy/ItemBlockTrophy.java#L85) depends on placement orientation. Exercise the printer with a rotated schematic.

### AMDIForge

Source [2ecec40599](https://github.com/GTNewHorizons/AMDIForge/tree/2ecec405991ed72a6676e35b1c9fdbf67151d00a) · 22 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [GraveNBT.readFromNBT](https://github.com/GTNewHorizons/AMDIForge/blob/2ecec405991ed72a6676e35b1c9fdbf67151d00a/src/main/java/eu/usrv/amdiforge/core/graveIO/GraveNBT.java#L108).
- [GraveNBT.readFromNBT](https://github.com/GTNewHorizons/AMDIForge/blob/2ecec405991ed72a6676e35b1c9fdbf67151d00a/src/main/java/eu/usrv/amdiforge/core/graveIO/GraveNBT.java#L108) accesses `size`, `Items`, `id`, `Count`. Compare those fields after capture, transform and restore.

### AmidstGTNH

Source [94f014b667](https://github.com/GTNewHorizons/AmidstGTNH/tree/94f014b66777fb228c8eef329759f35495da9760) · 89 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ForgeAmidst](https://github.com/GTNewHorizons/AmidstGTNH/blob/94f014b66777fb228c8eef329759f35495da9760/src/main/java/sedridor/forgeamidst/ForgeAmidst.java#L44). Indirect effects from dependencies, injection, configuration and server rules remain open.

### amunra

Source [4dcf15f568](https://github.com/GTNewHorizons/amunra/tree/4dcf15f568a84473f987657b2285d63a0e4cbe32) · 302 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [de.katzenpapst.amunra.block.BlockARChest](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/block/BlockARChest.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 17 source file(s) contain matching declarations, including [NbtHelper.writeToNBT](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/helper/NbtHelper.java#L16).
- Check preview rendering against `RendererSchematicChunk`. 11 source file(s) contain matching declarations, including [BlockRendererARChest.renderWorldBlock](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/client/renderer/BlockRendererARChest.java#L28).
- Check material picking against `BlockToItemStack`. 10 source file(s) contain matching declarations, including [BlockBasicMeta.getPickBlock](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/block/BlockBasicMeta.java#L245).
- Check printer placement against `SchematicPrinter`. 12 source file(s) contain matching declarations, including [BlockARChest.onBlockPlacedBy](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/block/BlockARChest.java#L172).
- [BlockARChest.onBlockPlacedBy](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/block/BlockARChest.java#L172) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BlockBasicMeta.getPickBlock](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/block/BlockBasicMeta.java#L245) queries world state when selecting an item. Compare the materials list against the placed variant.
- [NbtHelper.writeToNBT](https://github.com/GTNewHorizons/amunra/blob/4dcf15f568a84473f987657b2285d63a0e4cbe32/src/main/java/de/katzenpapst/amunra/helper/NbtHelper.java#L16) accesses `minX`, `minY`, `minZ`, `maxX`. Compare those fields after capture, transform and restore.

### Angelica

Source [996fb54cb0](https://github.com/GTNewHorizons/Angelica/tree/996fb54cb05e256ea6039f4e2c8457a221a179b7) · 2005 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `GeometrySupport` names [com.gtnewhorizons.angelica.rendering.StateAwareTessellator](https://github.com/GTNewHorizons/Angelica/blob/996fb54cb05e256ea6039f4e2c8457a221a179b7/src/main/java/com/gtnewhorizons/angelica/rendering/StateAwareTessellator.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [MobRandomizer.readFromNBT](https://github.com/GTNewHorizons/Angelica/blob/996fb54cb05e256ea6039f4e2c8457a221a179b7/src/main/java/com/prupe/mcpatcher/mob/MobRandomizer.java#L202).
- Check preview rendering against `RendererSchematicChunk`. 5 source file(s) contain matching declarations, including [ClipPlaneTestRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/Angelica/blob/996fb54cb05e256ea6039f4e2c8457a221a179b7/src/main/java/com/gtnewhorizons/angelica/rendering/ClipPlaneTestRenderer.java#L28).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockTest.onBlockPlaced](https://github.com/GTNewHorizons/Angelica/blob/996fb54cb05e256ea6039f4e2c8457a221a179b7/src/main/java/com/gtnewhorizons/angelica/common/BlockTest.java#L29).
- Check world or renderer transformation alongside the schematic world and render pipeline. 59 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Angelica/blob/996fb54cb05e256ea6039f4e2c8457a221a179b7/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/MixinEntityRenderer.java#L25).

### AngerMod

Source [c43058ffce](https://github.com/GTNewHorizons/AngerMod/tree/c43058ffce4ddc54d1139f8e88e83fba376ccfcb) · 13 tracked Java, Scala or Kotlin files outside test/example directories.

- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [PlayerSpawnProtection.onInteractEvent](https://github.com/GTNewHorizons/AngerMod/blob/c43058ffce4ddc54d1139f8e88e83fba376ccfcb/src/main/java/com/github/namikon/angermod/events/PlayerSpawnProtection.java#L152).

### AntiqueAtlas

Source [2e3cb5417e](https://github.com/GTNewHorizons/AntiqueAtlas/tree/2e3cb5417e15d41bf55d28a1201cb3c3035c95e4) · 104 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 5 source file(s) contain matching declarations, including [AtlasData.readFromNBT](https://github.com/GTNewHorizons/AntiqueAtlas/blob/2e3cb5417e15d41bf55d28a1201cb3c3035c95e4/src/main/java/hunternif/mc/atlas/core/AtlasData.java#L52).

### AppleCore

Source [238a465948](https://github.com/GTNewHorizons/AppleCore/tree/238a4659486471b05a1179435cfeee9b7fc14c28) · 75 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [EatingSpeedModifier.onItemUse](https://github.com/GTNewHorizons/AppleCore/blob/238a4659486471b05a1179435cfeee9b7fc14c28/AppleCoreExample/EatingSpeedModifier.java#L13).

### Applied-Energistics-2-NH

Source [3c90aa494b](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/tree/3c90aa494b927da0555e7a3ad6907f870fb89cb0) · 2205 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `AE2Items` names [appeng.api.parts.IPartHost](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/api/parts/IPartHost.java#L1) (10 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 100 source file(s) contain matching declarations, including [ICraftingLink.writeToNBT](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/api/networking/crafting/ICraftingLink.java#L45).
- Check preview rendering against `RendererSchematicChunk`. 7 source file(s) contain matching declarations, including [TESRWrapper.renderTileEntityAt](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/client/render/TESRWrapper.java#L41).
- Check material picking against `BlockToItemStack`. 6 source file(s) contain matching declarations, including [AEBaseSlabBlock.getPickBlock](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/block/AEBaseSlabBlock.java#L97).
- Check printer placement against `SchematicPrinter`. 30 source file(s) contain matching declarations, including [IPartItem.onItemUse](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/api/parts/IPartItem.java#L39).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [FMPEvent.playerInteract](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/fmp/FMPEvent.java#L139).
- [DimensionalCoord.writeToNBT](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/api/util/DimensionalCoord.java#L79) accesses `dim`, `x`, `y`, `z`. Compare those fields after capture, transform and restore.
- [AEBaseItemBlock.placeBlockAt](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/block/AEBaseItemBlock.java#L82) depends on placement orientation. Exercise the printer with a rotated schematic.
- [AEBaseSlabBlock.getPickBlock](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/block/AEBaseSlabBlock.java#L97) queries world state when selecting an item. Compare the materials list against the placed variant.
- [FMPEvent.playerInteract](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/appeng/fmp/FMPEvent.java#L139) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [RenderBlockProviderBase.renderWorldBlock](https://github.com/GTNewHorizons/Applied-Energistics-2-NH/blob/3c90aa494b927da0555e7a3ad6907f870fb89cb0/src/main/java/thaumicenergistics/client/render/RenderBlockProviderBase.java#L177) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### Applied-Energistics-2-Unofficial

Source [7043fe2e78](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/tree/7043fe2e783d3d9119646c046124d0b8d79be151) · 1615 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `AE2Items` names [appeng.api.parts.IPartHost](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/api/parts/IPartHost.java#L1) (7 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 64 source file(s) contain matching declarations, including [ICraftingLink.writeToNBT](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/api/networking/crafting/ICraftingLink.java#L45).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [TESRWrapper.renderTileEntityAt](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/client/render/TESRWrapper.java#L41).
- Check material picking against `BlockToItemStack`. 5 source file(s) contain matching declarations, including [AEBaseSlabBlock.getPickBlock](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/block/AEBaseSlabBlock.java#L97).
- Check printer placement against `SchematicPrinter`. 14 source file(s) contain matching declarations, including [IPartItem.onItemUse](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/api/parts/IPartItem.java#L39).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [FMPEvent.playerInteract](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/fmp/FMPEvent.java#L139).
- [DimensionalCoord.writeToNBT](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/api/util/DimensionalCoord.java#L79) accesses `dim`, `x`, `y`, `z`. Compare those fields after capture, transform and restore.
- [AEBaseItemBlock.placeBlockAt](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/block/AEBaseItemBlock.java#L82) depends on placement orientation. Exercise the printer with a rotated schematic.
- [AEBaseSlabBlock.getPickBlock](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/block/AEBaseSlabBlock.java#L97) queries world state when selecting an item. Compare the materials list against the placed variant.
- [FMPEvent.playerInteract](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/7043fe2e783d3d9119646c046124d0b8d79be151/src/main/java/appeng/fmp/FMPEvent.java#L139) can cancel the interaction. Verify the printer on a server with the handler enabled.

### ArchitectureCraft

Source [220d7abd18](https://github.com/GTNewHorizons/ArchitectureCraft/tree/220d7abd18f84c55cdca836c814891289be36751) · 132 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [gcewing.architecture.common.block.BlockSawbench](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/common/block/BlockSawbench.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [TileArchitecture.readFromNBT](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/common/tile/TileArchitecture.java#L124).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [BlockRenderDispatcher.renderWorldBlock](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/client/render/BlockRenderDispatcher.java#L33).
- Check material picking against `BlockToItemStack`. 2 source file(s) contain matching declarations, including [BlockArchitecture.getPickBlock](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/common/block/BlockArchitecture.java#L635).
- Check printer placement against `SchematicPrinter`. 7 source file(s) contain matching declarations, including [BlockArchitecture.onBlockPlacedBy](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/common/block/BlockArchitecture.java#L386).
- [TileArchitecture.readFromNBT](https://github.com/GTNewHorizons/ArchitectureCraft/blob/220d7abd18f84c55cdca836c814891289be36751/src/main/java/gcewing/architecture/common/tile/TileArchitecture.java#L124) accesses `side`, `turn`. Compare those fields after capture, transform and restore.

### AsieLib

Source [ff2a24d96f](https://github.com/GTNewHorizons/AsieLib/tree/ff2a24d96f3ef4d9fe85f3da2939796e081222e7) · 91 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 5 source file(s) contain matching declarations, including [IBattery.readFromNBT](https://github.com/GTNewHorizons/AsieLib/blob/ff2a24d96f3ef4d9fe85f3da2939796e081222e7/src/main/java/pl/asie/lib/api/tile/IBattery.java#L26).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [BlockBaseRender.renderWorldBlock](https://github.com/GTNewHorizons/AsieLib/blob/ff2a24d96f3ef4d9fe85f3da2939796e081222e7/src/main/java/pl/asie/lib/client/BlockBaseRender.java#L84).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockBase.onBlockPlacedBy](https://github.com/GTNewHorizons/AsieLib/blob/ff2a24d96f3ef4d9fe85f3da2939796e081222e7/src/main/java/pl/asie/lib/block/BlockBase.java#L241).
- [TileEntityBase.readFromNBT](https://github.com/GTNewHorizons/AsieLib/blob/ff2a24d96f3ef4d9fe85f3da2939796e081222e7/src/main/java/pl/asie/lib/block/TileEntityBase.java#L58) accesses `old_redstone`. Compare those fields after capture, transform and restore.
