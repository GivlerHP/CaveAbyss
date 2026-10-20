package ru.givler.caveabyss.core;
import java.util.*;
import ru.givler.caveabyss.lighting.NegativeSkyField;

public final class NegativeSkySmoke {
 private static int at(int x,int y,int z){return (y+64)*256+z*16+x;}
 public static void check() {
  byte[] opaque=new byte[16384];Arrays.fill(opaque,(byte)255);byte[] top=new byte[256],border=new byte[4096];
  // Open sky shaft across all four negative sections, with a side tunnel.
  for(int y=-64;y<0;y++)for(int z=6;z<=9;z++)for(int x=6;x<=9;x++)opaque[at(x,y,z)]=0;
  for(int z=6;z<=9;z++)for(int x=6;x<=9;x++)top[z*16+x]=15;
  for(int x=10;x<16;x++)opaque[at(x,-40,7)]=0;
  byte[] light=NegativeSkyField.solve(opaque,top,border);
  for(int y=-64;y<0;y++)if(light[at(7,y,7)]!=15)throw new AssertionError("Sky shaft dark at "+y);
  if(light[at(10,-40,7)]!=14||light[at(15,-40,7)]!=9)throw new AssertionError("Lateral attenuation incorrect");
  if(light[at(5,-40,7)]!=0)throw new AssertionError("Skylight penetrated opaque wall");
  // Closing above Y=0 removes all negative sky instead of retaining old light.
  Arrays.fill(top,(byte)0);light=NegativeSkyField.solve(opaque,top,border);
  for(byte value:light)if(value!=0)throw new AssertionError("Closed roof retained skylight");
  Arrays.fill(top,(byte)15);Arrays.fill(opaque,(byte)0);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)opaque[at(x,-2,z)]=3;
  light=NegativeSkyField.solve(opaque,top,border);
  if(light[at(7,-1,7)]!=15||light[at(7,-2,7)]!=12||light[at(7,-3,7)]!=11)throw new AssertionError("Water opacity ignored");
  // Neighbor chunk receives and then loses boundary light without a source of its own.
  Arrays.fill(opaque,(byte)0);Arrays.fill(top,(byte)0);Arrays.fill(border,(byte)0);
  border[24*16+7]=15;
  light=NegativeSkyField.solve(opaque,top,border);
  if(light[at(0,-40,7)]!=14||light[at(1,-40,7)]!=13)throw new AssertionError("Chunk-border attenuation incorrect");
  for(int repeat=0;repeat<16;repeat++) {
   for(int y=0;y<64;y++)for(int a=0;a<16;a++)border[y*16+a]=light[y*256+a*16];
   light=NegativeSkyField.solve(opaque,top,border);
  }
  for(byte value:light)if(value!=0)throw new AssertionError("Border feedback created permanent skylight");
  System.out.println("Negative sky: four-section open shaft, opaque roof closure, side tunnel, water attenuation, chunk border and extinction passed");
 }
}
