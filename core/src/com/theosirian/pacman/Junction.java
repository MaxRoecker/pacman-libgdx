package com.theosirian.pacman;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-10 20-35.</p>
 */
public class Junction extends Entity {

    protected Ghost ghost;

    public Junction(int x, int y, Ghost ghost) {
        super(x, y, ghost != null? ghost.getCollisionLayer() : null);
        this.ghost = ghost;
        setBounds(getX() + 7,getY()+7,2,2);
    }

}
