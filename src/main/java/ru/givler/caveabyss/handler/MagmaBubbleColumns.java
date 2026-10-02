package ru.givler.caveabyss.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import ru.givler.caveabyss.block.CaveBlocks;

/** Computes bubble columns from existing water, without replacing or saving water blocks. */
public final class MagmaBubbleColumns {
    public static boolean isColumnWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == CaveBlocks.seagrass || block == CaveBlocks.tallSeagrass || block == CaveBlocks.kelp
                || (block == Blocks.water || block == Blocks.flowing_water)
                && world.getBlockMetadata(x, y, z) == 0;
    }

    public static boolean isInColumn(World world, int x, int y, int z) {
        int minY = world.provider.dimensionId == 0 ? -64 : 0;
        if (y <= minY || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return false;
        for (int current = y; current > minY; current--) {
            if (!isColumnWater(world, x, current, z)) return false;
            if (world.getBlock(x, current - 1, z) == CaveBlocks.magma) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        World world = event.world;
        for (Object object : world.loadedEntityList) {
            Entity entity = (Entity)object;
            if (entity.isDead) continue;
            int x = MathHelper.floor_double(entity.posX);
            int y = MathHelper.floor_double(entity.boundingBox.minY + 0.1D);
            int z = MathHelper.floor_double(entity.posZ);
            if (!isInColumn(world, x, y, z)) continue;
            entity.motionY = Math.max(-0.16D, Math.min(-0.035D, entity.motionY - 0.025D));
            entity.velocityChanged = true;
            entity.fallDistance = 0;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase)entity;
                living.setAir(Math.min(300, living.getAir() + 2));
            }
        }
    }
}
