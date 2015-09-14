package com.theosirian.pacman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

import static com.theosirian.pacman.Entity.Direction.RIGHT;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-13 17-55.</p>
 */
public class Screen extends Entity {

    private Animation anim;

    public Screen(int x, int y, String path) {
        super(x, y, null);
        Texture texture = new Texture(Gdx.files.internal(path));
        int tileSize = texture.getHeight();
        int animationFrameCount = 1;
        TextureRegion[] regions = new TextureRegion[animationFrameCount];
        for (int i = 0; i < animationFrameCount; i++) {
            regions[i] = new TextureRegion(texture, tileSize * i, 0, tileSize, tileSize);
        }
        this.anim = new Animation(0.1f, regions);
        this.currentFrame = anim.getKeyFrame(0);
    }

    @Override
    public void draw(Batch batch) {
        batch.draw(currentFrame,0,0,320,336);
    }

}
