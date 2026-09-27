package ru.givler.caveabyss.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.Name("CaveAbyssCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"ru.givler.caveabyss.core"})
public final class CaveAbyssCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "ru.givler.caveabyss.core.WorldMinusOneTransformer",
                "ru.givler.caveabyss.core.ChunkNegativeTransformer",
                "ru.givler.caveabyss.core.VoidVisualTransformer",
                "ru.givler.caveabyss.core.NegativeRenderTransformer",
                "ru.givler.caveabyss.core.DeepChunkGeneratorTransformer"
        };
    }

    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
