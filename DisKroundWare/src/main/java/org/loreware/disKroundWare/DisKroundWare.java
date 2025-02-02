package org.loreware.disKroundWare;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public final class DisKroundWare extends JavaPlugin implements Listener, CommandExecutor {

    // ----------------- VARS -----------------
    FileConfiguration config;
    private SuperVanish superVanish;
    DiscordListener discordListener;
    JDA jda;
    Guild kround;
    TextChannel crossChatChannel;
    TextChannel leaveJoinChannel;
    // ----------------- VARS -----------------


    // ----------------- SETUP -----------------
    @Override
    public void onEnable() {
        System.out.println("DisKroundWare plugin enabled");

        saveResource("config.yml", /* replace */ true);
        config = getConfig();
//        saveDefaultConfig();
//        config = getConfig();
//        config.options().copyDefaults(true);
//        saveConfig();

        discordListener = new DiscordListener();

        try{
            jda = JDABuilder.createLight(
                            "MTMzNTI3Njc4NDg4NzAwNTM5NQ.Gmnwdt.v4BHSY86ziJvCc9TxT6cKE9HNmK7UrAGV0AXXQ",
                            EnumSet.of(
                                    GatewayIntent.GUILD_MESSAGES,
                                    GatewayIntent.MESSAGE_CONTENT,
                                    GatewayIntent.GUILD_MEMBERS,
                                    GatewayIntent.GUILD_MODERATION
                                    )
                    )
                    .setActivity(Activity.watching("your messages"))
                    .addEventListeners(discordListener)
                    .build();

            jda.awaitReady();
            getLogger().info("JDA is ready!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        kround = jda.getGuildById(getConf("discord.guildID"));
        crossChatChannel = kround.getTextChannelById(getConf("discord.crossChat.channelID"));
        leaveJoinChannel = kround.getTextChannelById(getConf("discord.leaveJoinMessages.channelID"));

        superVanish = (SuperVanish) getServer().getPluginManager().getPlugin("SuperVanish");

        discordListener.createCommands();

        new BukkitRunnable() {
            @Override
            public void run() {
                updateBotStatus(false);
            }
        }.runTaskLater(this, 100);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        System.out.println("DisKroundWare plugin disabled");

        if (jda != null) {
            jda.shutdown();
        }
    }

    // ----------------- SETUP -----------------


    // ----------------- DISCORD -----------------

    public void updateBotStatus(boolean leaving){
        int onlinePlayers = getServer().getOnlinePlayers().size();
        if (leaving) onlinePlayers--;

        onlinePlayers -= getVanishedCount();

        if(onlinePlayers == 1){
            jda.getPresence().setActivity(Activity.watching(getConf("discord.botActivity.onePlayer")));
            return;
        }
        jda.getPresence().setActivity(Activity.watching(
                getConf("discord.botActivity.multiplePlayers")
                .replace("{playerCount}", String.valueOf(onlinePlayers))));
    }

    // ----------------- DISCORD -----------------


    // ----------------- MINECRAFT -----------------

    @EventHandler
    public void onChatEvent(AsyncChatEvent event){
        if (!config.getBoolean("discord.crossChat.enabled")) return;
        Player sender = event.getPlayer();
        TextComponent message = (TextComponent) event.message();

        crossChatChannel.sendMessage(getConf("discord.crossChat.minecraftToDiscordMessage")
                .replace("{player}", sender.getName())
                .replace("{message}", message.content()))
                .queue();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("diskroundware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();
                    kround = jda.getGuildById(getConf("discord.guildID"));
                    crossChatChannel = kround.getTextChannelById(getConf("discord.crossChat.channelID"));
                    leaveJoinChannel = kround.getTextChannelById(getConf("discord.leaveJoinMessages.channelID"));
                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("helpop") || cmd.getName().equalsIgnoreCase("report") || cmd.getName().equalsIgnoreCase("ticket")) {
                player.sendMessage("§cComanda nu este disponibila in acest moment.");

                //TODO: Helpop command

            }

            else if(cmd.getName().equalsIgnoreCase("test")){
                Player target = getServer().getPlayer("robroblore");


                player.sendMessage(String.valueOf(isVanished(target)));
            }
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("diskroundware")){
            if(sender instanceof Player){
                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if(!isVanished(event.getPlayer())){
            updateBotStatus(false);

            if(config.getBoolean("discord.leaveJoinMessages.enabled")){
                leaveJoinChannel.sendMessage(getConf("discord.leaveJoinMessages.joinMessage")
                        .replace("{player}", event.getPlayer().getName())).queue();
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if(!isVanished(event.getPlayer())){
            updateBotStatus(true);

            if(config.getBoolean("discord.leaveJoinMessages.enabled")){
                if(config.getBoolean("discord.leaveJoinMessages.enabled")){
                    leaveJoinChannel.sendMessage(getConf("discord.leaveJoinMessages.leaveMessage")
                            .replace("{player}", event.getPlayer().getName())).queue();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerVanish(PlayerVanishStateChangeEvent event){
        new BukkitRunnable() {
            @Override
            public void run() {
                updateBotStatus(false);
            }
        }.runTaskLater(this, 20);

        if(config.getBoolean("discord.leaveJoinMessages.enabled")){
            if(event.isVanishing()){
                leaveJoinChannel.sendMessage(getConf("discord.leaveJoinMessages.leaveMessage")
                        .replace("{player}", event.getName())).queue();
            } else {
                leaveJoinChannel.sendMessage(getConf("discord.leaveJoinMessages.joinMessage")
                        .replace("{player}", event.getName())).queue();
            }
        }

    }

    public void broadcastMessage(String message){
        Component msg = Component.text(translateColor(message));

        Bukkit.broadcast(msg);
    }

    // ----------------- MINECRAFT -----------------


    // ----------------- UTILS -----------------

    public String translateColor(String message){
        return message.replaceAll("&", "§").replaceAll("§§", "&");
    }

    void debug(String m){
        System.out.println(m);
    }

    public String getConf(String path){
        return translateColor(config.getString(path, String.format("&4&l[entry %s not found]", path)));
    }

    public List<String> getConfList(String path){
        List<String> list = new ArrayList<>();
        for (String line : config.getStringList(path)){
            list.add(translateColor(line));
        }
        return list;
    }

    public boolean isVanished(Player player){
        return superVanish.getVanishStateMgr().isVanished(player.getUniqueId());
    }

    public int getVanishedCount(){
        return superVanish.getVanishStateMgr().getOnlineVanishedPlayers().size();
    }
    // ----------------- UTILS -----------------
}
