package ru.givler.caveabyss.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** Manual persistence probe for the negative range. */
public final class ProbeCommand extends CommandBase {
    @Override
    public String getCommandName() { return "caveabyssprobe"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/caveabyssprobe set|get <x> [y] <z>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if ((args.length != 3 && args.length != 4) || !(args[0].equals("set") || args[0].equals("get"))) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }
        int x = parseInt(sender, args[1]);
        int y = args.length == 4 ? parseInt(sender, args[2]) : -1;
        int z = parseInt(sender, args[args.length - 1]);
        if (y < -64 || y >= 0) {
            sender.addChatMessage(new ChatComponentText("Y must be between -64 and -1"));
            return;
        }
        World world = sender.getEntityWorld();
        if (args[0].equals("set")) {
            boolean changed = world.setBlock(x, y, z, Blocks.stone, 0, 3);
            sender.addChatMessage(new ChatComponentText("Y=" + y + " changed=" + changed));
        }
        sender.addChatMessage(new ChatComponentText("Y=" + y + " block=" + world.getBlock(x, y, z).getUnlocalizedName()
                + ", metadata=" + world.getBlockMetadata(x, y, z)));
    }
}
