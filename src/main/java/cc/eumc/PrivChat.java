package cc.eumc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivChat  extends JavaPlugin implements Listener {
    static int config_MinRange;
    static int config_MaxRange;
    static int DistancePerExclamation;

    static final Integer MODE_GLOBAL = 0;
    static final Integer MODE_RANGE_ONLY = 1;
    static final Integer MODE_RANGE_GLOBAL_LISTENING = 2;
    static final Integer MODE_SILENT = 3;

    static final String MESSAGE_PREFIX = "§b< ";

    Map<UUID, Long> UUIDTimeMap = new HashMap<UUID, Long>();
    Map<UUID, Integer> UUIDModeMap = new HashMap<UUID, Integer>();

    public static Plugin instance;

    public void onEnable() {
        instance = this;

        loadConfig();

        Bukkit.getPluginManager().registerEvents(this,this);

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT) {
                    @Override
                    public void onPacketSending(PacketEvent e) {
                        if (e.getPlayer() == null)
                            return;
                        if (e.getPacket() == null)
                            return;

//"color":"aqua","text":"<
                        try {

                            // String message = "";
                            try {
                                String jsonMessage = e.getPacket().getChatComponents().getValues().get(0).getJson();
                                if (jsonMessage != null)
                                    if(jsonMessage.contains("\"color\":\"aqua\",\"text\":\"< ")) // is it a system message?
                                        return;
                            } catch (Throwable ignore) { }

                        } catch(Throwable ignore) { }

                        UUID uuid = e.getPlayer().getPlayerProfile().getId();
                        if (!UUIDModeMap.containsKey(uuid)) {
                            return;
                        }
                        Integer playerMode = UUIDModeMap.get(uuid);
                        if (playerMode == MODE_RANGE_ONLY || playerMode == MODE_SILENT) {
                            e.setCancelled(true);
                        }
                    }
                }
        );

        sendInfo("Enabled");
    }

    public void onDisable() {
        sendInfo("Disabled");
    }

    public static void sendSevere(String message) {
        Bukkit.getServer().getLogger().severe("[桉树叶] [PrivChat] " + message);
    }

    public static void sendWarn(String message) {
        Bukkit.getServer().getLogger().warning("[桉树叶] [PrivChat] " + message);
    }

    public static void sendInfo(String message) {
        Bukkit.getServer().getLogger().info("[桉树叶] [PrivChat] " + message);
    }

    public static void loadConfig() {

        File file = new File(instance.getDataFolder(), "config.yml");

        if (!file.exists()) {
            instance.saveDefaultConfig();
        }

        instance.reloadConfig();

        config_MinRange = instance.getConfig().getInt("Settings.MinRange",16);
        config_MaxRange = instance.getConfig().getInt("Settings.MaxRange",32);

        if (config_MinRange > config_MaxRange) {
            config_MinRange += config_MaxRange;
            config_MaxRange = config_MinRange - config_MaxRange;
            config_MaxRange -= config_MinRange;
        }

        if (config_MinRange < config_MaxRange) {
            DistancePerExclamation = (config_MaxRange-config_MinRange)/3;
        }
        else if (config_MaxRange == config_MinRange) {
            DistancePerExclamation = 0;
        }

        sendInfo("Min chatting range: " + config_MinRange);
        sendInfo("Max chatting range: " + config_MaxRange);
        sendInfo("Range Increasing : " + DistancePerExclamation + " / Exclamation Mark(!)");

    }


    // MARK: - Event Handlers

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID uuid = getPlayerUUID(player);
         ;
        if (!UUIDModeMap.containsKey(uuid)) {
            return;
        }

        Integer mode = UUIDModeMap.get(uuid);

        if (mode == MODE_SILENT) {
            player.sendMessage(MESSAGE_PREFIX + "§e禅模式下无法发送消息, 双击空气切换模式 §b>");
            e.setCancelled(true);
            return;
        }

        if (mode == MODE_RANGE_ONLY || mode == MODE_RANGE_GLOBAL_LISTENING) {
            e.setCancelled(true);
            sendMessageRange(player, e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        UUID uuid = getPlayerUUID(player);
        long currentTime = System.currentTimeMillis();

        if (e.getAction() == Action.LEFT_CLICK_AIR) {
            if (UUIDTimeMap.containsKey(uuid)) {
                if (currentTime - UUIDTimeMap.get(uuid) < 1500) {
                    changeChatMode(player);
                    UUIDTimeMap.remove(uuid);
                    return;
                }
            }
            UUIDTimeMap.put(uuid,currentTime);
        }
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        UUID uuid = e.getPlayerProfile().getId();
        if (uuid != null) {
            UUIDModeMap.put(uuid, 0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.getPlayer().sendMessage(MESSAGE_PREFIX + "双击空气来切换聊天模式 >");
    }


    // MARK: - Helper methods

    public void changeChatMode(Player player) {
        UUID uuid = getPlayerUUID(player);
        Integer mode = 0;

        if (UUIDModeMap.containsKey(uuid)) {
            mode = UUIDModeMap.get(uuid) + 1;
            if (mode > MODE_SILENT) mode = 0;
        }

        UUIDModeMap.put(uuid, mode); // !important
        UUIDTimeMap.remove(uuid);

        String description = "聊天模式已切换";
        String short_description = description;

        if (mode == MODE_GLOBAL) {
            description = "你的消息可能被所有玩家接收";
            short_description = "全球模式";
        }
        else if (mode == MODE_RANGE_ONLY) {
            description = "离你一定范围内的玩家能接收到你的消息";
            short_description = "猫模式";
        }
        else if (mode == MODE_RANGE_GLOBAL_LISTENING) {
            description = "你可以接收全球聊天的内容";
            short_description = "猫模式 & 全球监听";
        }
        else if (mode == MODE_SILENT) {
            description = "你不会看到任何聊天内容";
            short_description = "禅模式";
        }

        player.sendTitle("§7⇋","§7§l"+ short_description ,2,15,4);
        player.sendMessage(MESSAGE_PREFIX + description + " >");
    }

    public void sendMessageRange(Player player, String message) {
        if (message.length() > 80) {
            player.sendMessage(MESSAGE_PREFIX + "§e抱歉，发送的消息太长（>80字符）。 §r为保证大部分玩家能完整阅读消息，请精简后发送§b >");
        } else {
            java.util.Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
            String plrName = player.getDisplayName();

            Integer spreadDistance = Math.toIntExact(message.chars().filter(num -> num == '!').count());
            spreadDistance += Math.toIntExact(message.chars().filter(num -> num == '！').count());
            spreadDistance = spreadDistance>3 ? config_MaxRange : (DistancePerExclamation*spreadDistance+config_MinRange);

            for (Player plr : players) {
                if (canPlayerReceiveRangeMessage(plr)) {
                    Integer distance = (int)plr.getLocation().distance(player.getLocation());
                    if (distance <= spreadDistance) {
                        plr.sendActionBar("§3" + plrName + ": §b" + message);
                    }
                }
            }
            player.sendActionBar("§3" + plrName + ": §b" + message);
        }
    }

    private static UUID getPlayerUUID(Player player) {
        UUID uuid = player.getPlayerProfile().getId();
        if (uuid == null){
            uuid = UUID.fromString(player.getDisplayName());
        }
        return uuid;
    }

    private Boolean canPlayerReceiveRangeMessage(Player player) {
        UUID uuid = getPlayerUUID(player);
        if (UUIDModeMap.containsKey(uuid)) {
            return !( UUIDModeMap.get(uuid) == MODE_SILENT );
        }
        return true;
    }
}