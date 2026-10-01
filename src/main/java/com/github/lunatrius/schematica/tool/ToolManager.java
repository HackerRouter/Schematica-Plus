// SPDX-License-Identifier: LGPL-3.0-only
// Litematica tool input, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.selection.SelectionRayTrace;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class ToolManager {
    public static final ToolManager INSTANCE = new ToolManager();
    private static ToolMode currentMode = ToolMode.AREA_SELECTION;
    private static Area grabbed;
    private static Object grabWorld;
    private static Vector3i grabPoint;
    private static double grabDistance;

    private ToolManager() {}
    private static Minecraft mc() { return Minecraft.getMinecraft(); }
    private static EntityLivingBase camera() { return mc().renderViewEntity == null ? mc().thePlayer : mc().renderViewEntity; }
    public static boolean isHoldingToolItem() {
        ItemStack held = mc().thePlayer == null ? null : mc().thePlayer.getHeldItem();
        return held != null && held.getItem() == ConfigurationHandler.toolItemType
            && (ConfigurationHandler.toolItemMeta < 0 || held.getItemDamage() == ConfigurationHandler.toolItemMeta);
    }
    public static boolean toolActive() { return ConfigurationHandler.toolItemEnabled && isHoldingToolItem(); }
    public static ToolMode getCurrentMode() { return currentMode; }
    public static void setCurrentMode(ToolMode mode) { releaseGrab(); currentMode = mode == null ? ToolMode.AREA_SELECTION : mode; }
    public void cycleMode() { cycleMode(true); }
    public static void cycleMode(boolean forward) { setCurrentMode(currentMode.cycle(forward)); WorldHandler.INSTANCE.saveSession(); }
    public static boolean currentModeUsesSchematic() { return currentMode.getUsesSchematic(); }
    public static boolean currentModeUsesAreaSelection() { return currentMode.getUsesAreaSelection(); }

    public static void toggleConfig(String name) {
        net.minecraftforge.common.config.Property p = ConfigurationHandler.configuration.get("tool", name, true);
        p.set(!p.getBoolean(true)); ConfigurationHandler.configuration.save();
        if (name.equals("toolItemEnabled")) ConfigurationHandler.toolItemEnabled = p.getBoolean(true);
        if (name.equals("pickBlockEnabled")) ConfigurationHandler.pickBlockEnabled = p.getBoolean(true);
    }

    public static boolean hotkey(String id) {
        if (mc().thePlayer == null) return false;
        if (id.equals("toolEnabledToggle")) { toggleConfig("toolItemEnabled"); return true; }
        if (id.equals("toolSelectElements") && ConfigurationHandler.toolItemEnabled) {
            if (currentMode.getUsesBlockPrimary() && Hotkeys.held("toolSelectModifierBlock1")) { pickState(true); return true; }
            if (currentMode.getUsesBlockSecondary() && Hotkeys.held("toolSelectModifierBlock2")) { pickState(false); return true; }
        }
        if (id.equals("toolPlaceCorner1") || id.equals("toolPlaceCorner2")) {
            if (!toolActive()) return false;
            placeCorner(id.equals("toolPlaceCorner1")); return true;
        }
        if (id.equals("toolSelectElements")) {
            if (!toolActive()) return false;
            if (currentMode.getUsesAreaSelection()) {
                if (Hotkeys.held("selectionGrabModifier")) toggleGrab();
                else selectAreaElement(mc().thePlayer);
            } else selectPlacement();
            return true;
        }
        if (id.equals("nudgeSelectionPositive") || id.equals("nudgeSelectionNegative")) { nudge(id.endsWith("Positive") ? 1 : -1); return true; }
        return ToolSelectionActions.hotkey(id);
    }

    public static boolean scroll(int wheel) {
        if (!toolActive() || mc().thePlayer == null) return false;
        int amount = wheel > 0 ? 1 : -1;
        Area area = AreaSelections.library().selected();
        if (Hotkeys.held("selectionGrabModifier") && currentMode.getUsesAreaSelection()) {
            if (grabbed != null) { grabDistance = Math.max(0, grabDistance + amount); tick(); return true; }
            if (area != null && area.originSelected()) { moveEntire(area, direction(amount)); return true; }
        }
        if (Hotkeys.held("selectionGrowModifier") && currentMode.getUsesAreaSelection()) {
            if (area != null && area.selectedBox() != null) {
                AreaSelections.capture(); AreaSelections.library().growSelected(area, amount); saveArea();
            }
            return true;
        }
        if (Hotkeys.held("selectionNudgeModifier")) { nudge(amount); return true; }
        if (Hotkeys.held("operationModeChangeModifier")) { cycleMode(amount < 0); return true; }
        return false;
    }

    static Vector3i direction(int amount) {
        Vec3 look = camera().getLook(1);
        double x = Math.abs(look.xCoord), y = Math.abs(look.yCoord), z = Math.abs(look.zCoord);
        if (y >= x && y >= z) return new Vector3i(0, look.yCoord < 0 ? -amount : amount, 0);
        return x >= z ? new Vector3i(look.xCoord < 0 ? -amount : amount, 0, 0) : new Vector3i(0, 0, look.zCoord < 0 ? -amount : amount);
    }
    public static void nudgeArea(EntityPlayer player, int amount) { nudge(amount); }
    static void nudge(int amount) {
        Vector3i delta = direction(amount);
        if (currentMode.getUsesAreaSelection()) {
            Area area = AreaSelections.library().selected();
            if (area == null || !SchematicaPlus.proxy.isSaveEnabled || !area.originSelected() && area.selectedBox() == null) return;
            AreaSelections.capture(); AreaSelections.library().moveSelected(area, delta.x, delta.y, delta.z); saveArea();
        } else {
            SchematicWorld world = ClientProxy.schematic;
            if (world == null) return;
            String region = world.subregions() == null ? null : world.subregions().selected;
            SchematicOrigin origin = region == null ? world.originPosition() : world.subregionPosition(region);
            movePlacement(new Vector3i(Math.addExact(origin.x, delta.x), Math.addExact(origin.y, delta.y), Math.addExact(origin.z, delta.z)));
        }
    }
    static void moveEntire(Area area, Vector3i delta) {
        if (!SchematicaPlus.proxy.isSaveEnabled) return;
        AreaSelections.capture(); AreaSelections.library().moveEntire(area, delta.x, delta.y, delta.z); saveArea();
    }
    static void saveArea() { AreaSelections.apply(); AreaSelections.saveCurrent(); }
    static Vector3i playerPosition() { return new Vector3i(MathHelper.floor_double(mc().thePlayer.posX), MathHelper.floor_double(mc().thePlayer.boundingBox.minY), MathHelper.floor_double(mc().thePlayer.posZ)); }
    static MovingObjectPosition trace(double distance) {
        Vec3 eye = camera().getPosition(1), look = camera().getLook(1);
        return mc().theWorld.rayTraceBlocks(eye, eye.addVector(look.xCoord * distance, look.yCoord * distance, look.zCoord * distance));
    }
    private static Vector3i target(boolean area) {
        MovingObjectPosition hit = trace(200);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        Vector3i point = new Vector3i(hit.blockX, hit.blockY, hit.blockZ);
        if (mc().thePlayer.isSneaking() == area) {
            ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit); point.add(side.offsetX, side.offsetY, side.offsetZ);
        }
        return point;
    }
    private static void placeCorner(boolean first) {
        Vector3i point = target(currentMode.getUsesAreaSelection());
        if (point == null) return;
        if (currentMode.getUsesAreaSelection()) {
            if (!SchematicaPlus.proxy.isSaveEnabled) return;
            Area area = AreaSelections.library().selected();
            if (area == null || !area.originSelected() && area.selectedBox() == null) return;
            AreaSelections.capture();
            if (area.originSelected() && Hotkeys.held("selectionGrabModifier")) {
                Vector3i old = area.origin(); AreaSelections.library().moveEntire(area, point.x - old.x, point.y - old.y, point.z - old.z);
            } else AreaSelections.library().click(area, first, point);
            AreaSelections.library().setGuide(area, true); saveArea();
        } else movePlacement(point);
    }
    private static void movePlacement(Vector3i point) {
        SchematicWorld world = ClientProxy.schematic;
        if (world == null || world.placementSettings().locked) return;
        String region = world.subregions() == null ? null : world.subregions().selected;
        if (region == null) world.moveOriginTo(point.x, point.y, point.z);
        else world.moveSubregionTo(region, point.x, point.y, point.z);
        WorldHandler.INSTANCE.saveSession();
    }
    public void onToolUse(EntityPlayer player, SchematicWorld schematic) { placeCorner(false); }
    public void onToolAttack(EntityPlayer player, SchematicWorld schematic) { placeCorner(true); }

    private static SelectionRayTrace.Hit areaHit() {
        Vec3 eyes = camera().getPosition(1), look = camera().getLook(1);
        MovingObjectPosition block = trace(200);
        double distance = block == null ? 200 : eyes.distanceTo(block.hitVec) + 0.001;
        return SelectionRayTrace.trace(AreaSelections.library().selected(), eyes.xCoord, eyes.yCoord, eyes.zCoord,
            look.xCoord, look.yCoord, look.zCoord, distance);
    }
    public static void selectAreaElement(EntityPlayer player) {
        if (!SchematicaPlus.proxy.isSaveEnabled) return;
        AreaSelectionLibrary library = AreaSelections.library(); Area area = library.selected();
        if (area == null || !area.guide()) return;
        AreaSelections.capture(); SelectionRayTrace.Hit hit = areaHit();
        if (hit != null && hit.box == null) library.selectOrigin(area, true);
        else if (hit != null) library.selectCorner(area, hit.box, hit.corner);
        else if (trace(200) == null) library.selectBox(area, null);
        saveArea();
    }
    private static void selectPlacement() {
        SchematicWorld previous = ClientProxy.schematic, best = null; String regionName = null;
        Vec3 eye = camera().getPosition(1), look = camera().getLook(1);
        MovingObjectPosition block = trace(200);
        double distance = block == null ? 200 : eye.distanceTo(block.hitVec) + 0.001;
        for (SchematicWorld world : ClientProxy.loadedSchematics) {
            if (!world.isRenderingEnabled()) continue;
            if (world.subregions() != null) for (SubRegionPlacements.Region region : world.subregions().regions()) {
                if (!region.enabled) continue;
                SchematicRegion box = world.subregionBounds(region.name());
                double hit = SelectionRayTrace.boxDistance(eye.xCoord, eye.yCoord, eye.zCoord, look.xCoord, look.yCoord, look.zCoord,
                    new Vector3i(box.minX, box.minY, box.minZ), new Vector3i(box.maxX, box.maxY, box.maxZ), distance);
                if (hit <= distance) { distance = hit; best = world; regionName = region.name(); }
            }
            SchematicOrigin origin = world.originPosition(); Vector3i p = new Vector3i(origin.x, origin.y, origin.z);
            double hit = SelectionRayTrace.boxDistance(eye.xCoord, eye.yCoord, eye.zCoord, look.xCoord, look.yCoord, look.zCoord, p, p, distance);
            if (hit <= distance) { distance = hit; best = world; regionName = null; }
        }
        if (previous != null && previous.subregions() != null) previous.selectSubregion(null);
        if (best != null || block == null) {
            ClientProxy.selectSchematic(best);
            if (best != null && best.subregions() != null) best.selectSubregion(Hotkeys.held("selectionGrabModifier") ? regionName : null);
            WorldHandler.INSTANCE.saveSession();
        }
    }
    private static void pickState(boolean primary) {
        MovingObjectPosition hit = trace(200); Block block = Blocks.air; int meta = 0;
        Vec3 eye = camera().getPosition(1), look = camera().getLook(1);
        double distance = hit == null ? 200 : eye.distanceTo(hit.hitVec);
        if (hit != null) { block = mc().theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ); meta = mc().theWorld.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ); }
        for (SchematicWorld world : ClientProxy.loadedSchematics) {
            if (!world.isRenderingEnabled()) continue;
            Vec3 start = eye.addVector(-world.position.x, -world.position.y, -world.position.z);
            MovingObjectPosition candidate = world.rayTraceRendered(start, start.addVector(look.xCoord * distance, look.yCoord * distance, look.zCoord * distance));
            if (candidate != null && candidate.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                double d = start.distanceTo(candidate.hitVec);
                if (d < distance) { distance = d; block = world.getBlock(candidate.blockX, candidate.blockY, candidate.blockZ); meta = world.getBlockMetadata(candidate.blockX, candidate.blockY, candidate.blockZ); }
            }
        }
        if (primary) currentMode.setPrimaryBlock(block, meta); else currentMode.setSecondaryBlock(block, meta);
        WorldHandler.INSTANCE.saveSession();
    }
    private static void toggleGrab() {
        if (grabbed != null) { releaseGrab(); return; }
        SelectionRayTrace.Hit hit = areaHit();
        if (hit == null || hit.box == null || !SchematicaPlus.proxy.isSaveEnabled) return;
        selectAreaElement(mc().thePlayer); grabbed = AreaSelections.library().selected(); grabWorld = mc().theWorld;
        grabDistance = hit.distance; grabPoint = grabPosition();
    }
    private static Vector3i grabPosition() {
        Vec3 eye = camera().getPosition(1), look = camera().getLook(1);
        return new Vector3i(MathHelper.floor_double(eye.xCoord + look.xCoord * grabDistance), MathHelper.floor_double(eye.yCoord + look.yCoord * grabDistance), MathHelper.floor_double(eye.zCoord + look.zCoord * grabDistance));
    }
    private static void releaseGrab() { if (grabbed != null && grabWorld == mc().theWorld) AreaSelections.saveCurrent(); grabbed = null; grabWorld = null; grabPoint = null; }
    public static void tick() {
        if (grabbed == null) return;
        if (mc().theWorld != grabWorld || mc().currentScreen != null || !toolActive() || grabbed != AreaSelections.library().selected() || !org.lwjgl.opengl.Display.isActive()) { releaseGrab(); return; }
        Vector3i next = grabPosition();
        if (!next.equals(grabPoint)) {
            try {
                AreaSelections.library().moveSelected(grabbed, next.x - grabPoint.x, next.y - grabPoint.y, next.z - grabPoint.z);
                AreaSelections.apply(); grabPoint = next;
            } catch (IllegalArgumentException | ArithmeticException error) { releaseGrab(); }
        }
    }
}
