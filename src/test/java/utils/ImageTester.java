package utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.geom.Point2D.Double;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;
import objects.Entity;
import renderer.Renderer;

public class ImageTester extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
    int xRes = screenRes.width;
    int yRes = screenRes.height;
    final Canvas canvas = new Canvas(xRes, yRes);
    final GraphicsContext gc = canvas.getGraphicsContext2D();

    ResourceLoader resourceLoader = new ResourceLoader("src/test/resources/");

    resourceLoader.loadMap("9x18plain");
    Map map = resourceLoader.getMap();
      Renderer r = new Renderer(gc, xRes, yRes, resourceLoader);

    Entity mip = new Entity(true, 1, new Double(1, 1));
    mip.setPacMan(true);

    Entity ghoul = new Entity(false, 4, new Double(1.5, 5.8));
    System.out.println(ghoul.getLocation().toString());
    ghoul.setPacMan(false);
    //ghoul.setDirection(Direction.RIGHT);

    Entity[] entities = new Entity[]{ghoul};
    Methods.updateImages(entities, resourceLoader);

    r.render(map, entities);
    stage.setScene(new Scene(new Group(canvas)));
    stage.show();

  }
}
