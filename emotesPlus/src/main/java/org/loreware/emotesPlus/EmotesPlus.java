package org.loreware.emotesPlus;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class EmotesPlus extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("EmotesPlus plugin enabled");

        saveResource("config.yml", /* replace */ true);

        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    public String getConf(String path){
        return Component.text(config.getString(path, "&4&l[config entry not found]").replaceAll("&", "§")).content();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if(cmd.getName().equalsIgnoreCase("kiss") || cmd.getName().equalsIgnoreCase("sarut")) {
                if (args.length == 0) {
                    player.sendMessage(getConf("messages.kissSpecifyPlayer").replace("{action}", "saruti"));
                    return true;
                }
                String pName = args[0];
                Player target = getServer().getPlayer(pName);

                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Jucatorul nu a fost gasit.");
                    return true;
                }

                double distance = player.getLocation().distance(target.getLocation());

                if (distance > 5) {
                    player.sendMessage(ChatColor.RED + "Jucatorul este prea departe.");
                    return true;
                }

                // Spawn heart particles
                player.sendMessage(ChatColor.GREEN + "L-ai sarutat pe " + target.getName());
                target.sendMessage(ChatColor.GREEN + player.getName() + " te-a sarutat.");

                player.spawnParticle(Particle.HEART, player.getEyeLocation(), 3, .5, .5, .5);
                target.spawnParticle(Particle.HEART, target.getEyeLocation(), 3, .5, .5, .5);
            }

            if(cmd.getName().equalsIgnoreCase("hug") || cmd.getName().equalsIgnoreCase("imbratisare")) {
                if (args.length == 0) {
                    player.sendMessage(ChatColor.RED + "Trebuie sa specifici ce jucator doresti sa imbratisezi.");
                    return true;
                }

                String pName = args[0];
                Player target = getServer().getPlayer(pName);

                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Jucatorul nu a fost gasit.");
                    return true;
                }

                double distance = player.getLocation().distance(target.getLocation());

                if (distance > 5) {
                    player.sendMessage(ChatColor.RED + "Jucatorul este prea departe.");
                    return true;
                }

                // Spawn heart particles
                player.sendMessage(ChatColor.GREEN + "L-ai imbratisat pe " + target.getName());
                target.sendMessage(ChatColor.GREEN + player.getName() + " te-a imbratisat.");

                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0,1,0), 6, .6, .2, .6);
                target.spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0,1,0), 6, .6, .2, .6);
            }

            else if(cmd.getName().equalsIgnoreCase("fart") || cmd.getName().equalsIgnoreCase("part") || cmd.getName().equalsIgnoreCase("besina")) {
                // Spawn heart particles
                player.sendMessage(ChatColor.GREEN + "Ai facut part!");

                player.spawnParticle(Particle.SNEEZE, player.getLocation(), 10, .1, 0, .1);
            }

            else if (cmd.getName().equalsIgnoreCase("slap")){
                if (args.length == 0) {
                    player.sendMessage(ChatColor.RED + "Trebuie sa specifici ce jucator doresti sa palmuiesti.");
                    return true;
                }

                String pName = args[0];
                Player target = getServer().getPlayer(pName);

                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Jucatorul nu a fost gasit.");
                    return true;
                }

                double distance = player.getLocation().distance(target.getLocation());

                if (distance > 5) {
                    player.sendMessage(ChatColor.RED + "Jucatorul este prea departe.");
                    return true;
                }

                // Spawn heart particles
                player.sendMessage(ChatColor.GREEN + "L-ai palmuit pe " + target.getName());
                target.sendMessage(ChatColor.GREEN + player.getName() + " te-a palmuit.");

                target.spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0,1,0), 6, .6, .2, .6);
                target.setVelocity(target.getVelocity().add(new Vector(0, .5, 0)));
            }

        }

        return true;
    }


}
