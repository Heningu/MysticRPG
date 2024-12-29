package eu.xaru.mysticrpg.pets;

import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

public class PetInstance {
    private final Pet pet;
    private final LivingEntity petEntity;
    private ModeledEntity modeledEntity;
    private String currentAnimation;
    private double animationPhase;

    // Add the armor stand reference
    private ArmorStand nameHologram;

    public PetInstance(Pet pet, LivingEntity petEntity) {
        this.pet = pet;
        this.petEntity = petEntity;
        this.currentAnimation = null;
        this.animationPhase = 0.0;
        this.nameHologram = null;
    }

    public Pet getPet() {
        return pet;
    }

    public LivingEntity getPetEntity() {
        return petEntity;
    }

    public ModeledEntity getModeledEntity() {
        return modeledEntity;
    }

    public void setModeledEntity(ModeledEntity modeledEntity) {
        this.modeledEntity = modeledEntity;
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(String currentAnimation) {
        this.currentAnimation = currentAnimation;
    }

    public double getAnimationPhase() {
        return animationPhase;
    }

    public void setAnimationPhase(double animationPhase) {
        this.animationPhase = animationPhase;
    }

    public ArmorStand getNameHologram() {
        return nameHologram;
    }

    public void setNameHologram(ArmorStand nameHologram) {
        this.nameHologram = nameHologram;
    }
}
