package com.github.lunatrius.schematica.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorldSessionTest {
    @Test public void savesRemovalAsEmptyBeforeReturningToDimension() {
        Map<String, List<String>> saved = new HashMap<>();
        saved.put("overworld", Arrays.asList("house"));
        saved.put("nether", Arrays.asList("portal"));
        List<String> loaded = new ArrayList<>();
        WorldSession<Object> session = new WorldSession<>(key -> saved.put(key, new ArrayList<>(loaded)),
            loaded::clear, key -> loaded.addAll(saved.get(key)));
        Object overworld = new Object();
        session.update(overworld, "overworld");
        loaded.clear();
        session.save();
        session.unload(overworld);
        session.update(new Object(), "nether");
        assertEquals(Arrays.asList("portal"), loaded);
        session.update(new Object(), "overworld");
        assertTrue(loaded.isEmpty());
        assertTrue(saved.get("overworld").isEmpty());
        assertEquals(Arrays.asList("portal"), saved.get("nether"));
    }

    @Test public void ignoresUnrelatedUnloadAndRestoresOncePerWorld() {
        List<String> events = new ArrayList<>();
        WorldSession<Object> session = new WorldSession<>(key -> events.add("save:" + key),
            () -> events.add("clear"), key -> events.add("restore:" + key));
        Object oldWorld = new Object();
        Object newWorld = new Object();
        session.update(oldWorld, "old");
        session.update(oldWorld, "old");
        session.unload(newWorld);
        session.unload(oldWorld);
        session.update(newWorld, "new");
        session.unload(oldWorld);
        session.update(newWorld, "new");
        assertEquals(Arrays.asList("clear", "restore:old", "save:old", "clear", "clear", "restore:new"), events);
    }

    @Test public void handlesWorldReplacementWithoutUnloadNotification() {
        List<String> events = new ArrayList<>();
        WorldSession<Object> session = new WorldSession<>(key -> events.add("save:" + key),
            () -> events.add("clear"), key -> events.add("restore:" + key));
        session.update(new Object(), "old");
        session.update(new Object(), "new");
        session.update(null, null);
        session.update(null, null);
        assertEquals(Arrays.asList("clear", "restore:old", "save:old", "clear", "restore:new", "save:new", "clear"), events);
    }
}
