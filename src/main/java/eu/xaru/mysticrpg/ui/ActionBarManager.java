package eu.xaru.mysticrpg.ui;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ActionBarManager {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;

    public ActionBarManager(Main plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void updateActionBar(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        int hp = data.getHp();
        int mana = data.getMana();

        String actionBarText = "HP: " + hp + " | Mana: " + mana;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarText));
    }

    public void updateActionBarOnDamage(Player player) {
        updateActionBar(player);
    }
}
