package com.github.lunatrius.schematica.network;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.network.message.MessageDownloadBegin;
import com.github.lunatrius.schematica.network.transfer.DownloadGeometry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadGeometryTest {
    private ISchematic schematic(DownloadGeometry geometry) {
        return (ISchematic) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getWidth": return 1024;
                    case "getHeight": return 4;
                    case "getLength": return 4;
                    case "getOrigin": return geometry.origin;
                    case "getRegions": return geometry.regions;
                    case "getIcon": return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    @Test public void maximumRegionSetPreservesUnicodeNamesBoundsAndOutsideOrigin() {
        List<SchematicRegion> regions = new ArrayList<>();
        String prefix = String.join("", Collections.nCopies(190, "区"));
        for (int i = 0; i < 256; i++) regions.add(new SchematicRegion(prefix + i, i * 4, 1, 1, i * 4 + 1, 2, 2));
        DownloadGeometry sent = new DownloadGeometry(new SchematicOrigin(-40, 90, 7), regions);
        sent.validate(1024, 4, 4);
        regions.clear();
        ByteBuf buf = Unpooled.buffer();
        try {
            sent.write(buf);
            DownloadGeometry received = DownloadGeometry.read(buf);
            received.validate(1024, 4, 4);
            assertFalse(buf.isReadable());
            assertEquals(256, received.regions.size());
            assertArrayEquals(sent.origin.coordinates(), received.origin.coordinates());
            for (int i = 0; i < 256; i++) {
                SchematicRegion a = sent.regions.get(i), b = received.regions.get(i);
                assertEquals(a.name, b.name);
                assertArrayEquals(new int[] {a.minX, a.minY, a.minZ, a.maxX, a.maxY, a.maxZ},
                    new int[] {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ});
            }
            assertThrows(UnsupportedOperationException.class, () -> received.regions.clear());
        } finally { buf.release(); }
    }

    @Test public void beginPacketCarriesGeometry() {
        DownloadGeometry geometry = new DownloadGeometry(new SchematicOrigin(5, -7, 9),
            Collections.singletonList(new SchematicRegion("part", 1, 1, 1, 2, 2, 2)));
        ByteBuf buf = Unpooled.buffer();
        try {
            new MessageDownloadBegin(schematic(geometry)).toBytes(buf);
            MessageDownloadBegin received = new MessageDownloadBegin();
            received.fromBytes(buf);
            assertEquals(1024, received.width);
            assertEquals(4, received.height);
            assertEquals(4, received.length);
            assertArrayEquals(geometry.origin.coordinates(), received.geometry.origin.coordinates());
            assertEquals("part", received.geometry.regions.get(0).name);
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }

    @Test public void invalidRegionBoundsNamesAndAllocationSizesAreRejected() {
        SchematicRegion a = new SchematicRegion("part", 0, 0, 0, 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> new DownloadGeometry(SchematicOrigin.ZERO,
            Arrays.asList(a, a)).validate(4, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> new DownloadGeometry(SchematicOrigin.ZERO,
            Collections.singletonList(a)).validate(1, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> new DownloadGeometry(SchematicOrigin.ZERO,
            Collections.singletonList(a.offset(-1, 0, 0))).validate(4, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.EMPTY.validate(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.EMPTY.validate(32767, 32767, 32767));
        assertThrows(IllegalArgumentException.class, () -> new DownloadGeometry(SchematicOrigin.ZERO, Collections.nCopies(257, a)));
    }

    @Test public void truncatedAndUnknownHeadersFailBeforeAcceptance() {
        DownloadGeometry geometry = new DownloadGeometry(SchematicOrigin.ZERO,
            Collections.singletonList(new SchematicRegion("part", 0, 0, 0, 1, 1, 1)));
        ByteBuf buf = Unpooled.buffer();
        try {
            geometry.write(buf);
            for (int size = 1; size < buf.readableBytes(); size++) {
                ByteBuf truncated = buf.slice(0, size);
                assertThrows(RuntimeException.class, () -> DownloadGeometry.read(truncated));
            }
            buf.setByte(0, 2);
            assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.read(buf.duplicate()));
            buf.setByte(0, 1).setShort(13, 257);
            assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.read(buf.duplicate()));
            buf.setShort(13, 1).setShort(15, 801);
            assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.read(buf.duplicate()));
            buf.setShort(15, 0);
            assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.read(buf.duplicate()));
            buf.setShort(15, 4).writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> DownloadGeometry.read(buf.duplicate()));
        } finally { buf.release(); }
    }
}
