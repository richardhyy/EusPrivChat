package cc.eumc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.sun.org.apache.xpath.internal.operations.Bool;
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
    static Integer config_MinRange;
    static Integer config_MaxRange;
    static Boolean config_Trigger_PreventFalseTriggering;
    static String config_Trigger_MouseButton;
    static Integer config_Trigger_MaxClickInterval;
    static Material config_Trigger_ItemInMainHand;
    static Boolean config_Trigger_Sneaking;
    static Integer DistancePerExclamation;

    static final Integer MODE_GLOBAL = 0;
    static final Integer MODE_RANGE_ONLY = 1;
    static final Integer MODE_RANGE_GLOBAL_LISTENING = 2;
    static final Integer MODE_SILENT = 3;


    static final String MESSAGE_PREFIX = "§b< ";

    static Map<UUID, Long> UUIDTimeMap = new HashMap<UUID, Long>();
    static Map<UUID, Integer> UUIDModeMap = new HashMap<UUID, Integer>();

    public static Plugin instance;

    public void onEnable() {
        instance = this;

        loadConfig();

        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getPluginCommand("chatmode").setExecutor(new PrivChatCommand());

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

        config_Trigger_PreventFalseTriggering = instance.getConfig().getBoolean("Settings.Trigger.PreventFalseTriggering",true);
        config_Trigger_MouseButton = instance.getConfig().getString("Settings.Trigger.MouseButton","left")=="right"?"right":"left";
        config_Trigger_MaxClickInterval = instance.getConfig().getInt("Settings.Trigger.MaxClickInterval",800);
        config_Trigger_ItemInMainHand = Material.getMaterial(instance.getConfig().getString("Settings.Trigger.ItemInMainHand","AIR"));
        config_Trigger_Sneaking = instance.getConfig().getBoolean("Settings.Trigger.Sneaking",true);

        if(config_Trigger_ItemInMainHand == null) config_Trigger_ItemInMainHand = Material.AIR;

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

        sendInfo("Trigger > PreventFalseTriggering: " + (config_Trigger_PreventFalseTriggering?"enabled":"disabled"));
        sendInfo("Trigger > MouseButton: " + config_Trigger_MouseButton);
        sendInfo("Trigger > MaxClickInterval: " + config_Trigger_MaxClickInterval);
        sendInfo("Trigger > ItemInMainHand: " + config_Trigger_ItemInMainHand.toString());
        sendInfo("Trigger > Sneaking: " + (config_Trigger_Sneaking?"required":"optional"));

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

        if (config_Trigger_PreventFalseTriggering) {
            // idea provided by Ghost_chu
            if (!(player.getLocation().getPitch() < -80)) return;
        }

        if (config_Trigger_Sneaking) {
            if (!player.isSneaking()) return;
        }

        if (player.getInventory().getItemInMainHand().getType() != config_Trigger_ItemInMainHand) return;

        if (config_Trigger_MouseButton == "left") {
            if (e.getAction() != Action.LEFT_CLICK_AIR)
                return;
        }
        else {
            if (e.getAction() != Action.RIGHT_CLICK_AIR)
                return;
        }

        UUID uuid = getPlayerUUID(player);
        long currentTime = System.currentTimeMillis();

        if (UUIDTimeMap.containsKey(uuid)) {
            long lastTime = UUIDTimeMap.get(uuid);

            if (lastTime == -1) return; // for the player has disabled the QuickChangeChatMode function

            if (currentTime - lastTime < config_Trigger_MaxClickInterval) {
                changeChatMode(player);
                UUIDTimeMap.remove(uuid);
                return;
            }
        }
        UUIDTimeMap.put(uuid, currentTime);
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
        e.getPlayer().sendMessage(MESSAGE_PREFIX + "双击空气快速切换聊天模式 或使用 /chatmode 手动切换 >");
    }


    // MARK: - Helper methods

    public static void changeChatMode(Player player) {
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

    protected static UUID getPlayerUUID(Player player) {
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