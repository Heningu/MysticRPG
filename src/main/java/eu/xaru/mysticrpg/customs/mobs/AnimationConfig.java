package eu.xaru.mysticrpg.customs.mobs;

public class AnimationConfig {
    private String idleAnimation = "idle";
    private String walkAnimation = "walk";
    private String attackAnimation = "attack"; // Default attack animation

    // Constructor
    public AnimationConfig() {}

    // Getters and Setters
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

    public String getAttackAnimation() {
        return attackAnimation;
    }

    public void setAttackAnimation(String attackAnimation) {
        this.attackAnimation = attackAnimation;
    }
}
