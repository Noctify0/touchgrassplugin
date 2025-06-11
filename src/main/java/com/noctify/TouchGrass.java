package com.noctify;

import com.noctify.GrassCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

import java.util.*;

public class TouchGrass extends JavaPlugin implements Listener {

    private final Map<UUID, Long> loginTimestamps = new HashMap<>();
    private final Set<UUID> affectedPlayers = new HashSet<>();
    private final Map<UUID, Integer> titleTasks = new HashMap<>();
    private int decaySeconds;
    private String titleMain;
    private String titleSub;
    private int titleFadeIn, titleStay, titleFadeOut;
    private List<PotionEffect> effects = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this, this);
        affectedPlayers.clear();
        titleTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        titleTasks.clear();

        this.getCommand("resetgrass").setExecutor(new GrassCommandExecutor(this, true));
        this.getCommand("applygrass").setExecutor(new GrassCommandExecutor(this, false));

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("touchgrass.bypass")) continue;
                if (affectedPlayers.contains(player.getUniqueId())) continue;

                long onlineTime = (System.currentTimeMillis() - loginTimestamps.getOrDefault(player.getUniqueId(), 0L)) / 1000;
                if (onlineTime >= decaySeconds) {
                    applyTouchGrassEffect(player);
                }
            }
        }, 20L * 60, 20L * 60);
    }

    private void loadConfigValues() {
        decaySeconds = getConfig().getInt("player_playtime", 7200);

        ConfigurationSection title = getConfig().getConfigurationSection("title");
        titleMain = title.getString("main", "§a🌿Touch Grass🌿");
        titleSub = title.getString("subtitle", "");
        titleFadeIn = title.getInt("fadein", 0);
        titleStay = title.getInt("stay", 40);
        titleFadeOut = title.getInt("fadeout", 0);

        effects.clear();
        for (Map<?, ?> eff : getConfig().getMapList("effects")) {
            try {
                PotionEffectType type = PotionEffectType.getByName((String) eff.get("type"));
                int duration = eff.get("duration") instanceof Number ? ((Number) eff.get("duration")).intValue() : Integer.MAX_VALUE;
                int amplifier = eff.get("amplifier") instanceof Number ? ((Number) eff.get("amplifier")).intValue() : 0;
                boolean ambient = eff.get("ambient") instanceof Boolean ? (Boolean) eff.get("ambient") : false;
                boolean particles = eff.get("particles") instanceof Boolean ? (Boolean) eff.get("particles") : false;
                boolean icon = eff.get("icon") instanceof Boolean ? (Boolean) eff.get("icon") : false;
                if (type != null) {
                    effects.add(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDisable() {
        titleTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        titleTasks.clear();
        affectedPlayers.clear();
    }

    public void resetGrassTimer(Player player) {
        loginTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        affectedPlayers.remove(player.getUniqueId());
        removeTitleTask(player.getUniqueId());
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loginTimestamps.put(player.getUniqueId(), System.currentTimeMillis());

        if (affectedPlayers.contains(player.getUniqueId())) {
            applyTouchGrassEffect(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loginTimestamps.remove(event.getPlayer().getUniqueId());
        removeTitleTask(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (affectedPlayers.contains(event.getEntity().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    applyTouchGrassEffect(event.getEntity()), 20L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (affectedPlayers.contains(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    applyTouchGrassEffect(event.getPlayer()), 20L);
        }
    }

    @EventHandler
    public void onMilkDrink(PlayerItemConsumeEvent event) {
        if (affectedPlayers.contains(event.getPlayer().getUniqueId()) &&
                event.getItem().getType() == Material.MILK_BUCKET) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    applyTouchGrassEffect(event.getPlayer()), 5L);
        }
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player &&
                affectedPlayers.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    applyTouchGrassEffect(player), 5L);
        }
    }

    public void applyTouchGrassEffect(Player player) {
        UUID uuid = player.getUniqueId();
        affectedPlayers.add(uuid);
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(effect);
        }
        startTitleTask(player);
    }

    private void startTitleTask(Player player) {
        UUID uuid = player.getUniqueId();
        removeTitleTask(uuid);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (player.isOnline() && affectedPlayers.contains(uuid)) {
                player.sendTitle(titleMain, titleSub, titleFadeIn, titleStay, titleFadeOut);
            }
        }, 0L, 40L);
        titleTasks.put(uuid, taskId);
    }

    private void removeTitleTask(UUID uuid) {
        Integer taskId = titleTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}