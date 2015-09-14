package com.theosirian.pacman;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class Teleport extends Entity {

    protected Vector2 teleportPosition, offset;
    protected Pacman pacman;
    protected List<Ghost> ghosts;

    public Teleport(int x, int y, int targetX, int targetY, Pacman.Direction outDirection, Pacman player, List<Ghost> ghosts) {
	    super(x, y, player != null ? player.getCollisionLayer() : null);
        teleportPosition = new Vector2(targetX, targetY);
        offset = outDirection.getUnitVector();
        offset.x = (offset.x * Settings.SPRITE_WIDTH) - 1;
        offset.y = (offset.y * Settings.SPRITE_HEIGHT) - 1;
        setBounds(getX() + offset.x, getY() + offset.y, Settings.SPRITE_WIDTH + 2, Settings.SPRITE_HEIGHT + 2);
        pacman = player;
        this.ghosts = ghosts;
    }

    public void update(float delta) {
        setBounds(getX() + offset.x, getY() + offset.y, Settings.SPRITE_WIDTH + 2, Settings.SPRITE_HEIGHT + 2);
        if (bounds.contains(pacman.getBounds())) {
            pacman.teleport((int) (teleportPosition.x), (int) (teleportPosition.y));
            System.out.println("Teleport Pacman!");
        }
        for(Ghost ghost : this.ghosts){
            if(bounds.contains(ghost.getBounds())){
                ghost.teleport((int) (teleportPosition.x), (int) (teleportPosition.y));
                System.out.println("Teleport Ghost!");
            }
        }
    }
}
