//package eu.xaru.mysticrpg.world;
//
//import eu.xaru.mysticrpg.enums.EModulePriority;
//import eu.xaru.mysticrpg.interfaces.IBaseModule;
//import eu.xaru.mysticrpg.managers.ModuleManager;
//import eu.xaru.mysticrpg.storage.Callback;
//import eu.xaru.mysticrpg.storage.SaveModule;
//import eu.xaru.mysticrpg.storage.SaveHelper;
//import eu.xaru.mysticrpg.utils.DebugLoggerModule;
//import dev.jorel.commandapi.CommandAPICommand;
//import dev.jorel.commandapi.arguments.StringArgument;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//
//public class WorldModule implements IBaseModule {
//    private DebugLoggerModule logger;
//    private SaveHelper saveHelper;
//    private Map<String, Region> regions; // Holds all regions
//
//    @Override
//    public void initialize() {
//        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
//        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
//        saveHelper = saveModule.getSaveHelper();
//
//        regions = new HashMap<>();
//        loadAllRegions(); // Load existing regions from the database
//
//        logger.log(Level.INFO, "WorldModule initialized", 0);
//    }
//
//    @Override
//    public void start() {
//        registerCommands();
//        logger.log(Level.INFO, "WorldModule started", 0);
//    }
//
//    @Override
//    public void stop() {
//        logger.log(Level.INFO, "WorldModule stopped", 0);
//    }
//
//    @Override
//    public void unload() {
//        logger.log(Level.INFO, "WorldModule unloaded", 0);
//    }
//
//    @Override
//    public List<Class<? extends IBaseModule>> getDependencies() {
//        return List.of(DebugLoggerModule.class, SaveModule.class);
//    }
//
//    @Override
//    public EModulePriority getPriority() {
//        return EModulePriority.NORMAL;
//    }
//
//    private void registerCommands() {
//        // Register commands using CommandAPI
//        new CommandAPICommand("mysticregions")
//                .withSubcommand(new CommandAPICommand("create")
//                        .withArguments(new StringArgument("name"))
//                        .executesPlayer((player, args) -> {
//                            String name = (String) args[0];
//                            String regionId = "ID-" + name;
//                            Region newRegion = new Region(regionId, name, ""); // Coordinates will be set in config command
//                            addRegion(newRegion);
//                            player.sendMessage("Region " + name + " created with ID " + regionId);
//                        })
//                )
//                .withSubcommand(new CommandAPICommand("config")
//                        .withArguments(new StringArgument("id"))
//                        .executesPlayer((player, args) -> {
//                            String id = (String) args[0];
//                            Region region = getRegionById(id);
//                            if (region != null) {
//                                // Logic to start area selection for region (use a plugin like WorldEdit to handle this)
//                                player.sendMessage("Right-click to set region corners for " + id);
//                            } else {
//                                player.sendMessage("Region not found!");
//                            }
//                        })
//                )
//                .withSubcommand(new CommandAPICommand("settings")
//                        .withArguments(new StringArgument("id"), new StringArgument("flags"))
//                        .executesPlayer((player, args) -> {
//                            String id = (String) args[0];
//                            String flagsString = (String) args[1];
//                            Region region = getRegionById(id);
//                            if (region != null) {
//                                // Parse flags from string and apply to region
//                                String[] flagPairs = flagsString.split(", ");
//                                for (String flagPair : flagPairs) {
//                                    String[] parts = flagPair.split(":");
//                                    if (parts.length == 2) {
//                                        String flag = parts[0];
//                                        boolean value = Boolean.parseBoolean(parts[1]);
//                                        region.setFlag(flag, value);
//                                    }
//                                }
//                                saveRegion(region); // Save updated region
//                                player.sendMessage("Region settings updated for " + id);
//                            } else {
//                                player.sendMessage("Region not found!");
//                            }
//                        })
//                )
//                .register();
//    }
//
//    private void loadAllRegions() {
//        saveHelper.loadAllRegions(new Callback<List<Region>>() {
//            @Override
//            public void onSuccess(List<Region> result) {
//                for (Region region : result) {
//                    regions.put(region.getId(), region);
//                }
//                logger.log(Level.INFO, "Loaded all regions from the database", 0);
//            }
//
//            @Override
//            public void onFailure(Throwable throwable) {
//                logger.error("Failed to load regions: " + throwable.getMessage());
//            }
//        });
//    }
//
//    public Region getRegionById(String id) {
//        return regions.get(id);
//    }
//
//    public void addRegion(Region region) {
//        regions.put(region.getId(), region);
//        saveHelper.saveRegion(region, new Callback<Void>() {
//            @Override
//            public void onSuccess(Void result) {
//                logger.log(Level.INFO, "Region saved: " + region.getId(), 0);
//            }
//
//            @Override
//            public void onFailure(Throwable throwable) {
//                logger.error("Failed to save region: " + throwable.getMessage());
//            }
//        });
//    }
//
//    public void removeRegion(String id) {
//        Region region = regions.remove(id);
//        if (region != null) {
//            saveHelper.deleteRegion(id, new Callback<Void>() {
//                @Override
//                public void onSuccess(Void result) {
//                    logger.log(Level.INFO, "Region deleted: " + id, 0);
//                }
//
//                @Override
//                public void onFailure(Throwable throwable) {
//                    logger.error("Failed to delete region: " + throwable.getMessage());
//                }
//            });
//        }
//    }
//}
