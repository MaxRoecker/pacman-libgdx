package com.theosirian.pacman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.PathFinderQueue;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-10 16-41.</p>
 */
public class Ghost extends Entity {

    private Animation anim;
    private float speed, animTime = 0f;
    private Vector2 targetPosition;
    private Vector2 objectivePosition;
    private Direction lastDirection;
    private List<Junction> junctions;

    public Ghost(int x, int y, TiledMapTileLayer collisionLayer) {
        super(x, y, collisionLayer);
        Texture texture = new Texture(Gdx.files.internal("petman.png"));
        int tileSize = texture.getHeight();
        int animationFrameCount = 1;
        TextureRegion[] regions = new TextureRegion[animationFrameCount];
        for (int i = 0; i < animationFrameCount; i++) {
            regions[i] = new TextureRegion(texture, tileSize * i, 0, tileSize, tileSize);
        }
        anim = new Animation(0.1f, regions);
        targetPosition = new Vector2(x, y);
        targetPosition = new Vector2(x, y);
        setBounds(getX(), getY(), 16, 16);
        speed = 1;
        direction = Direction.NONE;
        lastDirection = Direction.RIGHT;
        currentFrame = anim.getKeyFrame(0);
        this.junctions = new ArrayList<>();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        position.set(targetPosition);

        for (Junction junction : junctions) {
            System.out.println(junction.getBounds());
            if (junction.getBounds().contains(this.getBounds())) {
                System.out.printf("Colidiu!");

                Vector2 smaller = Direction.NONE.getUnitVector();
                float smallerDistance = Float.MAX_VALUE;
                for (Direction d : Direction.values()) {
                    if (d != Direction.NONE && d != lastDirection.getOppositeDirection()) {
                        Vector2 v = position.cpy();
                        v.mulAdd(d.getUnitVector(), this.speed);
                        if (testCollision(v)) {
                            if (smallerDistance > this.objectivePosition.dst(v)) {
                                smaller = v.cpy();
                                smallerDistance = this.objectivePosition.dst(v);
                            }
                        }
                    }
                }
                targetPosition.set(smaller.cpy());
            }else{
                targetPosition.set(position.cpy().mulAdd(lastDirection.getUnitVector(),this.speed));
            }
        }


    }

    @Override
    public void draw(Batch batch) {
        batch.draw(currentFrame, position.x, position.y, origin.x, origin.y, size.x, size.y, scale.x, scale.y, rotation);
    }

    public Vector2 getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Vector2 targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Vector2 getObjectivePosition() {
        return objectivePosition;
    }

    public void setObjectivePosition(Vector2 objectivePosition) {
        this.objectivePosition = objectivePosition;
    }

    public List<Junction> getJunctions() {
        return junctions;
    }

    public void setJunctions(List<Junction> junctions) {
        this.junctions = junctions;
    }
}
