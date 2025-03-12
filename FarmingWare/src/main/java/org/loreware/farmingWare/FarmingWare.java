package org.loreware.farmingWare;

import net.advancedplugins.ae.api.AEAPI;
import net.advancedplugins.ae.impl.effects.api.EffectsActivateEvent;
import net.advancedplugins.ae.impl.effects.effects.abilities.AdvancedAbility;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;



import java.util.ArrayList;
import java.util.List;

public final class FarmingWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    @Override
    public void onEnable() {
        System.out.println("BankWare plugin enabled");

        saveResource("config.yml", /* replace */ true);
        config = getConfig();

//        saveDefaultConfig();
//        config = getConfig();
//        config.options().copyDefaults(true);
//        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEnchantActivate(EffectsActivateEvent event) {
        if (event.getEffect().getNameNoLevel().equalsIgnoreCase("Replanter")) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AEAPI.hasCustomEnchant("Replanter", item)){
            return;
        }

        if (block.getType() == Material.SUGAR_CANE || block.getType() == Material.CACTUS || block.getType() == Material.BAMBOO) {
            return; // Allow breaking these crops
        }

        // Check if the block is a crop (wheat, carrots, potatoes, beetroot)
        if (block.getBlockData() instanceof Ageable ageable) {
            // Check if the crop is NOT fully grown
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(0);
                block.setBlockData(ageable);
                event.setCancelled(true);  // Prevent breaking
            } else {
                // Determine the crop type for replanting
                Material cropType = switch (block.getType()) {
                    default -> Material.WHEAT; // Default to wheat if unknown
                    case CARROTS -> Material.CARROTS;
                    case POTATOES -> Material.POTATOES;
                    case BEETROOTS -> Material.BEETROOTS;
                    case NETHER_WART -> Material.NETHER_WART;
                };

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    // Set the block back to a freshly planted crop
                    block.setType(cropType);
                    Ageable newCrop = (Ageable) block.getBlockData();
                    newCrop.setAge(0); // Set to newly planted
                    block.setBlockData(newCrop);
                }, 2L);
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("farmingware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();

                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("farmingware")) {
            if (sender instanceof Player) {
                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }
        return null;
    }



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

    // ----------------- UTILS -----------------


    @Override
    public void onDisable() {
        System.out.println("BankWare plugin disabled");
    }
}
