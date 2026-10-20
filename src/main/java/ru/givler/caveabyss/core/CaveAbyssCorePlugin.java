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
                "ru.givler.caveabyss.core.MboHeightTransformer",
                "ru.givler.caveabyss.core.NegativeSkyTransformer",
                "ru.givler.caveabyss.core.WorldMinusOneTransformer",
                "ru.givler.caveabyss.core.NegativeBlockPacketTransformer",
                "ru.givler.caveabyss.core.NegativeDigTraceTransformer",
                "ru.givler.caveabyss.core.NegativeMiningClientTransformer",
                "ru.givler.caveabyss.core.NegativeItemTransformer",
                "ru.givler.caveabyss.core.DeepRandomTickTransformer",
                "ru.givler.caveabyss.core.PistonHeightTransformer",
                "ru.givler.caveabyss.core.BlockMechanicsHeightTransformer",
                "ru.givler.caveabyss.core.DeepSpawnTransformer",
                "ru.givler.caveabyss.core.ChunkNegativeTransformer",
                "ru.givler.caveabyss.core.VoidDamageTransformer",
                "ru.givler.caveabyss.core.VoidVisualTransformer",
                "ru.givler.caveabyss.core.NegativeRenderTransformer",
                "ru.givler.caveabyss.core.DeepChunkGeneratorTransformer",
                "ru.givler.caveabyss.core.RtgChunkGeneratorTransformer",
                "ru.givler.caveabyss.core.CauldronBukkitTransformer",
                "ru.givler.caveabyss.core.BopPlantSupportTransformer",
                "ru.givler.caveabyss.core.NeodymiumHeightTransformer",
                "ru.givler.caveabyss.core.AngelicaHeightTransformer"
        };
    }

    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
