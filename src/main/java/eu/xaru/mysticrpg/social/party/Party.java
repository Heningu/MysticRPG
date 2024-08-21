package eu.xaru.mysticrpg.social.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Party {
    private final UUID leader;
    private final List<UUID> members;
    private final Map<UUID, String> roles;
    private final String name;

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        this.members = new ArrayList<>();
        this.roles = new HashMap<>();
        this.members.add(this.leader);
        this.roles.put(this.leader, "Leader");
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
            roles.put(player.getUniqueId(), "Member"); // Default role
        }
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
        roles.remove(player.getUniqueId());
    }

    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }

    public boolean isFull() {
        return members.size() >= 3;
    }

    // Method to return a formatted list of party members with their roles
    public List<String> getMemberListWithRoles() {
        List<String> memberList = new ArrayList<>();
        // Ensure leader is always first
        memberList.add("1: " + Bukkit.getPlayer(leader).getName() + " (Leader)");
        int index = 2;
        for (UUID memberId : members) {
            if (!memberId.equals(leader)) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    String role = roles.getOrDefault(memberId, "Member");
                    memberList.add(index + ": " + member.getName() + " (" + role + ")");
                    index++;
                }
            }
        }
        return memberList;
    }
}
