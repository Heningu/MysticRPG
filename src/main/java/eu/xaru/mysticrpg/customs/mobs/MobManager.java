package eu.xaru.mysticrpg.customs.mobs;

import com.ticxo.modelengine.api.model.ModeledEntity;
import eu.xaru.mysticrpg.customs.items.CustomItem;
import eu.xaru.mysticrpg.customs.items.CustomItemModule;
import eu.xaru.mysticrpg.customs.items.ItemManager;
import eu.xaru.mysticrpg.customs.mobs.actions.Action;
import eu.xaru.mysticrpg.customs.mobs.actions.ActionStep;
import eu.xaru.mysticrpg.customs.mobs.actions.Condition;
import eu.xaru.mysticrpg.customs.mobs.actions.steps.DelayActionStep;
import eu.xaru.mysticrpg.customs.mobs.bossbar.MobBossBarHandler;
import eu.xaru.mysticrpg.economy.EconomyHelper;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.party.Party;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class MobManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, CustomMob> mobConfigurations;
    private final Map<UUID, CustomMobInstance> activeMobs = new HashMap<>();
    private final Random random = new Random();
    private final ItemManager itemManager;
    private final EconomyHelper economyHelper;
    private final PartyHelper partyHelper;

    public MobManager(JavaPlugin plugin, Map<String, CustomMob> mobConfigurations, EconomyHelper economyHelper) {
        this.plugin = plugin;
        this.economyHelper = economyHelper;
        this.mobConfigurations = mobConfigurations;
        this.itemManager = ModuleManager.getInstance().getModuleInstance(CustomItemModule.class).getItemManager();

        // Initialize PartyHelper
        PartyModule partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
        if (partyModule != null) {
            this.partyHelper = partyModule.getPartyHelper();
        } else {
            this.partyHelper = null;
            Bukkit.getLogger().warning("PartyModule is not available. Party features will be disabled.");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Scheduled tasks
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobAnimations, 0L, 5L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkCombatStatus, 0L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, 0L, 10L);
    }

    public CustomMobInstance spawnMobAtLocation(CustomMob customMob, Location location) {
        if (location.getWorld() == null) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot spawn mob '" + customMob.getName() + "' because location world is null.");
            return null;
        }

        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, customMob.getEntityType());
        applyCustomAttributes(mob, customMob);

        // Set name and health
        mob.setCustomName(createMobNameTag(customMob, customMob.getHealth()));
        mob.setCustomNameVisible(true);
        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customMob.getHealth());
        }
        mob.setHealth(customMob.getHealth());

        // Apply the model
        ModeledEntity modeledEntity = ModelHandler.applyModel(mob, customMob.getModelId());
        if (modeledEntity != null) {
            modeledEntity.setBaseEntityVisible(false);
        }

        CustomMobInstance mobInstance = new CustomMobInstance(customMob, location, mob, modeledEntity);
        activeMobs.put(mob.getUniqueId(), mobInstance);

        executeActions(mobInstance, ActionTriggers.ON_SPAWN);
        return mobInstance;
    }

    private void applyCustomAttributes(LivingEntity mob, CustomMob customMob) {
        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(customMob.getMovementSpeed());

        AttributeInstance armor = mob.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(customMob.getBaseArmor());

        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            if (customMob.getEquipment() != null) {
                ItemStack helmet = getCustomItemStack(customMob.getEquipment().getHelmet());
                if (helmet != null) equipment.setHelmet(helmet);

                ItemStack chestplate = getCustomItemStack(customMob.getEquipment().getChestplate());
                if (chestplate != null) equipment.setChestplate(chestplate);

                ItemStack leggings = getCustomItemStack(customMob.getEquipment().getLeggings());
                if (leggings != null) equipment.setLeggings(leggings);

                ItemStack boots = getCustomItemStack(customMob.getEquipment().getBoots());
                if (boots != null) equipment.setBoots(boots);

                ItemStack weapon = getCustomItemStack(customMob.getEquipment().getWeapon());
                if (weapon != null) equipment.setItemInMainHand(weapon);
            }
        }
    }

    private ItemStack getCustomItemStack(String itemId) {
        CustomItem item = itemManager.getCustomItem(itemId);
        return item != null ? item.toItemStack() : null;
    }

    private String createMobNameTag(CustomMob mob, double currentHp) {
        return Utils.getInstance().$(String.format("[LVL%d] %s [%.1f❤]", mob.getLevel(), mob.getName(), currentHp));
    }

    public CustomMobInstance findMobInstance(LivingEntity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    public void executeActions(CustomMobInstance mobInstance, String trigger) {
        List<Action> actions = mobInstance.getCustomMob().getActions().get(trigger);
        if (actions == null) return;

        for (Action action : actions) {
            if (mobInstance.isPerformingAction()) continue;

            boolean conditionsMet = true;
            for (Condition condition : action.getTargetConditions()) {
                if (!condition.evaluate(mobInstance.getEntity(), mobInstance.getTarget())) {
                    conditionsMet = false;
                    break;
                }
            }

            if (conditionsMet) {
                mobInstance.setPerformingAction(true);
                Bukkit.getScheduler().runTask(plugin, () -> executeActionSteps(mobInstance, action.getSteps(), 0));
            }
        }
    }

    private void executeActionSteps(CustomMobInstance mobInstance, List<ActionStep> steps, int index) {
        if (index >= steps.size()) {
            mobInstance.setPerformingAction(false);
            return;
        }

        ActionStep step = steps.get(index);
        if (step instanceof DelayActionStep delay) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> executeActionSteps(mobInstance, steps, index + 1), (long)delay.getDelaySeconds() * 20);
        } else {
            step.execute(mobInstance);
            executeActionSteps(mobInstance, steps, index + 1);
        }
    }
    /**
     * Retrieves the configuration map of all custom mobs.
     *
     * @return A map where the keys are mob IDs and the values are their corresponding CustomMob objects.
     */
    public Map<String, CustomMob> getMobConfigurations() {
        return mobConfigurations;
    }


    private void updateMobAnimations() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            LivingEntity entity = mobInstance.getEntity();
            if (entity.isDead()) continue;

            String animation = mobInstance.isInCombat()
                    ? mobInstance.getCustomMob().getAnimationConfig().getCombatIdleAnimation()
                    : mobInstance.getCustomMob().getAnimationConfig().getIdleAnimation();

            if (!animation.equals(mobInstance.getCurrentAnimation())) {
                mobInstance.setCurrentAnimation(animation);
                ModelHandler.playAnimation(entity, mobInstance.getCustomMob().getModelId(), animation, 0, 0, 1, true);
            }
        }
    }



    private void checkCombatStatus() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            if (!mobInstance.isInCombat() || mobInstance.getTarget() == null) {
                mobInstance.setInCombat(false);
                executeActions(mobInstance, ActionTriggers.ON_LEAVE_COMBAT);
            }
        }
    }

    private void updateBossBars() {
        for (CustomMobInstance mobInstance : activeMobs.values()) {
            if (mobInstance.getBossBarHandler() != null) {
                mobInstance.getBossBarHandler().updateBossBar();
            }
        }
    }

    public static class ActionTriggers {
        public static final String ON_ATTACK = "onAttack";
        public static final String ON_SPAWN = "onSpawn";
        public static final String ON_ENTER_COMBAT = "onEnterCombat";
        public static final String ON_LEAVE_COMBAT = "onLeaveCombat";
        public static final String ON_DAMAGED = "onDamaged";
    }
}
