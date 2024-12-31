package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.player.social.FriendsGUI;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.pets.*;
import eu.xaru.mysticrpg.pets.content.effects.EffectRegistry;
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
import java.util.Map;

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

    public void openPetGUI(Player player) {

        Item controler = new FriendsGUI.ChangePageItem();

        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window
                        ? (Window) event.getView().getTopInventory().getHolder()
                        : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(
                        auctionHouse, equipmentModule, levelingModule, playerStat,
                        questModule, friendsModule, partyModule
                );
                mainMenu.openGUI(clickPlayer);
            }
        };

        Item border = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("")
                .addAllItemFlags()
        );

        // Build a list of items for each owned pet
        List<Item> petItems = new ArrayList<>();
        for (String petId : petHelper.getOwnedPetIds(player)) {
            Pet pet = petHelper.getPetById(petId);
            if (pet != null) {
                // We create an item for that pet
                petItems.add(createPetItem(player, petId, pet));
            }
        }

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

        Window.single()
                .setViewer(player)
                .setGui(gui)
                .setTitle(Utils.getInstance().$("Pets"))
                .open(player);
    }

    /**
     * Load from file each time, so we see the up-to-date level/xp for each pet.
     */
    private Item createPetItem(Player player, String petId, Pet pet) {

        // 1) read from file
        Map<String, PetFileStorage.PetProgress> fileData = PetFileStorage.loadPlayerPets(player);
        PetFileStorage.PetProgress progress = fileData.get(petId);
        if (progress != null) {
            pet.setLevel(progress.getLevel());
            pet.setCurrentXp(progress.getXp());
        } else {
            // If no entry => default from constructor
        }

        // Now 'pet' object has the correct level & xp from the file
        List<String> loreLines = new ArrayList<>();

        // Show color-coded rarity
        loreLines.add(ChatColor.GRAY + "Rarity: "
                + pet.getRarity().getColor() + pet.getRarity().name());

        // Level
        loreLines.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + pet.getLevel());

        // XP display:
        if (pet.getLevel() >= pet.getMaxLevel()) {
            loreLines.add(ChatColor.GRAY + "XP: " + ChatColor.GREEN + "MAX LEVEL!");
        } else {
            int leftoverNeeded = pet.getXpToNextLevel(); // e.g. 60
            int currentXp = pet.getCurrentXp();          // e.g. 90
            int totalNeeded = currentXp + leftoverNeeded; // e.g. 150
            // "90 / 150" instead of "90 / 60"
            loreLines.add(ChatColor.GRAY + "XP: "
                    + ChatColor.GREEN + currentXp
                    + ChatColor.GRAY + " / "
                    + ChatColor.RED + totalNeeded);
        }
        loreLines.add("");

        // 1) Stats
        if (pet.getAdditionalStats() != null && !pet.getAdditionalStats().isEmpty()) {
            loreLines.add(ChatColor.GRAY + "Stats:");
            for (Map.Entry<String, Object> entry : pet.getAdditionalStats().entrySet()) {
                String statName = entry.getKey();
                Object valueObj = entry.getValue();
                loreLines.add(ChatColor.YELLOW + statName + ChatColor.GRAY + ": " + ChatColor.GREEN + "+" + valueObj);
            }
            loreLines.add("");
        }

        // 2) Effects => fetch from registry for descriptions
        if (pet.getEffects() != null && !pet.getEffects().isEmpty()) {
            loreLines.add(ChatColor.GRAY + "Effects:");
            for (String effectId : pet.getEffects()) {
                var effectObj = EffectRegistry.get(effectId);
                if (effectObj != null) {
                    loreLines.add(ChatColor.YELLOW + effectObj.getId()
                            + ChatColor.GRAY + " - "
                            + effectObj.getDescription());
                } else {
                    loreLines.add(ChatColor.YELLOW + effectId);
                }
            }
            loreLines.add("");
        }

        // 3) The custom `lore:` lines
        if (pet.getLore() != null && !pet.getLore().isEmpty()) {
            for (String loreLine : pet.getLore()) {
                loreLines.add(ChatColor.GRAY + loreLine);
            }
            loreLines.add("");
        }

        // Instruction
        loreLines.add(ChatColor.YELLOW + "Click to equip or unequip this pet!");

        String[] finalLoreArray = loreLines.toArray(new String[0]);

        return new SimpleItem(
                new ItemBuilder(Material.valueOf(pet.getDisplayItem()))
                        .setDisplayName(ChatColor.GOLD + pet.getName())
                        .addLoreLines(finalLoreArray)
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
                if (petId.equals(equippedPetId)) {
                    // Pet is currently equipped => unequip
                    petHelper.unequipPet(clickPlayer);
                    clickPlayer.sendMessage(ChatColor.RED + "You have unequipped the pet: " + pet.getName());
                } else {
                    // Equip it
                    petHelper.equipPet(clickPlayer, petId);
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
