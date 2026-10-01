// SPDX-License-Identifier: LGPL-3.0-only
// Litematica schematic rebuild (Edit Schematic) operations, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.CellState;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SourceBlockPosition;
import com.github.lunatrius.schematica.client.world.SourceEditor;
import com.github.lunatrius.schematica.client.world.SourceEditor.Cell;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

/** Edits loaded schematic sources in memory; the real world is never changed. */
public final class SchematicRebuild {
    private static final double RANGE = 10;
    private static final int MAX_RUN = 10000;

    private SchematicRebuild() {}

    public static final class Target {
        public final SchematicWorld world;
        public final int x, y, z, side;
        public final double hitX, hitY, hitZ, distance;

        Target(SchematicWorld world, MovingObjectPosition hit, double distance) {
            this.world = world;
            x = hit.blockX + world.position.x; y = hit.blockY + world.position.y; z = hit.blockZ + world.position.z;
            side = hit.sideHit;
            hitX = hit.hitVec.xCoord + world.position.x; hitY = hit.hitVec.yCoord + world.position.y; hitZ = hit.hitVec.zCoord + world.position.z;
            this.distance = distance;
        }

        public ForgeDirection face() { return ForgeDirection.getOrientation(side); }
        int[] adjacent() { ForgeDirection d = face(); return new int[] {x + d.offsetX, y + d.offsetY, z + d.offsetZ}; }
    }

    static final class Owner {
        final SchematicLibrary.Source<SchematicSourceData> source;
        final SchematicWorld world;
        final SourceBlockPosition position;
        final Cell cell;

        Owner(SchematicLibrary.Source<SchematicSourceData> source, SchematicWorld world, SourceBlockPosition position, Cell cell) {
            this.source = source; this.world = world; this.position = position; this.cell = cell;
        }

        List<String> operations() { return SourceBlockPosition.combined(position.region, world.transformOperations); }
    }

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    /** Returns whether the press is consumed; like Litematica, only an edit that was carried out consumes it. */
    public static boolean click(boolean attack) {
        if (ToolManager.getCurrentMode() != ToolMode.REBUILD || ClientProxy.loadedSchematics.isEmpty()) return false;
        try {
            return attack ? attack() : use();
        } catch (IOException error) {
            Reference.logger.error("Could not edit the schematic source", error);
            message(EnumChatFormatting.RED, "schematica.message.rebuild.failed");
            return false;
        }
    }

    private static boolean attack() throws IOException {
        boolean direction = Hotkeys.held("schematicEditBreakPlaceDirection");
        boolean except = !direction && Hotkeys.held("schematicEditBreakAllExcept");
        boolean all = !direction && !except && Hotkeys.held("schematicEditBreakPlaceAll");
        Target target = trace(direction || except || all ? RANGE : mc().playerController.getBlockReachDistance() + 1);
        if (target == null) return false;
        Owner owner = owner(target.world, target.x, target.y, target.z);
        if (owner == null) return false;
        CellState targeted = composed(owner.world, target.x, target.y, target.z);
        if (direction) return setRun(owner, target.x, target.y, target.z, directionAway(target), CellState.AIR);
        if (except) return bulk(owner, rule -> {
            CellState keep = untransformed(targeted, rule.operations);
            return (block, meta) -> block != CellState.AIR.block && (block != keep.block || meta != keep.meta) ? CellState.AIR : null;
        });
        if (all) return bulk(owner, rule -> {
            CellState original = untransformed(targeted, rule.operations);
            return (block, meta) -> block == original.block && meta == original.meta ? CellState.AIR : null;
        });
        apply(owner.source, single(owner.cell, CellState.AIR));
        return true;
    }

    private static boolean use() throws IOException {
        Target target = trace(RANGE);
        if (target == null) return false;
        CellState placed = heldState(target);
        if (placed == null) return false;
        int[] next = target.adjacent();
        if (Hotkeys.held("schematicEditReplaceDirection")) {
            Owner owner = owner(target.world, target.x, target.y, target.z);
            return owner != null && setRun(owner, target.x, target.y, target.z, directionAway(target), placed);
        }
        if (Hotkeys.held("schematicEditReplaceAll")) {
            Owner owner = owner(target.world, target.x, target.y, target.z);
            if (owner == null) return false;
            CellState targeted = composed(owner.world, target.x, target.y, target.z);
            return bulk(owner, rule -> {
                CellState original = untransformed(targeted, rule.operations), replacement = untransformed(placed, rule.operations);
                return (block, meta) -> block == original.block && meta == original.meta ? replacement : null;
            });
        }
        if (Hotkeys.held("schematicEditReplaceBlock")) {
            Owner owner = owner(target.world, target.x, target.y, target.z);
            if (owner == null) return false;
            CellState targeted = composed(owner.world, target.x, target.y, target.z);
            if (placed.same(targeted)) return false;
            Map<Integer, CellState> states = new HashMap<>();
            return bulk(owner, rule -> (block, meta) -> block == targeted.block && placed.block != targeted.block
                ? states.computeIfAbsent(meta, value -> CellState.of(placed.block, value)) : null);
        }
        if (Hotkeys.held("schematicEditBreakPlaceDirection")) {
            Owner start = owner(target.world, next[0], next[1], next[2]);
            return start != null && composed(start.world, next[0], next[1], next[2]).isAir()
                && setRun(start, next[0], next[1], next[2], RebuildDirection.targeted(target.face(), facing(), target.hitX - target.x,
                    target.hitY - target.y, target.hitZ - target.z), placed);
        }
        if (Hotkeys.held("schematicEditBreakPlaceAll")) {
            Owner start = owner(target.world, next[0], next[1], next[2]);
            return start != null && composed(start.world, next[0], next[1], next[2]).isAir() && bulk(start, rule -> {
                CellState replacement = untransformed(placed, rule.operations);
                return (block, meta) -> block == CellState.AIR.block ? replacement : null;
            });
        }
        if (!inRange(next[0], next[1], next[2])) return false;
        Owner start = owner(target.world, next[0], next[1], next[2]);
        if (start == null) return false;
        apply(start.source, single(start.cell, untransformed(placed, start.operations())));
        return true;
    }

    /** Copies differing real-world blocks inside the current area selection into the schematic(s) shown there. */
    public static boolean replaceSelection() {
        Area area = AreaSelections.library().selected();
        if (area == null || area.boxes().isEmpty() || mc().theWorld == null || ClientProxy.loadedSchematics.isEmpty()) return false;
        com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i origin = area.origin();
        return RebuildJobs.start(new RebuildJobs.Selection(mc().theWorld, area.regions(), area.name(),
            String.format("x: %d, y: %d, z: %d", origin.x, origin.y, origin.z)));
    }

    public static Target trace(double range) {
        Minecraft mc = mc();
        EntityLivingBase camera = mc.renderViewEntity == null ? mc.thePlayer : mc.renderViewEntity;
        if (camera == null || mc.theWorld == null || !VisualSettings.schematicVisible()) return null;
        Vec3 eye = camera.getPosition(1), look = camera.getLook(1);
        double limit = range;
        MovingObjectPosition real = mc.theWorld.rayTraceBlocks(copy(eye, 0, 0, 0), eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), true);
        if (real != null && real.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) limit = eye.distanceTo(real.hitVec);
        Target best = null;
        for (SchematicWorld world : ClientProxy.loadedSchematics) {
            if (!world.isRenderingEnabled()) continue;
            Vec3 start = copy(eye, -world.position.x, -world.position.y, -world.position.z);
            MovingObjectPosition hit = world.rayTraceRendered(copy(start, 0, 0, 0),
                start.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), true);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
            double distance = start.distanceTo(hit.hitVec);
            if (distance <= limit + 1.0E-7 && (best == null || distance < best.distance)) best = new Target(world, hit, distance);
        }
        return best;
    }

    private static Vec3 copy(Vec3 vector, double x, double y, double z) {
        return Vec3.createVectorHelper(vector.xCoord + x, vector.yCoord + y, vector.zCoord + z);
    }

    /** The placement and source cell shown at a world position, preferring the given placement. */
    static Owner owner(SchematicWorld preferred, int x, int y, int z) {
        List<SchematicWorld> order = new ArrayList<>();
        if (preferred != null && ClientProxy.loadedSchematics.contains(preferred)) order.add(preferred);
        for (SchematicWorld world : ClientProxy.loadedSchematics) if (world != preferred) order.add(world);
        SchematicOrigin point = new SchematicOrigin(x, y, z);
        for (SchematicWorld world : order) {
            SchematicLibrary.Source<SchematicSourceData> source = ClientProxy.SCHEMATICS.sourceOf(world);
            if (source == null || !world.isEnabled() || world.subregions() == null
                || !world.getSchematic().containsBlock(x - world.position.x, y - world.position.y, z - world.position.z)) continue;
            SourceBlockPosition position = SourceBlockPosition.resolve(point, world.originPosition(), world.transformOperations, world.subregions());
            if (position == null) continue;
            try {
                return new Owner(source, world, position, SourceEditor.cell(source.data().editable(), position));
            } catch (IOException error) {
                Reference.logger.error("Could not prepare the schematic source for editing", error);
                return null;
            }
        }
        return null;
    }

    static CellState composed(SchematicWorld world, int x, int y, int z) {
        int lx = x - world.position.x, ly = y - world.position.y, lz = z - world.position.z;
        ISchematic schematic = world.getSchematic();
        return new CellState(schematic.getBlock(lx, ly, lz), schematic.getBlockMetadata(lx, ly, lz), null);
    }

    private static CellState untransformed(CellState state, List<String> operations) {
        return state.transform(SourceBlockPosition.inverse(operations), new HashMap<>());
    }

    static boolean inRange(int x, int y, int z) { return RenderLayerSettings.RANGE.contains(x, y, z); }

    public static ForgeDirection facing() {
        switch (MathHelper.floor_double(mc().thePlayer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3) {
            case 0: return ForgeDirection.SOUTH;
            case 1: return ForgeDirection.WEST;
            case 2: return ForgeDirection.NORTH;
            default: return ForgeDirection.EAST;
        }
    }

    private static ForgeDirection directionAway(Target target) {
        ForgeDirection direction = RebuildDirection.targeted(target.face(), facing(), target.hitX - target.x, target.hitY - target.y, target.hitZ - target.z);
        return direction == target.face() ? direction.getOpposite() : direction;
    }

    /** Sets a straight run of identical cells, stopping at a different state, region, layer range or the run limit. */
    private static boolean setRun(Owner start, int x, int y, int z, ForgeDirection direction, CellState state) throws IOException {
        CellState first = composed(start.world, x, y, z);
        Map<Cell, CellState> changes = new LinkedHashMap<>();
        Map<String, CellState> cache = new HashMap<>();
        changes.put(start.cell, state.transform(SourceBlockPosition.inverse(start.operations()), cache));
        for (int i = 0; i < MAX_RUN; i++) {
            x += direction.offsetX; y += direction.offsetY; z += direction.offsetZ;
            if (!inRange(x, y, z) || !first.same(composed(start.world, x, y, z))) break;
            Owner next = owner(start.world, x, y, z);
            if (next == null || next.world != start.world || !next.position.name().equals(start.position.name())) break;
            changes.put(next.cell, state.transform(SourceBlockPosition.inverse(next.operations()), cache));
        }
        apply(start.source, changes);
        return true;
    }

    interface CellRule { CellState apply(Block block, int meta); }
    interface RegionRule { CellRule create(RegionScope scope); }

    static final class RegionScope {
        final SubRegionPlacements.Region region;
        final List<String> operations;

        RegionScope(SubRegionPlacements.Region region, List<String> operations) { this.region = region; this.operations = operations; }
    }

    /** Applies a rule to every cell of the selected subregion, or of the whole selected placement, inside the layer range. */
    private static boolean bulk(Owner owner, RegionRule factory) throws IOException {
        SubRegionPlacements regions = owner.world.subregions();
        List<SubRegionPlacements.Region> scope = new ArrayList<>();
        if (regions.selected != null) scope.add(regions.get(regions.selected));
        else if (ClientProxy.schematic == owner.world) scope.addAll(regions.regions());
        else {
            message(EnumChatFormatting.GOLD, "litematica.message.warn.schematic_rebuild_placement_not_selected");
            return false;
        }
        return RebuildJobs.start(new RebuildJobs.Bulk(owner, scope, factory, RenderLayerSettings.RANGE.mode() != RenderLayerRange.Mode.ALL));
    }

    private static Map<Cell, CellState> single(Cell cell, CellState state) {
        Map<Cell, CellState> changes = new LinkedHashMap<>();
        changes.put(cell, state);
        return changes;
    }

    private static void apply(SchematicLibrary.Source<SchematicSourceData> source, Map<Cell, CellState> changes) throws IOException {
        RebuildJobs.Edits edits = new RebuildJobs.Edits();
        for (Map.Entry<Cell, CellState> entry : changes.entrySet()) edits.put(source, entry.getKey(), entry.getValue());
        edits.flush(true);
        edits.publish();
    }

    /** The state the held block item would place against the targeted face, or the picked primary block with an empty hand. */
    static CellState heldState(Target target) {
        EntityPlayer player = mc().thePlayer;
        ItemStack stack = player.getHeldItem();
        if (stack == null) {
            Block primary = ToolMode.REBUILD.getPrimaryBlock();
            return primary == null ? null : CellState.of(primary, ToolMode.REBUILD.getPrimaryMeta());
        }
        if (!(stack.getItem() instanceof ItemBlock)) return null;
        ItemBlock item = (ItemBlock) stack.getItem();
        int[] at = target.adjacent();
        float hx = (float) (target.hitX - target.x), hy = (float) (target.hitY - target.y), hz = (float) (target.hitZ - target.z);
        int meta = item.getMetadata(stack.getItemDamage());
        CellState.Scratch world = CellState.Scratch.create();
        double px = player.posX, py = player.posY, pz = player.posZ;
        try {
            meta = item.field_150939_a.onBlockPlaced(world, 0, 0, 0, target.side, hx, hy, hz, meta);
            player.posX = px - at[0]; player.posY = py - at[1]; player.posZ = pz - at[2];
            if (!item.placeBlockAt(stack.copy(), player, world, 0, 0, 0, target.side, hx, hy, hz, meta)) return CellState.of(item.field_150939_a, meta);
            CellState state = world.state();
            return state.isAir() ? CellState.of(item.field_150939_a, meta) : state;
        } catch (RuntimeException | LinkageError error) {
            Reference.logger.warn("Could not simulate placing {} into the schematic", item.field_150939_a, error);
            return CellState.of(item.field_150939_a, meta);
        } finally {
            player.posX = px; player.posY = py; player.posZ = pz;
        }
    }

    static void message(EnumChatFormatting color, String key, Object... arguments) {
        if (mc().thePlayer == null) return;
        ChatComponentTranslation text = new ChatComponentTranslation(key, arguments);
        text.getChatStyle().setColor(color);
        mc().thePlayer.addChatMessage(text);
    }
}
