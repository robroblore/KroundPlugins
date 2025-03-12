package org.loreware.disKroundWare;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class DisKroundWare extends JavaPlugin implements Listener, CommandExecutor {

    // ----------------- VARS -----------------
    FileConfiguration config;
    FileConfiguration links;
    private SuperVanish superVanish;
    DiscordListener discordListener;
    JDA jda;
    Guild kround;
    TextChannel crossChatChannel;
    TextChannel leaveJoinChannel;
    TextChannel serverCommandsChannel;
    ForumChannel helpopChannel;
    // ----------------- VARS -----------------


    // ----------------- SETUP -----------------
    @Override
    public void onEnable() {
        System.out.println("DisKroundWare plugin enabled");

        saveDefaultConfig();
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        links = getLinksConfig();

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
        helpopChannel = kround.getForumChannelById(getConf("discord.helpop.channelID"));
        serverCommandsChannel = kround.getTextChannelById(getConf("discord.serverCommands.channelID"));

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

        message.content(message.content().replaceAll("@everyone", "@ everyone"));

        crossChatChannel.sendMessage(getConf("discord.crossChat.message")
                .replace("{player}", sender.getName())
                .replace("{message}", message.content()))
                .queue();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event){
        if (!config.getBoolean("discord.serverCommands.enabled")) return;

        String message = event.getMessage();
        String player = event.getPlayer().getName();
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        List<String> fullCommand = List.of(message.split(" "));

        String command = fullCommand.getFirst();

        if(command.equals("/l") || command.equals("/login") || command.equals("/register") || command.equals("/log")
                || command.equals("/changepassword") || command.equals("/changepass") ||
                command.equals("/reg") || command.equals("/unregister") || command.equals("/unreg")){

            return;
        }

        serverCommandsChannel.sendMessage(getConf("discord.serverCommands.message")
                .replace("{player}", player)
                .replace("{command}", message)
                .replace("{time}", currentDateTime.format(formatter))).queue();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsoleCommand(ServerCommandEvent event) {
        String command = event.getCommand(); // The command without "/"

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        serverCommandsChannel.sendMessage(getConf("discord.serverCommands.message")
                .replace("{player}", "Console")
                .replace("{command}", command)
                .replace("{time}", currentDateTime.format(formatter))).queue();
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
                    serverCommandsChannel = kround.getTextChannelById(getConf("discord.serverCommands.channelID"));
                    leaveJoinChannel = kround.getTextChannelById(getConf("discord.leaveJoinMessages.channelID"));
                    helpopChannel = kround.getForumChannelById(getConf("discord.helpop.channelID"));
                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("helpop") || cmd.getName().equalsIgnoreCase("report") || cmd.getName().equalsIgnoreCase("ticket")) {
                if(args.length == 0){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.helpop.usage"));
                    return true;
                }

                String message = String.join(" ", args);

                player.sendMessage(getConf("messages.prefix") + getConf("minecraft.helpop.sent"));

                boolean threadExists = false;
                for (ThreadChannel thread : helpopChannel.getThreadChannels()) {
                    if (thread.getName().equalsIgnoreCase(player.getName())) {
                        thread.sendMessage(player.getName() + ": " + message).queue();
                        threadExists = true;
                        break;
                    }
                }

                if (!threadExists) {
                    helpopChannel.createForumPost(player.getName(), MessageCreateData
                                    .fromContent(player.getName() + ": " + message))
                            .queue();
                }

                for(Player staff : getServer().getOnlinePlayers()){
                    if(staff.hasPermission("diskroundware.helpop")){
                        staff.sendMessage(getConf("messages.prefix") + getConf("minecraft.helpop.message")
                                .replace("{player}", player.getName())
                                .replace("{message}", message));
                    }
                }
            }

            else if(cmd.getName().equalsIgnoreCase("reply")){
                if(args.length < 2){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.reply.usage"));
                    return true;
                }
                Player target = getServer().getPlayer(args[0]);

                if(target == null){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.reply.noPlayer")
                            .replace("{player}", args[0]));
                    return true;
                }

                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                boolean threadExists = false;

                for (ThreadChannel thread : helpopChannel.getThreadChannels()) {
                    if (thread.getName().equalsIgnoreCase(target.getName())) {
                        thread.sendMessage(player.getName() + ": " + message).queue();
                        threadExists = true;
                        break;
                    }
                }

                for(Player staff : getServer().getOnlinePlayers()){
                    if(staff.hasPermission("diskroundware.helpop") && staff != player){
                        staff.sendMessage(getConf("messages.prefix") + getConf("minecraft.reply.staffToStaff")
                                .replace("{staff}", player.getName())
                                .replace("{player}", target.getName()));
                    }
                }

                if(!threadExists){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.reply.noThread")
                            .replace("{player}", target.getName()));
                    return true;
                }

                target.sendMessage(getConf("messages.prefix") + getConf("minecraft.reply.fromMinecraftMessage")
                        .replace("{staff}", player.getName())
                        .replace("{message}", message));

                Title title = Title.title(
                        Component.text(getConf("minecraft.helpop.title")),
                        Component.text(getConf("minecraft.helpop.subtitle")
                                .replace("{staff}", player.getName())
                                .replace("{platform}", "minecraft")),
                        Title.Times.times(Duration.ofSeconds(1),
                                Duration.ofSeconds(config.getInt("minecraft.helpop.titleDuration")), Duration.ofSeconds(1))
                );

                target.showTitle(title);
                target.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, (float) config.getDouble("minecraft.helpop.volume"), 1f);

                player.sendMessage(getConf("messages.prefix") +
                        getConf("minecraft.reply.success").replace("{player}", target.getName()));
            }

            else if(cmd.getName().equalsIgnoreCase("close")){

                if(args.length < 1){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.close.usage"));
                    return true;
                }

                String pName = args[0];

                boolean threadExists = false;

                for (ThreadChannel thread : helpopChannel.getThreadChannels()) {
                    if (thread.getName().equalsIgnoreCase(pName)) {
                        thread.sendMessage(getConf("discord.close.message")
                                .replace("{staff}", player.getName())
                                .replace("{platform}", "minecraft")).queue(response -> {
                            thread.getManager().setName(thread.getName() + " (closed)").setArchived(true).setLocked(true).queue();
                        });
                        threadExists = true;
                        break;
                    }
                }

                if(threadExists){
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.close.success")
                            .replace("{player}", pName));

                    Player target = getServer().getPlayer(pName);

                    if(target != null){
                        target.sendMessage(getConf("messages.prefix") + getConf("minecraft.close.fromMinecraftClose")
                                .replace("{staff}", player.getName()));
                    }

                } else {
                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.close.noThread")
                            .replace("{player}", pName));
                }
            }

            else if (cmd.getName().equalsIgnoreCase("link")) {
                String entry = links.getString(String.valueOf(player.getUniqueId()));

                if (args.length == 0 || args[0].equalsIgnoreCase("discord")) {
                    if(entry != null && entry.length() > 6){
                        player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.alreadyLinked"));
                        player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.removeUsage"));
                        return true;
                    }

                    int code = new Random().nextInt(100000, 999999);
                    links.set(String.valueOf(player.getUniqueId()), String.valueOf(code));

                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.code")
                            .replace("{code}", String.valueOf(code)));

                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.codeUsage")
                            .replace("{code}", String.valueOf(code)));

                    saveLinksConfig();
                    return true;
                } else if(args[0].equalsIgnoreCase("confirm")){
                    if(entry == null || !entry.startsWith("?")){
                        for(String line : getConfList("minecraft.link.notLinked")){
                            player.sendMessage(getConf("messages.prefix") + line);
                        }
                        return true;
                    }

                    links.set(String.valueOf(player.getUniqueId()), entry.substring(1));
                    saveLinksConfig();

                    kround.retrieveMemberById(entry.substring(1)).queue(member -> {
                        player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.success")
                                .replace("{username}", member.getEffectiveName()));
                    });

                    return true;
                } else if(args[0].equalsIgnoreCase("remove")) {
                    // Remove the link
                    links.set(String.valueOf(player.getUniqueId()), null);

                    player.sendMessage(getConf("messages.prefix") + getConf("minecraft.link.removeSuccess"));

                    saveLinksConfig();
                    return true;
                }
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

        else if (cmd.getName().equalsIgnoreCase("helpop") || cmd.getName().equalsIgnoreCase("report") || cmd.getName().equalsIgnoreCase("ticket")) {
            return Collections.emptyList();
        }

        else if (cmd.getName().equalsIgnoreCase("reply")) {
            if(args.length == 1){
                List<String> list = new ArrayList<>();
                for(ThreadChannel thread : helpopChannel.getThreadChannels()){
                    list.add(thread.getName());
                }
                return list;
            }

            if(args.length > 1){
                return Collections.emptyList();
            }
        }

        else if (cmd.getName().equalsIgnoreCase("close")) {
            if(args.length == 1){
                List<String> list = new ArrayList<>();
                for(ThreadChannel thread : helpopChannel.getThreadChannels()){
                    list.add(thread.getName());
                }
                return list;
            }

            if(args.length > 1){
                return Collections.emptyList();
            }
        }

        else if (cmd.getName().equalsIgnoreCase("link")) {
            List<String> list = new ArrayList<>();

            list.add("discord");
            list.add("confirm");
            list.add("remove");
            return list;
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

        getServer().broadcast(msg);
    }

    // ----------------- MINECRAFT -----------------


    // ----------------- UTILS -----------------

    public YamlConfiguration getLinksConfig(){
        File linksFile = new File(getDataFolder(), "links.yml");
        if (!linksFile.exists()) {
            saveResource("links.yml", false);
        }

        return YamlConfiguration.loadConfiguration(linksFile);
    }

    public void saveLinksConfig(){
        File linksFile = new File(getDataFolder(), "links.yml");
        try {
            links.save(linksFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
