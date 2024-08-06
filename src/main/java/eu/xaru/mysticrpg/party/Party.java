package eu.xaru.mysticrpg.party;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    private final UUID leader;
    private final List<UUID> members;
    private final String name;

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        this.members = new ArrayList<>();
        this.members.add(this.leader);
        this.name = leader.getName();
    }

    public UUID getLeader() {
        return leader;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    public void addMember(Player player) {
        if (members.size() < 3) {
            members.add(player.getUniqueId());
        }
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }

    public boolean isFull() {
        return members.size() >= 3;
    }
}
