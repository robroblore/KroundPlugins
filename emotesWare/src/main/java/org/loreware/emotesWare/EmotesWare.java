package org.loreware.emotesWare;

import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class EmotesWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    final HashMap<UUID, Long> cooldowns = new HashMap<>();

    int cooldownTime = 5;

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("EmotesWare plugin enabled");

        saveDefaultConfig();

        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        cooldownTime = config.getInt("vars.defaultCooldown", 5);

        getServer().getPluginManager().registerEvents(this, this);
    }


    public String getConf(String path){
        return Component.text(config.getString(path, String.format("&4&l[entry %s not found]", path)).replaceAll("&", "§")).content();
    }

    public boolean checkForCooldown(Player player){
        if (!player.hasPermission("emotesware.bypassCooldown")){
            if (!cooldowns.containsKey(player.getUniqueId())){
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            } else{
                long timeElapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());

                if (timeElapsed < cooldownTime * 1000L){
                    player.sendMessage(getConf("messages.prefix") + getConf("messages.errors.cooldown")
                            .replace("{time}", String.format("%.2f", (cooldownTime * 1000L - timeElapsed) / 1000f)));
                    return false;
                } else{
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }
        }

        return true;
    }

    public Player basicEmoteHandler(Player player, String[] args, String emoteName){

        // Cooldown management

        if(!checkForCooldown(player)) return null;

        if (args.length == 0) {
            player.sendMessage(getConf("messages.prefix") + getConf("messages.errors.specifyPlayer")
                    .replace("{action}", getConf(String.format("actions.%s.1", emoteName))));
            return null;
        }

        String pName = args[0];
        Player target = getServer().getPlayer(pName);

        if (target == null) {
            player.sendMessage(getConf("messages.prefix") + getConf("messages.errors.playerNotFound")
                    .replace("{player}", pName));
            return null;
        }

        double distance = player.getLocation().distance(target.getLocation());

        if (distance > 5) {
            player.sendMessage(getConf("messages.prefix") + getConf("messages.errors.playerTooFar")
                    .replace("{player}", pName));
            return null;
        }

        player.sendMessage(getConf("messages.prefix") + getConf("messages.actions.caster")
                .replace("{action}", getConf(String.format("actions.%s.2", emoteName)))
                .replace("{target}", target.getName()));
        target.sendMessage(getConf("messages.prefix") + getConf("messages.actions.target")
                .replace("{action}", getConf(String.format("actions.%s.2", emoteName)))
                .replace("{player}", player.getName()));

        return target;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if(cmd.getName().equalsIgnoreCase("emotesware")){
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();
                    cooldownTime = config.getInt("vars.defaultCooldown", 5);
                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }

            else if(cmd.getName().equalsIgnoreCase("kiss") || cmd.getName().equalsIgnoreCase("sarut")) {
                Player target = basicEmoteHandler(player, args, "kiss");

                if (target == null) return true;

                // Spawn heart particles
                player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 3, .5, .5, .5);
                target.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 3, .5, .5, .5);
            }

            else if(cmd.getName().equalsIgnoreCase("hug") || cmd.getName().equalsIgnoreCase("imbratisare")) {
                Player target = basicEmoteHandler(player, args, "hug");

                if (target == null) return true;

                if (target == player){
                    player.sendMessage(getConf("messages.prefix") + getConf("messages.actions.selfHug"));
                }

                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0,1,0), 6, .6, .2, .6);
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0,1,0), 6, .6, .2, .6);
            }

            else if(cmd.getName().equalsIgnoreCase("fart") || cmd.getName().equalsIgnoreCase("part") || cmd.getName().equalsIgnoreCase("besina")) {
                if(!checkForCooldown(player)) return true;


                player.sendMessage(getConf("messages.prefix") + getConf("messages.actions.part"));

                player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation(), 10, .1, 0, .1);
            }

            else if(cmd.getName().equalsIgnoreCase("superfart") || cmd.getName().equalsIgnoreCase("superpart") || cmd.getName().equalsIgnoreCase("superbesina")) {
                if(!checkForCooldown(player)) return true;

                player.sendMessage(getConf("messages.prefix") + getConf("messages.actions.superpart"));

                player.setVelocity(player.getVelocity().add(new Vector(0, 0.5, 0)));

                player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation(), 50, .1, 1, .1);
            }

            else if (cmd.getName().equalsIgnoreCase("slap")){
                if(!checkForCooldown(player)) return true;

                Player target = basicEmoteHandler(player, args, "slap");

                if (target == null) return true;

                target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0,1,0), 6, .6, .2, .6);
                target.setVelocity(target.getVelocity().add(new Vector(0, .5, 0)));
            }

            else if (cmd.getName().equalsIgnoreCase("superslap")){
                if(!checkForCooldown(player)) return true;

                Player target = basicEmoteHandler(player, args, "superslap");

                if (target == null) return true;

                target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0,1,0), 200, .6, 40, .6);
                target.setVelocity(target.getVelocity().add(new Vector(0, 50, 0)));
            }

        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("emotesware")){
            if(sender instanceof Player){
                Player player = (Player) sender;

                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }
        return null;
    }


}
