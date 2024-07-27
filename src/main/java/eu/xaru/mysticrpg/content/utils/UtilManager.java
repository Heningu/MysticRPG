package eu.xaru.mysticrpg.content.utils;

public class UtilManager {
    private final MainUtil mainUtil;
    private final NBTUtils nbtUtils;

    public UtilManager() {
        this.mainUtil = MainUtil.getInstance();
        this.nbtUtils = NBTUtils.getInstance();
    }

    public MainUtil getMainUtil() {
        return mainUtil;
    }

    public NBTUtils getNbtUtils() {
        return nbtUtils;
    }
}
