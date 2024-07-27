package eu.xaru.mysticrpg.ui;

import org.bukkit.entity.Player;

public class UiProvider {

    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(message));
    }
}
