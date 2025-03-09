package com.jape.dreamcam;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.event.block.Action;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class DreamCam extends JavaPlugin implements CommandExecutor, Listener, TabCompleter {
    private Map<String, Location> cameras = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, String> cameraRegions = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, Vector> cameraDirections = Collections.synchronizedMap(new LinkedHashMap<>());

    private Map<UUID, Location> previousLocations = new HashMap<>();
    private Map<UUID, Boolean> inCameraMode = new HashMap<>();
    private Map<UUID, GameMode> previousGameModes = new HashMap<>();
    private Map<UUID, List<String>> playerCameras = new HashMap<>();
    private Map<UUID, Integer> playerCameraIndex = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("cameraa").setExecutor(this);
        getCommand("cameraa").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        loadCamerasFromConfig();
    }

    @Override
    public void onDisable() {
        saveCamerasToConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Verwendung: /camera create|delete|menu|reload|save|load (Kameraname) (Regionsname)");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                player.sendMessage("Verwendung: /camera create (Kameraname) (Regionsname)");
                return true;
            }

            String cameraName = args[1];
            String regionName = args[2];
            Location cameraLocation = player.getLocation();
            Vector viewDirection = player.getLocation().getDirection();

            cameras.put(cameraName, cameraLocation);
            cameraRegions.put(cameraName, regionName);
            cameraDirections.put(cameraName, viewDirection);

            player.sendMessage("Kamera '" + cameraName + "' in der Region '" + regionName + "' wurde erstellt und gespeichert.");
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage("Verwendung: /camera delete (Kameraname|Regionsname)");
                return true;
            }

            String name = args[1];
            if (cameras.containsKey(name)) {
                // Einzelne Kamera löschen
                cameras.remove(name);
                cameraRegions.remove(name);
                cameraDirections.remove(name);
                player.sendMessage("Kamera '" + name + "' wurde gelöscht.");
            } else {
                // Ganze Region löschen
                List<String> camerasToRemove = new ArrayList<>();
                for (Map.Entry<String, String> entry : cameraRegions.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(name)) {
                        camerasToRemove.add(entry.getKey());
                    }
                }
                if (!camerasToRemove.isEmpty()) {
                    for (String cameraName : camerasToRemove) {
                        cameras.remove(cameraName);
                        cameraRegions.remove(cameraName);
                        cameraDirections.remove(cameraName);
                    }
                    player.sendMessage("Alle Kameras in der Region '" + name + "' wurden gelöscht.");
                } else {
                    player.sendMessage("Es wurde keine Kamera oder Region mit dem Namen '" + name + "' gefunden.");
                }
            }
        } else if (args[0].equalsIgnoreCase("menu")) {
            if (args.length < 2) {
                player.sendMessage("Verwendung: /camera menu (Regionsname)");
                return true;
            }

            String regionName = args[1];
            List<String> camerasInRegion = new ArrayList<>();
            for (Map.Entry<String, String> entry : cameraRegions.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(regionName)) {
                    camerasInRegion.add(entry.getKey());
                }
            }

            int size = ((camerasInRegion.size() - 1) / 9 + 1) * 9;
            Inventory cameraMenu = Bukkit.createInventory(null, size, "Kameras in " + regionName);

            for (String cameraName : camerasInRegion) {
                ItemStack cameraItem = new ItemStack(Material.BLUE_CONCRETE);
                ItemMeta meta = cameraItem.getItemMeta();
                meta.setDisplayName(cameraName);
                cameraItem.setItemMeta(meta);
                cameraMenu.addItem(cameraItem);
            }

            player.openInventory(cameraMenu);
        } else if (args[0].equalsIgnoreCase("reload")) {
            loadCamerasFromConfig();
            player.sendMessage("Kameras wurden neu geladen.");
        } else if (args[0].equalsIgnoreCase("save")) {
            saveCamerasToConfig();
            player.sendMessage("Kameras wurden gespeichert.");
        } else if (args[0].equalsIgnoreCase("load")) {
            loadCamerasFromConfig();
            player.sendMessage("Kameras wurden geladen.");
        } else {
            player.sendMessage("Unbekannter Befehl. Verwendung: /camera create|delete|menu|reload|save|load (Kameraname) (Regionsname)");
        }

        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Kameras in ")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.BLUE_CONCRETE && item.getItemMeta().hasDisplayName()) {
                String cameraName = item.getItemMeta().getDisplayName();
                if (cameras.containsKey(cameraName)) {
                    Location cameraLocation = cameras.get(cameraName);
                    previousLocations.put(player.getUniqueId(), player.getLocation());
                    previousGameModes.put(player.getUniqueId(), player.getGameMode());
                    inCameraMode.put(player.getUniqueId(), true);
                    givePlayerPotionEffect(player, PotionEffectType.NIGHT_VISION, 600000000, 2, true);
                    player.setGameMode(GameMode.SPECTATOR);

                    Vector viewDirection = cameraDirections.get(cameraName);
                    if (viewDirection != null) {
                        cameraLocation.setDirection(viewDirection);
                    }

                    player.teleport(cameraLocation);
                    sendActionBar(player, "Du wurdest zur Kamera " + ChatColor.AQUA + cameraName + ChatColor.WHITE + " teleportiert. Drücke Shift, um zurückzukehren.");
                    player.closeInventory();

                    // Kamera Liste und Index für den Spieler speichern
                    List<String> camerasInRegion = new ArrayList<>();
                    for (Map.Entry<String, String> entry : cameraRegions.entrySet()) {
                        if (entry.getValue().equalsIgnoreCase(event.getView().getTitle().replace("Kameras in ", ""))) {
                            camerasInRegion.add(entry.getKey());
                        }
                    }
                    playerCameras.put(player.getUniqueId(), camerasInRegion);
                    playerCameraIndex.put(player.getUniqueId(), camerasInRegion.indexOf(cameraName));
                } else {
                    player.sendMessage("Diese Kamera existiert nicht.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (inCameraMode.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (inCameraMode.getOrDefault(player.getUniqueId(), false) && player.isSneaking()) {
            Location previousLocation = previousLocations.get(player.getUniqueId());
            if (previousLocation != null) {
                player.teleport(previousLocation);
                player.setGameMode(previousGameModes.get(player.getUniqueId()));
            }
            inCameraMode.remove(player.getUniqueId());
            previousLocations.remove(player.getUniqueId());
            previousGameModes.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (inCameraMode.getOrDefault(player.getUniqueId(), false)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                switchToNextCamera(player);
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                switchToPreviousCamera(player);
            }
        }
    }

    private void switchToPreviousCamera(Player player) {
        UUID playerId = player.getUniqueId();
        int currentIndex = playerCameraIndex.get(playerId);
        List<String> camerasInRegion = playerCameras.get(playerId);

        if (currentIndex > 0) {
            currentIndex--;
        } else {
            currentIndex = camerasInRegion.size() - 1; // wrap around to the last camera
        }

        String cameraName = camerasInRegion.get(currentIndex);
        Location cameraLocation = cameras.get(cameraName);
        Vector viewDirection = cameraDirections.get(cameraName);
        if (viewDirection != null) {
            cameraLocation.setDirection(viewDirection);
        }

        player.teleport(cameraLocation);
        sendActionBar(player, "Du wurdest zur Kamera " + ChatColor.AQUA + cameraName + ChatColor.WHITE + " teleportiert. Drücke Shift, um zurückzukehren.");
        playerCameraIndex.put(playerId, currentIndex);
    }

    private void switchToNextCamera(Player player) {
        UUID playerId = player.getUniqueId();
        int currentIndex = playerCameraIndex.get(playerId);
        List<String> camerasInRegion = playerCameras.get(playerId);

        if (currentIndex < camerasInRegion.size() - 1) {
            currentIndex++;
        } else {
            currentIndex = 0; // wrap around to the first camera
        }

        String cameraName = camerasInRegion.get(currentIndex);
        Location cameraLocation = cameras.get(cameraName);
        Vector viewDirection = cameraDirections.get(cameraName);
        if (viewDirection != null) {
            cameraLocation.setDirection(viewDirection);
        }

        player.teleport(cameraLocation);
        sendActionBar(player, "Du wurdest zur Kamera " + ChatColor.AQUA + cameraName + ChatColor.WHITE + " teleportiert.");
        playerCameraIndex.put(playerId, currentIndex);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("camera")) {
            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("create", "delete", "menu", "reload", "save", "load");
                for (String subCommand : subCommands) {
                    if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("delete")) {
                    for (String name : cameras.keySet()) {
                        if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(name);
                        }
                    }
                    for (String regionName : new HashSet<>(cameraRegions.values())) {
                        if (regionName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(regionName);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("menu")) {
                    Set<String> regions = new HashSet<>(cameraRegions.values());
                    for (String regionName : regions) {
                        if (regionName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(regionName);
                        }
                    }
                }
            }
        }
        return completions;
    }

    private void saveCamerasToConfig() {
        FileConfiguration config = getConfig();
        config.set("cameras", null); // Clear existing cameras section

        for (Map.Entry<String, Location> entry : cameras.entrySet()) {
            String cameraName = entry.getKey();
            Location location = entry.getValue();
            String path = "cameras." + cameraName;

            config.set(path + ".world", location.getWorld().getName());
            config.set(path + ".x", location.getX());
            config.set(path + ".y", location.getY());
            config.set(path + ".z", location.getZ());
            config.set(path + ".region", cameraRegions.get(cameraName));
            config.set(path + ".direction", cameraDirections.get(cameraName));
        }

        saveConfig();
    }

    private void loadCamerasFromConfig() {
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection("cameras")) return;

        for (String cameraName : config.getConfigurationSection("cameras").getKeys(false)) {
            String path = "cameras." + cameraName;
            String worldName = config.getString(path + ".world");
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            String regionName = config.getString(path + ".region");
            Vector direction = config.getVector(path + ".direction");

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
            cameras.put(cameraName, location);
            cameraRegions.put(cameraName, regionName);
            cameraDirections.put(cameraName, direction);
        }
    }

    public void givePlayerPotionEffect(Player player, PotionEffectType effectType, int duration, int intensity, boolean hidepotioneffect) {
        PotionEffect effect = new PotionEffect(effectType, duration, intensity, hidepotioneffect);
        player.addPotionEffect(effect);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
