package org.loreware.forceAuthWare;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ForceAuthWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event){
        String playerIP = event.getPlayer().getAddress().getAddress().getHostAddress();
        String playerName = event.getPlayer().getName();
        reloadConfig();
        config = getConfig();

        for (String ip: config.getStringList("ips")){
            if (ip.equals(playerIP)){
                event.getPlayer().sendMessage("Ai ipu in config ba pula");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "authme forcelogin " + playerName);
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("forceauthme")) {
                if (player.isOp()) {
                    String newIp = player.getAddress().getAddress().getHostAddress();
                    List<String> ipList = getConfig().getStringList("ips");
                    if (!ipList.contains(newIp)) { // Avoid duplicates
                        ipList.add(newIp);
                        getConfig().set("ips", ipList); // Update the config
                        saveConfig(); // Save changes to file
                    }
                }
            }
        }
        return true;
    }
}
