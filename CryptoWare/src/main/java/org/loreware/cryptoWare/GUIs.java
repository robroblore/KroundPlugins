package org.loreware.cryptoWare;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIs {

    CryptoWare cryptoWare = CryptoWare.getInstance();

    public void openTraderGUI(Player player) {
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 36, Component.text(cryptoWare.getConf("UI.trader.title")));

        // Create items with new Component-based display names
        ItemStack myServersItem = cryptoWare.getHead("serverTier2");
        ItemMeta meta = myServersItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.trader.myServersItem")).decoration(TextDecoration.ITALIC, false));
            myServersItem.setItemMeta(meta);
        }

        ItemStack marketItem = cryptoWare.getHead("market");
        ItemMeta meta2 = marketItem.getItemMeta();
        if (meta2 != null) {
            meta2.displayName(Component.text(cryptoWare.getConf("UI.trader.marketItem")).decoration(TextDecoration.ITALIC, false));
            marketItem.setItemMeta(meta2);
        }

        // Fill the inventory with black stained glass panes
        fillInventoryWithGlass(inv);

        // Place items in GUI
        inv.setItem(11, myServersItem);
        inv.setItem(13, getInfoItem(player));
        inv.setItem(15, marketItem);

        inv.setItem(31, getQuitItem());
//        inv.setItem(30, getBackItem());

        // Open the GUI for the player
        player.openInventory(inv);
    }

    public void openMarketGUI(Player player){
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(cryptoWare.getConf("UI.market.title")));

        ItemStack serverRowItem = cryptoWare.getHead("serverRow");
        ItemMeta meta = serverRowItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.market.serverRowItem")).decoration(TextDecoration.ITALIC, false));
            serverRowItem.setItemMeta(meta);
        }


        fillInventoryWithGlass(inv);

        inv.setItem(11, serverRowItem);
        inv.setItem(13, getServerItem("1"));
        inv.setItem(14, getServerItem("2"));
        inv.setItem(15, getServerItem("3"));

        inv.setItem(49, getQuitItem());
        inv.setItem(48, getBackItem());

        player.openInventory(inv);
    }

    public void openMyServersGUI(Player player){
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 36, Component.text(cryptoWare.getConf("UI.myServers.title")));



        inv.setItem(31, getQuitItem());
        inv.setItem(30, getBackItem());

        player.openInventory(inv);
    }

    public ItemStack getServerItem(String serverTier){
        ItemStack serverItem = cryptoWare.getHead("serverTier" + serverTier);
        ItemMeta meta = serverItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.market.serverTier" + serverTier + "Item")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();

            double price = cryptoWare.config.getDouble("servers.tier" + serverTier + ".price");
            double durability = cryptoWare.config.getDouble("servers.tier" + serverTier + ".durability");
            double production = cryptoWare.config.getDouble("servers.tier" + serverTier + ".production");

            for(String line: cryptoWare.getConfList("UI.market.serverLore")){
                lore.add(Component.text(line
                        .replace("{price}", String.valueOf(price))
                        .replace("{durability}", String.valueOf(durability))
                        .replace("{production}", String.valueOf(production))));
            }

            meta.lore(lore);
            serverItem.setItemMeta(meta);
        }
        return serverItem;
    }

    public ItemStack getInfoItem(Player player){
        ItemStack infoItem = cryptoWare.getHead("infoItem");
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.trader.infoItem")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();

            for(String line: cryptoWare.getConfList("UI.trader.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(0))));
            }
            meta.lore(lore);
            infoItem.setItemMeta(meta);
        }

        return infoItem;
    }

    public ItemStack getQuitItem(){
        ItemStack quitItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = quitItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.quitItem")).decoration(TextDecoration.ITALIC, false));
            quitItem.setItemMeta(meta);
        }
        return quitItem;
    }

    public ItemStack getBackItem(){
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta meta = backItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cryptoWare.getConf("UI.backItem")).decoration(TextDecoration.ITALIC, false));
            backItem.setItemMeta(meta);
        }
        return backItem;
    }

    // Method to fill inventory with black stained glass panes
    private void fillInventoryWithGlass(Inventory inventory) {
        // Create a black stained glass pane ItemStack
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

        // Optionally, you can set a custom name or other properties for the glass panes
        ItemMeta meta = grayGlass.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text("")); // Optional: Set a display name (you can leave it empty)
            grayGlass.setItemMeta(meta);
        }

        // Fill the entire inventory with black stained glass panes
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, grayGlass); // Set the item at the current slot
        }
    }

}
