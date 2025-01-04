package eu.xaru.mysticrpg.npc.customnpc;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.npc.customnpc.dialogues.DialogueManager;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * CustomNPCModule loads the NPC data from "customnpcs" files, handles
 * commands for creating/editing NPCs, registers events for interaction,
 * and now includes commands to reload models from config.
 */
public class CustomNPCModule implements IBaseModule {

    private final JavaPlugin plugin;
    private final CustomNPCManager npcManager;
    private EventManager eventManager;

    public CustomNPCModule() {
        this.plugin = JavaPlugin.getPlugin(MysticCore.class);
        this.npcManager = new CustomNPCManager();
    }

    @Override
    public void initialize() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        if (saveModule == null) {
            throw new IllegalStateException("SaveModule not initialized.");
        }
        eventManager = new EventManager(plugin);
        registerCommands();

        DebugLogger.getInstance().log(Level.INFO, "CustomNPCModule initialized successfully.", 0);
    }

    @Override
    public void start() {
        // 1) Load existing NPC data from disk (but do not spawn them here)
        npcManager.loadAllFromDisk();

        // 2) Load dialogues from folder
        File dialoguesFolder = new File(plugin.getDataFolder(), "customnpcs/dialogues");
        DialogueManager.getInstance().loadAllDialogues(dialoguesFolder);

        // 3) Register event handlers
        registerEventHandlers();

        DebugLogger.getInstance().log(Level.INFO, "CustomNPCModule started", 0);
        Bukkit.getScheduler().runTaskLater(plugin, this::reloadAllModels, 120L);
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    /**
     * This is where we define all subcommands (via CommandAPI).
     *
     * We add two new subcommands:
     * - /xarunpc reloadmodels
     * - /xarunpc reloadmodel <npcId>
     */
    private void registerCommands() {
        new CommandAPICommand("xarunpc")
                // /xarunpc create <npcId> <npcName> <modelId>
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("npcId"))
                        .withArguments(new StringArgument("npcName"))
                        .withArguments(new StringArgument("modelId"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("npcId");
                            String npcName = (String) args.get("npcName");
                            String modelId = (String) args.get("modelId");

                            Location loc = player.getLocation();
                            CustomNPC npc = npcManager.createNPC(id, npcName, loc, modelId);
                            player.sendMessage(Utils.getInstance().$(
                                    "Created custom NPC '" + npcName + "' with ID " + id));
                        })
                )
                // /xarunpc delete <npcId>
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("npcId"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("npcId");
                            boolean success = npcManager.deleteNPC(id);
                            if (success) {
                                player.sendMessage(Utils.getInstance().$(
                                        "Deleted custom NPC with ID " + id));
                            } else {
                                player.sendMessage(Utils.getInstance().$(
                                        "No custom NPC found with ID " + id));
                            }
                        })
                )
                // /xarunpc list
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            if (npcManager.getAllNPCs().isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No CustomNPCs found."));
                                return;
                            }
                            player.sendMessage(Utils.getInstance().$("CustomNPCs:"));
                            for (CustomNPC npc : npcManager.getAllNPCs()) {
                                String wName = "null";
                                if (npc.getLocation() != null && npc.getLocation().getWorld() != null) {
                                    wName = npc.getLocation().getWorld().getName();
                                }
                                player.sendMessage(Utils.getInstance().$(
                                        " - ID=" + npc.getId() +
                                                ", Name=" + npc.getName() +
                                                ", Model=" + (npc.getModelId() == null ? "none" : npc.getModelId()) +
                                                ", World=" + wName +
                                                (npc.getLocation() != null
                                                        ? " [" + npc.getLocation().getBlockX() + ","
                                                        + npc.getLocation().getBlockY() + ","
                                                        + npc.getLocation().getBlockZ() + "]"
                                                        : "")
                                ));
                            }
                        })
                )
                // /xarunpc dialogue add <npcId> <dialogueId>
                .withSubcommand(new CommandAPICommand("dialogue")
                        .withSubcommand(new CommandAPICommand("add")
                                .withArguments(new StringArgument("npcId"))
                                .withArguments(new StringArgument("dialogueId"))
                                .executesPlayer((player, args) -> {
                                    String npcId = (String) args.get("npcId");
                                    String dialogueId = (String) args.get("dialogueId");

                                    CustomNPC npc = npcManager.getNPC(npcId);
                                    if (npc == null) {
                                        player.sendMessage(Utils.getInstance().$(
                                                "No NPC found with ID: " + npcId));
                                        return;
                                    }
                                    npc.addDialogue(dialogueId);
                                    player.sendMessage(Utils.getInstance().$(
                                            "Dialogue '" + dialogueId + "' was added to NPC '" + npcId + "'."
                                    ));
                                })
                        )
                )

                /*
                 * NEW SUBCOMMAND #1: /xarunpc reloadmodels
                 * Reloads the model from config for ALL NPCs in memory
                 */
                .withSubcommand(new CommandAPICommand("reloadmodels")
                        .executesPlayer((player, args) -> {
                            if (npcManager.getAllNPCs().isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No CustomNPCs found to reload."));
                                return;
                            }

                            // For each NPC, despawn & spawn => reattach the model from config
                            for (CustomNPC npc : npcManager.getAllNPCs()) {
                                npc.despawn();
                                npc.spawn();
                            }
                            player.sendMessage(Utils.getInstance().$("All NPC models have been reloaded!"));
                        })
                )

                /*
                 * NEW SUBCOMMAND #2: /xarunpc reloadmodel <npcId>
                 * Reloads the model from config for ONE specific NPC
                 */
                .withSubcommand(new CommandAPICommand("reloadmodel")
                        .withArguments(new StringArgument("npcId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    // Provide suggestions of existing NPC IDs
                                    return npcManager.getAllNPCs().stream()
                                            .map(CustomNPC::getId)
                                            .toArray(String[]::new);
                                }))
                        )
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npcId");
                            CustomNPC npc = npcManager.getNPC(npcId);
                            if (npc == null) {
                                player.sendMessage(Utils.getInstance().$(
                                        "No NPC found with ID: " + npcId));
                                return;
                            }

                            npc.despawn();
                            npc.spawn();
                            player.sendMessage(Utils.getInstance().$("NPC '" + npcId + "' model has been reloaded!"));
                        })
                )

                .register();

        // /xarudialogue <response> <dialogueId>
        new CommandAPICommand("xarudialogue")
                .withArguments(new StringArgument("response")
                        .replaceSuggestions(ArgumentSuggestions.strings("yes","no")))
                .withArguments(new StringArgument("dialogueId"))
                .executesPlayer((player, args) -> {
                    String response = (String) args.get("response");
                    String dialogueId = (String) args.get("dialogueId");
                    DialogueManager.getInstance().handleResponse(player, response, dialogueId);
                })
                .register();
    }

    public void reloadAllModels() {
        if (npcManager.getAllNPCs().isEmpty()) {
            return;
        }
        DebugLogger.getInstance().log(Level.INFO, "Tackling ModelEngine Errors", 0);

        // For each NPC, despawn & spawn => reattach the model from config
        for (CustomNPC npc : npcManager.getAllNPCs()) {
            npc.despawn();
            npc.spawn();

        }
    }



    /**
     * Registers the event that handles right-click interaction on NPC stands.
     */
    private void registerEventHandlers() {
        eventManager.registerEvent(PlayerInteractAtEntityEvent.class, event -> {
            if (!(event.getRightClicked() instanceof ArmorStand stand)) {
                return;
            }

            for (String tag : stand.getScoreboardTags()) {
                if (tag.startsWith("XaruLinkedEntity_")) {
                    String suffix = tag.substring("XaruLinkedEntity_".length());
                    if (suffix.endsWith("_model")) {
                        suffix = suffix.substring(0, suffix.length() - "_model".length());
                    } else if (suffix.endsWith("_name")) {
                        suffix = suffix.substring(0, suffix.length() - "_name".length());
                    }

                    if (suffix.startsWith("NPC_")) {
                        String realNpcId = suffix.substring("NPC_".length());
                        CustomNPC npc = npcManager.getNPC(realNpcId);
                        if (npc != null) {
                            event.setCancelled(true);
                            npc.interact(event.getPlayer());
                        }
                    }
                    break;
                }
            }
        });
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(SaveModule.class);
    }

    @Override
    public eu.xaru.mysticrpg.enums.EModulePriority getPriority() {
        return eu.xaru.mysticrpg.enums.EModulePriority.LOW;
    }
}
