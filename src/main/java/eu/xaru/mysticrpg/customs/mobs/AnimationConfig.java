package eu.xaru.mysticrpg.customs.mobs;

public class AnimationConfig {
    private String idleAnimation = "idle";
    private String walkAnimation = "walk";
    private String combatIdleAnimation = "combat_idle"; // Add this
    private String combatWalkAnimation = "combat_walk"; // Add this
    private String attackAnimation = "attack";

    // Getters and Setters for combat animations
    public String getCombatIdleAnimation() {
        return combatIdleAnimation;
    }

    public void setCombatIdleAnimation(String combatIdleAnimation) {
        this.combatIdleAnimation = combatIdleAnimation;
    }

    public String getCombatWalkAnimation() {
        return combatWalkAnimation;
    }

    public void setCombatWalkAnimation(String combatWalkAnimation) {
        this.combatWalkAnimation = combatWalkAnimation;
    }

    // Getters and Setters for other animations
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
