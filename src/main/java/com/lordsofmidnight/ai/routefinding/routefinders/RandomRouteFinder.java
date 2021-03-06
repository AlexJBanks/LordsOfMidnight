package com.lordsofmidnight.ai.routefinding.routefinders;

import com.lordsofmidnight.ai.routefinding.RouteFinder;
import com.lordsofmidnight.gamestate.points.Point;
import com.lordsofmidnight.utils.enums.Direction;
import java.util.Random;

/**
 * Random direction generator. Twice as likely to produce a route towards the target as one away
 * from it.
 *
 * @author Lewis Ackroyd
 */
public class RandomRouteFinder implements RouteFinder {

  private static final Random R = new Random();

  /**
   * Creates an instance of this {@link RouteFinder}.
   *
   * @author Lewis Ackroyd
   */
  public RandomRouteFinder() {
  }

  /**
   * Returns random direction to travel in until the next junction is reached.
   *
   * @param myLocation The start point.
   * @param targetLocation The target point.
   * @return The direction to travel in, or DEFAULT if no direction could be produced.
   * @author Lewis Ackroyd
   */
  @Override
  public Direction getRoute(Point myLocation, Point targetLocation) {
    if (myLocation == null || targetLocation == null) {
      return DEFAULT;
    }
    Direction dir = DEFAULT;
    int dirValue = R.nextInt(6);
    switch (dirValue) {
      case 0: {
        dir = Direction.UP;
        break;
      }
      case 1: {
        dir = Direction.DOWN;
        break;
      }
      case 2: {
        dir = Direction.LEFT;
        break;
      }
      case 3: {
        dir = Direction.RIGHT;
        break;
      }
      case 4: {
        // makes ghoul twice as likely to move towards mipsman as away from them
        if (myLocation.getY() > targetLocation.getY()) {
          dir = Direction.UP;
        } else {
          dir = Direction.DOWN;
        }
        break;
      }
      case 5: {
        if (myLocation.getX() > targetLocation.getX()) {
          dir = Direction.LEFT;
        } else {
          dir = Direction.RIGHT;
        }
        break;
      }
    }
    return dir;
  }
}
