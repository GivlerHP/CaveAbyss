package ru.givler.caveabyss.core;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.entity.Entity;
import ru.givler.mbo.block.BlockObserver;
import ru.givler.mbo.core.ObserverHooks;
import ru.givler.mbo.registry.BlockRegistry;
import ru.givler.mbo.lighting.ColoredLightApi;

public final class MboHeightSmoke {
 static class TestWorld extends World {
  BlockObserver observer;int reads,pulses;
  TestWorld(){super(null,"height",(net.minecraft.world.WorldSettings)null,null,null);}
  protected int func_152379_p(){return 0;}
  protected IChunkProvider createChunkProvider(){return null;}
  public Entity getEntityByID(int id){return null;}
  public boolean blockExists(int x,int y,int z){return true;}
  public int getBlockMetadata(int x,int y,int z){return 3;}
  public void scheduleBlockUpdate(int x,int y,int z,Block b,int delay){pulses++;}
  public Block getBlock(int x,int y,int z){reads++;return observer;}
 }
 public static void check()throws Exception {
  Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
  TestWorld world=(TestWorld)unsafe.allocateInstance(TestWorld.class);world.observer=(BlockObserver)unsafe.allocateInstance(BlockObserver.class);
  Field provider=World.class.getDeclaredField("provider");provider.setAccessible(true);provider.set(world,new net.minecraft.world.WorldProviderSurface());
  Field registry=BlockRegistry.class.getField("Observer");Object saved=registry.get(null);registry.set(null,world.observer);
  try {
   ColoredLightApi.registerMinimumY(0,-64);
   ObserverHooks.onBlockChanged(world,0,-32,0);if(world.reads!=6 || world.pulses!=1)throw new AssertionError("Negative observer neighbors skipped: reads="+world.reads+" pulses="+world.pulses+" source="+ObserverHooks.class.getProtectionDomain().getCodeSource());
   world.reads=0;ObserverHooks.onMetadataChanged(true,world,0,-64,0);if(world.reads!=5)throw new AssertionError("Minimum boundary wrong");
   int before=world.reads;ObserverHooks.onMetadataChanged(false,world,0,-32,0);if(world.reads!=before)throw new AssertionError("Unchanged metadata notified");
   world.provider.dimensionId=1;world.reads=0;ObserverHooks.onBlockChanged(world,0,-32,0);if(world.reads!=0)throw new AssertionError("Vanilla dimension gained negative observers");
  }finally {registry.set(null,saved);}
  Set<Integer> variants=new HashSet<Integer>();
  for(int x=-12;x<12;x++){int hash=ru.givler.mbo.client.texture.NaturalTextureHooks.random(x,-32,4,2);if(hash!=ru.givler.mbo.client.texture.NaturalTextureHooks.random(x,-32,4,2))throw new AssertionError("Unstable negative-Y texture random");variants.add(hash&7);}
  if(variants.size()<4)throw new AssertionError("Negative-Y natural variations collapsed");
  if(CtmHeightHooks.parse("","","")!=null)throw new AssertionError("Unrestricted CTM changed");
  BitSet all=CtmHeightHooks.parse("","255","");if(!all.get(-64)||!all.get(-1)||!all.get(0)||!all.get(255)||all.get(-65))throw new AssertionError("Default signed range");
  BitSet signed=CtmHeightHooks.parse("","","-64--32 -1 0 12-14");
  if(!signed.get(-64)||!signed.get(-32)||signed.get(-31)||!signed.get(-1)||!signed.get(14)||signed.get(15))throw new AssertionError("Signed CTM list");
  BitSet positive=CtmHeightHooks.parse("0","10","");if(positive.get(-1)||!positive.get(0)||positive.get(11))throw new AssertionError("Explicit positive range changed");
  try(JarFile jar=new JarFile("libs/angelica-2.2.33-dev.jar")) {
   byte[] original;try(InputStream in=jar.getInputStream(jar.getJarEntry("com/prupe/mcpatcher/ctm/TileOverride.class"))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);original=out.toByteArray();}
   byte[] patched=new MboHeightTransformer().transform("com.prupe.mcpatcher.ctm.TileOverride","com.prupe.mcpatcher.ctm.TileOverride",original);
   if(!Arrays.equals(patched,new MboHeightTransformer().transform("com.prupe.mcpatcher.ctm.TileOverride","com.prupe.mcpatcher.ctm.TileOverride",patched)))throw new AssertionError("Double CTM patch");
   ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);
   for(MethodNode m:node.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);
  }
  System.out.println("MBO lower-height: observer block/metadata changes, minimum/dimension bounds, CTM signed/default/explicit selectors and actual Angelica bytecode passed");
 }
}
