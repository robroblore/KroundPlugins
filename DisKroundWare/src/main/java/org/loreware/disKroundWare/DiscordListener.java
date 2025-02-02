package org.loreware.disKroundWare;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordListener extends ListenerAdapter {

    DisKroundWare diskround = (DisKroundWare) Bukkit.getPluginManager().getPlugin("DisKroundWare");

    public void createCommands(){
        CommandListUpdateAction commands = diskround.jda.updateCommands();

        commands.addCommands(
                Commands.slash("online", "Primeste o lista cu toti playerii online pe server")
                        .setContexts(InteractionContextType.GUILD)
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
        );

        commands.queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;

        switch (event.getName())
        {
            case "online":
                StringBuilder answer = new StringBuilder(
                        diskround.getConf("discord.commands.online.playersOn")).append("\n");

                boolean hasPlayers = false;

                for(Player player : Bukkit.getOnlinePlayers()){
                    if(diskround.isVanished(player)) continue;
                    answer.append(player.getName()).append("\n");
                    hasPlayers = true;
                }

                if(!hasPlayers){
                    event.reply(diskround.getConf("discord.commands.online.noPlayers")).queue();
                } else{
                    event.reply(answer.toString()).queue();
                }
                break;
            default:
                event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!event.isFromGuild()) return;
        User Author = event.getAuthor();
        Member member = event.getMember();

        if (event.getChannel() == diskround.crossChatChannel) {
            if(!diskround.config.getBoolean("discord.crossChat.enabled")) return;
            TextChannel channel = event.getChannel().asTextChannel();
            if (Author.isBot()) return;

            String message = event.getMessage().getContentDisplay();

            diskround.broadcastMessage(diskround.getConf("discord.crossChat.discordToMinecraftMessage")
                    .replace("{username}", member.getEffectiveName())
                    .replace("{message}", message));
        }

        else if (event.getChannel() instanceof ThreadChannel) {
            ThreadChannel channel = event.getChannel().asThreadChannel();

            if(!channel.getParentChannel().getId().equals(diskround.getConf("discord.helpop.channelID"))) return;
        }
    }

}
