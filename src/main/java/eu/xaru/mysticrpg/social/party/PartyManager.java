package eu.xaru.mysticrpg.social.party;

import eu.xaru.mysticrpg.player.leveling.LevelingManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {
    private final Map<UUID, Party> parties = new HashMap<>();
    private final LevelingManager levelingManager;

    public PartyManager(LevelingManager levelingManager) {
        this.levelingManager = levelingManager;
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader);
        parties.put(leader.getUniqueId(), party);
        return party;
    }

    public Party getParty(Player player) {
        return parties.values().stream()
                .filter(party -> party.isMember(player))
                .findFirst()
                .orElse(null);
    }

    public void disbandParty(Player leader) {
        Party party = parties.remove(leader.getUniqueId());
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage("Your party has been disbanded.");
                }
            }
        }
    }

    public void leaveParty(Player player) {
        Party party = getParty(player);
        if (party != null) {
            party.removeMember(player);
            if (party.getLeader().equals(player.getUniqueId()) || party.getMembers().isEmpty()) {
                disbandParty(player);
            } else {
                for (UUID memberId : party.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        member.sendMessage(player.getName() + " has left the party.");
                    }
                }
            }
        }
    }

    public void invitePlayer(Player inviter, Player invitee) {
        Party party = getParty(inviter);
        if (party == null) {
            party = createParty(inviter);
        }

        if (party.isFull()) {
            inviter.sendMessage("Your party is full.");
            return;
        }

        invitee.sendMessage("[Party] " + inviter.getName() + " invited you to the party \"" + party.getName() + "\" [ACCEPT] [DECLINE]");
    }

    public boolean isInParty(Player player) {
        return getParty(player) != null;
    }

    public void shareXp(Player player, int totalXp) {
        Party party = getParty(player);
        if (party != null) {
            int share = totalXp / party.getMembers().size();
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    levelingManager.addXp(member, share);
                }
            }
        }
    }
}
