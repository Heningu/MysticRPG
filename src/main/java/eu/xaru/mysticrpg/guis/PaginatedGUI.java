package eu.xaru.mysticrpg.guis;

import eu.xaru.mysticrpg.guis.components.Button;
import eu.xaru.mysticrpg.guis.components.EventDebouncer;
import eu.xaru.mysticrpg.guis.enums.GUIButtonType;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Abstract class for GUIs that require pagination.
 *
 * @param <T> The type of items to paginate.
 */
public abstract class PaginatedGUI<T> extends BaseGUI {
    private final PaginationHelper<T> paginationHelper;
    private final int itemsPerPage;
    private final int nextPageSlot;
    private final int previousPageSlot;
    private final EventDebouncer debouncer = new EventDebouncer();

    /**
     * Constructs a PaginatedGUI.
     *
     * @param player         The player interacting with the GUI.
     * @param title          The title of the inventory.
     * @param size           The size of the inventory (must be a multiple of 9).
     * @param items          The list of items to paginate.
     * @param itemsPerPage   Number of items per page.
     * @param nextPageSlot   The slot index for the "Next Page" button.
     * @param prevPageSlot   The slot index for the "Previous Page" button.
     */
    public PaginatedGUI(Player player, String title, int size, List<T> items, int itemsPerPage,
                        int nextPageSlot, int prevPageSlot) {
        super(player, title, size);
        this.itemsPerPage = itemsPerPage;
        this.paginationHelper = new PaginationHelper<>(items, itemsPerPage);
        this.nextPageSlot = nextPageSlot;
        this.previousPageSlot = prevPageSlot;
    }

    @Override
    protected void initialize() {
        // Add Pagination Buttons
        addPaginationButtons();

        // Display items for the current page
        displayCurrentPage();
    }

    /**
     * Adds pagination buttons (Next and Previous) to the GUI.
     */
    private void addPaginationButtons() {
        // Next Page Button
        addButton(nextPageSlot, new Button(GUIButtonType.NEXT_PAGE.getItem(), this::onNextPage));

        // Previous Page Button
        addButton(previousPageSlot, new Button(GUIButtonType.PREVIOUS_PAGE.getItem(), this::onPreviousPage));
    }

    /**
     * Displays items for the current page.
     */
    private void displayCurrentPage() {
        // Define the range for item slots (assuming items start from slot 9 to slot size - 9)
        int startSlot = 9;
        int endSlot = inventory.getSize() - 9;
        for (int slot = startSlot; slot < endSlot; slot++) {
            inventory.setItem(slot, null);
        }

        List<T> currentItems = paginationHelper.getCurrentPageItems();
        for (int i = 0; i < currentItems.size(); i++) {
            T item = currentItems.get(i);
            ItemStack itemStack = createItemStack(item);
            int slot = startSlot + i;
            addButton(slot, new Button(itemStack, event -> onItemClick(event, item)));
        }

        updatePaginationButtons();
    }

    /**
     * Updates the state (visibility) of pagination buttons based on the current page.
     */
    private void updatePaginationButtons() {
        // Next Page Button
        if (paginationHelper.hasNextPage()) {
            inventory.setItem(nextPageSlot, GUIButtonType.NEXT_PAGE.getItem());
        } else {
            inventory.setItem(nextPageSlot, Utils.createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, ""));
        }

        // Previous Page Button
        if (paginationHelper.hasPreviousPage()) {
            inventory.setItem(previousPageSlot, GUIButtonType.PREVIOUS_PAGE.getItem());
        } else {
            inventory.setItem(previousPageSlot, Utils.createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, ""));
        }
    }

    /**
     * Handler for the "Next Page" button click.
     *
     * @param event The InventoryClickEvent triggered by the click.
     */
    private void onNextPage(InventoryClickEvent event) {
        if (debouncer.canPerform(player)) {
            paginationHelper.nextPage();
            displayCurrentPage();
        } else {
            player.sendMessage(Utils.getInstance().$("Please wait before performing this action again."));
        }
    }

    /**
     * Handler for the "Previous Page" button click.
     *
     * @param event The InventoryClickEvent triggered by the click.
     */
    private void onPreviousPage(InventoryClickEvent event) {
        if (debouncer.canPerform(player)) {
            paginationHelper.previousPage();
            displayCurrentPage();
        } else {
            player.sendMessage(Utils.getInstance().$("Please wait before performing this action again."));
        }
    }

    /**
     * Handler for individual item clicks within the paginated GUI.
     *
     * @param event The InventoryClickEvent triggered by the click.
     * @param item  The item associated with the clicked slot.
     */
    protected abstract void onItemClick(InventoryClickEvent event, T item);

    /**
     * Creates an ItemStack representation of the item.
     *
     * @param item The item to represent.
     * @return The ItemStack to display in the GUI.
     */
    protected abstract ItemStack createItemStack(T item);

    /**
     * Retrieves the current page number.
     *
     * @return The current page number.
     */
    public int getCurrentPage() {
        return paginationHelper.getCurrentPage();
    }

    /**
     * Retrieves the total number of pages.
     *
     * @return Total number of pages.
     */
    public int getTotalPages() {
        return paginationHelper.getTotalPages();
    }

    /**
     * Refreshes the GUI to reflect any changes.
     */
    public void refresh() {
        displayCurrentPage();
    }
}
