package ru.givler.caveabyss.core;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;

/** Temporary server/client trace for items spawned at the negative-Y seam. */
public final class NegativeItemTrace {
    private static final Logger LOG = LogManager.getLogger("CaveAbyss/NegativeItems");
    private final Map<Entity, Integer> samples = Collections.synchronizedMap(new WeakHashMap<Entity, Integer>());

    @SubscribeEvent
    public void joined(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityItem && nearSeam(event.entity)) {
            log("JOIN", (EntityItem) event.entity);
        }
    }

    @SubscribeEvent
    public void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.getTotalWorldTime() % 20 != 0) return;
        for (Object candidate : event.world.loadedEntityList) {
            if (!(candidate instanceof EntityItem)) continue;
            EntityItem item = (EntityItem) candidate;
            if (!nearSeam(item)) continue;
            Integer count = samples.get(item);
            if (count != null && count >= 8) continue;
            samples.put(item, count == null ? 1 : count + 1);
            log("WORLD_TICK", item);
        }
    }

    private static boolean nearSeam(Entity entity) {
        return entity.posY > -6 && entity.posY < 2;
    }

    private static void log(String stage, EntityItem item) {
        LOG.info("{} {} id={} pos=({}, {}, {}) motionY={} age={} ticks={} chunk=({},{},{}) added={} ground={} noClip={} blockAtY={} blockBelow={}",
                item.worldObj.isRemote ? "CLIENT" : "SERVER", stage, item.getEntityId(),
                item.posX, item.posY, item.posZ, item.motionY, item.age, item.ticksExisted,
                item.chunkCoordX, item.chunkCoordY, item.chunkCoordZ, item.addedToChunk,
                item.onGround, item.noClip,
                net.minecraft.block.Block.getIdFromBlock(item.worldObj.getBlock(
                        MathHelper.floor_double(item.posX), MathHelper.floor_double(item.posY), MathHelper.floor_double(item.posZ))),
                net.minecraft.block.Block.getIdFromBlock(item.worldObj.getBlock(
                        MathHelper.floor_double(item.posX), MathHelper.floor_double(item.posY) - 1, MathHelper.floor_double(item.posZ))));
    }
}
