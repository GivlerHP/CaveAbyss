package ru.givler.caveabyss.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftOreRenderer;
import ru.givler.caveabyss.client.AmethystBudRenderer;
import ru.givler.caveabyss.client.AquaticPlantRenderer;
import ru.givler.caveabyss.client.MagmaBubbleEffects;
import cpw.mods.fml.common.FMLCommonHandler;

public class ClientProxy extends CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
	}
	
	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		AmethystBudRenderer.register();
		AquaticPlantRenderer.register();
		FMLCommonHandler.instance().bus().register(new MagmaBubbleEffects());
		if (ThaumcraftIntegration.isEnabled()) ThaumcraftOreRenderer.register();
	}
	
	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}

}
