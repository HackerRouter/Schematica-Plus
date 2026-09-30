package com.github.lunatrius.schematica.world.schematic;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BlockIdCodecTest {
    @Test public void preservesEveryUnsignedShortIncludingMultiplesOf256() {
        for (int id = 0; id <= 65535; id++) {
            assertEquals(id, BlockIdCodec.decode(BlockIdCodec.low(id), BlockIdCodec.high(id)));
        }
    }
}
