package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DeadlockDetector {

    private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long period;
    private final TimeUnit unit;
    private final ModuleManager moduleManager;
    private final DebugLoggerModule logger;

    public DeadlockDetector(long period, TimeUnit unit, ModuleManager moduleManager, DebugLoggerModule logger) {
        this.period = period;
        this.unit = unit;
        this.moduleManager = moduleManager;
        this.logger = logger;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            long[] deadlockedThreadIds = mbean.findDeadlockedThreads();

            if (deadlockedThreadIds != null) {
                ThreadInfo[] threadInfos = mbean.getThreadInfo(deadlockedThreadIds);
                handleDeadlock(threadInfos);
            }
        }, period, period, unit);
    }

    private void handleDeadlock(ThreadInfo[] deadlockedThreads) {
        logError("Deadlock detected! Analyzing threads involved...");

        for (ThreadInfo threadInfo : deadlockedThreads) {
            if (threadInfo != null) {
                logError("Thread: " + threadInfo.getThreadName() + " (ID: " + threadInfo.getThreadId() + ") is involved in a deadlock.");
                logError("Thread state: " + threadInfo.getThreadState());
                logError("Locked resource: " + threadInfo.getLockName());

                for (StackTraceElement ste : threadInfo.getStackTrace()) {
                    logError("\t at " + ste.toString().trim());
                }

                // Check if any module is involved
                checkModuleInvolvement(threadInfo);
            }
        }
    }

    private void checkModuleInvolvement(ThreadInfo threadInfo) {
        for (Class<? extends IBaseModule> moduleClass : moduleManager.getLoadedModules()) {
            IBaseModule module = moduleManager.getModuleInstance(moduleClass);

            if (module != null && threadInfo.getThreadName().contains(moduleClass.getSimpleName())) {
                logError("Module " + moduleClass.getSimpleName() + " seems to be involved in the deadlock.");
                logInfo("Attempting to reload module " + moduleClass.getSimpleName() + " to resolve the deadlock...");

                // Attempt to reload the module
                moduleManager.stopAndUnloadModule(moduleClass);
                try {
                    moduleManager.loadModule(moduleClass);
                } catch (Exception e) {
                    logError("Failed to reload module " + moduleClass.getSimpleName(), e);
                }
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void logError(String message, Exception e) {
        if (logger != null) {
            logger
                    .error(message + " Exception: " + e.getMessage(), e, null);
        }
    }

    private void logError(String message) {
        if (logger != null) {
            logger.error(message);
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.log(Level.INFO, message, 0);
        }
    }
}
