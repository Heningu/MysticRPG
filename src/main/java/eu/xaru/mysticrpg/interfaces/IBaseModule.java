package eu.xaru.mysticrpg.interfaces;

import eu.xaru.mysticrpg.enums.EModulePriority;
import java.util.Collections;
import java.util.List;

public interface IBaseModule {

    /**
     * Called when the module is initialized.
     * Perform setup or configuration here.
     *
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Called when the module is started.
     * Begin the module's operations here.
     *
     * @throws Exception if the module fails to start
     */
    void start() throws Exception;

    /**
     * Called when the module is stopped.
     * Cease the module's operations here.
     *
     * @throws Exception if the module fails to stop
     */
    void stop() throws Exception;

    /**
     * Called when the module is unloaded.
     * Perform cleanup and resource release here.
     *
     * @throws Exception if the module fails to unload
     */
    void unload() throws Exception;

    /**
     * Returns a list of dependencies that this module requires.
     * Dependencies will be loaded and initialized before this module.
     *
     * @return a list of module classes that this module depends on
     */
    default List<Class<? extends IBaseModule>> getDependencies() {
        return Collections.emptyList();
    }

    /**
     * Returns the priority of this module.
     * Modules with higher priority are loaded and started before others.
     *
     * @return the priority of the module
     */
    default EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    /**
     * Indicates whether the module should be lazily loaded.
     * Lazy modules are only loaded when explicitly required.
     *
     * @return true if the module is lazy-loaded, false otherwise
     */
    default boolean isLazy() {
        return false;
    }
}
