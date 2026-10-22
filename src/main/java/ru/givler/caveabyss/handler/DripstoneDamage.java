package ru.givler.caveabyss.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import ru.givler.caveabyss.block.BlockPointedDripstone;
import ru.givler.caveabyss.block.CaveBlocks;

/** Keep vanilla fall protection and Forge fall hooks; replace only the distance. */
public final class DripstoneDamage {
    @SubscribeEvent public void onFall(LivingFallEvent event) {
        EntityLivingBase entity = event.entityLiving;
        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX), z = MathHelper.floor_double(entity.posZ);
        int y = MathHelper.floor_double(entity.boundingBox.minY - .2);
        if (!world.isRemote && CaveBlocks.pointedDripstone instanceof BlockPointedDripstone
                && ((BlockPointedDripstone)CaveBlocks.pointedDripstone).same(world, x, y, z, 1)
                && world.getBlock(x, y + 1, z) != CaveBlocks.pointedDripstone && event.distance > 1)
            event.distance = (event.distance - 1) * 2 + 3;
    }
}
