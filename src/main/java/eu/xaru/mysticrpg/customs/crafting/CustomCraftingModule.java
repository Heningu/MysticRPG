package eu.xaru.mysticrpg.customs.crafting;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class CustomCraftingModule implements IBaseModule {

    private final JavaPlugin plugin;
    
    private CraftingHelper craftingHelper;

    public CustomCraftingModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
    }

    @Override
    public void initialize() {


        craftingHelper = new CraftingHelper(plugin);

        // Disable default crafting
        Bukkit.getPluginManager().registerEvents(new CraftingDisabler(), plugin);

        // Register commands
        registerCommands();

    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void registerCommands() {
        // /crafting command to open the crafting GUI
        new CommandAPICommand("crafting")
                .withPermission("mysticrpg.crafting")
                .executesPlayer((player, args) -> {
                    craftingHelper.openCraftingGUI(player);
                })
                .register();

        // /customrecipe command with subcommands
        new CommandAPICommand("customrecipe")
                .withPermission("mysticrpg.customrecipe")
                .withSubcommand(new CommandAPICommand("unlock")
                        .withArguments(new StringArgument("recipeId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> craftingHelper.getRecipeIds().toArray(new String[0]))))
                        .withOptionalArguments(new PlayerArgument("player")
                                .replaceSuggestions(ArgumentSuggestions.empty())) // No suggestions needed for player
                        .executes((sender, args) -> {
                            String recipeId = (String) args.get("recipeId");
                            Player target;
                            if (args.get("player") != null) {
                                target = (Player) args.get("player");
                            } else if (sender instanceof Player) {
                                target = (Player) sender;
                            } else {
                                sender.sendMessage(Utils.getInstance().$(ChatColor.RED + "You must specify a player when running this command from the console."));
                                return;
                            }
                            if (craftingHelper.unlockRecipe(target, recipeId)) {
                                sender.sendMessage(Utils.getInstance().$("Recipe " + recipeId + " unlocked for " + target.getName()));
                            } else {
                                sender.sendMessage(Utils.getInstance().$(ChatColor.RED + "Failed to unlock recipe " + recipeId));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("lock")
                        .withArguments(new StringArgument("recipeId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> craftingHelper.getRecipeIds().toArray(new String[0]))))
                        .withOptionalArguments(new PlayerArgument("player")
                                .replaceSuggestions(ArgumentSuggestions.empty()))
                        .executes((sender, args) -> {
                            String recipeId = (String) args.get("recipeId");
                            Player target;
                            if (args.get("player") != null) {
                                target = (Player) args.get("player");
                            } else if (sender instanceof Player) {
                                target = (Player) sender;
                            } else {
                                sender.sendMessage(Utils.getInstance().$(ChatColor.RED + "You must specify a player when running this command from the console."));
                                return;
                            }
                            if (craftingHelper.lockRecipe(target, recipeId)) {
                                sender.sendMessage(Utils.getInstance().$("Recipe " + recipeId + " locked for " + target.getName()));
                            } else {
                                sender.sendMessage(Utils.getInstance().$(ChatColor.RED + "Failed to lock recipe " + recipeId));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            craftingHelper.loadCustomRecipes(); // Reload custom recipes
                            sender.sendMessage(Utils.getInstance().$("Custom recipes reloaded successfully."));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            sender.sendMessage(Utils.getInstance().$("Available Custom Recipes:"));
                            for (String recipeId : craftingHelper.getRecipeIds()) {
                                sender.sendMessage(Utils.getInstance().$("- " + recipeId));
                            }
                        }))
                .register();
    }
}
