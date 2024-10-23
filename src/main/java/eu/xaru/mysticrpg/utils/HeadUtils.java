package eu.xaru.mysticrpg.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures.*;
import org.bukkit.profile.PlayerProfile.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for creating custom player heads with specific textures.
 */
public class HeadUtils {

    // Reusing the same "random" UUID for consistency across heads.
    private static final UUID RANDOM_UUID = UUID.fromString("92864445-51c5-4c3b-9039-517c9927d1b4");

    /**
     * Creates a player head with a custom Base64-encoded texture.
     *
     * @param base64      The Base64 string representing the texture.
     * @param displayName The display name for the head.
     * @return The customized player head ItemStack.
     */
    public static ItemStack createCustomHead(String base64, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName(ChatColor.YELLOW + displayName);
            PlayerProfile profile = getProfileFromBase64(base64);
            if (profile != null) {
                skullMeta.setOwnerProfile(profile);
            } else {
                skullMeta.setDisplayName(ChatColor.RED + "Invalid Texture");
            }
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    /**
     * Creates a player head based on the EntityType.
     * This method maps each EntityType to a predefined Base64 texture.
     *
     * @param entityType  The EntityType of the mob.
     * @param displayName The display name for the head.
     * @return The customized player head ItemStack.
     */
    public static ItemStack createEntityHead(org.bukkit.entity.EntityType entityType, String displayName) {
        String base64 = getBase64ForEntity(entityType);
        if (base64 == null) {
            // Fallback to a default head if no texture is found
            ItemStack defaultHead = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) defaultHead.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Unknown Mob");
                defaultHead.setItemMeta(meta);
            }
            return defaultHead;
        }
        return createCustomHead(base64, displayName);
    }

    /**
     * Converts a Base64-encoded JSON string to a URL.
     *
     * @param base64 The Base64 string representing the texture JSON.
     * @return The URL extracted from the Base64 string, or null if invalid.
     */
    public static URL getUrlFromBase64(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            // Extract the URL from the JSON structure.
            // Example JSON: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/your_texture_hash"}}}
            int startIndex = decoded.indexOf("\"url\":\"") + 7;
            int endIndex = decoded.indexOf("\"}}}", startIndex);
            String url = decoded.substring(startIndex, endIndex);
            return new URL(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PlayerProfile from a Base64-encoded texture string.
     *
     * @param base64 The Base64 string representing the texture.
     * @return The PlayerProfile with the custom texture, or null if invalid.
     */
    private static PlayerProfile getProfileFromBase64(String base64) {
        URL textureUrl = getUrlFromBase64(base64);
        if (textureUrl == null) {
            return null;
        }
        PlayerProfile profile = Bukkit.createPlayerProfile(RANDOM_UUID);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(textureUrl);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Retrieves the Base64 texture string for a given EntityType.
     * Populate this method with actual Base64 strings for each EntityType.
     *
     * @param entityType The EntityType of the mob.
     * @return The Base64 texture string or null if not defined.
     */
    private static String getBase64ForEntity(org.bukkit.entity.EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
                return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzYxODAxZjNkYzBhZjEwY2Q0ZjU2N2RmMjYzNmZjZTAxZWIxN2U2MTEyODZlNGRkZDFhNGJiZjQ2ZjAzN2JkIn19fQ=="; // Zombie
            case SKELETON:
                return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2E0ZjE0MDQ4MDJkY2UyNjM5NzNkMTA3ZjI1YjEyMTZkZGU1NzMwNGY2YjZjMzUwMzY0M2UwNDc0ODJlNDdkMiJ9fX0="; // Skeleton
            case CREEPER:
                return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY0M2M3NmNjYmYzOGY1NzZkM2M3ZDA3YjM0MDAxMzUyOTViNjQ2YjE1YWE2ZjAwOTc3NmU4Yzg0NmU0NDczOSJ9fX0="; // Creeper
            // Add more cases for other EntityTypes with their corresponding Base64 textures
            default:
                return null;
        }
    }
}
