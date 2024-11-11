//package eu.xaru.mysticrpg.npc;
//
//import com.github.juliarn.npclib.api.Platform;
//import com.github.juliarn.npclib.api.Position;
//import com.github.juliarn.npclib.api.profile.Profile;
//import com.github.juliarn.npclib.api.profile.ProfileProperty;
//import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
//import eu.xaru.mysticrpg.managers.ModuleManager;
//import eu.xaru.mysticrpg.player.leveling.LevelModule;
//import eu.xaru.mysticrpg.quests.QuestModule;
//import eu.xaru.mysticrpg.storage.PlayerDataCache;
//import eu.xaru.mysticrpg.storage.SaveModule;
//import org.bukkit.ChatColor;
//import org.bukkit.Location;
//import org.bukkit.World;
//import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.plugin.Plugin;
//import org.bukkit.plugin.java.JavaPlugin;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.UUID;
//
//public class NPC {
//
//    private String name;
//    private Location location;
//    private String behavior;
//    private String skin;
//    private YamlConfiguration config;
//    private File configFile;
//
//    private final QuestModule questModule;
//    private final PlayerDataCache playerDataCache;
//    private final LevelModule levelModule;
//
//    private final DialogueManager dialogueManager;
//
//    private final JavaPlugin plugin;
//    private final Platform<World, Player, ItemStack, Plugin> npcPlatform;
//    private com.github.juliarn.npclib.api.Npc npcEntity;
//
//    public NPC(String name, Location location, Platform<World, Player, ItemStack, Plugin> npcPlatform) {
//        this.name = name;
//        this.location = location;
//        this.behavior = "default";
//        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
//        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
//        this.playerDataCache = saveModule.getPlayerDataCache();
//        this.levelModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
//        this.dialogueManager = new DialogueManager(this);
//
//        this.plugin = JavaPlugin.getPlugin(eu.xaru.mysticrpg.cores.MysticCore.class);
//        this.npcPlatform = npcPlatform;
//
//        loadConfig();
//        spawn();
//    }
//
//    private void loadConfig() {
//        File npcsFolder = new File(plugin.getDataFolder(), "npcs");
//        this.configFile = new File(npcsFolder, name + ".yml");
//        this.config = YamlConfiguration.loadConfiguration(configFile);
//        dialogueManager.loadDialogues();
//    }
//
//    private void saveConfig() {
//        try {
//            config.save(configFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void spawn() {
//        Position npcPosition = BukkitPlatformUtil.positionFromBukkitLegacy(location);
//        Profile.Resolved npcProfile = Profile.resolved(name, UUID.randomUUID());
//
//        if (skin != null && !skin.isEmpty()) {
//            String[] skinData = skin.split(":");
//            if (skinData.length == 2) {
//                // Create a ProfileProperty for textures and add it to the profile
//                ProfileProperty textureProperty = new ProfileProperty("textures", skinData[0], skinData[1]);
//                npcProfile = npcProfile.withProperty(textureProperty);
//                createNPC(npcPosition, npcProfile);
//            } else {
//                npcPlatform.profileResolver().resolveProfile(Profile.unresolved(skin)).thenAccept(profile -> {
//                    Profile.Resolved resolvedProfile = Profile.resolved(profile.name(), profile.uniqueId(), profile.properties());
//                    createNPC(npcPosition, resolvedProfile);
//                });
//                return;
//            }
//        } else {
//            createNPC(npcPosition, npcProfile);
//        }
//    }
//
//
//
//    private void createNPC(Position npcPosition, Profile npcProfile) {
//        npcPlatform.newNpcBuilder()
//                .position(npcPosition)
//                .profile(npcProfile)
//                .npcSettings(builder -> builder
//                        .profileResolver((player, npc) -> npcPlatform.profileResolver()
//                                .resolveProfile(Profile.unresolved(player.getUniqueId()))
//                                .thenApply(profile -> npc.profile().withProperties(profile.properties())))
//                        .trackingRule((npc, player) -> player.getExp() >= 50) // Example tracking rule
//                )
//                .thenAccept(builder -> {
//                    npcEntity = builder.buildAndTrack();
//                    registerNPCListeners();
//                });
//    }
//
//    public void despawn() {
//        if (npcEntity != null) {
//            npcEntity.unlink();
//            npcEntity = null;
//        }
//    }
//
//    public void update() {
//        // Update any additional settings here if needed
//    }
//
//    public void interact(Player player) {
//        if (dialogueManager.hasDialogues()) {
//            dialogueManager.startDialogue(player);
//        } else {
//            String message = config.getString("interaction.message", "Hello, %player%!");
//            message = message.replace("%player%", player.getName());
//            player.sendMessage(Utils.getInstance().$(name + ": " + + message);
//        }
//    }
//
//    public void setSkin(String skinData) {
//        this.skin = skinData;
//        config.set("skin", skinData);
//        saveConfig();
//        despawn();
//        spawn();
//    }
//
//    public String getSkin() {
//        return skin;
//    }
//
//    private void registerNPCListeners() {
//        npcPlatform.eventManager().registerEventHandler(com.github.juliarn.npclib.api.event.InteractNpcEvent.class, event -> {
//            if (event.npc().equals(this.npcEntity)) {
//                Player player = event.player();
//                interact(player);
//            }
//        });
//
//        npcPlatform.eventManager().registerEventHandler(com.github.juliarn.npclib.api.event.AttackNpcEvent.class, event -> {
//            if (event.npc().equals(this.npcEntity)) {
//                Player player = event.player();
//                player.sendMessage(Utils.getInstance().$("Please don't attack " + name + "!");
//            }
//        });
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public Location getLocation() {
//        return location;
//    }
//
//    public DialogueManager getDialogueManager() {
//        return dialogueManager;
//    }
//
//    public YamlConfiguration getConfig() {
//        return config;
//    }
//
//    public JavaPlugin getPlugin() {
//        return plugin;
//    }
//
//    public void setBehavior(String behavior) {
//        this.behavior = behavior;
//        config.set("behavior", behavior);
//        saveConfig();
//        update();
//    }
//
//    public String getBehavior() {
//        return behavior;
//    }
//}
