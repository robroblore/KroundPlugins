package org.loreware.disKroundWare;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;
import io.papermc.paper.event.player.AsyncChatEvent;
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
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.ServerForumChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DisKroundWare extends JavaPlugin implements Listener, CommandExecutor {

    // ----------------- VARS -----------------
    Server kround;
    TextChannel testingChannel;
    TextChannel leaveJoinChannel;
    ServerForumChannel helpopChannel;
    DiscordApi api;
    FileConfiguration config;
    private SuperVanish superVanish;
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

        superVanish = (SuperVanish) getServer().getPluginManager().getPlugin("SuperVanish");

        api = createDiscordAPI();

        kround = api.getServerById("1278086361412538421").get();
        testingChannel = kround.getTextChannelById(
                getConf("discord.crossChat.channelID")).get();
        leaveJoinChannel = kround.getTextChannelById(
                getConf("discord.leaveJoinMessages.channelID")).get();
        helpopChannel = kround.getForumChannelById(
                getConf("discord.helpop.channelID")).get();

        createDiscordCommands();

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
        api.disconnect();
    }

    // ----------------- SETUP -----------------


    // ----------------- DISCORD -----------------

    DiscordApi createDiscordAPI(){
        return new DiscordApiBuilder()
                .setToken("MTMzNTI3Njc4NDg4NzAwNTM5NQ.GVRMH4._9IMmS5-hjUpk4GiTC_Jug8AIfdy8kH1xjzmYM")
                .addIntents(Intent.MESSAGE_CONTENT)
                .login().join();
    }

    void createDiscordCommands(){
//        api.addMessageCreateListener(event -> {
//            if (event.getMessageContent().equalsIgnoreCase("!ip")) {
//                event.getChannel().sendMessage("KroundV2 ip: 185.206.149.81:25591");
//            }
//        });

        api.addMessageCreateListener(this::onMessageCreatedDiscord);

//        SlashCommand ipCommand = SlashCommand
//                .with("ip", "Intreaba botul despre ip-ul serverului")
//                .createGlobal(api)
//                .join();

        SlashCommand onlineCommand = SlashCommand
                .with("online", "Primeste o lista cu toti playerii online pe server")
                .createGlobal(api)
                .join();

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            if(Objects.equals(slashCommandInteraction.getFullCommandName(), "online")){
                StringBuilder answer = new StringBuilder(
                        getConf("discord.commands.online")).append("\n");
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(isVanished(player)) continue;
                    answer.append(player.getName()).append("\n");
                }

                slashCommandInteraction.createImmediateResponder()
                        .setContent(answer.toString())
                        .respond();
            }
//            else if(Objects.equals(slashCommandInteraction.getFullCommandName(), "ip")){
//                slashCommandInteraction.createImmediateResponder().setContent("KroundV2 ip: 185.206.149.81:25591")
//                        .respond();
//            }
        });
    }

    public void onMessageCreatedDiscord(MessageCreateEvent event){
        if (!config.getBoolean("discord.crossChat.enabled")) return;
        if (event.getServerTextChannel().get() != testingChannel) return;
        if (event.getMessageAuthor().isBotUser() || !event.getMessageAuthor().isUser()) return;

        String username = event.getMessageAuthor().getDisplayName();

        String message = event.getMessageContent();

        Component msg = Component.text(getConf("discord.crossChat.discordToMinecraftMessage")
                .replace("{username}", username)
                .replace("{message}", message));

        Bukkit.broadcast(msg);
    }

    public void updateBotStatus(boolean leaving){
        int onlinePlayers = getServer().getOnlinePlayers().size();
        if (leaving) onlinePlayers--;

        onlinePlayers -= getVanishedCount();

        if(onlinePlayers == 1){
            api.updateActivity(ActivityType.WATCHING, getConf("discord.botActivity.onePlayer"));
            return;
        }
        api.updateActivity(ActivityType.WATCHING,
                getConf("discord.botActivity.multiplePlayers")
                        .replace("{playerCount}", String.valueOf(onlinePlayers)));
    }

    // ----------------- DISCORD -----------------


    // ----------------- MINECRAFT -----------------

    @EventHandler
    public void onChatEvent(AsyncChatEvent event){
        if (!config.getBoolean("discord.crossChat.enabled")) return;
        Player sender = event.getPlayer();
        TextComponent message = (TextComponent) event.message();

        new MessageBuilder()
                .append(getConf("discord.crossChat.minecraftToDiscordMessage")
                        .replace("{player}", sender.getName())
                        .replace("{message}", message.content()))
                .send(testingChannel);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("diskroundware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();
                    testingChannel = kround.getTextChannelById(
                            getConf("discord.crossChat.channelID")).get();
                    leaveJoinChannel = kround.getTextChannelById(
                            getConf("discord.leaveJoinMessages.channelID")).get();
                    helpopChannel = kround.getForumChannelById(
                            getConf("discord.helpop.channelID")).get();
                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("helpop") || cmd.getName().equalsIgnoreCase("report") || cmd.getName().equalsIgnoreCase("ticket")) {
                player.sendMessage("§cComanda nu este disponibila in acest moment.");



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
                new MessageBuilder()
                        .append(getConf("discord.leaveJoinMessages.joinMessage")
                                .replace("{player}", event.getPlayer().getName()))
                        .send(leaveJoinChannel);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if(!isVanished(event.getPlayer())){
            updateBotStatus(true);

            if(config.getBoolean("discord.leaveJoinMessages.enabled")){
                new MessageBuilder()
                        .append(getConf("discord.leaveJoinMessages.leaveMessage")
                                .replace("{player}", event.getPlayer().getName()))
                        .send(leaveJoinChannel);
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
                new MessageBuilder()
                        .append(getConf("discord.leaveJoinMessages.leaveMessage")
                                .replace("{player}", event.getName()))
                        .send(leaveJoinChannel);
            } else {
                new MessageBuilder()
                        .append(getConf("discord.leaveJoinMessages.joinMessage")
                                .replace("{player}", event.getName()))
                        .send(leaveJoinChannel);
            }
        }

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
