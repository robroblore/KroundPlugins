package org.loreware.cryptoWare;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;

public class GUIs {

    CryptoWare cryptoWare = CryptoWare.getInstance();

    public void openTraderGUI(Player player) {
        String UIpath = "UI.trader.";
        // Use the new method to create the inventory with a title as Component
        Inventory inv = cryptoWare.getInventoryFromConfig(UIpath, 36);

        ItemStack myServersItem = cryptoWare.getItemStackFromConfig(UIpath+"myServersItem", null);
        ItemStack shopItem = cryptoWare.getItemStackFromConfig(UIpath+"shopItem", null);
        ItemStack marketItem = cryptoWare.getItemStackFromConfig(UIpath+"marketItem", null);

        Map<String, String> infoPlaceholders = new java.util.HashMap<>(Map.of());
        infoPlaceholders.put("{player}", player.getName());
        infoPlaceholders.put("{balance}", "69");
        ItemStack infoItem = cryptoWare.getItemStackFromConfig(UIpath+"infoItem", infoPlaceholders);

        fillInventory(inv, UIpath);

        // Place items in GUI
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"myServersItem", 11), myServersItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"shopItem", 13), shopItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"marketItem", 15), marketItem);

        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"infoItem", 35), infoItem);

        ItemStack quitItem = cryptoWare.getItemStackFromConfig(UIpath+"quitItem", null);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"quitItem", 31), quitItem);

        // Open the GUI for the player
        player.openInventory(inv);
    }

    public void openShopGUI(Player player){
        String UIpath = "UI.shop.";
        Inventory inv = cryptoWare.getInventoryFromConfig(UIpath, 54);
        fillInventory(inv, UIpath);

        ItemStack serverRowItem = cryptoWare.getItemStackFromConfig(UIpath+"serverRowItem", null);

        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"serverRowItem", 11), serverRowItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"serverTier1Item", 13), getServerItem("1"));
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"serverTier2Item", 14), getServerItem("2"));
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"serverTier3Item", 15), getServerItem("3"));

        ItemStack upgradesRowItem = cryptoWare.getItemStackFromConfig(UIpath+"upgradesRowItem", null);

        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"upgradesRowItem", 29), upgradesRowItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"upgradeExtinguisherItem", 31), getUpgradeItem("Extinguisher"));
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"upgradeOverclockItem", 32), getUpgradeItem("Overclock"));
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"upgradeTestItem", 33), getUpgradeItem("Test"));

        Map<String, String> infoPlaceholders = new java.util.HashMap<>(Map.of());
        infoPlaceholders.put("{player}", player.getName());
        infoPlaceholders.put("{balance}", "69");
        ItemStack infoItem = cryptoWare.getItemStackFromConfig(UIpath+"infoItem", infoPlaceholders);

        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"infoItem", 53), infoItem);

        ItemStack quitItem = cryptoWare.getItemStackFromConfig(UIpath+"quitItem", null);
        ItemStack backItem = cryptoWare.getItemStackFromConfig(UIpath+"backItem", null);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"quitItem", 49), quitItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"backItem", 48), backItem);

        player.openInventory(inv);
    }

    public void openMarketGUI(Player player){
        String UIpath = "UI.market.";
        // Use the new method to create the inventory with a title as Component
        Inventory inv = cryptoWare.getInventoryFromConfig(UIpath, 54);

        fillInventory(inv, UIpath);

        ItemStack quitItem = cryptoWare.getItemStackFromConfig(UIpath+"quitItem", null);
        ItemStack backItem = cryptoWare.getItemStackFromConfig(UIpath+"backItem", null);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"quitItem", 49), quitItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"backItem", 48), backItem);

        player.openInventory(inv);
    }

    public void openMyServersGUI(Player player){
        // Use the new method to create the inventory with a title as Component
        String UIpath = "UI.myServers.";
        Inventory inv = cryptoWare.getInventoryFromConfig(UIpath, 45);
        fillInventory(inv, UIpath);

        ItemStack emptyUpgradeSlotItem = cryptoWare.getItemStackFromConfig(UIpath+"emptyUpgradeSlotItem", null);
        ItemStack emptyServerSlotItem = cryptoWare.getItemStackFromConfig(UIpath+"emptyServerSlotItem", null);
        ItemStack emptyGlobalUpgradeSlotItem = cryptoWare.getItemStackFromConfig(UIpath+"emptyGlobalUpgradeSlotItem", null);

        for(int slot: cryptoWare.config.getIntegerList(UIpath+"upgradeSlots")){
            inv.setItem(slot, emptyUpgradeSlotItem);
        }

        for(int slot: cryptoWare.config.getIntegerList(UIpath+"serverSlots")){
            inv.setItem(slot, emptyServerSlotItem);
        }

        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"emptyGlobalUpgradeSlotItem", 41), emptyGlobalUpgradeSlotItem);

        ItemStack quitItem = cryptoWare.getItemStackFromConfig(UIpath+"quitItem", null);
        ItemStack backItem = cryptoWare.getItemStackFromConfig(UIpath+"backItem", null);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"quitItem", 40), quitItem);
        inv.setItem(cryptoWare.getItemSlotFromConfig(UIpath+"backItem", 39), backItem);

        player.openInventory(inv);
    }

    public ItemStack getUpgradeItem(String upgradeName){
        String path = "default_items.upgrades." + upgradeName;
        double price = cryptoWare.config.getDouble(path + ".price");
        int durability = cryptoWare.config.getInt(path + ".durability");
        Map<String, String> upgradePlaceholders = new java.util.HashMap<>(Map.of());
        upgradePlaceholders.put("{price}", String.valueOf(price));
        upgradePlaceholders.put("{durability}", String.valueOf(durability));

        ItemStack item = cryptoWare.getItemStackFromConfig("UI.shop.upgrade"+upgradeName+"Item", upgradePlaceholders);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(cryptoWare, "pointer"),
                org.bukkit.persistence.PersistentDataType.STRING, "upgrades."+upgradeName);
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack getServerItem(String serverTier){
        String path = "default_items.servers.tier" + serverTier;
        double price = cryptoWare.config.getDouble(path + ".price");
        int durability = cryptoWare.config.getInt(path + ".durability");
        double production = cryptoWare.config.getDouble(path + ".production");
        Map<String, String> serverPlaceholders = new java.util.HashMap<>(Map.of());
        serverPlaceholders.put("{price}", String.valueOf(price));
        serverPlaceholders.put("{durability}", String.valueOf(durability));
        serverPlaceholders.put("{production}", String.valueOf(production));

        ItemStack item = cryptoWare.getItemStackFromConfig("UI.shop.serverTier"+serverTier+"Item", serverPlaceholders);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(cryptoWare, "pointer"),
                org.bukkit.persistence.PersistentDataType.STRING, "servers.tier"+serverTier);
        item.setItemMeta(meta);

        return item;
    }

    // Method to fill inventory with black stained glass panes
    private void fillInventory(Inventory inventory, String path) {
        // Create a black stained glass pane ItemStack
        Material mat = Material.getMaterial(cryptoWare.getConf(path + "fillerMaterial"));
        ItemStack grayGlass = new ItemStack(mat, 1);

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
