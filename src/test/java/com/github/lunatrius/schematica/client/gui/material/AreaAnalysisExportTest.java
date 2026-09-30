package com.github.lunatrius.schematica.client.gui.material;

import org.junit.Test;
import static org.junit.Assert.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.lunatrius.schematica.client.gui.material.MaterialListExport.Format;

public class AreaAnalysisExportTest {
    @Test public void partialAnalysisIsIdentifiedInEveryExportFormat() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(java.util.Collections.singletonList(new MaterialListModel.Entry<>("stone", "Stone", "minecraft:stone", 12, 0, 0, 0)));
        for (Format format : Format.values()) {
            String original = MaterialListExport.format(model, "Selection", format, value -> "{}");
            String text = MaterialListExport.withAnalysisStatus(original, format, 300, 2);
            if (format == Format.JSON) {
                JsonObject data = new JsonParser().parse(text).getAsJsonObject();
                assertFalse(data.get("complete").getAsBoolean());
                assertEquals(300, data.get("unverified_positions").getAsInt());
                assertEquals(2, data.get("skipped_positions").getAsInt());
                assertEquals(12, data.getAsJsonArray("items").get(0).getAsJsonObject().get("total").getAsInt());
                assertTrue(new JsonParser().parse(MaterialListExport.withAnalysisStatus(original, format, 0, 0))
                    .getAsJsonObject().get("complete").getAsBoolean());
            } else if (format == Format.CSV) {
                String[] lines = text.split("\n");
                assertEquals(2, lines.length);
                assertTrue(lines[0].endsWith("\"Unverified positions\",\"Skipped positions\""));
                assertTrue(lines[1].endsWith(",300,2"));
            } else {
                assertTrue(text.startsWith("Area analysis: unverified positions=300, skipped positions=2\n"));
                assertTrue(text.contains("Stone"));
            }
        }
    }
}
