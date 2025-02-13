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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    DisKroundWare diskround = (DisKroundWare) Bukkit.getPluginManager().getPlugin("DisKroundWare");

    public void createCommands(){
        CommandListUpdateAction commands = diskround.jda.updateCommands();

        commands.addCommands(
                Commands.slash("online", "Primeste o lista cu toti playerii online pe server")
                        .setContexts(InteractionContextType.GUILD)
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
        );

        commands.addCommands(
                Commands.slash("close", "Inchide un mesaj de helpop")
                        .setContexts(InteractionContextType.GUILD)
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
        );

        commands.addCommands(
                Commands.slash("link", "Conecteaza contul de discord cu cel de minecraft")
                        .setContexts(InteractionContextType.GUILD)
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .addOption(OptionType.STRING, "code", "Codul de conectare", true)
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

                for(Player player : diskround.getServer().getOnlinePlayers()){
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

            case "close":
                if(!(event.getChannel() instanceof ThreadChannel)){
                    event.reply("Aceasta comanda trebuie folosita numai pe un thread de helpop")
                            .setEphemeral(true).queue();
                    return;
                }
                if(event.getChannel().asThreadChannel().getParentChannel() != diskround.helpopChannel){
                    event.reply("Aceasta comanda trebuie folosita numai pe un thread de helpop")
                            .setEphemeral(true).queue();
                    return;
                }

                Player player = diskround.getServer().getPlayer(event.getChannel().getName());

                if(player != null){
                    player.sendMessage(diskround.getConf("messages.prefix") + diskround.getConf("minecraft.close.fromDiscordClose")
                            .replace("{username}", event.getMember().getEffectiveName()));
                }

                event.reply(diskround.getConf("discord.close.message")
                        .replace("{staff}", event.getMember().getEffectiveName())
                        .replace("{platform}", "discord")).setEphemeral(false).queue(response -> {
                    event.getChannel().asThreadChannel().getManager().setName(event.getChannel().getName() + " (closed)").setArchived(true).setLocked(true).queue();
                });
                break;

            case "link":
                String code = event.getOption("code").getAsString();

                for(String uuid : diskround.links.getKeys(false)){
                    String entry = diskround.links.getString(uuid);
                    if(entry != null && entry.equals(code)){
                        diskround.links.set(uuid, "?" + event.getMember().getId());
                        diskround.saveLinksConfig();

                        UUID playerUUID = UUID.fromString(uuid);

                        event.reply(diskround.getConf("discord.commands.link.success")
                                .replace("{player}", diskround.getServer().getOfflinePlayer(playerUUID).getName())).setEphemeral(true).queue();
                        return;
                    }
                }


                event.reply(diskround.getConf("discord.commands.link.noCode")).setEphemeral(true).queue();


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

            diskround.broadcastMessage(diskround.getConf("minecraft.crossChat.message")
                    .replace("{username}", member.getEffectiveName())
                    .replace("{message}", message));
        }

        else if(!(event.getChannel() instanceof ThreadChannel)){
            return;
        }

        else if (event.getChannel().asThreadChannel().getParentChannel() == diskround.helpopChannel) {
            if(!diskround.config.getBoolean("discord.helpop.enabled")) return;
            ThreadChannel channel = event.getChannel().asThreadChannel();
            if (Author.isBot()) return;

            String message = event.getMessage().getContentDisplay();

            Player player = diskround.getServer().getPlayer(channel.getName());

            if (player == null) {
                event.getMessage().reply(diskround.getConf("discord.helpop.playerOffline")
                        .replace("{player}", channel.getName())).queue();
                return;
            }

            for(Player staff : diskround.getServer().getOnlinePlayers()){
                if(staff.hasPermission("diskroundware.helpop")){
                    staff.sendMessage(diskround.getConf("messages.prefix") +
                            diskround.getConf("minecraft.reply.staffToStaff")
                            .replace("{staff}", member.getEffectiveName())
                            .replace("{player}", player.getName()));
                }
            }

            player.sendMessage(diskround.getConf("messages.prefix") + diskround.getConf("minecraft.reply.fromDiscordMessage")
                    .replace("{username}", member.getEffectiveName())
                    .replace("{message}", message));

            Title title = Title.title(
                    Component.text(diskround.getConf("minecraft.helpop.title")),
                    Component.text(diskround.getConf("minecraft.helpop.subtitle")
                            .replace("{staff}", member.getEffectiveName())
                            .replace("{platform}", "discord")),
                    Title.Times.times(Duration.ofSeconds(1),
                            Duration.ofSeconds(diskround.config.getInt("minecraft.helpop.titleDuration")), Duration.ofSeconds(1))
            );

            player.showTitle(title);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, (float) diskround.config.getDouble("minecraft.helpop.volume"), 1f);
        }
    }

}
