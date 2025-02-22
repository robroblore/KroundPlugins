package org.loreware.cryptoWare;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryInteractions implements Listener {
    CryptoWare cryptoWare = CryptoWare.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.trader.title")))){
            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;
            event.setCancelled(true);

            if(checkItem(clickedItem, "UI.trader.myServersItem")){
                cryptoWare.GUIs.openMyServersGUI(player);
            } else if(checkItem(clickedItem, "UI.trader.marketItem")){
                cryptoWare.GUIs.openMarketGUI(player);
            }
        }

        else if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.market.title")))){
            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;
            event.setCancelled(true);

            if(clickedItem.getType() == Material.ARROW){
                cryptoWare.GUIs.openTraderGUI(player);
            }

        }

        else if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.myServers.title")))){
            event.setCancelled(true);

            if(clickedItem.getType() == Material.ARROW){
                cryptoWare.GUIs.openTraderGUI(player);
            }
        }

        else return;

        if(clickedItem.getType() == Material.BARRIER){
            player.closeInventory();
        }
    }

    public boolean checkItem(ItemStack item, String confId){
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                .equals(cryptoWare.getConf(confId));
    }

}
