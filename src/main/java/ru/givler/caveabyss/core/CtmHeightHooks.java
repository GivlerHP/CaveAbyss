package ru.givler.caveabyss.core;

import java.util.BitSet;
import java.util.regex.*;

/** Signed CTM selectors. Vanilla positive indices keep their original meaning. */
public final class CtmHeightHooks {
 private CtmHeightHooks() {}
 public static BitSet getHeightListProperty(Object properties,String prefix) {
  try {
   java.lang.reflect.Method get=properties.getClass().getMethod("getString",String.class,String.class);
   return parse((String)get.invoke(properties,"minHeight"+prefix,""),(String)get.invoke(properties,"maxHeight"+prefix,""),(String)get.invoke(properties,"heights"+prefix,""));
  }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read CTM height selectors",e);}
 }
 public static BitSet parse(String minimum,String maximum,String heights) {
  minimum=minimum.trim();maximum=maximum.trim();heights=heights.trim();
  if(minimum.isEmpty() && maximum.isEmpty() && heights.isEmpty())return null;
  SignedHeights result=new SignedHeights();
  if(heights.isEmpty()) {
   int min=minimum.isEmpty()?-64:Integer.parseInt(minimum),max=maximum.isEmpty()?255:Integer.parseInt(maximum);
   result.range(min,max);
  }else {
   Pattern pattern=Pattern.compile("(-?\\d+)(?:\\s*-\\s*(-?\\d+))?");
   for(String token:heights.split("[ ,]+")) {
    Matcher m=pattern.matcher(token);if(!m.matches())continue;
    int min=Integer.parseInt(m.group(1)),max=m.group(2)==null?min:Integer.parseInt(m.group(2));result.range(min,max);
   }
  }
  return result;
 }
 private static final class SignedHeights extends BitSet {
  private final BitSet negative=new BitSet();
  void range(int min,int max) {for(int y=Math.max(-64,min);y<=Math.min(255,max);y++){if(y<0)negative.set(-y-1);else set(y);}}
  @Override public boolean get(int y){return y<0?y>=-64 && negative.get(-y-1):super.get(y);}
 }
}
