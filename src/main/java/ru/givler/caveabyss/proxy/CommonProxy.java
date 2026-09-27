package ru.givler.caveabyss.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import ru.givler.caveabyss.data.MinusOneLayer;
import ru.givler.caveabyss.handler.VoidDamageGuard;

public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new MinusOneLayer());
		MinecraftForge.EVENT_BUS.register(new ru.givler.caveabyss.network.MinusOneNetwork());
		MinecraftForge.EVENT_BUS.register(new VoidDamageGuard());
	}
	
	public void init(FMLInitializationEvent event) {
	}
	
	public void postInit(FMLPostInitializationEvent event) {
	}

}
