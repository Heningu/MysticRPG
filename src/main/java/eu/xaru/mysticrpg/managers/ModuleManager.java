package eu.xaru.mysticrpg.managers;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import eu.xaru.mysticrpg.utils.DeadlockDetector;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ModuleManager {
    private final ConcurrentHashMap<Class<? extends IBaseModule>, WeakReference<IBaseModule>> loadedModules = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Class<? extends IBaseModule>> loadingOrder = new CopyOnWriteArrayList<>();
    private final Set<Class<? extends IBaseModule>> currentlyLoadingModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ReferenceQueue<IBaseModule> referenceQueue = new ReferenceQueue<>();
    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private final DebugLoggerModule logger;
    private final DeadlockDetector deadlockDetector;

    private static volatile ModuleManager instance;

    private ModuleManager() {
        logger = getModuleInstance(DebugLoggerModule.class);
        deadlockDetector = new DeadlockDetector(10, TimeUnit.SECONDS, this, logger);
        deadlockDetector.start();
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

    public synchronized void loadAllModules() {
        unloadAllModules(); // Ensure previous modules are unloaded
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("eu.xaru.mysticrpg")  // Specify the package to scan
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesImplementing(IBaseModule.class.getName())) {
                @SuppressWarnings("unchecked")
                Class<? extends IBaseModule> moduleClass = (Class<? extends IBaseModule>) classInfo.loadClass();
                loadModule(moduleClass);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Failed to load modules. Exception: " + e.getMessage(), e, null);
            } else {
                e.printStackTrace();
            }
        }

        startModules();
    }

    public synchronized void loadModule(Class<? extends IBaseModule> moduleClass) throws Exception {
        if (loadedModules.containsKey(moduleClass) || currentlyLoadingModules.contains(moduleClass)) return;

        currentlyLoadingModules.add(moduleClass);
        Constructor<? extends IBaseModule> constructor = moduleClass.getDeclaredConstructor();
        constructor.setAccessible(true);  // Force access to the private constructor
        IBaseModule module = constructor.newInstance();

        for (Class<? extends IBaseModule> dependency : module.getDependencies()) {
            loadModule(dependency);
        }

        insertModuleInOrder(moduleClass, module.getPriority());
        module.initialize();
        loadedModules.put(moduleClass, new WeakReference<>(module, referenceQueue));

        if (logger != null) {
            logger.log(Level.INFO, "Module " + moduleClass.getSimpleName() + " initialized successfully.", 0);
        }
        currentlyLoadingModules.remove(moduleClass);
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

    public synchronized void startModules() {
        for (Class<? extends IBaseModule> moduleClass : loadingOrder) {
            try {
                IBaseModule module = getModuleInstance(moduleClass);
                if (module != null) {
                    module.start();
                    if (logger != null) {
                        logger.log(Level.INFO, "Module " + moduleClass.getSimpleName() + " started.", 0);
                    }
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Failed to start module " + moduleClass.getSimpleName() + ". Exception: " + e.getMessage(), e, null);
                }
            }
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
                if (logger != null) {
                    logger.log(Level.INFO, "Module " + moduleClass.getSimpleName() + " stopped and unloaded.", 0);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Failed to stop or unload module " + moduleClass.getSimpleName() + ". Exception: " + e.getMessage(), e, null);
                }
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

    public synchronized void monitorAndRepairModules() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapMemoryUsage.getUsed();
        long maxMemory = heapMemoryUsage.getMax();

        if ((double) usedMemory / maxMemory > 0.8) {
            if (logger != null) {
                logger.warn("High memory usage detected. Attempting to identify and repair modules.");
            }

            reloadModules();
        }

        cleanupReferences();
    }

    private void reloadModules() {
        for (Class<? extends IBaseModule> moduleClass : loadedModules.keySet()) {
            try {
                stopAndUnloadModule(moduleClass);
                loadModule(moduleClass);
                if (logger != null) {
                    logger.log(Level.INFO, "Module " + moduleClass.getSimpleName() + " reloaded successfully.", 0);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Failed to reload module " + moduleClass.getSimpleName(), e, null);
                }
            }
        }
    }

    private void cleanupResources() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    if (logger != null) logger.error("Thread pool did not terminate.", null, null);
                }
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

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
        deadlockDetector.shutdown();
        unloadAllModules();
        cleanupResources();
    }

    public Set<Class<? extends IBaseModule>> getLoadedModules() {
        return loadedModules.keySet();
    }
}
