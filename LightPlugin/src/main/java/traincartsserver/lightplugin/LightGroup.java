package traincartsserver.lightplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class LightGroup {
    private final String name;
    private final Set<Location> lightLocations;
    private final LightPlugin plugin;

    public LightGroup(String name, LightPlugin plugin) {
        this.name = name;
        this.plugin = plugin;
        this.lightLocations = new HashSet<>();
    }

    public void addLight(Location location) {
        lightLocations.add(location);
    }

    public void removeLight(Location location) {
        lightLocations.remove(location);
    }

    public void removeAllLights() {
        lightLocations.clear();
    }

    public void setLevel(int level, float speed) {
        if (speed == 0.0f) {
            setImmediateLevel(level);
        } else {
            fadeLevel(level, speed);
        }
    }

    private void setImmediateLevel(int level) {
        for (Location location : lightLocations) {
            Block block = location.getBlock();
            if (block.getType() == Material.LIGHT) {
                block.setBlockData(Bukkit.createBlockData(Material.LIGHT, "[level=" + level + "]"));
            }
        }
    }

    private void fadeLevel(int targetLevel, float speed) {
        for (Location location : lightLocations) {
            Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int currentLevel = location.getBlock().getBlockData().getAsString().contains("level") ? Integer.parseInt(location.getBlock().getBlockData().getAsString().split("=")[1].replaceAll("[^\\d]", "")) : 0;

                @Override
                public void run() {
                    if (currentLevel < targetLevel) {
                        currentLevel++;
                    } else if (currentLevel > targetLevel) {
                        currentLevel--;
                    }
                    Block block = location.getBlock();
                    if (block.getType() == Material.LIGHT) {
                        block.setBlockData(Bukkit.createBlockData(Material.LIGHT, "[level=" + currentLevel + "]"));
                    }
                    if (currentLevel == targetLevel) {
                        cancel();
                    }
                }

                private void cancel() {
                    Bukkit.getScheduler().cancelTask(this.hashCode());
                }
            }, 0L, (long) (20L * speed));
        }
    }

    public String getName() {
        return name;
    }

    public Set<Location> getLightLocations() {
        return lightLocations;
    }
}
