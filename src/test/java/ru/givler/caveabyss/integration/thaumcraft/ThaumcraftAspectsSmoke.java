package ru.givler.caveabyss.integration.thaumcraft;
import java.util.*;
import java.io.*;
import net.minecraft.item.Item;
import cpw.mods.fml.common.registry.GameRegistry;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.*;
public final class ThaumcraftAspectsSmoke {
    private static final String MOD = "caveabyss";
    private static final Map<String,Item> ITEMS = new HashMap<>();
    static void check(boolean ok, String text) { if (!ok) throw new AssertionError(text); }
    static int amount(String name, int meta, Aspect aspect) {
        AspectList list = ThaumcraftApi.objectTags.get(Arrays.asList(ITEMS.get(name),meta));
        check(list!=null,"Missing tag: "+name+":"+meta); return list.getAmount(aspect);
    }
    public static void main(String[] args) throws Exception {
        java.lang.reflect.Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
        java.lang.reflect.Field instance = cpw.mods.fml.common.Loader.class.getDeclaredField("instance"); instance.setAccessible(true);
        instance.set(null, unsafe.allocateInstance(cpw.mods.fml.common.Loader.class));
        java.lang.reflect.Field mods = cpw.mods.fml.common.Loader.class.getDeclaredField("namedMods"); mods.setAccessible(true);
        mods.set(instance.get(null), new java.util.HashMap<>());
        net.minecraft.init.Bootstrap.func_151354_b();
        Object controller = unsafe.allocateInstance(cpw.mods.fml.common.LoadController.class);
        java.lang.reflect.Field state = cpw.mods.fml.common.LoadController.class.getDeclaredField("state"); state.setAccessible(true);
        state.set(controller, cpw.mods.fml.common.LoaderState.POSTINITIALIZATION);
        java.lang.reflect.Field control = cpw.mods.fml.common.Loader.class.getDeclaredField("modController"); control.setAccessible(true); control.set(instance.get(null), controller);
        cpw.mods.fml.common.ModMetadata metadata = new cpw.mods.fml.common.ModMetadata(); metadata.modId = MOD;
        java.lang.reflect.Field active = cpw.mods.fml.common.LoadController.class.getDeclaredField("activeContainer"); active.setAccessible(true);
        active.set(controller, new cpw.mods.fml.common.DummyModContainer(metadata));

        // Verify the gateway without allowing the optional API to load.
        final ClassLoader parent = ThaumcraftAspectsSmoke.class.getClassLoader();
        ClassLoader withoutThaumcraft = new ClassLoader(parent) {
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("thaumcraft.")) throw new ClassNotFoundException("Thaumcraft deliberately absent");
                if (!name.equals("ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration")) return super.loadClass(name,resolve);
                try (InputStream input=parent.getResourceAsStream(name.replace('.','/')+".class")) {
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[4096];
                    for(int count;(count=input.read(buffer))!=-1;) bytes.write(buffer,0,count);
                    byte[] data=bytes.toByteArray();return defineClass(name,data,0,data.length);
                } catch(IOException e) {throw new ClassNotFoundException(name,e);}
            }
        };
        withoutThaumcraft.loadClass("ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration").getMethod("postInit").invoke(null);
        for (String name : new String[]{"amethyst_shard","deepslate_thaumcraft_ore","deepslate_coal_ore"}) {
            Item item = new Item(); ITEMS.put(name,item); GameRegistry.registerItem(item,name);
        }
        ThaumcraftAspects.register();

        check(amount("amethyst_shard",32767,Aspect.CRYSTAL)==2,"Amethyst shard");
        check(amount("deepslate_thaumcraft_ore",1,Aspect.AIR)==3,"Air ore metadata");
        check(amount("deepslate_thaumcraft_ore",2,Aspect.FIRE)==3,"Fire ore metadata");
        check(amount("deepslate_thaumcraft_ore",4,Aspect.EARTH)==4,"Compound earth ore amounts");
        check(amount("deepslate_thaumcraft_ore",7,Aspect.TRAP)==3,"Amber ore metadata");
        check(amount("deepslate_coal_ore",32767,Aspect.ENERGY)==2,"Vanilla ore balance");

        ThaumcraftAspects.register();

        check(amount("amethyst_shard",32767,Aspect.CRYSTAL)==2,"Amethyst shard");
        check(amount("deepslate_thaumcraft_ore",1,Aspect.AIR)==3,"Air ore metadata");
        check(amount("deepslate_thaumcraft_ore",2,Aspect.FIRE)==3,"Fire ore metadata");
        check(amount("deepslate_thaumcraft_ore",4,Aspect.EARTH)==4,"Compound earth ore amounts");
        check(amount("deepslate_thaumcraft_ore",7,Aspect.TRAP)==3,"Amber ore metadata");
        check(amount("deepslate_coal_ore",32767,Aspect.ENERGY)==2,"Vanilla ore balance");

        System.out.println("PASS explicit assignments, metadata, absent optional content, repeat registration and no-Thaumcraft gateway");
    }
}
