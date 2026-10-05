package com.github.lunatrius.schematica.client.gui.material;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class CustomMaterialListFileTest {
    @Test public void readsUpstreamJsonAndTheDamageForms() throws IOException {
        CustomMaterialListFile list = CustomMaterialListFile.parseJson("{\"name\":\"Shop\",\"items\":["
            + "{\"id\":\"minecraft:diamond_pickaxe\",\"count\":5},{\"id\":\"minecraft:wool\",\"damage\":14,\"count\":3},"
            + "{\"id\":\"minecraft:wool@14\",\"count\":2},{\"id\":\"stone\",\"count\":1},{\"id\":\"x:y\",\"count\":0},{\"count\":4}]}", "f.json");
        assertEquals("Shop", list.name);
        assertEquals(3, list.items.size());
        assertEquals("minecraft:diamond_pickaxe", list.items.get(0).id);
        assertEquals(14, list.items.get(1).damage);
        assertEquals(5, list.items.get(1).count);
        assertEquals("minecraft:stone", list.items.get(2).id);
    }

    @Test public void readsTextLinesAndSkipsCommentsAndBadLines() throws IOException {
        CustomMaterialListFile list = CustomMaterialListFile.parseText("# list\nminecraft:cooked_beef 64\n\nminecraft:dye@4 10\nbad line here\nminecraft:apple x\n", "list.txt");
        assertEquals("list.txt", list.name);
        assertEquals(2, list.items.size());
        assertEquals(4, list.items.get(1).damage);
    }

    @Test(expected = IOException.class) public void rejectsFilesWithoutItems() throws IOException {
        CustomMaterialListFile.parseJson("{\"name\":\"x\",\"items\":[]}", "x.json");
    }

    @Test public void writesWhatItReads() throws IOException {
        String json = CustomMaterialListFile.toJson("A", Arrays.asList(new CustomMaterialListFile.Item("minecraft:wool", 3, 7),
            new CustomMaterialListFile.Item("minecraft:stone", 0, 2)));
        assertFalse(json.contains("\"damage\": 0"));
        CustomMaterialListFile list = CustomMaterialListFile.parseJson(json, "a.json");
        assertEquals("A", list.name);
        assertEquals(3, list.items.get(0).damage);
        assertEquals(2, list.items.get(1).count);
        assertTrue(CustomMaterialListFile.isListFile("X.TXT"));
        assertFalse(CustomMaterialListFile.isListFile("x.litematic"));
    }
}
