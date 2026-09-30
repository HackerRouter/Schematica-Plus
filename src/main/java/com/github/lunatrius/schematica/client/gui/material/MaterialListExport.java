package com.github.lunatrius.schematica.client.gui.material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class MaterialListExport {
    public enum Format { TXT, CSV, JSON }

    private MaterialListExport() {}

    public static <T> String format(MaterialListModel<T> model, String title, Format format, Function<T, String> variant) {
        List<MaterialListModel.Entry<T>> entries = model.visible();
        if (format == Format.JSON) {
            JsonObject root = new JsonObject();
            root.addProperty("format", "schematica_plus:material_list");
            root.addProperty("version", 1);
            root.addProperty("title", title);
            root.addProperty("multiplier", model.multiplier());
            JsonArray items = new JsonArray();
            for (MaterialListModel.Entry<T> entry : entries) {
                JsonObject item = new JsonObject();
                item.addProperty("item", entry.registryName);
                item.addProperty("name", entry.name);
                item.addProperty("variant", variant.apply(entry.key));
                item.addProperty("total", model.total(entry));
                item.addProperty("missing", model.missing(entry));
                item.addProperty("available", entry.available);
                item.addProperty("unverified_positions", entry.unknown);
                items.add(item);
            }
            root.add("items", items);
            return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Item", "Total (x" + model.multiplier() + ")", "Missing", "Available"});
        for (MaterialListModel.Entry<T> entry : entries) {
            rows.add(new String[] {plain(entry.name), Long.toString(model.total(entry)), Long.toString(model.missing(entry)),
                Long.toString(entry.available)});
        }
        StringBuilder output = new StringBuilder();
        if (format == Format.CSV) {
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) output.append(',');
                    output.append('"').append(row[i].replace("\"", "\"\"")).append('"');
                }
                output.append('\n');
            }
        } else {
            int[] widths = new int[4];
            for (String[] row : rows) for (int i = 0; i < row.length; i++) widths[i] = Math.max(widths[i], row[i].length());
            output.append(plain(title)).append('\n');
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) output.append(" | ");
                    if (i == 0) output.append(row[i]);
                    for (int pad = row[i].length(); pad < widths[i]; pad++) output.append(' ');
                    if (i != 0) output.append(row[i]);
                }
                output.append('\n');
            }
        }
        return output.toString();
    }

    private static String plain(String text) { return text.replaceAll("§.", "").replaceAll("[\\r\\n\\t]", " "); }

    public static String withAnalysisStatus(String text, Format format, int unverified, int skipped) {
        if (format == Format.JSON) {
            JsonObject data = new com.google.gson.JsonParser().parse(text).getAsJsonObject();
            data.addProperty("source", "area_analysis");
            data.addProperty("unverified_positions", unverified);
            data.addProperty("skipped_positions", skipped);
            data.addProperty("complete", unverified == 0 && skipped == 0);
            return new GsonBuilder().setPrettyPrinting().create().toJson(data) + "\n";
        }
        if (format == Format.CSV) {
            String[] rows = text.split("\n");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < rows.length; i++) result.append(rows[i]).append(i == 0
                ? ",\"Unverified positions\",\"Skipped positions\"" : "," + unverified + "," + skipped).append('\n');
            return result.toString();
        }
        return "Area analysis: unverified positions=" + unverified + ", skipped positions=" + skipped + "\n" + text;
    }

    public static Path write(Path directory, Format format, String content) throws IOException {
        Files.createDirectories(directory);
        Path output = Files.createTempFile(directory, "schematica_plus-materials-", "." + format.name().toLowerCase(java.util.Locale.ROOT));
        try {
            Files.write(output, content.getBytes(StandardCharsets.UTF_8));
            return output;
        } catch (IOException e) {
            Files.deleteIfExists(output);
            throw e;
        }
    }
}
