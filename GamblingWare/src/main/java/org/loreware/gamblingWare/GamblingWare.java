package org.loreware.gamblingWare;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GamblingWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void OnPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if(block == null) return;
        String location = block.getLocation().toString();

        List<String> locations = config.getStringList("slotMachines");
        if(locations.contains(location)) {
            getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "dm open pacanele " + player.getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)  {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("gamblingware")) {
                if(args.length == 1) {
                    if(args[0].equalsIgnoreCase("createslot")) {
                        player.sendMessage("Slot machine created successfully at the block you are looking at!");

                        // Get the block the player is looking at
                        // Create a slot machine at that block
                        Block block = player.getTargetBlock(null, 5);

                        List<String> locations = config.getStringList("slotMachines");
                        locations.add(block.getLocation().toString());

                        config.set("slotMachines", locations);
                        saveConfig();
                    }
                }
            }

//            else if(cmd.getName().equalsIgnoreCase("gamblingwarepublic")) {
//                if(args[0].equals("ashdkljfgkhjasgfdhjkasdghkjsadgajdwajyktsdkjyfajsdfgnsafdnjbvazfdjchstgfchjsgfjsdfgjhagf")){
//
//                }
//            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bankware")) {
            List<String> list = new ArrayList<>();

            if(args.length == 1) {
                if(sender.hasPermission("gamblingware.canCreateSlots")) list.add("createslot");
            }
            return list;
        }
    return null;
    }
}
