package com.theosirian.pacman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.theosirian.pacman.Entity.Direction.*;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-10 16-41.</p>
 */
public class Ghost extends Entity {

    private float animationTime;
    private Animation normalAnimation;
    private Animation weakenedAnimation;
    private Animation endWeakenedAnimation;
    private Animation deadAnimation;
    private float speed = 0f;
    private Vector2 targetPosition;
    private Vector2 objectivePosition;
    private Direction lastDirection;
    private List<Junction> junctions;
    private Pacman pacman;
    private String color;
    private int id;
    private int delay;
    private int weakenedDuration;
    private boolean alive;
    private boolean eated;

    public Ghost(int x, int y, TiledMapTileLayer collisionLayer, String color, Pacman pacman, int id) {
        super(x, y, collisionLayer);
        Texture texture = new Texture(Gdx.files.internal("ghost-" + color + ".png"));
        int tileSize = texture.getHeight();
        /*Normal animation*/
        int animationFrameCount = 4;
        TextureRegion[] regions = new TextureRegion[animationFrameCount];
        for (int i = 0; i < animationFrameCount; i++) {
            regions[i] = new TextureRegion(texture, tileSize * i, 0, tileSize, tileSize);
        }
        this.normalAnimation = new Animation(0.1f, regions);
        /* Weakened animation */
        animationFrameCount = 1;
        regions = new TextureRegion[animationFrameCount];
        regions[0] = new TextureRegion(texture, tileSize * 4, 0, tileSize, tileSize);
        this.weakenedAnimation = new Animation(0.1f, regions);
        /* Weakened end animation */
        animationFrameCount = 2;
        regions = new TextureRegion[animationFrameCount];
        regions[0] = new TextureRegion(texture, tileSize * 3, 0, tileSize, tileSize);
        regions[1] = new TextureRegion(texture, tileSize * 4, 0, tileSize, tileSize);
        this.endWeakenedAnimation = new Animation(0.1f, regions);
        /* Dead animation */
        animationFrameCount = 1;
        regions = new TextureRegion[animationFrameCount];
        regions[0] = new TextureRegion(texture, tileSize * 5, 0, tileSize, tileSize);
        this.deadAnimation = new Animation(0.1f, regions);
        this.animationTime = 0f;

        this.targetPosition = new Vector2(x, y);
        this.targetPosition = new Vector2(x, y);
        this.setBounds(getX(), getY(), 16, 16);
        this.speed = 1f;
        this.direction = RIGHT;
        this.lastDirection = RIGHT;
        this.currentFrame = normalAnimation.getKeyFrame(0);
        this.junctions = new ArrayList<>();
        this.pacman = pacman;
        this.color = color;
        this.id = id;
        this.delay = (id - 1) * 250;
        this.objectivePosition = new Vector2(9 * 16, 13 * 16);
        this.weakenedDuration = 0;
        this.alive = true;
        this.eated = false;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        this.animationTime += delta;
        position.set(targetPosition);
        if (this.delay <= 0) {
            if (this.delay == 0) {
                delay--;
                this.teleport(9 * 16, 13 * 16);
            }
            for (Junction j : this.junctions) {
                if ((this.getX() == j.getX()) && (this.getY() == j.getY())) {
                    List<Direction> orderedDirections = this.smallerDirection(new ArrayList<>(Arrays.asList(values())));
                    orderedDirections.remove(NONE);
                    Direction best = orderedDirections.remove(0);
                    Vector2 v = this.position.cpy();
                    v.mulAdd(best.getUnitVector(), this.speed);
                    while ((orderedDirections.size() > 0) && (best == lastDirection.getOppositeDirection()) || (!testCollision(v))) {
                        best = orderedDirections.remove(0);
                        v = this.position.cpy();
                        v.mulAdd(best.getUnitVector(), this.speed);
                    }
                    if (orderedDirections.size() == 0) {
                        best = lastDirection.getOppositeDirection();
                    }
                    lastDirection = best;
                }
            }
            targetPosition = targetPosition.mulAdd(lastDirection.getUnitVector(), this.speed);
        } else {
            delay--;
        }

        /*Check weak*/
        if (this.weakenedDuration >= 0) {
            this.weakenedDuration--;
        }

        /*Check animation */
        if (this.alive) {
            if (this.weakenedDuration <= 0) {
                currentFrame = currentFrameFromDirection(lastDirection);
            } else if (this.weakenedDuration < 100) {
                currentFrame = endWeakenedAnimation.getKeyFrame(animationTime, true);
            } else {
                currentFrame = weakenedAnimation.getKeyFrame(0);
            }
        } else {
            currentFrame = deadAnimation.getKeyFrame(0);
        }

        /*Check Pacman collision*/
        Rectangle rectangle = new Rectangle(this.getBounds());
        rectangle.setX(rectangle.getX() + 4);
        rectangle.setY(rectangle.getY() + 4);
        rectangle.setWidth(rectangle.getWidth() - 8);
        rectangle.setHeight(rectangle.getHeight() - 8);
        if (pacman.getBounds().overlaps(rectangle)) {

            if (this.weakenedDuration > 0) {
                this.alive = false;
                this.eated = true;
            } else {
                if (this.alive) {
                    pacman.setAlive(false);
                }
            }

        }

    }

    private TextureRegion currentFrameFromDirection(Direction best) {
        switch (best) {
            case UP:
                return normalAnimation.getKeyFrame(0.2f);
            case DOWN:
                return normalAnimation.getKeyFrame(0.3f);
            case LEFT:
                return normalAnimation.getKeyFrame(0.1f);
            case RIGHT:
                return normalAnimation.getKeyFrame(0f);
            default:
                return normalAnimation.getKeyFrame(0.1f);
        }
    }

    public List<Direction> smallerDirection(List<Direction> directions) {
        List<Direction> ordered = new ArrayList<>();
        int size = directions.size();
        for (int i = 0; i < size; i++) {
            int smallerIndex = 0;
            Direction smaller = directions.get(0);
            Vector2 v = this.position.cpy();
            v.mulAdd(smaller.getUnitVector(), this.speed);
            float smallerDistance = this.objectivePosition.dst(v);
            for (int j = 0; j < directions.size(); j++) {
                Direction d = directions.get(j);
                v = this.position.cpy();
                v.mulAdd(d.getUnitVector(), this.speed);
                if (testCollision(v)) {
                    if (smallerDistance > this.objectivePosition.dst(v)) {
                        smaller = d;
                        smallerDistance = this.objectivePosition.dst(v);
                        smallerIndex = j;
                    }
                }
            }
            ordered.add(smaller);
            directions.remove(smallerIndex);
        }
        return ordered;
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

    public Ghost teleport(int x, int y) {
        setX(x);
        setY(y);
        setTargetX(x);
        setTargetY(y);
        setBounds(getX(), getY(), 16, 16);
        return this;
    }

    public int getTargetY() {
        return (int) targetPosition.y;
    }

    public void setTargetY(int targetY) {
        this.targetPosition.y = targetY;
    }

    public int getTargetX() {
        return (int) targetPosition.x;
    }

    public void setTargetX(int targetX) {
        this.targetPosition.x = targetX;
    }

    public boolean isInObjective() {
        return this.position.epsilonEquals(this.objectivePosition, 0);
    }

    public int getId() {
        return id;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isEated() {
        return eated;
    }

    public void setEated(boolean eated) {
        this.eated = eated;
    }

    public String getColor() {
        return color;
    }

    public void setWeakenedDuration(int weakenedDuration) {
        this.weakenedDuration = weakenedDuration * 250;
    }
}
