package eu.xaru.mysticrpg.content.menus;

import java.util.ArrayList;
import java.util.List;

public class MenuData {
    private String menuName;
    private int rows;
    private int columns;
    private boolean fillRestWithWhiteGlass;
    private boolean useHotbarItem;
    private HotbarItem hotbarItem;
    private List<MenuItem> items = new ArrayList<>();

    // Getters and setters
    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public boolean isFillRestWithWhiteGlass() {
        return fillRestWithWhiteGlass;
    }

    public void setFillRestWithWhiteGlass(boolean fillRestWithWhiteGlass) {
        this.fillRestWithWhiteGlass = fillRestWithWhiteGlass;
    }

    public boolean isUseHotbarItem() {
        return useHotbarItem;
    }

    public void setUseHotbarItem(boolean useHotbarItem) {
        this.useHotbarItem = useHotbarItem;
    }

    public HotbarItem getHotbarItem() {
        return hotbarItem;
    }

    public void setHotbarItem(HotbarItem hotbarItem) {
        this.hotbarItem = hotbarItem;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

    public void addItem(MenuItem item) {
        this.items.add(item);
    }
}
