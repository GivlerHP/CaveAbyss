package ru.givler.caveabyss.integration.mbo;

import java.lang.reflect.Method;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cpw.mods.fml.common.Loader;

/** MBO extends the vanilla cauldron: low two bits = level, bit 2 = lava. */
public final class DripstoneCauldrons {
    private static Method connectedMetadata, normalize;

    private DripstoneCauldrons() { }

    public static void init() {
        if (!Loader.isModLoaded("mbo")) return;
        try {
            Class<?> hooks = Class.forName("ru.givler.mbo.core.CauldronHooks");
            connectedMetadata = hooks.getMethod("getConnectedMetadata", IBlockAccess.class, int.class, int.class, int.class);
            normalize = hooks.getMethod("normalizeConnected", World.class, int.class, int.class, int.class);
        } catch (ReflectiveOperationException exception) {
            connectedMetadata = null;
            normalize = null;
            cpw.mods.fml.common.FMLLog.warning("[CaveAbyss] Update MBO for dripstone lava cauldrons");
        }
    }

    public static int filledMetadata(int metadata, boolean lava, boolean mbo) {
        int level = metadata & 3;
        if (level == 3 || lava && !mbo || level > 0 && ((metadata & 4) != 0) != lava) return metadata;
        return lava ? 7 : level + 1;
    }

    public static void fill(World world, int x, int y, int z, boolean lava) {
        if (world.isRemote || world.getBlock(x, y, z) != Blocks.cauldron) return;
        try {
            int metadata = connectedMetadata == null ? world.getBlockMetadata(x, y, z)
                    : (Integer)connectedMetadata.invoke(null, world, x, y, z);
            int next = filledMetadata(metadata, lava, connectedMetadata != null);
            if (next == metadata) return;
            world.setBlockMetadataWithNotify(x, y, z, next, 3);
            if (normalize != null) normalize.invoke(null, world, x, y, z);
            world.func_147453_f(x, y, z, Blocks.cauldron);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot fill MBO cauldron from dripstone", exception);
        }
    }
}
