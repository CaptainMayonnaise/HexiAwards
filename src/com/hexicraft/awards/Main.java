package com.hexicraft.awards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Ollie
 * @version 1.0
 */
public class Main extends JavaPlugin {

    private ArrayList<String> awards;
    private ArrayList<String> winners;
    private ArrayList<String> quotes;
    private ArrayList<String> nominees;
    private boolean loaded = false;
    private int counter = 0;

    @Override
    public void onEnable() {
        getLogger().warning(ChatColor.RED + reload().getMessage());
    }

    private ReturnCode reload() {
        if (loadFiles()) {
            loaded = true;
            counter = 0;
            return ReturnCode.SUCCESS;
        } else {
            loaded = false;
            return ReturnCode.NOT_LOADED_PROPERLY;
        }
    }

    private boolean loadFiles() {
        try {
            awards = (ArrayList<String>) Files.readAllLines(Paths.get(this.getDataFolder() + File.separator +
                            "awards.txt"), Charset.defaultCharset());
            winners = (ArrayList<String>) Files.readAllLines(Paths.get(this.getDataFolder() + File.separator +
                            "winners.txt"), Charset.defaultCharset());
            quotes = (ArrayList<String>) Files.readAllLines(Paths.get(this.getDataFolder() + File.separator +
                            "quotes.txt"), Charset.defaultCharset());
            nominees = (ArrayList<String>) Files.readAllLines(Paths.get(this.getDataFolder() + File.separator +
                            "nominees.txt"), Charset.defaultCharset());
        } catch (IOException e) {
            getLogger().warning("An I/O error has occurred while enabling HexiAwards:");
            e.printStackTrace();
            return false;
        }
        if (awards.size() != winners.size() ||
                awards.size() != quotes.size() ||
                awards.size() != nominees.size() ||
                awards.size() == 0) {
            getLogger().warning("The size of the files are different.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        ReturnCode code;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (cmd.getName().toLowerCase()) {
                case "award":
                    code = award(player, args);
                    break;
                default:
                    code = ReturnCode.INVALID_COMMAND;
            }
        } else {
            code = ReturnCode.NOT_PLAYER;
        }

        if (code != null && code.hasMessage()) {
            // Send the resulting message to the sender
            sender.sendMessage(ChatColor.RED + code.getMessage(cmd));
        }
        return true;
    }

    private ReturnCode award(Player player, String[] args) {
        if (args.length == 0) {
            return next(player);
        } else {
            String cmdLower = args[0].toLowerCase();
            if (!Objects.equals(cmdLower, "reload") && !loaded) {
                return ReturnCode.NOT_LOADED_PROPERLY;
            }
            switch (cmdLower) {
                case "reload":
                    return reload();
                case "peek":
                    return peek(player);
                case "p":
                    return peek(player);
                case "next":
                    return next(player);
                default:
                    if (isInteger(cmdLower)) {
                        return set(player, Integer.parseInt(cmdLower));
                    } else {
                        return ReturnCode.INVALID_COMMAND;
                    }
            }
        }
    }

    private ReturnCode next(Player player) {
        sendToAll();
        giveItem(player);
        counter = (counter + 1) % awards.size();
        return ReturnCode.SUCCESS;
    }

    private ReturnCode peek(Player player) {
        player.sendMessage(ChatColor.GOLD + "Nominees: " + ChatColor.WHITE + nominees.get(counter));
        sendSingle(player);
        return ReturnCode.SUCCESS;
    }

    private ReturnCode set(Player player, int newCounter) {
        counter = newCounter % awards.size();
        return peek(player);
    }

    private void sendToAll() {
        for (Player currentPlayer : Bukkit.getServer().getOnlinePlayers()) {
            sendSingle(currentPlayer);
        }

    }

    private void sendSingle(Player player) {
        sendTime(player, 5, 75, 25);
        sendSubtitle(player, winners.get(counter));
        sendTitle(player, awards.get(counter));
        player.sendMessage(ChatColor.GOLD + "The " + ChatColor.WHITE + awards.get(counter) + ChatColor.GOLD +
                " award has been won by " + ChatColor.WHITE + winners.get(counter) + ChatColor.GOLD + "!");
        player.sendMessage("" + ChatColor.DARK_GRAY + ChatColor.ITALIC + quotes.get(counter));
    }

    private void sendTime(Player player, int fadeIn, int display, int fadeOut) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/title " +
                player.getName() +
                " times " +
                fadeIn +
                " " +
                display +
                " " +
                fadeOut);
    }

    private void sendSubtitle(Player player, String winner) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " +
                player.getName() +
                " subtitle [\"\",{\"text\":\"Won by \",\"color\":\"white\"},{\"text\":\"" +
                winner +
                "\",\"color\":\"gold\"}]");
    }

    private void sendTitle(Player player, String award) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " +
                player.getName() +
                " title [\"\",{\"text\":\"" +
                award +
                "\",\"color\":\"gold\"}]");
    }

    private void giveItem(Player player) {
        ItemStack award = new MaterialData(Material.GOLD_BLOCK).toItemStack(1);
        ItemMeta meta = award.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + awards.get(counter));
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "HexiAwards 2015");
        lore.add(ChatColor.GRAY + "Won by " + ChatColor.GOLD + winners.get(counter));
        lore.addAll(wrapQuote(quotes.get(counter)));
        meta.setLore(lore);
        award.setItemMeta(meta);
        player.getInventory().addItem(award);
    }

    private Collection<? extends String> wrapQuote(String quote) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        currentLine.append(ChatColor.DARK_GRAY);
        currentLine.append(ChatColor.ITALIC);
        StringBuilder currentWord = new StringBuilder();
        for (char c : quote.toCharArray()) {
            currentWord.append(c);
            if (c == ' ') {
                currentLine.append(currentWord);
                currentWord.setLength(0);
            }
            if (currentLine.length() + currentWord.length() >= 30) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(ChatColor.DARK_GRAY);
                currentLine.append(ChatColor.ITALIC);
            }
        }
        currentLine.append(currentWord);
        if (currentLine.length() > 4) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /**
     * Source: http://stackoverflow.com/questions/237159/whats-the-best-way-to-check-to-see-if-a-string-represents-an-integer-in-java
     * @param str String to be tested.
     * @return Whether or not the string is an integer.
     */
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
