package com.noctify;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class GrassCommandExecutor implements CommandExecutor {

    private final TouchGrass plugin;
    private final boolean isReset;

    public GrassCommandExecutor(TouchGrass plugin, boolean isReset) {
        this.plugin = plugin;
        this.isReset = isReset;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage("Only operators can use this command.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /" + label + " <@a|@s|player>");
            return true;
        }
        List<Player> targets = new ArrayList<>();
        String arg = args[0];
        if (arg.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else if (arg.equalsIgnoreCase("@s")) {
            targets.add(player);
        } else {
            Player target = Bukkit.getPlayerExact(arg);
            if (target == null) {
                sender.sendMessage("Player not found: " + arg);
                return true;
            }
            targets.add(target);
        }
        for (Player target : targets) {
            if (isReset) {
                plugin.resetGrassTimer(target);
                sender.sendMessage("Reset grass timer for " + target.getName());
            } else {
                plugin.applyTouchGrassEffect(target);
                sender.sendMessage("Applied grass effect to " + target.getName());
            }
        }
        return true;
    }
}