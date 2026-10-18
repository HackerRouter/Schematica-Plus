package com.github.lunatrius.schematica.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class SharedPlacement {
    static final int MAX_TRANSFORMS = 64, MAX_SUBREGIONS = 65536;

    public final UUID id;
    public final String name, fileName, hash;
    public final long size;
    public final int dimension;
    public final int[] origin;
    public final List<String> transforms;
    public final String subregions;
    public final String ownerId, ownerName, modifierId, modifierName;
    public final int version;

    public SharedPlacement(UUID id, String name, String fileName, String hash, long size, int dimension, int[] origin, List<String> transforms,
        String subregions, String ownerId, String ownerName, String modifierId, String modifierName, int version) {
        this.id = id; this.name = name; this.fileName = fileName; this.hash = hash; this.size = size; this.dimension = dimension;
        this.origin = origin.clone(); this.transforms = Collections.unmodifiableList(new ArrayList<>(transforms));
        this.subregions = subregions == null ? "" : subregions;
        this.ownerId = ownerId; this.ownerName = ownerName; this.modifierId = modifierId; this.modifierName = modifierName; this.version = version;
    }

    public String extension() {
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    public CloudPath file() {
        return CloudPath.root(CloudPath.SHARED).child(hash + extension());
    }

    public SharedPlacement owned(UUID owner, String name) {
        return new SharedPlacement(id, this.name, fileName, hash, size, dimension, origin, transforms, subregions,
            owner.toString(), name, owner.toString(), name, version);
    }

    public SharedPlacement modified(SharedPlacement change, UUID modifier, String name) {
        return new SharedPlacement(id, this.name, fileName, hash, size, dimension, change.origin, change.transforms, change.subregions,
            ownerId, ownerName, modifier.toString(), name, version + 1);
    }

    public void validate() {
        if (name == null || name.isEmpty() || name.length() > 128 || !CloudPath.schematicName(fileName) || !hash.matches("[0-9a-f]{64}") || size < 0
            || origin.length != 3 || transforms.size() > MAX_TRANSFORMS || subregions.length() > MAX_SUBREGIONS) {
            throw new IllegalArgumentException("Invalid shared placement");
        }
        for (int value : origin) if (value < -30000000 || value > 30000000) throw new IllegalArgumentException("Invalid shared placement");
        for (String operation : transforms) {
            if (operation == null || operation.length() != 1 || "XYZxyz".indexOf(operation.charAt(0)) < 0) throw new IllegalArgumentException("Invalid shared placement");
        }
        if (!subregions.isEmpty()) subregionData();
    }

    public JsonObject subregionData() {
        return subregions.isEmpty() ? null : new JsonParser().parse(subregions).getAsJsonObject();
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Id", id.toString());
        tag.setString("Name", name);
        tag.setString("File", fileName);
        tag.setString("Hash", hash);
        tag.setLong("Size", size);
        tag.setInteger("Dimension", dimension);
        tag.setIntArray("Origin", origin);
        NBTTagList operations = new NBTTagList();
        for (String operation : transforms) operations.appendTag(new NBTTagString(operation));
        tag.setTag("Transforms", operations);
        tag.setString("Subregions", subregions);
        tag.setString("OwnerId", ownerId);
        tag.setString("Owner", ownerName);
        tag.setString("ModifierId", modifierId);
        tag.setString("Modifier", modifierName);
        tag.setInteger("Version", version);
        return tag;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("name", name);
        json.addProperty("file", fileName);
        json.addProperty("hash", hash);
        json.addProperty("size", size);
        json.addProperty("dimension", dimension);
        JsonArray position = new JsonArray();
        for (int value : origin) position.add(new JsonPrimitive(value));
        json.add("origin", position);
        JsonArray operations = new JsonArray();
        for (String operation : transforms) operations.add(new JsonPrimitive(operation));
        json.add("transforms", operations);
        json.addProperty("subregions", subregions);
        json.addProperty("ownerId", ownerId);
        json.addProperty("owner", ownerName);
        json.addProperty("modifierId", modifierId);
        json.addProperty("modifier", modifierName);
        json.addProperty("version", version);
        return json;
    }

    public static SharedPlacement fromJson(JsonObject json) {
        JsonArray position = json.getAsJsonArray("origin");
        int[] origin = new int[position.size()];
        for (int i = 0; i < origin.length; i++) origin[i] = position.get(i).getAsInt();
        List<String> transforms = new ArrayList<>();
        for (JsonElement operation : json.getAsJsonArray("transforms")) transforms.add(operation.getAsString());
        SharedPlacement placement = new SharedPlacement(UUID.fromString(json.get("id").getAsString()), json.get("name").getAsString(),
            json.get("file").getAsString(), json.get("hash").getAsString(), json.get("size").getAsLong(), json.get("dimension").getAsInt(), origin,
            transforms, json.get("subregions").getAsString(), json.get("ownerId").getAsString(), json.get("owner").getAsString(),
            json.get("modifierId").getAsString(), json.get("modifier").getAsString(), json.get("version").getAsInt());
        placement.validate();
        return placement;
    }

    public static SharedPlacement read(NBTTagCompound tag) {
        NBTTagList operations = tag.getTagList("Transforms", Constants.NBT.TAG_STRING);
        if (operations.tagCount() > MAX_TRANSFORMS) throw new IllegalArgumentException("Invalid shared placement");
        List<String> transforms = new ArrayList<>();
        for (int i = 0; i < operations.tagCount(); i++) transforms.add(operations.getStringTagAt(i));
        SharedPlacement placement = new SharedPlacement(UUID.fromString(tag.getString("Id")), tag.getString("Name"), tag.getString("File"),
            tag.getString("Hash"), tag.getLong("Size"), tag.getInteger("Dimension"), tag.getIntArray("Origin"), transforms,
            tag.getString("Subregions"), tag.getString("OwnerId"), tag.getString("Owner"), tag.getString("ModifierId"), tag.getString("Modifier"),
            tag.getInteger("Version"));
        placement.validate();
        return placement;
    }
}
