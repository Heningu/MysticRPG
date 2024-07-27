package eu.xaru.mysticrpg.content.modules;

import eu.xaru.mysticrpg.Main;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Collections;
import java.util.UUID;

public class DamageIndicators implements Module, Listener {

    private final Main plugin;

    public DamageIndicators(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "DamageIndicators";
    }

    @Override
    public boolean load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Location loc = event.getEntity().getLocation().add(0, 1, 0);
        double damage = event.getDamage();
        String hologramId = "MysticRPG_" + UUID.randomUUID();
        Hologram hologram = DHAPI.createHologram(hologramId, loc, false, Collections.singletonList(ChatColor.RED + "" + damage));
        Bukkit.getScheduler().runTaskLater(plugin, () -> DHAPI.removeHologram(hologramId), 40L); // Remove hologram after 2 seconds
    }
}
