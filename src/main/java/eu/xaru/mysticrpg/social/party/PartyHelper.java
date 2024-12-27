package eu.xaru.mysticrpg.social.party;

import eu.xaru.mysticrpg.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PartyHelper {

    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    private final Map<UUID, UUID> pendingInvitations = new HashMap<>();

    /**
     * Invites a player to a party.
     *
     * @param inviter The player sending the invitation.
     * @param invitee The player being invited.
     */
    public void invitePlayer(Player inviter, Player invitee) {
        if (inviter.equals(invitee)) {
            inviter.sendMessage(Utils.getInstance().$("You cannot invite yourself."));
            return;
        }

        if (playerPartyMap.containsKey(invitee.getUniqueId())) {
            inviter.sendMessage(Utils.getInstance().$(invitee.getName() + " is already in a party."));
            return;
        }

        if (pendingInvitations.containsKey(invitee.getUniqueId())) {
            inviter.sendMessage(Utils.getInstance().$(invitee.getName() + " already has a pending invitation."));
            return;
        }

        Party party = playerPartyMap.get(inviter.getUniqueId());
        if (party == null) {
            party = new Party(inviter.getUniqueId());
            playerPartyMap.put(inviter.getUniqueId(), party);
        }

        if (party.getMembers().size() >= 3) {
            inviter.sendMessage(Utils.getInstance().$("Your party is full."));
            return;
        }

        pendingInvitations.put(invitee.getUniqueId(), inviter.getUniqueId());
        inviter.sendMessage(Utils.getInstance().$("You have invited " + invitee.getName() + " to your party."));

        invitee.sendMessage(Utils.getInstance().$(inviter.getName() + " has invited you to join their party."));

        // Create clickable messages
        TextComponent acceptButton = new TextComponent("[Accept]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Click to accept the party invitation.")));

        TextComponent declineButton = new TextComponent("[Decline]");
        declineButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline"));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Click to decline the party invitation.")));

        TextComponent message = new TextComponent(" ");
        message.addExtra(acceptButton);
        message.addExtra(" ");
        message.addExtra(declineButton);

        // Send the clickable message
        invitee.spigot().sendMessage(ChatMessageType.CHAT, message);

        // Schedule invitation timeout
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), () -> {
            if (pendingInvitations.containsKey(invitee.getUniqueId())) {
                pendingInvitations.remove(invitee.getUniqueId());
                inviter.sendMessage(Utils.getInstance().$(invitee.getName() + " did not respond to your party invitation."));
                invitee.sendMessage(Utils.getInstance().$("Party invitation from " + inviter.getName() + " has expired."));
            }
        }, 1200L); // 1200 ticks = 60 seconds
    }

    /**
     * Accepts a pending party invitation.
     *
     * @param player The player accepting the invitation.
     */
    public void acceptInvitation(Player player) {
        UUID inviterUUID = pendingInvitations.remove(player.getUniqueId());
        if (inviterUUID == null) {
            player.sendMessage(Utils.getInstance().$("You have no pending party invitations."));
            return;
        }

        Party party = playerPartyMap.get(inviterUUID);
        if (party == null) {
            party = new Party(inviterUUID);
            playerPartyMap.put(inviterUUID, party);
        }

        if (party.getMembers().size() >= 3) {
            player.sendMessage(Utils.getInstance().$("The party is already full."));
            return;
        }

        party.addMember(player.getUniqueId());
        playerPartyMap.put(player.getUniqueId(), party);

        // Notify party members
        for (UUID memberUUID : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(Utils.getInstance().$(player.getName() + " has joined the party."));
            }
        }
    }

    /**
     * Declines a pending party invitation.
     *
     * @param player The player declining the invitation.
     */
    public void declineInvitation(Player player) {
        UUID inviterUUID = pendingInvitations.remove(player.getUniqueId());
        if (inviterUUID == null) {
            player.sendMessage(Utils.getInstance().$("You have no pending party invitations."));
            return;
        }

        Player inviter = Bukkit.getPlayer(inviterUUID);
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(Utils.getInstance().$(player.getName() + " has declined your party invitation."));
        }

        player.sendMessage(Utils.getInstance().$("You have declined the party invitation from " + (inviter != null ? inviter.getName() : "a player") + "."));
    }

    /**
     * Leaves the current party.
     *
     * @param player The player leaving the party.
     */
    public void leaveParty(Player player) {
        Party party = playerPartyMap.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Utils.getInstance().$("You are not in a party."));
            return;
        }

        party.removeMember(player.getUniqueId());
        playerPartyMap.remove(player.getUniqueId());
        player.sendMessage(Utils.getInstance().$("You have left the party."));

        // Notify remaining party members
        for (UUID memberUUID : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(Utils.getInstance().$(player.getName() + " has left the party."));
            }
        }

        // If the leader leaves or the party is empty, disband the party
        if (party.getLeader().equals(player.getUniqueId()) || party.getMembers().isEmpty()) {
            disbandParty(party);
        } else if (party.getLeader() == null) {
            // Assign a new leader if the leader left
            UUID newLeaderUUID = party.getMembers().iterator().next();
            party.setLeader(newLeaderUUID);
            Player newLeader = Bukkit.getPlayer(newLeaderUUID);
            if (newLeader != null && newLeader.isOnline()) {
                newLeader.sendMessage(Utils.getInstance().$("You are now the party leader."));
            }
        }
    }

    /**
     * Disbands a party.
     *
     * @param party The party to disband.
     */
    private void disbandParty(Party party) {
        for (UUID memberUUID : party.getMembers()) {
            playerPartyMap.remove(memberUUID);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(Utils.getInstance().$("Your party has been disbanded."));
            }
        }
    }

    /**
     * Handles player disconnection.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        leaveParty(player);
    }

    /**
     * Handles entity death for XP sharing.
     *
     * @param event  The EntityDeathEvent.
     * @param killer The player who killed the entity.
     */
    public void handleEntityDeath(EntityDeathEvent event, Player killer) {
        // XP sharing is handled in MobManager
    }

    /**
     * Gets the party of a player.
     *
     * @param playerUUID The UUID of the player.
     * @return The Party object or null if not in a party.
     */
    public Party getParty(UUID playerUUID) {
        return playerPartyMap.get(playerUUID);
    }

    /**
     * Kicks a player from the party.
     *
     * @param leader       The party leader.
     * @param targetPlayer The player to kick.
     */
    public void kickPlayer(Player leader, Player targetPlayer) {
        Party party = playerPartyMap.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(Utils.getInstance().$("You are not in a party."));
            return;
        }

        if (!party.getLeader().equals(leader.getUniqueId())) {
            leader.sendMessage(Utils.getInstance().$("You are not the party leader."));
            return;
        }

        if (!party.getMembers().contains(targetPlayer.getUniqueId())) {
            leader.sendMessage(Utils.getInstance().$(targetPlayer.getName() + " is not in your party."));
            return;
        }

        party.removeMember(targetPlayer.getUniqueId());
        playerPartyMap.remove(targetPlayer.getUniqueId());

        leader.sendMessage(Utils.getInstance().$("You have kicked " + targetPlayer.getName() + " from the party."));
        targetPlayer.sendMessage(Utils.getInstance().$("You have been kicked from the party by " + leader.getName() + "."));

        // Notify remaining party members
        for (UUID memberUUID : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(Utils.getInstance().$(targetPlayer.getName() + " has been kicked from the party."));
            }
        }
    }
}
