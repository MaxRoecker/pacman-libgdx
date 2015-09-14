package com.theosirian.pacman;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Game extends ApplicationAdapter implements InputProcessor {

    public static final byte MODE_INTRO_SCREEN = 0;
    public static final byte MODE_HELP_SCREEN = 1;
    public static final byte MODE_GAME_SCREEN = 2;
    public static final byte MODE_GAMEOVER_SCREEN = 3;
    public static final byte MODE_WINNER_SCREEN = 4;

    private final ServerConnection serverConnection = new ServerConnection(this);

    private SpriteBatch batch;
    private BitmapFont font;
    private TiledMap tiledMap;
    private OrthographicCamera camera;
    private TiledMapRenderer tiledMapRenderer;
    private Pacman pacman;
    protected int score;
    protected int lifes;
    private List<Ghost> ghosts;
    private List<Junction> junctions;
    private List<Pacdot> pacdots, destroyed;
    private List<Teleport> teleportPoints;
    private List<Vector2> playerSpawnPoints, fruitSpawnPoints, ghostSpawnPoints;
    private Random rand;
    private String[] mapFiles;
    private int currentMap;
    private int mode;
    private float transition, transitionTime;
    private TiledMap transitionMap;
    private TiledMapRenderer transitionMapRenderer;
    private int pacdotCount, bonusPacdotCount;
    private Screen screen;
    private Instant start;

    @Override
    public void create() {
        Gdx.input.setInputProcessor(this);
        mapFiles = Gdx.files.internal(Settings.MAP_LIST_PATH).readString().replaceAll("\r", "").split("\n");
        batch = new SpriteBatch();
        rand = new Random();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        transition = 0;
        transitionTime = 2f;
        transitionMap = null;
        loadTextures();
        loadMap(mapFiles[currentMap].trim());
        loadFont(Settings.FONT_PATH, Settings.FONT_SIZE);
        mode = MODE_INTRO_SCREEN;
    }

    @Override
    public void resume() {
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (transition > 0) {
            camera.update();
            transitionMap.getLayers().get("Map").setOpacity(transition / transitionTime);
            transitionMapRenderer.setView(camera);
            transitionMapRenderer.render();
            tiledMap.getLayers().get("Map").setOpacity(1 - (transition / transitionTime));
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();
            transition -= Gdx.graphics.getDeltaTime();
        } else {
            camera.update();
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();
            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            /*Check splash screen */
            if (mode != MODE_GAME_SCREEN) {
                screen.draw(batch);
                if (mode == MODE_GAMEOVER_SCREEN) {
                    font.setColor(Color.WHITE);
                    font.draw(batch, "SCORE: " + String.format("%04d", pacman.getScore()), 100, 170);
                }else if(mode == MODE_WINNER_SCREEN){
                    font.setColor(Color.WHITE);
                    font.draw(batch, "SCORE: " + String.format("%04d", pacman.getScore()), 100, 230);
                }


                /*font.setColor(Color.WHITE);
                font.draw(batch, "PRESS [SPACE] TO START", 1 * 16, 3 * 16);
                font.draw(batch, "PRESS [H] FOR HELP", 1 * 16, 2 * 16);
                /**/
            } else {
                pacman.update(Gdx.graphics.getDeltaTime());

                for (Ghost ghost : ghosts) {
                    ghost.update(Gdx.graphics.getDeltaTime());
                    if (ghost.isInObjective()) {
                        serverConnection.ghostNextPosition(ghost);
                        System.out.printf("Trocou! %s\n",ghost.getColor());
                        if (!ghost.isAlive()) {
                            ghost.setAlive(true);
                            ghost.setWeakenedDuration(0);
                        }
                    }
                    if (ghost.isEated()) {
                        ghost.setEated(false);
                        serverConnection.eatGhost(ghost);
                        ghost.setWeakenedDuration(0);
                    }
                }
                pacdots.stream().forEach(p -> p.update(Gdx.graphics.getDeltaTime()));

                for (Pacdot pacdot : pacdots) {
                    if (pacdot instanceof BonusPacdot) {
                        if (((BonusPacdot) pacdot).isEated()) {
                            serverConnection.bonusScore();
                        }
                    }
                    if (pacdot instanceof BigPacdot) {
                        if (pacdot.destroy) {
                            int time = serverConnection.eatSuperDot();
                            for (Ghost ghost : ghosts) {
                                ghost.setWeakenedDuration(time);
                            }
                        }
                    }
                }

                pacdots.stream().filter(p -> p.destroy).forEach(p -> {
                    destroyed.add(p);
                    p.dispose();
                });
                pacdots.removeAll(destroyed);
                float percentage = ((float) pacdots.size()) / pacdotCount;
                switch (bonusPacdotCount) {
                    case 0:
                        if (percentage <= 0.7f) {
                            Vector2 spawn = fruitSpawnPoints.get(rand.nextInt(fruitSpawnPoints.size()));
                            pacdots.add(new BonusPacdot((int) spawn.x * 16, (int) spawn.y * 16, pacman));
                            bonusPacdotCount++;
                        }
                        break;
                    case 1:
                        if (percentage <= 0.3f) {
                            Vector2 spawn = fruitSpawnPoints.get(rand.nextInt(fruitSpawnPoints.size()));
                            pacdots.add(new BonusPacdot((int) spawn.x * 16, (int) spawn.y * 16, pacman));
                            bonusPacdotCount++;
                        }
                        break;
                    default:
                        break;
                }
                destroyed.clear();
                teleportPoints.stream().forEach(tp -> tp.update(Gdx.graphics.getDeltaTime()));

                /*Check the update phase*/
                if (!pacman.isAlive()) {
                    pacman.setAlive(true);
                    serverConnection.death();
                    transitMap(currentMap);
                } else if (pacdots.isEmpty()) {
                    serverConnection.nextLevel();
                } else {
                    for (Pacdot p : pacdots) p.draw(batch);
                    pacman.draw(batch);
                    for (Ghost ghost : ghosts) ghost.draw(batch);
                    font.setColor(Color.WHITE);
                    font.draw(batch, "SCORE: " + String.format("%04d", pacman.getScore()), 160, 12);
                    font.draw(batch, "LIFES: " + String.format("%d", pacman.getLifeCounter()), 4, 12);
                    font.draw(batch, "TIME: " + String.format("%d", Duration.between(this.start,Instant.now()).toMillis()/1000), 4, 332);
                }
            }
            batch.end();
        }
    }

    @Override
    public void dispose() {
        System.out.println("Disposing of Textures...");
        SmallPacdot.sprite.dispose();
        BigPacdot.sprite.dispose();
        BonusPacdot.sprite.dispose();
    }

    private boolean loadTextures() {
        SmallPacdot.sprite = new Texture(Gdx.files.internal("pacman-small-pacdot.png"));
        BigPacdot.sprite = new Texture(Gdx.files.internal("pacman-big-pacdot.png"));
        BonusPacdot.sprite = new Texture(Gdx.files.internal("pacman-bonus-pacdot.png"));
        return SmallPacdot.sprite != null && BigPacdot.sprite != null && BonusPacdot.sprite != null;
    }

    private boolean loadFont(String file, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(file));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = size;
        font = generator.generateFont(params);
        generator.dispose();
        return font != null;
    }

    private boolean loadMap(String mapFile) {
        tiledMap = new TmxMapLoader().load(mapFile);
        MapProperties mapProperties = tiledMap.getProperties();
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        if (screen == null) {
            screen = new Screen(0, 0, "screen-splash.png");
        }

        if (mode != MODE_GAME_SCREEN) {
            /*if(screen == null){
                screen = new Screen(0,0);
            }
            screen.setScreen("screen-splash.png");*/
        } else {
            TiledMapTileLayer collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Map");
            TiledMapTileLayer objectLayer = ((TiledMapTileLayer) tiledMap.getLayers().get("Objects"));
            MapLayer teleportLayer = tiledMap.getLayers().get("Teleports");
            MapLayer junctionLayer = tiledMap.getLayers().get("Nodes");

            objectLayer.setOpacity(0);
            if (pacdots != null) {
                pacdots.stream().forEach(Pacdot::dispose);
                pacdots.clear();
            } else {
                pacdots = new ArrayList<>();
            }
            if (destroyed != null) {
                destroyed.stream().forEach(Pacdot::dispose);
                destroyed.clear();
            } else {
                destroyed = new ArrayList<>();
            }
            if (playerSpawnPoints != null) {
                playerSpawnPoints.clear();
            } else {
                playerSpawnPoints = new ArrayList<>();
            }
            if (fruitSpawnPoints != null) {
                fruitSpawnPoints.clear();
            } else {
                fruitSpawnPoints = new ArrayList<>();
            }
            if (ghostSpawnPoints != null) {
                ghostSpawnPoints.clear();
            } else {
                ghostSpawnPoints = new ArrayList<>();
            }
            if (teleportPoints != null) {
                teleportPoints.stream().forEach(Teleport::dispose);
                teleportPoints.clear();
            } else {
                teleportPoints = new ArrayList<>();
            }
            boolean quit = false;
            for (int i = 0, l1 = mapProperties.get("width", Integer.class); i < l1 && !quit; i++) {
                for (int j = 0, l2 = mapProperties.get("height", Integer.class); j < l2 && !quit; j++) {
                    TiledMapTileLayer.Cell c = objectLayer.getCell(i, j);

                    /*TiledMapTileLayer.Cell jota = collisionLayer.getCell(i,j);
                    if(jota != null){
                        if(jota.getTile() != null){
                            if(jota.getTile().getProperties() != null){
                                if(jota.getTile().getProperties().get("block") != null){
                                    if(jota.getTile().getProperties().get("block", String.class).equalsIgnoreCase("0")){
                                        System.out.printf("{%d,%d},\n",i,j);
                                    }
                                }
                            }
                        }
                    }*/

                    if (c == null) continue;
                    TiledMapTile t = c.getTile();
                    if (t == null) continue;
                    MapProperties properties = t.getProperties();
                    if (properties == null) continue;

                    String type = properties.get("type", String.class);
                    if (type == null) continue;
                    switch (type) {
                        case "bigDot":
                            pacdots.add(new BigPacdot(i * 16, j * 16, pacman));
                            //System.out.printf("Big Dot: {%d;%d}\n", i, j);
                            break;
                        case "smallDot":
                            pacdots.add(new SmallPacdot(i * 16, j * 16, pacman));
                            break;
                        case "ghostSpawn":
                            ghostSpawnPoints.add(new Vector2(i, j));
                            //System.out.printf("Ghost Spawn: {%d;%d}\n", i, j);
                            break;
                        case "fruitSpawn":
                            fruitSpawnPoints.add(new Vector2(i, j));
                            //System.out.printf("Fruit Spawn: {%d;%d}\n", i, j);
                            break;
                        case "playerSpawn":
                            playerSpawnPoints.add(new Vector2(i, j));
                            //System.out.printf("Player Spawn: {%d;%d}\n", i, j);
                            break;
                    }
                }
            }
            pacdotCount = pacdots.size();
            bonusPacdotCount = 0;
            Vector2 spawn = playerSpawnPoints.get(rand.nextInt(playerSpawnPoints.size()));

            if (pacman == null) {
                pacman = new Pacman((int) spawn.x * 16, (int) spawn.y * 16, collisionLayer);
                pacman.setScore(this.score);
                pacman.setLifeCounter(this.lifes);
            } else {
                pacman.teleport((int) spawn.x * 16, (int) spawn.y * 16).stopMoving().setCollisionLayer(collisionLayer);
            }

            pacdots.stream().forEach(p -> p.setPacman(pacman));

            String[] positions = serverConnection.getParameters();


            String[] ghostColors = {"blue", "orange", "pink", "red"};
            junctions = new ArrayList<>();
            ghosts = new ArrayList<>();
            for (int i = 0; i < ghostColors.length; i++) {
                String ghostColor = ghostColors[i];
                int gx = Integer.parseInt(positions[2 * i + 1]);
                int gy = Integer.parseInt(positions[2 * i + 2]);
                Ghost ghost = new Ghost(gx * 16, gy * 16, collisionLayer, ghostColor, this.pacman, i + 1);
                List<Junction> ghostJunctions = new ArrayList<>();
                for (MapObject junction : junctionLayer.getObjects()) {
                    if (junction.getProperties().containsKey("x") && junction.getProperties().containsKey("y")) {
                        int x = junction.getProperties().get("x", Float.class).intValue() / 16;
                        int y = junction.getProperties().get("y", Float.class).intValue() / 16;
                        Junction j = new Junction(x * 16, y * 16, ghost);
                        this.junctions.add(j);
                        ghostJunctions.add(j);
                    }
                }
                ghost.setJunctions(ghostJunctions);
                this.ghosts.add(ghost);
            }

            for (MapObject m : teleportLayer.getObjects()) {
                if (m.getProperties().containsKey("x") && m.getProperties().containsKey("y") && m.getProperties().containsKey("targetX") && m.getProperties().containsKey("targetY") && m.getProperties().containsKey("outDirection")) {
                    int x = m.getProperties().get("x", Float.class).intValue() / 16;
                    int y = m.getProperties().get("y", Float.class).intValue() / 16;
                    int targetX = Integer.parseInt(m.getProperties().get("targetX", String.class));
                    int targetY = Integer.parseInt(m.getProperties().get("targetY", String.class));
                    String outDirection = m.getProperties().get("outDirection", String.class);
                    teleportPoints.add(new Teleport(x * 16, y * 16, targetX * 16, targetY * 16, Entity.Direction.parseString(outDirection), pacman, this.ghosts));
                    //System.out.printf("Teleport: {%d;%d} -> {%d;%d} (%s)\n", x, y, targetX, targetY, outDirection);
                }
            }
        }
        return true;
    }

    void win(int finalScore) {
        this.mode = MODE_WINNER_SCREEN;
        transitMap(0);
        screen = new Screen(0, 0, "screen-winner.png");
        this.pacman = new Pacman(0,0,null);
        this.pacman.setScore(finalScore);
        System.out.println("Win!");
        return;
    }

    void gameover(int finalScore) {
        this.mode = MODE_GAMEOVER_SCREEN;
        transitMap(0);
        screen = new Screen(0, 0, "screen-gameover.png");
        this.pacman.setScore(finalScore);
        return;
    }


    @Override
    public boolean keyDown(int keycode) {
        //TODO: ADD MOVEMENT KEYS
        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                if (mode == MODE_GAME_SCREEN) {
                    pacman.setDirection(Pacman.Direction.UP);
                }
                break;
            case Input.Keys.S:
            case Input.Keys.DOWN:
                if (mode == MODE_GAME_SCREEN) {
                    pacman.setDirection(Pacman.Direction.DOWN);
                }
                break;
            case Input.Keys.A:
            case Input.Keys.LEFT:
                if (mode == MODE_GAME_SCREEN) {
                    pacman.setDirection(Pacman.Direction.LEFT);
                }
                break;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                if (mode == MODE_GAME_SCREEN) {
                    pacman.setDirection(Pacman.Direction.RIGHT);
                }
                break;
            case Input.Keys.H:
                if (mode == MODE_INTRO_SCREEN) {
                    mode = MODE_HELP_SCREEN;
                    screen = new Screen(0, 0, "screen-help.png");
                }
                break;
            case Input.Keys.SPACE:
                if (mode == MODE_INTRO_SCREEN || mode == MODE_HELP_SCREEN) {
                    mode = MODE_GAME_SCREEN;
                    /*Server*/
                    serverConnection.init();
                    serverConnection.nextLevel();
                } else if (mode == MODE_GAMEOVER_SCREEN) {
                    mode = MODE_INTRO_SCREEN;
                    serverConnection.init();
                    this.pacman.setLifeCounter(lifes);
                    screen = new Screen(0, 0, "screen-splash.png");
                } else if (mode == MODE_WINNER_SCREEN) {
                    mode = MODE_INTRO_SCREEN;
                    screen = new Screen(0, 0, "screen-splash.png");
                }
                break;
            case Input.Keys.ESCAPE:
                Gdx.app.exit();
                break;
        }
        return false;
    }

    public void transitMap(int toMap) {
        this.currentMap = toMap;
        transitionMap = tiledMap;
        transitionMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        transition = transitionTime;
        loadMap(mapFiles[toMap].trim());
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    public Pacman getPacman() {
        return pacman;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }
}
