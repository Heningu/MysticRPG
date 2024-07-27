package eu.xaru.mysticrpg.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsHook {

    private final LuckPerms luckPerms;

    public LuckPermsHook() {
        this.luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
    }

    public boolean hasPermission(Player player, String permission) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null && user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    public void addPermission(UUID uuid, String permission) {
        CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            if (user != null) {
                Node node = PermissionNode.builder(permission).build();
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
            }
        });
    }

    public void removePermission(UUID uuid, String permission) {
        CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            if (user != null) {
                Node node = PermissionNode.builder(permission).build();
                user.data().remove(node);
                luckPerms.getUserManager().saveUser(user);
            }
        });
    }
}
