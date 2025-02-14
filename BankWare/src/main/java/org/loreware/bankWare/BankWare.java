package org.loreware.bankWare;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import de.rapha149.signgui.exception.SignGUIVersionException;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BankWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;
    FileConfiguration accounts;
    private static Economy econ;

    @Override
    public void onEnable() {
        System.out.println("BankWare plugin enabled");

        saveResource("config.yml", /* replace */ true);
        config = getConfig();

//        saveDefaultConfig();
//        config = getConfig();
//        config.options().copyDefaults(true);
//        saveConfig();

        accounts = getAccountsConfig();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp.getProvider();

        getServer().getPluginManager().registerEvents(this, this);
    }

    public double getBankBalance(Player player) {
        accounts = getAccountsConfig();
        UUID uuid = player.getUniqueId();
        if(!accounts.isConfigurationSection(uuid.toString())){
            accounts.createSection(uuid.toString());
            accounts.set(uuid.toString() + ".balance", 0.0);
            saveAccountsConfig();
        }

        accounts.set(uuid.toString() + ".lastName" , player.getName());
        saveAccountsConfig();

        double amount = accounts.getDouble(uuid.toString() + ".balance");
        amount = Math.round(amount*100.0)/100.0; // Round to 2 digits after the .

        return amount;
    }

    public void depositToBank(Player player, double amount) {
        if(econ.getBalance(player) < amount){
            player.sendMessage(getConf("messages.prefix") + getConf("messages.depositFail"));
            return;
        }
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);
        econ.withdrawPlayer(player, amount);
        balance += amount;
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        player.sendMessage(getConf("messages.prefix") + getConf("messages.depositSuccess")
                .replace("{amount}", String.valueOf(amount))
                .replace("{balance}", String.valueOf(balance)));
    }

    public void withdrawFromBank(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);
        if(balance < amount){
            player.sendMessage(getConf("messages.prefix") + getConf("messages.withdrawFail"));
            return;
        }
        balance -= amount;
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        econ.depositPlayer(player, amount);
        player.sendMessage(getConf("messages.prefix") + getConf("messages.withdrawSuccess")
                .replace("{amount}", String.valueOf(amount))
                .replace("{balance}", String.valueOf(balance)));
    }


    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();

        if(npc.data().get("isPartier") != null){
            Player player = event.getClicker();

            player.sendMessage(translateColor("§7{name} §8► §r&d&kA&r &2La multi ani Denis!!! &r&d&kA".replace("{name}", npc.getName())));
            return;
        }


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
            double bal = getBankBalance(player);
            double interestPerc = config.getDouble("interest.percentage");
            double interestAmount = bal * (interestPerc / 100);
            interestAmount = Math.round(interestAmount*100.0)/100.0; // Round to 2 digits after the .
            List<Integer> timeLeftTillMidnight = getTimeLeftUntilMidnight();
            for(String line: getConfList("UI.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(bal))
                        .replace("{interest}", String.valueOf(interestPerc))
                        .replace("{interestAmount}", String.valueOf(interestAmount))
                        .replace("{nextInterestHours}", timeLeftTillMidnight.getFirst().toString())
                        .replace("{nextInterestMinutes}", timeLeftTillMidnight.get(1).toString())));
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

    public void openDepositMenu(Player player, double amount) {
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(getConf("UI.deposit.depositTitle")));

        // Create items with new Component-based display names
        ItemStack confirmDepositItem = new ItemStack(Material.GREEN_DYE);
        ItemMeta meta = confirmDepositItem.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text(getConf("UI.deposit.confirmDepositItem").replace("{amount}", String.valueOf(amount))));
            confirmDepositItem.setItemMeta(meta);
        }

        ItemStack cancelDepositItem = new ItemStack(Material.RED_DYE);
        ItemMeta meta2 = cancelDepositItem.getItemMeta();
        if (meta2 != null) {
            meta2.itemName(Component.text(getConf("UI.deposit.cancelDepositItem")));
            cancelDepositItem.setItemMeta(meta2);
        }

        ItemStack setAmountDepositItem = new ItemStack(Material.PAPER);
        ItemMeta meta3 = setAmountDepositItem.getItemMeta();
        if (meta3 != null) {
            meta3.itemName(Component.text(getConf("UI.deposit.setAmountDepositItem")));
            setAmountDepositItem.setItemMeta(meta3);
        }

        ItemStack infoItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta4 = infoItem.getItemMeta();
        if (meta4 != null) {
            meta4.itemName(Component.text(getConf("UI.infoItem")));
            List<Component> lore = new ArrayList<>();
            double bal = getBankBalance(player);
            double interestPerc = config.getDouble("interest.percentage");
            double interestAmount = bal * (interestPerc / 100);
            interestAmount = Math.round(interestAmount*100.0)/100.0; // Round to 2 digits after the .
            List<Integer> timeLeftTillMidnight = getTimeLeftUntilMidnight();
            for(String line: getConfList("UI.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(bal))
                        .replace("{interest}", String.valueOf(interestPerc))
                        .replace("{interestAmount}", String.valueOf(interestAmount))
                        .replace("{nextInterestHours}", timeLeftTillMidnight.getFirst().toString())
                        .replace("{nextInterestMinutes}", timeLeftTillMidnight.get(1).toString())));
            }
            meta4.lore(lore);
            infoItem.setItemMeta(meta4);
        }

        ItemStack invalidAmountDepositItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta5 = invalidAmountDepositItem.getItemMeta();
        if (meta5 != null) {
            meta5.itemName(Component.text(getConf("UI.deposit.invalidAmountDepositItem")));
            invalidAmountDepositItem.setItemMeta(meta5);
        }

        ItemStack inputAmountFirstItem = new ItemStack(Material.YELLOW_DYE);
        ItemMeta meta6 = inputAmountFirstItem.getItemMeta();
        if (meta6 != null) {
            meta6.itemName(Component.text(getConf("UI.deposit.inputAmountFirstItem")));
            inputAmountFirstItem.setItemMeta(meta6);
        }

        // Fill the inventory with black stained glass panes
        fillInventoryWithGlass(inv);

        // Place items in GUI
        if(amount <= 0) inv.setItem(11, inputAmountFirstItem);
        else inv.setItem(11, confirmDepositItem);

        if(amount < 0) inv.setItem(13, invalidAmountDepositItem);
        else inv.setItem(13, setAmountDepositItem);

        inv.setItem(15, cancelDepositItem);

        inv.setItem(22, infoItem);

        // Open the GUI for the player
        player.openInventory(inv);
    }

    public void openWithdrawMenu(Player player, double amount) {
        // Use the new method to create the inventory with a title as Component
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(getConf("UI.withdraw.withdrawTitle")));

        // Create items with new Component-based display names
        ItemStack confirmWithdrawItem = new ItemStack(Material.GREEN_DYE);
        ItemMeta meta = confirmWithdrawItem.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text(getConf("UI.withdraw.confirmWithdrawItem").replace("{amount}", String.valueOf(amount))));
            confirmWithdrawItem.setItemMeta(meta);
        }

        ItemStack cancelWithdrawItem = new ItemStack(Material.RED_DYE);
        ItemMeta meta2 = cancelWithdrawItem.getItemMeta();
        if (meta2 != null) {
            meta2.itemName(Component.text(getConf("UI.withdraw.cancelWithdrawItem")));
            cancelWithdrawItem.setItemMeta(meta2);
        }

        ItemStack setAmountWithdrawItem = new ItemStack(Material.PAPER);
        ItemMeta meta3 = setAmountWithdrawItem.getItemMeta();
        if (meta3 != null) {
            meta3.itemName(Component.text(getConf("UI.withdraw.setAmountWithdrawItem")));
            setAmountWithdrawItem.setItemMeta(meta3);
        }

        ItemStack infoItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta4 = infoItem.getItemMeta();
        if (meta4 != null) {
            meta4.itemName(Component.text(getConf("UI.infoItem")));
            List<Component> lore = new ArrayList<>();
            double bal = getBankBalance(player);
            double interestPerc = config.getDouble("interest.percentage");
            double interestAmount = bal * (interestPerc / 100);
            interestAmount = Math.round(interestAmount*100.0)/100.0; // Round to 2 digits after the .
            List<Integer> timeLeftTillMidnight = getTimeLeftUntilMidnight();
            for(String line: getConfList("UI.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(bal))
                        .replace("{interest}", String.valueOf(interestPerc))
                        .replace("{interestAmount}", String.valueOf(interestAmount))
                        .replace("{nextInterestHours}", timeLeftTillMidnight.getFirst().toString())
                        .replace("{nextInterestMinutes}", timeLeftTillMidnight.get(1).toString())));
            }
            meta4.lore(lore);
            infoItem.setItemMeta(meta4);
        }

        ItemStack invalidAmountWithdrawItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta5 = invalidAmountWithdrawItem.getItemMeta();
        if (meta5 != null) {
            meta5.itemName(Component.text(getConf("UI.withdraw.invalidAmountWithdrawItem")));
            invalidAmountWithdrawItem.setItemMeta(meta5);
        }

        ItemStack inputAmountFirstItem = new ItemStack(Material.YELLOW_DYE);
        ItemMeta meta6 = inputAmountFirstItem.getItemMeta();
        if (meta6 != null) {
            meta6.itemName(Component.text(getConf("UI.withdraw.inputAmountFirstItem")));
            inputAmountFirstItem.setItemMeta(meta6);
        }

        // Fill the inventory with black stained glass panes
        fillInventoryWithGlass(inv);

        // Place items in GUI

        if(amount <= 0) inv.setItem(11, inputAmountFirstItem);
        else inv.setItem(11, confirmWithdrawItem);

        if(amount < 0) inv.setItem(13, invalidAmountWithdrawItem);
        else inv.setItem(13, setAmountWithdrawItem);

        inv.setItem(15, cancelWithdrawItem);
        inv.setItem(22, infoItem);

        // Open the GUI for the player
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        // Check if the clicked inventory is your banker inventory
        if (event.getView().title().equals(Component.text(getConf("UI.bankerTitle")))){
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Check which item was clicked
            if (clickedItem.getType() == Material.CHEST) {
                openDepositMenu(player, 0);
            } else if (clickedItem.getType() == Material.DROPPER) {
                openWithdrawMenu(player, 0);
            }
        }

        else if (event.getView().title().equals(Component.text(getConf("UI.deposit.depositTitle")))){
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (clickedItem.getType() == Material.GREEN_DYE) {
                String name = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().itemName());
                String sAmount = name.split("\\$")[1];
                double amount = Double.parseDouble(sAmount);
                depositToBank(player, amount);
            } else if (clickedItem.getType() == Material.RED_DYE) {
                openBankerGUI(player);
            } else if (clickedItem.getType() == Material.PAPER || clickedItem.getType() == Material.REDSTONE_BLOCK) {
                try {
                    SignGUI gui = SignGUI.builder()
                            // set lines
                            .setLines(
                                    null,
                                    getConf("UI.deposit.depositSignLines.line2"),
                                    getConf("UI.deposit.depositSignLines.line3"),
                                    getConf("UI.deposit.depositSignLines.line4")
                            )

                            // set the sign type
                            .setType(Material.DARK_OAK_SIGN)

                            // set the sign color
                            .setColor(DyeColor.BLACK)

                            // set the handler/listener (called when the player finishes editing)
                            .setHandler((p, result) -> {
                                String line0 = result.getLine(0);

                                double amount;

                                try {
                                    amount = Double.parseDouble(line0);
                                } catch (Exception e) {
                                    amount = -1;
                                }

                                if (amount <= 0) amount = -1;

                                double finalAmount = Math.round(amount*100.0)/100.0; // Round to 2 digits after the .
                                return List.of(
                                        SignGUIAction.run(() -> {
                                            // Run on the main thread
                                            Bukkit.getScheduler().runTask(this, () -> openDepositMenu(p, finalAmount));
                                        })
                                );
                            })

                            // build the SignGUI
                            .build();

                    // open the sign
                    gui.open(player);
                } catch (SignGUIVersionException e) {
                    // This error is thrown if SignGUI does not support this server version (yet).
                }
            }
        }

        else if (event.getView().title().equals(Component.text(getConf("UI.withdraw.withdrawTitle")))){
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (clickedItem.getType() == Material.GREEN_DYE) {
                String name = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().itemName());
                String sAmount = name.split("\\$")[1];
                double amount = Double.parseDouble(sAmount);
                withdrawFromBank(player, amount);
            } else if (clickedItem.getType() == Material.RED_DYE) {
                openBankerGUI(player);
            } else if (clickedItem.getType() == Material.PAPER || clickedItem.getType() == Material.REDSTONE_BLOCK) {
                try {
                    SignGUI gui = SignGUI.builder()
                            // set lines
                            .setLines(
                                    null,
                                    getConf("UI.withdraw.withdrawSignLines.line2"),
                                    getConf("UI.withdraw.withdrawSignLines.line3"),
                                    getConf("UI.withdraw.withdrawSignLines.line4")
                            )

                            // set the sign type
                            .setType(Material.DARK_OAK_SIGN)

                            // set the sign color
                            .setColor(DyeColor.BLACK)

                            // set the handler/listener (called when the player finishes editing)
                            .setHandler((p, result) -> {
                                String line0 = result.getLine(0);

                                double amount;

                                try {
                                    amount = Double.parseDouble(line0);
                                } catch (Exception e) {
                                    amount = -1;
                                }

                                if (amount <= 0) amount = -1;

                                double finalAmount = Math.round(amount*100.0)/100.0; // Round to 2 digits after the .
                                return List.of(
                                        SignGUIAction.run(() -> {
                                            // Run on the main thread
                                            Bukkit.getScheduler().runTask(this, () -> openWithdrawMenu(p, finalAmount));
                                        })
                                );
                            })

                            // build the SignGUI
                            .build();

                    // open the sign
                    gui.open(player);
                } catch (SignGUIVersionException e) {
                    // This error is thrown if SignGUI does not support this server version (yet).
                }
            }
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
                    accounts = getAccountsConfig();

                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("createbanker")) {
                if (args.length == 2) {
                    createBanker(player.getLocation(), args[0], args[1]);
                    player.sendMessage(getConf("messages.prefix") + "§2Banker created.");
                }
                else {
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

    public List<Integer> getTimeLeftUntilMidnight() {
        LocalTime now = LocalTime.now();
        LocalTime midnight = LocalTime.MIDNIGHT; // 00:00
        Duration duration = Duration.between(now, midnight);

        // If it's past midnight, add 24h to correctly count till next midnight
        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60; // Get remaining minutes
        List<Integer> timeLeft = new ArrayList<>();
        timeLeft.add((int) hours);
        timeLeft.add((int) minutes);
        return timeLeft;
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
