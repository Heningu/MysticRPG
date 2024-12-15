package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;

/**
 * SaveHelper handles serialization and deserialization of complex objects like ItemStack.
 */
public class SaveHelper {

    /**
     * Serializes an ItemStack to a Base64 encoded string.
     *
     * @param item The ItemStack to serialize.
     * @return The Base64 encoded string.
     */
    public static String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            DebugLogger.getInstance().error("Failed to serialize ItemStack to Base64.", e);
            return null;
        }
    }

    /**
     * Deserializes an ItemStack from a Base64 encoded string.
     *
     * @param data The Base64 encoded string.
     * @return The deserialized ItemStack.
     */
    public static ItemStack itemStackFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            DebugLogger.getInstance().error("Failed to deserialize ItemStack from Base64.", e);
            return null;
        }
    }
}
