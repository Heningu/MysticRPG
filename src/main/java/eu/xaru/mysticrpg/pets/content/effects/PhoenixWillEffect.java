package eu.xaru.mysticrpg.pets.content.effects;

/**
 * Phoenix Will effect. The actual "save from death" logic
 * is in CustomDamageHandler, but we store name/desc here for the GUI.
 */
public class PhoenixWillEffect implements IPetEffect {

    @Override
    public String getId() {
        return "phoenixwill";
    }

    @Override
    public String getDescription() {
        return "Cheat death once per life, leaving you at 1 HP for 2s.";
    }

    // We skip apply(...) here because the logic is in CustomDamageHandler
    // (i.e. reading "phoenixwill" effect from PetEffectTracker).
}
