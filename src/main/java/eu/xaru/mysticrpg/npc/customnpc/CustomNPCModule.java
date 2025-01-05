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
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

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
        // 1) Load existing NPC data
        npcManager.loadAllFromDisk();

        // 2) Load dialogues from folder
        File dialoguesFolder = new File(plugin.getDataFolder(), "customnpcs/dialogues");
        DialogueManager.getInstance().loadAllDialogues(dialoguesFolder);

        // 3) Register event
        registerEventHandlers();

        DebugLogger.getInstance().log(Level.INFO, "CustomNPCModule started", 0);
    }

    @Override
    public void stop() {
    }

    @Override
    public void unload() {
    }

    private void registerCommands() {
        new CommandAPICommand("xarunpc")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("npcId"))
                        .withArguments(new StringArgument("npcName"))
                        .withArguments(new StringArgument("modelId"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("npcId");

                            // underscores => spaces
                            String rawName = ((String) args.get("npcName")).replace('_',' ');
                            String modelId = (String) args.get("modelId");

                            Location loc = player.getLocation();
                            CustomNPC npc = npcManager.createNPC(id, rawName, loc, modelId);

                            player.sendMessage(Utils.getInstance().$("Created NPC '" + rawName + "' (ID=" + id + ")"));
                        })
                )
                .withSubcommand(new CommandAPICommand("setprefix")
                        .withArguments(new StringArgument("npcId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    return npcManager.getAllNPCs().stream()
                                            .map(CustomNPC::getId).toArray(String[]::new);
                                }))
                        )
                        .withArguments(new StringArgument("prefix"))
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npcId");
                            String prefix = (String) args.get("prefix");

                            CustomNPC npc = npcManager.getNPC(npcId);
                            if (npc == null) {
                                player.sendMessage(Utils.getInstance().$("No NPC found with ID: " + npcId));
                                return;
                            }
                            npc.setPrefix(prefix);
                            player.sendMessage(Utils.getInstance().$("Set prefix for NPC '" + npcId + "' to '" + prefix + "'"));
                        })
                )
                .withSubcommand(new CommandAPICommand("setsuffix")
                        .withArguments(new StringArgument("npcId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    return npcManager.getAllNPCs().stream()
                                            .map(CustomNPC::getId).toArray(String[]::new);
                                }))
                        )
                        .withArguments(new StringArgument("suffix"))
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npcId");
                            String suffix = (String) args.get("suffix");

                            CustomNPC npc = npcManager.getNPC(npcId);
                            if (npc == null) {
                                player.sendMessage(Utils.getInstance().$("No NPC found with ID: " + npcId));
                                return;
                            }
                            npc.setSuffix(suffix);
                            player.sendMessage(Utils.getInstance().$("Set suffix for NPC '" + npcId + "' to '" + suffix + "'"));
                        })
                )
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("npcId"))
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("npcId");
                            boolean success = npcManager.deleteNPC(id);
                            if (success) {
                                player.sendMessage(Utils.getInstance().$("Deleted NPC with ID " + id));
                            } else {
                                player.sendMessage(Utils.getInstance().$("No NPC found with ID " + id));
                            }
                        })
                )
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
                .withSubcommand(new CommandAPICommand("reloadmodels")
                        .executesPlayer((player, args) -> {
                            if (npcManager.getAllNPCs().isEmpty()) {
                                player.sendMessage(Utils.getInstance().$("No CustomNPCs found to reload."));
                                return;
                            }

                            for (CustomNPC npc : npcManager.getAllNPCs()) {
                                eu.xaru.mysticrpg.entityhandling.EntityHandler.getInstance().deleteNPC(npc);
                                eu.xaru.mysticrpg.entityhandling.EntityHandler.getInstance().spawnNPC(npc, true);
                            }
                            player.sendMessage(Utils.getInstance().$("All NPC models have been reloaded!"));
                        })
                )
                .withSubcommand(new CommandAPICommand("reloadmodel")
                        .withArguments(new StringArgument("npcId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    return npcManager.getAllNPCs().stream()
                                            .map(CustomNPC::getId)
                                            .toArray(String[]::new);
                                }))
                        )
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npcId");
                            CustomNPC npc = npcManager.getNPC(npcId);
                            if (npc == null) {
                                player.sendMessage(Utils.getInstance().$("No NPC found with ID: " + npcId));
                                return;
                            }

                            eu.xaru.mysticrpg.entityhandling.EntityHandler.getInstance().deleteNPC(npc);
                            eu.xaru.mysticrpg.entityhandling.EntityHandler.getInstance().spawnNPC(npc, true);

                            player.sendMessage(Utils.getInstance().$("Model reloaded for NPC '" + npcId + "'"));
                        })
                )
                .withSubcommand(new CommandAPICommand("behaviour")
                        .withSubcommand(new CommandAPICommand("rotate")
                                .withArguments(new StringArgument("npcId")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                            return npcManager.getAllNPCs().stream()
                                                    .map(CustomNPC::getId)
                                                    .toArray(String[]::new);
                                        }))
                                )
                                .withArguments(new StringArgument("yaw")
                                        .replaceSuggestions(ArgumentSuggestions.strings("0","90","180","270"))
                                )
                                .executesPlayer((player, args) -> {
                                    String npcId = (String) args.get("npcId");
                                    String yawStr = (String) args.get("yaw");

                                    float yaw;
                                    try {
                                        yaw = Float.parseFloat(yawStr);
                                    } catch (NumberFormatException e) {
                                        player.sendMessage("Invalid yaw value: " + yawStr);
                                        return;
                                    }

                                    CustomNPC npc = npcManager.getNPC(npcId);
                                    if (npc == null) {
                                        player.sendMessage("No NPC found with ID " + npcId);
                                        return;
                                    }

                                    boolean success = eu.xaru.mysticrpg.entityhandling.EntityHandler
                                            .getInstance().rotateNPC(npc, yaw);

                                    if (success) {
                                        player.sendMessage("Rotated NPC '" + npcId + "' to yaw=" + yaw);
                                    } else {
                                        player.sendMessage("Entity not found in memory for NPC '" + npcId + "'");
                                    }
                                })
                        )
                        .withSubcommand(new CommandAPICommand("lookclosest")
                                .withArguments(new StringArgument("npcId")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                            return npcManager.getAllNPCs().stream()
                                                    .map(CustomNPC::getId)
                                                    .toArray(String[]::new);
                                        }))
                                )
                                .executesPlayer((player, args) -> {
                                    String npcId = (String) args.get("npcId");
                                    CustomNPC npc = npcManager.getNPC(npcId);
                                    if (npc == null) {
                                        player.sendMessage("No NPC found with ID " + npcId);
                                        return;
                                    }

                                    Player closest = null;
                                    double closestDist = Double.MAX_VALUE;
                                    for (Player p : player.getWorld().getPlayers()) {
                                        double dist = p.getLocation().distance(npc.getLocation());
                                        if (dist < closestDist) {
                                            closestDist = dist;
                                            closest = p;
                                        }
                                    }
                                    if (closest == null) {
                                        player.sendMessage("No players nearby to look at!");
                                        return;
                                    }

                                    boolean success = eu.xaru.mysticrpg.entityhandling.EntityHandler
                                            .getInstance().lookAtLocation(npc, closest.getLocation());

                                    if (success) {
                                        player.sendMessage("NPC '" + npcId + "' is now looking at " + closest.getName());
                                    } else {
                                        player.sendMessage("Entity not found or invalid location for NPC '" + npcId + "'");
                                    }
                                })
                        )
                )
                .withSubcommand(new CommandAPICommand("adddialogue")
                        .withArguments(new StringArgument("npcId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    return npcManager.getAllNPCs().stream()
                                            .map(CustomNPC::getId)
                                            .toArray(String[]::new);
                                }))
                        )
                        .withArguments(new StringArgument("dialogueId")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    if (!DialogueManager.getInstance().hasDialogues()) {
                                        return new String[]{"no_dialogues_loaded"};
                                    } else {
                                        return DialogueManager.getInstance().getAllDialogueIds();
                                    }
                                }))
                        )
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npcId");
                            String dialogueId = (String) args.get("dialogueId");

                            CustomNPC npc = npcManager.getNPC(npcId);
                            if (npc == null) {
                                player.sendMessage(Utils.getInstance().$("No NPC found with ID: " + npcId));
                                return;
                            }
                            npc.addDialogue(dialogueId);
                            player.sendMessage(Utils.getInstance().$("Dialogue '" + dialogueId + "' was added to NPC '" + npcId + "'." ));
                        })
                )
                .register();

        // /xarudialogue <yes/no> <dialogueId> <npcId>
        new CommandAPICommand("xarudialogue")
                .withArguments(new StringArgument("response")
                        .replaceSuggestions(ArgumentSuggestions.strings("yes","no")))
                .withArguments(new StringArgument("dialogueId"))
                .withArguments(new StringArgument("npcId"))
                .executesPlayer((player, args) -> {
                    String response = (String) args.get("response");
                    String dialogueId = (String) args.get("dialogueId");
                    String npcId = (String) args.get("npcId");

                    DialogueManager.getInstance().handleResponse(player, response, dialogueId, npcId);
                })
                .register();
    }

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
