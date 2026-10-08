// Server placement capability regression coverage, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.proxy.ServerProxy;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccuratePlacementClientTest {
    private static final class Stairs extends BlockStairs { Stairs() { super(new Block(Material.wood) {}, 0); } }

    @Test public void handshakeDoesNotSkipClientOrientationForUnsupportedBlocks() {
        CommonProxy previous = SchematicaPlus.proxy;
        String protocol = ConfigurationHandler.easyPlaceProtocolVersion;
        try {
            SchematicaPlus.proxy = new ServerProxy();
            SchematicaPlus.proxy.supportsAccuratePlacement = true;
            ConfigurationHandler.easyPlaceProtocolVersion = "auto";
            assertTrue(AccuratePlacementClient.active(new Stairs()));
            assertFalse(AccuratePlacementClient.active(new Block(Material.rock) {}));
            ConfigurationHandler.easyPlaceProtocolVersion = "none";
            assertFalse(AccuratePlacementClient.active(new Stairs()));
            ConfigurationHandler.easyPlaceProtocolVersion = "auto";
            SchematicaPlus.proxy.supportsAccuratePlacement = false;
            assertFalse(AccuratePlacementClient.active(new Stairs()));
        } finally {
            SchematicaPlus.proxy = previous;
            ConfigurationHandler.easyPlaceProtocolVersion = protocol;
        }
    }
}
