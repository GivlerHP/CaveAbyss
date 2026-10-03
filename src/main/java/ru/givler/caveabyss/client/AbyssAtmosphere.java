package ru.givler.caveabyss.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import ru.givler.caveabyss.world.abyss.AbyssDimensions;

public final class AbyssAtmosphere {
    private static final IRenderHandler EMPTY = new IRenderHandler() {
        @Override public void render(float partial, WorldClient world, Minecraft minecraft) { }
    };
    @SubscribeEvent public void load(WorldEvent.Load event) {
        if (event.world.isRemote && AbyssDimensions.isAbyss(event.world)) {
            event.world.provider.setSkyRenderer(EMPTY);
            event.world.provider.setCloudRenderer(EMPTY);
            event.world.provider.setWeatherRenderer(EMPTY);
        }
    }
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogColors event) {
        if (AbyssDimensions.isAbyss(event.entity.worldObj) && !event.entity.isInWater()
                && event.block.getMaterial() != net.minecraft.block.material.Material.lava) {
            event.red = 0.035F; event.green = 0.045F; event.blue = 0.04F;
        }
    }
    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Remote registrations must not leak into the next server connection.
        for (int id : DimensionManager.getStaticDimensionIDs()) {
            if (DimensionManager.getWorld(id) != null) continue;
            if (AbyssDimensions.isDepthDimension(id))
                DimensionManager.unregisterDimension(id);
        }
    }
}
