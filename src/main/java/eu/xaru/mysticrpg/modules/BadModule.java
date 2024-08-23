//package eu.xaru.mysticrpg.modules;
//
//import eu.xaru.mysticrpg.enums.EModulePriority;
//import eu.xaru.mysticrpg.interfaces.IBaseModule;
//
//import java.util.Collections;
//import java.util.List;
//
//public class BadModule implements IBaseModule {
//
//    private long delay = 50; // Initial delay in milliseconds
//    private final long MAX_DELAY = 5000; // Maximum delay to simulate stalling
//
//    @Override
//    public void initialize() throws Exception {
//        simulateStallingBehavior("initialize");
//    }
//
//    @Override
//    public void start() throws Exception {
//        simulateStallingBehavior("start");
//    }
//
//    private void simulateStallingBehavior(String phase) {
//        new Thread(() -> {
//            while (true) {
//                try {
//                    // Simulate work
//                    long startTime = System.currentTimeMillis();
//
//                    // Artificially slow down the module over time
//                    Thread.sleep(delay);
//
//                    // Log execution details
//                    System.out.println("BadModule " + phase + " phase executed with a delay of " + delay + " ms.");
//
//                    // Increase delay exponentially to simulate stalling
//                    delay = Math.min(delay * 2, MAX_DELAY);
//
//                    long endTime = System.currentTimeMillis();
//                    System.out.println("BadModule " + phase + " phase completed in " + (endTime - startTime) + " ms.");
//
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break; // Exit if interrupted (e.g., during module restart)
//                }
//            }
//        }).start();
//    }
//
//    @Override
//    public void stop() throws Exception {
//        // No proper cleanup, intentionally poorly managed
//        System.out.println("BadModule stopped without proper cleanup.");
//    }
//
//    @Override
//    public void unload() throws Exception {
//        // No proper resource management, simulating a bad module
//        System.out.println("BadModule unloaded without proper resource management.");
//    }
//
//    @Override
//    public List<Class<? extends IBaseModule>> getDependencies() {
//        return Collections.emptyList(); // No dependencies
//    }
//
//    @Override
//    public EModulePriority getPriority() {
//        return EModulePriority.LOW; // Lowest priority, as this is a poorly designed module
//    }
//}
