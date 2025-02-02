package alces.artifacts;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class Artifacts extends JavaPlugin implements TabCompleter, Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigData();

        getCommand("artifacts").setExecutor(this);
        getCommand("artifacts").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("Artifacts plugin has been enabled!");
    }

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
    }

    @Override
    public void onDisable() {
        getLogger().info("Artifacts plugin has been disabled!");
    }

    @EventHandler
    public void onInventoryChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        updateEffects(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Bukkit.getScheduler().runTaskLater(this, () -> updateEffects((Player) event.getWhoClicked()), 1L);
        }
    }

    private void updateEffects(Player player) {
        Set<String> activeArtifacts = new HashSet<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                activeArtifacts.add(itemName);
            }
        }

        applyEffects(player, activeArtifacts);
    }

    private void applyEffects(Player player, Set<String> artifacts) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        if (artifacts.contains("Dragon's Heart")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1));
        }
        if (artifacts.contains("Soul Reaper's Mask")) {
            player.getAttribute(Attribute.GENERIC_LUCK).setBaseValue(1);
        }
        if (artifacts.contains("Swiftstone")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1));
        }
        if (artifacts.contains("Lunar Tear")) {
            if (player.getWorld().getTime() > 13000) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
            } else {
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (hasArtifact(player, "Volcanic Sigil")) {
            player.getWorld().createExplosion(player.getLocation(), 4F, false, false);
        }
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            if (hasArtifact(player, "Soul Reaper's Mask")) {
                double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                double currentHealth = player.getHealth();
                if (currentHealth < maxHealth) {
                    player.setHealth(Math.min(currentHealth + 4, maxHealth));
                }
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(ChatColor.RED + "Artifacts cannot be used in crafting!");
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeEffects(event.getPlayer());
    }

    private void removeEffects(Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.getAttribute(Attribute.GENERIC_LUCK).setBaseValue(0);
    }

    public static ItemStack getArtifact(String name, Material material, ChatColor color, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + name);
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.GRAY + line);
        }
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private boolean hasArtifact(Player player, String artifactName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(artifactName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("artifacts.command")) {
            player.sendMessage(ChatColor.GREEN + getMessage("no_permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("artifacts.reload")) {
                player.sendMessage(ChatColor.GREEN + getMessage("no_permission"));
                return true;
            }
            reloadConfig();
            reloadConfigData();
            player.sendMessage(ChatColor.GREEN + getMessage("reloaded"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + getMessage("usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(getMessage("player_not_found").replace("{player}", args[0]));
            return true;
        }

        ItemStack artifact = getArtifact(args[1]);
        if (artifact == null) {
            player.sendMessage(getMessage("invalid_artifact"));
            return true;
        }

        target.getInventory().addItem(artifact);
        target.sendMessage(getMessage("received_artifact").replace("{artifact}", artifact.getItemMeta().getDisplayName()));
        player.sendMessage(getMessage("given_artifact")
                .replace("{player}", target.getName())
                .replace("{artifact}", artifact.getItemMeta().getDisplayName()));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("artifacts")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                return getArtifactNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    public void reloadConfigData() {
        config = getConfig();
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + key, "&cMessage not found!"));
    }

    public ItemStack getArtifact(String name) {
        if (!config.contains("artifacts." + name)) {
            return null;
        }

        String materialName = config.getString("artifacts." + name + ".material", "STONE");
        String displayName = ChatColor.translateAlternateColorCodes('&', config.getString("artifacts." + name + ".name", "&fUnknown Artifact"));
        List<String> lore = config.getStringList("artifacts." + name + ".lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public List<String> getArtifactNames() {
        return new ArrayList<>(config.getConfigurationSection("artifacts").getKeys(false));
    }
}
