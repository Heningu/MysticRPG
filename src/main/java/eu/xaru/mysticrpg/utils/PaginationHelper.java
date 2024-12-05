package eu.xaru.mysticrpg.utils;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaginationHelper {
    private List<ItemStack> items;
    private int itemsPerPage;
    private int currentPage;

    public PaginationHelper(List<ItemStack> items, int itemsPerPage) {
        this.items = new ArrayList<>(items);
        this.itemsPerPage = itemsPerPage;
        this.currentPage = 0;
    }

    public void updateItems(List<ItemStack> newItems) {
        this.items = new ArrayList<>(newItems);
        // Adjust currentPage if necessary
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = Math.max(totalPages - 1, 0);
        }
    }

    public List<ItemStack> getPageItems() {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        if (start >= items.size()) {
            return Collections.emptyList();
        }
        return items.subList(start, end);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
        }
    }
}
