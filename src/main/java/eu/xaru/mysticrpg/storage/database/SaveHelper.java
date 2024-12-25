package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * SaveHelper handles (de)serialization of ItemStack to/from Base64.
 */
public class SaveHelper {

    public static String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos);
            out.writeObject(item);
            out.close();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to serialize ItemStack.", e);
            return null;
        }
    }

    public static ItemStack itemStackFromBase64(String data) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream in = new BukkitObjectInputStream(bis);
            ItemStack item = (ItemStack) in.readObject();
            in.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            DebugLogger.getInstance().error("Failed to deserialize ItemStack.", e);
            return null;
        }
    }
}
