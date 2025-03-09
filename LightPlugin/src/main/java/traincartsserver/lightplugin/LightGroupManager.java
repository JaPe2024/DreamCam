package traincartsserver.lightplugin;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LightGroupManager {
    private final LightPlugin plugin;
    private final Map<String, LightGroup> groups;

    public LightGroupManager(LightPlugin plugin) {
        this.plugin = plugin;
        this.groups = new HashMap<>();
        loadGroups();
    }

    public void addGroup(LightGroup group) {
        groups.put(group.getName(), group);
    }

    public boolean groupExists(String name) {
        return groups.containsKey(name);
    }

    public LightGroup getGroup(String name) {
        return groups.get(name);
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public void saveGroups() {
        FileConfiguration config = plugin.getConfig();
        config.set("lightGroups", null); // Clear existing data
        for (LightGroup group : groups.values()) {
            config.set("lightGroups." + group.getName(), group.getLightLocations().toArray(new Location[0]));
        }
        plugin.saveConfig();
    }

    public void loadGroups() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("lightGroups")) {
            for (String groupName : config.getConfigurationSection("lightGroups").getKeys(false)) {
                LightGroup group = new LightGroup(groupName, plugin);
                List<?> list = config.getList("lightGroups." + groupName);
                if (list != null) {
                    for (Object loc : list) {
                        if (loc instanceof Location) {
                            group.addLight((Location) loc);
                        }
                    }
                }
                groups.put(groupName, group);
            }
        }
    }
}
