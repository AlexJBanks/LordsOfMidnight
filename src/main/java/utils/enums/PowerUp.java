package utils.enums;

import java.util.ArrayList;
import javafx.scene.image.Image;
import objects.Entity;
import utils.Point;

/**
 * An enum for the powerUps that both stores them and implements them
 *
 * @author Matthew Jones
 */
public enum PowerUp {
  WEB(20), SPEED(200), BLUESHELL(20), INVINCIBLE(200);

  private final int EFFECTTIME;
  private Image image;
  private Entity effected;
  private int counter;
  private Point location;

  private Boolean onMap;

  public Boolean getOnMap() {
    return onMap;
  }

  PowerUp(int effectTime) {
    EFFECTTIME = effectTime;
    location = null;
  }

  public Point getLocation() {
    return location;
  }

  /**
   * @return the Image to render them
   */
  public Image getImage() {
    return image;
  }

  /**
   * Called when the player uses this powerUp
   *
   * @param user The entity that used the powerUp
   * @param activePowerUps All active powerUps in the game
   */
  public void use(Entity user, ArrayList<PowerUp> activePowerUps) {
    switch (this) {
      case WEB:
        this.onMap = true;
        location = user.getMoveInDirection(0.5, user.getDirection().getInverse());
        activePowerUps.add(this);
        break;
      case SPEED:
        user.setVelocity(user.getVelocity() * 1.2);
        activePowerUps.add(this);
        this.effected = user;
        counter = 0;
        break;

      case BLUESHELL:

        break;
      case INVINCIBLE:
        activePowerUps.add(this);
        this.effected = user;
        counter = 0;
        break;
    }
  }

  /**
   * Called when the powerUp that is placed or used on another player is triggered
   *
   * @param victim The entity effected by the powerUp
   * @param activePowerUps All active powerUps in the game
   */
  public void trigger(Entity victim, ArrayList<PowerUp> activePowerUps) {
    switch (this) {
      case WEB:
        victim.setVelocity(0);
        activePowerUps.add(this);
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
          effected.resetVelocity();
          break;
        case BLUESHELL:
          effected.resetVelocity();
          break;
      }
      return true;
    }
    return false;
  }
}
