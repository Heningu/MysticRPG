package eu.xaru.mysticrpg.guis.player.social;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyHelper;
import eu.xaru.mysticrpg.social.party.Party;
import eu.xaru.mysticrpg.social.party.PartyModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyGUI {

    private final PartyHelper partyHelper;

    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final PlayerStatModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;



    public PartyGUI(PartyHelper partyHelper) {
        this.partyHelper = partyHelper;
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openPartyGUI(Player player) {
        Party party = partyHelper.getParty(player.getUniqueId());


        Item hint = new SimpleItem(new ItemBuilder(Material.PAPER)
                .setDisplayName(ChatColor.GOLD + "Useful Tip")
                .addLoreLines(
                        "",
                        "To kick a player from your party,",
                        "simply click on one if you are the leader"
                )
                .addAllItemFlags()
        );


        Item filler = new SimpleItem(
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                        .setDisplayName("")
        );

        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };

        Gui.Builder guiBuilder = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# # P # T # Z # #",
                        "B H # # # # # # #")
                .addIngredient('#', filler)
                .addIngredient('H', hint)
                .addIngredient('B', back);

        List<UUID> members = new ArrayList<>();

        if (party != null) {
            members.addAll(party.getMembers());
        }

        // Always add the player as the leader if no party exists
        if (members.isEmpty()) {
            members.add(player.getUniqueId());
        }

        for (int i = 0; i < 3; i++) {
            if (i < members.size()) {
                UUID memberUUID = members.get(i);
                Player member = player.getServer().getPlayer(memberUUID);
                if (member == null) {
                    continue; // Skip null players
                }

                ItemStack memberHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) memberHead.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(member);
                    meta.setDisplayName(ChatColor.GREEN + member.getName());
                    memberHead.setItemMeta(meta);
                }

                Item memberItem = new SimpleItem(memberHead) {
                    @Override
                    public void handleClick(ClickType clickType, Player clickPlayer, InventoryClickEvent event) {
                        if (party != null && party.getLeader().equals(clickPlayer.getUniqueId()) && !member.getUniqueId().equals(clickPlayer.getUniqueId())) {
                            partyHelper.kickPlayer(clickPlayer, member);
                            openPartyGUI(clickPlayer); // Refresh the GUI after a kick
                        } else if (member.getUniqueId().equals(clickPlayer.getUniqueId())) {
                            clickPlayer.sendMessage(ChatColor.RED + "You cannot kick yourself.");
                        } else {
                            clickPlayer.sendMessage(ChatColor.RED + "Only the leader can manage party members.");
                        }
                    }
                };

                switch (i) {
                    case 0 -> guiBuilder.addIngredient('P', memberItem); // Leader
                    case 1 -> guiBuilder.addIngredient('T', memberItem); // Second player
                    case 2 -> guiBuilder.addIngredient('Z', memberItem); // Last player
                }
            } else {
                ItemStack emptyHead = new ItemStack(Material.SKELETON_SKULL);
                SkullMeta meta = (SkullMeta) emptyHead.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GRAY + "Empty Slot");
                    emptyHead.setItemMeta(meta);
                }

                Item emptyItem = new SimpleItem(emptyHead);
                switch (i) {
                    case 0 -> guiBuilder.addIngredient('P', emptyItem);
                    case 1 -> guiBuilder.addIngredient('T', emptyItem);
                    case 2 -> guiBuilder.addIngredient('Z', emptyItem);
                }
            }
        }

        Gui gui = guiBuilder.build();

        Window window = Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(ChatColor.BLUE + "Party Menu")
                .build();

        window.open();
    }
}
