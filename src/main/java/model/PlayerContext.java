package model;

import java.util.Arrays;

public final class PlayerContext {
    private final Hockeyist[] hockeyists;
    private final World world;

    public PlayerContext(Hockeyist[] hockeyists, World world) {
        this.hockeyists = Arrays.copyOf(hockeyists, hockeyists.length);
        this.world = world;
    }

    public Hockeyist[] getHockeyists() {
        return Arrays.copyOf(hockeyists, hockeyists.length);
    }

    public World getWorld() {
        return world;
    }
}
