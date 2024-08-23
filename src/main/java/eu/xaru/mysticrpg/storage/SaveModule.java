package eu.xaru.mysticrpg.storage;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SaveModule implements IBaseModule {

    private SaveHelper saveHelper;
    private DebugLoggerModule logger;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        String connectionString = "mongodb://xarumystic:password@85.202.163.30:29095/xarumystic?tls=true&tlsAllowInvalidCertificates=true";
        try {
            saveHelper = new SaveHelper(connectionString, "xarumystic", "playerData", logger);
            logger.log(Level.INFO, "SaveModule initialized", 0);
        } catch (Exception e) {
            logger.error("Failed to initialize SaveModule: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "SaveModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "SaveModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "SaveModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of(DebugLoggerModule.class);  // Depend on DebugLoggerModule
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.HIGH;  // Ensure it loads early
    }

    public PlayerData getPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        logger.log(Level.INFO, "Loading data for player: " + player.getName(), 0);
        PlayerData playerData = saveHelper.loadPlayer(playerUUID);
        logger.logObject(playerData);
        return playerData;
    }

    public void savePlayerData(PlayerData playerData) {
        logger.log(Level.INFO, "Saving data for player: " + playerData.uuid(), 0);
        saveHelper.savePlayer(playerData);
        logger.log(Level.INFO, "Data saved for player: " + playerData.uuid(), 0);
    }
}
