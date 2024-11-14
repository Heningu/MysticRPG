package eu.xaru.mysticrpg.dungeons.instance.puzzles;

import eu.xaru.mysticrpg.dungeons.instance.DungeonInstance;

public abstract class Puzzle {

    protected final DungeonInstance instance;

    public Puzzle(DungeonInstance instance) {
        this.instance = instance;
    }

    public abstract void initialize();

    public abstract void onPlayerInteract();

    public abstract boolean isCompleted();
}
