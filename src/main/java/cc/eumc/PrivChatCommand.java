package cc.eumc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PrivChatCommand implements CommandExecutor {

    Plugin instance = PrivChat.instance;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player && !commandSender.isOp()) {
            commandSender.sendMessage("§c[PrivChat] Permission Denied");
            return true;
        }
        commandSender.sendMessage("§b[PrivChat] Reloading configuration...");
        PrivChat.loadConfig();
        return true;
    }
}
