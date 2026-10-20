package ru.givler.caveabyss.core;

import java.lang.reflect.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import ru.givler.caveabyss.handler.VoidDamageGuard;
import sun.misc.Unsafe;

public final class VoidDamageSmoke {
 public static void check()throws Exception {
  Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);Unsafe u=(Unsafe)uf.get(null);
  World world=(World)u.allocateInstance(net.minecraft.world.WorldServer.class);
  Field provider=World.class.getDeclaredField("provider");provider.setAccessible(true);provider.set(world,new net.minecraft.world.WorldProviderSurface());
  EntityLivingBase entity=(EntityLivingBase)u.allocateInstance(net.minecraft.entity.monster.EntityZombie.class);entity.worldObj=world;
  for(int dimension:new int[]{0,-1,1}) {
   world.provider.dimensionId=dimension;
   double threshold=VoidDamageGuard.naturalThreshold(entity),expected=dimension==0?-128D:-64D;
   if(threshold!=expected)throw new AssertionError("Wrong natural void threshold for dimension "+dimension);

  }
  entity.worldObj=null;
  if(VoidDamageGuard.naturalThreshold(entity)!=-64D)throw new AssertionError("Worldless threshold changed");
  for(Method m:VoidDamageGuard.class.getDeclaredMethods())
   if(m.isAnnotationPresent(cpw.mods.fml.common.eventhandler.SubscribeEvent.class))throw new AssertionError("Global void damage cancellation restored");
  System.out.println("Void damage: natural overworld threshold -128, vanilla dimensions -64, no global damage-event cancellation");
 }
}
