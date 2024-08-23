//package eu.xaru.mysticrpg.economy;
//
//import dev.jorel.commandapi.CommandAPICommand;
//import dev.jorel.commandapi.arguments.TextArgument;
//import eu.xaru.mysticrpg.enums.EModulePriority;
//import eu.xaru.mysticrpg.interfaces.IBaseModule;
//import eu.xaru.mysticrpg.storage.PlayerDataManager;
//import eu.xaru.mysticrpg.ui.CustomScoreboardManager;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//
//import java.util.List;
//
//public class EconomyModule implements IBaseModule {
//
//   // private final EconomyHelper economyHelper;
//    private final JavaPlugin plugin;
//
//    public EconomyModule(JavaPlugin plugin) {
//        this.plugin = plugin;
//      //  this.economyHelper = new EconomyHelper(new PlayerDataManager(), new CustomScoreboardManager());
//    }
//
//    @Override
//    public void initialize() {
//        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule initialized");
//    }
//
//    @Override
//    public void start() {
//       // registerCommands();
//        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule started");
//    }
//
//    @Override
//    public void stop() {
//        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule stopped");
//    }
//
//    @Override
//    public void unload() {
//        Bukkit.getLogger().info(ChatColor.GREEN + "EconomyModule unloaded");
//    }
//
//    @Override
//    public List<Class<? extends IBaseModule>> getDependencies() {
//        return List.of();  // No dependencies as it can load independently
//    }
//
//    @Override
//    public EModulePriority getPriority() {
//        return EModulePriority.NORMAL;  // This can be adjusted based on when you want the module to load
//    }
//
//    private void registerCommands() {
//        new CommandAPICommand("economy")
//                .withSubcommands(
//                        new CommandAPICommand("balance")
//                                .executesPlayer((player, args) -> {
//                                    double balance = economyHelper.getBalance(player);
//                                    player.sendMessage("Your balance: $" + economyHelper.formatBalance(balance));
//                                }),
//                        new CommandAPICommand("send")
//                                .withArguments(new TextArgument("target"), new TextArgument("amount"))
//                                .executesPlayer((player, args) -> {
//                                    String targetName = (String) args[0];
//                                    Player target = Bukkit.getPlayer(targetName);
//                                    if (target != null) {
//                                        try {
//                                            double amount = Double.parseDouble((String) args[1]);
//                                            economyHelper.sendMoney(player, target, amount);
//                                            player.sendMessage("You sent $" + amount + " to " + target.getName());
//                                            target.sendMessage("You received $" + amount + " from " + player.getName());
//                                        } catch (NumberFormatException e) {
//                                            player.sendMessage("Invalid amount.");
//                                        }
//                                    } else {
//                                        player.sendMessage("Player not found.");
//                                    }
//                                })
//                )
//                .register();
//    }
//}
