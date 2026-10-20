package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class NegativeSkyTransformer implements IClassTransformer,Opcodes {
 private static final String HOOK="ru/givler/caveabyss/lighting/NegativeSkyLighting";
 public byte[] transform(String name,String target,byte[] bytes) {
  if(bytes==null || !"net.minecraft.world.World".equals(target))return bytes;
  ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int count=0;
  for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(HOOK))return bytes;
  for(MethodNode m:node.methods) {
   String mapped=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name,m.name,m.desc),desc=FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(m.desc);
   boolean block=m.name.equals("markAndNotifyBlock"),meta=desc.equals("(IIIII)Z") && (m.name.equals("setBlockMetadataWithNotify")||mapped.equals("func_72921_c")||mapped.equals("setBlockMetadataWithNotify"));
   if(!block&&!meta)continue;
   for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==(meta?IRETURN:RETURN)) {
    InsnList code=new InsnList();if(meta)code.add(new InsnNode(DUP));code.add(new VarInsnNode(ALOAD,0));
    code.add(new VarInsnNode(ILOAD,1));code.add(new VarInsnNode(ILOAD,2));code.add(new VarInsnNode(ILOAD,3));
    code.add(new MethodInsnNode(INVOKESTATIC,HOOK,meta?"metadataChanged":"changed",meta?"(ZLnet/minecraft/world/World;III)V":"(Lnet/minecraft/world/World;III)V",false));m.instructions.insertBefore(i,code);
   }
   count++;
  }
  if(count!=2)throw new IllegalStateException("CaveAbyss skylight change hooks="+count);
  ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
 }
}
