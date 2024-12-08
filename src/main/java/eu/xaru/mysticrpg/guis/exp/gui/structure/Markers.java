package eu.xaru.mysticrpg.guis.exp.gui.structure;

import org.jetbrains.annotations.NotNull;
import eu.xaru.mysticrpg.guis.exp.gui.AbstractPagedGui;
import eu.xaru.mysticrpg.guis.exp.gui.AbstractScrollGui;
import eu.xaru.mysticrpg.guis.exp.gui.AbstractTabGui;

/**
 * Registry class for default markers
 */
public class Markers {
    
    /**
     * The marker for horizontal content list slots in {@link AbstractPagedGui PagedGuis},
     * {@link AbstractScrollGui ScrollGuis} and {@link AbstractTabGui TabGuis}
     */
    public static final @NotNull Marker CONTENT_LIST_SLOT_HORIZONTAL = new Marker(true);
    
    /**
     * The marker for vertical content list slots in {@link AbstractPagedGui PagedGuis},
     * {@link AbstractScrollGui ScrollGuis} and {@link AbstractTabGui TabGuis}
     */
    public static final @NotNull Marker CONTENT_LIST_SLOT_VERTICAL = new Marker(false);
    
}
