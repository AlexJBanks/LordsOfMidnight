package ai.routefinding.routefinders;

import ai.mapping.Mapping;
import ai.mapping.PointSet;
import ai.routefinding.RouteFinder;
import objects.Entity;
import utils.Map;
import utils.Point;
import utils.enums.Direction;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Route finding algorithm that will aim to reach the junction that will next be reached by Mipsman.
 *
 * @author Lewis Ackroyd
 */
public class NextJunctionRouteFinder implements RouteFinder {

    private final Entity[] allAgents;
    private final Map map;
    private final PointSet junctions;
    private final HashMap<Point, HashSet<Point>> edges;

    public NextJunctionRouteFinder(Entity[] allAgents, Map map, PointSet junctions, HashMap<Point, HashSet<Point>> edges) {
        this.allAgents = allAgents;
        this.map = map;
        this.junctions = junctions;
        this.edges = edges;
    }

    /**
     * */
    @Override
    public Direction getRoute(Point myLocation, Point targetLocation) {
        Point mipsmanLocation = null;
        Direction mipsmanDirection = null;
        for (Entity entity: allAgents) {
            if (entity.isMipsman()) {
                mipsmanLocation = entity.getLocation().getCopy();
                mipsmanDirection = entity.getDirection();
                break;
            }
        }
        if (mipsmanLocation==null || mipsmanDirection==null) {
            return null;
        }
        targetLocation = Mapping.findNextJunction(mipsmanLocation, mipsmanDirection, map, junctions);
        return new AStarRouteFinder(junctions, edges, map).getRoute(myLocation, targetLocation);
    }
}
