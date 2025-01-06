package eu.xaru.mysticrpg.customs.mobs;

/**
 * A minimal animation config with only idle & walk references.
 *
 * If you want more, just add them here. ModelEngine 4.0.8
 * has default animations so you typically only need an
 * idle/walk if you want custom ones.
 */
public class AnimationConfig {

    private String idleAnimation = "idle";
    private String walkAnimation = "walk";

    public AnimationConfig() {
    }

    public String getIdleAnimation() {
        return idleAnimation;
    }

    public void setIdleAnimation(String idleAnimation) {
        this.idleAnimation = idleAnimation;
    }

    public String getWalkAnimation() {
        return walkAnimation;
    }

    public void setWalkAnimation(String walkAnimation) {
        this.walkAnimation = walkAnimation;
    }
}
