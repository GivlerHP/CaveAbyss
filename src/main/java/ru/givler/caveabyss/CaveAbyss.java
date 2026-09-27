package ru.givler.caveabyss;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import ru.givler.caveabyss.network.MinusOneNetwork;
import ru.givler.caveabyss.command.ProbeCommand;
import ru.givler.caveabyss.proxy.CommonProxy;

@Mod(modid= CaveAbyss.ID, name= CaveAbyss.NAME, version= CaveAbyss.VERSION)
public class CaveAbyss {
	public static final String ID = "caveabyss";
	public static final String NAME = "CaveAbyss";
	public static final String VERSION = "@VERSION@";
	
	@Instance(ID)
	public static CaveAbyss instance;
	
	@SidedProxy(clientSide="ru.givler.caveabyss.proxy.ClientProxy", serverSide="ru.givler.caveabyss.proxy.CommonProxy")
	public static CommonProxy proxy;


	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinusOneNetwork.init();
		proxy.preInit(event);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		proxy.init(event);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new ProbeCommand());
	}
}
