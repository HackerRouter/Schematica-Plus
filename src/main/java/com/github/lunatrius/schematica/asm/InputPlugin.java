package com.github.lunatrius.schematica.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.github.lunatrius.schematica.asm.")
public final class InputPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() { return new String[] {InputTransformer.class.getName()}; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
