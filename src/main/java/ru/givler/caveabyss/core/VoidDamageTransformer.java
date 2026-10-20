package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Move only natural void damage below the extended world; commands retain their damage. */
public final class VoidDamageTransformer implements IClassTransformer,Opcodes {
 public byte[] transform(String name,String target,byte[] bytes) {
  if(bytes==null || !"net.minecraft.entity.Entity".equals(target))return bytes;
  ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int count=0;
  for(MethodNode m:node.methods) {
   String mapped=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name,m.name,m.desc);
   if(!m.desc.equals("()V") || !(m.name.equals("onEntityUpdate") || mapped.equals("func_70030_z") || mapped.equals("onEntityUpdate")))continue;
   for(AbstractInsnNode i:m.instructions.toArray()) {
    if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("ru/givler/caveabyss/handler/VoidDamageGuard"))return bytes;
    if(!(i instanceof LdcInsnNode) || !Double.valueOf(-64D).equals(((LdcInsnNode)i).cst))continue;
    InsnList code=new InsnList();code.add(new VarInsnNode(ALOAD,0));
    code.add(new MethodInsnNode(INVOKESTATIC,"ru/givler/caveabyss/handler/VoidDamageGuard","naturalThreshold","(Lnet/minecraft/entity/Entity;)D",false));
    m.instructions.insertBefore(i,code);m.instructions.remove(i);count++;
   }
  }
  if(count!=1)throw new IllegalStateException("CaveAbyss natural void threshold anchors="+count);
  ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
 }
}
