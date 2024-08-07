/*package eu.xaru.mysticrpg.leveling;

import eu.xaru.mysticrpg.party.PartyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class LevelingListener implements Listener {
    private final LevelingManager levelingManager;
    private final PartyManager partyManager;

    public LevelingListener(LevelingManager levelingManager, PartyManager partyManager) {
        this.levelingManager = levelingManager;
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            int xp = levelingManager.getXpForEntity(event.getEntityType().name());

            // Share XP with party
            if (partyManager != null && partyManager.isInParty(killer)) {
                partyManager.shareXp(killer, xp);
            } else {
                levelingManager.addXp(killer, xp);
            }

            // Disable default XP drop
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        int xp = levelingManager.getXpForEntity(event.getBlock().getType().name());

        // Share XP with party
        if (partyManager != null && partyManager.isInParty(player)) {
            partyManager.shareXp(player, xp);
        } else {
            levelingManager.addXp(player, xp);
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // Cancel default XP gain
        event.setAmount(0);
    }
}
*/