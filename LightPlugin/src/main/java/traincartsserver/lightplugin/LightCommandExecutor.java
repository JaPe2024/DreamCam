package traincartsserver.lightplugin;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LightCommandExecutor implements CommandExecutor {
    private final LightPlugin plugin;
    private final LightGroupManager groupManager;

    public LightCommandExecutor(LightPlugin plugin, LightGroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
    }

    @Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            return false;
        }

        String subcommand = args[0];

        if (subcommand.equalsIgnoreCase("create")) {
            if (args.length != 2) {
                player.sendMessage("Usage: /light create <group>");
                return true;
            }
            String groupName = args[1];
            if (groupManager.groupExists(groupName)) {
                player.sendMessage("Group already exists.");
                return true;
            }
            LightGroup group = new LightGroup(groupName, plugin);
            groupManager.addGroup(group);
            player.sendMessage("Light group created: " + groupName);
        } else if (subcommand.equalsIgnoreCase("add")) {
            if (args.length != 2) {
                player.sendMessage("Usage: /light add <group>");
                return true;
            }
            String groupName = args[1];
            if (!groupManager.groupExists(groupName)) {
                player.sendMessage("Group does not exist.");
                return true;
            }
            LightGroup group = groupManager.getGroup(groupName);
            Location location = player.getTargetBlockExact(5).getLocation();
            group.addLight(location);
            groupManager.saveGroups();
            player.sendMessage("Added light to group: " + groupName);
        } else if (subcommand.equalsIgnoreCase("remove")) {
            if (args.length != 2) {
                player.sendMessage("Usage: /light remove <group>");
                return true;
            }
            String groupName = args[1];
            if (!groupManager.groupExists(groupName)) {
                player.sendMessage("Group does not exist.");
                return true;
            }
            LightGroup group = groupManager.getGroup(groupName);
            Location location = player.getTargetBlockExact(5).getLocation();
            group.removeLight(location);
            groupManager.saveGroups();
            player.sendMessage("Removed light from group: " + groupName);
        } else if (subcommand.equalsIgnoreCase("level")) {
            if (args.length != 3 && args.length != 4) {
                player.sendMessage("Usage: /light level <group> <level> [speed]");
                return true;
            }
            String groupName = args[1];
            if (!groupManager.groupExists(groupName)) {
                player.sendMessage("Group does not exist.");
                return true;
            }
            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid level.");
                return true;
            }
            float speed = 0.0f;
            if (args.length == 4) {
                try {
                    speed = Float.parseFloat(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid speed.");
                    return true;
                }
            }
            LightGroup group = groupManager.getGroup(groupName);
            group.setLevel(level, speed);
            player.sendMessage("Set light level for group: " + groupName);
        } else {
            return false;
        }

        return true;
    }
}
