//package eu.xaru.mysticrpg.npc;
//
//import com.github.juliarn.npclib.api.Platform;
//import dev.jorel.commandapi.CommandAPICommand;
//import dev.jorel.commandapi.arguments.StringArgument;
//import eu.xaru.mysticrpg.cores.MysticCore;
//import eu.xaru.mysticrpg.enums.EModulePriority;
//import eu.xaru.mysticrpg.interfaces.IBaseModule;
//import eu.xaru.mysticrpg.managers.EventManager;
//import eu.xaru.mysticrpg.managers.ModuleManager;
//import eu.xaru.mysticrpg.utils.DebugLoggerModule;
//import org.bukkit.ChatColor;
//import org.bukkit.plugin.java.JavaPlugin;
//
//import org.bukkit.World;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.plugin.Plugin;
//
//
//
//
//import java.util.List;
//import java.util.logging.Level;
//
//public class NPCModule implements IBaseModule {
//
//    private final JavaPlugin plugin;
//    private DebugLoggerModule logger;
//    private NPCManager npcManager;
//    private EventManager eventManager;
//
//    public NPCModule() {
//        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
//    }
//
//    @Override
//    public void initialize() {
//        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
//
//        // Get the NPC-Lib platform from MysticCore
//        Platform<World, Player, ItemStack, Plugin> npcPlatform = ((MysticCore) plugin).getNpcPlatform();
//
//        npcManager = new NPCManager(plugin, npcPlatform);
//
//        eventManager = new EventManager(plugin);
//
//        registerCommands();
//        registerEventHandlers();
//
//        logger.log(Level.INFO, "NPCModule initialized successfully.", 0);
//    }
//
//    @Override
//    public void start() {
//        npcManager.loadNPCs();
//        logger.log(Level.INFO, "NPCModule started", 0);
//    }
//
//    @Override
//    public void stop() {
//        npcManager.saveNPCs();
//        logger.log(Level.INFO, "NPCModule stopped", 0);
//    }
//
//    @Override
//    public void unload() {
//        logger.log(Level.INFO, "NPCModule unloaded", 0);
//    }
//
//    @Override
//    public List<Class<? extends IBaseModule>> getDependencies() {
//        return List.of(DebugLoggerModule.class);
//    }
//
//    @Override
//    public EModulePriority getPriority() {
//        return EModulePriority.LOW;
//    }
//
//    private void registerCommands() {
//        new CommandAPICommand("npc")
//                .withSubcommand(new CommandAPICommand("spawn")
//                        .withArguments(new StringArgument("name"))
//                        .executesPlayer((player, args) -> {
//                            String name = (String) args.get("name");
//                            npcManager.createNPC(player.getLocation(), name);
//                            player.sendMessage(ChatColor.GREEN + "NPC '" + name + "' spawned.");
//                        }))
//                .withSubcommand(new CommandAPICommand("delete")
//                        .withArguments(new StringArgument("name"))
//                        .executesPlayer((player, args) -> {
//                            String name = (String) args.get("name");
//                            boolean success = npcManager.deleteNPC(name);
//                            if (success) {
//                                player.sendMessage(ChatColor.GREEN + "NPC '" + name + "' deleted.");
//                            } else {
//                                player.sendMessage(ChatColor.RED + "NPC '" + name + "' not found.");
//                            }
//                        }))
//                .withSubcommand(new CommandAPICommand("setbehavior")
//                        .withArguments(new StringArgument("name"), new StringArgument("behavior"))
//                        .executesPlayer((player, args) -> {
//                            String name = (String) args.get("name");
//                            String behavior = (String) args.get("behavior");
//                            boolean success = npcManager.setNPCBehavior(name, behavior);
//                            if (success) {
//                                player.sendMessage(ChatColor.GREEN + "NPC '" + name + "' behavior set to '" + behavior + "'.");
//                            } else {
//                                player.sendMessage(ChatColor.RED + "NPC '" + name + "' not found or behavior invalid.");
//                            }
//                        }))
//                .withSubcommand(new CommandAPICommand("changeskin")
//                        .withArguments(new StringArgument("name"), new StringArgument("skinData"))
//                        .executesPlayer((player, args) -> {
//                            String name = (String) args.get("name");
//                            String skinData = (String) args.get("skinData");
//                            boolean success = npcManager.changeNPCSkin(name, skinData);
//                            if (success) {
//                                player.sendMessage(ChatColor.GREEN + "NPC '" + name + "' skin changed.");
//                            } else {
//                                player.sendMessage(ChatColor.RED + "NPC '" + name + "' not found.");
//                            }
//                        }))
//                .register();
//
//        // Register the /npcdialogue command to handle player responses
//        new CommandAPICommand("npcdialogue")
//                .withArguments(new StringArgument("response"), new StringArgument("npcName"), new StringArgument("dialogueId"))
//                .executesPlayer((player, args) -> {
//                    String response = (String) args.get("response");
//                    String npcName = (String) args.get("npcName");
//                    String dialogueId = (String) args.get("dialogueId");
//                    NPC npc = npcManager.getNPC(npcName);
//                    if (npc != null) {
//                        Dialogue dialogue = npc.getDialogueManager().getDialogueById(dialogueId);
//                        if (dialogue != null) {
//                            dialogue.handleResponse(player, response);
//                        } else {
//                            player.sendMessage(ChatColor.RED + "Dialogue not found.");
//                        }
//                    } else {
//                        player.sendMessage(ChatColor.RED + "NPC not found.");
//                    }
//                })
//                .register();
//    }
//
//    private void registerEventHandlers() {
//        // Register any necessary event handlers
//    }
//
//    public NPCManager getNpcManager() {
//        return npcManager;
//    }
//}
