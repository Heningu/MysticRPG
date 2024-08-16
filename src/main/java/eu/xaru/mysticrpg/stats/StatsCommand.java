package eu.xaru.mysticrpg.stats;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;
    private final StatMenu statMenu;

    public StatsCommand(MysticCore plugin, PlayerDataManager playerDataManager, StatMenu statMenu) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.statMenu = statMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                statMenu.openStatMenu(player);
                return true;
            } else {
                sender.sendMessage("This command can only be used by players.");
                return false;
            }
        }

        if (!sender.hasPermission("mysticrpg.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /stats <reset|increase> <PLAYER> [ATTRIBUTE]");
            return false;
        }

        String action = args[0];
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage("Player not found.");
            return false;
        }

        PlayerData data = playerDataManager.getPlayerData(target);

        switch (action.toLowerCase()) {
            case "reset":
                resetStats(data);
                sender.sendMessage("Player " + playerName + "'s stats have been reset.");
                break;

            case "increase":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /stats increase <PLAYER> <ATTRIBUTE>");
                    return false;
                }

                String attribute = args[2].toLowerCase();
                boolean increased = increaseAttribute(data, attribute);
                if (increased) {
                    sender.sendMessage("Increased " + playerName + "'s " + attribute + " by 1.");
                } else {
                    sender.sendMessage("Failed to increase " + attribute + " for " + playerName + ".");
                }
                break;

            default:
                sender.sendMessage("Invalid action. Use 'reset' or 'increase'.");
                return false;
        }

        playerDataManager.save(target);
        return true;
    }

    private void resetStats(PlayerData data) {
        data.setVitality(1);
        data.setIntelligence(1);
        data.setDexterity(1);
        data.setStrength(1);
        data.setHp(20);  // Reset to base HP
        data.setMana(20);  // Reset to base Mana
        data.setAttackDamage(1);  // Reset base attack damage
        data.setAttackDamageDex(1);  // Reset base dexterity attack damage
        data.setAttributePoints(10);  // Reset attribute points to a default value
    }

    private boolean increaseAttribute(PlayerData data, String attribute) {
        if (data.getAttributePoints() <= 0) return false;

        switch (attribute) {
            case "vitality":
                data.setVitality(data.getVitality() + 1);
                data.setHp(data.getHp() + 2);
                break;
            case "intelligence":
                data.setIntelligence(data.getIntelligence() + 1);
                data.setMana(data.getMana() + 2);
                break;
            case "dexterity":
                data.setDexterity(data.getDexterity() + 1);
                data.setAttackDamageDex(data.getAttackDamageDex() + 1);
                break;
            case "strength":
                data.setStrength(data.getStrength() + 1);
                data.setAttackDamage(data.getAttackDamage() + 1);
                break;
            default:
                return false;
        }

        data.setAttributePoints(data.getAttributePoints() - 1);
        return true;
    }
}
