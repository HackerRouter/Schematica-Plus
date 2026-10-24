package com.github.lunatrius.schematica.client.renderer;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeometrySupportTest {
    public static final class Api {
        static final Api INSTANCE = new Api();
        static boolean enabled;
        public static Api getInstance() { return INSTANCE; }
        public boolean active() { return enabled; }
        public static boolean staticActive() { return enabled; }
        public boolean broken() { throw new IllegalStateException(); }
    }

    @Test public void optionalFlagsFollowRuntimeChangesWithoutHardDependencies() {
        GeometrySupport.OptionalFlag absent = new GeometrySupport.OptionalFlag("missing.render.Api", null, "active");
        GeometrySupport.OptionalFlag iris = new GeometrySupport.OptionalFlag(Api.class.getName(), "getInstance", "active");
        GeometrySupport.OptionalFlag optifine = new GeometrySupport.OptionalFlag(Api.class.getName(), null, "staticActive");
        GeometrySupport.OptionalFlag meshing = new GeometrySupport.OptionalFlag(Api.class.getName(), null, "active");
        for (boolean enabled : new boolean[] {false, true, false}) {
            Api.enabled = enabled;
            assertFalse(absent.active(null));
            assertEquals(enabled, iris.active(null));
            assertEquals(enabled, optifine.active(null));
            assertEquals(enabled, meshing.active(Api.INSTANCE));
        }
    }

    @Test public void unavailableOrBrokenInstalledApisRetainNativeRendering() {
        assertTrue(new GeometrySupport.OptionalFlag(Api.class.getName(), null, "missing").active(null));
        assertTrue(new GeometrySupport.OptionalFlag(Api.class.getName(), "getInstance", "broken").active(null));
        assertTrue(new GeometrySupport.OptionalFlag(Api.class.getName(), null, "active").active(null));
    }
}
