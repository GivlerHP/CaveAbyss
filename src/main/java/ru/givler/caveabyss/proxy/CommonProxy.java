package ru.givler.caveabyss.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import ru.givler.caveabyss.data.MinusOneLayer;

public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
        ru.givler.caveabyss.lighting.NegativeSkyLighting sky = new ru.givler.caveabyss.lighting.NegativeSkyLighting();
        MinecraftForge.EVENT_BUS.register(sky);
        FMLCommonHandler.instance().bus().register(sky);
		MinecraftForge.EVENT_BUS.register(new MinusOneLayer());
		MinecraftForge.EVENT_BUS.register(new ru.givler.caveabyss.network.MinusOneNetwork());
		ru.givler.caveabyss.core.NegativeItemTrace itemTrace = new ru.givler.caveabyss.core.NegativeItemTrace();
		MinecraftForge.EVENT_BUS.register(itemTrace);
		FMLCommonHandler.instance().bus().register(itemTrace);
		FMLCommonHandler.instance().bus().register(new ru.givler.caveabyss.handler.MagmaBubbleColumns());
	}
	
	public void init(FMLInitializationEvent event) {
	}
	
	public void postInit(FMLPostInitializationEvent event) {
	}

}
