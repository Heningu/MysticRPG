package eu.xaru.mysticrpg.admin.players;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.admin.features.PlayerFeature;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Date;

public class PlayerBanFeature extends PlayerFeature {

    public PlayerBanFeature(Main plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, Player target) {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        if (banList.isBanned(target.getName())) {
            banList.pardon(target.getName());
            player.sendMessage(target.getName() + " has been unbanned.");
        } else {
            banList.addBan(target.getName(), "You have been banned by an admin.", (Date) null, null);
            target.kickPlayer("You have been banned by an admin.");
            player.sendMessage(target.getName() + " has been banned.");
        }
    }

    public ItemStack getPlayerHead(Player target) {
        ItemStack head = new ItemStack(org.bukkit.Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setOwningPlayer(target);
        head.setItemMeta(meta);
        return head;
    }
}
