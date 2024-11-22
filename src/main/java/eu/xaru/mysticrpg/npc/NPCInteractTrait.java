package eu.xaru.mysticrpg.npc;

import eu.xaru.mysticrpg.utils.Utils;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class NPCInteractTrait extends Trait {

    private final NPC npc;

    public NPCInteractTrait(NPC npc) {
        super("NPCInteractTrait");
        this.npc = npc;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            npc.interact(player);
        }
    }

    @EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.getInstance().$("Please don't attack " + npc.getName() + "!")));
        }
    }
}
