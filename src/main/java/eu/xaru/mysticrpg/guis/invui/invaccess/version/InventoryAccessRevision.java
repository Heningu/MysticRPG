package eu.xaru.mysticrpg.guis.invui.invaccess.version;

import eu.xaru.mysticrpg.guis.invui.invaccess.util.VersionUtils;

public enum InventoryAccessRevision {
    
    // this order is required
    R22("r22", "1.21.3");
    
    public static final InventoryAccessRevision REQUIRED_REVISION = getRequiredRevision();
    
    private final String packageName;
    private final int[] since;
    
    InventoryAccessRevision(String packageName, String since) {
        this.packageName = packageName;
        this.since = VersionUtils.toMajorMinorPatch(since);
    }
    
    private static InventoryAccessRevision getRequiredRevision() {
        for (InventoryAccessRevision revision : values())
            if (VersionUtils.isServerHigherOrEqual(revision.getSince())) return revision;
        
        throw new UnsupportedOperationException("Your version of Minecraft is not supported by InventoryAccess");
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public int[] getSince() {
        return since;
    }
    
}
