package ru.givler.caveabyss.core;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/** Temporary test-only mod: exercise the real renderer in an isolated flat world. */
@Mod(modid="caveangelicaprobe", name="Cave Angelica migration probe", version="1")
public class AngelicaWorldProbe {
    private boolean launched;
    private int ticks;
    private boolean platform;
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            System.out.println("[AngelicaProbe] Starting isolated world from " + mc.currentScreen.getClass().getName());
            launched = true;
            mc.gameSettings.pauseOnLostFocus = false;
            WorldSettings settings = new WorldSettings(123L, net.minecraft.world.WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
            settings.func_82750_a("2;7,2x3,2;1;");
            mc.launchIntegratedServer("AngelicaMigrationProbe", "Angelica Migration Probe", settings);
        }
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiIngameMenu) mc.displayGuiScreen(null);
        ticks++;
        if (ticks == 30) {
            mc.thePlayer.capabilities.isFlying = true;
            mc.thePlayer.noClip = true;
            mc.thePlayer.setPositionAndRotation(0.5, -20, 0.5, 0, 35);
            mc.renderGlobal.loadRenderers();
            System.out.println("[AngelicaProbe] Lower-world camera active at Y=-20");
        }
        if (ticks == 400) {
            try {
                Class<?> rendererClass=Class.forName("com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer");
                Object renderer=rendererClass.getMethod("getInstance").invoke(null);
                int minimum=(Integer)rendererClass.getMethod("getMinimumBuildHeight").invoke(renderer);
                if(minimum!=-64)throw new AssertionError("Renderer minimum: "+minimum);
                Object manager=rendererClass.getMethod("getRenderSectionManager").invoke(renderer);
                boolean empty=(Boolean)manager.getClass().getMethod("isSectionVisuallyEmpty",int.class,int.class,int.class).invoke(manager,0,-2,0);
                if(empty)throw new AssertionError("Server platform missing from client section");
                net.minecraft.world.chunk.Chunk chunk=mc.theWorld.getChunkFromChunkCoords(0,0);
                net.minecraft.world.chunk.storage.ExtendedBlockStorage snapshot=AngelicaHeightHooks.section(chunk,-2);
                System.out.println("[AngelicaProbe] Light self="+mc.theWorld.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,-23,4)
                    +", adjacent="+mc.theWorld.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,-23,3)
                    +", snapshot="+snapshot.getExtBlocklightValue(0,9,4)
                    +", emission="+mc.theWorld.getBlock(0,-23,4).getLightValue(mc.theWorld,0,-23,4));
                Class.forName("com.seibel.distanthorizons.common.render.openGl.glObject.GLProxy").getMethod("getInstance").invoke(null);
                System.out.println("[AngelicaProbe] PASS: minimum=-64, lower section nonempty, DH GL context initialized");
            } catch(Exception error) { throw new RuntimeException(error); }
            System.out.println("[AngelicaProbe] Rendered 370 ticks below zero; renderer=" + mc.renderGlobal.getClass().getName());
            System.out.println(net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer()));
            mc.shutdown();
        }
    }

    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || platform) return;
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        net.minecraft.world.WorldServer world = server.worldServerForDimension(0);
        for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=2;cz++)world.getChunkFromChunkCoords(cx,cz);
        for (int x=-8; x<=8; x++) for (int z=-8; z<=8; z++) world.setBlock(x,-24,z,net.minecraft.init.Blocks.stone);
        world.setBlock(0,-23,4,net.minecraft.init.Blocks.air);
        world.setBlock(0,-23,4,net.minecraft.init.Blocks.glowstone);
        System.out.println("[AngelicaProbe] Server light self="+world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,-23,4)
            +", adjacent="+world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,-23,3));
        net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP)server.getConfigurationManager().playerEntityList.get(0);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.setPositionAndUpdate(0.5,-20,0.5);
        platform=true;
        System.out.println("[AngelicaProbe] Server platform placed at Y=-24");
    }
}
