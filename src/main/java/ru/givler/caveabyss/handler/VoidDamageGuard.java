package ru.givler.caveabyss.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class VoidDamageGuard {
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.source == DamageSource.outOfWorld && event.entityLiving.posY >= -128.0D)
            event.setCanceled(true);
    }
}
