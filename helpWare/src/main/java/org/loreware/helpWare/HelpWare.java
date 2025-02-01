package org.loreware.helpWare;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class HelpWare extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    HashMap<String, List<String>> helpMessages = new HashMap<>();

    @Override
    public void onEnable() {
        System.out.println("HelpWare plugin enabled");

        saveDefaultConfig();

        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        helpMessages = getHelpMessages();

        getServer().getPluginManager().registerEvents(this, this);
    }

    public HashMap<String, List<String>> getHelpMessages(){
        HashMap<String, List<String>> helpMessages = new HashMap<>();
        for (String key : config.getConfigurationSection("helplist").getKeys(false)){
            helpMessages.put(key, getConfList("helplist." + key));
        }
        return helpMessages;
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

    public String translateColor(String message){
        return Component.text(message.replaceAll("&", "§")).content();
    }


    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event){
        String message = event.getMessage();

        List<String> fullCommand = List.of(message.split(" "));

        String command = fullCommand.getFirst();

        List<String> args = fullCommand.subList(1, fullCommand.size());

        if (Objects.equals(command, "/help") && args.isEmpty()){
            event.setCancelled(true);
            Player player = event.getPlayer();

            List<String> helpMessages = getConfList("messages.help");

            for (String line : helpMessages) {
                player.sendMessage(line);
            }
        } else if(Objects.equals(command, "/help") && !args.isEmpty()){
            event.setCancelled(true);
            Player player = event.getPlayer();

            String helpKey = args.getFirst();

            if (helpMessages.containsKey(helpKey)){
                List<String> helpMessage = helpMessages.get(helpKey);


                for (String line : helpMessage) {
                    player.sendMessage(line);
                }
            } else{
                player.sendMessage(getConf("messages.prefix") + getConf("messages.errors.helpNotFound")
                        .replace("{command}", helpKey));
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("helpware")) {
                if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadConfig();
                    config = getConfig();
                    helpMessages = getHelpMessages();
                    player.sendMessage(getConf("messages.prefix") + "§2Config reloaded.");
                    return true;
                }
            }
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("helpware")){
            if(sender instanceof Player){
                Player player = (Player) sender;

                List<String> list = new ArrayList<>();

                list.add("reload");
                return list;
            }
        }
        else if(cmd.getName().equalsIgnoreCase("help")){
            if(sender instanceof Player){
                Player player = (Player) sender;

                return new ArrayList<>(helpMessages.keySet());
            }
        }
        return null;
    }
}
