package org.loreware.sug_i;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;


public final class Sug_i extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        System.out.println("Sug(i) plugin enabled");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");
        getComponentLogger().error("CE MI O MAI SUG DENISE");

        getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void onAsyncPlayerChatEvent(AsyncChatEvent event) {
        Component message = event.message().replaceText(TextReplacementConfig.builder()
                .match(Pattern.compile("[5s]ug[iy1YI!]*", Pattern.CASE_INSENSITIVE))
                .replacement((matchResult, builder) -> {
                    String matchedText = matchResult.group(); // Get the full match (including unwanted characters)
                    String cleanedText = matchedText.replaceAll("[iy1IY!]+$", ""); // Remove all trailing 'i', 'y', or '1'
                    return Component.text(cleanedText);
                }).build());
        event.message(message);
    }
}
