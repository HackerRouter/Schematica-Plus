package com.github.lunatrius.schematica.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModVersionTest {
    @Test public void ordersReleasePrereleaseAndDevBuilds() {
        List<String> ordered = Arrays.asList(
            "1.0.0-alpha.1",
            "1.0.0-beta.1",
            "1.0.0.beta.2",
            "1.0.0-beta.3",
            "1.0.0-beta.4",
            "1.0.0-beta.5",
            "1.0.0-beta.5-dirty",
            "1.0.0-beta.5-master.342+8875ebf3d2",
            "1.0.0-beta.10",
            "1.0.0-rc.1",
            "1.0.0",
            "1.0.0-dirty",
            "1.0.0-master.5+abcdef12",
            "1.0.1",
            "1.1.0",
            "2.0.0"
        );
        for (int i = 0; i < ordered.size(); i++) {
            ModVersion current = ModVersion.parse(ordered.get(i));
            assertNotNull(ordered.get(i), current);
            for (int j = i + 1; j < ordered.size(); j++) {
                ModVersion next = ModVersion.parse(ordered.get(j));
                assertNotNull(ordered.get(j), next);
                assertTrue(ordered.get(i) + " < " + ordered.get(j), current.compareTo(next) < 0);
                assertTrue(ordered.get(j) + " > " + ordered.get(i), next.compareTo(current) > 0);
            }
        }
    }

    @Test public void normalizesPrefixAndBuildMetadata() {
        ModVersion a = ModVersion.parse("v1.0.0-beta.5+abc123");
        ModVersion b = ModVersion.parse("1.0.0-beta.5+def456");
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(0, a.compareTo(b));
        assertEquals(a, b);
        assertTrue(a.isPrerelease());
        assertFalse(a.isDevBuild());

        ModVersion finalRelease = ModVersion.parse("1.0.0");
        assertNotNull(finalRelease);
        assertFalse(finalRelease.isPrerelease());
        assertFalse(finalRelease.isDevBuild());

        ModVersion dev = ModVersion.parse("1.0.0-beta.5-master.342+8875ebf3d2");
        assertNotNull(dev);
        assertTrue(dev.isPrerelease());
        assertTrue(dev.isDevBuild());
        assertEquals(342, dev.devCommits);
    }

    @Test public void rejectsNonSemanticVersionStrings() {
        for (String invalid : Arrays.asList(null, "", "   ", "db05cb5", "db05cb5-dirty", "different-build", "1", "1.0.0.5")) {
            assertNull(String.valueOf(invalid), ModVersion.parse(invalid));
        }
    }
}
