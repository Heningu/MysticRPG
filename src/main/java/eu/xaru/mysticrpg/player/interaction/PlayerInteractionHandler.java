package eu.xaru.mysticrpg.player.interaction;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.EventManager;
import eu.xaru.mysticrpg.social.friends.FriendsHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlayerInteractionHandler implements IBaseModule {

    private final EventManager eventManager = new EventManager(JavaPlugin.getPlugin(MysticCore.class));

    @Override
    public void initialize() throws Exception {
        eventManager.registerEvent(PlayerInteractEntityEvent.class, event -> {
            if(event.getRightClicked() instanceof Player){
                Player p = event.getPlayer();
                Player target = (Player) event.getRightClicked();
                if(p.isSneaking()){
                    //open interaction interview
                    PlayerInteractionMenu.openPlayerInteractionMenu(p, target);

                }
            }
        });

        eventManager.registerEvent(InventoryDragEvent.class, event -> {
            if(event.getView().getTitle().equals(PlayerInteractionMenu.interactionInventoryName)){
                event.setCancelled(true);
            }
        });
        eventManager.registerEvent(InventoryClickEvent.class, event -> {
            if(event.getView().getTitle().equals(PlayerInteractionMenu.interactionInventoryName)){
                Inventory inv = event.getInventory();
                switch (event.getSlot()) {
                    case 1:
                        //Party Invite Slot

                        break;
                    case 4:
                        //Trade Invite Slot

                        break;
                    case 7:
                        //Friend Add Slot
                        break;
                    default:
                        //PlaceholderSlots
                        event.setCancelled(true);
                        break;
                }
            }
        });
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void unload() throws Exception {

    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();
    }

    @Override
    public EModulePriority getPriority() {
        return null;
    }
}
