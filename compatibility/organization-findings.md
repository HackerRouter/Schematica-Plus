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

### AspectRecipeIndex

Source [315f8c7e74](https://github.com/GTNewHorizons/AspectRecipeIndex/tree/315f8c7e746d209655f1e6a2f0a97790d424dfea) · 29 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [AspectRecipeIndex](https://github.com/GTNewHorizons/AspectRecipeIndex/blob/315f8c7e746d209655f1e6a2f0a97790d424dfea/src/main/java/com/gtnewhorizons/aspectrecipeindex/AspectRecipeIndex.java#L17). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Avaritia

Source [9412321283](https://github.com/GTNewHorizons/Avaritia/tree/9412321283e471f4cd399ced8cba8e8438b80583) · 146 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [fox.spiteful.avaritia.blocks.BlockMatterClusterOpener](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/blocks/BlockMatterClusterOpener.java#L1) (4 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [InfiniteFoxes.writeToNBT](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/compat/tails/InfiniteFoxes.java#L123).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [RenderInfinitato.renderWorldBlock](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/compat/botania/RenderInfinitato.java#L32).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [BlockCompressor.onBlockPlacedBy](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/blocks/BlockCompressor.java#L78).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [LudicrousEvents.onPlayerMine](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/LudicrousEvents.java#L44).
- [BlockCompressor.onBlockPlacedBy](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/blocks/BlockCompressor.java#L78) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileMatterClusterOpener.writeToNBT](https://github.com/GTNewHorizons/Avaritia/blob/9412321283e471f4cd399ced8cba8e8438b80583/src/main/java/fox/spiteful/avaritia/tile/TileMatterClusterOpener.java#L118) accesses `clusterInput`, `itemOutput`, `fluidOutput`. Compare those fields after capture, transform and restore.

### Avaritiaddons

Source [9cda711b4d](https://github.com/GTNewHorizons/Avaritiaddons/tree/9cda711b4db004cd84d3c0406b6e7d30f50eb161) · 37 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileEntityAvaritiaddonsChest.readFromNBT](https://github.com/GTNewHorizons/Avaritiaddons/blob/9cda711b4db004cd84d3c0406b6e7d30f50eb161/src/main/java/wanion/avaritiaddons/block/chest/TileEntityAvaritiaddonsChest.java#L85).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RendererAvaritiaddonsChest.renderTileEntityAt](https://github.com/GTNewHorizons/Avaritiaddons/blob/9cda711b4db004cd84d3c0406b6e7d30f50eb161/src/main/java/wanion/avaritiaddons/block/chest/RendererAvaritiaddonsChest.java#L28).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [BlockAvaritiaddonsChest.onBlockPlacedBy](https://github.com/GTNewHorizons/Avaritiaddons/blob/9cda711b4db004cd84d3c0406b6e7d30f50eb161/src/main/java/wanion/avaritiaddons/block/chest/BlockAvaritiaddonsChest.java#L57).
- [BlockAvaritiaddonsChest.onBlockPlacedBy](https://github.com/GTNewHorizons/Avaritiaddons/blob/9cda711b4db004cd84d3c0406b6e7d30f50eb161/src/main/java/wanion/avaritiaddons/block/chest/BlockAvaritiaddonsChest.java#L57) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityAvaritiaddonsChest.readFromNBT](https://github.com/GTNewHorizons/Avaritiaddons/blob/9cda711b4db004cd84d3c0406b6e7d30f50eb161/src/main/java/wanion/avaritiaddons/block/chest/TileEntityAvaritiaddonsChest.java#L85) accesses `FacingSide`. Compare those fields after capture, transform and restore.

### Backhand

Source [c332a5053b](https://github.com/GTNewHorizons/Backhand/tree/c332a5053b655975f803e53928de26f5b613b4a0) · 72 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 3 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Backhand/blob/c332a5053b655975f803e53928de26f5b613b4a0/src/main/java/xonin/backhand/mixins/early/minecraft/MixinEntityRenderer.java#L14).

### Battlegear2-for-Backhand

Source [d5bab15bfa](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/tree/d5bab15bfa8a467afb751cdfe2e0d2ada119429a) · 176 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileEntityFlagPole.readFromNBT](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/heraldry/TileEntityFlagPole.java#L67).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [FlagPoleTileRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/client/renderer/FlagPoleTileRenderer.java#L35).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemBlockFlagPole.placeBlockAt](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/heraldry/ItemBlockFlagPole.java#L20).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BattlemodeHookContainerClass.playerInteract](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/BattlemodeHookContainerClass.java#L80).
- [BattlemodeHookContainerClass.playerInteract](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/BattlemodeHookContainerClass.java#L80) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [TileEntityFlagPole.readFromNBT](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/d5bab15bfa8a467afb751cdfe2e0d2ada119429a/src/main/java/mods/battlegear2/heraldry/TileEntityFlagPole.java#L67) accesses `orientation`, `flag`, `hasUpdate`. Compare those fields after capture, transform and restore.

### Baubles-Expanded

Source [2a49984830](https://github.com/GTNewHorizons/Baubles-Expanded/tree/2a499848304d6df9222c8b642d38ecd2a2affaae) · 33 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Baubles](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2a499848304d6df9222c8b642d38ecd2a2affaae/src/main/java/baubles/common/Baubles.java#L29). Indirect effects from dependencies, injection, configuration and server rules remain open.

### bdlib

Source [d402315bdb](https://github.com/GTNewHorizons/bdlib/tree/d402315bdb52a70af1b343c2b192cb8c1b420bbe) · 209 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [BlockRef.writeToNBT](https://github.com/GTNewHorizons/bdlib/blob/d402315bdb52a70af1b343c2b192cb8c1b420bbe/src/main/scala/net/bdew/lib/block/BlockRef.scala#L55).
- Check preview rendering against `RendererSchematicChunk`. 3 source file(s) contain matching declarations, including [CoverRenderer.renderWorldBlock](https://github.com/GTNewHorizons/bdlib/blob/d402315bdb52a70af1b343c2b192cb8c1b420bbe/src/main/scala/net/bdew/lib/covers/CoverRenderer.scala#L29).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [KeepData.getPickBlock](https://github.com/GTNewHorizons/bdlib/blob/d402315bdb52a70af1b343c2b192cb8c1b420bbe/src/main/scala/net/bdew/lib/block/KeepData.scala#L80).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [KeepData.placeBlockAt](https://github.com/GTNewHorizons/bdlib/blob/d402315bdb52a70af1b343c2b192cb8c1b420bbe/src/main/scala/net/bdew/lib/block/KeepData.scala#L147).

### BeeBetterAtBees-GTNH

Source [8f9f5b8686](https://github.com/GTNewHorizons/BeeBetterAtBees-GTNH/tree/8f9f5b868698c4cb51d0e2a731084b8887da1ad7) · 18 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BeeBetterAtBees](https://github.com/GTNewHorizons/BeeBetterAtBees-GTNH/blob/8f9f5b868698c4cb51d0e2a731084b8887da1ad7/src/main/java/hellfirepvp/beebetteratbees/common/BeeBetterAtBees.java#L17). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BetterAchievements

Source [aee6d9850a](https://github.com/GTNewHorizons/BetterAchievements/tree/aee6d9850a016ae8612c682400bbd285de3d6335) · 41 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BetterAchievementsDemo](https://github.com/GTNewHorizons/BetterAchievements/blob/aee6d9850a016ae8612c682400bbd285de3d6335/src/demo/java/betterachievementsdemo/BetterAchievementsDemo.java#L16). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BetterBuildersWands

Source [975080753b](https://github.com/GTNewHorizons/BetterBuildersWands/tree/975080753b4dfd90738be0cd642629d329737f86) · 38 tracked Java, Scala or Kotlin files outside test/example directories.

- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockShim.getPickBlock](https://github.com/GTNewHorizons/BetterBuildersWands/blob/975080753b4dfd90738be0cd642629d329737f86/src/main/java/portablejim/bbw/shims/BlockShim.java#L13).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemBasicWand.onItemUse](https://github.com/GTNewHorizons/BetterBuildersWands/blob/975080753b4dfd90738be0cd642629d329737f86/src/main/java/portablejim/bbw/core/items/ItemBasicWand.java#L49).

### BetterCrashes

Source [fb6f8d5734](https://github.com/GTNewHorizons/BetterCrashes/tree/fb6f8d5734f45d4ce338948c23f49fe38b66d80b) · 18 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BetterCrashes](https://github.com/GTNewHorizons/BetterCrashes/blob/fb6f8d5734f45d4ce338948c23f49fe38b66d80b/src/main/java/vfyjxf/bettercrashes/BetterCrashes.java#L11). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BetterLoadingScreen

Source [1d8747fed6](https://github.com/GTNewHorizons/BetterLoadingScreen/tree/1d8747fed604724f968796bc4d645a3bf9e5e28b) · 35 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BetterLoadingScreen](https://github.com/GTNewHorizons/BetterLoadingScreen/blob/1d8747fed604724f968796bc4d645a3bf9e5e28b/src/main/java/alexiil/mods/load/BetterLoadingScreen.java#L28). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BetterP2P

Source [4950a4700a](https://github.com/GTNewHorizons/BetterP2P/tree/4950a4700aa1d9f804f34d6daf584219e7656778) · 45 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemAdvancedMemoryCard.onItemUse](https://github.com/GTNewHorizons/BetterP2P/blob/4950a4700aa1d9f804f34d6daf584219e7656778/src/main/java/com/projecturanus/betterp2p/item/ItemAdvancedMemoryCard.kt#L58).

### BetterQuesting

Source [23507243db](https://github.com/GTNewHorizons/BetterQuesting/tree/23507243db7a58a71b1651a4d63442b8ea3f583b) · 523 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 45 source file(s) contain matching declarations, including [RewardPlaceholder.readFromNBT](https://github.com/GTNewHorizons/BetterQuesting/blob/23507243db7a58a71b1651a4d63442b8ea3f583b/src/main/java/betterquesting/api/placeholders/rewards/RewardPlaceholder.java#L36).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [BlockObservationStation.onBlockPlacedBy](https://github.com/GTNewHorizons/BetterQuesting/blob/23507243db7a58a71b1651a4d63442b8ea3f583b/src/main/java/betterquesting/blocks/BlockObservationStation.java#L36).
- [RewardPlaceholder.readFromNBT](https://github.com/GTNewHorizons/BetterQuesting/blob/23507243db7a58a71b1651a4d63442b8ea3f583b/src/main/java/betterquesting/api/placeholders/rewards/RewardPlaceholder.java#L36) accesses `orig_data`. Compare those fields after capture, transform and restore.

### BetterStorage

Source [928aa35a75](https://github.com/GTNewHorizons/BetterStorage/tree/928aa35a75ac146611a15770a9bc0d8571e9defa) · 241 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 11 source file(s) contain matching declarations, including [TileEntityCrate.readFromNBT](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/tile/crate/TileEntityCrate.java#L447).
- Check preview rendering against `RendererSchematicChunk`. 8 source file(s) contain matching declarations, including [BetterStorageRenderingHandler.renderWorldBlock](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/client/renderer/BetterStorageRenderingHandler.java#L67).
- Check material picking against `BlockToItemStack`. 3 source file(s) contain matching declarations, including [TileContainerBetterStorage.getPickBlock](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/tile/TileContainerBetterStorage.java#L59).
- Check printer placement against `SchematicPrinter`. 11 source file(s) contain matching declarations, including [ItemBackpack.onItemUse](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/item/ItemBackpack.java#L273).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [BackpackHandler.onPlayerInteract](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/misc/handlers/BackpackHandler.java#L140).
- [CommonProxy.onPlayerInteract](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/proxy/CommonProxy.java#L93) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [TileEntityCrate.readFromNBT](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/tile/crate/TileEntityCrate.java#L447) accesses `crateId`. Compare those fields after capture, transform and restore.
- [TileArmorStand.getPickBlock](https://github.com/GTNewHorizons/BetterStorage/blob/928aa35a75ac146611a15770a9bc0d8571e9defa/src/main/java/net/mcft/betterstorage/tile/stand/TileArmorStand.java#L99) queries world state when selecting an item. Compare the materials list against the placed variant.

### Binnie

Source [29d8fed2a3](https://github.com/GTNewHorizons/Binnie/tree/29d8fed2a3cb9358a6bab09c60afda3c98ff4936) · 803 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BinnieVisualAdapter` names [binnie.botany.api.IFlower](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/api/IFlower.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 30 source file(s) contain matching declarations, including [TileEntityFlower.readFromNBT](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/flower/TileEntityFlower.java#L64).
- Check preview rendering against `RendererSchematicChunk`. 8 source file(s) contain matching declarations, including [RendererBotany.renderWorldBlock](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/flower/RendererBotany.java#L23).
- Check material picking against `BlockToItemStack`. 10 source file(s) contain matching declarations, including [BlockCeramic.getPickBlock](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/ceramic/BlockCeramic.java#L139).
- Check printer placement against `SchematicPrinter`. 10 source file(s) contain matching declarations, including [BlockFlower.onBlockPlacedBy](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/flower/BlockFlower.java#L84).
- Check interaction events against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [Botany.onShearFlower](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/Botany.java#L218).
- [TileEntityFlower.readFromNBT](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/botany/flower/TileEntityFlower.java#L64) accesses `section`, `owner`, `caterTime`, `cater`. Compare those fields after capture, transform and restore.
- [BlockMetadata.getPickBlock](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/core/block/BlockMetadata.java#L121) queries world state when selecting an item. Compare the materials list against the placed variant.
- [ItemETDoor.onItemUse](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/extratrees/block/ItemETDoor.java#L75) depends on placement orientation. Exercise the printer with a rotated schematic.
- [FenceRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Binnie/blob/29d8fed2a3cb9358a6bab09c60afda3c98ff4936/src/main/java/binnie/extratrees/block/decor/FenceRenderer.java#L141) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### BiomeBridge

Source [83a52ccb9c](https://github.com/GTNewHorizons/BiomeBridge/tree/83a52ccb9c15f1e9a23161bbab585d9e56742659) · 2 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BiomeBridge](https://github.com/GTNewHorizons/BiomeBridge/blob/83a52ccb9c15f1e9a23161bbab585d9e56742659/src/main/java/glowredman/biomebridge/BiomeBridge.java#L11). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BlockLimiter

Source [9af0e3a4d5](https://github.com/GTNewHorizons/BlockLimiter/tree/9af0e3a4d51b3c1f4b61945da13203541bc17178) · 8 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BlockLimiter](https://github.com/GTNewHorizons/BlockLimiter/blob/9af0e3a4d51b3c1f4b61945da13203541bc17178/src/main/java/com/github/namikon/blocklimiter/BlockLimiter.java#L19). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BlockRenderer6343

Source [a0851c029e](https://github.com/GTNewHorizons/BlockRenderer6343/tree/a0851c029e2e5a6db89eeab0ac8c38f3f7fc7767) · 33 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BlockRenderer6343](https://github.com/GTNewHorizons/BlockRenderer6343/blob/a0851c029e2e5a6db89eeab0ac8c38f3f7fc7767/src/main/java/blockrenderer6343/BlockRenderer6343.java#L17). Indirect effects from dependencies, injection, configuration and server rules remain open.

### BloodArsenal

Source [d8a4a11bcb](https://github.com/GTNewHorizons/BloodArsenal/tree/d8a4a11bcb3383b92ed6d918b204f97b00f46993) · 136 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 5 source file(s) contain matching declarations, including [InventoryAugmentedHolding.readFromNBT](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/common/items/sigil/holding/InventoryAugmentedHolding.java#L159).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [TileLifeInfuserRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/client/renderer/block/TileLifeInfuserRenderer.java#L38).
- Check material picking against `BlockToItemStack`. 3 source file(s) contain matching declarations, including [BlockBloodCake.getPickBlock](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/common/block/BlockBloodCake.java#L181).
- Check printer placement against `SchematicPrinter`. 5 source file(s) contain matching declarations, including [BlockCompactedMRS.onBlockPlacedBy](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/common/block/BlockCompactedMRS.java#L75).
- [BlockCompactedMRS.getPickBlock](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/common/block/BlockCompactedMRS.java#L163) queries world state when selecting an item. Compare the materials list against the placed variant.
- [InventoryAugmentedHolding.readFromNBT](https://github.com/GTNewHorizons/BloodArsenal/blob/d8a4a11bcb3383b92ed6d918b204f97b00f46993/src/main/java/com/arc/bloodarsenal/common/items/sigil/holding/InventoryAugmentedHolding.java#L159) accesses `Slot`. Compare those fields after capture, transform and restore.

### BloodMagic

Source [0c361f2482](https://github.com/GTNewHorizons/BloodMagic/tree/0c361f24820d635f46f04059b01e20d036137896) · 756 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone](https://github.com/GTNewHorizons/BloodMagic/blob/0c361f24820d635f46f04059b01e20d036137896/src/main/java/WayofTime/alchemicalWizardry/common/tileEntity/TEMasterStone.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 24 source file(s) contain matching declarations, including [TileReagentHandler.readFromNBT](https://github.com/GTNewHorizons/BloodMagic/blob/0c361f24820d635f46f04059b01e20d036137896/src/main/java/WayofTime/alchemicalWizardry/api/alchemy/energy/TileReagentHandler.java#L12).
- Check preview rendering against `RendererSchematicChunk`. 13 source file(s) contain matching declarations, including [RenderAlchemicCalcinator.renderTileEntityAt](https://github.com/GTNewHorizons/BloodMagic/blob/0c361f24820d635f46f04059b01e20d036137896/src/main/java/WayofTime/alchemicalWizardry/common/renderer/block/RenderAlchemicCalcinator.java#L33).
- Check printer placement against `SchematicPrinter`. 7 source file(s) contain matching declarations, including [BlockBelljar.onBlockPlacedBy](https://github.com/GTNewHorizons/BloodMagic/blob/0c361f24820d635f46f04059b01e20d036137896/src/main/java/WayofTime/alchemicalWizardry/common/block/BlockBelljar.java#L95).
- [TileSegmentedReagentHandler.readFromNBT](https://github.com/GTNewHorizons/BloodMagic/blob/0c361f24820d635f46f04059b01e20d036137896/src/main/java/WayofTime/alchemicalWizardry/api/alchemy/energy/TileSegmentedReagentHandler.java#L37) accesses `reagentTanks`, `attunedTankMap`, `reagent`, `amount`. Compare those fields after capture, transform and restore.

### Botania

Source [d0f9c2c9e6](https://github.com/GTNewHorizons/Botania/tree/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9) · 953 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [vazkii.botania.common.block.BlockAvatar](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/common/block/BlockAvatar.java#L1) (12 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 7 source file(s) contain matching declarations, including [Challenge.writeToNBT](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/client/challenge/Challenge.java#L29).
- Check preview rendering against `RendererSchematicChunk`. 46 source file(s) contain matching declarations, including [RenderAltar.renderWorldBlock](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/client/render/block/RenderAltar.java#L38).
- Check material picking against `BlockToItemStack`. 15 source file(s) contain matching declarations, including [BlockAltGrass.getPickBlock](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/common/block/BlockAltGrass.java#L110).
- Check printer placement against `SchematicPrinter`. 49 source file(s) contain matching declarations, including [SubTileEntity.onBlockPlacedBy](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/api/subtile/SubTileEntity.java#L116).
- Check interaction events against `SchematicPrinter`. 7 source file(s) contain matching declarations, including [MultiblockRenderHandler.onPlayerInteract](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/client/core/handler/MultiblockRenderHandler.java#L71).
- [MultiblockRenderHandler.onPlayerInteract](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/client/core/handler/MultiblockRenderHandler.java#L71) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [BlockAltGrass.getPickBlock](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/common/block/BlockAltGrass.java#L110) queries world state when selecting an item. Compare the materials list against the placed variant.
- [BlockAvatar.onBlockPlacedBy](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/common/block/BlockAvatar.java#L132) depends on placement orientation. Exercise the printer with a rotated schematic.
- [InventoryHelper.readFromNBT](https://github.com/GTNewHorizons/Botania/blob/d0f9c2c9e659e0340eb8c9c3e7801c04e497c3e9/src/main/java/vazkii/botania/common/core/helper/InventoryHelper.java#L310) accesses `size`, `Items`, `Slot`. Compare those fields after capture, transform and restore.

### Botanic-horizons

Source [172d79e04a](https://github.com/GTNewHorizons/Botanic-horizons/tree/172d79e04a42dd51fa4a234ba042c2c460d6bedc) · 71 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [net.fuzzycraft.botanichorizons.addons.tileentity.AutomationTileEntity](https://github.com/GTNewHorizons/Botanic-horizons/blob/172d79e04a42dd51fa4a234ba042c2c460d6bedc/src/main/java/net/fuzzycraft/botanichorizons/addons/tileentity/AutomationTileEntity.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [AutomationTileEntity.writeToNBT](https://github.com/GTNewHorizons/Botanic-horizons/blob/172d79e04a42dd51fa4a234ba042c2c460d6bedc/src/main/java/net/fuzzycraft/botanichorizons/addons/tileentity/AutomationTileEntity.java#L151).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [BlockAdvancedAlchemyPool.onBlockPlacedBy](https://github.com/GTNewHorizons/Botanic-horizons/blob/172d79e04a42dd51fa4a234ba042c2c460d6bedc/src/main/java/net/fuzzycraft/botanichorizons/addons/block/BlockAdvancedAlchemyPool.java#L74).

### BQ3-Thermos-patch

Source [1be8dbf860](https://github.com/GTNewHorizons/BQ3-Thermos-patch/tree/1be8dbf8602569fd406534104486f83b926e9d26) · 2 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Indirect effects from dependencies, injection, configuration and server rules remain open.

### BugTorch

Source [a0fd8d779f](https://github.com/GTNewHorizons/BugTorch/tree/a0fd8d779f443d439300e32f5ad76c79ff37f8dd) · 108 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 3 source file(s) contain matching declarations, including [MixinWorld.World](https://github.com/GTNewHorizons/BugTorch/blob/a0fd8d779f443d439300e32f5ad76c79ff37f8dd/src/main/java/jss/bugtorch/mixins/early/minecraft/fastrandom/MixinWorld.java#L10).

### BuildCraft

Source [898116026e](https://github.com/GTNewHorizons/BuildCraft/tree/898116026e949887dca4024cde080cc6851a1503) · 1025 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BuildCraftPipeAdapter` names [buildcraft.api.core.ISerializable](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/api/core/ISerializable.java#L1) (16 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 98 source file(s) contain matching declarations, including [INBTStoreable.readFromNBT](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/api/core/INBTStoreable.java#L7).
- Check preview rendering against `RendererSchematicChunk`. 23 source file(s) contain matching declarations, including [RenderArchitect.renderTileEntityAt](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/builders/render/RenderArchitect.java#L23).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockGenericPipe.getPickBlock](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/transport/BlockGenericPipe.java#L539).
- Check printer placement against `SchematicPrinter`. 18 source file(s) contain matching declarations, including [BlockConstructionMarker.onBlockPlacedBy](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/builders/BlockConstructionMarker.java#L55).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [BlockTankPlacementHandler.onTankPlace](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/factory/BlockTankPlacementHandler.java#L13).
- [Position.writeToNBT](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/api/core/Position.java#L136) accesses `i`, `j`, `k`, `orientation`. Compare those fields after capture, transform and restore.
- [BlockTankPlacementHandler.onTankPlace](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/factory/BlockTankPlacementHandler.java#L13) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [PipeRendererWorld.renderWorldBlock](https://github.com/GTNewHorizons/BuildCraft/blob/898116026e949887dca4024cde080cc6851a1503/src/main/java/buildcraft/transport/render/PipeRendererWorld.java#L233) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### BuildCraftCompat

Source [62543a7926](https://github.com/GTNewHorizons/BuildCraftCompat/tree/62543a79265c0f37a10dd188272b0a566496c9c4) · 143 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [PipeItemsPropolis.readFromNBT](https://github.com/GTNewHorizons/BuildCraftCompat/blob/62543a79265c0f37a10dd188272b0a566496c9c4/src/main/java/buildcraft/compat/forestry/pipes/PipeItemsPropolis.java#L37).
- [PipeLogicPropolis.readFromNBT](https://github.com/GTNewHorizons/BuildCraftCompat/blob/62543a79265c0f37a10dd188272b0a566496c9c4/src/main/java/buildcraft/compat/forestry/pipes/PipeLogicPropolis.java#L39) accesses `TypeFilter`, `GenomeFilterS`. Compare those fields after capture, transform and restore.

### BuildCraftOilTweak

Source [150d0013e1](https://github.com/GTNewHorizons/BuildCraftOilTweak/tree/150d0013e1d6dae7514772219b50b64424941f48) · 13 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [BuildCraftOilTweak](https://github.com/GTNewHorizons/BuildCraftOilTweak/blob/150d0013e1d6dae7514772219b50b64424941f48/src/main/java/buildcraft/oiltweak/BuildCraftOilTweak.java#L23). Indirect effects from dependencies, injection, configuration and server rules remain open.

### CarpentersBlocks

Source [4dda8ff4d6](https://github.com/GTNewHorizons/CarpentersBlocks/tree/4dda8ff4d6f90403670e95d46aef0a571c79b057) · 140 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CarpentersVisualAdapter` names [com.carpentersblocks.tileentity.TEBase](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TEBase.readFromNBT](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L75).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [BlockHandlerBase.renderWorldBlock](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/renderer/BlockHandlerBase.java#L122).
- Check printer placement against `SchematicPrinter`. 17 source file(s) contain matching declarations, including [BlockCarpentersBarrier.onBlockPlacedBy](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/block/BlockCarpentersBarrier.java#L79).
- [BlockCarpentersGate.onBlockPlacedBy](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/block/BlockCarpentersGate.java#L188) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BlockHandlerBase.renderWorldBlock](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/renderer/BlockHandlerBase.java#L122) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [TECarpentersSafe.readFromNBT](https://github.com/GTNewHorizons/CarpentersBlocks/blob/4dda8ff4d6f90403670e95d46aef0a571c79b057/src/main/java/com/carpentersblocks/tileentity/TECarpentersSafe.java#L136) accesses `inventorySize`. Compare those fields after capture, transform and restore.

### Celeritas

Source [b2a70f7f28](https://github.com/GTNewHorizons/Celeritas/tree/b2a70f7f28638eb3124a337584567fa72e00452a) · 1409 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `GeometrySupport` names [net.irisshaders.iris.api.v0.IrisApi](https://github.com/GTNewHorizons/Celeritas/blob/b2a70f7f28638eb3124a337584567fa72e00452a/modern/src/main/shaders_java/net/irisshaders/iris/api/v0/IrisApi.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check world or renderer transformation alongside the schematic world and render pipeline. 9 source file(s) contain matching declarations, including [ChunkAccessor.Chunk](https://github.com/GTNewHorizons/Celeritas/blob/b2a70f7f28638eb3124a337584567fa72e00452a/forge122/src/main/java/org/taumc/celeritas/mixin/core/terrain/ChunkAccessor.java#L7).

### Chisel

Source [a431e2a65c](https://github.com/GTNewHorizons/Chisel/tree/a431e2a65cf41f6e8341801b94d6ca9400e7d60b) · 179 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [team.chisel.block.BlockSnakestone](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/BlockSnakestone.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileEntityAutoChisel.readFromNBT](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/tileentity/TileEntityAutoChisel.java#L110).
- Check preview rendering against `RendererSchematicChunk`. 12 source file(s) contain matching declarations, including [RendererCTMPane.renderWorldBlock](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/client/render/RendererCTMPane.java#L213).
- Check material picking against `BlockToItemStack`. 5 source file(s) contain matching declarations, including [BlockCarvableBeacon.getPickBlock](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/BlockCarvableBeacon.java#L68).
- Check printer placement against `SchematicPrinter`. 6 source file(s) contain matching declarations, including [BlockCarvableBeacon.onBlockPlacedBy](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/BlockCarvableBeacon.java#L83).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ChiselController.onInteract](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/item/chisel/ChiselController.java#L66).
- [BlockCarvableBeacon.getPickBlock](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/BlockCarvableBeacon.java#L68) queries world state when selecting an item. Compare the materials list against the placed variant.
- [BlockCarvableStairs.onBlockPlacedBy](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/BlockCarvableStairs.java#L64) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityAutoChisel.readFromNBT](https://github.com/GTNewHorizons/Chisel/blob/a431e2a65cf41f6e8341801b94d6ca9400e7d60b/src/main/java/team/chisel/block/tileentity/TileEntityAutoChisel.java#L110) accesses `Items`, `Slot`, `CustomName`. Compare those fields after capture, transform and restore.

### ChiselTones

Source [46445985f8](https://github.com/GTNewHorizons/ChiselTones/tree/46445985f8987c92fa897746c500d31739671b4b) · 3 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ChiselTones](https://github.com/GTNewHorizons/ChiselTones/blob/46445985f8987c92fa897746c500d31739671b4b/src/main/java/tehnut/chiseltones/ChiselTones.java#L15). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ChromaticTooltips

Source [3936db9468](https://github.com/GTNewHorizons/ChromaticTooltips/tree/3936db9468104c99e78a9461b4c3af52d2f790a4) · 95 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ChromaticTooltips](https://github.com/GTNewHorizons/ChromaticTooltips/blob/3936db9468104c99e78a9461b4c3af52d2f790a4/src/main/java/com/slprime/chromatictooltips/ChromaticTooltips.java#L13). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ChromaticTooltipsCompat

Source [4e51e114f7](https://github.com/GTNewHorizons/ChromaticTooltipsCompat/tree/4e51e114f7a51e8fb0ab9a36322bf00be9a94eb3) · 88 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ChromaticTooltipsCompat](https://github.com/GTNewHorizons/ChromaticTooltipsCompat/blob/4e51e114f7a51e8fb0ab9a36322bf00be9a94eb3/src/main/java/com/slprime/chromatictooltipscompat/ChromaticTooltipsCompat.java#L13). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ChunkAPI

Source [199251b75d](https://github.com/GTNewHorizons/ChunkAPI/tree/199251b75d41c23ae5085bb32e39a3380efe7c7a) · 47 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [ChunkMixin.Chunk](https://github.com/GTNewHorizons/ChunkAPI/blob/199251b75d41c23ae5085bb32e39a3380efe7c7a/src/main/java/com/falsepattern/chunk/internal/mixin/mixins/client/vanilla/ChunkMixin.java#L42).

### Climate-Control

Source [3ade14a9fe](https://github.com/GTNewHorizons/Climate-Control/tree/3ade14a9fef34d1335510afb0e14efff866e1290) · 176 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [Savee.readFromNBT](https://github.com/GTNewHorizons/Climate-Control/blob/3ade14a9fef34d1335510afb0e14efff866e1290/src/main/java/climateControl/utils/Savee.java#L107).

### CodeChickenCore

Source [fd7d5c269f](https://github.com/GTNewHorizons/CodeChickenCore/tree/fd7d5c269f91a1a1bcd7a46a16636358b90a129c) · 176 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `VisualStreams` names [codechicken.lib.data.MCDataInput](https://github.com/GTNewHorizons/CodeChickenCore/blob/fd7d5c269f91a1a1bcd7a46a16636358b90a129c/src/main/java/codechicken/lib/data/MCDataInput.java#L1) (5 matching upstream source names). Check reflected members and client state with the deployed versions.

### CompactKineticGenerators

Source [f26daa9e65](https://github.com/GTNewHorizons/CompactKineticGenerators/tree/f26daa9e65df38105c2abcb3e5b3526097197812) · 35 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [BasicTileCkg.readFromNBT](https://github.com/GTNewHorizons/CompactKineticGenerators/blob/f26daa9e65df38105c2abcb3e5b3526097197812/src/main/java/com/lulan/compactkineticgenerators/tileentity/BasicTileCkg.java#L121).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockCkg.onBlockPlacedBy](https://github.com/GTNewHorizons/CompactKineticGenerators/blob/f26daa9e65df38105c2abcb3e5b3526097197812/src/main/java/com/lulan/compactkineticgenerators/block/BlockCkg.java#L340).
- [BlockCkg.onBlockPlacedBy](https://github.com/GTNewHorizons/CompactKineticGenerators/blob/f26daa9e65df38105c2abcb3e5b3526097197812/src/main/java/com/lulan/compactkineticgenerators/block/BlockCkg.java#L340) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BasicTileCkg.readFromNBT](https://github.com/GTNewHorizons/CompactKineticGenerators/blob/f26daa9e65df38105c2abcb3e5b3526097197812/src/main/java/com/lulan/compactkineticgenerators/tileentity/BasicTileCkg.java#L121) accesses `type`. Compare those fields after capture, transform and restore.

### Computronics

Source [f217c6f000](https://github.com/GTNewHorizons/Computronics/tree/f217c6f0003ee2b6a910b29f7f7ffc185927633d) · 311 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [pl.asie.computronics.integration.railcraft.tile.TileDigitalDetector](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/integration/railcraft/tile/TileDigitalDetector.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 20 source file(s) contain matching declarations, including [DroneStationPluggable.readFromNBT](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/integration/buildcraft/pluggable/DroneStationPluggable.java#L106).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [AudioCableRender.renderWorldBlock](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/client/AudioCableRender.java#L62).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [BlockDigitalDetector.onBlockPlacedBy](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/integration/railcraft/block/BlockDigitalDetector.java#L54).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [SwarmProvider.onPlayerInteract](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/integration/forestry/nanomachines/SwarmProvider.java#L122).
- [AudioCableRender.renderWorldBlock](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/client/AudioCableRender.java#L62) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [ActionParameterLampColor.writeToNBT](https://github.com/GTNewHorizons/Computronics/blob/f217c6f0003ee2b6a910b29f7f7ffc185927633d/src/main/java/pl/asie/computronics/integration/buildcraft/statements/parameters/ActionParameterLampColor.java#L107) accesses `size`, `color`. Compare those fields after capture, transform and restore.

### Controlling

Source [1a27d4494f](https://github.com/GTNewHorizons/Controlling/tree/1a27d4494f363bf1b775b950e4d659c333c79fba) · 17 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Controlling](https://github.com/GTNewHorizons/Controlling/blob/1a27d4494f363bf1b775b950e4d659c333c79fba/src/main/java/com/blamejared/controlling/Controlling.java#L14). Indirect effects from dependencies, injection, configuration and server rules remain open.

### CookingForBlockheads

Source [6bf2bd408c](https://github.com/GTNewHorizons/CookingForBlockheads/tree/6bf2bd408c522225ee92864ce9c03b724c5fd416) · 142 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [net.blay09.mods.cookingforblockheads.block.BlockBaseKitchen](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/block/BlockBaseKitchen.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 8 source file(s) contain matching declarations, including [BaseKitchenTileWithInventory.readFromNBT](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/tile/BaseKitchenTileWithInventory.java#L65).
- Check preview rendering against `RendererSchematicChunk`. 20 source file(s) contain matching declarations, including [CabinetBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/client/render/block/CabinetBlockRenderer.java#L23).
- Check printer placement against `SchematicPrinter`. 8 source file(s) contain matching declarations, including [BlockCookingTable.onBlockPlacedBy](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/block/BlockCookingTable.java#L41).
- [BlockCookingTable.onBlockPlacedBy](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/block/BlockCookingTable.java#L41) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BaseKitchenTileWithInventory.readFromNBT](https://github.com/GTNewHorizons/CookingForBlockheads/blob/6bf2bd408c522225ee92864ce9c03b724c5fd416/src/main/java/net/blay09/mods/cookingforblockheads/tile/BaseKitchenTileWithInventory.java#L65) accesses `Items`, `Slot`, `Color`, `IsFlipped`. Compare those fields after capture, transform and restore.

### CoreTweaks

Source [92cc4fd905](https://github.com/GTNewHorizons/CoreTweaks/tree/92cc4fd905fba09abf7823d7b34e9941d7ae1b4b) · 102 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 10 source file(s) contain matching declarations, including [MixinRenderGlobal.RenderGlobal](https://github.com/GTNewHorizons/CoreTweaks/blob/92cc4fd905fba09abf7823d7b34e9941d7ae1b4b/src/main/java/makamys/coretweaks/mixin/bugfix/displaylistdelete/MixinRenderGlobal.java#L10).

### CosmeticArmorReworked

Source [7adf5a2147](https://github.com/GTNewHorizons/CosmeticArmorReworked/tree/7adf5a2147ebb662fa9ca63dfe84d13517c7c49d) · 21 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [InventoryCosArmor.readFromNBT](https://github.com/GTNewHorizons/CosmeticArmorReworked/blob/7adf5a2147ebb662fa9ca63dfe84d13517c7c49d/src/main/java/lain/mods/cos/inventory/InventoryCosArmor.java#L108).
- [InventoryCosArmor.readFromNBT](https://github.com/GTNewHorizons/CosmeticArmorReworked/blob/7adf5a2147ebb662fa9ca63dfe84d13517c7c49d/src/main/java/lain/mods/cos/inventory/InventoryCosArmor.java#L108) accesses `CosArmor.Inventory.Size`, `CosArmor.Inventory`, `Slot`, `isSkinArmor`. Compare those fields after capture, transform and restore.

### CraftTweaker

Source [f0ae935d58](https://github.com/GTNewHorizons/CraftTweaker/tree/f0ae935d582fe9725d3e8eaaf7819b96f0a94c3a) · 286 tracked Java, Scala or Kotlin files outside test/example directories.

- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ForgeEventHandler.onPlayerInteract](https://github.com/GTNewHorizons/CraftTweaker/blob/f0ae935d582fe9725d3e8eaaf7819b96f0a94c3a/src/main/java/minetweaker/mc1710/ForgeEventHandler.java#L38).

### CropsNH

Source [629909b7ec](https://github.com/GTNewHorizons/CropsNH/tree/629909b7ec14ffcf178a409f7f614a39a2c71f7c) · 416 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileEntityCropSticks.writeToNBT](https://github.com/GTNewHorizons/CropsNH/blob/629909b7ec14ffcf178a409f7f614a39a2c71f7c/src/main/java/com/gtnewhorizon/cropsnh/tileentity/TileEntityCropSticks.java#L719).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RenderBlockBase.renderTileEntityAt](https://github.com/GTNewHorizons/CropsNH/blob/629909b7ec14ffcf178a409f7f614a39a2c71f7c/src/main/java/com/gtnewhorizon/cropsnh/renderers/blocks/RenderBlockBase.java#L98).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [ItemCropSticks.placeBlockAt](https://github.com/GTNewHorizons/CropsNH/blob/629909b7ec14ffcf178a409f7f614a39a2c71f7c/src/main/java/com/gtnewhorizon/cropsnh/items/blocks/ItemCropSticks.java#L32).

### CubicChunks1710

Source [9d2c673e72](https://github.com/GTNewHorizons/CubicChunks1710/tree/9d2c673e72d42c6fadd54146989246fca169c16c) · 331 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CubicChunks` names [com.cardinalstar.cubicchunks.world.cube.ICubeProvider](https://github.com/GTNewHorizons/CubicChunks1710/blob/9d2c673e72d42c6fadd54146989246fca169c16c/src/main/java/com/cardinalstar/cubicchunks/world/cube/ICubeProvider.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [CubicChunksSavedData.readFromNBT](https://github.com/GTNewHorizons/CubicChunks1710/blob/9d2c673e72d42c6fadd54146989246fca169c16c/src/main/java/com/cardinalstar/cubicchunks/world/CubicChunksSavedData.java#L96).
- Check world or renderer transformation alongside the schematic world and render pipeline. 13 source file(s) contain matching declarations, including [MixinChunk_Cubes.Chunk](https://github.com/GTNewHorizons/CubicChunks1710/blob/9d2c673e72d42c6fadd54146989246fca169c16c/src/main/java/com/cardinalstar/cubicchunks/mixin/early/client/MixinChunk_Cubes.java#L44).
- [CubicChunksSavedData.readFromNBT](https://github.com/GTNewHorizons/CubicChunks1710/blob/9d2c673e72d42c6fadd54146989246fca169c16c/src/main/java/com/cardinalstar/cubicchunks/world/CubicChunksSavedData.java#L96) accesses `minHeight`, `maxHeight`, `fillerBlocks`, `topId`. Compare those fields after capture, transform and restore.
- MixinChunk exposes getLoadedCube(int), getTopYWithStaging(int, int) and getPrecipitationIndex(). These names and arities match the modern column path in Plus. The lighting update additionally depends on ILightingManager.doOnBlockSetLightUpdates and the cube lighting state.

### Custom-Main-Menu

Source [327b7f886d](https://github.com/GTNewHorizons/Custom-Main-Menu/tree/327b7f886d97d82f8f102fddfd61e408f68efd23) · 52 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [CustomMainMenu](https://github.com/GTNewHorizons/Custom-Main-Menu/blob/327b7f886d97d82f8f102fddfd61e408f68efd23/src/main/java/lumien/custommainmenu/CustomMainMenu.java#L22). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Darkerer

Source [d9eefaaab5](https://github.com/GTNewHorizons/Darkerer/tree/d9eefaaab5de427637f87cc7b9650657b0902a55) · 10 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 2 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Darkerer/blob/d9eefaaab5de427637f87cc7b9650657b0902a55/src/main/java/glowredman/darkerer/mixins/MixinEntityRenderer.java#L21).

### Dashboard

Source [078f9e3b87](https://github.com/GTNewHorizons/Dashboard/tree/078f9e3b871713cd0faeeeabd6127573f3db8d21) · 80 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinEntityRenderer_Camera.EntityRenderer](https://github.com/GTNewHorizons/Dashboard/blob/078f9e3b871713cd0faeeeabd6127573f3db8d21/src/main/java/com/gtnewhorizons/dashboard/mixins/early/MixinEntityRenderer_Camera.java#L15).

### Databack

Source [0c892a333f](https://github.com/GTNewHorizons/Databack/tree/0c892a333fbe77ca2b88b272b4b68ce3bc71ab83) · 476 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [ChunkSetData.readFromNBT](https://github.com/GTNewHorizons/Databack/blob/0c892a333fbe77ca2b88b272b4b68ce3bc71ab83/src/main/java/databack/common/worldgen/feature/population/ChunkSetData.java#L47).
- Check world or renderer transformation alongside the schematic world and render pipeline. 2 source file(s) contain matching declarations, including [MixinWorld_Ext.World](https://github.com/GTNewHorizons/Databack/blob/0c892a333fbe77ca2b88b272b4b68ce3bc71ab83/src/main/java/databack/mixins/early/MixinWorld_Ext.java#L12).
- [ChunkSetData.readFromNBT](https://github.com/GTNewHorizons/Databack/blob/0c892a333fbe77ca2b88b272b4b68ce3bc71ab83/src/main/java/databack/common/worldgen/feature/population/ChunkSetData.java#L47) accesses `chunks`. Compare those fields after capture, transform and restore.

### Default-Configs

Source [2c6b8d49a6](https://github.com/GTNewHorizons/Default-Configs/tree/2c6b8d49a6b0b26188ee2eb45cbebc9e5d907f47) · 9 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [DefaultKeys](https://github.com/GTNewHorizons/Default-Configs/blob/2c6b8d49a6b0b26188ee2eb45cbebc9e5d907f47/src/main/java/net/blay09/mods/defaultkeys/DefaultKeys.java#L12). Indirect effects from dependencies, injection, configuration and server rules remain open.

### DefaultServerList

Source [70c9c88b0c](https://github.com/GTNewHorizons/DefaultServerList/tree/70c9c88b0c7faa20633ffec10fec340626e36e76) · 4 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Indirect effects from dependencies, injection, configuration and server rules remain open.

### DefaultWorldGenerator

Source [d9e5e36d95](https://github.com/GTNewHorizons/DefaultWorldGenerator/tree/d9e5e36d95fbfc0e82dbad20b85453a38ad1ca8e) · 8 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [DefaultWorldGenerator](https://github.com/GTNewHorizons/DefaultWorldGenerator/blob/d9e5e36d95fbfc0e82dbad20b85453a38ad1ca8e/src/main/java/com/fireball1725/defaultworldgenerator/DefaultWorldGenerator.java#L16). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Display-Panels

Source [f94737c548](https://github.com/GTNewHorizons/Display-Panels/tree/f94737c5488a1e9579d88ba051c0d79ee36e945f) · 213 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CoordinateLinks` names [shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/tileentities/TileEntityInfoPanel.java#L1) (4 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 13 source file(s) contain matching declarations, including [InventoryItem.readFromNBT](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/InventoryItem.java#L121).
- Check preview rendering against `RendererSchematicChunk`. 4 source file(s) contain matching declarations, including [MainBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/renderers/MainBlockRenderer.java#L70).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [BlockNuclearControlMain.onBlockPlaced](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/blocks/BlockNuclearControlMain.java#L135).
- [InventoryItem.readFromNBT](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/InventoryItem.java#L121) accesses `ItemInventory`, `Slot`. Compare those fields after capture, transform and restore.
- [MainBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Display-Panels/blob/f94737c5488a1e9579d88ba051c0d79ee36e945f/src/main/java/shedar/mods/ic2/nuclearcontrol/renderers/MainBlockRenderer.java#L70) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### DistantHorizons

Source [b287f8388b](https://github.com/GTNewHorizons/DistantHorizons/tree/b287f8388b60714b76156266d344cc9d9baef7ea) · 885 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 6 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/DistantHorizons/blob/b287f8388b60714b76156266d344cc9d9baef7ea/dh/forge112/src/main/java/com/seibel/distanthorizons/forge112/mixins/client/MixinEntityRenderer.java#L40).
- Check interaction events against `SchematicPrinter`. 5 source file(s) contain matching declarations, including [ForgeClientProxy.rightClickBlockEvent](https://github.com/GTNewHorizons/DistantHorizons/blob/b287f8388b60714b76156266d344cc9d9baef7ea/dh/forge/src/main/java/com/seibel/distanthorizons/forge/ForgeClientProxy.java#L114).

### Draconic-Evolution

Source [04c232f7cf](https://github.com/GTNewHorizons/Draconic-Evolution/tree/04c232f7cf9616f800d9efde95137655d2924771) · 387 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [com.brandon3055.draconicevolution.common.blocks.Potentiometer](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/Potentiometer.java#L1) (11 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 43 source file(s) contain matching declarations, including [MultiblockHelper.writeToNBT](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/multiblock/MultiblockHelper.java#L76).
- Check preview rendering against `RendererSchematicChunk`. 18 source file(s) contain matching declarations, including [RenderPortal.renderWorldBlock](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/client/render/block/RenderPortal.java#L24).
- Check material picking against `BlockToItemStack`. 10 source file(s) contain matching declarations, including [CKeyStone.getPickBlock](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/CKeyStone.java#L102).
- Check printer placement against `SchematicPrinter`. 19 source file(s) contain matching declarations, including [ChaosCrystal.onBlockPlacedBy](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/ChaosCrystal.java#L95).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [MinecraftForgeEventHandler.playerInteract](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/handler/MinecraftForgeEventHandler.java#L405).
- [CKeyStone.getPickBlock](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/CKeyStone.java#L102) queries world state when selecting an item. Compare the materials list against the placed variant.
- [DraconiumChest.onBlockPlacedBy](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/DraconiumChest.java#L209) depends on placement orientation. Exercise the printer with a rotated schematic.
- [MultiblockHelper.writeToNBT](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/blocks/multiblock/MultiblockHelper.java#L76) accesses `X_`, `Y_`, `Z_`, `Init_`. Compare those fields after capture, transform and restore.
- [MinecraftForgeEventHandler.playerInteract](https://github.com/GTNewHorizons/Draconic-Evolution/blob/04c232f7cf9616f800d9efde95137655d2924771/src/main/java/com/brandon3055/draconicevolution/common/handler/MinecraftForgeEventHandler.java#L405) can cancel the interaction. Verify the printer on a server with the handler enabled.

### DummyCore

Source [3d699ff99f](https://github.com/GTNewHorizons/DummyCore/tree/3d699ff99f956f4cef56055d800a216343e82d79) · 58 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [CoreInitialiser](https://github.com/GTNewHorizons/DummyCore/blob/3d699ff99f956f4cef56055d800a216343e82d79/src/main/java/DummyCore/Core/CoreInitialiser.java#L36). Indirect effects from dependencies, injection, configuration and server rules remain open.

### DuraDisplay

Source [58d45414b6](https://github.com/GTNewHorizons/DuraDisplay/tree/58d45414b65b74cc370bd5cd9cd707ca960586f3) · 31 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [DuraDisplay](https://github.com/GTNewHorizons/DuraDisplay/blob/58d45414b65b74cc370bd5cd9cd707ca960586f3/src/main/java/com/caedis/duradisplay/DuraDisplay.java#L15). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Electro-Magic-Tools

Source [3d90755aea](https://github.com/GTNewHorizons/Electro-Magic-Tools/tree/3d90755aea0cf5bb32e500d0d36726590f38258f) · 96 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [emt.tile.TileEntityEMT](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/tile/TileEntityEMT.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 5 source file(s) contain matching declarations, including [TileEntityEMT.readFromNBT](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/tile/TileEntityEMT.java#L14).
- Check printer placement against `SchematicPrinter`. 6 source file(s) contain matching declarations, including [BlockBaseContainer.onBlockPlacedBy](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/block/BlockBaseContainer.java#L123).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [EMTEventHandler.createCloud](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/util/EMTEventHandler.java#L81).
- [BlockBaseContainer.onBlockPlacedBy](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/block/BlockBaseContainer.java#L123) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityEMT.readFromNBT](https://github.com/GTNewHorizons/Electro-Magic-Tools/blob/3d90755aea0cf5bb32e500d0d36726590f38258f/src/main/java/emt/tile/TileEntityEMT.java#L14) accesses `facing`, `isOn`. Compare those fields after capture, transform and restore.

### EnderCore

Source [3763a06c5e](https://github.com/GTNewHorizons/EnderCore/tree/3763a06c5ee5ea658f399559c4249932e7f9eb60) · 169 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileEntityEnder.readFromNBT](https://github.com/GTNewHorizons/EnderCore/blob/3763a06c5ee5ea658f399559c4249932e7f9eb60/src/main/java/com/enderio/core/common/TileEntityEnder.java#L82).
- Check preview rendering against `RendererSchematicChunk`. 4 source file(s) contain matching declarations, including [DirectionalModelRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/EnderCore/blob/3763a06c5ee5ea658f399559c4249932e7f9eb60/src/main/java/com/enderio/core/client/render/DirectionalModelRenderer.java#L98).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockEnder.getPickBlock](https://github.com/GTNewHorizons/EnderCore/blob/3763a06c5ee5ea658f399559c4249932e7f9eb60/src/main/java/com/enderio/core/common/BlockEnder.java#L141).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [RightClickCropHandler.handleCropRightClick](https://github.com/GTNewHorizons/EnderCore/blob/3763a06c5ee5ea658f399559c4249932e7f9eb60/src/main/java/com/enderio/core/common/handlers/RightClickCropHandler.java#L65).
- [RightClickCropHandler.handleCropRightClick](https://github.com/GTNewHorizons/EnderCore/blob/3763a06c5ee5ea658f399559c4249932e7f9eb60/src/main/java/com/enderio/core/common/handlers/RightClickCropHandler.java#L65) can cancel the interaction. Verify the printer on a server with the handler enabled.

### EnderIO

Source [749ea96c62](https://github.com/GTNewHorizons/EnderIO/tree/749ea96c62f01883108f755b05fb4843e06e35c4) · 851 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `EnderIOConduitAdapter` names [crazypants.enderio.conduit.AbstractConduit](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/conduit/AbstractConduit.java#L1) (9 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 31 source file(s) contain matching declarations, including [AbstractConduit.writeToNBT](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/conduit/AbstractConduit.java#L353).
- Check preview rendering against `RendererSchematicChunk`. 29 source file(s) contain matching declarations, including [ConduitBundleRenderer.renderWorldBlock](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/conduit/render/ConduitBundleRenderer.java#L58).
- Check material picking against `BlockToItemStack`. 2 source file(s) contain matching declarations, including [BlockDarkSteelAnvil.getPickBlock](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/block/BlockDarkSteelAnvil.java#L97).
- Check printer placement against `SchematicPrinter`. 32 source file(s) contain matching declarations, including [BlockDarkSteelPressurePlate.onBlockPlacedBy](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/block/BlockDarkSteelPressurePlate.java#L120).
- [BlockDarkSteelAnvil.getPickBlock](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/block/BlockDarkSteelAnvil.java#L97) queries world state when selecting an item. Compare the materials list against the placed variant.
- [AbstractConduit.writeToNBT](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/conduit/AbstractConduit.java#L353) accesses `connections`, `externalConnections`, `signalActive`. Compare those fields after capture, transform and restore.
- [ConduitBundleRenderer.renderWorldBlock](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/conduit/render/ConduitBundleRenderer.java#L58) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [BlockEnderIO.onBlockPlacedBy](https://github.com/GTNewHorizons/EnderIO/blob/749ea96c62f01883108f755b05fb4843e06e35c4/src/main/java/crazypants/enderio/enderface/BlockEnderIO.java#L66) depends on placement orientation. Exercise the printer with a rotated schematic.

### EnderStorage

Source [780054ab6a](https://github.com/GTNewHorizons/EnderStorage/tree/780054ab6af449e56c2f1927310514b42aff61c6) · 37 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CodeChickenVisualAdapter` names [codechicken.enderstorage.storage.item.TileEnderChest](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/storage/item/TileEnderChest.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [TileFrequencyOwner.readFromNBT](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/common/TileFrequencyOwner.java#L61).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [EnderChestRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/storage/item/EnderChestRenderer.java#L133).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockEnderStorage.getPickBlock](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/common/BlockEnderStorage.java#L68).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [BlockEnderStorage.onBlockPlacedBy](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/common/BlockEnderStorage.java#L171).
- [BlockEnderStorage.getPickBlock](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/common/BlockEnderStorage.java#L68) queries world state when selecting an item. Compare the materials list against the placed variant.
- [TileFrequencyOwner.readFromNBT](https://github.com/GTNewHorizons/EnderStorage/blob/780054ab6af449e56c2f1927310514b42aff61c6/src/main/java/codechicken/enderstorage/common/TileFrequencyOwner.java#L61) accesses `freq`, `owner`. Compare those fields after capture, transform and restore.

### EnderZoo

Source [2e122a4f1f](https://github.com/GTNewHorizons/EnderZoo/tree/2e122a4f1fa4e3dfd0c88143e68066fd38fed580) · 82 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemSpawnEgg.onItemUse](https://github.com/GTNewHorizons/EnderZoo/blob/2e122a4f1fa4e3dfd0c88143e68066fd38fed580/src/main/java/crazypants/enderzoo/item/ItemSpawnEgg.java#L89).

### EnhancedLootBags

Source [c51008127d](https://github.com/GTNewHorizons/EnhancedLootBags/tree/c51008127dfe6cf1865800bb4528f73aba0bd179) · 20 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [EnhancedLootBags](https://github.com/GTNewHorizons/EnhancedLootBags/blob/c51008127dfe6cf1865800bb4528f73aba0bd179/src/main/java/eu/usrv/enhancedlootbags/EnhancedLootBags.java#L38). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Et-Futurum-Requiem

Source [bd47fafc55](https://github.com/GTNewHorizons/Et-Futurum-Requiem/tree/bd47fafc554825dd510981ffd4e8a488f5cf8ca7) · 767 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `SchematicWorld` names [ganymedes01.etfuturum.blocks.BlockBanner](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/blocks/BlockBanner.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 12 source file(s) contain matching declarations, including [TileEntityBanner.writeToNBT](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBanner.java#L55).
- Check preview rendering against `RendererSchematicChunk`. 38 source file(s) contain matching declarations, including [BlockAmethystClusterRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockAmethystClusterRenderer.java#L25).
- Check material picking against `BlockToItemStack`. 3 source file(s) contain matching declarations, including [BaseCaveVines.getPickBlock](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/blocks/BaseCaveVines.java#L84).
- Check printer placement against `SchematicPrinter`. 29 source file(s) contain matching declarations, including [BlockAmethystCluster.onBlockPlaced](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/blocks/BlockAmethystCluster.java#L103).
- Check world or renderer transformation alongside the schematic world and render pipeline. 18 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/mixins/early/backlytra/client/MixinEntityRenderer.java#L13).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [ServerEventHandler.onPlaceBlock](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/core/handlers/ServerEventHandler.java#L762).
- [BlockBanner.getPickBlock](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/blocks/BlockBanner.java#L116) queries world state when selecting an item. Compare the materials list against the placed variant.
- [BlockBarrel.onBlockPlacedBy](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/blocks/BlockBarrel.java#L84) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BlockColoredWaterCauldronRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/client/renderer/block/BlockColoredWaterCauldronRenderer.java#L22) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [DebugTestItem.onLeftClick](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/items/DebugTestItem.java#L68) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [TileEntityBanner.writeToNBT](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/bd47fafc554825dd510981ffd4e8a488f5cf8ca7/src/main/java/ganymedes01/etfuturum/tileentities/TileEntityBanner.java#L55) accesses `Base`, `IsStanding`, `Patterns`. Compare those fields after capture, transform and restore.

### Eternal-Singularity

Source [a6e9185f18](https://github.com/GTNewHorizons/Eternal-Singularity/tree/a6e9185f180b8898927193a2afc340d4d8642b7f) · 8 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [EternalSingularityMod](https://github.com/GTNewHorizons/Eternal-Singularity/blob/a6e9185f180b8898927193a2afc340d4d8642b7f/src/main/java/singulariteam/eternalsingularity/EternalSingularityMod.java#L25). Indirect effects from dependencies, injection, configuration and server rules remain open.

### FarmingForEngineers

Source [8580636021](https://github.com/GTNewHorizons/FarmingForEngineers/tree/8580636021109bf8142537ecc825f9a0faa6d97f) · 55 tracked Java, Scala or Kotlin files outside test/example directories.

- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [MarketBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/FarmingForEngineers/blob/8580636021109bf8142537ecc825f9a0faa6d97f/src/main/java/com/guigs44/farmingforengineers/client/render/block/MarketBlockRenderer.java#L29).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockMarket.onBlockPlacedBy](https://github.com/GTNewHorizons/FarmingForEngineers/blob/8580636021109bf8142537ecc825f9a0faa6d97f/src/main/java/com/guigs44/farmingforengineers/block/BlockMarket.java#L75).
- [BlockMarket.onBlockPlacedBy](https://github.com/GTNewHorizons/FarmingForEngineers/blob/8580636021109bf8142537ecc825f9a0faa6d97f/src/main/java/com/guigs44/farmingforengineers/block/BlockMarket.java#L75) depends on placement orientation. Exercise the printer with a rotated schematic.

### Fether

Source [e43ace4e05](https://github.com/GTNewHorizons/Fether/tree/e43ace4e05fc254c9f58b622db789724ea47c190) · 32 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemNetherBed.onItemUse](https://github.com/GTNewHorizons/Fether/blob/e43ace4e05fc254c9f58b622db789724ea47c190/src/main/java/glowredman/fether/items/ItemNetherBed.java#L15).
- [ItemNetherBed.onItemUse](https://github.com/GTNewHorizons/Fether/blob/e43ace4e05fc254c9f58b622db789724ea47c190/src/main/java/glowredman/fether/items/ItemNetherBed.java#L15) depends on placement orientation. Exercise the printer with a rotated schematic.

### FindIt

Source [296f29ae76](https://github.com/GTNewHorizons/FindIt/tree/296f29ae764cfcb56ae87338766b4d7f65bda21e) · 32 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [FindIt](https://github.com/GTNewHorizons/FindIt/blob/296f29ae764cfcb56ae87338766b4d7f65bda21e/src/main/java/com/gtnh/findit/FindIt.java#L29). Indirect effects from dependencies, injection, configuration and server rules remain open.

### FloodLights

Source [75693f4e0b](https://github.com/GTNewHorizons/FloodLights/tree/75693f4e0b89fd4798acf814a70139cfe3318d56) · 81 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [de.keridos.floodlights.block.BlockFL](https://github.com/GTNewHorizons/FloodLights/blob/75693f4e0b89fd4798acf814a70139cfe3318d56/src/main/java/de/keridos/floodlights/block/BlockFL.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [TileEnergyHandler.readFromNBT](https://github.com/GTNewHorizons/FloodLights/blob/75693f4e0b89fd4798acf814a70139cfe3318d56/src/main/java/cofh/api/energy/TileEnergyHandler.java#L18).
- Check preview rendering against `RendererSchematicChunk`. 4 source file(s) contain matching declarations, including [RotatableBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/FloodLights/blob/75693f4e0b89fd4798acf814a70139cfe3318d56/src/main/java/de/keridos/floodlights/client/render/block/RotatableBlockRenderer.java#L89).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [BlockFL.onBlockPlacedBy](https://github.com/GTNewHorizons/FloodLights/blob/75693f4e0b89fd4798acf814a70139cfe3318d56/src/main/java/de/keridos/floodlights/block/BlockFL.java#L122).
- [RotatableBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/FloodLights/blob/75693f4e0b89fd4798acf814a70139cfe3318d56/src/main/java/de/keridos/floodlights/client/render/block/RotatableBlockRenderer.java#L89) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### ForbiddenMagic

Source [7bae526481](https://github.com/GTNewHorizons/ForbiddenMagic/tree/7bae5264813ab44bce76b5d41fe44e468ab869bb) · 153 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [AMVector3.writeToNBT](https://github.com/GTNewHorizons/ForbiddenMagic/blob/7bae5264813ab44bce76b5d41fe44e468ab869bb/src/api/java/am2/api/math/AMVector3.java#L202).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [TileEntityWrathCageRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/ForbiddenMagic/blob/7bae5264813ab44bce76b5d41fe44e468ab869bb/src/main/java/fox/spiteful/forbidden/client/TileEntityWrathCageRenderer.java#L56).
- Check material picking against `BlockToItemStack`. 2 source file(s) contain matching declarations, including [BlockArcaneCake.getPickBlock](https://github.com/GTNewHorizons/ForbiddenMagic/blob/7bae5264813ab44bce76b5d41fe44e468ab869bb/src/main/java/fox/spiteful/forbidden/blocks/BlockArcaneCake.java#L194).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [BlockRoseBush.onBlockPlacedBy](https://github.com/GTNewHorizons/ForbiddenMagic/blob/7bae5264813ab44bce76b5d41fe44e468ab869bb/src/main/java/fox/spiteful/forbidden/blocks/BlockRoseBush.java#L112).
- [AMVector3.writeToNBT](https://github.com/GTNewHorizons/ForbiddenMagic/blob/7bae5264813ab44bce76b5d41fe44e468ab869bb/src/api/java/am2/api/math/AMVector3.java#L202) accesses `Vec3_x`, `Vec3_y`, `Vec3_z`. Compare those fields after capture, transform and restore.

### ForestryMC

Source [a17580e04d](https://github.com/GTNewHorizons/ForestryMC/tree/a17580e04d4e833b24cc114b93b0b816ffc0a6b5) · 1356 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [forestry.arboriculture.blocks.BlockLog](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/arboriculture/blocks/BlockLog.java#L1) (6 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 82 source file(s) contain matching declarations, including [INBTTagable.readFromNBT](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/api/core/INBTTagable.java#L12).
- Check preview rendering against `RendererSchematicChunk`. 12 source file(s) contain matching declarations, including [RenderCandleBlock.renderWorldBlock](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/apiculture/render/RenderCandleBlock.java#L32).
- Check material picking against `BlockToItemStack`. 9 source file(s) contain matching declarations, including [BlockCandle.getPickBlock](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/apiculture/blocks/BlockCandle.java#L251).
- Check printer placement against `SchematicPrinter`. 15 source file(s) contain matching declarations, including [BlockCandle.onBlockPlacedBy](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/apiculture/blocks/BlockCandle.java#L276).
- [BeekeepingLogic.readFromNBT](https://github.com/GTNewHorizons/ForestryMC/blob/a17580e04d4e833b24cc114b93b0b816ffc0a6b5/src/main/java/forestry/apiculture/BeekeepingLogic.java#L83) accesses `BreedingTime`, `Throttle`, `queen`, `Active`. Compare those fields after capture, transform and restore.

### Forgelin

Source [97e3937690](https://github.com/GTNewHorizons/Forgelin/tree/97e3937690bf1ee71786e587b988ac4195fc3f2d) · 7 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Forgelin](https://github.com/GTNewHorizons/Forgelin/blob/97e3937690bf1ee71786e587b988ac4195fc3f2d/src/main/kotlin/net/shadowfacts/forgelin/Forgelin.kt#L11). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ForgeMultipart

Source [3b181eb231](https://github.com/GTNewHorizons/ForgeMultipart/tree/3b181eb231a68ee578c53aa72b276ca5c8e29366) · 243 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `ForgeMultipart` names [codechicken.microblock.MicroMaterialRegistry](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/microblock/MicroMaterialRegistry.java#L1) (7 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [TileMultipart.writeToNBT](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/multipart/TileMultipart.java#L759).
- Check preview rendering against `RendererSchematicChunk`. 3 source file(s) contain matching declarations, including [ISBRHPart.renderWorldBlock](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/multipart/ISBRHPart.java#L8).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockMultipart.getPickBlock](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/multipart/BlockMultipart.java#L277).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [ItemMicroPart.onItemUse](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/microblock/ItemMicroPart.java#L60).
- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinRenderGlobal.RenderGlobal](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/mixin/java/codechicken/multipart/mixin/early/MixinRenderGlobal.java#L22).
- [TileMultipart.writeToNBT](https://github.com/GTNewHorizons/ForgeMultipart/blob/3b181eb231a68ee578c53aa72b276ca5c8e29366/src/main/scala/codechicken/multipart/TileMultipart.java#L759) accesses `id`, `parts`. Compare those fields after capture, transform and restore.

### ForgeRelocation

Source [0bfcd5cd98](https://github.com/GTNewHorizons/ForgeRelocation/tree/0bfcd5cd98ba67a7cea3af3d727f4e2ed1a04f66) · 33 tracked Java, Scala or Kotlin files outside test/example directories.

- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [frame.renderWorldBlock](https://github.com/GTNewHorizons/ForgeRelocation/blob/0bfcd5cd98ba67a7cea3af3d727f4e2ed1a04f66/src/main/scala/mrtjp/mcframes/frame.scala#L171).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [IFramePlacement.onItemUse](https://github.com/GTNewHorizons/ForgeRelocation/blob/0bfcd5cd98ba67a7cea3af3d727f4e2ed1a04f66/src/main/scala/mrtjp/mcframes/api/IFramePlacement.java#L33).

### ForgeRelocationFMP

Source [ce9d90d663](https://github.com/GTNewHorizons/ForgeRelocationFMP/tree/ce9d90d663e6bf2ca245d9d559e4ce218a3af707) · 3 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [framefmp.onItemUse](https://github.com/GTNewHorizons/ForgeRelocationFMP/blob/ce9d90d663e6bf2ca245d9d559e4ce218a3af707/src/main/scala/mrtjp/relocationfmp/framefmp.scala#L155).

### Foundation

Source [81be9e9402](https://github.com/GTNewHorizons/Foundation/tree/81be9e940236560eb694b26d3d969beb7a8b992a) · 26 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinChunk.Chunk](https://github.com/GTNewHorizons/Foundation/blob/81be9e940236560eb694b26d3d969beb7a8b992a/src/main/java/com/gtnewhorizons/foundation/mixins/early/client/MixinChunk.java#L21).

### Freecam

Source [977b0f87cb](https://github.com/GTNewHorizons/Freecam/tree/977b0f87cbcf2ddb8ac30d961d944943935b43ce) · 23 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Freecam/blob/977b0f87cbcf2ddb8ac30d961d944943935b43ce/src/main/java/com/caedis/freecam/mixins/early/minecraft/MixinEntityRenderer.java#L24).

### Fullscreen-Windowed-Minecraft

Source [56ab7787a2](https://github.com/GTNewHorizons/Fullscreen-Windowed-Minecraft/tree/56ab7787a2b499789165fc3263234c8cb9cf2a90) · 12 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [FullscreenWindowed](https://github.com/GTNewHorizons/Fullscreen-Windowed-Minecraft/blob/56ab7787a2b499789165fc3263234c8cb9cf2a90/src/main/java/com/hancinworld/fw/FullscreenWindowed.java#L33). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Gadomancy

Source [d4256d85f9](https://github.com/GTNewHorizons/Gadomancy/tree/d4256d85f9989ad46034eddc4652f45ab13b0e72) · 291 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [makeo.gadomancy.common.blocks.BlockArcaneDropper](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/BlockArcaneDropper.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 4 source file(s) contain matching declarations, including [SynchronizedTileEntity.readFromNBT](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/tiles/SynchronizedTileEntity.java#L22).
- Check preview rendering against `RendererSchematicChunk`. 20 source file(s) contain matching declarations, including [BlockExtendedNodeJarRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/client/renderers/block/BlockExtendedNodeJarRenderer.java#L76).
- Check material picking against `BlockToItemStack`. 2 source file(s) contain matching declarations, including [BlockStickyJar.getPickBlock](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/BlockStickyJar.java#L141).
- Check printer placement against `SchematicPrinter`. 8 source file(s) contain matching declarations, including [BlockArcaneDropper.onBlockPlaced](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/BlockArcaneDropper.java#L74).
- [RenderTileEssentiaCompressor.renderTileEntityAt](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/client/renderers/tile/RenderTileEssentiaCompressor.java#L34) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [BlockStickyJar.getPickBlock](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/BlockStickyJar.java#L141) queries world state when selecting an item. Compare the materials list against the placed variant.
- [BlockStoneMachine.onBlockPlacedBy](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/BlockStoneMachine.java#L191) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileArcanePackager.readFromNBT](https://github.com/GTNewHorizons/Gadomancy/blob/d4256d85f9989ad46034eddc4652f45ab13b0e72/src/main/java/makeo/gadomancy/common/blocks/tiles/TileArcanePackager.java#L47) accesses `Items`, `Slot`. Compare those fields after capture, transform and restore.

### Galacticraft

Source [61f55046d5](https://github.com/GTNewHorizons/Galacticraft/tree/61f55046d555135cc75720115b955ab4a032f7af) · 914 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `GalacticraftVisualAdapter` names [micdoodle8.mods.galacticraft.api.vector.BlockVec3](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/api/vector/BlockVec3.java#L1) (23 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 58 source file(s) contain matching declarations, including [GCInvSaveData.readFromNBT](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/command/GCInvSaveData.java#L21).
- Check preview rendering against `RendererSchematicChunk`. 33 source file(s) contain matching declarations, including [BlockRendererBreathableAir.renderWorldBlock](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/client/render/block/BlockRendererBreathableAir.java#L21).
- Check material picking against `BlockToItemStack`. 21 source file(s) contain matching declarations, including [BlockBasic.getPickBlock](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/blocks/BlockBasic.java#L172).
- Check printer placement against `SchematicPrinter`. 38 source file(s) contain matching declarations, including [BlockAirLockFrame.onBlockPlacedBy](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/blocks/BlockAirLockFrame.java#L62).
- Check world or renderer transformation alongside the schematic world and render pipeline. 4 source file(s) contain matching declarations, including [EntityRendererMixin.EntityRenderer](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/mixins/early/minecraft/EntityRendererMixin.java#L21).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [EventHandlerGC.onPlayerClicked](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/event/EventHandlerGC.java#L221).
- [BlockBasic.getPickBlock](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/blocks/BlockBasic.java#L172) queries world state when selecting an item. Compare the materials list against the placed variant.
- [BlockCargoLoader.onBlockPlacedBy](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/blocks/BlockCargoLoader.java#L184) depends on placement orientation. Exercise the printer with a rotated schematic.
- [GCInvSaveData.readFromNBT](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/command/GCInvSaveData.java#L21) accesses `Slot`. Compare those fields after capture, transform and restore.
- [EventHandlerGC.onPlayerClicked](https://github.com/GTNewHorizons/Galacticraft/blob/61f55046d555135cc75720115b955ab4a032f7af/src/main/java/micdoodle8/mods/galacticraft/core/event/EventHandlerGC.java#L221) can cancel the interaction. Verify the printer on a server with the handler enabled.

### Galaxia

Source [08bdb4109b](https://github.com/GTNewHorizons/Galaxia/tree/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85) · 673 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [com.gtnewhorizons.galaxia.registry.block.GalaxiaMultiblockBase](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/block/GalaxiaMultiblockBase.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 24 source file(s) contain matching declarations, including [GalaxiaTeamData.writeToNBT](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/compat/teams/GalaxiaTeamData.java#L30).
- Check preview rendering against `RendererSchematicChunk`. 5 source file(s) contain matching declarations, including [RenderElevatorCable.renderTileEntityAt](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/client/render/elevator/RenderElevatorCable.java#L83).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockFumarole.getPickBlock](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/block/special/BlockFumarole.java#L71).
- Check printer placement against `SchematicPrinter`. 9 source file(s) contain matching declarations, including [BlockAirlockController.onBlockPlacedBy](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/block/special/BlockAirlockController.java#L88).
- Check world or renderer transformation alongside the schematic world and render pipeline. 4 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/mixin/MixinEntityRenderer.java#L18).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [AndroidControl.interact](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/android/AndroidControl.java#L864).
- [GalaxiaTeamData.writeToNBT](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/compat/teams/GalaxiaTeamData.java#L30) accesses `permActions`, `permRoles`, `mergedTeamIds`. Compare those fields after capture, transform and restore.
- [AndroidControl.interact](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/android/AndroidControl.java#L864) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [BlockBodyStation.onBlockPlacedBy](https://github.com/GTNewHorizons/Galaxia/blob/08bdb4109bad9ef92a13ddbcd80b8d04fe3efa85/src/main/java/com/gtnewhorizons/galaxia/registry/block/special/BlockBodyStation.java#L76) depends on placement orientation. Exercise the printer with a rotated schematic.

### gendustry

Source [23d31c7bfe](https://github.com/GTNewHorizons/gendustry/tree/23d31c7bfe53ceff470390ef5507de404d85c954) · 190 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [GeneSampleInfo.writeToNBT](https://github.com/GTNewHorizons/gendustry/blob/23d31c7bfe53ceff470390ef5507de404d85c954/src/main/scala/net/bdew/gendustry/forestry/GeneSampleInfo.scala#L23).
- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [HiveSpawnDebugger.onItemUse](https://github.com/GTNewHorizons/gendustry/blob/23d31c7bfe53ceff470390ef5507de404d85c954/src/main/scala/net/bdew/gendustry/items/HiveSpawnDebugger.scala#L63).

### Gravitation-Suite-Neo

Source [2d726ceb2c](https://github.com/GTNewHorizons/Gravitation-Suite-Neo/tree/2d726ceb2ca81b0b014c12338c05f21f8bb62f6e) · 65 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [EntityPlasmaBallMKII.writeToNBT](https://github.com/GTNewHorizons/Gravitation-Suite-Neo/blob/2d726ceb2ca81b0b014c12338c05f21f8bb62f6e/src/main/java/com/gtnewhorizons/gravisuiteneo/common/EntityPlasmaBallMKII.java#L108).
- [EntityPlasmaBallMKII.writeToNBT](https://github.com/GTNewHorizons/Gravitation-Suite-Neo/blob/2d726ceb2ca81b0b014c12338c05f21f8bb62f6e/src/main/java/com/gtnewhorizons/gravisuiteneo/common/EntityPlasmaBallMKII.java#L108) accesses `charge`. Compare those fields after capture, transform and restore.

### GregoryTweaksForCrafting

Source [12b3eeebe7](https://github.com/GTNewHorizons/GregoryTweaksForCrafting/tree/12b3eeebe7d4ac1a0fc6edaa2e079f3ea9a0c4bf) · 43 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [CraftingTweaks](https://github.com/GTNewHorizons/GregoryTweaksForCrafting/blob/12b3eeebe7d4ac1a0fc6edaa2e079f3ea9a0c4bf/src/main/java/net/blay09/mods/craftingtweaks/CraftingTweaks.java#L39). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GT5-Unofficial

Source [98c96af60b](https://github.com/GTNewHorizons/GT5-Unofficial/tree/98c96af60bf2897654af05d038c82b3b66c105a8) · 3355 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `MachineUpdateLock` names [gregtech.GTMod](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/gregtech/GTMod.java#L1) (14 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 60 source file(s) contain matching declarations, including [TileEntityHeatedWaterPump.writeToNBT](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/common/tileentities/classic/TileEntityHeatedWaterPump.java#L71).
- Check preview rendering against `RendererSchematicChunk`. 16 source file(s) contain matching declarations, including [RendererSwitchingColorFluid.renderWorldBlock](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/client/renderer/RendererSwitchingColorFluid.java#L99).
- Check printer placement against `SchematicPrinter`. 30 source file(s) contain matching declarations, including [BWTileEntityContainer.onBlockPlacedBy](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/common/blocks/BWTileEntityContainer.java#L103).
- Check world or renderer transformation alongside the schematic world and render pipeline. 4 source file(s) contain matching declarations, including [WorldMixin.World](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/mixin/java/gregtech/mixin/mixins/early/minecraft/WorldMixin.java#L13).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [GTProxy.onPlayerInteraction](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/gregtech/common/GTProxy.java#L1086).
- [RendererSwitchingColorFluid.renderWorldBlock](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/client/renderer/RendererSwitchingColorFluid.java#L99) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [BWTileEntityContainer.onBlockPlacedBy](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/common/blocks/BWTileEntityContainer.java#L103) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityHeatedWaterPump.writeToNBT](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/bartworks/common/tileentities/classic/TileEntityHeatedWaterPump.java#L71) accesses `ItemStack`, `FluidStack`, `fuel`, `maxfuel`. Compare those fields after capture, transform and restore.
- [GTProxy.onPlayerInteraction](https://github.com/GTNewHorizons/GT5-Unofficial/blob/98c96af60bf2897654af05d038c82b3b66c105a8/src/main/java/gregtech/common/GTProxy.java#L1086) can cancel the interaction. Verify the printer on a server with the handler enabled.

### GTNH-Credits

Source [2b8e5dcf17](https://github.com/GTNewHorizons/GTNH-Credits/tree/2b8e5dcf17ad7fd50a55179380bc506cc04d3867) · 172 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [GTNHCredits](https://github.com/GTNewHorizons/GTNH-Credits/blob/2b8e5dcf17ad7fd50a55179380bc506cc04d3867/src/main/java/net/noiraude/gtnhcredits/GTNHCredits.java#L10). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GTNH-TC-Wands

Source [0d7c8efad8](https://github.com/GTNewHorizons/GTNH-TC-Wands/tree/0d7c8efad864d91ac6443e9c1d443d48640b9070) · 18 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [TCWandsMod](https://github.com/GTNewHorizons/GTNH-TC-Wands/blob/0d7c8efad864d91ac6443e9c1d443d48640b9070/src/main/java/com/gtnewhorizons/tcwands/TCWandsMod.java#L12). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GTNH-Web-Map

Source [a989a30670](https://github.com/GTNewHorizons/GTNH-Web-Map/tree/a989a30670d8763b8975df6e1cd215333a6e1e75) · 342 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [DynmapMod](https://github.com/GTNewHorizons/GTNH-Web-Map/blob/a989a30670d8763b8975df6e1cd215333a6e1e75/src/main/java/org/dynmap/forge/DynmapMod.java#L34). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GTNH-Web-Map-Client

Source [ff9ec54231](https://github.com/GTNewHorizons/GTNH-Web-Map-Client/tree/ff9ec54231781e4f1caa5b32481ee81a3106f640) · 17 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [GwmClientMod](https://github.com/GTNewHorizons/GTNH-Web-Map-Client/blob/ff9ec54231781e4f1caa5b32481ee81a3106f640/src/main/java/com/gtnewhorizons/gwmclient/GwmClientMod.java#L17). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GTNHExtLib

Source [f7c13a83a7](https://github.com/GTNewHorizons/GTNHExtLib/tree/f7c13a83a747f876440c63fa0736bc09af84a219) · 3 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [GTNHExtLib](https://github.com/GTNewHorizons/GTNHExtLib/blob/f7c13a83a747f876440c63fa0736bc09af84a219/src/main/java/com/gtnewhorizons/gtnhextlib/GTNHExtLib.java#L5). Indirect effects from dependencies, injection, configuration and server rules remain open.

### GTNHLib

Source [fcc5c9b444](https://github.com/GTNewHorizons/GTNHLib/tree/fcc5c9b444359fb2ffcb6c4d86d4458c46118220) · 529 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [MixinEnhancedInfusionRecipe.writeToNBT](https://github.com/GTNewHorizons/GTNHLib/blob/fcc5c9b444359fb2ffcb6c4d86d4458c46118220/src/main/java/com/gtnewhorizon/gtnhlib/mixins/late/MixinEnhancedInfusionRecipe.java#L81).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [ModelISBRH.renderWorldBlock](https://github.com/GTNewHorizons/GTNHLib/blob/fcc5c9b444359fb2ffcb6c4d86d4458c46118220/src/main/java/com/gtnewhorizon/gtnhlib/client/model/ModelISBRH.java#L81).
- Check world or renderer transformation alongside the schematic world and render pipeline. 10 source file(s) contain matching declarations, including [EntityRendererAccessor.EntityRenderer](https://github.com/GTNewHorizons/GTNHLib/blob/fcc5c9b444359fb2ffcb6c4d86d4458c46118220/src/main/java/com/gtnewhorizon/gtnhlib/mixins/early/EntityRendererAccessor.java#L8).
- [MixinEnhancedInfusionRecipe.writeToNBT](https://github.com/GTNewHorizons/GTNHLib/blob/fcc5c9b444359fb2ffcb6c4d86d4458c46118220/src/main/java/com/gtnewhorizon/gtnhlib/mixins/late/MixinEnhancedInfusionRecipe.java#L81) accesses `input`, `output`, `nullOutput`, `strict`. Compare those fields after capture, transform and restore.

### Guide-API

Source [877662ac37](https://github.com/GTNewHorizons/Guide-API/tree/877662ac37b76ec0bf57169ab51f0c0c8728cea5) · 75 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemGuideBook.onItemUse](https://github.com/GTNewHorizons/Guide-API/blob/877662ac37b76ec0bf57169ab51f0c0c8728cea5/src/main/java/amerifrance/guideapi/items/ItemGuideBook.java#L58).

### GuideNH

Source [8bdffd5e94](https://github.com/GTNewHorizons/GuideNH/tree/8bdffd5e94bed3c089d1fcadbe8706e3bb789643) · 1503 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [GuidebookScenePreviewPlayerEntity.readFromNBT](https://github.com/GTNewHorizons/GuideNH/blob/8bdffd5e94bed3c089d1fcadbe8706e3bb789643/src/main/java/com/hfstudio/guidenh/guide/internal/scene/GuidebookScenePreviewPlayerEntity.java#L49).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [ForgeMultipartHelpers.renderWorldBlock](https://github.com/GTNewHorizons/GuideNH/blob/8bdffd5e94bed3c089d1fcadbe8706e3bb789643/src/main/java/com/hfstudio/guidenh/integration/forgemultipart/ForgeMultipartHelpers.java#L120).
- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinTessellatorSceneExportCapture.Tessellator](https://github.com/GTNewHorizons/GuideNH/blob/8bdffd5e94bed3c089d1fcadbe8706e3bb789643/src/main/java/com/hfstudio/guidenh/mixins/early/minecraft/MixinTessellatorSceneExportCapture.java#L16).
- [GuidebookScenePreviewPlayerEntity.readFromNBT](https://github.com/GTNewHorizons/GuideNH/blob/8bdffd5e94bed3c089d1fcadbe8706e3bb789643/src/main/java/com/hfstudio/guidenh/guide/internal/scene/GuidebookScenePreviewPlayerEntity.java#L49) accesses `Pos`, `Motion`, `Rotation`, `FallDistance`. Compare those fields after capture, transform and restore.

### Hardcore-Ender-Expansion

Source [b1ca4b5108](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/tree/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d) · 629 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [chylex.hee.block.BlockEndermanHead](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/block/BlockEndermanHead.java#L1) (4 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 18 source file(s) contain matching declarations, including [DragonAchievementManager.readFromNBT](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/entity/boss/dragon/managers/DragonAchievementManager.java#L93).
- Check preview rendering against `RendererSchematicChunk`. 13 source file(s) contain matching declarations, including [RenderBlockCrossedDecoration.renderWorldBlock](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/render/block/RenderBlockCrossedDecoration.java#L23).
- Check material picking against `BlockToItemStack`. 4 source file(s) contain matching declarations, including [BlockBiomeIslandCore.getPickBlock](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/block/BlockBiomeIslandCore.java#L48).
- Check printer placement against `SchematicPrinter`. 15 source file(s) contain matching declarations, including [BlockEndermanHead.onBlockPlacedBy](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/block/BlockEndermanHead.java#L75).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [TempleEvents.onPlayerInteract](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/mechanics/misc/TempleEvents.java#L181).
- [BlockEndermanHead.onBlockPlacedBy](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/block/BlockEndermanHead.java#L75) depends on placement orientation. Exercise the printer with a rotated schematic.
- [BlockEssenceAltar.getPickBlock](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/block/BlockEssenceAltar.java#L81) queries world state when selecting an item. Compare the materials list against the placed variant.
- [DragonAchievementManager.readFromNBT](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/b1ca4b5108db8d7163f8d25490c3ae4a31918c1d/src/main/java/chylex/hee/entity/boss/dragon/managers/DragonAchievementManager.java#L93) accesses `___timer`. Compare those fields after capture, transform and restore.

### HelpFixer

Source [bd0603fd84](https://github.com/GTNewHorizons/HelpFixer/tree/bd0603fd84fe7e2a017b9919c28437ff3e9a2b69) · 1 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [HelpFixer](https://github.com/GTNewHorizons/HelpFixer/blob/bd0603fd84fe7e2a017b9919c28437ff3e9a2b69/src/main/java/com/matthewprenger/helpfixer/HelpFixer.java#L23). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Hodgepodge

Source [ec66e0b0da](https://github.com/GTNewHorizons/Hodgepodge/tree/ec66e0b0da39c8beca641022b8db4283f50e8f57) · 767 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [MixinTileAnchorPassive.onBlockPlacedBy](https://github.com/GTNewHorizons/Hodgepodge/blob/ec66e0b0da39c8beca641022b8db4283f50e8f57/src/main/java/com/mitchej123/hodgepodge/mixins/late/railcraft/MixinTileAnchorPassive.java#L17).
- Check world or renderer transformation alongside the schematic world and render pipeline. 37 source file(s) contain matching declarations, including [MixinRenderGlobal.RenderGlobal](https://github.com/GTNewHorizons/Hodgepodge/blob/ec66e0b0da39c8beca641022b8db4283f50e8f57/src/main/java/com/mitchej123/hodgepodge/mixins/early/debug/MixinRenderGlobal.java#L29).

### HoloInventory

Source [96935151c0](https://github.com/GTNewHorizons/HoloInventory/tree/96935151c0db960b7e2c26d7e067c09cb7ad7631) · 35 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [Coord.writeToNBT](https://github.com/GTNewHorizons/HoloInventory/blob/96935151c0db960b7e2c26d7e067c09cb7ad7631/src/main/java/net/dries007/holoInventory/util/Coord.java#L55).

### Horizon-QA

Source [e05abc05be](https://github.com/GTNewHorizons/Horizon-QA/tree/e05abc05be88e9072c7ed298e4ae81c158213515) · 162 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemHorizonWand.onItemUse](https://github.com/GTNewHorizons/Horizon-QA/blob/e05abc05be88e9072c7ed298e4ae81c158213515/src/main/java/com/gtnewhorizons/horizonqa/item/ItemHorizonWand.java#L60).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [SelectionBoxRenderer.onPlayerInteract](https://github.com/GTNewHorizons/Horizon-QA/blob/e05abc05be88e9072c7ed298e4ae81c158213515/src/main/java/com/gtnewhorizons/horizonqa/visual/SelectionBoxRenderer.java#L14).
- [SelectionBoxRenderer.onPlayerInteract](https://github.com/GTNewHorizons/Horizon-QA/blob/e05abc05be88e9072c7ed298e4ae81c158213515/src/main/java/com/gtnewhorizons/horizonqa/visual/SelectionBoxRenderer.java#L14) can cancel the interaction. Verify the printer on a server with the handler enabled.

### HydroEnergy

Source [62ccae6f38](https://github.com/GTNewHorizons/HydroEnergy/tree/62ccae6f384f55792dfd1154a26624798a9297a9) · 61 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [HEServer.readFromNBT](https://github.com/GTNewHorizons/HydroEnergy/blob/62ccae6f384f55792dfd1154a26624798a9297a9/src/main/java/com/sinthoras/hydroenergy/server/HEServer.java#L49).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [HEWaterRenderer.renderWorldBlock](https://github.com/GTNewHorizons/HydroEnergy/blob/62ccae6f384f55792dfd1154a26624798a9297a9/src/main/java/com/sinthoras/hydroenergy/client/renderer/HEWaterRenderer.java#L43).
- Check world or renderer transformation alongside the schematic world and render pipeline. 5 source file(s) contain matching declarations, including [ChunkClientMixin.Chunk](https://github.com/GTNewHorizons/HydroEnergy/blob/62ccae6f384f55792dfd1154a26624798a9297a9/src/main/java/com/sinthoras/hydroenergy/mixins/early/ChunkClientMixin.java#L12).

### IFU

Source [48ddad516b](https://github.com/GTNewHorizons/IFU/tree/48ddad516bb891edb4a229c255614651e70b0204) · 19 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [InventoryOreFinder.writeToNBT](https://github.com/GTNewHorizons/IFU/blob/48ddad516bb891edb4a229c255614651e70b0204/src/main/java/com/encraft/dz/inventory/InventoryOreFinder.java#L110).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemOreFinderTool.onItemUse](https://github.com/GTNewHorizons/IFU/blob/48ddad516bb891edb4a229c255614651e70b0204/src/main/java/com/encraft/dz/items/ItemOreFinderTool.java#L193).
- [InventoryOreFinder.writeToNBT](https://github.com/GTNewHorizons/IFU/blob/48ddad516bb891edb4a229c255614651e70b0204/src/main/java/com/encraft/dz/inventory/InventoryOreFinder.java#L110) accesses `Slot`. Compare those fields after capture, transform and restore.

### IguanaTweaksTConstruct

Source [3e94b249d4](https://github.com/GTNewHorizons/IguanaTweaksTConstruct/tree/3e94b249d40a9cbbde90436d203ec206cc3cb663) · 101 tracked Java, Scala or Kotlin files outside test/example directories.

- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [IguanaTileEntitySkullRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/IguanaTweaksTConstruct/blob/3e94b249d40a9cbbde90436d203ec206cc3cb663/src/main/java/iguanaman/iguanatweakstconstruct/mobheads/renderers/IguanaTileEntitySkullRenderer.java#L56).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [IguanaSkull.onItemUse](https://github.com/GTNewHorizons/IguanaTweaksTConstruct/blob/3e94b249d40a9cbbde90436d203ec206cc3cb663/src/main/java/iguanaman/iguanatweakstconstruct/mobheads/items/IguanaSkull.java#L118).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ClayBucketHandler.onRightClickBlock](https://github.com/GTNewHorizons/IguanaTweaksTConstruct/blob/3e94b249d40a9cbbde90436d203ec206cc3cb663/src/main/java/iguanaman/iguanatweakstconstruct/claybuckets/ClayBucketHandler.java#L45).
- [IguanaSkull.onItemUse](https://github.com/GTNewHorizons/IguanaTweaksTConstruct/blob/3e94b249d40a9cbbde90436d203ec206cc3cb663/src/main/java/iguanaman/iguanatweakstconstruct/mobheads/items/IguanaSkull.java#L118) depends on placement orientation. Exercise the printer with a rotated schematic.

### Infernal-Mobs

Source [b52b7bd473](https://github.com/GTNewHorizons/Infernal-Mobs/tree/b52b7bd47311f640bca30ec117a4c32d26641c64) · 44 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [InfernalMobsCore](https://github.com/GTNewHorizons/Infernal-Mobs/blob/b52b7bd47311f640bca30ec117a4c32d26641c64/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L93). Indirect effects from dependencies, injection, configuration and server rules remain open.

### InfoHUD

Source [f29c35cee0](https://github.com/GTNewHorizons/InfoHUD/tree/f29c35cee079233413b78e8680473551a0d3b1bd) · 61 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [InfoHUD](https://github.com/GTNewHorizons/InfoHUD/blob/f29c35cee079233413b78e8680473551a0d3b1bd/src/main/java/com/gtnewhorizons/infohud/InfoHUD.java#L13). Indirect effects from dependencies, injection, configuration and server rules remain open.

### InGame-Info-XML

Source [b555c63084](https://github.com/GTNewHorizons/InGame-Info-XML/tree/b555c63084d17036b97cf106b6d32b9ca24353db) · 73 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [InGameInfoXML](https://github.com/GTNewHorizons/InGame-Info-XML/blob/b555c63084d17036b97cf106b6d32b9ca24353db/src/main/java/com/github/lunatrius/ingameinfo/InGameInfoXML.java#L21). Indirect effects from dependencies, injection, configuration and server rules remain open.

### InventoryBogoSorter

Source [1e75ea6971](https://github.com/GTNewHorizons/InventoryBogoSorter/tree/1e75ea6971c644e8ca048c12aa38b7c2e701fb42) · 148 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `InventoryClickCompat` names [com.cleanroommc.bogosorter.ShortcutHandler](https://github.com/GTNewHorizons/InventoryBogoSorter/blob/1e75ea6971c644e8ca048c12aa38b7c2e701fb42/src/main/java/com/cleanroommc/bogosorter/ShortcutHandler.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.

### ironchest

Source [b3c67de096](https://github.com/GTNewHorizons/ironchest/tree/b3c67de09677979c491be2caeaea8ce31c327ea5) · 29 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `VisualAdapters` names [cpw.mods.ironchest.TileEntityCrystalChest](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/TileEntityCrystalChest.java#L1) (2 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileEntityIronChest.readFromNBT](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/TileEntityIronChest.java#L176).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [TileEntityIronChestRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/client/TileEntityIronChestRenderer.java#L181).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockIronChest.onBlockPlacedBy](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/BlockIronChest.java#L141).
- [BlockIronChest.onBlockPlacedBy](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/BlockIronChest.java#L141) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityIronChest.readFromNBT](https://github.com/GTNewHorizons/ironchest/blob/b3c67de09677979c491be2caeaea8ce31c327ea5/src/main/java/cpw/mods/ironchest/TileEntityIronChest.java#L176) accesses `Items`, `Slot`, `facing`. Compare those fields after capture, transform and restore.

### IronChestMinecarts

Source [7d8dd0389e](https://github.com/GTNewHorizons/IronChestMinecarts/tree/7d8dd0389ed639ff3179901e754a0b1793437bde) · 15 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemMinecartIronChest.onItemUse](https://github.com/GTNewHorizons/IronChestMinecarts/blob/7d8dd0389ed639ff3179901e754a0b1793437bde/src/main/java/ganymedes01/ironchestminecarts/minecarts/ItemMinecartIronChest.java#L23).

### IronTankMinecarts

Source [8baf79a9bd](https://github.com/GTNewHorizons/IronTankMinecarts/tree/8baf79a9bd968fa0005e6d8686624c7d141d3209) · 16 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemMinecartIronTank.onItemUse](https://github.com/GTNewHorizons/IronTankMinecarts/blob/8baf79a9bd968fa0005e6d8686624c7d141d3209/src/main/java/secondderivative/irontankminecarts/minecarts/ItemMinecartIronTank.java#L21).

### Irontanks

Source [4f0ff12f01](https://github.com/GTNewHorizons/Irontanks/tree/4f0ff12f01689ca2dd3bca7c0d7bf0ce01f27b3f) · 19 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileIronTank.readFromNBT](https://github.com/GTNewHorizons/Irontanks/blob/4f0ff12f01689ca2dd3bca7c0d7bf0ce01f27b3f/src/main/java/com/indemnity83/irontank/tile/TileIronTank.java#L24).
- [TileIronTank.readFromNBT](https://github.com/GTNewHorizons/Irontanks/blob/4f0ff12f01689ca2dd3bca7c0d7bf0ce01f27b3f/src/main/java/com/indemnity83/irontank/tile/TileIronTank.java#L24) accesses `type`. Compare those fields after capture, transform and restore.

### Jabba

Source [14c9a4351e](https://github.com/GTNewHorizons/Jabba/tree/14c9a4351e240b57b99862031820e5ae186695d4) · 55 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/common/blocks/TileEntityBarrel.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [BSpaceStorageHandler.writeToNBT](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/bspace/BSpaceStorageHandler.java#L235).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [BlockBarrelRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/client/render/BlockBarrelRenderer.java#L50).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockBarrel.onBlockPlacedBy](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/common/blocks/BlockBarrel.java#L82).
- [BSpaceStorageHandler.writeToNBT](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/bspace/BSpaceStorageHandler.java#L235) accesses `version`, `maxBarrelID`, `barrelCoords`, `storages`. Compare those fields after capture, transform and restore.
- [BlockBarrelRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Jabba/blob/14c9a4351e240b57b99862031820e5ae186695d4/src/main/java/mcp/mobius/betterbarrels/client/render/BlockBarrelRenderer.java#L50) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### JarJar

Source [8b02e4ed5c](https://github.com/GTNewHorizons/JarJar/tree/8b02e4ed5c0a5368c5c1e795febb604e7fe8a850) · 35 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [JarJar](https://github.com/GTNewHorizons/JarJar/blob/8b02e4ed5c0a5368c5c1e795febb604e7fe8a850/src/main/java/com/mitchej123/jarjar/JarJar.java#L12). Indirect effects from dependencies, injection, configuration and server rules remain open.

### journeymap-legacy

Source [2e194152cb](https://github.com/GTNewHorizons/journeymap-legacy/tree/2e194152cbcb3f376d09becd203d4c53daf7e9ab) · 298 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [NBTWorldSaveDataHandler.readFromNBT](https://github.com/GTNewHorizons/journeymap-legacy/blob/2e194152cbcb3f376d09becd203d4c53daf7e9ab/src/main/java/journeymap/server/nbt/NBTWorldSaveDataHandler.java#L27).

### JustEnoughCalculation

Source [72f9da2e37](https://github.com/GTNewHorizons/JustEnoughCalculation/tree/72f9da2e371f68b7323cc2dfc9a15c5900c6169c) · 105 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [JustEnoughCalculation](https://github.com/GTNewHorizons/JustEnoughCalculation/blob/72f9da2e371f68b7323cc2dfc9a15c5900c6169c/src/main/java/me/towdium/jecalculation/JustEnoughCalculation.java#L25). Indirect effects from dependencies, injection, configuration and server rules remain open.

### LiquidLogged

Source [007b33b431](https://github.com/GTNewHorizons/LiquidLogged/tree/007b33b43121cf2ca26f453e47c4ff6b9b73f7ee) · 63 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `LiquidLogged` names [com.gtnewhorizons.liquidlogged.api.LiquidLoggedAPI](https://github.com/GTNewHorizons/LiquidLogged/blob/007b33b43121cf2ca26f453e47c4ff6b9b73f7ee/src/main/java/com/gtnewhorizons/liquidlogged/api/LiquidLoggedAPI.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check world or renderer transformation alongside the schematic world and render pipeline. 8 source file(s) contain matching declarations, including [AccessorChunk.Chunk](https://github.com/GTNewHorizons/LiquidLogged/blob/007b33b43121cf2ca26f453e47c4ff6b9b73f7ee/src/main/java/com/gtnewhorizons/liquidlogged/mixins/early/minecraft/AccessorChunk.java#L9).
- The public static getFluid(IBlockAccess, int, int, int) and setFluid(World, int, int, int, Fluid) declarations match the Plus lookup. Plus limits stored fluid capture to Y 0–255.

### LittleTiles

Source [00f5c2613c](https://github.com/GTNewHorizons/LittleTiles/tree/00f5c2613caaa875bea997a4ad658de77a23b6f1) · 294 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 18 source file(s) contain matching declarations, including [SubContainer.writeToNBT](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/creativecore/common/container/SubContainer.java#L143).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [SpecialBlockTilesRenderer.renderWorldBlock](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/littletiles/client/render/SpecialBlockTilesRenderer.java#L45).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockTile.getPickBlock](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/littletiles/common/blocks/BlockTile.java#L425).
- Check printer placement against `SchematicPrinter`. 11 source file(s) contain matching declarations, including [ItemBlockTiles.onItemUse](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/littletiles/common/items/ItemBlockTiles.java#L100).
- [SlotControl.writeToNBT](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/creativecore/common/container/slot/SlotControl.java#L49) accesses `drag`. Compare those fields after capture, transform and restore.
- [SpecialBlockTilesRenderer.renderWorldBlock](https://github.com/GTNewHorizons/LittleTiles/blob/00f5c2613caaa875bea997a4ad658de77a23b6f1/src/main/java/com/creativemd/littletiles/client/render/SpecialBlockTilesRenderer.java#L45) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### LogisticsPipes

Source [89f527a52d](https://github.com/GTNewHorizons/LogisticsPipes/tree/89f527a52d21c93044126c51bc894c16d9f2c77d) · 1063 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `LogisticsPipesVisualAdapter` names [logisticspipes.network.LPDataInputStream](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/network/LPDataInputStream.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 81 source file(s) contain matching declarations, including [LogisticsSecurityTileEntity.readFromNBT](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/blocks/LogisticsSecurityTileEntity.java#L152).
- Check preview rendering against `RendererSchematicChunk`. 6 source file(s) contain matching declarations, including [LogisticsPipeWorldRenderer.renderWorldBlock](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/renderer/LogisticsPipeWorldRenderer.java#L208).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [LogisticsBlockGenericPipe.getPickBlock](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/pipes/basic/LogisticsBlockGenericPipe.java#L717).
- Check printer placement against `SchematicPrinter`. 8 source file(s) contain matching declarations, including [LogisticsSolidBlock.onBlockPlacedBy](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/blocks/LogisticsSolidBlock.java#L91).
- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinWorld.World](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/mixins/early/minecraft/MixinWorld.java#L14).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [LogisticsEventListener.onPlayerInteract](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/LogisticsEventListener.java#L97).
- [LogisticsEventListener.onPlayerInteract](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/LogisticsEventListener.java#L97) can cancel the interaction. Verify the printer on a server with the handler enabled.
- [LogisticsSecurityTileEntity.readFromNBT](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/blocks/LogisticsSecurityTileEntity.java#L152) accesses `UUID`, `allowCC`, `allowAutoDestroy`, `settings`. Compare those fields after capture, transform and restore.
- [LogisticsPipeWorldRenderer.renderWorldBlock](https://github.com/GTNewHorizons/LogisticsPipes/blob/89f527a52d21c93044126c51bc894c16d9f2c77d/src/main/java/logisticspipes/renderer/LogisticsPipeWorldRenderer.java#L208) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### LootGames

Source [e657b5fe45](https://github.com/GTNewHorizons/LootGames/tree/e657b5fe45b7f41e84358c9d993f49bb373c6e16) · 163 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [eu.usrv.legacylootgames.gol.tiles.LegacyGameOfLightTile](https://github.com/GTNewHorizons/LootGames/blob/e657b5fe45b7f41e84358c9d993f49bb373c6e16/src/main/java/eu/usrv/legacylootgames/gol/tiles/LegacyGameOfLightTile.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [LegacyGameOfLightTile.readFromNBT](https://github.com/GTNewHorizons/LootGames/blob/e657b5fe45b7f41e84358c9d993f49bb373c6e16/src/main/java/eu/usrv/legacylootgames/gol/tiles/LegacyGameOfLightTile.java#L164).
- Check preview rendering against `RendererSchematicChunk`. 3 source file(s) contain matching declarations, including [SudokuRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/LootGames/blob/e657b5fe45b7f41e84358c9d993f49bb373c6e16/src/main/java/ru/timeconqueror/lootgames/client/render/SudokuRenderer.java#L67).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [CommonEventHandler.onPlayerInteract](https://github.com/GTNewHorizons/LootGames/blob/e657b5fe45b7f41e84358c9d993f49bb373c6e16/src/main/java/ru/timeconqueror/timecore/api/common/CommonEventHandler.java#L13).

### LunatriusCore

Source [a4f311761f](https://github.com/GTNewHorizons/LunatriusCore/tree/a4f311761f219f0de3d2cdb73abf18419a02e3ca) · 28 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [LunatriusCore](https://github.com/GTNewHorizons/LunatriusCore/blob/a4f311761f219f0de3d2cdb73abf18419a02e3ca/src/main/java/com/github/lunatrius/core/LunatriusCore.java#L18). Indirect effects from dependencies, injection, configuration and server rules remain open.

### lwjgl3ify

Source [b2545808e6](https://github.com/GTNewHorizons/lwjgl3ify/tree/b2545808e6e6a50f8b7f84a0388532a9d4076f61) · 676 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinEntityRenderer.EntityRenderer](https://github.com/GTNewHorizons/lwjgl3ify/blob/b2545808e6e6a50f8b7f84a0388532a9d4076f61/src/main/java/me/eigenraven/lwjgl3ify/mixins/early/game/MixinEntityRenderer.java#L16).

### MagicBees

Source [d635a7a406](https://github.com/GTNewHorizons/MagicBees/tree/d635a7a4069cea443234b36c3872953dc23f5e5c) · 170 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 10 source file(s) contain matching declarations, including [MagicApiaryInventory.writeToNBT](https://github.com/GTNewHorizons/MagicBees/blob/d635a7a4069cea443234b36c3872953dc23f5e5c/src/main/java/magicbees/bees/MagicApiaryInventory.java#L147).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [RendererEffectJar.renderTileEntityAt](https://github.com/GTNewHorizons/MagicBees/blob/d635a7a4069cea443234b36c3872953dc23f5e5c/src/main/java/magicbees/client/render/RendererEffectJar.java#L19).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [BlockHive.getPickBlock](https://github.com/GTNewHorizons/MagicBees/blob/d635a7a4069cea443234b36c3872953dc23f5e5c/src/main/java/magicbees/block/BlockHive.java#L85).
- [MagicApiaryInventory.writeToNBT](https://github.com/GTNewHorizons/MagicBees/blob/d635a7a4069cea443234b36c3872953dc23f5e5c/src/main/java/magicbees/bees/MagicApiaryInventory.java#L147) accesses `Slot`, `Items`. Compare those fields after capture, transform and restore.

### MalisisDoors

Source [084e8818be](https://github.com/GTNewHorizons/MalisisDoors/tree/084e8818be08e30d6b4b23e10b7f1607ff32d707) · 259 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [net.malisis.doors.block.BlockMixer](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/block/BlockMixer.java#L1) (6 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 15 source file(s) contain matching declarations, including [MalisisInventory.readFromNBT](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/core/inventory/MalisisInventory.java#L386).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [MalisisRenderer.renderWorldBlock](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/core/renderer/MalisisRenderer.java#L257).
- Check material picking against `BlockToItemStack`. 4 source file(s) contain matching declarations, including [MixedBlock.getPickBlock](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/block/MixedBlock.java#L89).
- Check printer placement against `SchematicPrinter`. 16 source file(s) contain matching declarations, including [BlockMixer.onBlockPlacedBy](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/block/BlockMixer.java#L69).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [DoorEventHandlerClient.onClientInteract](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/event/DoorEventHandlerClient.java#L52).
- [MalisisInventory.readFromNBT](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/core/inventory/MalisisInventory.java#L386) accesses `Items`, `Slot`. Compare those fields after capture, transform and restore.
- [MalisisRenderer.renderWorldBlock](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/core/renderer/MalisisRenderer.java#L257) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [BlockMixer.onBlockPlacedBy](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/block/BlockMixer.java#L69) depends on placement orientation. Exercise the printer with a rotated schematic.
- [MixedBlock.getPickBlock](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/block/MixedBlock.java#L89) queries world state when selecting an item. Compare the materials list against the placed variant.
- [DoorEventHandlerClient.onClientInteract](https://github.com/GTNewHorizons/MalisisDoors/blob/084e8818be08e30d6b4b23e10b7f1607ff32d707/src/main/java/net/malisis/doors/event/DoorEventHandlerClient.java#L52) can cancel the interaction. Verify the printer on a server with the handler enabled.

### Mantle

Source [b5157be91e](https://github.com/GTNewHorizons/Mantle/tree/b5157be91e303cd0d7022078ea27bace20eb1eb5) · 89 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CoordinateLinks` names [mantle.blocks.abstracts.MultiServantLogic](https://github.com/GTNewHorizons/Mantle/blob/b5157be91e303cd0d7022078ea27bace20eb1eb5/src/main/java/mantle/blocks/abstracts/MultiServantLogic.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 3 source file(s) contain matching declarations, including [ExpandableInventoryLogic.readFromNBT](https://github.com/GTNewHorizons/Mantle/blob/b5157be91e303cd0d7022078ea27bace20eb1eb5/src/main/java/mantle/blocks/abstracts/ExpandableInventoryLogic.java#L96).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [InventoryBlock.onBlockPlaced](https://github.com/GTNewHorizons/Mantle/blob/b5157be91e303cd0d7022078ea27bace20eb1eb5/src/main/java/mantle/blocks/abstracts/InventoryBlock.java#L114).
- [ExpandableInventoryLogic.readFromNBT](https://github.com/GTNewHorizons/Mantle/blob/b5157be91e303cd0d7022078ea27bace20eb1eb5/src/main/java/mantle/blocks/abstracts/ExpandableInventoryLogic.java#L96) accesses `InvName`, `Items`, `Slot`. Compare those fields after capture, transform and restore.

### MaterialLib

Source [21f2e4f33c](https://github.com/GTNewHorizons/MaterialLib/tree/21f2e4f33cc3e5e358c69771cda6db30410108c6) · 90 tracked Java, Scala or Kotlin files outside test/example directories.

- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [ShapeBlockRenderingHandler.renderWorldBlock](https://github.com/GTNewHorizons/MaterialLib/blob/21f2e4f33cc3e5e358c69771cda6db30410108c6/src/main/java/com/ruling_0/materiallib/api/ShapeBlockRenderingHandler.java#L122).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [ItemUseCallback.onItemUse](https://github.com/GTNewHorizons/MaterialLib/blob/21f2e4f33cc3e5e358c69771cda6db30410108c6/src/main/java/com/ruling_0/materiallib/api/ItemUseCallback.java#L13).

### MatterManipulator

Source [9b3c113ff7](https://github.com/GTNewHorizons/MatterManipulator/tree/9b3c113ff7a418ce2d3ad5d7c728c26bdaf3949f) · 133 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinWorldDropCapturing.World](https://github.com/GTNewHorizons/MatterManipulator/blob/9b3c113ff7a418ce2d3ad5d7c728c26bdaf3949f/src/mixin/java/com/recursive_pineapple/matter_manipulator/mixin/mixins/early/MixinWorldDropCapturing.java#L19).

### Minecraft-Backpack-Mod

Source [0430295ad9](https://github.com/GTNewHorizons/Minecraft-Backpack-Mod/tree/0430295ad9a8dbe92187f910316e62e1ba273f88) · 100 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 7 source file(s) contain matching declarations, including [ISaveableInventory.readFromNBT](https://github.com/GTNewHorizons/Minecraft-Backpack-Mod/blob/0430295ad9a8dbe92187f910316e62e1ba273f88/src/main/java/de/eydamos/backpack/inventory/ISaveableInventory.java#L7).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemBackpackBase.onItemUse](https://github.com/GTNewHorizons/Minecraft-Backpack-Mod/blob/0430295ad9a8dbe92187f910316e62e1ba273f88/src/main/java/de/eydamos/backpack/item/ItemBackpackBase.java#L82).

### minecraft-prometheus-exporter

Source [191e068da0](https://github.com/GTNewHorizons/minecraft-prometheus-exporter/tree/191e068da0311cfe58f1c0adf60ec4df6e672d8a) · 16 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [PrometheusExporterMod](https://github.com/GTNewHorizons/minecraft-prometheus-exporter/blob/191e068da0311cfe58f1c0adf60ec4df6e672d8a/src/main/java/com/github/cpburnz/minecraft_prometheus_exporter/PrometheusExporterMod.java#L42). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Minetweaker-Gregtech-5-Addon

Source [032eb9f5b0](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/tree/032eb9f5b0c2cfdde41b2a130540c4cbceb62483) · 67 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [GTTweaker](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/032eb9f5b0c2cfdde41b2a130540c4cbceb62483/src/main/java/gttweaker/GTTweaker.java#L74). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Mobs-Info

Source [21174e0cd0](https://github.com/GTNewHorizons/Mobs-Info/tree/21174e0cd0310843706de770a7854737b5b32b84) · 128 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [PlayerDataManager.readFromNBT](https://github.com/GTNewHorizons/Mobs-Info/blob/21174e0cd0310843706de770a7854737b5b32b84/src/main/java/com/kuba6000/mobsinfo/savedata/PlayerDataManager.java#L55).
- [PlayerDataManager.readFromNBT](https://github.com/GTNewHorizons/Mobs-Info/blob/21174e0cd0310843706de770a7854737b5b32b84/src/main/java/com/kuba6000/mobsinfo/savedata/PlayerDataManager.java#L55) accesses `players`, `uuid`, `data`. Compare those fields after capture, transform and restore.

### ModernMarkings

Source [cfba350bbd](https://github.com/GTNewHorizons/ModernMarkings/tree/cfba350bbd200a9e23b8a8bd9e18eb33d67e801b) · 13 tracked Java, Scala or Kotlin files outside test/example directories.

- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [MarkingFloorRenderer.renderWorldBlock](https://github.com/GTNewHorizons/ModernMarkings/blob/cfba350bbd200a9e23b8a8bd9e18eb33d67e801b/src/main/java/modernmarkings/renderer/MarkingFloorRenderer.java#L56).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [MarkingFloor.onBlockPlacedBy](https://github.com/GTNewHorizons/ModernMarkings/blob/cfba350bbd200a9e23b8a8bd9e18eb33d67e801b/src/main/java/modernmarkings/blocks/MarkingFloor.java#L41).
- [MarkingFloor.onBlockPlacedBy](https://github.com/GTNewHorizons/ModernMarkings/blob/cfba350bbd200a9e23b8a8bd9e18eb33d67e801b/src/main/java/modernmarkings/blocks/MarkingFloor.java#L41) depends on placement orientation. Exercise the printer with a rotated schematic.

### ModTweaker

Source [3665efa137](https://github.com/GTNewHorizons/ModTweaker/tree/3665efa137504aef3f68ed129aa07703cca6075b) · 237 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ModTweaker2](https://github.com/GTNewHorizons/ModTweaker/blob/3665efa137504aef3f68ed129aa07703cca6075b/src/main/java/modtweaker2/ModTweaker2.java#L46). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ModularUI

Source [8d2a2c47a9](https://github.com/GTNewHorizons/ModularUI/tree/8d2a2c47a9e3105ead800d1d1d34ab5a1ff1cfbb) · 142 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ModularUI](https://github.com/GTNewHorizons/ModularUI/blob/8d2a2c47a9e3105ead800d1d1d34ab5a1ff1cfbb/src/main/java/com/gtnewhorizons/modularui/ModularUI.java#L31). Indirect effects from dependencies, injection, configuration and server rules remain open.

### ModularUI2

Source [7e777df4a4](https://github.com/GTNewHorizons/ModularUI2/tree/7e777df4a4f0adb77dc1a1e7e5f803fffbf42f0d) · 631 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [ModularUI](https://github.com/GTNewHorizons/ModularUI2/blob/7e777df4a4f0adb77dc1a1e7e5f803fffbf42f0d/src/main/java/com/cleanroommc/modularui/ModularUI.java#L18). Indirect effects from dependencies, injection, configuration and server rules remain open.

### MouseTweaks

Source [1a2e3b95e4](https://github.com/GTNewHorizons/MouseTweaks/tree/1a2e3b95e4d2b83d22791e17c40a225df16f61cb) · 17 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [MouseTweaksForge](https://github.com/GTNewHorizons/MouseTweaks/blob/1a2e3b95e4d2b83d22791e17c40a225df16f61cb/src/main/java/yalter/mousetweaks/loaders/MouseTweaksForge.java#L17). Indirect effects from dependencies, injection, configuration and server rules remain open.

### MrTJPCore

Source [6cbdbe819c](https://github.com/GTNewHorizons/MrTJPCore/tree/6cbdbe819c08fbdc537a08f5640e89af3f89e73b) · 83 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [tileblock.readFromNBT](https://github.com/GTNewHorizons/MrTJPCore/blob/6cbdbe819c08fbdc537a08f5640e89af3f89e73b/src/main/scala/mrtjp/core/block/tileblock.scala#L620).
- Check preview rendering against `RendererSchematicChunk`. 2 source file(s) contain matching declarations, including [tileblock.renderWorldBlock](https://github.com/GTNewHorizons/MrTJPCore/blob/6cbdbe819c08fbdc537a08f5640e89af3f89e73b/src/main/scala/mrtjp/core/block/tileblock.scala#L93).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [tileblock.getPickBlock](https://github.com/GTNewHorizons/MrTJPCore/blob/6cbdbe819c08fbdc537a08f5640e89af3f89e73b/src/main/scala/mrtjp/core/block/tileblock.scala#L269).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [coreblock.placeBlockAt](https://github.com/GTNewHorizons/MrTJPCore/blob/6cbdbe819c08fbdc537a08f5640e89af3f89e73b/src/main/scala/mrtjp/core/block/coreblock.scala#L38).

### MyEssentials-Core

Source [cc00bddde0](https://github.com/GTNewHorizons/MyEssentials-Core/tree/cc00bddde0fe3bff315e9fca89d35721feca3e7b) · 78 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [SignClassTransformer.writeToNBT](https://github.com/GTNewHorizons/MyEssentials-Core/blob/cc00bddde0fe3bff315e9fca89d35721feca3e7b/src/main/java/myessentials/classtransformers/SignClassTransformer.java#L28).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [Tool.onItemUse](https://github.com/GTNewHorizons/MyEssentials-Core/blob/cc00bddde0fe3bff315e9fca89d35721feca3e7b/src/main/java/myessentials/entities/api/tool/Tool.java#L35).
- Check interaction events against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [SignManager.onPlayerInteract](https://github.com/GTNewHorizons/MyEssentials-Core/blob/cc00bddde0fe3bff315e9fca89d35721feca3e7b/src/main/java/myessentials/entities/api/sign/SignManager.java#L38).

### MyPermissions

Source [6318dd9912](https://github.com/GTNewHorizons/MyPermissions/tree/6318dd99129ad429d22bb66588cb3ac3ff6db370) · 33 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [MyPermissions](https://github.com/GTNewHorizons/MyPermissions/blob/6318dd99129ad429d22bb66588cb3ac3ff6db370/src/main/java/mypermissions/MyPermissions.java#L23). Indirect effects from dependencies, injection, configuration and server rules remain open.

### MyTown2

Source [b71446a0d4](https://github.com/GTNewHorizons/MyTown2/tree/b71446a0d4fc5e0b57e212b58ea8ad5dc8ac4bdf) · 87 tracked Java, Scala or Kotlin files outside test/example directories.

- Check printer placement against `SchematicPrinter`. 3 source file(s) contain matching declarations, including [PlotSelectionTool.onItemUse](https://github.com/GTNewHorizons/MyTown2/blob/b71446a0d4fc5e0b57e212b58ea8ad5dc8ac4bdf/src/main/java/mytown/entities/tools/PlotSelectionTool.java#L46).
- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ProtectionHandlers.onBlockPlacement](https://github.com/GTNewHorizons/MyTown2/blob/b71446a0d4fc5e0b57e212b58ea8ad5dc8ac4bdf/src/main/java/mytown/protection/ProtectionHandlers.java#L145).

### Natura

Source [f84ee064e2](https://github.com/GTNewHorizons/Natura/tree/f84ee064e22b4e92d172d89612c7d9339171d172) · 195 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [mods.natura.blocks.tech.NetherrackFurnaceLogic](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/tech/NetherrackFurnaceLogic.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [NetherrackFurnaceLogic.readFromNBT](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/tech/NetherrackFurnaceLogic.java#L130).
- Check preview rendering against `RendererSchematicChunk`. 6 source file(s) contain matching declarations, including [BerryRender.renderWorldBlock](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/client/BerryRender.java#L16).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [NDoor.getPickBlock](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/trees/NDoor.java#L118).
- Check printer placement against `SchematicPrinter`. 15 source file(s) contain matching declarations, including [ThornVines.onBlockPlaced](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/crops/ThornVines.java#L60).
- [NetherPistonBase.onBlockPlacedBy](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/tech/NetherPistonBase.java#L112) depends on placement orientation. Exercise the printer with a rotated schematic.
- [NetherrackFurnaceLogic.readFromNBT](https://github.com/GTNewHorizons/Natura/blob/f84ee064e22b4e92d172d89612c7d9339171d172/src/main/java/mods/natura/blocks/tech/NetherrackFurnaceLogic.java#L130) accesses `Items`, `Slot`, `BurnTime`, `CookTime`. Compare those fields after capture, transform and restore.

### NaturesCompass

Source [babf1615c7](https://github.com/GTNewHorizons/NaturesCompass/tree/babf1615c7cdf37630da4790b5c4221dfa17784d) · 34 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NaturesCompass](https://github.com/GTNewHorizons/NaturesCompass/blob/babf1615c7cdf37630da4790b5c4221dfa17784d/src/main/java/com/chaosthedude/naturescompass/NaturesCompass.java#L31). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Navigator

Source [788b66357d](https://github.com/GTNewHorizons/Navigator/tree/788b66357db8fb6953322a76ce12b43ded133e83) · 73 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Navigator](https://github.com/GTNewHorizons/Navigator/blob/788b66357db8fb6953322a76ce12b43ded133e83/src/main/java/com/gtnewhorizons/navigator/Navigator.java#L12). Indirect effects from dependencies, injection, configuration and server rules remain open.

### nei-custom-diagram

Source [87a13c2884](https://github.com/GTNewHorizons/nei-custom-diagram/tree/87a13c2884238248fc5497612f5d1ca508f3db53) · 111 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NeiCustomDiagram](https://github.com/GTNewHorizons/nei-custom-diagram/blob/87a13c2884238248fc5497612f5d1ca508f3db53/src/main/java/com/github/dcysteine/neicustomdiagram/main/NeiCustomDiagram.java#L20). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NEI-Integration

Source [1e88b70767](https://github.com/GTNewHorizons/NEI-Integration/tree/1e88b70767d8eef8330d9f16d57ad7ca02de9505) · 63 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NEIIntegration](https://github.com/GTNewHorizons/NEI-Integration/blob/1e88b70767d8eef8330d9f16d57ad7ca02de9505/src/main/java/tonius/neiintegration/NEIIntegration.java#L15). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Nei-Recipe-Panels

Source [96555f62d7](https://github.com/GTNewHorizons/Nei-Recipe-Panels/tree/96555f62d7dd6fb95220d1c8103f0cd223bc8390) · 40 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [RecipePanelTile.readFromNBT](https://github.com/GTNewHorizons/Nei-Recipe-Panels/blob/96555f62d7dd6fb95220d1c8103f0cd223bc8390/src/main/java/com/gtnewhorizons/neirecipepanel/block/RecipePanelTile.java#L76).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RecipePanelRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/Nei-Recipe-Panels/blob/96555f62d7dd6fb95220d1c8103f0cd223bc8390/src/main/java/com/gtnewhorizons/neirecipepanel/client/render/RecipePanelRenderer.java#L27).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [RecipePanelBlock.getPickBlock](https://github.com/GTNewHorizons/Nei-Recipe-Panels/blob/96555f62d7dd6fb95220d1c8103f0cd223bc8390/src/main/java/com/gtnewhorizons/neirecipepanel/block/RecipePanelBlock.java#L143).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemRecipePanel.onItemUse](https://github.com/GTNewHorizons/Nei-Recipe-Panels/blob/96555f62d7dd6fb95220d1c8103f0cd223bc8390/src/main/java/com/gtnewhorizons/neirecipepanel/item/ItemRecipePanel.java#L75).

### neiaddons

Source [cd4b95fc9a](https://github.com/GTNewHorizons/neiaddons/tree/cd4b95fc9a8f6952c6d872caca134a9e7e6d74dd) · 60 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NEIAddons](https://github.com/GTNewHorizons/neiaddons/blob/cd4b95fc9a8f6952c6d872caca134a9e7e6d74dd/src/main/scala/net/bdew/neiaddons/NEIAddons.java#L28). Indirect effects from dependencies, injection, configuration and server rules remain open.

### nesql-exporter

Source [dba624e097](https://github.com/GTNewHorizons/nesql-exporter/tree/dba624e097daee0ef16fde94c3c997e43a0dc59c) · 132 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Main](https://github.com/GTNewHorizons/nesql-exporter/blob/dba624e097daee0ef16fde94c3c997e43a0dc59c/src/main/java/com/github/dcysteine/nesql/exporter/main/Main.java#L19). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NetherPortalFix

Source [836a60dc6b](https://github.com/GTNewHorizons/NetherPortalFix/tree/836a60dc6bd6e61463f4c986b2901914f57ded4a) · 2 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NetherPortalFix](https://github.com/GTNewHorizons/NetherPortalFix/blob/836a60dc6bd6e61463f4c986b2901914f57ded4a/src/main/java/net/blay09/mods/netherportalfix/NetherPortalFix.java#L11). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NewHorizonsCoreMod

Source [240cd0b1aa](https://github.com/GTNewHorizons/NewHorizonsCoreMod/tree/240cd0b1aadfd8020e68f5fb987083096083e6f1) · 310 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [com.dreammaster.modbabychest.TileEntityBabyChest](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/TileEntityBabyChest.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [TileEntityBabyChest.readFromNBT](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/TileEntityBabyChest.java#L91).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RenderBabyChest.renderTileEntityAt](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/RenderBabyChest.java#L19).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [BlockBabyChest.onBlockPlacedBy](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/BlockBabyChest.java#L151).
- [BlockBabyChest.onBlockPlacedBy](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/BlockBabyChest.java#L151) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityBabyChest.readFromNBT](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/240cd0b1aadfd8020e68f5fb987083096083e6f1/src/main/java/com/dreammaster/modbabychest/TileEntityBabyChest.java#L91) accesses `Items`, `Slot`, `facing`. Compare those fields after capture, transform and restore.

### Nodal-Mechanics

Source [298f678ad9](https://github.com/GTNewHorizons/Nodal-Mechanics/tree/298f678ad946329fa9f22b0a12d51654a3522792) · 11 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NodalMechanics](https://github.com/GTNewHorizons/Nodal-Mechanics/blob/298f678ad946329fa9f22b0a12d51654a3522792/src/main/java/shukaro/nodalmechanics/NodalMechanics.java#L19). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NoHotbarNeeded

Source [dcacd08964](https://github.com/GTNewHorizons/NoHotbarNeeded/tree/dcacd08964eb6386bd7383a5e47fecee5ab692a4) · 8 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NoHotbarNeeded](https://github.com/GTNewHorizons/NoHotbarNeeded/blob/dcacd08964eb6386bd7383a5e47fecee5ab692a4/src/main/java/net/xonich/mc/nohotbarneeded/NoHotbarNeeded.java#L9). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NotEnoughCharacters

Source [5c15126bf5](https://github.com/GTNewHorizons/NotEnoughCharacters/tree/5c15126bf5fbcdbc8060423858d6b0c46e3a3fbc) · 16 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NotEnoughCharacters](https://github.com/GTNewHorizons/NotEnoughCharacters/blob/5c15126bf5fbcdbc8060423858d6b0c46e3a3fbc/src/main/java/net/moecraft/nechar/NotEnoughCharacters.java#L18). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NotEnoughEnergistics

Source [130483def6](https://github.com/GTNewHorizons/NotEnoughEnergistics/tree/130483def6d297ff51062968777f9f568a4a65cf) · 64 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [NotEnoughEnergistics](https://github.com/GTNewHorizons/NotEnoughEnergistics/blob/130483def6d297ff51062968777f9f568a4a65cf/src/main/java/com/github/vfyjxf/nee/NotEnoughEnergistics.java#L24). Indirect effects from dependencies, injection, configuration and server rules remain open.

### NotEnoughIds

Source [b80765bfc8](https://github.com/GTNewHorizons/NotEnoughIds/tree/b80765bfc817499711da0e1fb04f01d1141b3179) · 43 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 4 source file(s) contain matching declarations, including [MixinWorld.World](https://github.com/GTNewHorizons/NotEnoughIds/blob/b80765bfc817499711da0e1fb04f01d1141b3179/src/main/java/com/gtnewhorizons/neid/mixins/early/minecraft/MixinWorld.java#L11).

### NotEnoughItems

Source [6b7e465306](https://github.com/GTNewHorizons/NotEnoughItems/tree/6b7e465306810ad11d2413214077237798be13a7) · 303 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `NeiBridge` names [codechicken.nei.bookmark.BookmarkGrid](https://github.com/GTNewHorizons/NotEnoughItems/blob/6b7e465306810ad11d2413214077237798be13a7/src/main/java/codechicken/nei/bookmark/BookmarkGrid.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemMobSpawner.onItemUse](https://github.com/GTNewHorizons/NotEnoughItems/blob/6b7e465306810ad11d2413214077237798be13a7/src/main/java/codechicken/nei/ItemMobSpawner.java#L67).

### Nuclear-Control

Source [1c706582ac](https://github.com/GTNewHorizons/Nuclear-Control/tree/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9) · 214 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `CoordinateLinks` names [shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/tileentities/TileEntityInfoPanel.java#L1) (4 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 13 source file(s) contain matching declarations, including [InventoryItem.readFromNBT](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/InventoryItem.java#L121).
- Check preview rendering against `RendererSchematicChunk`. 4 source file(s) contain matching declarations, including [MainBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/renderers/MainBlockRenderer.java#L137).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [BlockNuclearControlMain.onBlockPlaced](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/blocks/BlockNuclearControlMain.java#L135).
- [InventoryItem.readFromNBT](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/InventoryItem.java#L121) accesses `ItemInventory`, `Slot`. Compare those fields after capture, transform and restore.
- [MainBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/Nuclear-Control/blob/1c706582ac5f3a3f3cc7db89f55c4c2da15a5dd9/src/main/java/shedar/mods/ic2/nuclearcontrol/renderers/MainBlockRenderer.java#L137) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.

### NuclearHorizons

Source [0229c627f1](https://github.com/GTNewHorizons/NuclearHorizons/tree/0229c627f1ba6bd48ead0f2c09e3ae1b1c73d45d) · 97 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 7 source file(s) contain matching declarations, including [TileAccessHatch.writeToNBT](https://github.com/GTNewHorizons/NuclearHorizons/blob/0229c627f1ba6bd48ead0f2c09e3ae1b1c73d45d/src/main/java/com/recursive_pineapple/nuclear_horizons/reactors/tile/TileAccessHatch.java#L67).
- [TileAccessHatch.writeToNBT](https://github.com/GTNewHorizons/NuclearHorizons/blob/0229c627f1ba6bd48ead0f2c09e3ae1b1c73d45d/src/main/java/com/recursive_pineapple/nuclear_horizons/reactors/tile/TileAccessHatch.java#L67) accesses `version`, `reactorRelX`, `reactorRelY`, `reactorRelZ`. Compare those fields after capture, transform and restore.

### Nutrition

Source [c42ac73dde](https://github.com/GTNewHorizons/Nutrition/tree/c42ac73dde3c179f2d47db72738a53e16188c5e3) · 41 tracked Java, Scala or Kotlin files outside test/example directories.

- Check interaction events against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [EventAllowOvereating.rightClickBlock](https://github.com/GTNewHorizons/Nutrition/blob/c42ac73dde3c179f2d47db72738a53e16188c5e3/src/main/java/ca/wescook/nutrition/events/EventAllowOvereating.java#L17).

### oauth

Source [f38b03312a](https://github.com/GTNewHorizons/oauth/tree/f38b03312aedff1bd72c5c2bb373a3d2e0b77a4f) · 7 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [OAuth](https://github.com/GTNewHorizons/oauth/blob/f38b03312aedff1bd72c5c2bb373a3d2e0b77a4f/src/main/java/com/sintinium/oauth/OAuth.java#L8). Indirect effects from dependencies, injection, configuration and server rules remain open.

### OCGlasses

Source [35a455fd0c](https://github.com/GTNewHorizons/OCGlasses/tree/35a455fd0ceed1f9e1b9573a32c4e0caf5525633) · 98 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 2 source file(s) contain matching declarations, including [Widget.writeToNBT](https://github.com/GTNewHorizons/OCGlasses/blob/35a455fd0ceed1f9e1b9573a32c4e0caf5525633/src/main/java/com/bymarcin/openglasses/surface/Widget.java#L34).
- [OpenGlassesTerminalTileEntity.writeToNBT](https://github.com/GTNewHorizons/OCGlasses/blob/35a455fd0ceed1f9e1b9573a32c4e0caf5525633/src/main/java/com/bymarcin/openglasses/tileentity/OpenGlassesTerminalTileEntity.java#L330) accesses `currID`, `listSize`, `widgetType`, `ID`. Compare those fields after capture, transform and restore.

### Offscreen

Source [1a4b448e83](https://github.com/GTNewHorizons/Offscreen/tree/1a4b448e83ad1947d59bbfa2957f958c3dac59ae) · 62 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [EntityRendererFocus.EntityRenderer](https://github.com/GTNewHorizons/Offscreen/blob/1a4b448e83ad1947d59bbfa2957f958c3dac59ae/src/main/java/com/sbancuz/offscreen/mixins/early/EntityRendererFocus.java#L12).

### OpenBlocks

Source [fa609d073b](https://github.com/GTNewHorizons/OpenBlocks/tree/fa609d073b315ec1efbae56facb5e88fa255fa4c) · 381 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 21 source file(s) contain matching declarations, including [HeightMapData.readFromNBT](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/common/HeightMapData.java#L21).
- Check preview rendering against `RendererSchematicChunk`. 30 source file(s) contain matching declarations, including [BlockCanvasRenderer.renderWorldBlock](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/client/renderer/block/BlockCanvasRenderer.java#L32).
- Check material picking against `BlockToItemStack`. 5 source file(s) contain matching declarations, including [BlockTank.getPickBlock](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/common/block/BlockTank.java#L70).
- Check printer placement against `SchematicPrinter`. 18 source file(s) contain matching declarations, including [BlockGuide.onItemUse](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/common/block/BlockGuide.java#L268).
- [BlockCanvasRenderer.renderWorldBlock](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/client/renderer/block/BlockCanvasRenderer.java#L32) queries tile data while rendering. Preview a populated tile after saving and reopening its schematic.
- [HeightMapData.readFromNBT](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/common/HeightMapData.java#L21) accesses `Alpha`. Compare those fields after capture, transform and restore.
- [BlockTank.getPickBlock](https://github.com/GTNewHorizons/OpenBlocks/blob/fa609d073b315ec1efbae56facb5e88fa255fa4c/src/main/java/openblocks/common/block/BlockTank.java#L70) queries world state when selecting an item. Compare the materials list against the placed variant.

### OpenComputers

Source [a313175a3c](https://github.com/GTNewHorizons/OpenComputers/tree/a313175a3cbf47f8642dd77838a2d4a56842677c) · 1108 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `TileFacingAdapter` names [li.cil.oc.common.tileentity.traits.Rotatable](https://github.com/GTNewHorizons/OpenComputers/blob/a313175a3cbf47f8642dd77838a2d4a56842677c/src/main/scala/li/cil/oc/common/tileentity/traits/Rotatable.scala#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 8 source file(s) contain matching declarations, including [TileEntityEnvironment.readFromNBT](https://github.com/GTNewHorizons/OpenComputers/blob/a313175a3cbf47f8642dd77838a2d4a56842677c/src/main/java/li/cil/oc/api/prefab/TileEntityEnvironment.java#L130).
- Check preview rendering against `RendererSchematicChunk`. 10 source file(s) contain matching declarations, including [BlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/OpenComputers/blob/a313175a3cbf47f8642dd77838a2d4a56842677c/src/main/scala/li/cil/oc/client/renderer/block/BlockRenderer.scala#L89).
- Check material picking against `BlockToItemStack`. 6 source file(s) contain matching declarations, including [Cable.getPickBlock](https://github.com/GTNewHorizons/OpenComputers/blob/a313175a3cbf47f8642dd77838a2d4a56842677c/src/main/scala/li/cil/oc/common/block/Cable.scala#L67).
- Check printer placement against `SchematicPrinter`. 12 source file(s) contain matching declarations, including [Item.placeBlockAt](https://github.com/GTNewHorizons/OpenComputers/blob/a313175a3cbf47f8642dd77838a2d4a56842677c/src/main/scala/li/cil/oc/common/block/Item.scala#L89).

### OpenModsLib

Source [3889667370](https://github.com/GTNewHorizons/OpenModsLib/tree/38896673704952abff874cbe4e52369ffdaa43ee) · 675 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [openmods.block.OpenBlock](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/block/OpenBlock.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 29 source file(s) contain matching declarations, including [GenericInventory.readFromNBT](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/inventory/GenericInventory.java#L120).
- Check preview rendering against `RendererSchematicChunk`. 5 source file(s) contain matching declarations, including [BlockRenderingHandler.renderWorldBlock](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/renderer/BlockRenderingHandler.java#L54).
- Check material picking against `BlockToItemStack`. 3 source file(s) contain matching declarations, including [ICustomPickItem.getPickBlock](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/api/ICustomPickItem.java#L7).
- Check printer placement against `SchematicPrinter`. 8 source file(s) contain matching declarations, including [IPlaceAwareTile.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/api/IPlaceAwareTile.java#L12).
- [OpenBlock.getPickBlock](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/block/OpenBlock.java#L238) queries world state when selecting an item. Compare the materials list against the placed variant.
- [SyncableUUID.writeToNBT](https://github.com/GTNewHorizons/OpenModsLib/blob/38896673704952abff874cbe4e52369ffdaa43ee/src/main/java/openmods/sync/SyncableUUID.java#L42) accesses `MSB`, `LSB`. Compare those fields after capture, transform and restore.

### OpenModularTurrets

Source [2f1ea3ba1b](https://github.com/GTNewHorizons/OpenModularTurrets/tree/2f1ea3ba1ba8fc35a07e7a4fcb10002aedbd493e) · 272 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 5 source file(s) contain matching declarations, including [LeverTileEntity.writeToNBT](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2f1ea3ba1ba8fc35a07e7a4fcb10002aedbd493e/src/main/java/openmodularturrets/tileentity/LeverTileEntity.java#L53).
- Check preview rendering against `RendererSchematicChunk`. 21 source file(s) contain matching declarations, including [DisposableItemTurretRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2f1ea3ba1ba8fc35a07e7a4fcb10002aedbd493e/src/main/java/openmodularturrets/client/render/renderers/blockitem/DisposableItemTurretRenderer.java#L38).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [LeverBlock.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2f1ea3ba1ba8fc35a07e7a4fcb10002aedbd493e/src/main/java/openmodularturrets/blocks/misc/LeverBlock.java#L58).
- [AbstractInvExpander.writeToNBT](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2f1ea3ba1ba8fc35a07e7a4fcb10002aedbd493e/src/main/java/openmodularturrets/tileentity/expander/AbstractInvExpander.java#L41) accesses `Slot`, `Inventory`. Compare those fields after capture, transform and restore.

### OpenPrinter

Source [160a2ff67b](https://github.com/GTNewHorizons/OpenPrinter/tree/160a2ff67bc97976cfa3345e922601ba0251d938) · 46 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [pcl.openprinter.blocks.BlockFileCabinet](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/blocks/BlockFileCabinet.java#L1) (3 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 4 source file(s) contain matching declarations, including [FolderInventory.readFromNBT](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/items/FolderInventory.java#L205).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [PrinterRenderer.renderTileEntityAt](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/tileentityrender/PrinterRenderer.java#L26).
- Check printer placement against `SchematicPrinter`. 4 source file(s) contain matching declarations, including [BlockFileCabinet.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/blocks/BlockFileCabinet.java#L109).
- [BlockFileCabinet.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/blocks/BlockFileCabinet.java#L109) depends on placement orientation. Exercise the printer with a rotated schematic.
- [FolderInventory.readFromNBT](https://github.com/GTNewHorizons/OpenPrinter/blob/160a2ff67bc97976cfa3345e922601ba0251d938/src/main/java/pcl/openprinter/items/FolderInventory.java#L205) accesses `ItemInventory`, `Slot`. Compare those fields after capture, transform and restore.

### OpenSecurity

Source [49063f36c2](https://github.com/GTNewHorizons/OpenSecurity/tree/49063f36c2770bac38a0b0f9eaa1105ff811f814) · 91 tracked Java, Scala or Kotlin files outside test/example directories.

- Existing Plus lookup in `BlockMetaTransform` names [pcl.opensecurity.blocks.BlockOSBase](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/blocks/BlockOSBase.java#L1) (1 matching upstream source names). Check reflected members and client state with the deployed versions.
- Check tile serialization against `TileNBTCompat`. 13 source file(s) contain matching declarations, including [TileEntityAlarm.readFromNBT](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/tileentity/TileEntityAlarm.java#L166).
- Check preview rendering against `RendererSchematicChunk`. 3 source file(s) contain matching declarations, including [RenderDisplayPanel.renderTileEntityAt](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/client/renderer/RenderDisplayPanel.java#L28).
- Check material picking against `BlockToItemStack`. 2 source file(s) contain matching declarations, including [BlockSecurityDoor.getPickBlock](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/blocks/BlockSecurityDoor.java#L105).
- Check printer placement against `SchematicPrinter`. 8 source file(s) contain matching declarations, including [BlockDoorController.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/blocks/BlockDoorController.java#L40).
- [BlockKVM.onBlockPlacedBy](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/blocks/BlockKVM.java#L46) depends on placement orientation. Exercise the printer with a rotated schematic.
- [TileEntityBiometricReader.readFromNBT](https://github.com/GTNewHorizons/OpenSecurity/blob/49063f36c2770bac38a0b0f9eaa1105ff811f814/src/main/java/pcl/opensecurity/tileentity/TileEntityBiometricReader.java#L64) accesses `eventName`. Compare those fields after capture, transform and restore.

### Opis

Source [7776b35d82](https://github.com/GTNewHorizons/Opis/tree/7776b35d82f26328d6a46527c27469480ef78d93) · 243 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [modOpis](https://github.com/GTNewHorizons/Opis/blob/7776b35d82f26328d6a46527c27469480ef78d93/src/main/java/mcp/mobius/opis/modOpis.java#L74). Indirect effects from dependencies, injection, configuration and server rules remain open.

### OverloadedArmorBar

Source [444b077fae](https://github.com/GTNewHorizons/OverloadedArmorBar/tree/444b077fae58e809cf90c4adfb1122f126a9b566) · 5 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [OverloadedArmorBar](https://github.com/GTNewHorizons/OverloadedArmorBar/blob/444b077fae58e809cf90c4adfb1122f126a9b566/src/main/java/locusway/overloadedarmorbar/OverloadedArmorBar.java#L11). Indirect effects from dependencies, injection, configuration and server rules remain open.

### PersonalSpace

Source [e3c3a2e1a4](https://github.com/GTNewHorizons/PersonalSpace/tree/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1) · 30 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [PortalTileEntity.readFromNBT](https://github.com/GTNewHorizons/PersonalSpace/blob/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1/src/main/java/me/eigenraven/personalspace/block/PortalTileEntity.java#L70).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [RenderPortal.renderTileEntityAt](https://github.com/GTNewHorizons/PersonalSpace/blob/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1/src/main/java/me/eigenraven/personalspace/block/RenderPortal.java#L18).
- Check material picking against `BlockToItemStack`. 1 source file(s) contain matching declarations, including [PortalBlock.getPickBlock](https://github.com/GTNewHorizons/PersonalSpace/blob/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1/src/main/java/me/eigenraven/personalspace/block/PortalBlock.java#L134).
- Check printer placement against `SchematicPrinter`. 2 source file(s) contain matching declarations, including [PortalBlock.onBlockPlacedBy](https://github.com/GTNewHorizons/PersonalSpace/blob/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1/src/main/java/me/eigenraven/personalspace/block/PortalBlock.java#L85).
- [PortalTileEntity.readFromNBT](https://github.com/GTNewHorizons/PersonalSpace/blob/e3c3a2e1a4f759c3a64eb2392992f73e0b1335a1/src/main/java/me/eigenraven/personalspace/block/PortalTileEntity.java#L70) accesses `isReturnPortal`, `remoteDimensionId`, `remotePos`, `remoteDir`. Compare those fields after capture, transform and restore.

### PlanNH

Source [45a0d1efca](https://github.com/GTNewHorizons/PlanNH/tree/45a0d1efca52b946ae1cc1139f8b2e852fb44e6c) · 273 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [PlanNH](https://github.com/GTNewHorizons/PlanNH/blob/45a0d1efca52b946ae1cc1139f8b2e852fb44e6c/src/main/java/com/sbancuz/plannh/PlanNH.java#L14). Indirect effects from dependencies, injection, configuration and server rules remain open.

### Player-API

Source [5a2a3849db](https://github.com/GTNewHorizons/Player-API/tree/5a2a3849dba3f5eefee075931bfcaddaefc5fff7) · 19 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Indirect effects from dependencies, injection, configuration and server rules remain open.

### Postea

Source [bdd8977182](https://github.com/GTNewHorizons/Postea/tree/bdd8977182c57d0920769bcf85d26f92519e7e8c) · 39 tracked Java, Scala or Kotlin files outside test/example directories.

- Check world or renderer transformation alongside the schematic world and render pipeline. 1 source file(s) contain matching declarations, including [MixinChunk.Chunk](https://github.com/GTNewHorizons/Postea/blob/bdd8977182c57d0920769bcf85d26f92519e7e8c/src/main/java/com/gtnewhorizons/postea/mixins/early/MixinChunk.java#L10).

### ProjectBlue

Source [a2678a26af](https://github.com/GTNewHorizons/ProjectBlue/tree/a2678a26af80a2ad580aae032030ede3df0ad3b4) · 60 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [BaseTileEntity.readFromNBT](https://github.com/GTNewHorizons/ProjectBlue/blob/a2678a26af80a2ad580aae032030ede3df0ad3b4/src/main/scala/gcewing/projectblue/BaseTileEntity.java#L56).
- Check preview rendering against `RendererSchematicChunk`. 1 source file(s) contain matching declarations, including [BaseBlockRenderer.renderWorldBlock](https://github.com/GTNewHorizons/ProjectBlue/blob/a2678a26af80a2ad580aae032030ede3df0ad3b4/src/main/scala/gcewing/projectblue/BaseBlockRenderer.java#L65).
- Check printer placement against `SchematicPrinter`. 1 source file(s) contain matching declarations, including [ItemMultiPartJ.onItemUse](https://github.com/GTNewHorizons/ProjectBlue/blob/a2678a26af80a2ad580aae032030ede3df0ad3b4/src/main/scala/gcewing/projectblue/ItemMultiPartJ.java#L35).

### ProjectRed

Source [50ae9866a9](https://github.com/GTNewHorizons/ProjectRed/tree/50ae9866a93557366bb42cfa4a36bf8863076dae) · 220 tracked Java, Scala or Kotlin files outside test/example directories.

- Check tile serialization against `TileNBTCompat`. 1 source file(s) contain matching declarations, including [blocks.readFromNBT](https://github.com/GTNewHorizons/ProjectRed/blob/50ae9866a93557366bb42cfa4a36bf8863076dae/src/main/scala/mrtjp/projectred/illumination/blocks.scala#L537).
- Check preview rendering against `RendererSchematicChunk`. 4 source file(s) contain matching declarations, including [TileBarrel.renderTileEntityAt](https://github.com/GTNewHorizons/ProjectRed/blob/50ae9866a93557366bb42cfa4a36bf8863076dae/src/main/scala/mrtjp/projectred/exploration/TileBarrel.scala#L345).
- Check printer placement against `SchematicPrinter`. 9 source file(s) contain matching declarations, including [connectabletiles.onBlockPlaced](https://github.com/GTNewHorizons/ProjectRed/blob/50ae9866a93557366bb42cfa4a36bf8863076dae/src/main/scala/mrtjp/projectred/core/connectabletiles.scala#L224).

### Pseudo

Source [4a9b7b7a01](https://github.com/GTNewHorizons/Pseudo/tree/4a9b7b7a012f923d5595ab07bdcb30123b6dfd1b) · 4 tracked Java, Scala or Kotlin files outside test/example directories.

- No direct match in the listed checks. Mod entry point: [Pseudo](https://github.com/GTNewHorizons/Pseudo/blob/4a9b7b7a012f923d5595ab07bdcb30123b6dfd1b/src/main/java/com/gtnewhorizons/pseudo/Pseudo.java#L12). Indirect effects from dependencies, injection, configuration and server rules remain open.
