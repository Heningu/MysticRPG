/*package eu.xaru.mysticrpg.commands;

import eu.xaru.mysticrpg.customs.crafting.RecipeManager;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CustomRecipeCommand implements CommandExecutor {

    private final RecipeManager recipeManager;
    private final PlayerDataManager playerDataManager;

    public CustomRecipeCommand(RecipeManager recipeManager, PlayerDataManager playerDataManager) {
        this.recipeManager = recipeManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /customrecipe <list|check|unlock|lock> [args]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                handleList(sender);
                break;
            case "check":
                if (args.length == 2) {
                    handleCheck(sender, args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /customrecipe check <playername>");
                }
                break;
            case "unlock":
            case "lock":
                if (args.length == 3) {
                    handleToggle(sender, args[0], args[1], args[2]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /customrecipe " + args[0] + " <playername> <recipeid>");
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Usage: /customrecipe <list|check|unlock|lock> [args]");
                break;
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        List<String> recipeList = recipeManager.getAllRecipes();
        sender.sendMessage(ChatColor.GREEN + "Available Recipes:");
        for (String recipe : recipeList) {
            sender.sendMessage(ChatColor.YELLOW + "- " + recipe);
        }
    }

    private void handleCheck(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        PlayerData playerData = playerDataManager.getPlayerData(player);
        List<String> recipeList = recipeManager.getAllRecipes();

        sender.sendMessage(ChatColor.GREEN + "Recipes for " + playerName + ":");
        for (String recipeID : recipeList) {
            boolean isUnlocked = playerData.isRecipeUnlocked(recipeID);
            String status = isUnlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked";
            sender.sendMessage(ChatColor.YELLOW + "- " + recipeID + ": " + status);
        }
    }

    private void handleToggle(CommandSender sender, String action, String playerName, String recipeID) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        PlayerData playerData = playerDataManager.getPlayerData(player);

        if (!recipeManager.isRecipeValid(recipeID)) {
            sender.sendMessage(ChatColor.RED + "Invalid recipe ID.");
            return;
        }

        boolean unlock = action.equalsIgnoreCase("unlock");
        playerData.setRecipeUnlocked(recipeID, unlock);
        playerData.save();

        String status = unlock ? "Unlocked" : "Locked";
        sender.sendMessage(ChatColor.GREEN + "Recipe " + recipeID + " for player " + playerName + " is now " + status + ".");
    }
}*/
