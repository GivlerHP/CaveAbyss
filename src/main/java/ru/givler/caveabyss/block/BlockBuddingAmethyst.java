package ru.givler.caveabyss.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/** Fragile, immovable source of amethyst buds. */
public final class BlockBuddingAmethyst extends Block {
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};

    public BlockBuddingAmethyst() {
        super(Material.rock);
        setBlockName("budding_amethyst");
        setBlockTextureName("caveabyss:budding_amethyst");
        setHardness(1.5F);
        setResistance(1.5F);
        setStepSound(soundTypeGlass);
        setHarvestLevel("pickaxe", 0);
        setTickRandomly(true);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public int getMobilityFlag() {
        return 2;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || random.nextInt(5) != 0) return;
        int face = random.nextInt(6);
        int bx = x + DX[face], by = y + DY[face], bz = z + DZ[face];
        if (by < -64 || by > 255 || !world.getChunkProvider().chunkExists(bx >> 4, bz >> 4)) return;
        Block target = world.getBlock(bx, by, bz);
        if (target == Blocks.air || (target == Blocks.water || target == Blocks.flowing_water)
                && world.getBlockMetadata(bx, by, bz) == 0) {
            world.setBlock(bx, by, bz, CaveBlocks.amethystBuds[0], face, 3);
        } else {
            for (int stage = 0; stage < 3; stage++) {
                if (target == CaveBlocks.amethystBuds[stage]
                        && world.getBlockMetadata(bx, by, bz) == face) {
                    world.setBlock(bx, by, bz, CaveBlocks.amethystBuds[stage + 1], face, 3);
                    return;
                }
            }
        }
    }
}
