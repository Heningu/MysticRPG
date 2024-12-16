package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.player.social.FriendsGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.pets.Pet;
import eu.xaru.mysticrpg.pets.PetHelper;
import eu.xaru.mysticrpg.pets.PetsModule;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class PetGUI {

    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final StatsModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;
    private final PlayerDataCache playerDataCache;
    private final PetsModule petsModule;
    private final PetHelper petHelper;


    public PetGUI() {
        this.petsModule = ModuleManager.getInstance().getModuleInstance(PetsModule.class);
        this.petHelper = petsModule.getPetHelper();
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }



    public void openPetGUI(Player player){


        Item controler = new FriendsGUI.ChangePageItem();

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


        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags()
        );

        // Fetch player's owned pets
        List<Item> petItems = new ArrayList<>();
        for (String petId : petHelper.getOwnedPetIds(player)) {
            Pet pet = petHelper.getPetById(petId);
            if (pet != null) {
                petItems.add(createPetItem(player, pet));
            }
        }



        // Create the paged GUI
        Gui gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "X > # # # # # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', border)
                .addIngredient('X', back)
                .addIngredient('>', controler)
                .setContent(petItems)
                .build();

        // Open the GUI in a window
        Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(Utils.getInstance().$("Pets"))
                .open(player);
    }


    private Item createPetItem(Player player, Pet pet) {
        return new SimpleItem(new ItemBuilder(Material.valueOf(pet.getDisplayItem()))
                .setDisplayName(ChatColor.GOLD + pet.getName())
                .addLoreLines("", ChatColor.YELLOW + "Click to equip or unequip this pet!", "")
                .addAllItemFlags()
        ) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                PlayerDataCache cache = PlayerDataCache.getInstance();
                PlayerData playerData = cache.getCachedPlayerData(clickPlayer.getUniqueId());

                if (playerData == null) {
                    clickPlayer.sendMessage(ChatColor.RED + "Unable to load your data. Please try again later.");
                    return;
                }

                String equippedPetId = playerData.getEquippedPet();

                if (pet.getId().equals(equippedPetId)) {
                    // Pet is currently equipped, so unequip it
                    petHelper.unequipPet(clickPlayer);
                    clickPlayer.sendMessage(ChatColor.RED + "You have unequipped the pet: " + pet.getName());
                } else {
                    // Pet is not equipped, so equip it
                    petHelper.equipPet(clickPlayer, pet.getId());
                    clickPlayer.sendMessage(ChatColor.GREEN + "You have equipped the pet: " + pet.getName());
                }
            }
        };
    }



    public static class ChangePageItem extends ControlItem<PagedGui<?>> {
        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.RIGHT) {
                getGui().goForward();
            } else if (clickType == ClickType.LEFT) {
                getGui().goBack();
            }
        }

        @Override
        public ItemProvider getItemProvider(PagedGui<?> gui) {
            return new ItemBuilder(Material.ARROW)
                    .setDisplayName("Switch Pages")
                    .addLoreLines(
                            "",
                            ChatColor.GRAY + "Current page: " + (gui.getCurrentPage() + 1) + " of " + gui.getPageAmount(),
                            ChatColor.GREEN + "Left-click to go forward",
                            ChatColor.RED + "Right-click to go back"
                    )
                    .addEnchantment(Enchantment.UNBREAKING, 1, true)
                    .addAllItemFlags();
        }
    }
}
