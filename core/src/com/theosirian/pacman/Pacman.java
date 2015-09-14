package com.theosirian.pacman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

public class Pacman extends Entity {

    private Animation anim;
	private int movementPredictionCounter, score, lifeCounter;
	private float speed, animTime = 0f;
    private Direction previousDirection, movementPrediction;
    private Vector2 targetPosition;
    private boolean alive;

    public Pacman(int x, int y, TiledMapTileLayer collisionLayer) {
        super(x, y, collisionLayer);
        Texture texture = new Texture(Gdx.files.internal("pacman.png"));
	    int tileSize = texture.getHeight();
	    int animationFrameCount = 4;
        TextureRegion[] regions = new TextureRegion[animationFrameCount];
        for (int i = 0; i < animationFrameCount; i++) {
            regions[i] = new TextureRegion(texture, tileSize * i, 0, tileSize, tileSize);
        }
        anim = new Animation(0.1f, regions);
        targetPosition = new Vector2(x, y);
        setBounds(getX(), getY(), 16, 16);
        speed = 2;
        direction = Direction.NONE;
        previousDirection = Direction.NONE;
        movementPrediction = Direction.NONE;
        movementPredictionCounter = 0;
        score = 0;
        alive = true;
    }

	@Override
    public void update(float delta) {
        animTime += delta;
        currentFrame = anim.getKeyFrame(direction != Direction.NONE ? animTime : 0, true);
        if (!position.epsilonEquals(targetPosition, 0.001f)) {
            position.set(targetPosition);
        } else {
            Vector2 wantToMove = Vector2.Zero;
            boolean noPrediction = true;
            if (movementPrediction != Direction.NONE) {
                wantToMove.set(position.cpy().mulAdd(movementPrediction.getUnitVector(), speed));
                if (testCollision(wantToMove)) {
                    setDirection(movementPrediction);
                    movementPrediction = Direction.NONE;
                    movementPredictionCounter = 0;
                    noPrediction = false;
                } else if (movementPredictionCounter > 12) {
                    movementPrediction = Direction.NONE;
                    movementPredictionCounter = 0;
                } else {
                    movementPredictionCounter++;
                }
            }
            if (noPrediction) {
                wantToMove.set(position.cpy().mulAdd(direction.getUnitVector(), speed));
                if (testCollision(wantToMove)) {
                    setDirection(direction);
                    targetPosition.set(wantToMove);
                } else {
                    wantToMove.set(position.cpy().mulAdd(previousDirection.getUnitVector(), speed));
                    if (testCollision(wantToMove)) {
                        movementPrediction = direction;
                        movementPredictionCounter = 0;
                        setDirection(previousDirection);
                        targetPosition.set(wantToMove);
                    }
                }
            }
        }
        setRotation(direction.toRotation());
		setBounds(getX(), getY(), 16, 16);
    }

	@Override
    public void draw(Batch batch) {
		super.draw(batch);
        batch.draw(currentFrame, position.x, position.y, origin.x, origin.y, size.x, size.y, scale.x, scale.y, rotation);
    }

    public Pacman teleport(int x, int y) {
        setX(x);
        setY(y);
        setTargetX(x);
        setTargetY(y);
	    setBounds(getX(), getY(), 16, 16);
        return this;
    }

    public Pacman stopMoving() {
        direction = Direction.NONE;
        previousDirection = Direction.NONE;
        movementPrediction = Direction.NONE;
        movementPredictionCounter = 0;
        return this;
    }

	@Override
	public void setDirection(Direction dir) {
		this.previousDirection = this.direction;
		this.direction = dir;
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

	public void changeScore(int i) {
		this.score += i;
	}

	public int getScore() {
		return this.score;
	}

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLifeCounter() {
        return lifeCounter;
    }

    public void setLifeCounter(int lifeCounter) {
        this.lifeCounter = lifeCounter;
    }
}
