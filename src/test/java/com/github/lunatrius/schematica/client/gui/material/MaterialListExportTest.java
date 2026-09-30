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
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void exportsOnlyDisplayedEntriesWithEscapedNamesAndLongCounts() throws Exception {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(Arrays.asList(new MaterialListModel.Entry<>("a", "Pipe, \"铜\"", "mod:pipe", 2000000000, 1, 0, 0),
            new MaterialListModel.Entry<>("b", "Hidden", "mod:hidden", 4, 4, 0, 0)));
        model.ignore("b");
        model.setMultiplier(3);
        String csv = MaterialListExport.format(model, "Test", MaterialListExport.Format.CSV, key -> key);
        assertTrue(csv.contains("\"Pipe, \"\"铜\"\"\",\"6000000000\",\"6000000000\",\"0\""));
        assertFalse(csv.contains("Hidden"));
        String json = MaterialListExport.format(model, "Test", MaterialListExport.Format.JSON, key -> "{Variant:1}");
        assertEquals(6000000000L, new JsonParser().parse(json).getAsJsonObject().getAsJsonArray("items")
            .get(0).getAsJsonObject().get("total").getAsLong());
        Path first = MaterialListExport.write(temporary.getRoot().toPath(), MaterialListExport.Format.CSV, csv);
        Path second = MaterialListExport.write(temporary.getRoot().toPath(), MaterialListExport.Format.CSV, "second");
        assertNotEquals(first, second);
        assertEquals(csv, new String(Files.readAllBytes(first), StandardCharsets.UTF_8));
    }
}
