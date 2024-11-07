package eu.xaru.mysticrpg.social.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new HashSet<>();

    public Party(UUID leaderUUID) {
        this.leader = leaderUUID;
        this.members.add(leaderUUID);
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID memberUUID) {
        members.add(memberUUID);
    }

    public void removeMember(UUID memberUUID) {
        members.remove(memberUUID);
    }

    /**
     * Gets the size of the party.
     *
     * @return The number of members in the party.
     */
    public int getPartySize() {
        return members.size();
    }
}
