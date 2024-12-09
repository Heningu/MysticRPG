package eu.xaru.mysticrpg.guis.invui.inventory.event;

public interface UpdateReason {
    
    /**
     * An {@link UpdateReason} that suppresses all event calls.
     */
    UpdateReason SUPPRESSED = new UpdateReason() {};
    
}
