/*package eu.xaru.mysticrpg.customs.mobs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MobGUICommand implements CommandExecutor {

    private final CustomMobCreatorModule customMobCreatorModule;

    public MobGUICommand(CustomMobCreatorModule customMobCreatorModule) {
        this.customMobCreatorModule = customMobCreatorModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            customMobCreatorModule.openMobGUI(player);
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}
*/