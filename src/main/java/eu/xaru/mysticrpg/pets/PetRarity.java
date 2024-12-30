package eu.xaru.mysticrpg.pets;

import org.bukkit.ChatColor;

public enum PetRarity {
    COMMON(ChatColor.WHITE),
    EPIC(ChatColor.DARK_PURPLE),
    LEGENDARY(ChatColor.GOLD),
    UNIQUE(ChatColor.AQUA);

    private final ChatColor color;

    PetRarity(ChatColor color) {
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }
}
