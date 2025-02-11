package org.loreware.bankWare;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BankWare extends JavaPlugin implements Listener, CommandExecutor {

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


    public boolean deleteBanker(String name) {
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getName().equalsIgnoreCase(name) && npc.data().get("isBanker") != null) {
                npc.destroy();
                return true;
            }
        }
        return false;
    }

    public NPC createBanker(Location location, String npcName, String skinName) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        // Create an NPC (fake player)
        NPC npc = registry.createNPC(EntityType.PLAYER, npcName);

        npc.data().setPersistent("isBanker",true);

        // Spawn the NPC at a location
        npc.spawn(location);

        // Set the skin
        npc.getOrAddTrait(SkinTrait.class).setSkinName(skinName);

        saveConfig();

        return npc;
    }

    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();

        if (npc.data().get("isBanker") == null) {
            return;
        }

        Player player = event.getClicker();

        player.sendMessage(getConf("messages.prefix") + getConf("messages.bankerInteract")
                .replace("{name}", npc.getName()));
        openBankerGUI(player);
    }

    public void openBankerGUI(Player player) {
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(getConf("UI.bankerTitle")));

        // Create items with new Component-based display names
        ItemStack depositItem = new ItemStack(Material.CHEST);
        ItemMeta meta = depositItem.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text(getConf("UI.depositItem")));
            depositItem.setItemMeta(meta);
        }

        ItemStack withdrawItem = new ItemStack(Material.DROPPER);
        ItemMeta meta2 = withdrawItem.getItemMeta();
        if (meta2 != null) {
            meta2.itemName(Component.text(getConf("UI.withdrawItem")));
            withdrawItem.setItemMeta(meta2);
        }

        ItemStack infoItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta3 = infoItem.getItemMeta();
        if (meta3 != null) {
            meta3.itemName(Component.text(getConf("UI.infoItem")));
            List<Component> lore = new ArrayList<>();
            for(String line: getConfList("UI.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(1000))
                        .replace("{interest}", String.valueOf(0.5))
                        .replace("{interestAmount}", String.valueOf(0.05 * 1000))
                        .replace("{nextInterestDate}", "la pulivara")));
            }
            meta3.lore(lore);
            infoItem.setItemMeta(meta3);
        }

        // Fill the inventory with black stained glass panes
        fillInventoryWithGlass(inv);

        // Place items in GUI
        inv.setItem(11, depositItem);
        inv.setItem(13, infoItem);
        inv.setItem(15, withdrawItem);

        // Open the GUI for the player
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        // Check if the clicked inventory is your banker inventory
        if (event.getView().title().equals(Component.text(getConf("UI.bankerTitle")))) {
            event.setCancelled(true);  // Prevent taking items or moving items within the inventory
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("bankware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();

                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("createbanker")) {
                if (args.length == 2) {
                    createBanker(player.getLocation(), args[0], args[1]);
                    player.sendMessage(getConf("messages.prefix") + "§2Banker created.");
                } else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /createbanker <name> <skin>");
                }
            }

            else if (cmd.getName().equalsIgnoreCase("deletebanker")) {
                if (args.length == 1) {
                    if(deleteBanker(args[0])) player.sendMessage(getConf("messages.prefix") + "§2Banker deleted.");
                    else player.sendMessage(getConf("messages.prefix") + "§4Banker not found.");
                } else {
                    player.sendMessage(getConf("messages.prefix") + "§4Usage: /deletebanker <name>");
                }
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
            for(NPC npc: CitizensAPI.getNPCRegistry()){
                if(npc.data().get("isBanker") != null) list.add(npc.getName());
            }

            return list;
        }
        return null;
    }



    // ----------------- UTILS -----------------

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
