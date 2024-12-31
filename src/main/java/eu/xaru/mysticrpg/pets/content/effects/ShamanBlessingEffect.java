package eu.xaru.mysticrpg.pets.content.effects;

/**
 * Phoenix Will effect. The actual "save from death" logic
 * is in CustomDamageHandler, but we store name/desc here for the GUI.
 */
public class ShamanBlessingEffect implements IPetEffect {

    @Override
    public String getId() {
        return "shamanblessing";
    }

    @Override
    public String getDescription() {
        return "Heals you every second, increasing with level.";
    }

    // We won't do direct logic here, because we apply it in CustomDamageHandler's regenerate logic.

}
