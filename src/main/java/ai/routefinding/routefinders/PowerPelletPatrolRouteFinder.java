package com.lordsofmidnight.ai.routefinding.routefinders;

import ai.routefinding.RouteFinder;
import utils.Point;
import utils.enums.Direction;

/**
 * Route finding algorithm that will locate the nearest power pellet and patrol around it, but not collect it.
 *
 * @author Lewis Ackroyd
 */
public class PowerPelletPatrolRouteFinder implements RouteFinder {
    private static final boolean COMPLETE = false;

    @Override
    public Direction getRoute(Point myLocation, Point targetLocation) {
        if (!COMPLETE) {
            return new RandomRouteFinder().getRoute(myLocation, targetLocation);
        }
        return DEFAULT;
    }
}
