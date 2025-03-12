package org.loreware.cryptoWare;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;
import org.loreware.bankWare.BankWare;
import javax.annotation.Nullable;
import javax.naming.Name;
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
    }

    public boolean buyItemShop(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            double price = 0;
            if(!item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "path")
                    , PersistentDataType.STRING)) return false;

            price = config.getDouble(item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(this, "path"), PersistentDataType.STRING) + ".price");

            if(!bankWare.payFromBank(player, price)){
                player.sendMessage(getConf("messages.prefix") + getConf("messages.itemTransactionNotEnoughMoney")
                        .replace("{price}", String.valueOf(price)));
                return false;
            }

            player.getInventory().addItem(item);
            player.sendMessage(getConf("messages.prefix") + getConf("messages.itemTransactionSuccessful")
                    .replace("{price}", String.valueOf(price))
                    .replace("{item}", getStringFromTextComponent(item.getItemMeta().displayName())));
            return true;
        } else {
            player.sendMessage(getConf("messages.prefix") + getConf("messages.itemTransactionInventoryFull"));
            return false;
        }
    }

    public ItemStack createUpgradeItem(String path){
        int durability = config.getInt(path + ".durability");
        Map<String, String> serverPlaceholders = new java.util.HashMap<>(Map.of());
        serverPlaceholders.put("{durability}", String.valueOf(durability));
        serverPlaceholders.put("{max_durability}", String.valueOf(durability));

        ItemStack item = getItemStackFromConfig(path, serverPlaceholders);
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer data = meta.getPersistentDataContainer();

        data.set(new NamespacedKey(this, "path"), PersistentDataType.STRING, path);
        data.set(new NamespacedKey(this, "durability"), PersistentDataType.INTEGER, durability);

        data.set(new NamespacedKey(this, "unique_id"), PersistentDataType.STRING, UUID.randomUUID().toString());

        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createServerItem(String path){
        int durability = config.getInt(path + ".durability");
        double production = config.getDouble(path + ".production");
        Map<String, String> serverPlaceholders = new java.util.HashMap<>(Map.of());
        serverPlaceholders.put("{durability}", String.valueOf(durability));
        serverPlaceholders.put("{max_durability}", String.valueOf(durability));
        serverPlaceholders.put("{production}", String.valueOf(production));

        ItemStack item = getItemStackFromConfig(path, serverPlaceholders);
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer data = meta.getPersistentDataContainer();

        data.set(new NamespacedKey(this, "path"), PersistentDataType.STRING, path);
        data.set(new NamespacedKey(this, "durability"), PersistentDataType.INTEGER, durability);

        data.set(new NamespacedKey(this, "unique_id"), PersistentDataType.STRING, UUID.randomUUID().toString());

        item.setItemMeta(meta);

        return item;
    }

    public void deleteItemFromAccount(Player player, ItemStack item, int slot){
        accounts = getAccountsConfig();
        UUID uuid = player.getUniqueId();
        ConfigurationSection section = accounts.getConfigurationSection(uuid.toString());
        if(section == null) return;

        if(!item.hasItemMeta()) return;
        if(!item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "path"))) return;
        String path = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(this, "path"), PersistentDataType.STRING);
        String type = path.split("\\.")[1];

        if (type.equals("upgrades")){
            type += "." + path.split("\\.")[2];
        }

        debug("deleting item from account: " + type + "." + slot);
        section.set(type + "." + slot, null);
        saveAccountsConfig();
    }

    public void saveItemToAccount(Player player, ItemStack item, int slot){
        accounts = getAccountsConfig();
        UUID uuid = player.getUniqueId();
        if(!accounts.isConfigurationSection(uuid.toString())){
            accounts.createSection(uuid.toString());
            accounts.createSection(uuid + ".servers");
            accounts.createSection(uuid + ".upgrades");
            accounts.createSection(uuid + ".coins");
            accounts.set(uuid + ".coins.bitcoin", 0.0);
            accounts.set(uuid + ".coins.lorewarecoin", 0.0);
            saveAccountsConfig();
        }

        accounts.set(uuid + ".lastName" , player.getName());

        if(!item.hasItemMeta()) return ;

        if(!item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "path")
                , PersistentDataType.STRING)) return;

        String path = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(this, "path"), PersistentDataType.STRING);

        String type = path.split("\\.")[1];

        if (type.equals("upgrades")){
            type += "." + path.split("\\.")[2];
        }

        accounts.set(uuid + "." + type + "." + slot, item);

        saveAccountsConfig();
    }

    public void loadItemsFromAccounts(Player player, Inventory inv){
        accounts = getAccountsConfig();
        ConfigurationSection section = accounts.getConfigurationSection(player.getUniqueId().toString());
        if(section == null) return;

        ConfigurationSection servers = section.getConfigurationSection("servers");
        ConfigurationSection localUpgrades = section.getConfigurationSection("upgrades.local");
        ConfigurationSection globalUpgrades = section.getConfigurationSection("upgrades.global");

        if(servers != null){
            for(String key: servers.getKeys(false)){
                int slot = config.getIntegerList("UI.myServers." + "serverSlots").get(Integer.parseInt(key));
                ItemStack item = servers.getItemStack(key);
                inv.setItem(slot, item);
            }
        }

        if(localUpgrades != null){
            for(String key: localUpgrades.getKeys(false)){
                int slot = config.getIntegerList("UI.myServers." + "upgradeSlots").get(Integer.parseInt(key));
                ItemStack item = localUpgrades.getItemStack(key);
                inv.setItem(slot, item);
            }
        }

        if(globalUpgrades != null){
            for(String key: globalUpgrades.getKeys(false)){
                int slot = config.getIntegerList("UI.myServers." + "globalUpgradeSlots").get(Integer.parseInt(key));
                ItemStack item = globalUpgrades.getItemStack(key);
                inv.setItem(slot, item);
            }
        }
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

        return npc;
    }


    // ----------------- UTILS -----------------

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

    public String getStringFromTextComponent(Component component){
        return PlainTextComponentSerializer.plainText().serialize(component);
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

    public ItemStack getHead(String id, boolean useBase64) {
        if(useBase64 || headDatabaseAPI == null){
            return getCustomHead(id);
        }
        else {
            return getHeadDB(id);
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
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        }

        return head;
    }

    public Inventory getInventoryFromConfig(String path, int defSize){
        return Bukkit.createInventory(null, config.getInt(path+"size", defSize),
                Component.text(getConf(path+"title")));
    }

    public int getItemSlotFromConfig(String path, int def){
        return config.getInt(path + ".slot", def);
    }

    public ItemStack getItemStackFromConfig(String path, @Nullable Map<String, String> placeholders) {
        if (!config.contains(path + ".material")) return null;
        Material material = Material.getMaterial(config.getString(path + ".material", "BARRIER"));
        int amount = config.getInt(path + ".amount", 1);

        ItemStack item;

        if (material == Material.PLAYER_HEAD) {
            item = getHead(getConf(path + ".headID"), config.getBoolean(path + ".useBase64"));
        }
        else{
            item = new ItemStack(material, amount);
        }

        ItemMeta meta = item.getItemMeta();

        // Load display name
        if (config.contains(path + ".display_name")) {
            meta.displayName(Component.text(getConf(path + ".display_name")).decoration(TextDecoration.ITALIC, false));
        }

        // Load lore
        if (config.contains(path + ".lore")) {
            List<Component> lore = new ArrayList<>();

            for(String line: getConfList(path + ".lore")){
                if(placeholders != null){
                    for(Map.Entry<String, String> entry: placeholders.entrySet()){
                        line = line.replace(entry.getKey(), entry.getValue());
                    }
                }

                lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
        }

        item.setItemMeta(meta);
        return item;
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
