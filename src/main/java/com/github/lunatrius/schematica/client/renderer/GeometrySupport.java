package com.github.lunatrius.schematica.client.renderer;

import java.lang.reflect.Method;

final class GeometrySupport {
    private static final OptionalFlag IRIS = new OptionalFlag("net.irisshaders.iris.api.v0.IrisApi", "getInstance", "isShaderPackInUse");
    private static final OptionalFlag OPTIFINE = new OptionalFlag("Config", null, "isShaders");
    private static final OptionalFlag MULTITEXTURE = new OptionalFlag("Config", null, "isMultiTexture");
    private static final OptionalFlag MESHING = new OptionalFlag("com.gtnewhorizons.angelica.rendering.StateAwareTessellator", null, "angelica$isCeleritasMeshing");

    static boolean supported(Object tessellator) {
        return !IRIS.active(null) && !OPTIFINE.active(null) && !MULTITEXTURE.active(null) && !MESHING.active(tessellator);
    }

    static final class OptionalFlag {
        private Method factory, method;
        private boolean failed;

        OptionalFlag(String name, String factoryName, String methodName) {
            try {
                Class<?> type = Class.forName(name, false, GeometrySupport.class.getClassLoader());
                if (factoryName != null) factory = type.getMethod(factoryName);
                method = type.getMethod(methodName);
            } catch (ClassNotFoundException absent) {
                method = null;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                failed = true;
            }
        }

        boolean active(Object instance) {
            if (failed) return true;
            if (method == null) return false;
            try {
                Object receiver = factory == null ? instance : factory.invoke(null);
                return !Boolean.FALSE.equals(method.invoke(receiver));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                failed = true;
                return true;
            }
        }
    }
}
