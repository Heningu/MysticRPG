package eu.xaru.mysticrpg.managers;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ModuleManager {
    private final ConcurrentHashMap<Class<? extends IBaseModule>, WeakReference<IBaseModule>> loadedModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends IBaseModule>, LinkedList<Long>> executionTimes = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Class<? extends IBaseModule>> loadingOrder = new CopyOnWriteArrayList<>();
    private final Set<Class<? extends IBaseModule>> currentlyLoadingModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private DebugLoggerModule logger;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final ReferenceQueue<IBaseModule> referenceQueue = new ReferenceQueue<>();

    private static volatile ModuleManager instance;

    private static final int SAMPLE_SIZE = 3;  // Adjusted to require fewer data points
    private static final long STALL_THRESHOLD = 4000_000_000L;  // Threshold for detecting a stall
    private final int MONITOR_INTERVAL = 5; // Interval in seconds for monitoring

    private ModuleManager() {
        startMonitoring();
    }

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

    private void startMonitoring() {
        // Start periodic sampling to collect execution times continuously
        monitorExecutor.scheduleAtFixedRate(this::monitorModules, 0, MONITOR_INTERVAL, TimeUnit.SECONDS);
    }

    private void monitorModules() {
        for (Class<? extends IBaseModule> moduleClass : loadedModules.keySet()) {
            checkModuleExecutionTime(moduleClass);
        }
    }

    private void checkModuleExecutionTime(Class<? extends IBaseModule> moduleClass) {
        LinkedList<Long> times = executionTimes.get(moduleClass);
        if (times != null && times.size() >= SAMPLE_SIZE) {
            long averageTime = times.stream().mapToLong(Long::longValue).sum() / times.size();
            if (logger != null) {
                logger.log("Average execution time for " + moduleClass.getSimpleName() + ": " + (averageTime / 1_000_000) + " ms.", Level.INFO);
            }

            if (averageTime > STALL_THRESHOLD) {
                logWarn("Module " + moduleClass.getSimpleName() + " is stalling with an average execution time of " + (averageTime / 1_000_000) + " ms.");
                restartModule(moduleClass);
            } else {
                logInfo("Module " + moduleClass.getSimpleName() + " is operating within normal limits.");
            }
        } else {
           // logInfo("Not enough data points yet for " + moduleClass.getSimpleName() + ". Currently recorded: " + (times != null ? times.size() : 0));
        }
    }

    private void restartModule(Class<? extends IBaseModule> moduleClass) {
        try {
            logWarn("Restarting module " + moduleClass.getSimpleName() + " due to stalling.");
            stopAndUnloadModule(moduleClass);
            loadModule(moduleClass);
            startModule(moduleClass);
        } catch (Exception e) {
            logError("Failed to restart module " + moduleClass.getSimpleName(), e);
        }
    }

    public synchronized void loadAllModules() {
        unloadAllModules(); // Ensure previous modules are unloaded

        // Initialize and start the logger first
        loadAndStartLogger();

        // Then load and start all other modules based on priority
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("eu.xaru.mysticrpg")
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesImplementing(IBaseModule.class.getName())) {
                @SuppressWarnings("unchecked")
                Class<? extends IBaseModule> moduleClass = (Class<? extends IBaseModule>) classInfo.loadClass();
                if (moduleClass != DebugLoggerModule.class) {
                    loadModule(moduleClass);
                }
            }
        } catch (Exception e) {
            logError("Failed to load modules.", e);
        }

        startModules();
    }

    private void loadAndStartLogger() {
        try {
            loadModule(DebugLoggerModule.class);
            logger = getModuleInstance(DebugLoggerModule.class);
            if (logger != null) {
                logger.start();
                logger.log(Level.INFO, "Logger initialized as the first module.", 0);
            } else {
                System.out.println("Failed to initialize the logger!");
            }
        } catch (Exception e) {
            logError("Failed to load or start logger", e);
        }
    }

    public synchronized void loadModule(Class<? extends IBaseModule> moduleClass) throws Exception {
        if (loadedModules.containsKey(moduleClass) || currentlyLoadingModules.contains(moduleClass)) return;

        currentlyLoadingModules.add(moduleClass);
        Constructor<? extends IBaseModule> constructor = moduleClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        IBaseModule module = constructor.newInstance();

        for (Class<? extends IBaseModule> dependency : module.getDependencies()) {
            loadModule(dependency);
        }

        insertModuleInOrder(moduleClass, module.getPriority());

        long startTime = System.nanoTime();
        module.initialize();
        long duration = System.nanoTime() - startTime;
        recordExecutionTime(moduleClass, duration);

        loadedModules.put(moduleClass, new WeakReference<>(module, referenceQueue));

        currentlyLoadingModules.remove(moduleClass);
    }

    private void recordExecutionTime(Class<? extends IBaseModule> moduleClass, long duration) {
        executionTimes.computeIfAbsent(moduleClass, k -> new LinkedList<>());
        LinkedList<Long> times = executionTimes.get(moduleClass);
        if (times.size() >= SAMPLE_SIZE) {
            times.poll(); // Remove the oldest sample if we have enough samples
        }
        times.add(duration);
        if (logger != null) {
            logger.log("Recorded execution time for " + moduleClass.getSimpleName() + ": " + (duration / 1_000_000) + " ms.", Level.INFO);
        }
    }

    private void insertModuleInOrder(Class<? extends IBaseModule> moduleClass, EModulePriority priority) {
        synchronized (loadingOrder) {
            int index = 0;
            for (Class<? extends IBaseModule> clazz : loadingOrder) {
                EModulePriority existingPriority = getModuleInstance(clazz).getPriority();
                if (existingPriority.ordinal() > priority.ordinal()) {
                    index++;
                } else {
                    break;
                }
            }
            loadingOrder.add(index, moduleClass);
        }
    }

    private void startModule(Class<? extends IBaseModule> moduleClass) {
        IBaseModule module = getModuleInstance(moduleClass);
        if (module != null) {
            long startTime = System.nanoTime();
            try {
                module.start();
                long duration = System.nanoTime() - startTime;
                recordExecutionTime(moduleClass, duration);
            } catch (Exception e) {
                logError("Failed to start module " + moduleClass.getSimpleName(), e);
            }
        }
    }

    public synchronized void startModules() {
        for (Class<? extends IBaseModule> moduleClass : loadingOrder) {
            startModule(moduleClass);
        }
    }

    public synchronized void stopAndUnloadModule(Class<? extends IBaseModule> moduleClass) {
        WeakReference<IBaseModule> moduleRef = loadedModules.remove(moduleClass);
        IBaseModule module = moduleRef != null ? moduleRef.get() : null;

        if (module != null) {
            try {
                module.stop();
                module.unload();
                loadingOrder.remove(moduleClass);
                logInfo("Module " + moduleClass.getSimpleName() + " stopped and unloaded.");
            } catch (Exception e) {
                logError("Failed to stop or unload module " + moduleClass.getSimpleName(), e);
            }
        }

        cleanupResources();
    }

    public synchronized void unloadAllModules() {
        List<Class<? extends IBaseModule>> modulesToUnload = new ArrayList<>(loadingOrder);
        Collections.reverse(modulesToUnload); // Unload in reverse order of loading

        for (Class<? extends IBaseModule> moduleClass : modulesToUnload) {
            stopAndUnloadModule(moduleClass);
        }

        cleanupResources();
        if (logger != null) {
            logger.log(Level.INFO, "All modules have been unloaded.", 0);
        }
    }

    public synchronized <T extends IBaseModule> T getModuleInstance(Class<T> moduleClass) {
        WeakReference<IBaseModule> moduleRef = loadedModules.get(moduleClass);
        return moduleClass.cast(moduleRef != null ? moduleRef.get() : null);
    }

    private void cleanupResources() {
        cleanupReferences();
    }

    private void cleanupReferences() {
        WeakReference<? extends IBaseModule> ref;
        while ((ref = (WeakReference<? extends IBaseModule>) referenceQueue.poll()) != null) {
            if (logger != null) {
                logger.log(Level.INFO, "Cleaning up weak reference to module.", 0);
            }
            ref.clear();
        }
    }

    public void shutdown() {
        monitorExecutor.shutdownNow();
        unloadAllModules();
        cleanupResources();
    }

    public Set<Class<? extends IBaseModule>> getLoadedModules() {
        return loadedModules.keySet();
    }

    private void logError(String message, Exception e) {
        if (logger != null) {
            logger.error(message + " Exception: " + e.getMessage(), e, null);
        }
    }

    private void logError(String message) {
        if (logger != null) {
            logger.error(message);
        }
    }

    private void logWarn(String message) {
        if (logger != null) {
            logger.warn(message);
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.log(Level.INFO, message, 0);
        }
    }
}