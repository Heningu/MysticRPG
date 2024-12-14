package eu.xaru.mysticrpg.player.interaction.trading;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a unique key for a trade request based on initiator and target.
 */
public class TradeRequestKey {
    private final UUID initiatorUUID;
    private final UUID targetUUID;

    public TradeRequestKey(UUID initiatorUUID, UUID targetUUID) {
        this.initiatorUUID = initiatorUUID;
        this.targetUUID = targetUUID;
    }

    public UUID getInitiatorUUID() {
        return initiatorUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TradeRequestKey that = (TradeRequestKey) o;
        return initiatorUUID.equals(that.initiatorUUID) && targetUUID.equals(that.targetUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiatorUUID, targetUUID);
    }
}
