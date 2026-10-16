package net.minecraft.world.chunk;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
public class Chunk {
 public World worldObj = new World(); public boolean isChunkLoaded = true, negativeReady = true;
 public boolean[] negativeBlocks = {true,true,true,true};
 public final ExtendedBlockStorage[] storage = new ExtendedBlockStorage[16];
 public Chunk(){for(int i=0;i<16;i++)storage[i]=new ExtendedBlockStorage();}
 public ExtendedBlockStorage[] getBlockStorageArray(){return storage;}
}
