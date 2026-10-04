package ru.givler.caveabyss.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;

/** Keep negative-Y item positions when entity movement packets reach the client. */
public final class NegativeItemHooks {
    private NegativeItemHooks() { }

    public static void applyNetworkPosition(Entity entity, double x, double y, double z,
                                            float yaw, float pitch, int interpolationTicks) {
        if (entity instanceof EntityItem && y < 0) {
            // Entity.setPositionAndRotation2 starts the collision top at zero.
            // A floor below zero therefore moves an item to Y=0.125.
            entity.setPositionAndRotation(x, y, z, yaw, pitch);
        } else {
            entity.setPositionAndRotation2(x, y, z, yaw, pitch, interpolationTicks);
        }
    }
}
