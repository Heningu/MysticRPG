package eu.xaru.mysticrpg.utils;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * A generic pagination helper to manage paginated views of a list.
 *
 * @param <T> The type of items to paginate.
 */
public class PaginationHelper<T> {
    private List<T> items;
    private final int itemsPerPage;
    private final int totalPages;
    private int currentPage;

    /**
     * Constructs a PaginationHelper with the given items and items per page.
     *
     * @param items        The list of items to paginate.
     * @param itemsPerPage The number of items per page. Must be greater than 0.
     * @throws IllegalArgumentException if itemsPerPage is less than 1.
     */
    public PaginationHelper(List<T> items, int itemsPerPage) {
        if (itemsPerPage < 1) {
            throw new IllegalArgumentException("itemsPerPage must be at least 1.");
        }
        // Defensive copy and make the list unmodifiable to ensure immutability
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.itemsPerPage = itemsPerPage;
        this.totalPages = calculateTotalPages();
        this.currentPage = 1; // Start at page 1
    }

    /**
     * Calculates the total number of pages based on items and itemsPerPage.
     *
     * @return Total number of pages.
     */
    private int calculateTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    /**
     * Updates the list of items and recalculates total pages.
     * Adjusts the current page if necessary to ensure it remains within valid bounds.
     *
     * @param newItems The new list of items to paginate.
     */
    public void updateItems(List<T> newItems) {
        // Defensive copy and make the list unmodifiable
        this.items = List.copyOf(newItems);
        // Recalculate total pages
        int newTotalPages = calculateTotalPages();
        // Adjust current page if it exceeds the new total pages
        if (currentPage > newTotalPages) {
            currentPage = Math.max(newTotalPages, 1);
        }
    }

    /**
     * Retrieves the list of items on the current page.
     *
     * @return A list of items on the current page.
     */
    public List<T> getCurrentPageItems() {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        return items.subList(startIndex, endIndex);
    }

    /**
     * Retrieves the total number of pages.
     *
     * @return Total number of pages.
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Retrieves the current page number.
     *
     * @return The current page number (1-based index).
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Checks if there is a next page.
     *
     * @return True if next page exists, false otherwise.
     */
    public boolean hasNextPage() {
        return currentPage < totalPages;
    }

    /**
     * Checks if there is a previous page.
     *
     * @return True if previous page exists, false otherwise.
     */
    public boolean hasPreviousPage() {
        return currentPage > 1;
    }

    /**
     * Moves to the next page if possible.
     *
     * @return True if successfully moved to next page, false otherwise.
     */
    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            return true;
        }
        return false;
    }

    /**
     * Moves to the previous page if possible.
     *
     * @return True if successfully moved to previous page, false otherwise.
     */
    public boolean previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            return true;
        }
        return false;
    }

    /**
     * Jumps to a specific page number.
     *
     * @param page The page number to jump to. Must be between 1 and totalPages.
     * @return True if successfully jumped to the page, false otherwise.
     */
    public boolean jumpToPage(int page) {
        if (page >= 1 && page <= totalPages) {
            currentPage = page;
            return true;
        }
        return false;
    }

    /**
     * Resets the pagination to the first page.
     */
    public void reset() {
        this.currentPage = 1;
    }

    /**
     * Returns a string representation of the pagination state.
     *
     * @return String indicating current page and total pages.
     */
    @Override
    public String toString() {
        return "PaginationHelper{" +
                "currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                '}';
    }

    /**
     * Factory method to create a PaginationHelper for ItemStacks.
     *
     * @param items        The list of ItemStacks to paginate.
     * @param itemsPerPage The number of items per page.
     * @return A new PaginationHelper instance for ItemStacks.
     */
    public static PaginationHelper<ItemStack> forItemStacks(List<ItemStack> items, int itemsPerPage) {
        return new PaginationHelper<>(items, itemsPerPage);
    }

    public void setCurrentPage(int i) {
        this.currentPage = i;
    }
}
