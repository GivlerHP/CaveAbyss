package ru.givler.caveabyss.core;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

/** Reproduces the missing client-only S23 getters on a dedicated server. */
final class PacketSideSmoke {
    static void check() throws Exception {
        final String packetName="net.minecraft.network.play.server.S23PacketBlockChange";
        final String traceName=NegativeBlockTrace.class.getName();
        byte[] patched=new NegativeBlockPacketTransformer().transform(packetName,packetName,bytes(packetName));
        ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);
        Set<String> getters=new HashSet<>(Arrays.asList("func_148879_d","func_148878_e","func_148877_f","func_148880_c","func_148881_g"));
        int before=node.methods.size();node.methods.removeIf(method->getters.contains(method.name));
        if(before-node.methods.size()!=5)throw new AssertionError("S23 getter fixture changed");
        ClassWriter writer=new ClassWriter(0);node.accept(writer);final byte[] stripped=writer.toByteArray();
        ClassLoader loader=new ClassLoader(PacketSideSmoke.class.getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)) {
                    if(!name.equals(packetName)&&!name.equals(traceName)&&!name.startsWith(traceName+"$"))return super.loadClass(name,resolve);
                    Class<?> type=findLoadedClass(name);
                    if(type==null)try {
                        byte[] data=name.equals(packetName)?stripped:bytes(name);type=defineClass(name,data,0,data.length);
                    }catch(IOException error){throw new ClassNotFoundException(name,error);}
                    if(resolve)resolveClass(type);return type;
                }
            }
        };
        Class<?> packet=loader.loadClass(packetName),trace=loader.loadClass(traceName);
        Method correction=trace.getMethod("correction",packet,boolean.class);
        Field y=packet.getDeclaredField("field_148885_b");y.setAccessible(true);
        for(int coordinate:new int[]{-64,-1,0,255}) {
            Object sent=packet.newInstance();y.setInt(sent,coordinate);
            correction.invoke(null,sent,false);correction.invoke(null,sent,true);
            PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());
            try {
                packet.getMethod("writePacketData",PacketBuffer.class).invoke(sent,buffer);
                Object received=packet.newInstance();packet.getMethod("readPacketData",PacketBuffer.class).invoke(received,buffer);
                if(y.getInt(received)!=coordinate||buffer.readableBytes()!=0)throw new AssertionError("Signed S23 round-trip: "+coordinate);
            }finally{buffer.release();}
        }
        System.out.println("Dedicated-server S23 send/read without client getters; signed Y -64/-1/0/255 passed");
    }
    private static byte[] bytes(String name)throws IOException {
        try(InputStream input=PacketSideSmoke.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
            ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1)output.write(buffer,0,count);return output.toByteArray();
        }
    }
}
