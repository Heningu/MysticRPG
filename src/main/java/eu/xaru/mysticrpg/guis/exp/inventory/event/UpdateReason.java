package eu.xaru.mysticrpg.guis.exp.inventory.event;

public interface UpdateReason {
    
    /**
     * An {@link UpdateReason} that suppresses all event calls.
     */
    UpdateReason SUPPRESSED = new UpdateReason() {};
    
}
