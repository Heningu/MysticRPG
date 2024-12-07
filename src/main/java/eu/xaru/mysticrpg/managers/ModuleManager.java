package eu.xaru.mysticrpg.managers;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.utils.DebugLogger;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of modules, including loading, initializing, starting, stopping, and unloading.
 * Ensures modules are loaded in the correct order based on dependencies and priorities.
 * Handles circular dependencies with auto-correction strategies.
 * Supports lazy loading of modules.
 */
public class ModuleManager {

    // Map to hold loaded modules
    private final Map<Class<? extends IBaseModule>, IBaseModule> loadedModules = new ConcurrentHashMap<>();

    // List to maintain the order of module loading
    private final List<Class<? extends IBaseModule>> loadingOrder = new ArrayList<>();

    // Cache for module instances to avoid repeated instantiation
    private final Map<Class<? extends IBaseModule>, IBaseModule> moduleInstanceCache = new ConcurrentHashMap<>();

    // Set to track modules that are lazy-loaded and not yet loaded
    private final Set<Class<? extends IBaseModule>> lazyModules = ConcurrentHashMap.newKeySet();

    // Singleton instance
    private static volatile ModuleManager instance;

    // Private constructor for singleton pattern
    private ModuleManager() {
        // Initialize if needed
    }

    /**
     * Retrieves the singleton instance of ModuleManager.
     *
     * @return the singleton instance
     */
    public static ModuleManager getInstance() {
        if (instance == null) {
            synchronized (ModuleManager.class) {
                if (instance == null) {
                    instance = new ModuleManager();
                }
            }
        }
        return instance;
    }

    /**
     * Loads all modules automatically using ClassGraph, resolving dependencies and ordering by priority.
     * Eagerly loads non-lazy modules and registers lazy modules for on-demand loading.
     */
    public synchronized void loadAllModules() {
        unloadAllModules(); // Ensure previous modules are unloaded

        // Discover all module classes within eu.xaru.mysticrpg and its subpackages
        Set<Class<? extends IBaseModule>> moduleClasses = discoverModules();

        if (moduleClasses.isEmpty()) {
            DebugLogger.getInstance().warning("No modules found to load.");
            return;
        }

        // Separate eager and lazy modules
        Set<Class<? extends IBaseModule>> eagerModules = moduleClasses.stream()
                .filter(moduleClass -> {
                    IBaseModule module = instantiateModule(moduleClass);
                    if (module != null && module.isLazy()) {
                        lazyModules.add(moduleClass);
                        DebugLogger.getInstance().log("Module {} marked as lazy-loaded.", moduleClass.getSimpleName());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());

        // Resolve dependencies and determine loading order for eager modules
        List<Class<? extends IBaseModule>> orderedEagerModules = resolveLoadingOrder(eagerModules);

        if (orderedEagerModules == null) {
            DebugLogger.getInstance().error("Failed to resolve eager module loading order due to circular dependencies.");
            return;
        }

        DebugLogger.getInstance().log("Final eager module loading order: {}", orderedEagerModules.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", ")));

        // Load and initialize eager modules in the determined order
        for (Class<? extends IBaseModule> moduleClass : orderedEagerModules) {
            try {
                loadModule(moduleClass);
                DebugLogger.getInstance().log("Module {} loaded successfully.", moduleClass.getSimpleName());
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load module {}: {}", moduleClass.getSimpleName(), e.getMessage(), e);
            }
        }

        // Start all eager modules
        startModules();
    }

    /**
     * Uses ClassGraph to discover all classes implementing IBaseModule within the eu.xaru.mysticrpg package and its subpackages.
     *
     * @return a set of module classes
     */
    private Set<Class<? extends IBaseModule>> discoverModules() {
        Set<Class<? extends IBaseModule>> modules = new HashSet<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("eu.xaru.mysticrpg") // Broad scan of the entire package
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesImplementing(IBaseModule.class.getName())) {
                if (classInfo.isAbstract() || classInfo.isInterface()) {
                    continue; // Skip abstract classes and interfaces
                }
                @SuppressWarnings("unchecked")
                Class<? extends IBaseModule> moduleClass = (Class<? extends IBaseModule>) classInfo.loadClass();
                modules.add(moduleClass);
                DebugLogger.getInstance().debug("Discovered module class: {}", moduleClass.getSimpleName());
            }
        } catch (Exception e) {
            DebugLogger.getInstance().error("Error during module discovery: {}", e.getMessage(), e);
        }
        return modules;
    }

    /**
     * Resolves the loading order based on dependencies and priorities using topoDebugLogger.getInstance().logical sorting.
     * Handles the `FIRST` priority group by ensuring they are loaded before others.
     * Implements auto-correction strategies for circular dependencies.
     *
     * @param modules the set of discovered module classes
     * @return a list of module classes ordered for loading, or null if unresolved circular dependencies exist
     */
    private List<Class<? extends IBaseModule>> resolveLoadingOrder(Set<Class<? extends IBaseModule>> modules) {
        // Build dependency graph
        Map<Class<? extends IBaseModule>, Set<Class<? extends IBaseModule>>> dependencyGraph = new HashMap<>();
        for (Class<? extends IBaseModule> module : modules) {
            try {
                IBaseModule moduleInstance = instantiateModule(module);
                if (moduleInstance == null) {
                    DebugLogger.getInstance().warn("Module {} could not be instantiated for dependency resolution.", module.getSimpleName());
                    dependencyGraph.put(module, new HashSet<>());
                    continue;
                }

                List<Class<? extends IBaseModule>> dependencies = moduleInstance.getDependencies();

                // Enforce that `FIRST` modules only depend on other `FIRST` modules
                if (moduleInstance.getPriority() == EModulePriority.FIRST) {
                    for (Class<? extends IBaseModule> dependency : dependencies) {
                        IBaseModule depModule = moduleInstanceCache.get(dependency);
                        if (depModule != null && depModule.getPriority() != EModulePriority.FIRST) {
                            DebugLogger.getInstance().error("Module {} in `FIRST` group cannot depend on non-`FIRST` module {}.",
                                    module.getSimpleName(), dependency.getSimpleName());
                            throw new IllegalArgumentException("Invalid dependency: `FIRST` modules cannot depend on non-`FIRST` modules.");
                        }
                    }
                }

                dependencyGraph.put(module, new HashSet<>(dependencies));
                DebugLogger.getInstance().debug("Module {} dependencies: {}", module.getSimpleName(),
                        dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
            } catch (Exception e) {
                DebugLogger.getInstance().error("Error instantiating module {} for dependency resolution: {}", module.getSimpleName(), e.getMessage(), e);
                dependencyGraph.put(module, new HashSet<>()); // Assume no dependencies on failure
            }
        }

        // Perform topoDebugLogger.getInstance().logical sort with cycle detection
        List<Class<? extends IBaseModule>> sortedModules = new ArrayList<>();
        Set<Class<? extends IBaseModule>> visited = new HashSet<>();
        Set<Class<? extends IBaseModule>> visiting = new HashSet<>();
        boolean hasCycle = false;

        for (Class<? extends IBaseModule> module : modules) {
            if (!visited.contains(module)) {
                if (!topologicalSort(module, dependencyGraph, sortedModules, visited, visiting)) {
                    hasCycle = true;
                    break;
                }
            }
        }

        if (hasCycle) {
            DebugLogger.getInstance().warn("Circular dependencies detected. Attempting to auto-correct loading order.");

            // Attempt to break cycles by removing dependencies that cause cycles
            // Strategy: Remove the lowest priority dependency in the cycle
            Set<Class<? extends IBaseModule>> modulesInCycle = findModulesInCycle(dependencyGraph);
            if (modulesInCycle.isEmpty()) {
                DebugLogger.getInstance().error("Unable to identify modules involved in circular dependencies.");
                return null;
            }

            // Find the module with the lowest priority in the cycle
            Class<? extends IBaseModule> lowestPriorityModule = modulesInCycle.stream()
                    .min(Comparator.comparingInt(this::getModulePriorityOrdinal))
                    .orElse(null);

            if (lowestPriorityModule != null) {
                DebugLogger.getInstance().warn("Auto-correcting by removing dependencies from module {}", lowestPriorityModule.getSimpleName());
                dependencyGraph.put(lowestPriorityModule, new HashSet<>()); // Remove its dependencies

                // Retry topoDebugLogger.getInstance().logical sort after correction
                sortedModules.clear();
                visited.clear();
                visiting.clear();
                hasCycle = false;

                for (Class<? extends IBaseModule> module : modules) {
                    if (!visited.contains(module)) {
                        if (!topologicalSort(module, dependencyGraph, sortedModules, visited, visiting)) {
                            hasCycle = true;
                            break;
                        }
                    }
                }

                if (hasCycle) {
                    DebugLogger.getInstance().error("Failed to resolve circular dependencies after auto-correction.");
                    return null;
                }
            } else {
                DebugLogger.getInstance().error("No suitable module found to break the circular dependency.");
                return null;
            }
        }

        // Sort by priority in ascending order (FIRST=0 first)
        sortedModules.sort(Comparator.comparingInt((Class<? extends IBaseModule> cls) -> {
            IBaseModule module = moduleInstanceCache.get(cls);
            return module != null ? module.getPriority().ordinal() : EModulePriority.LOW.ordinal();
        }));

        DebugLogger.getInstance().debug("Resolved loading order after handling dependencies: {}", sortedModules.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", ")));
        return sortedModules;
    }

    /**
     * Performs a topoDebugLogger.getInstance().logical sort on the dependency graph.
     *
     * @param module          the current module being visited
     * @param dependencyGraph the dependency graph
     * @param sortedModules   the list to populate with sorted modules
     * @param visited         the set of already visited modules
     * @param visiting        the set of modules currently being visited (for cycle detection)
     * @return true if successful, false if a circular dependency is detected
     */
    private boolean topologicalSort(Class<? extends IBaseModule> module,
                                    Map<Class<? extends IBaseModule>, Set<Class<? extends IBaseModule>>> dependencyGraph,
                                    List<Class<? extends IBaseModule>> sortedModules,
                                    Set<Class<? extends IBaseModule>> visited,
                                    Set<Class<? extends IBaseModule>> visiting) {
        visiting.add(module);
        for (Class<? extends IBaseModule> dependency : dependencyGraph.getOrDefault(module, Collections.emptySet())) {
            if (!dependencyGraph.containsKey(dependency)) {
                DebugLogger.getInstance().warn("Module {} depends on unknown module {}", module.getSimpleName(), dependency.getSimpleName());
                continue; // Skip unknown dependencies
            }
            if (visiting.contains(dependency)) {
                DebugLogger.getInstance().error("Circular dependency detected: {} <-> {}", module.getSimpleName(), dependency.getSimpleName());
                return false; // Circular dependency detected
            }
            if (!visited.contains(dependency)) {
                if (!topologicalSort(dependency, dependencyGraph, sortedModules, visited, visiting)) {
                    return false;
                }
            }
        }
        visiting.remove(module);
        visited.add(module);
        sortedModules.add(module);
        return true;
    }

    /**
     * Identifies all modules involved in circular dependencies.
     *
     * @param dependencyGraph the dependency graph
     * @return a set of modules involved in cycles
     */
    private Set<Class<? extends IBaseModule>> findModulesInCycle(Map<Class<? extends IBaseModule>, Set<Class<? extends IBaseModule>>> dependencyGraph) {
        Set<Class<? extends IBaseModule>> modulesInCycle = new HashSet<>();
        Set<Class<? extends IBaseModule>> visited = new HashSet<>();
        Set<Class<? extends IBaseModule>> stack = new HashSet<>();

        for (Class<? extends IBaseModule> module : dependencyGraph.keySet()) {
            if (detectCycleDFS(module, dependencyGraph, visited, stack, modulesInCycle)) {
                // Cycle detected, modules involved are added to modulesInCycle
            }
        }
        return modulesInCycle;
    }

    /**
     * Depth-First Search to detect cycles and collect modules involved.
     *
     * @param module          the current module
     * @param dependencyGraph the dependency graph
     * @param visited         the set of already visited modules
     * @param stack           the current recursion stack
     * @param modulesInCycle  the set to collect modules involved in cycles
     * @return true if a cycle is detected, false otherwise
     */
    private boolean detectCycleDFS(Class<? extends IBaseModule> module,
                                   Map<Class<? extends IBaseModule>, Set<Class<? extends IBaseModule>>> dependencyGraph,
                                   Set<Class<? extends IBaseModule>> visited,
                                   Set<Class<? extends IBaseModule>> stack,
                                   Set<Class<? extends IBaseModule>> modulesInCycle) {
        if (stack.contains(module)) {
            modulesInCycle.add(module);
            return true;
        }
        if (visited.contains(module)) {
            return false;
        }
        visited.add(module);
        stack.add(module);
        for (Class<? extends IBaseModule> dependency : dependencyGraph.getOrDefault(module, Collections.emptySet())) {
            if (detectCycleDFS(dependency, dependencyGraph, visited, stack, modulesInCycle)) {
                modulesInCycle.add(module);
                return true;
            }
        }
        stack.remove(module);
        return false;
    }

    /**
     * Retrieves the ordinal value of a module's priority.
     *
     * @param moduleClass the class of the module
     * @return the ordinal value of the module's priority
     */
    private int getModulePriorityOrdinal(Class<? extends IBaseModule> moduleClass) {
        IBaseModule module = moduleInstanceCache.get(moduleClass);
        return module != null ? module.getPriority().ordinal() : EModulePriority.LOW.ordinal();
    }

    /**
     * Instantiates a module without registering it.
     * Caches the instance to avoid repeated instantiation.
     *
     * @param moduleClass the class of the module to instantiate
     * @return an instance of the module, or null if instantiation fails
     */
    private IBaseModule instantiateModule(Class<? extends IBaseModule> moduleClass) {
        if (moduleInstanceCache.containsKey(moduleClass)) {
            return moduleInstanceCache.get(moduleClass);
        }
        try {
            Constructor<? extends IBaseModule> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            IBaseModule module = constructor.newInstance();
            moduleInstanceCache.put(moduleClass, module);
            return module;
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to instantiate module {}: {}", moduleClass.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Loads and initializes a single module.
     *
     * @param moduleClass the class of the module to load
     * @throws Exception if loading fails
     */
    private void loadModule(Class<? extends IBaseModule> moduleClass) throws Exception {
        if (loadedModules.containsKey(moduleClass)) {
            DebugLogger.getInstance().warn("Module {} is already loaded.", moduleClass.getSimpleName());
            return;
        }

        IBaseModule module = moduleInstanceCache.get(moduleClass);
        if (module == null) {
            module = instantiateModule(moduleClass);
            if (module == null) {
                throw new Exception("Module instantiation failed.");
            }
        }

        // Initialize the module
        module.initialize();
        loadedModules.put(moduleClass, module);
        loadingOrder.add(moduleClass);
        DebugLogger.getInstance().log("Module {} initialized.", moduleClass.getSimpleName());
    }

    /**
     * Starts all loaded modules in the determined loading order.
     */
    public synchronized void startModules() {
        for (Class<? extends IBaseModule> moduleClass : loadingOrder) {
            startModule(moduleClass);
        }
    }

    /**
     * Starts a single module.
     *
     * @param moduleClass the class of the module to start
     */
    private void startModule(Class<? extends IBaseModule> moduleClass) {
        IBaseModule module = loadedModules.get(moduleClass);
        if (module == null) {
            DebugLogger.getInstance().warn("Module {} is not loaded and cannot be started.", moduleClass.getSimpleName());
            return;
        }
        try {
            module.start();
            DebugLogger.getInstance().log("Module {} started.", moduleClass.getSimpleName());
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to start module {}: {}", moduleClass.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Stops and unloads a specific module.
     *
     * @param moduleClass the class of the module to stop and unload
     */
    public synchronized void stopAndUnloadModule(Class<? extends IBaseModule> moduleClass) {
        IBaseModule module = loadedModules.remove(moduleClass);
        if (module == null) {
            DebugLogger.getInstance().warn("Module {} is not loaded.", moduleClass.getSimpleName());
            return;
        }

        try {
            module.stop();
            module.unload();
            loadingOrder.remove(moduleClass);
            DebugLogger.getInstance().log("Module {} stopped and unloaded.", moduleClass.getSimpleName());
        } catch (Exception e) {
            DebugLogger.getInstance().error("Failed to stop or unload module {}: {}", moduleClass.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Stops and unloads all loaded modules in reverse loading order.
     */
    public synchronized void unloadAllModules() {
        List<Class<? extends IBaseModule>> modulesToUnload = new ArrayList<>(loadingOrder);
        Collections.reverse(modulesToUnload); // Unload in reverse order

        for (Class<? extends IBaseModule> moduleClass : modulesToUnload) {
            stopAndUnloadModule(moduleClass);
        }

        loadingOrder.clear();
        loadedModules.clear();
        moduleInstanceCache.clear();
        lazyModules.clear();

        DebugLogger.getInstance().log("All modules have been unloaded.");
    }

    /**
     * Retrieves an instance of a loaded module.
     *
     * @param moduleClass the class of the module to retrieve
     * @param <T>         the type of the module
     * @return the module instance, or null if not loaded
     */
    public synchronized <T extends IBaseModule> T getModuleInstance(Class<T> moduleClass) {
        IBaseModule module = loadedModules.get(moduleClass);
        if (module == null) {
            DebugLogger.getInstance().warn("Module {} is not loaded.", moduleClass.getSimpleName());
            return null;
        }
        return moduleClass.cast(module);
    }

    /**
     * Loads a lazy module on-demand, ensuring its dependencies are loaded first.
     *
     * @param moduleClass the class of the module to load
     * @return true if the module was loaded successfully, false otherwise
     */
    public synchronized boolean loadLazyModule(Class<? extends IBaseModule> moduleClass) {
        if (!lazyModules.contains(moduleClass)) {
            DebugLogger.getInstance().warn("Module {} is not marked as lazy-loaded.", moduleClass.getSimpleName());
            return false;
        }

        // Remove from lazyModules as it will be loaded
        lazyModules.remove(moduleClass);

        // Resolve dependencies and determine loading order for this module and its dependencies
        Set<Class<? extends IBaseModule>> modulesToLoad = new HashSet<>();
        collectDependencies(moduleClass, modulesToLoad, new HashSet<>());

        // Exclude already loaded modules
        modulesToLoad.removeAll(loadedModules.keySet());

        if (modulesToLoad.isEmpty()) {
            DebugLogger.getInstance().log("No new modules to load for {}", moduleClass.getSimpleName());
            return true;
        }

        List<Class<? extends IBaseModule>> orderedModules = resolveLoadingOrder(modulesToLoad);

        if (orderedModules == null) {
            DebugLogger.getInstance().error("Failed to resolve loading order for lazy module {} due to circular dependencies.", moduleClass.getSimpleName());
            return false;
        }

        DebugLogger.getInstance().log("Loading lazy module {} and its dependencies in order: {}", moduleClass.getSimpleName(),
                orderedModules.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));

        // Load and initialize modules in the determined order
        for (Class<? extends IBaseModule> cls : orderedModules) {
            try {
                loadModule(cls);
                DebugLogger.getInstance().log("Module {} loaded successfully.", cls.getSimpleName());
            } catch (Exception e) {
                DebugLogger.getInstance().error("Failed to load module {}: {}", cls.getSimpleName(), e.getMessage(), e);
                return false;
            }
        }

        // Start the newly loaded modules
        for (Class<? extends IBaseModule> cls : orderedModules) {
            startModule(cls);
        }

        return true;
    }

    /**
     * Recursively collects dependencies for a module.
     *
     * @param module        the module to collect dependencies for
     * @param modulesToLoad the set to collect modules into
     * @param visited       the set of already visited modules to prevent infinite recursion
     */
    private void collectDependencies(Class<? extends IBaseModule> module,
                                     Set<Class<? extends IBaseModule>> modulesToLoad,
                                     Set<Class<? extends IBaseModule>> visited) {
        if (visited.contains(module)) {
            return;
        }
        visited.add(module);
        IBaseModule moduleInstance = instantiateModule(module);
        if (moduleInstance == null) {
            DebugLogger.getInstance().warn("Module {} could not be instantiated during dependency collection.", module.getSimpleName());
            return;
        }
        for (Class<? extends IBaseModule> dependency : moduleInstance.getDependencies()) {
            if (!loadedModules.containsKey(dependency)) {
                modulesToLoad.add(dependency);
                collectDependencies(dependency, modulesToLoad, visited);
            }
        }
        modulesToLoad.add(module);
    }

    /**
     * Shuts down the ModuleManager by unloading all modules.
     */
    public synchronized void shutdown() {
        unloadAllModules();
        DebugLogger.getInstance().log("ModuleManager has been shut down.");
    }

    /**
     * Returns a set of currently loaded modules.
     *
     * @return an unmodifiable set of loaded module classes
     */
    public Set<Class<? extends IBaseModule>> getLoadedModules() {
        return Collections.unmodifiableSet(loadedModules.keySet());
    }
}
