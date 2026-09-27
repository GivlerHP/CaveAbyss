package ru.givler.caveabyss.client;

import net.minecraft.client.multiplayer.WorldClient;

/** Keeps the lower sky visible throughout the extended Overworld. */
public final class SkyRenderHooks {
    private SkyRenderHooks() {}

    public static double adjustHorizon(double vanillaHorizon, WorldClient world) {
        return world != null && world.provider.dimensionId == 0 ? -64.0D : vanillaHorizon;
    }
}
