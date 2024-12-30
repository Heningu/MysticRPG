package eu.xaru.mysticrpg.pets.content;

import eu.xaru.mysticrpg.pets.Pet;
import eu.xaru.mysticrpg.pets.PetRarity;

import java.util.*;

public final class PetRegistry {

    private static final List<Pet> ALL_PETS = new ArrayList<>();

    static {
        registerFireWolf();
        registerPhoenix();  // <--- NEW
        registerShaman();
    }

    private PetRegistry() {
    }

    /**
     * Returns unmodifiable list of all registered pets.
     */
    public static List<Pet> getAllPets() {
        return Collections.unmodifiableList(ALL_PETS);
    }

    private static void registerFireWolf() {
        Map<Integer, Integer> xpTable = new HashMap<>();
        xpTable.put(2, 50);
        xpTable.put(3, 150);
        xpTable.put(4, 300);
        xpTable.put(5, 600);
        xpTable.put(6, 1200);
        xpTable.put(7, 2000);
        xpTable.put(8, 3000);
        xpTable.put(9, 5000);
        xpTable.put(10, 7500);

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("HEALTH", 50);
        statsMap.put("STRENGTH", 10);

        List<String> effectList = new ArrayList<>();
        effectList.add("firetick"); // corresponds to FireTickEffect

        List<String> loreList = new ArrayList<>();
        loreList.add("The fire wolf from hell.");
        loreList.add("Keeping him close will inflict");
        loreList.add("fire damage to your enemies");
        loreList.add("Obtained by XX");

        Pet firewolf = new Pet(
                "firewolf",
                "Fire Wolf",
                "BONE",
                "pet_firewolf",
                "idle",
                "walk",
                statsMap,
                effectList,
                loreList,
                PetRarity.LEGENDARY,
                10,
                1,
                0,
                xpTable
        );
        ALL_PETS.add(firewolf);
    }

    private static void registerShaman() {
        // xp table
        Map<Integer, Integer> xpTable = new HashMap<>();
        xpTable.put(2, 60);
        xpTable.put(3, 150);
        xpTable.put(4, 300);
        xpTable.put(5, 600);
        xpTable.put(6, 1200);
        xpTable.put(7, 2000);
        xpTable.put(8, 3000);
        xpTable.put(9, 4500);
        xpTable.put(10, 7000);

        // stats
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("HEALTH", 45);
        statsMap.put("DEFENSE", 3);

        // effect
        List<String> effectList = Collections.singletonList("shamanblessing");

        List<String> loreList = new ArrayList<>();
        loreList.add("A shaman who blesses its owner.");
        loreList.add("Restores HP each second whenever it can,");
        loreList.add("2 HP if until level5, 3 HP until level10, 5 HP at level10.");
        loreList.add("Stacks with normal regeneration");

        Pet shaman = new Pet(
                "shaman",
                "Shaman",
                "OAK_SAPLING",
                "pet_shaman",
                "idle",
                "walk",
                statsMap,
                effectList,
                loreList,
                PetRarity.EPIC,
                10,   // maxLevel
                1,    // startLevel
                0,    // startXp
                xpTable
        );
        ALL_PETS.add(shaman);
    }


    private static void registerPhoenix() {
        // xp table
        Map<Integer, Integer> xpTable = new HashMap<>();
        xpTable.put(2, 60);
        xpTable.put(3, 150);
        xpTable.put(4, 300);
        xpTable.put(5, 500);
        xpTable.put(6, 900);
        xpTable.put(7, 1500);
        xpTable.put(8, 2500);
        xpTable.put(9, 4000);
        xpTable.put(10, 6500);

        // stats
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("HEALTH", 60);
        statsMap.put("DEFENSE", 5);

        // The effect is "phoenixwill"
        List<String> effectList = Collections.singletonList("phoenixwill");

        List<String> loreList = new ArrayList<>();
        loreList.add("A mystical phoenix with a will to survive.");
        loreList.add("Can prevent one lethal blow per life,");
        loreList.add("locking your HP at 1 for 2 seconds!");
        loreList.add("After usage, effect ends until next death.");

        Pet phoenix = new Pet(
                "phoenix",
                "Phoenix",
                "BLAZE_POWDER", // or FIRE_CHARGE, or something
                "pet_phoenix",
                "idle",
                "walk",
                statsMap,
                effectList,
                loreList,
                PetRarity.LEGENDARY,
                10,
                1,
                0,
                xpTable
        );


        ALL_PETS.add(phoenix);
    }
}
