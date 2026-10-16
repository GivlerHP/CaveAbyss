package ru.givler.caveabyss.data;
import net.minecraft.world.chunk.Chunk;
public class MinusOneLayer { public static boolean isClientChunkReady(Chunk c){return c.negativeReady;}
 public static boolean hasBlocksInSection(Chunk c,int s){return c.negativeBlocks[s];} }
