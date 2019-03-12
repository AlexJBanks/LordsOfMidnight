package utils.enums;

import java.util.HashMap;
import java.util.UUID;
import objects.Entity;
import objects.Pellet;
import objects.PowerUpBox;
import utils.Methods;
import utils.Point;

/**
 * An enum for the powerUps that both stores them and implements them
 *
 * @author Matthew Jones
 */
public enum PowerUp {
  WEB(100, "web"), SPEED(300, "speed"), BLUESHELL(20, "blueshell"), INVINCIBLE(200, "invincible");

  private final String NAME;
  private final int EFFECTTIME;
  private Entity effected;
  private int counter = 500;
  private Boolean onMap = false;
  private Entity user;
  private int currentFrame = 0;
  public UUID id;

  public Boolean getOnMap() {
    return onMap;
  }

  PowerUp(int effectTime, String name) {
    this.EFFECTTIME = effectTime;
    this.NAME = name;
    id = UUID.randomUUID();
  }

  /**
   * Used to communicate powerups to clients
   *
   * @return the PowerUp corresponding to the int provided
   */
  public static PowerUp fromInt(int n) {
    switch (n) {
      case 0:
        return WEB;
      case 1:
        return SPEED;
      case 2:
        return BLUESHELL;
      case 3:
        return INVINCIBLE;
    }
    return null;
  }

  /**
   * Called when the powerUp that is placed or used on another player is triggered
   *
   * @param victim The entity effected by the powerUp
   * @param activePowerUps All active powerUps in the game
   */
  public void trigger(Entity victim, HashMap<UUID, PowerUp> activePowerUps) {
    switch (this) {
      case WEB:
        victim.setStunned(true);
        activePowerUps.put(id, this);
        this.effected = victim;
        counter = 0;
        break;
      case BLUESHELL:

        break;
    }
  }

  /**
   * Called each physics update to increment the timers
   *
   * @return if the powerUp has finished and should be removed
   */
  public boolean incrementTime() {
    counter++;
    if (counter == EFFECTTIME) {
      switch (this) {
        case WEB:
          effected.setStunned(false);
          break;
        case BLUESHELL:
          effected.setDead(false);
          break;
        case SPEED:
          effected.changeBonusSpeed(-0.03);
          break;
        case INVINCIBLE:
          effected.setInvincible(false);
          break;
      }
      return true;
    }
    return false;
  }

  /**
   * Called when the player uses this powerUp
   *
   * @param user The entity that used the powerUp
   * @param activePowerUps All active powerUps in the game
   */
  public void use(Entity user, HashMap<UUID, PowerUp> activePowerUps,
      HashMap<String, Pellet> pellets,
      Entity[] agents) {
    this.user = user;
    switch (this) {
      case WEB:
        this.onMap = true;
        Point loc = user.getMoveInDirection(1.1, user.getFacing().getInverse());
        int x = (int) loc.getX();
        int y = (int) loc.getY();
        PowerUpBox box = new PowerUpBox(x + 0.5, y + 0.5);
        box.setTrap(this);
        pellets.put(x + "," + y, box);
        break;
      case SPEED:
        user.changeBonusSpeed(0.03);
        activePowerUps.put(id, this);
        this.effected = user;
        counter = 0;
        break;

      case BLUESHELL:
        effected = agents[Methods.findWinner(agents)];
        this.user = user;

        break;
      case INVINCIBLE:
        activePowerUps.put(id, this);
        this.effected = user;
        user.setInvincible(true);
        counter = 0;
        break;
    }
  }

  /**
   * Used to communicate powerups to clients
   *
   * @return the int corresponding to the powerup's enum
   */
  public int toInt() {
    // TODO Auto-generated method stub
    switch (this) {
      case WEB:
        return 0;
      case SPEED:
        return 1;
      case BLUESHELL:
        return 2;
      case INVINCIBLE:
        return 3;
    }
    return -1;
  }

  public Entity getUser() {
    return this.user;
  }

  @Override
  public String toString() {
    return this.NAME;
  }

  public void incrementFrame() {
    this.currentFrame++;
  }

  public int getCurrentFrame() {
    return this.currentFrame;
  }
}




