package net.minecraft.world;
public class ChunkCoordIntPair { final int x,z; public ChunkCoordIntPair(int x,int z){this.x=x;this.z=z;}
 public int hashCode(){return x*31+z;} public boolean equals(Object o){return o instanceof ChunkCoordIntPair&&((ChunkCoordIntPair)o).x==x&&((ChunkCoordIntPair)o).z==z;}}
