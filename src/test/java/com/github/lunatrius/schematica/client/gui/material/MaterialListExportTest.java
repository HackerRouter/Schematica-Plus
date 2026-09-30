package com.github.lunatrius.schematica.client.gui.material;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class MaterialListExportTest {
    static String label(String key) {
        return label("en_US", key);
    }

    static String label(String language, String key) {
        String domain = key.startsWith("schematica.") ? "schematica" : "schematica_plus_litematica";
        try (java.io.InputStream stream = MaterialListExportTest.class.getResourceAsStream("/assets/" + domain + "/lang/" + language + ".lang")) {
            String value = net.minecraft.util.StringTranslate.parseLangFile(stream).get(key);
            assertNotNull(key, value);
            return value;
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void localizesHumanReadableExportsAndKeepsJsonFieldsStable() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(java.util.Collections.singletonList(new MaterialListModel.Entry<>("stone", "石头", "minecraft:stone", 12, 0, 0, 0)));
        java.util.function.Function<String, String> chinese = key -> label("zh_CN", key);
        for (MaterialListExport.Format format : MaterialListExport.Format.values()) {
            String text = MaterialListExport.format(model, "测试", format, value -> "{}", chinese);
            text = MaterialListExport.withAnalysisStatus(text, format, 2, 1, chinese);
            if (format == MaterialListExport.Format.JSON) {
                com.google.gson.JsonObject json = new JsonParser().parse(text).getAsJsonObject();
                assertEquals(2, json.get("unverified_positions").getAsInt());
                assertEquals("minecraft:stone", json.getAsJsonArray("items").get(0).getAsJsonObject().get("item").getAsString());
            } else {
                assertTrue(text.contains(chinese.apply("litematica.gui.label.material_list.title.total")));
                assertTrue(text.contains("未确认的位置"));
                assertTrue(text.contains("跳过的位置"));
                assertFalse(text.contains("Unverified positions"));
            }
        }
        String csv = MaterialListExport.format(model, "测试", MaterialListExport.Format.CSV, value -> "{}", key -> "a,\"b\"");
        assertTrue(csv.startsWith("\"a,\"\"b\"\"\","));
    }

    @Test public void exportsOnlyDisplayedEntriesWithEscapedNamesAndLongCounts() throws Exception {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(Arrays.asList(new MaterialListModel.Entry<>("a", "Pipe, \"铜\"", "mod:pipe", 2000000000, 1, 0, 0),
            new MaterialListModel.Entry<>("b", "Hidden", "mod:hidden", 4, 4, 0, 0)));
        model.ignore("b");
        model.setMultiplier(3);
        String csv = MaterialListExport.format(model, "Test", MaterialListExport.Format.CSV, key -> key, MaterialListExportTest::label);
        assertTrue(csv.contains("\"Pipe, \"\"铜\"\"\",\"6000000000\",\"6000000000\",\"0\""));
        assertFalse(csv.contains("Hidden"));
        String json = MaterialListExport.format(model, "Test", MaterialListExport.Format.JSON, key -> "{Variant:1}", MaterialListExportTest::label);
        assertEquals(6000000000L, new JsonParser().parse(json).getAsJsonObject().getAsJsonArray("items")
            .get(0).getAsJsonObject().get("total").getAsLong());
        Path first = MaterialListExport.write(temporary.getRoot().toPath(), MaterialListExport.Format.CSV, csv);
        Path second = MaterialListExport.write(temporary.getRoot().toPath(), MaterialListExport.Format.CSV, "second");
        assertNotEquals(first, second);
        assertEquals(csv, new String(Files.readAllBytes(first), StandardCharsets.UTF_8));
    }
}
