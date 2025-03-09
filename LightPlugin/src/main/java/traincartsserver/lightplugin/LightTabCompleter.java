package traincartsserver.lightplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LightTabCompleter implements TabCompleter {
    private final LightGroupManager groupManager;

    public LightTabCompleter(LightGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public List<String> onTabComplete( CommandSender sender, Command command,  String alias,  String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "add", "remove", "level");
        }
        if (args.length == 2) {
            Set<String> groupNames = groupManager.getGroupNames();
            return new ArrayList<>(groupNames);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("level")) {
            return Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
        }
        return null;
    }
}
