package ru.givler.caveabyss.core;

import java.lang.reflect.Field;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.world.ChunkEvent;
import ru.givler.caveabyss.data.MinusOneLayer;
import sun.misc.Unsafe;

/** Reproduces an opacity callback asking World for metadata before provider registration. */
public final class MigrationRelightSmoke {
    public static class MetadataWorld extends World {
        boolean registered;
        int reads;
        MetadataWorld() { super(null,"migration",(net.minecraft.world.WorldSettings)null,null,null); }
        protected int func_152379_p() { return 0; }
        protected IChunkProvider createChunkProvider() { return null; }
        public Entity getEntityByID(int id) { return null; }
        @Override public int getBlockMetadata(int x,int y,int z) {
            if(!registered) throw new AssertionError("Opacity callback would recursively reload this chunk");
            reads++;return 3;
        }
    }
    public static class MetadataChunk extends Chunk {
        int relights;
        boolean failOnce;
        MetadataChunk(World world) { super(world,0,0); }
        @Override public void generateSkylightMap() {
            worldObj.getBlockMetadata(0,64,0);
            if(failOnce){failOnce=false;throw new IllegalStateException("test callback failure");}
            relights++;
        }
        @Override public void setChunkModified() { }
    }
    public static void check() throws Exception {
        Field access=Unsafe.class.getDeclaredField("theUnsafe");access.setAccessible(true);
        Unsafe unsafe=(Unsafe)access.get(null);
        MetadataWorld world=(MetadataWorld)unsafe.allocateInstance(MetadataWorld.class);
        Field provider=World.class.getDeclaredField("provider");provider.setAccessible(true);provider.set(world,new WorldProviderSurface());
        MetadataChunk chunk=(MetadataChunk)unsafe.allocateInstance(MetadataChunk.class);
        Field owner=Chunk.class.getDeclaredField("worldObj");owner.setAccessible(true);owner.set(chunk,world);
        MinusOneLayer.deferMigrationRelight(chunk);
        if(world.reads!=0||chunk.relights!=0)throw new AssertionError("Load-data phase must not relight");
        world.registered=true;
        MinusOneLayer handler=new MinusOneLayer();handler.onChunkLoad(new ChunkEvent.Load(chunk));
        if(world.reads!=1||chunk.relights!=1)throw new AssertionError("Registered chunk must be relit");
        handler.onChunkLoad(new ChunkEvent.Load(chunk));
        if(chunk.relights!=1)throw new AssertionError("Duplicate load repeated migration relight");
        MinusOneLayer.deferMigrationRelight(chunk);chunk.failOnce=true;
        try{handler.onChunkLoad(new ChunkEvent.Load(chunk));throw new AssertionError("Callback failure swallowed");}
        catch(IllegalStateException expected){}
        handler.onChunkLoad(new ChunkEvent.Load(chunk));
        if(chunk.relights!=2)throw new AssertionError("Failed relight lost its pending state");
        System.out.println("PASS migration relight: no pre-registration world reads, metadata callback safe after load, once-only and failure retry");
    }
}
