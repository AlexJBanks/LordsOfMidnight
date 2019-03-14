package com.lordsofmidnight.gamestate.maps;

import java.util.HashMap;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.lordsofmidnight.objects.Entity;
import com.lordsofmidnight.renderer.Renderer;
import com.lordsofmidnight.utils.ResourceLoader;

/**
 * @author Tim Cheung
 */
public class MapPreview {

  private final ResourceLoader resourceLoader;
  private int xRes;
  private int yRes;

  /**
   * @param x x resolution of previews
   * @param y y resolution of previews
   */
  public MapPreview(int x, int y) {
    this.xRes = x;
    this.yRes = y;
    this.resourceLoader = new ResourceLoader("src/com.lordsofmidnight.main/resources/");
  }

  /**
   * @param mapName name of map file (no file extension)
   * @return JavaFx image of a preview of the map
   */
  public Image getMapPreview(String mapName) {

    //create a separate instance of resource loader from the game to not overwrite its loaded themes

    //load map name into resource loader
    resourceLoader.loadMap(mapName);

    return getScreenshot(resourceLoader.getMap());
  }

  public Image getMapPreview(Map map) {
    resourceLoader.loadMap(map);
    return getScreenshot(map);
  }

  private Image getScreenshot(Map map) {
    Canvas canvas = new Canvas(xRes, yRes);
    Group screenshotGroup = new Group();
    screenshotGroup.getChildren().add(canvas);

    Scene previewScene = new Scene(screenshotGroup);

    Stage hiddenWindow = new Stage();
    hiddenWindow.setScene(previewScene);

    GraphicsContext gc = canvas.getGraphicsContext2D();

    Renderer renderer = new Renderer(gc, xRes, yRes, resourceLoader);
    renderer.setResolution(this.xRes, this.yRes);

    gc.setFill(Color.TRANSPARENT);
    gc.fillRect(0, 0, xRes, yRes);
    renderer.renderGameOnly(map, new Entity[]{}, 0, new HashMap<>(), null);

    SnapshotParameters parameters = new SnapshotParameters();
    parameters.setFill(Color.TRANSPARENT);
    WritableImage screenshot = new WritableImage(xRes, yRes);

    canvas.snapshot(parameters, screenshot);

    hiddenWindow.hide();

    return screenshot;
  }
}
