package ru.givler.caveabyss.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftOreRenderer;
import ru.givler.caveabyss.client.AmethystBudRenderer;

public class ClientProxy extends CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
	}
	
	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		AmethystBudRenderer.register();
		if (ThaumcraftIntegration.isEnabled()) ThaumcraftOreRenderer.register();
	}
	
	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}

}
