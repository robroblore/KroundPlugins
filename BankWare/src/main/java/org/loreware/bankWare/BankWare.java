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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class BankWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;
    FileConfiguration accounts;
    private static Economy econ;

    @Override
    public void onEnable() {
        System.out.println("BankWare plugin enabled");
//        saveResource("config.yml", /* replace */ true);
//        config = getConfig();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        config = getConfig();

        accounts = getAccountsConfig();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp.getProvider();

        getServer().

                getPluginManager().registerEvents(this, this);
    }

    public double getBankBalance(OfflinePlayer player) {
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
        amount = round(amount);

        return amount;
    }

    public void depositToBank(Player player, double amount) {
        if(econ.getBalance(player) < amount){
            player.sendMessage(getConf("messages.prefix") + getConf("messages.depositFail")
                    .replace("{amount}", String.valueOf(amount)));
            return;
        }
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);
        econ.withdrawPlayer(player, amount);
        balance = round(balance + amount);
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        player.sendMessage(getConf("messages.prefix") + getConf("messages.depositSuccess")
                .replace("{amount}", String.valueOf(amount))
                .replace("{balance}", String.valueOf(balance)));
        setNextInterest(player, true);
        player.playSound(player, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
    }

    public void withdrawFromBank(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);
        if(balance < amount){
            player.sendMessage(getConf("messages.prefix") + getConf("messages.withdrawFail")
                    .replace("{amount}", String.valueOf(amount)));
            return;
        }
        balance = round(balance - amount);
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        econ.depositPlayer(player, amount);
        player.sendMessage(getConf("messages.prefix") + getConf("messages.withdrawSuccess")
                .replace("{amount}", String.valueOf(amount))
                .replace("{balance}", String.valueOf(balance)));
        setNextInterest(player, true);
        player.playSound(player, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
    }

    public List<OfflinePlayer> getBaltop() {
        List<OfflinePlayer> players = Arrays.asList(Bukkit.getOfflinePlayers());
        players.sort(Comparator.comparingDouble(this::getPlayersTotalBal).reversed());

        return players;
    }

    public Double getPlayersTotalBal(OfflinePlayer player){
        return round(econ.getBalance(player) + getBankBalance(player));
    }

    public boolean payFromBank(Player player, double amount){
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);
        if(balance < amount) return false;

        balance = round(balance - amount);
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        setNextInterest(player, true);

        return true;
    }

    public void payToBank(Player player, double amount){
        UUID uuid = player.getUniqueId();
        double balance = getBankBalance(player);

        balance = round(balance + amount);
        accounts.set(uuid.toString() + ".balance", balance);
        saveAccountsConfig();
        setNextInterest(player, true);
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
        calculateAccumulatedInterest(player);
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

        ItemStack infoItem = getInfoItem(player);

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

        ItemStack infoItem = getInfoItem(player);

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

        ItemStack infoItem = getInfoItem(player);

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

    public ItemStack getInfoItem(Player player){
        ItemStack infoItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text(getConf("UI.infoItem")));
            List<Component> lore = new ArrayList<>();
            double bal = getBankBalance(player);
            double interestPerc = getPlayersInterestPerc(player);
            double interestAmount = bal * (interestPerc / 100);
            interestAmount = round(interestAmount); // Round to 2 digits after the .

            for(String line: getConfList("UI.infoItemLore")){
                lore.add(Component.text(line
                        .replace("{player}", player.getName())
                        .replace("{balance}", String.valueOf(bal))
                        .replace("{interest}", String.valueOf(interestPerc))
                        .replace("{interestAmount}", String.valueOf(interestAmount))
                        .replace("{nextInterestHours}", String.valueOf(getTimeLeftTillInterest(player) / 60))
                        .replace("{nextInterestMinutes}", String.valueOf(getTimeLeftTillInterest(player) % 60)))
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            infoItem.setItemMeta(meta);
        }

        return infoItem;
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
                player.closeInventory();
            } else if (clickedItem.getType() == Material.RED_DYE) {
                openBankerGUI(player);
            } else if (clickedItem.getType() == Material.PAPER || clickedItem.getType() == Material.REDSTONE_BLOCK  || clickedItem.getType() == Material.YELLOW_DYE) {
                player.sendMessage(getConf("messages.prefix") + getConf("messages.idiotProof"));
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

                            .setLocation(player.getLocation().subtract(0, 5, 0))

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

                                double finalAmount = round(amount); // Round to 2 digits after the .
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
                } catch (Exception e) {
                    debug(e.getMessage());
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
                player.closeInventory();
            } else if (clickedItem.getType() == Material.RED_DYE) {
                openBankerGUI(player);
            } else if (clickedItem.getType() == Material.PAPER || clickedItem.getType() == Material.REDSTONE_BLOCK || clickedItem.getType() == Material.YELLOW_DYE) {
                player.sendMessage(getConf("messages.prefix") + getConf("messages.idiotProof"));
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

                            .setLocation(player.getLocation().subtract(0, 5, 0))

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

                                double finalAmount = round(amount); // Round to 2 digits after the .
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

    public int getTimeLeftTillInterest(Player player){
        if(accounts.getString(player.getUniqueId().toString() + ".nextInterest") == null) setNextInterest(player, true);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextInterest = LocalDateTime.parse(accounts.getString(player.getUniqueId().toString() + ".nextInterest"));
        Duration duration = Duration.between(now, nextInterest);
        return (int) duration.toMinutes();
    }

    public void setNextInterest(Player player, boolean startingNow){
        if(startingNow) {
            accounts.set(player.getUniqueId().toString() + ".nextInterest",
                    LocalDateTime.now().plusMinutes(config.getInt("interest.interval")).toString());
        }
        else{
            accounts.set(player.getUniqueId().toString() + ".nextInterest",
                    LocalDateTime.parse(accounts.getString(player.getUniqueId().toString() + ".nextInterest"))
                            .plusMinutes(config.getInt("interest.interval")).toString());
        }

        saveAccountsConfig();
    }

    public void calculateAccumulatedInterest(Player player){
        if(getTimeLeftTillInterest(player) > config.getInt("interest.interval")){
            setNextInterest(player, true);
        }
        while(LocalDateTime.now().isAfter(LocalDateTime.parse(accounts.getString(player.getUniqueId().toString() + ".nextInterest")))){
            double balance = getBankBalance(player);
            double interestPerc = getPlayersInterestPerc(player);
            double interestAmount = balance * (interestPerc / 100);
            interestAmount = round(interestAmount); // Round to 2 digits after the .
            accounts.set(player.getUniqueId().toString() + ".balance", balance + interestAmount);
            setNextInterest(player, false);
            saveAccountsConfig();
        }
    }

    public double getPlayersInterestPerc(Player player){
        List<String> tiers = new ArrayList<>(config.getConfigurationSection("interest.tiers").getKeys(false));

        for (int i = tiers.size() - 1; i >= 0; i--) {
            String tier = tiers.get(i);
            double value = config.getDouble("interest.tiers." + tier);
            if(player.hasPermission("bankware.interest." + tier)) return value;
        }
        return 0;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("bankware")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
                        if(!player.hasPermission("bankware.canReload")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        reloadConfig();
                        config = getConfig();
                        accounts = getAccountsConfig();

                        player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                        return true;
                    }
                    else if(args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")){
                        if(!player.hasPermission("bankware.givetake")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            player.sendMessage(getConf("messages.prefix") + "§4Player not found.");
                            return true;
                        }
                        double balance = getBankBalance(target);
                        player.sendMessage(getConf("messages.prefix")
                                + "§2" + target.getName() + "'s balance: §e$" + balance);
                    }
                    else if(args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("t")){
                        if(!player.hasPermission("bankware.givetake")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        if (args.length > 2) {
                            Player target = Bukkit.getPlayer(args[2]);
                            if (target == null) {
                                player.sendMessage(getConf("messages.prefix") + "§4Player not found.");
                                return true;
                            }
                            double playerBalance = getBankBalance(target);
                            payFromBank(target, Math.min(playerBalance, Double.parseDouble(args[1])));
                            player.sendMessage(getConf("messages.prefix")
                                    + "§2Taken §e$" + Math.min(playerBalance, Double.parseDouble(args[1])) + "§2 from " + target.getName());
                        }
                        else{
                            player.sendMessage(getConf("messages.prefix") + "§4Usage: /bankware give <amount> <player>");
                        }
                    }
                    else if(args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("g")){
                        if(!player.hasPermission("bankware.givetake")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        if (args.length > 2) {
                            Player target = Bukkit.getPlayer(args[2]);
                            if (target == null) {
                                player.sendMessage(getConf("messages.prefix") + "§4Player not found.");
                                return true;
                            }
                            payToBank(target, Double.parseDouble(args[1]));
                            player.sendMessage(getConf("messages.prefix")
                                    + "§2Given §e$" + Double.parseDouble(args[1]) + "§2 to " + target.getName());
                        }
                        else{
                            player.sendMessage(getConf("messages.prefix") + "§4Usage: /bankware take <amount> <player>");
                        }
                    }
                    else if (args[0].equalsIgnoreCase("createbanker")) {
                        if(!player.hasPermission("bankware.createdeletePOI")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        if (args.length > 2) {
                            createBanker(player.getLocation(), args[1], args[2]);
                            player.sendMessage(getConf("messages.prefix") + "§2Banker created.");
                        }
                        else {
                            player.sendMessage(getConf("messages.prefix") + "§4Usage: /createbanker <name> <skin>");
                        }
                    }
                    else if(args[0].equalsIgnoreCase("deletebanker")){
                        if(!player.hasPermission("bankware.createdeletePOI")){
                            player.sendMessage(getConf("messages.prefix") + "§4You don't have permission to do that.");
                            return true;
                        }
                        if (args.length > 1) {
                            if(deleteBanker(args[1])) player.sendMessage(getConf("messages.prefix") + "§2Banker deleted.");
                            else player.sendMessage(getConf("messages.prefix") + "§4Banker not found.");
                        } else {
                            player.sendMessage(getConf("messages.prefix") + "§4Usage: /deletebanker <name>");
                        }
                    }
                }
            }
            else if (cmd.getName().equalsIgnoreCase("bank")) {
                openBankerGUI(player);
            }
        }
        return true;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        // Check if the command is '/baltop'
        String command = event.getBuffer().toLowerCase();
        if (command.startsWith("/baltop")) {
            // Create a list of possible completions
            List<String> completions = event.getCompletions();

            // Check if the player typed '/baltop ' (without anything after it) or '/baltop t'
            if (command.equals("/baltop ") || command.startsWith("/baltop t")) {
                completions.add("total");  // Add "total" as a suggestion
            }

            // Set the possible completions for the command
            event.setCompletions(completions);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase(); // Get command and normalize case

        // Check if the player typed exactly "/baltop total"
        if (message.equals("/baltop total")) {
            event.setCancelled(true); // Prevent Vault from handling this command

            Player p = event.getPlayer();
            List<String> baltop = getBaltop();
            p.sendMessage(getConf("messages.prefix") + "§2Top balances:");
            for (String line : baltop) {
                p.sendMessage("§e" + line);
            }
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bankware")) {
            List<String> list = new ArrayList<>();

            if(args.length == 1) {
                if (sender.hasPermission("bankware.canReload")) list.add("reload");
                if (sender.hasPermission("bankware.createdeletePOI")) {
                    list.add("createbanker");
                    list.add("deletebanker");
                }
                if(sender.hasPermission("bankware.givetake")) {
                    list.add("give");
                    list.add("take");
                    list.add("balance");
                }
            }
            else if(args.length == 2){
                if (!sender.hasPermission("bankware.createdeletePOI")) return null;
                if(args[0].equalsIgnoreCase("deletebanker")){
                    for(NPC npc: CitizensAPI.getNPCRegistry()){
                        if(npc.data().get("isBanker") != null) list.add(npc.getName());
                    }
                }
                else if(args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")){
                    return null;
                }
            }
            else if (args.length == 3 &&
                    (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")
                    || args[0].equalsIgnoreCase("g") || args[0].equalsIgnoreCase("t"))){
                return null;
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

    public double round(double amount){
        return Math.round(amount*100.0)/100.0;
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

    // Method to fill inventory with black stained glass panes
    private void fillInventoryWithGlass(Inventory inventory) {
        // Create a black stained glass pane ItemStack
        ItemStack blackGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1); // 1 pane at a time

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
