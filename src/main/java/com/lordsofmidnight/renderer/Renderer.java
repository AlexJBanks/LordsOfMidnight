package com.lordsofmidnight.renderer;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

import com.lordsofmidnight.gamestate.points.PointMap;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import com.lordsofmidnight.objects.Entity;
import com.lordsofmidnight.objects.Pellet;
import com.lordsofmidnight.objects.PowerUpBox;
import com.lordsofmidnight.objects.powerUps.PowerUp;
import com.lordsofmidnight.utils.GameLoop;
import com.lordsofmidnight.gamestate.maps.Map;
import com.lordsofmidnight.gamestate.points.Point;
import com.lordsofmidnight.utils.ResourceLoader;
import com.lordsofmidnight.utils.Settings;
import com.lordsofmidnight.utils.UpDownIterator;
import com.lordsofmidnight.utils.enums.MapElement;
import com.lordsofmidnight.utils.enums.RenderingMode;

public class Renderer {

  private final GraphicsContext gc;
  private final long secondInNanoseconds = (long) Math.pow(10, 9);
  private final HeadsUpDisplay hudRender;
  private final ProjectileFX projectileManager;
  private Map map;
  private ResourceLoader r;
  private int xResolution;
  private int yResolution;
  private Point2D.Double mapRenderingCorner;
  private ArrayList<Image> mapTiles;
  private Image background;
  private BufferedImage palette;
  private double tileSizeX;
  private double tileSizeY;
  private int clientID;
  private Font geoSmall;
  private Font geoLarge;
  private long lastFrame;
  private int fps = 0;
  private int frameCounter = 0;
  private long timeSum;
  private Entity clientEntity = null;
  private BufferedImage playerColours;
  private ExplosionFX explosionManager;

  private ArrayList<Point2D.Double> traversalOrder = new ArrayList<>();

  /**
   * @param _gc Graphics context to render the game onto
   * @param _xResolution Game x resolution
   * @param _yResolution Game y resolution
   * @param r Asset loader
   */
  public Renderer(GraphicsContext _gc, int _xResolution, int _yResolution, ResourceLoader r) {
    this.r = r;
    this.map = r.getMap();
    this.gc = _gc;
    this.xResolution = _xResolution;
    this.yResolution = _yResolution;
    this.background = r.getBackground();
    this.palette = r.getBackgroundPalette();
    this.playerColours = r.getPlayerPalette();
    this.hudRender = new HeadsUpDisplay(gc, _xResolution, _yResolution, r);
    this.explosionManager = new ExplosionFX(gc, r);
    this.projectileManager = new ProjectileFX(gc, r, this);
    this.initMapTraversal(r.getMap());
  }

  /**
   * @param map Game Map
   * @param entityArr Playable com.lordsofmidnight.objects
   * @param now Current game time in nanoseconds
   * @param pellets Consumable com.lordsofmidnight.objects
   */
  public void render(
      Map map,
      Entity[] entityArr,
      long now,
      PointMap<Pellet> pellets,
      ConcurrentHashMap<UUID, PowerUp> activePowerUps,
      int gameTime) {

    this.clientEntity = getClientEntity(new ArrayList<>(Arrays.asList(entityArr)));

    long timeElapsed = now - lastFrame;
    // clear screen
    gc.clearRect(0, 0, xResolution, yResolution);
    renderBackground(map);
    renderGameOnly(map, entityArr, now, pellets, activePowerUps);
    hudRender.renderHUD(entityArr, gameTime);
    hudRender.renderInventory(
        getClientEntity(new ArrayList<>(Arrays.asList(entityArr))), timeElapsed);
    // showFPS(timeElapsed);

    lastFrame = now;

    if (clientEntity.isDead()) {
      int timeUntilRespawn =
          Math.round((clientEntity.getDeathTime() - clientEntity.getDeathCounter()) / 100);
      hudRender.renderDeathScreen(timeUntilRespawn, clientEntity);
    }
  }

  /** initialises map array, map traversal order, map tiles and fonts */
  public void initMapTraversal(Map map) {

    final int ROW = map.getMaxX();
    final int COL = map.getMaxY();

    this.traversalOrder = new ArrayList<>();
    // find diagonal traversal order (map depth order traversal)
    for (int line = 1; line <= (ROW + COL - 1); line++) {
      int start_col = Math.max(0, line - ROW);

      int count = Math.min(line, Math.min(COL - start_col, ROW));

      // Print elements of this line
      for (int j = 0; j < count; j++) {
        int x = Math.min(ROW, line) - j - 1;
        int y = start_col + j;
        this.traversalOrder.add(new Double(x, y));
      }
    }

    this.mapTiles = r.getMapTiles();
    this.mapRenderingCorner = getMapRenderingCorner();
    tileSizeX = r.getMapTiles().get(0).getWidth();
    tileSizeY = r.getMapTiles().get(0).getHeight();

    // set fonts
    final double fontRatio = 0.07;
    try {
      this.geoLarge =
          Font.loadFont(
              new FileInputStream(new File("src/main/resources/font/Geo-Regular.ttf")),
              xResolution * fontRatio);
      this.geoSmall =
          Font.loadFont(
              new FileInputStream(new File("src/main/resources/font/Geo-Regular.ttf")),
              0.8 * xResolution * fontRatio);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param colour sets Graphics context fill colour using an intRGB (gc.setFillColour only allows
   *     setting of colour com.lordsofmidnight.objects)
   */
  public static Color intRGBtoColour(int colour) {
    return new Color(
        (colour >> 16 & 0xFF) / (double) 255,
        (colour >> 8 & 0xFF) / (double) 255,
        (colour & 0xFF) / (double) 255,
        1);
  }

  public void renderGameOnly(
      Map map,
      Entity[] entityArr,
      long now,
      PointMap<Pellet> pellets,
      ConcurrentHashMap<UUID, PowerUp> activePowerUps) {

    int[][] rawMap = map.raw();
    ArrayList<Entity> entities = new ArrayList<>(Arrays.asList(entityArr));
    // sort entities to get rendering order
    entities.sort(Comparator.comparingDouble(o -> o.getLocation().getX() + o.getLocation().getY()));

    int entityCounter = 0;
    Image currentSprite = null;
    Double rendCoord;
    Point spriteCoord = new Point(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE);

    int x;
    int y;

    HashMap<Entity, HashMap<com.lordsofmidnight.utils.enums.PowerUp, PowerUp>> entityPowerUps =
        new HashMap<>();
    for (Entity e : entityArr) {
      entityPowerUps.put(e, new HashMap<>());
    }
    if (activePowerUps != null) {
      for (PowerUp p : activePowerUps.values()) {
        entityPowerUps.get(p.getUser()).put(p.getType(), p);
      }
    }

    // Render floor first (floors will never be on a higher layer than anything apart form the
    // background
    for (Double coord : traversalOrder) {
      x = (int) coord.getX();
      y = (int) coord.getY();

      if (MapElement.FLOOR.toInt() == rawMap[x][y]) {
        rendCoord =
            getIsoCoord(
                x,
                y,
                mapTiles.get(MapElement.FLOOR.toInt()).getHeight(),
                mapTiles.get(MapElement.FLOOR.toInt()).getWidth());
        gc.drawImage(mapTiles.get(MapElement.FLOOR.toInt()), rendCoord.x, rendCoord.y);
      }
    }

    // TODO refactor the way the translucent pellet is fetched
    Image translucentPellet = r.getTranslucentPellet().get(0);
    // TODO refactor the way the translucent pellet is fetched
    Image pellet = r.getPellet().get(0);
    // TODO refactor the way the translucent pellet is fetched
    Image powerup = r.getPowerBox().get(0);

    // Loop through grid in diagonal traversal to render walls and entities by depth
    for (Double coord : traversalOrder) {

      // render consumable com.lordsofmidnight.objects on top
      Pellet currentPellet = pellets.get(new Point(coord.getX(), coord.getY(), map));
      if (currentPellet != null && currentPellet.isActive()) {

        // TODO use better way of finding if client is mipsman
        Entity client = getClientEntity(entities);

        if (currentPellet.canUse(client)) {
          if (currentPellet instanceof PowerUpBox) {
            currentSprite = powerup;
          } else {
            currentSprite = pellet;
          }
        } else {
          if (currentPellet instanceof PowerUpBox) {
            // currentSprite = powerup;
          } else {
            currentSprite = translucentPellet;
          }
        }

        // render pellet using either translucent or opaque sprite
        double x_ = currentPellet.getLocation().getX() - 0.5;
        double y_ = currentPellet.getLocation().getY() - 0.5;
        rendCoord = getIsoCoord(x_, y_, currentSprite.getHeight(), currentSprite.getWidth());
        gc.drawImage(currentSprite, rendCoord.getX(), rendCoord.getY());
      }

      x = (int) coord.getX();
      y = (int) coord.getY();

      currentSprite = mapTiles.get(rawMap[x][y]);
      rendCoord = getIsoCoord(x, y, currentSprite.getHeight(), currentSprite.getWidth());
      if (MapElement.FLOOR.toInt() == rawMap[x][y]) {
        continue;
      }

      // render wall (or any other non passable terrain)
      gc.drawImage(currentSprite, rendCoord.x, rendCoord.y);

      if (entityCounter < entities.size()) {
        spriteCoord = entities.get(entityCounter).getLocation();
      }

      // is the current entities depth the same or deeper than the wall just rendered?
      while (entityCounter < entities.size()
          && ((x + y) >= ((int) spriteCoord.getX() + (int) spriteCoord.getY()))
          && spriteCoord.getX() > x) {

        if (now == 0) {
          renderEntity(entities.get(entityCounter), null, 0);
          entityCounter++;
        } else {
          Entity entityToRender = entities.get(entityCounter);
          renderEntity(entityToRender, entityPowerUps.get(entityToRender), now - lastFrame);
          entityCounter++;
        }

        // point to the next entity
        if (entityCounter < entities.size()) {
          spriteCoord = entities.get(entityCounter).getLocation();
        }
      }
    }
    if (now != 0) {
      explosionManager.render(now - lastFrame);
      projectileManager.render(now - lastFrame, activePowerUps);
    }
  }

  private Entity getClientEntity(ArrayList<Entity> entities) {
    // TODO refactor the way the render knows the client is MIPSman
    for (Entity e : entities) {
      if (e.getClientId() == clientID) {
        return e;
      }
    }
    return null;
  }

  /** @param timeElapsed current time in nanoseconds */
  private void showFPS(long timeElapsed) {

    gc.setTextAlign(TextAlignment.CENTER);
    if (timeSum > secondInNanoseconds) {
      fps = frameCounter / (int) (timeSum / secondInNanoseconds);
      gc.fillText("FPS:" + fps, xResolution / 2, yResolution - 100);
      timeSum = 0;
      frameCounter = 0;
    } else {
      gc.fillText("FPS:" + fps, xResolution / 2, yResolution - 100);
      timeSum += timeElapsed;
      frameCounter++;
    }
  }

  /**
   * allows com.lordsofmidnight.renderer to show a marker on who is the client's entity
   *
   * @param _id the ID of the entity which the client controls
   */
  public void setClientID(int _id) {
    this.clientID = _id;
  }

  public void renderCollisionAnimation(
      Entity newMipsMan,
      Entity[] entities,
      Map map,
      AnimationTimer renderingLoop,
      GameLoop inputProcessor) {
    java.lang.Double[] num = {1.0, 1.0, 1.1, 1.25, 1.4};
    UpDownIterator<java.lang.Double> entitySize = new UpDownIterator<>(num);

    java.lang.Double[] opacity = new java.lang.Double[4];
    for (int i = 0; i < opacity.length; i++) {
      opacity[i] = 0.5 + i * 0.06;
    }
    UpDownIterator<java.lang.Double> backgroundOpacity = new UpDownIterator<>(opacity);

    Image currentSprite = newMipsMan.getImage().get(newMipsMan.getCurrentFrame());
    final double renderAnimationTime = 0.75 * Math.pow(10, 9);
    double startTime = System.nanoTime();
    final int frames = 40;
    final double frameTime = renderAnimationTime / frames;
    new AnimationTimer() {
      double currentTime = System.nanoTime();
      double multiplier = entitySize.next();
      double opacity = backgroundOpacity.next();

      @Override
      public void handle(long now) {
        if (now - startTime > renderAnimationTime) {
          renderingLoop.start();
          inputProcessor.unpause();
          this.stop();
        } else {
          if (System.nanoTime() - currentTime > frameTime) {
            multiplier = entitySize.next();
            opacity = backgroundOpacity.next();
            currentTime = System.nanoTime();
          }
          renderCollision(newMipsMan, entities, map, multiplier, opacity, currentSprite);
        }
      }
    }.start();
  }

  private void renderCollision(
      Entity newMipsMan,
      Entity[] entities,
      Map map,
      double sizeMultiplier,
      double backgroundOpacity,
      Image currentSprite) {
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setFont(geoLarge);
    renderBackground(map);
    renderGameOnly(map, entities, 0, new PointMap<>(map), null);
    gc.setFill(new Color(0, 0, 0, backgroundOpacity));
    gc.fillRect(0, 0, xResolution, yResolution);

    double x = newMipsMan.getLocation().getX() - 0.5;
    double y = newMipsMan.getLocation().getY() - 0.5;
    Double rendCoord =
        getIsoCoord(
            x,
            y,
            currentSprite.getHeight() * sizeMultiplier,
            currentSprite.getWidth() * sizeMultiplier);

    gc.drawImage(
        currentSprite,
        rendCoord.getX(),
        rendCoord.getY(),
        currentSprite.getWidth() * sizeMultiplier,
        currentSprite.getHeight() * sizeMultiplier);
    gc.setFill(intRGBtoColour(playerColours.getRGB(1, newMipsMan.getClientId())));
    gc.fillText(newMipsMan.getName(), xResolution / 2, yResolution * 0.2);
    gc.fillText("CAPTURED MIPS", xResolution / 2, yResolution * 0.45);
    gc.setStroke(Color.WHITE);
    gc.setLineWidth(2 * (yResolution / 768));
    gc.strokeText(newMipsMan.getName(), xResolution / 2, yResolution * 0.2);
    gc.strokeText("CAPTURED MIPS", xResolution / 2, yResolution * 0.45);
  }

  /**
   * @param x cartesian x coordinate
   * @param y cartesian Y coordinate
   * @param spriteHeight vertical offset
   */
  public Point2D.Double getIsoCoord(double x, double y, double spriteHeight, double spriteWidth) {
    double isoX =
        mapRenderingCorner.getX()
            - (y - x) * (this.tileSizeX / (double) 2)
            + ((tileSizeX - spriteWidth) / 2);
    double isoY =
        mapRenderingCorner.getY()
            + (y + x) * (this.tileSizeY / (double) 2)
            + (tileSizeY - spriteHeight);
    return new Point2D.Double(isoX, isoY);
  }

  /**
   * @param e entitiy to render
   * @param timeElapsed time since last frame to decide whether to move to next animation frame
   */
  private void renderEntity(
      Entity e,
      HashMap<com.lordsofmidnight.utils.enums.PowerUp, PowerUp> selfPowerUps,
      long timeElapsed) {
    // choose correct animation
    ArrayList<Image> currentSprites = e.getImage();
    if (secondInNanoseconds / e.getAnimationSpeed() < e.getTimeSinceLastFrame()
        && timeElapsed > 0) {
      e.setTimeSinceLastFrame(0);
      e.nextFrame();
      for (PowerUp p : selfPowerUps.values()) {
        p.incrementFrame();
      }
    } else {
      e.setTimeSinceLastFrame(e.getTimeSinceLastFrame() + timeElapsed);
    }
    Image currentSprite = currentSprites.get(e.getCurrentFrame());

    double x = e.getLocation().getX() - 0.5;
    double y = e.getLocation().getY() - 0.5;
    Point2D.Double rendCoord =
        getIsoCoord(x, y, currentSprite.getHeight(), currentSprite.getWidth());

    Point deathLocation = e.getDeathLocation();
    if (deathLocation != null) {
      Point loc = e.getLocation();
      Point2D.Double coord =
          getIsoCoord(loc.getX(), loc.getY(), currentSprite.getWidth(), currentSprite.getHeight());
      explosionManager.addExplosion(coord.getX(), coord.getY());
    }

    if (e.isDead() && e.getDeathCounter() > e.getDeathTime() * 0.5) {
      if (secondInNanoseconds / e.getAnimationSpeed() < e.getTimeSinceLastFrame()
          && timeElapsed > 0) {
        e.toggleHidden();
      }
      if (!e.getHidden()) {
        gc.drawImage(currentSprite, rendCoord.getX(), rendCoord.getY());
      }
      return;
    }
    if (e.isDead()) {
      return;
    }

    gc.drawImage(currentSprite, rendCoord.getX(), rendCoord.getY());

    if (selfPowerUps != null) {
      renderPowerUpEffects(e, selfPowerUps, rendCoord);
    }
    // render marker for entity
    if (e.getClientId() != clientID && !e.isMipsman()) {
      return;
    }

    Image marker = (e.isMipsman()) ? r.getMipMarker() : r.getMClientMarker();
    Point2D.Double coord =
        getIsoCoord(x, y, marker.getHeight() + currentSprite.getHeight(), marker.getWidth());

    gc.drawImage(marker, coord.getX(), coord.getY());
  }

  private void renderPowerUpEffects(
      Entity e,
      HashMap<com.lordsofmidnight.utils.enums.PowerUp, PowerUp> selfPowerUps,
      Double rendCoord) {
    if (e.isSpeeding()) {
      PowerUp speed = selfPowerUps.get(com.lordsofmidnight.utils.enums.PowerUp.SPEED);
      ArrayList<Image> sprites = r.getPowerUps().get(com.lordsofmidnight.utils.enums.PowerUp.SPEED);
      gc.drawImage(
          sprites.get(speed.getCurrentFrame() % sprites.size()),
          rendCoord.getX(),
          rendCoord.getY());
    }
    if (e.isInvincible()) {
      PowerUp invincible = selfPowerUps.get(com.lordsofmidnight.utils.enums.PowerUp.INVINCIBLE);
      gc.drawImage(
          r.getPowerUps()
              .get(com.lordsofmidnight.utils.enums.PowerUp.INVINCIBLE)
              .get(
                  invincible.getCurrentFrame()
                      % r.getPowerUps()
                          .get(com.lordsofmidnight.utils.enums.PowerUp.INVINCIBLE)
                          .size()),
          rendCoord.getX(),
          rendCoord.getY());
    }
    // is the entity stunned?
    if (e.isStunned()) {
      gc.drawImage(
          r.getPowerUps().get(com.lordsofmidnight.utils.enums.PowerUp.WEB).get(0),
          rendCoord.getX(),
          rendCoord.getY());
    }
  }

  /**
   * render the background image and pyramid under game map
   *
   * @param map game map
   */
  private void renderBackground(Map map) {
    // render backing image
    final double MAP_BORDER = xResolution * 0.005;
    gc.drawImage(background, 0, 0, xResolution, yResolution);

    // Render map base
    Point2D.Double tmpCoord = getIsoCoord(-1, -1, tileSizeY, tileSizeX);
    Point2D.Double topLeft =
        new Double(tmpCoord.getX() + 0.5 * tileSizeX, tmpCoord.getY() - 0.5 * MAP_BORDER);

    tmpCoord = getIsoCoord(map.getMaxX(), -1, tileSizeY, tileSizeX);
    Point2D.Double topRight =
        new Double(tmpCoord.getX() + MAP_BORDER + tileSizeX, tmpCoord.getY() + 0.5 * tileSizeY);

    tmpCoord = getIsoCoord(-1, map.getMaxY(), tileSizeY, tileSizeX);
    Point2D.Double bottomLeft =
        new Double(tmpCoord.getX() - 0.5 * MAP_BORDER, tmpCoord.getY() + 0.5 * tileSizeY);

    tmpCoord = getIsoCoord(map.getMaxX(), map.getMaxY(), tileSizeY, tileSizeX);
    Point2D.Double bottomRight =
        new Double(
            tmpCoord.getX() + 0.5 * tileSizeX, tmpCoord.getY() + 0.5 * MAP_BORDER + tileSizeY);

    // get first colour from palette (lightest tone)
    gc.setFill(intRGBtoColour(palette.getRGB(0, 0)));
    gc.fillPolygon(
        new double[] {topLeft.getX(), topRight.getX(), bottomRight.getX(), bottomLeft.getX()},
        new double[] {topLeft.getY(), topRight.getY(), bottomRight.getY(), bottomLeft.getY()},
        4);

    // Render Pyramid underside

    double yChange = topRight.getX() - bottomRight.getX();

    double percentageXRes = 0.04;
    double ratio = ((percentageXRes * xResolution) / yChange) * (map.getMaxY() / (double) 20);

    double x =
        getIsoCoord(map.getMaxX() / (double) 2, map.getMaxY() / (double) 2, tileSizeY, tileSizeX)
            .getX();
    double y = bottomRight.getY() + yChange * ratio;

    Point2D.Double pyramidVertex = new Point2D.Double(x, y);

    // get third colour from palette (darkest tone)
    gc.setFill(intRGBtoColour(palette.getRGB(2, 0)));
    gc.fillPolygon(
        new double[] {topRight.getX(), bottomRight.getX(), pyramidVertex.getX()},
        new double[] {topRight.getY(), bottomRight.getY(), pyramidVertex.getY()},
        3);

    // get second colour from palette (medium tone)
    gc.setFill(intRGBtoColour(palette.getRGB(1, 0)));
    gc.fillPolygon(
        new double[] {bottomLeft.getX(), bottomRight.getX(), pyramidVertex.getX()},
        new double[] {bottomLeft.getY(), bottomRight.getY(), pyramidVertex.getY()},
        3);

    // Draw black outline
    gc.setStroke(Color.BLACK);
    gc.strokePolygon(
        new double[] {bottomLeft.getX(), bottomRight.getX(), pyramidVertex.getX()},
        new double[] {bottomLeft.getY(), bottomRight.getY(), pyramidVertex.getY()},
        3);

    gc.strokePolygon(
        new double[] {topRight.getX(), bottomRight.getX(), pyramidVertex.getX()},
        new double[] {topRight.getY(), bottomRight.getY(), pyramidVertex.getY()},
        3);

    gc.strokePolygon(
        new double[] {topLeft.getX(), topRight.getX(), bottomRight.getX(), bottomLeft.getX()},
        new double[] {topLeft.getY(), topRight.getY(), bottomRight.getY(), bottomLeft.getY()},
        4);
  }

  /** @param entities playable entities to get their scores */
  private void renderHUD(Entity[] entities) {
    gc.setFill(Color.WHITE);
    final double paddingRatio = 0.1;
    final double offset = paddingRatio * yResolution;
    double nameScoreGap = yResolution * paddingRatio;

    // calculate corner coordinate to render other players scores from
    Point2D.Double topLeft = new Double(offset, offset);
    Point2D.Double topRight = new Double(xResolution - offset, offset);
    Point2D.Double botLeft = new Double(offset, yResolution - offset - nameScoreGap);
    Point2D.Double botRight = new Double(xResolution - offset, yResolution - offset - nameScoreGap);

    ArrayList<Point2D.Double> scoreCoord =
        new ArrayList<>(Arrays.asList(topLeft, topRight, botLeft, botRight));

    // calculate number of other players
    Entity[] otherPlayers = new Entity[entities.length - 1];
    Entity self = null;
    int playerCounter = 0;
    for (Entity e : entities) {
      if (e.getClientId() != clientID) {
        otherPlayers[playerCounter] = e;
        playerCounter++;
      } else {
        self = e;
      }
    }

    // render own score
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setFont(geoLarge);
    gc.fillText("Score:" + self.getScore(), xResolution / 2, yResolution / 13);

    // render other players scores
    for (int i = 0; i < otherPlayers.length; i++) {
      if ((i % 2 == 0)) {
        gc.setTextAlign(TextAlignment.LEFT);
      } else {
        gc.setTextAlign(TextAlignment.RIGHT);
      }
      Point2D.Double cornerCoord = scoreCoord.get(i);
      gc.setFont(geoSmall);
      gc.fillText(
          "Score:" + otherPlayers[i].getScore(),
          cornerCoord.getX(),
          cornerCoord.getY() + nameScoreGap);
      gc.setFont(geoLarge);
      gc.fillText("Player" + otherPlayers[i].getClientId(), cornerCoord.getX(), cornerCoord.getY());
    }
  }

  /** @return The top right corner coordinate to start rendering game map from */
  private Point2D.Double getMapRenderingCorner() {

    double bottomLeftX = -map.getMaxY() * (this.tileSizeX / (double) 2);
    double topRightX = map.getMaxX() * (this.tileSizeX / (double) 2);
    double mapMidPointX = bottomLeftX + 0.5 * Math.abs(topRightX - bottomLeftX);
    //    System.out.println("offset: " + mapMidPointX);
    return new Point2D.Double((this.xResolution / (double) 2) - mapMidPointX, yResolution / 6);
  }

  /** use to override settings given by the settings class */
  public void setResolution(int x, int y) {
    r.refreshSettings(x, y, RenderingMode.SMOOTH_SCALING, Settings.getTheme());
    hudRender.setResolution(x, y);
    this.xResolution = x;
    this.yResolution = y;
    this.map = r.getMap();
    this.initMapTraversal(this.map);
    this.tileSizeX = r.getMapTiles().get(0).getWidth();
    this.tileSizeY = r.getMapTiles().get(0).getHeight();
    this.mapRenderingCorner = getMapRenderingCorner();
    this.background = r.getBackground();
    this.palette = r.getBackgroundPalette();
    this.explosionManager.refreshSettings();
  }

  public void refreshSettings() {
    r.refreshSettings();
    hudRender.setResolution(Settings.getxResolution(), Settings.getyResolution());
    this.xResolution = Settings.getxResolution();
    this.yResolution = Settings.getyResolution();
    this.map = r.getMap();
    this.initMapTraversal(this.map);
    this.mapRenderingCorner = getMapRenderingCorner();
    this.background = r.getBackground();
    this.palette = r.getBackgroundPalette();
    this.explosionManager.refreshSettings();
  }
}
