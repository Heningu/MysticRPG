package eu.xaru.mysticrpg.interfaces;

import eu.xaru.mysticrpg.enums.EModulePriority;
import java.util.List;

public interface IBaseModule {

    /**
     * Called when the module is initialized.
     * This is where any setup or configuration should occur.
     *
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Called when the module is started.
     * This should handle any logic required to begin the module's operation.
     *
     * @throws Exception if the module fails to start
     */
    void start() throws Exception;

    /**
     * Called when the module is stopped.
     * This should handle any logic required to cease the module's operation.
     *
     * @throws Exception if the module fails to stop
     */
    void stop() throws Exception;

    /**
     * Called when the module is unloaded.
     * This should handle any cleanup, freeing resources, or other final operations.
     *
     * @throws Exception if the module fails to unload
     */
    void unload() throws Exception;

    /**
     * Returns a list of dependencies that this module requires.
     * The dependencies will be loaded and initialized before this module.
     *
     * @return a list of module classes that this module depends on
     */
    List<Class<? extends IBaseModule>> getDependencies();

    /**
     * Returns the priority of this module.
     * Modules with higher priority will be initialized and started before others.
     *
     * @return the priority of the module
     */
    EModulePriority getPriority();
}
