package main;

import audio.AudioController;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import objects.Entity;
import objects.Pellet;
import renderer.Renderer;
import server.ClientLobbySession;
import server.ServerGameplayHandler;
import server.ServerLobby;
import server.telemeters.DumbTelemetry;
import server.telemeters.HostTelemetry;
import server.telemeters.Telemetry;
import ui.MenuController;
import utils.Input;
import utils.Map;
import utils.Methods;
import utils.ResourceLoader;
import utils.enums.Direction;
import utils.enums.RenderingMode;
import utils.enums.ScreenResolution;

public class Client extends Application {

  Map map;
  private int id;
  private String name;
  private String[] playerNames;
  private KeyController keyController;
  //  private HostTelemetry telemetry;
  private Telemetry telemetry;
  private AudioController audioController;
  private Scene gameScene;
  private Stage primaryStage;
  private Renderer renderer;
  private int xRes = 1920;
  private int yRes = 1080;
  private ScreenResolution screenRes = ScreenResolution.LOW;
  private RenderingMode renderingMode = RenderingMode.STANDARD_SCALING;
  private ResourceLoader resourceLoader;
  private Entity[] agents;
  private Queue<Input> inputs;
  private ServerLobby server;
  private ServerGameplayHandler serverGameplayHandler;
  private ClientLobbySession clientLobbySession;
  private Queue<String> clientIn;
  private Queue<Input> keypressQueue;
  private boolean isHost;
  private boolean singlePlayer = false;
  private BlockingQueue<Input> incomingQueue; // only used in singleplayer
  private HashMap<String, Pellet> pellets;
  private int MIPID;
  private Canvas canvas = new Canvas();

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
    this.renderer.setClientID(id);
  }

  public String[] getPlayerNames() {
    return this.playerNames;
  }

  public void setPlayerNames(String[] names) {
    this.playerNames = names;
  }

  public void setScreenRes(ScreenResolution s) {
    this.screenRes = s;
  }

  public void setRenderingMode(RenderingMode rm) {
    this.renderingMode = rm;
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    //        Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
    //        xRes = screenRes.width;
    //        yRes = screenRes.height;

    int id = 0; // This will be changed if main joins a lobby, telemetry will give it new id
    audioController = new AudioController();
    keyController = new KeyController();
    resourceLoader = new ResourceLoader("src/main/resources/");
    this.primaryStage = primaryStage;
//    audioController.playMusic(Sounds.intro);
    MenuController menuController = new MenuController(audioController, primaryStage, this);
    StackPane root = (StackPane) menuController.createMainMenu();
    root.getStylesheets().add(getClass().getResource("/ui/stylesheet.css").toExternalForm());
    Scene scene = new Scene(root, xRes, yRes);
    canvas = new Canvas(xRes, yRes);
    Group gameRoot = new Group();
    gameRoot.getChildren().add(canvas);
    this.gameScene = new Scene(gameRoot);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    renderer = new Renderer(gc, xRes, yRes, resourceLoader);
    primaryStage.setScene(scene);
    primaryStage
        .widthProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              menuController.scaleImages((double) newVal, (double) oldVal);
            });

    primaryStage.show();
    updateResolution(this.screenRes);
  }

  public void startSinglePlayerGame() {

    singlePlayer = true;
    System.out.println("Starting single player game...");
    map = resourceLoader.getMap();

    incomingQueue = new LinkedBlockingQueue<>();
    this.telemetry = new HostTelemetry(map, incomingQueue, resourceLoader);
    this.primaryStage.setScene(gameScene);
    this.id = 0;

    gameScene.setOnKeyPressed(keyController);

    startGame();
  }

  public void createMultiplayerLobby() {
    System.out.println("Created multiplayer lobby");
    isHost = true;

    clientIn = new LinkedList<>();
    // outgoing keypressQueue for local client
    keypressQueue = new LinkedBlockingQueue<>();
    try {
      this.server = new ServerLobby();
      clientLobbySession = new ClientLobbySession(clientIn, keypressQueue, this, name);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void joinMultiplayerLobby() {
    map = resourceLoader.getMap();
    isHost = false;
    BlockingQueue<String> clientIn = new LinkedBlockingQueue<String>();
    keypressQueue = new LinkedBlockingQueue<Input>();
    try {

      clientLobbySession = new ClientLobbySession(clientIn, keypressQueue, this, name);
      this.telemetry = new DumbTelemetry(map, clientIn, resourceLoader);
      this.telemetry.setMipID(MIPID);
      System.out.println("MIP ID: " + MIPID);
      // waits for game to start
      while (!clientLobbySession.isGameStarted()) {
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      this.primaryStage.setScene(gameScene);
      gameScene.setOnKeyPressed(keyController);
      startGame();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void startMultiplayerGame() {

    if (isHost) {
      System.out.println("Starting multiplayer for host");
      BlockingQueue<Input> inputQueue = new LinkedBlockingQueue<Input>();
      BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();
      ServerGameplayHandler s = server.gameStart(inputQueue, outputQueue);

      int playerCount = server.getPlayerCount();
      System.out.println("PLAYER COUNT IS: " + playerCount);
      map = resourceLoader.getMap();
      this.telemetry =
          new HostTelemetry(this.map, playerCount, inputQueue, outputQueue, this.resourceLoader);
      this.telemetry.setMipID(MIPID);
      System.out.println("MIP ID: " + MIPID);
      map = resourceLoader.getMap();
      gameScene.setOnKeyPressed(keyController);
      startGame();
    }
  }

  public void setMap(Map m) {
    this.map = m;
  }

  public void updateResolution(ScreenResolution s) {
    this.screenRes = s;
    switch (s) {
      case LOW:
        primaryStage.setWidth(1366);
        primaryStage.setHeight(768);
        xRes = 1366;
        yRes = 768;
        break;
      case MEDIUM:
        primaryStage.setWidth(1920);
        primaryStage.setHeight(1080);
        xRes = 1920;
        yRes = 1080;
        break;
      case HIGH:
        primaryStage.setWidth(2650);
        primaryStage.setHeight(1440);
        xRes = 2650;
        yRes = 1440;
        break;
    }

    System.out.println(xRes + " " + yRes);
    canvas.setWidth(xRes);
    canvas.setHeight(yRes);
    renderer.setResolution(xRes, yRes, this.renderingMode);
  }

  public void setName(String n) {
    updateResolution(this.screenRes);
    if (n.matches(".*[a-zA-Z]+.*")) {
      this.name = n;
    } else {
      this.name = "Joe Bloggs";
    }
  }

  public void setMIP(int id) {
    this.MIPID = id;
  }

  private void startGame() {
    updateResolution(this.screenRes);
    if (telemetry != null) {
      //            telemetry.setMipID(this.MIPID);
      agents = telemetry.getAgents();
      map = telemetry.getMap();
      pellets = telemetry.getPellets();
    }
    this.telemetry.startGame();
    Methods.updateImages(agents, resourceLoader);
    this.primaryStage.setScene(gameScene);
    // AnimationTimer started once game has started
    new AnimationTimer() {
      @Override
      public void handle(long now) {
        processInput();
        renderer.render(map, agents, now, pellets);
      }
    }.start();
    //        telemetry.startAI();
  }

  /**
   * Process the players input given in via the keyboard @Author Matthew Jones
   */
  private void processInput() {
    if (keyController.UseItem()) {
      // TODO tell the server that the player wants to use their item
    }
    Direction input = keyController.getActiveKey();
    Direction current = agents[id].getDirection();
    if (input == null || input == current) {
      return;
    }
    //    System.out.println(input.toString() + "     " + current + " ID: " + id);
    if (!Methods.validateDirection(input, agents[id].getLocation(), map)) {
      return;
    }
    switch (input) {
      case UP:
        informServer(new Input(this.id, Direction.UP));
        break;
      case DOWN:
        informServer(new Input(this.id, Direction.DOWN));
        break;
      case LEFT:
        informServer(new Input(this.id, Direction.LEFT));
        break;
      case RIGHT:
        informServer(new Input(this.id, Direction.RIGHT));
        break;
    }
  }

  private void informServer(Input input) {
    if (singlePlayer) {
      incomingQueue.add(input);
      System.out.println("Added input to queue");
    } else {
      System.out.println("TRIED TO SEND MY KEY MOVEMENT");
      if (getId() == 0) {
        this.telemetry.addInput(input);
      } else {
        keypressQueue.add(input);
      }
    }
  }

  // communicates to slients
  private void stopMultiplayerGame() {
    if (isHost) {
      telemetry.stopGame();
      server.gameStop();
      //TODO render the end of game screen
    }
    // TODO add options in future for client to quit a game
  }

}
