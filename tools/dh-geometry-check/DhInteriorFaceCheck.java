import java.lang.reflect.*;
import java.util.*;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.*;
import com.seibel.distanthorizons.core.dataObjects.render.columnViews.ColumnRenderView;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class DhInteriorFaceCheck {
    static Constructor<LodQuadBuilder> ctor;
    static Method method;
    static Field quads;
    static IClientLevelWrapper world;
    static List<BufferQuad> build(int bottom, EDhDirection dir, boolean sameDetail, int neighborMin, int neighborMax, int light) throws Exception {
        LodQuadBuilder builder = ctor.newInstance();
        PhantomArrayListCheckout pool = new PhantomArrayListCheckout(null);
        pool.addLongArrayListRef(new LongArrayList()); pool.addLongArrayListRef(new LongArrayList());
        LongArrayList data = new LongArrayList();
        if (neighborMax > neighborMin) data.add(RenderDataPointUtil.createDataPoint(neighborMax,neighborMin,0xff808080,light,0,(byte)0));
        try (ColumnRenderView adjacent = ColumnRenderView.getPooled(data,data.size(),0,data.size())) {
            method.invoke(null,builder,pool,world,adjacent,sameDetail,Integer.MIN_VALUE,dir,
                (short)(dir==EDhDirection.WEST?0:dir==EDhDirection.EAST?64:16),
                (short)bottom,(short)(dir==EDhDirection.NORTH?0:dir==EDhDirection.SOUTH?64:16),
                (short)1,(short)16,0xff808080,(byte)0,(byte)0);
        }
        List<BufferQuad> output = new ArrayList<>();
        for (List<?> list : (List<?>[])quads.get(builder)) for(Object quad:list) output.add((BufferQuad)quad);
        return output;
    }
    public static void main(String[] args) throws Exception {
        world = (IClientLevelWrapper) Proxy.newProxyInstance(DhInteriorFaceCheck.class.getClassLoader(),new Class<?>[]{IClientLevelWrapper.class},
            (p,m,a) -> m.getName().equals("getShade") ? 1.0f : null);
        ctor = LodQuadBuilder.class.getDeclaredConstructor(); ctor.setAccessible(true);
        method = Arrays.stream(ColumnBox.class.getDeclaredMethods()).filter(m -> m.getName().equals("makeAdjVerticalQuad")).findFirst().get(); method.setAccessible(true);
        quads = LodQuadBuilder.class.getDeclaredField("opaqueQuads"); quads.setAccessible(true);
        for (int bottom : new int[]{0,16,48,64,128,240,304}) for(EDhDirection dir:new EDhDirection[]{EDhDirection.NORTH,EDhDirection.SOUTH,EDhDirection.EAST,EDhDirection.WEST}) {
            for(int light:new int[]{0,15}) {
                if(!build(bottom,dir,true,bottom,bottom+16,light).isEmpty()) throw new AssertionError("Covered same-detail face at "+bottom+"/"+dir);
            }
            List<BufferQuad> exposed = build(bottom,dir,true,bottom,bottom+8,15);
            if(exposed.size()!=1 || exposed.get(0).y!=bottom+8 || exposed.get(0).widthNorthSouthOrHeight!=8)
                throw new AssertionError("Partially exposed face damaged at "+bottom+"/"+dir);
            if(build(bottom,dir,true,0,0,15).size()!=1) throw new AssertionError("Open-air face lost");
            if(build(bottom,dir,false,bottom,bottom+16,15).size()!=1) throw new AssertionError("Different-detail seam fallback lost");
        }
        System.out.println("PASS: real ColumnBox emits zero covered same-detail faces on all four borders; open air, partial exposure and mixed-detail seams preserved at Y=-64..255");
    }
}
