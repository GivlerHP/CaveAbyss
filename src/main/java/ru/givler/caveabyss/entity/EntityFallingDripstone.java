package ru.givler.caveabyss.entity;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import ru.givler.caveabyss.block.CaveBlocks;

/** Uses vanilla falling physics and NBT, with a pointed renderer and impact damage. */
public final class EntityFallingDripstone extends EntityFallingBlock implements IEntityAdditionalSpawnData {
    public EntityFallingDripstone(World world) {
        super(world, 0, 0, 0, CaveBlocks.pointedDripstone, 8);
    }

    public EntityFallingDripstone(World world, double x, double y, double z) {
        super(world, x, y, z, CaveBlocks.pointedDripstone, 8);
        field_145812_b = 1; // The source was already removed, along with the detached chain.
    }

    @Override protected void fall(float distance) {
        if (worldObj.isRemote || distance <= 1) return;
        int damage = Math.min(40, (int)Math.ceil(distance - 1) * 6);
        for (Object value : worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox))
            ((Entity)value).attackEntityFrom(DamageSource.fallingBlock, damage);
    }

    @Override public void writeSpawnData(ByteBuf data) { data.writeInt(field_145812_b); }
    @Override public void readSpawnData(ByteBuf data) { field_145812_b = data.readInt(); }
}
