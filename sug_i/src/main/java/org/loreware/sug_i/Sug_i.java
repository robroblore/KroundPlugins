package org.loreware.sug_i;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
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
                .match(Pattern.compile("sugi|sug1|5ug1|5ugi", Pattern.CASE_INSENSITIVE))
                .replacement("sug") // Maintain consistent case in replacement
                .build());
        event.message(message);
    }
}
