package ru.givler.caveabyss.lighting;

/** Sky-light solution for one negative chunk, seeded at Y=0 and loaded horizontal borders. */
public final class NegativeSkyField {
 public static final int SIZE=16384;
 private NegativeSkyField() {}
 public static byte[] solve(byte[] opacity,byte[] top,byte[] border) {
  byte[] light=new byte[SIZE];int[] queue=new int[SIZE];boolean[] queued=new boolean[SIZE];
  int head=0,tail=0,pending=0;
  for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
   int level=top[z*16+x]&15;
   for(int y=63;y>=0;y--) {
    int at=y*256+z*16+x,op=opacity[at]&255;
    level=Math.max(0,level-(level==15 && op==0?0:Math.max(1,op)));
    light[at]=(byte)level;
   }
  }
  for(int y=0;y<64;y++)for(int a=0;a<16;a++)for(int side=0;side<4;side++) {
   int x=side==0?0:side==1?15:a,z=side==2?0:side==3?15:a,at=y*256+z*16+x;
   int value=Math.max(0,(border[side*1024+y*16+a]&15)-Math.max(1,opacity[at]&255));
   if(value>(light[at]&15))light[at]=(byte)value;
  }
  for(int at=0;at<SIZE;at++)if((light[at]&15)>1){queue[tail]=at;tail=(tail+1)%SIZE;queued[at]=true;pending++;}
  while(pending>0) {
   int at=queue[head];head=(head+1)%SIZE;pending--;queued[at]=false;
   int x=at&15,z=(at>>4)&15,y=at>>8,level=light[at]&15;
   for(int side=0;side<6;side++) {
    int nx=x+(side==0?-1:side==1?1:0),nz=z+(side==2?-1:side==3?1:0),ny=y+(side==4?-1:side==5?1:0);
    if(nx<0||nx>=16||nz<0||nz>=16||ny<0||ny>=64)continue;
    int next=ny*256+nz*16+nx,value=level-Math.max(1,opacity[next]&255);
    if(value<=(light[next]&15))continue;
    light[next]=(byte)value;
    if(value>1 && !queued[next]){queue[tail]=next;tail=(tail+1)%SIZE;queued[next]=true;pending++;}
   }
  }
  return light;
 }
}
