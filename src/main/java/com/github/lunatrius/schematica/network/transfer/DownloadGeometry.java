package com.github.lunatrius.schematica.network.transfer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.SchematicLimits;

import io.netty.buffer.ByteBuf;

public final class DownloadGeometry {
    public static final DownloadGeometry LEGACY = new DownloadGeometry(SchematicOrigin.ZERO, Collections.emptyList());

    public final SchematicOrigin origin;
    public final List<SchematicRegion> regions;

    public DownloadGeometry(SchematicOrigin origin, List<SchematicRegion> regions) {
        if (origin == null || regions == null || regions.size() > 256) throw new IllegalArgumentException("Invalid download geometry");
        this.origin = origin;
        this.regions = Collections.unmodifiableList(new ArrayList<>(regions));
    }

    public static DownloadGeometry of(ISchematic schematic) {
        return new DownloadGeometry(schematic.getOrigin(), schematic.getRegions());
    }

    public boolean requiresSupport() {
        return !origin.isZero() || !regions.isEmpty();
    }

    public void validate(int width, int height, int length) {
        SchematicLimits.volume(width, height, length);
        Set<String> names = new HashSet<>();
        for (SchematicRegion region : regions) {
            if (region == null || !names.add(region.name) || region.minX < 0 || region.minY < 0 || region.minZ < 0
                || region.maxX >= width || region.maxY >= height || region.maxZ >= length) {
                throw new IllegalArgumentException("Invalid download region");
            }
        }
    }

    public static DownloadGeometry read(ByteBuf buf) {
        if (!buf.isReadable()) return LEGACY;
        if (buf.readUnsignedByte() != 1) throw new IllegalArgumentException("Unsupported download geometry");
        SchematicOrigin origin = new SchematicOrigin(buf.readInt(), buf.readInt(), buf.readInt());
        int count = buf.readUnsignedShort();
        if (count > 256) throw new IllegalArgumentException("Too many download regions");
        List<SchematicRegion> regions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int size = buf.readUnsignedShort();
            if (size < 1 || size > 800 || size > buf.readableBytes() - 24) throw new IllegalArgumentException("Invalid download region name");
            byte[] name = new byte[size];
            buf.readBytes(name);
            regions.add(new SchematicRegion(new String(name, StandardCharsets.UTF_8), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt()));
        }
        if (buf.isReadable()) throw new IllegalArgumentException("Trailing download geometry");
        return new DownloadGeometry(origin, regions);
    }

    public void write(ByteBuf buf) {
        buf.writeByte(1);
        buf.writeInt(origin.x).writeInt(origin.y).writeInt(origin.z);
        buf.writeShort(regions.size());
        for (SchematicRegion region : regions) {
            byte[] name = region.name.getBytes(StandardCharsets.UTF_8);
            if (name.length < 1 || name.length > 800) throw new IllegalArgumentException("Invalid download region name");
            buf.writeShort(name.length).writeBytes(name);
            buf.writeInt(region.minX).writeInt(region.minY).writeInt(region.minZ);
            buf.writeInt(region.maxX).writeInt(region.maxY).writeInt(region.maxZ);
        }
    }
}
