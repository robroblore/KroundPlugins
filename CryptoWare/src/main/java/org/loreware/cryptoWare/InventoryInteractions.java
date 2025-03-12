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

import java.util.List;

public class InventoryInteractions implements Listener {
    CryptoWare cryptoWare = CryptoWare.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        int clickedSlot = event.getSlot();

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
            if(event.getClickedInventory().getType() != InventoryType.PLAYER) event.setCancelled(true);

            List<Integer> serverSlots = cryptoWare.config.getIntegerList(UIpath+"serverSlots");
            List<Integer> upgradeSlots = cryptoWare.config.getIntegerList(UIpath+"upgradeSlots");
            List<Integer> globalUpgradeSlots = cryptoWare.config.getIntegerList(UIpath+"globalUpgradeSlots");

            if(event.getClickedInventory().getType() != InventoryType.PLAYER){
                if(serverSlots.contains(clickedSlot)){
                    if(clickedItem.getType() ==
                            Material.getMaterial(cryptoWare.getConf(UIpath+"emptyServerSlotItem.material"))){
                        if(!cursorItem.hasItemMeta()) return;
                        if(!cursorItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(cryptoWare, "path"))) return;
                        String path = cursorItem.getItemMeta().getPersistentDataContainer()
                                .get(new NamespacedKey(cryptoWare, "path"), PersistentDataType.STRING);
                        String type = path.split("\\.")[1];
                        if (!type.equals("servers")) return;

                        cryptoWare.saveItemToAccount(player, cursorItem, serverSlots.indexOf(clickedSlot));
                        inv.setItem(clickedSlot, cursorItem);
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    } else{
                        cryptoWare.deleteItemFromAccount(player, clickedItem, serverSlots.indexOf(clickedSlot));
                        cryptoWare.GUIs.openMyServersGUI(player);
                        player.setItemOnCursor(clickedItem);
                    }
                }


            else if(upgradeSlots.contains(clickedSlot)){
                if(clickedItem.getType() ==
                        Material.getMaterial(cryptoWare.getConf(UIpath+"emptyUpgradeSlotItem.material"))){
                    if(!cursorItem.hasItemMeta()) return;
                    if(!cursorItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(cryptoWare, "path"))) return;
                    String path = cursorItem.getItemMeta().getPersistentDataContainer()
                            .get(new NamespacedKey(cryptoWare, "path"), PersistentDataType.STRING);
                    String type = path.split("\\.")[1] + "." + path.split("\\.")[2];
                    if (!type.equals("upgrades.local")) return;

                    cryptoWare.saveItemToAccount(player, cursorItem, upgradeSlots.indexOf(clickedSlot));
                    inv.setItem(clickedSlot, cursorItem);
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                }else{
                    cryptoWare.deleteItemFromAccount(player, clickedItem, upgradeSlots.indexOf(clickedSlot));
                    cryptoWare.GUIs.openMyServersGUI(player);
                    player.setItemOnCursor(clickedItem);
                }
            }

            else if(globalUpgradeSlots.contains(clickedSlot)){
                if(clickedItem.getType() ==
                        Material.getMaterial(cryptoWare.getConf(UIpath+"emptyGlobalUpgradeSlotItem.material"))){
                    if(!cursorItem.hasItemMeta()) return;
                    if(!cursorItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(cryptoWare, "path"))) return;
                    String path = cursorItem.getItemMeta().getPersistentDataContainer()
                            .get(new NamespacedKey(cryptoWare, "path"), PersistentDataType.STRING);
                    String type = path.split("\\.")[1] + "." + path.split("\\.")[2];
                    if (!type.equals("upgrades.global")) return;

                    cryptoWare.saveItemToAccount(player, cursorItem, globalUpgradeSlots.indexOf(clickedSlot));
                    inv.setItem(clickedSlot, cursorItem);
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                } else{
                    cryptoWare.deleteItemFromAccount(player, clickedItem, globalUpgradeSlots.indexOf(clickedSlot));
                    cryptoWare.GUIs.openMyServersGUI(player);
                    player.setItemOnCursor(clickedItem);
                }
            }

            }

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

                ItemStack item = null;

                if(pointer.startsWith("default_items.servers")){
                   item  = cryptoWare.createServerItem(pointer);
                } else if(pointer.startsWith("default_items.upgrades")){
                    item = cryptoWare.createUpgradeItem(pointer);
                }
                cryptoWare.buyItemShop(player, item);

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
