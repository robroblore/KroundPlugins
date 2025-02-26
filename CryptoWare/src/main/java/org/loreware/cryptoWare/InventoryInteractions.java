package org.loreware.cryptoWare;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InventoryInteractions implements Listener {
    CryptoWare cryptoWare = CryptoWare.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (clickedItem == null) return;

        if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.trader.title")))){
            String UIpath = "UI.trader.";
            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;
            event.setCancelled(true);

            if(checkItem(clickedItem, UIpath+"myServersItem")){
                cryptoWare.GUIs.openMyServersGUI(player);
            }

            else if(checkItem(clickedItem, UIpath+"shopItem")){
                cryptoWare.GUIs.openShopGUI(player);
            }

            else if(checkItem(clickedItem, UIpath+"marketItem")){
                cryptoWare.GUIs.openMarketGUI(player);
            }
        }

        else if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.market.title")))){
            String UIpath = "UI.market.";
            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;
            event.setCancelled(true);

            if(clickedItem.getType() == Material.ARROW){
                cryptoWare.GUIs.openTraderGUI(player);
            }

        }

        else if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.myServers.title")))){
            String UIpath = "UI.myServers.";
            event.setCancelled(true);

            if(clickedItem.getType() == Material.ARROW){
                cryptoWare.GUIs.openTraderGUI(player);
            }
        }

        else if(event.getView().title().equals(Component.text(cryptoWare.getConf("UI.shop.title")))){
            String UIpath = "UI.shop.";
            event.setCancelled(true);

            if(clickedItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(cryptoWare, "pointer"))){
                String pointer = clickedItem.getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey(cryptoWare, "pointer"), PersistentDataType.STRING);

                if(pointer.startsWith("server")){
                    ItemStack item = cryptoWare.createServerItem(pointer);
                    cryptoWare.buyItemShop(player, item);
                } else if(pointer.startsWith("upgrades")){
                    ItemStack item = cryptoWare.createUpgradeItem(pointer);
                    cryptoWare.buyItemShop(player, item);
                }
            }

            if(clickedItem.getType() == Material.ARROW){
                cryptoWare.GUIs.openTraderGUI(player);
            }
        }

        else return;

        if(clickedItem.getType() == Material.BARRIER){
            player.closeInventory();
        }
    }

    public boolean checkItem(ItemStack item, String configItem){
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        return cryptoWare.getStringFromTextComponent(item.getItemMeta().displayName())
                .equals(cryptoWare.getConf(configItem+".display_name"));
    }

}
