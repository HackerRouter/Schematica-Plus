package com.github.lunatrius.schematica.client.input;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.Test;

import com.github.lunatrius.schematica.handler.VisualSettings;

import static org.junit.Assert.*;

public class ToggleHotkeysTest {
    @Test public void everyBooleanHotkeyFlipsAnOptionAndHasUpstreamText() throws Exception {
        int toggles = 0;
        for (String language : new String[] {"en_US", "zh_CN"}) {
            Properties lang = new Properties();
            try (InputStream stream = getClass().getResourceAsStream("/assets/schematica_plus_litematica/lang/" + language + ".lang")) {
                lang.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
            toggles = 0;
            for (Hotkey key : Hotkeys.ALL) {
                if (key.toggleGroup == null) continue;
                toggles++;
                assertTrue(key.keys().isEmpty());
                VisualSettings.Toggle toggle = VisualSettings.Toggle.byUpstream(key.id);
                assertNotNull(key.id, toggle);
                assertSame(toggle, VisualSettings.Toggle.byProperty(toggle.category, toggle.key));
                assertTrue(language + " " + key.translationKey(), lang.containsKey(key.translationKey()));
                assertTrue(language + " " + key.commentKey(), lang.containsKey(key.commentKey()));
            }
        }
        assertEquals(26, toggles);
    }
}
