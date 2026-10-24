package com.github.lunatrius.schematica.client.verifier;

import java.io.File;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import cpw.mods.fml.common.registry.GameData;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.util.BlockGroups;
import com.github.lunatrius.schematica.world.storage.Schematic;

public final class VerifierSmoke implements AutoCloseable {
    private final Minecraft mc;
    private final SchematicWorld placement;
    private final VerificationManager.Session session;
    private final int x, y, z;
    private int stage, pausedTicks;

    public VerifierSmoke(Minecraft mc, int x, int y, int z) throws Exception {
        this.mc = mc; this.x = x; this.y = y; this.z = z;
        rules();
        Schematic source = new Schematic(null, 48, 32, 1);
        for (int sx = 0; sx < 48; sx++) for (int sy = 0; sy < 32; sy++) source.setBlock(sx, sy, 0, Blocks.stone, 0);
        placement = new SchematicWorld(source);
        placement.name = "Verifier Smoke";
        placement.position.set(x, y, z);
        Placements.LIBRARY.create(Placements.LIBRARY.addMemory(new File("verifier-smoke.schemplus"), null), (data, previous) -> placement);
        mc.theWorld.setBlock(x, y, z, Blocks.stone, 0, 0);
        session = VerificationManager.INSTANCE.get(placement);
        session.start(mc);
    }

    public boolean tick() {
        VerificationManager.INSTANCE.tick(mc);
        if (stage == 3) {
            check(!session.running() && session.scan().checked() == 0, "paused results remain unapplied");
            if (++pausedTicks < 3) return false;
            session.start(mc); stage = 4;
            return false;
        }
        if (stage == 5) return true;
        if (session.scan() == null || !session.scan().done()) return false;
        if (stage == 0) {
            check(session.scan().count(VerificationScan.Type.CORRECT) == 1, "native correct count");
            check(session.scan().count(VerificationScan.Type.MISSING) == 1535, "native missing count");
            mc.theWorld.setBlock(x, y, z, Blocks.dirt, 0, 0);
            VerificationManager.INSTANCE.changed(x, y, z, x, y, z);
            stage = 1;
        } else if (stage == 1) {
            check(session.scan().count(VerificationScan.Type.WRONG_BLOCK) == 1, "native incremental mismatch");
            session.reset(); session.start(mc);
            VerificationManager.INSTANCE.tick(mc);
            placement.moveOriginTo(x + 1, y, z);
            VerificationManager.INSTANCE.tick(mc);
            check(session.scan() == null, "movement rejects pending verification");
            session.start(mc);
            VerificationManager.INSTANCE.tick(mc);
            session.pause();
            session.reset(); session.start(mc);
            VerificationManager.INSTANCE.tick(mc);
            session.pause();
            stage = 3;
        } else if (stage == 4) {
            check(session.scan().count(VerificationScan.Type.MISSING) == 1536, "resume uses the current origin");
            placement.setBlock(0, 0, 0, Blocks.glass, 0, 0);
            VerificationManager.INSTANCE.tick(mc);
            check(session.scan() == null, "source edit invalidates verification");
            session.start(mc); VerificationManager.INSTANCE.tick(mc);
            VerificationManager.INSTANCE.clear();
            check(session.scan() == null && VerificationManager.INSTANCE.sessions().isEmpty(), "world cleanup cancels outstanding work");
            System.out.println("RENDER_SMOKE_VERIFIER: native counts, incremental changes, rules parity, movement/edit cancellation, pause/resume, cleanup");
            stage = 5;
        }
        return stage == 5;
    }

    private void rules() {
        boolean different = BlockGroups.enabled, age = VisualSettings.ignoreCropAge, survival = VisualSettings.ignoreSurvivalStates;
        Block[] blocks = {Blocks.stone, Blocks.dirt, Blocks.wool, Blocks.oak_stairs, Blocks.brick_stairs,
            Blocks.wheat, Blocks.redstone_ore, Blocks.lit_redstone_ore, Blocks.wooden_slab, Blocks.air};
        try {
            for (int flags = 0; flags < 8; flags++) {
                BlockGroups.enabled = (flags & 1) != 0;
                VisualSettings.ignoreCropAge = (flags & 2) != 0;
                VisualSettings.ignoreSurvivalStates = (flags & 4) != 0;
                VerificationRules rules = new VerificationRules();
                for (Block a : blocks) for (Block b : blocks) for (int metadata : new int[] {0, 1, 3, 8, 15}) {
                    VerificationScan.State expected = state(a, metadata), found = state(b, 0);
                    check(VerificationScan.classify(expected, found) == rules.classify(expected, found,
                        BlockGroups.snapshot(expected.block), BlockGroups.snapshot(found.block)), "snapshot rules match native rules");
                }
            }
        } finally {
            BlockGroups.enabled = different; VisualSettings.ignoreCropAge = age; VisualSettings.ignoreSurvivalStates = survival;
        }
    }

    private VerificationScan.State state(Block block, int metadata) {
        return block == Blocks.air ? VerificationScan.State.AIR : new VerificationScan.State(GameData.getBlockRegistry().getNameForObject(block), metadata);
    }

    @Override public void close() {
        VerificationManager.INSTANCE.clear();
        Placements.LIBRARY.remove(placement);
        mc.theWorld.setBlockToAir(x, y, z);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
