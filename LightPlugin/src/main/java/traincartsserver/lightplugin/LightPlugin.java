package traincartsserver.lightplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class LightPlugin extends JavaPlugin {
    private LightGroupManager groupManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        groupManager = new LightGroupManager(this);
        LightCommandExecutor commandExecutor = new LightCommandExecutor(this, groupManager);
        getCommand("light").setExecutor(commandExecutor);
        getCommand("light").setTabCompleter(new LightTabCompleter(groupManager));
    }

    @Override
    public void onDisable() {
        groupManager.saveGroups();
    }

    public LightGroupManager getGroupManager() {
        return groupManager;
    }
}
