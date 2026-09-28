package ru.givler.caveabyss;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import ru.givler.caveabyss.network.MinusOneNetwork;
import ru.givler.caveabyss.block.CaveBlocks;
import ru.givler.caveabyss.proxy.CommonProxy;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration;
import ru.givler.caveabyss.integration.bop.BopIntegration;
import ru.givler.caveabyss.integration.mf2.Mf2Integration;
import ru.givler.caveabyss.config.DeepOreConfig;

@Mod(modid= CaveAbyss.ID, name= CaveAbyss.NAME, version= CaveAbyss.VERSION,
        dependencies="after:minefantasy2")
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
		DeepOreConfig.load(new java.io.File(event.getModConfigurationDirectory(), "caveabyss-ores.cfg"));
		CaveBlocks.register();
		ThaumcraftIntegration.preInit();
		BopIntegration.preInit();
		Mf2Integration.preInit();
		if (cpw.mods.fml.common.Loader.isModLoaded("minefantasy2"))
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
					new ru.givler.caveabyss.integration.mf2.DepletedDeepOreGuard());
		MinusOneNetwork.init();
		proxy.preInit(event);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		proxy.init(event);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		ThaumcraftIntegration.postInit();
		BopIntegration.postInit();
		Mf2Integration.postInit();
		if (cpw.mods.fml.common.Loader.isModLoaded("minefantasy2"))
			ru.givler.caveabyss.integration.mf2.Mf2Compatibility.register();
		proxy.postInit(event);
	}

}
