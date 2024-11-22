package eu.xaru.mysticrpg.pets;

import org.bukkit.entity.ArmorStand;

public class PetInstance {
    private final Pet pet;
    private final ArmorStand petEntity;
    private double animationPhase;

    public PetInstance(Pet pet, ArmorStand petEntity) {
        this.pet = pet;
        this.petEntity = petEntity;
        this.animationPhase = 0;
    }

    public Pet getPet() {
        return pet;
    }

    public ArmorStand getPetEntity() {
        return petEntity;
    }

    public double getAnimationPhase() {
        return animationPhase;
    }

    public void setAnimationPhase(double animationPhase) {
        this.animationPhase = animationPhase;
    }
}
