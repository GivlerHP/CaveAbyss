package ru.givler.caveabyss.handler;

import net.minecraft.entity.Entity;
import ru.givler.caveabyss.core.MinusOneHooks;

/** Natural void threshold only. Explicit outOfWorld damage must never be cancelled here. */
public final class VoidDamageGuard {
    private VoidDamageGuard() {}
    public static double naturalThreshold(Entity entity) {
        return entity.worldObj==null ? -64D : MinusOneHooks.minimumY(entity.worldObj)-64D;
    }
}
