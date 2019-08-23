package cc.eumc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PrivChatCommand implements CommandExecutor {

    Plugin instance = PrivChat.instance;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player)commandSender;
            if(strings.length == 1) {
                if (strings[0].equalsIgnoreCase("off")) {
                    PrivChat.UUIDTimeMap.put(PrivChat.getPlayerUUID(player), (long)-1);
                    commandSender.sendMessage(PrivChat.MESSAGE_PREFIX + "已停用快速切换 >");
                }
                else if(strings[0].equalsIgnoreCase("on")) {
                    PrivChat.UUIDTimeMap.remove(PrivChat.getPlayerUUID(player));
                    commandSender.sendMessage(PrivChat.MESSAGE_PREFIX + "已启用快速切换 >");
                }
                else {
                    player.sendMessage(PrivChat.MESSAGE_PREFIX + "使用 /chatmode §lon/off§b 启用/停用快速切换 >");
                }
                return true;
            }

            PrivChat.changeChatMode(player);
            player.sendMessage(PrivChat.MESSAGE_PREFIX + "使用 /chatmode §lon/off§b 启用/停用快速切换 >");

        }
        else if (commandSender instanceof ConsoleCommandSender) {
            commandSender.sendMessage("§b[PrivChat] Reloading configuration...");
            PrivChat.loadConfig();
        }

        return true;
    }
}
