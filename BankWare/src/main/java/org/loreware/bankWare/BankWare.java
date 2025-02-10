package org.loreware.bankWare;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BankWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;
    List<NPC> spawnedNPCs = new ArrayList<>();


    @Override
    public void onEnable() {
        System.out.println("BankWare plugin enabled");

//        saveResource("config.yml", /* replace */ true);
//        config = getConfig();

        saveDefaultConfig();
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.data().get("destroy")) {
                npc.destroy(); // Destroy only non-persistent NPCs
            }
        }

        loadPOIs();

        getServer().getPluginManager().registerEvents(this, this);
    }

    public void loadPOIs() {
        for (NPC npc: spawnedNPCs) {
            npc.destroy();
        }

        if(config.getConfigurationSection("locations.bankers") != null){
            for (String banker : config.getConfigurationSection("locations.bankers").getKeys(false)) {
                Location loc = loadLocation("bankers", banker);
                if (loc == null) continue;

                createNPCWithSkin(loc, banker, getConf("locations.bankers." + banker + ".skin"));
            }
        }


        if(config.getConfigurationSection("locations.vaults") != null) {
            for (String vault : config.getConfigurationSection("locations.vaults").getKeys(false)) {
                Location loc = loadLocation("vaults", vault);
                if (loc == null) continue;
            }
        }
    }

    public boolean deleteNPC(String name) {
        if (config.contains("locations.bankers." + name)) {
            config.set("locations.bankers." + name, null);
            saveConfig();
            loadPOIs();

            return true;
        }

        return false;
    }

    public NPC createNPCWithSkin(Location location, String npcName, String skinName) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        // Create an NPC (fake player)
        NPC npc = registry.createNPC(EntityType.PLAYER, npcName);

        npc.data().setPersistent("destroy", true);


        // Spawn the NPC at a location
        npc.spawn(location);

        // Set the skin
        npc.getOrAddTrait(SkinTrait.class).setSkinName(skinName);

        spawnedNPCs.add(npc);
        saveLocation("bankers", npcName, location);
        config.set("locations.bankers." + npcName + ".skin", skinName);
        saveConfig();

        return npc;
    }

    public void saveLocation(String poi, String name, Location loc) {
        config.set("locations." + poi + "." + name + ".world", loc.getWorld().getName());
        config.set("locations." + poi + "." + name + ".x", loc.getX());
        config.set("locations." + poi + "." + name + ".y", loc.getY());
        config.set("locations." + poi + "." + name + ".z", loc.getZ());
        config.set("locations." + poi + "." + name + ".yaw", loc.getYaw());
        config.set("locations." + poi + "." + name + ".pitch", loc.getPitch());
        saveConfig();
    }

    public Location loadLocation(String poi, String name) {
        if (!config.contains("locations." + poi + "." + name)) return null;

        World world = getServer().getWorld(config.getString("locations." + poi + "." + name + ".world"));
        double x = config.getDouble("locations." + poi + "." + name + ".x");
        double y = config.getDouble("locations." + poi + "." + name + ".y");
        double z = config.getDouble("locations." + poi + "." + name + ".z");
        float yaw = (float) config.getDouble("locations." + poi + "." + name + ".yaw");
        float pitch = (float) config.getDouble("locations." + poi + "." + name + ".pitch");

        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("bankware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();
                    loadPOIs();

                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("createbanker")) {
                if (args.length == 2) {
                    createNPCWithSkin(player.getLocation(), args[0], args[1]);
                    player.sendMessage(getConf("messages.prefix") + "§2Banker created.");
                } else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /createbanker <name> <skin>");
                }
            }

            else if (cmd.getName().equalsIgnoreCase("deletebanker")) {
                if (args.length == 1) {
                    if(deleteNPC(args[0])) player.sendMessage(getConf("messages.prefix") + "§2Banker deleted.");
                    else player.sendMessage(getConf("messages.prefix") + "§4Banker not found.");
                } else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /deletebanker <name>");
                }
            }

            else if(cmd.getName().equalsIgnoreCase("crash")){
                debug(args[33]);
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bankware")) {
            if (sender instanceof Player) {
                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }

        else if (cmd.getName().equalsIgnoreCase("createbanker")) {
            return Collections.emptyList();
        }

        else if (cmd.getName().equalsIgnoreCase("deletebanker")) {
            List<String> list = new ArrayList<>();
            for(NPC npc : spawnedNPCs) {
                list.add(npc.getName());
            }

            return list;
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
        for(NPC npc: spawnedNPCs){
            npc.destroy();
        }
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.data().get("destroy")) {
                npc.destroy(); // Destroy only non-persistent NPCs
            }
        }
    }
}
