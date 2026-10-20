package ru.givler.caveabyss.lighting;

import java.util.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import ru.givler.caveabyss.data.MinusOneLayer;
import ru.givler.caveabyss.network.MinusOneNetwork;

/** Server-side bounded relight queue. Never loads chunks to solve light. */
public final class NegativeSkyLighting {
 private static final Map<World,LinkedHashSet<Long>> PENDING=new WeakHashMap<World,LinkedHashSet<Long>>();
 private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
 private static boolean supported(World w){return w!=null && !w.isRemote && w.provider!=null && w.provider.dimensionId==0 && !w.provider.hasNoSky;}
 public static void changed(World w,int x,int y,int z) {
  if(!supported(w))return;
  enqueue(w,x>>4,z>>4);enqueue(w,(x>>4)-1,z>>4);enqueue(w,(x>>4)+1,z>>4);enqueue(w,x>>4,(z>>4)-1);enqueue(w,x>>4,(z>>4)+1);
 }
 public static void metadataChanged(boolean changed,World w,int x,int y,int z){if(changed)changed(w,x,y,z);}
 private static void enqueue(World w,int x,int z) {
  if(!w.getChunkProvider().chunkExists(x,z))return;
  LinkedHashSet<Long> queue=PENDING.get(w);if(queue==null){queue=new LinkedHashSet<Long>();PENDING.put(w,queue);}queue.add(key(x,z));
 }
 @SubscribeEvent public void load(ChunkEvent.Load e){changed(e.world,e.getChunk().xPosition<<4,0,e.getChunk().zPosition<<4);}
 @SubscribeEvent public void unload(WorldEvent.Unload e){PENDING.remove(e.world);}
 @SubscribeEvent public void tick(TickEvent.WorldTickEvent e) {
  if(e.phase!=TickEvent.Phase.END || !supported(e.world))return;
  LinkedHashSet<Long> queue=PENDING.get(e.world);if(queue==null)return;
  // At most two chunk solutions per tick; repeated block edits coalesce.
  for(int n=0;n<2 && !queue.isEmpty();n++) {
   Iterator<Long> it=queue.iterator();long key=it.next();it.remove();int cx=(int)(key>>32),cz=(int)key;
   if(e.world.getChunkProvider().chunkExists(cx,cz))relight(e.world,e.world.getChunkFromChunkCoords(cx,cz));
  }
 }
 private static int neighbor(World w,int x,int y,int z){return w.getChunkProvider().chunkExists(x>>4,z>>4)?MinusOneLayer.getLight(w,EnumSkyBlock.Sky,x,y,z):0;}
 private static void relight(World w,Chunk chunk) {
  int bx=chunk.xPosition<<4,bz=chunk.zPosition<<4;
  byte[] opacity=new byte[16384],top=new byte[256],border=new byte[4096];
  for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
   top[z*16+x]=(byte)w.getSavedLightValue(EnumSkyBlock.Sky,bx+x,0,bz+z);
   for(int y=-64;y<0;y++)opacity[(y+64)*256+z*16+x]=(byte)Math.min(255,Math.max(0,MinusOneLayer.getBlock(chunk,x,y,z).getLightOpacity(w,bx+x,y,bz+z)));
  }
  for(int y=-64;y<0;y++)for(int a=0;a<16;a++) {
   int i=(y+64)*16+a;
   border[i]=(byte)neighbor(w,bx-1,y,bz+a);border[1024+i]=(byte)neighbor(w,bx+16,y,bz+a);
   border[2048+i]=(byte)neighbor(w,bx+a,y,bz-1);border[3072+i]=(byte)neighbor(w,bx+a,y,bz+16);
  }
  byte[] solved=NegativeSkyField.solve(opacity,top,border);int sections=0;boolean edges=false;
  for(int y=-64;y<0;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
   int next=solved[(y+64)*256+z*16+x]&15;
   if(next==MinusOneLayer.getLight(chunk,EnumSkyBlock.Sky,x,y,z))continue;
   MinusOneLayer.setLight(chunk,EnumSkyBlock.Sky,bx+x,y,bz+z,next);sections|=1<<((y+64)>>4);
   if(x==0||x==15||z==0||z==15)edges=true;
  }
  if(sections!=0)MinusOneNetwork.broadcastLight(chunk,sections);
  if(edges){enqueue(w,chunk.xPosition-1,chunk.zPosition);enqueue(w,chunk.xPosition+1,chunk.zPosition);enqueue(w,chunk.xPosition,chunk.zPosition-1);enqueue(w,chunk.xPosition,chunk.zPosition+1);}
 }
}
