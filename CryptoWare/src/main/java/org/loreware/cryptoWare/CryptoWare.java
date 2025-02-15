package org.loreware.cryptoWare;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.loreware.bankWare.BankWare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CryptoWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;
    FileConfiguration accounts;

    BankWare bankWare;

    @Override
    public void onEnable() {
        System.out.println("CryptoWare plugin enabled");
        saveResource("config.yml", /* replace */ true);
        config = getConfig();
//        saveDefaultConfig();
//        getConfig().options().copyDefaults(true);
//        saveConfig();
//        config = getConfig();

        accounts = getAccountsConfig();

        bankWare = (BankWare) Bukkit.getPluginManager().getPlugin("BankWare");

        getServer().getPluginManager().registerEvents(this, this);

        scheduleNextDailyEvent();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("cryptoware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();

                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("trader")){
//                openTraderGUI(player);
            }

            else if(cmd.getName().equalsIgnoreCase("createtrader")) {
                if (args.length == 2) {
                    createTrader(player.getLocation(), args[0], args[1]);
                    player.sendMessage(getConf("messages.prefix") + "§2Trader created.");
                }
                else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /createtrader <name> <skin>");
                }
            }

            else if (cmd.getName().equalsIgnoreCase("deletetrader")) {
                if (args.length == 1) {
                    if(deleteTrader(args[0])) player.sendMessage(getConf("messages.prefix") + "§2Trader deleted.");
                    else player.sendMessage(getConf("messages.prefix") + "§4Trader not found.");
                } else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /deletetrader <name>");
                }
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cryptoware")) {
            if (sender instanceof Player) {
                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }

        else if (cmd.getName().equalsIgnoreCase("createtrader")) {
            return Collections.emptyList();
        }

        else if (cmd.getName().equalsIgnoreCase("deletetrader")) {
            List<String> list = new ArrayList<>();
            for(NPC npc: CitizensAPI.getNPCRegistry()){
                if(npc.data().get("isTrader") != null) list.add(npc.getName());
            }

            return list;
        }
        return null;
    }


    public boolean deleteTrader(String name) {
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getName().equalsIgnoreCase(name) && npc.data().get("isTrader") != null) {
                npc.destroy();
                return true;
            }
        }
        return false;
    }

    public NPC createTrader(Location location, String npcName, String skinName) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        // Create an NPC (fake player)
        NPC npc = registry.createNPC(EntityType.PLAYER, npcName);

        npc.data().setPersistent("isTrader",true);

        // Spawn the NPC at a location
        npc.spawn(location);

        // Set the skin
        npc.getOrAddTrait(SkinTrait.class).setSkinName(skinName);

        saveConfig();

        return npc;
    }


    // ----------------- UTILS -----------------

    private void scheduleNextDailyEvent() {
        World world = getServer().getWorld(getConf("trader.worldForDayCycle"));
        long currentTime = world.getTime();
        long ticksUntilNextDay = 24000 - currentTime;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Do something
            scheduleNextDailyEvent(); // Reschedule for the next day
        }, ticksUntilNextDay);
    }

    public YamlConfiguration getAccountsConfig(){
        File accountsFile = new File(getDataFolder(), "accounts.yml");
        if (!accountsFile.exists()) {
            saveResource("accounts.yml", false);
        }

        return YamlConfiguration.loadConfiguration(accountsFile);
    }

    public void saveAccountsConfig(){
        File accountsFile = new File(getDataFolder(), "accounts.yml");
        try {
            accounts.save(accountsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to fill inventory with black stained glass panes
    private void fillInventoryWithGlass(Inventory inventory) {
        // Create a black stained glass pane ItemStack
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1); // 1 pane at a time

        // Optionally, you can set a custom name or other properties for the glass panes
        ItemMeta meta = blackGlass.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text("")); // Optional: Set a display name (you can leave it empty)
            blackGlass.setItemMeta(meta);
        }

        // Fill the entire inventory with black stained glass panes
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, blackGlass); // Set the item at the current slot
        }
    }


    public double round(double amount){
        return Math.round(amount*100.0)/100.0;
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

    // ----------------- UTILS -----------------


    @Override
    public void onDisable() {
        System.out.println("cryptoware plugin disabled");
    }
}
