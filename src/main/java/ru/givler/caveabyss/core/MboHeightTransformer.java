package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Optional MBO observer and MCPatcher height adapters; no hard dependency. */
public final class MboHeightTransformer implements IClassTransformer,Opcodes {
 public byte[] transform(String name,String target,byte[] bytes) {
  if(bytes==null || !"com.prupe.mcpatcher.ctm.TileOverride".equals(target))return bytes;
  ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int count=0;
  for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("ru/givler/caveabyss/core/CtmHeightHooks"))return bytes;
  for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray()) {
   if(i instanceof MethodInsnNode) {
    MethodInsnNode call=(MethodInsnNode)i;
    if(call.owner.equals("com/prupe/mcpatcher/mal/biome/BiomeAPI") && call.name.equals("getHeightListProperty")) {
     call.owner="ru/givler/caveabyss/core/CtmHeightHooks";call.desc="(Ljava/lang/Object;Ljava/lang/String;)Ljava/util/BitSet;";count++;
    }
   }
  }
  if(count!=1)throw new IllegalStateException("CaveAbyss optional height anchors changed: "+target+" "+count);
  ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
 }
}
