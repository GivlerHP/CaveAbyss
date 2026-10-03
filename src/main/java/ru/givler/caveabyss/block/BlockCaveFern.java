package ru.givler.caveabyss.block;

import net.minecraft.block.BlockBush;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;

public final class BlockCaveFern extends BlockBush {
    public BlockCaveFern() {
        setBlockName("cave_fern");
        setBlockTextureName("minecraft:fern");
        setStepSound(soundTypeGrass);
        setLightLevel(0.2F);
    }

    @Override public boolean canBlockStay(World world, int x, int y, int z) {
        return world.getBlock(x, y - 1, z).getMaterial().isSolid();
    }
    @Override protected boolean canPlaceBlockOn(net.minecraft.block.Block block) {
        return block.getMaterial().isSolid();
    }
    @Override public int getRenderColor(int metadata) { return 0x579765; }
    @Override public int colorMultiplier(IBlockAccess world, int x, int y, int z) { return 0x579765; }
}
