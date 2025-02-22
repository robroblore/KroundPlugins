package org.loreware.cryptoWare;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;
import org.loreware.bankWare.BankWare;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.net.URI;

public final class CryptoWare extends JavaPlugin implements Listener, CommandExecutor {

    private static CryptoWare instance;

    FileConfiguration config;
    FileConfiguration accounts;

    private static HeadDatabaseAPI headDatabaseAPI;

    BankWare bankWare;
    GUIs GUIs;

    @Override
    public void onEnable() {
        instance = this;
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
        getServer().getPluginManager().registerEvents(new InventoryInteractions(), this);

        GUIs = new GUIs();

        Bukkit.getScheduler().runTaskLater(this, this::scheduleNextDailyEvent, 20L);
    }

    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();

        if (npc.data().get("isTrader") == null) {
            return;
        }

        Player player = event.getClicker();

        player.sendMessage(getConf("messages.prefix") + getConf("messages.traderInteract")
                .replace("{name}", npc.getName()));
        GUIs.openTraderGUI(player);
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
            // TODO: ask denis time set?
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

    public ItemStack getHead(String confHead) {
        if(config.getBoolean("heads." + confHead + ".useBase64") || headDatabaseAPI == null){
            return getCustomHead(getConf("heads." + confHead + ".id"));
        }
        else {
            return getHeadDB(config.getString("heads." + confHead + ".id"));
        }
    }

    public static ItemStack getHeadDB(String id) {
        if (headDatabaseAPI == null) {
            Bukkit.getLogger().severe("HeadDatabaseAPI is not initialized!");
            return null;
        }
        return headDatabaseAPI.getItemHead(id);
    }

    public static ItemStack getCustomHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String decoded = new String(Base64.getDecoder().decode(base64));

        if (meta != null) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            try {
                textures.setSkin(URI.create(decoded.substring("{\"textures\":{\"SKIN\":{\"url\":\"".length(), decoded.length() - "\"}}}".length())).toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        }

        return head;
    }

    // ----------------- UTILS -----------------

    public static CryptoWare getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CryptoWare instance is not initialized yet!");
        }
        return instance;
    }

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        headDatabaseAPI = new HeadDatabaseAPI();
    }

    @Override
    public void onDisable() {
        System.out.println("cryptoware plugin disabled");
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
                GUIs.openTraderGUI(player);
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
}
