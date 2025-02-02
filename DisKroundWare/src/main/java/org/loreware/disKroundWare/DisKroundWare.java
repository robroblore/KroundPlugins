package org.loreware.disKroundWare;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.Objects;

public final class DisKroundWare extends JavaPlugin implements Listener, CommandExecutor {

    // ----------------- VARS -----------------
    Server kround;
    TextChannel testingChannel;
    DiscordApi api;
    // ----------------- VARS -----------------


    // ----------------- SETUP -----------------
    @Override
    public void onEnable() {
        System.out.println("DisKroundWare plugin enabled");


        api = createDiscordAPI();

        kround = api.getServerById("1278086361412538421").get();
        testingChannel = kround.getTextChannelById("1335292249944096798").get();
        createDiscordCommands();

        updateBotStatus(false);

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

        SlashCommand sexCommand = SlashCommand
                .with("sex", "Intreaba botul daca vrea sa faceti sex")
                .createGlobal(api)
                .join();

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            if(Objects.equals(slashCommandInteraction.getFullCommandName(), "sex")){
                slashCommandInteraction.createImmediateResponder().setContent("Hai!")
                        .respond();
            }
//            else if(Objects.equals(slashCommandInteraction.getFullCommandName(), "ip")){
//                slashCommandInteraction.createImmediateResponder().setContent("KroundV2 ip: 185.206.149.81:25591")
//                        .respond();
//            }
        });
    }

    public void onMessageCreatedDiscord(MessageCreateEvent event){
        if (event.getServerTextChannel().get() != testingChannel) return;
        if (event.getMessageAuthor().isBotUser()) return;

        String username = event.getMessageAuthor().getDisplayName();

        String message = event.getMessageContent();

        Component msg = Component.text(translateColor("&9&l<Discord>&r " + username + ": " + message));

        Bukkit.broadcast(msg);
    }

    public void updateBotStatus(boolean leaving){
        int onlinePlayers = getServer().getOnlinePlayers().size();
        if (leaving) onlinePlayers--;
        if(onlinePlayers == 1){
            api.updateActivity(ActivityType.WATCHING, " 1 jucator online!");
            return;
        }
        api.updateActivity(ActivityType.WATCHING,
                onlinePlayers + " jucatori online!");
    }

    // ----------------- DISCORD -----------------


    // ----------------- MINECRAFT -----------------

    @EventHandler
    public void onChatEvent(AsyncChatEvent event){
        Player sender = event.getPlayer();
        TextComponent message = (TextComponent) event.message();

        new MessageBuilder()
                .append(sender.getName())
                .append(": ")
                .append(message.content())
                .send(testingChannel);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        updateBotStatus(false);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        updateBotStatus(true);
    }

    // ----------------- MINECRAFT -----------------


    // ----------------- UTILS -----------------

    public String translateColor(String message){
        return message.replaceAll("&", "§");
    }

    void debug(String m){
        System.out.println(m);
    }

    // ----------------- UTILS -----------------
}
