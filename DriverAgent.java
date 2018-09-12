/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

/**
 * Generic agent for controlling {@link Vehicles}
 */

public class DriverAgent implements Steppable, Driver {

    // Required for serialization
    private static final long serialVersionUID = 1;
    Stoppable stopper;

    // Properties
    public final int idNum;

    // Variables
    private Vehicle vehicle = null;
    private Driver.Directive nextDirective = Driver.Directive.NONE;
    private Int2D destination = null;

    // Accessors
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle v) { vehicle = v; }
    public Driver.Directive getNextDirective() { return nextDirective; }

    /** Constructor 
     *
     * @param id (required) int label for this class. Should be unique but 
     * uniqueness is not checked.
     */
    public DriverAgent(int id) {
        idNum = id;
    }

    boolean pathAheadClear(AgentCity ac, Int2D loc, Direction dir, int speed) {
        int x;
        int y;
        boolean isRoad = true;
        boolean isFree = true;
        boolean hasRightOfWay = true;

        switch (speed) {
            case 0:
                // one cell ahead
                x = loc.x + dir.getXOffset();
                y = loc.y + dir.getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    isRoad = ac.roadGrid.get(x, y) != 0;
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getSpeed() != 0;
                    }
                    if (!isRoad || !isFree) { return false; }
                } else { return false; }
                // two cells ahead
                x = loc.x + 2 * dir.getXOffset();
                y = loc.y + 2 * dir.getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.opposite();
                    }
                    if (!isFree) { return false; }
                }
                //  one cell ahead and one cell right
                x = loc.x + dir.getXOffset() + dir.onRight().getXOffset();
                y = loc.y + dir.getYOffset() + dir.onRight().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onLeft()
                            /*|| v.getSpeed() == 0*/;
                        hasRightOfWay = v.getSpeed() == 0 
                            && v.idNum < vehicle.idNum;
                        if (!isFree && !hasRightOfWay) {
                            return false;
                        }
                    }
                }
                //  one cell ahead and one cell left
                x = loc.x + dir.getXOffset() + dir.onLeft().getXOffset();
                y = loc.y + dir.getYOffset() + dir.onLeft().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onRight() 
                            /*|| v.getSpeed() == 0*/;
                        hasRightOfWay = v.getSpeed() == 0 && v.idNum < vehicle.idNum;
                        if (!isFree && !hasRightOfWay) {
                            return false;
                        }
                    }
                }
                break;
            case 1:
                // one cell ahead
                x = loc.x + dir.getXOffset();
                y = loc.y + dir.getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir;
                    }
                    if (!isFree) { return false; }
                }
                // two cells ahead
                x = loc.x + 2 * dir.getXOffset();
                y = loc.y + 2 * dir.getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    isRoad = ac.roadGrid.get(x, y) != 0;
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getSpeed() != 0;
                    }
                    if (!isRoad || !isFree) { return false; }
                } else { return false; }
                // three cells ahead
                x = loc.x + 3 * dir.getXOffset();
                y = loc.y + 3 * dir.getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.opposite();
                    }
                    if (!isFree) { return false; }
                }
                //  two cells ahead and one cell right
                x = loc.x + 2 * dir.getXOffset() + dir.onRight().getXOffset();
                y = loc.y + 2 * dir.getYOffset() + dir.onRight().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onLeft()
                            /*|| v.getSpeed() == 0*/;
                        if (!isFree) {
                            return false;
                        }
                    }
                }
                //  two cells ahead and one cell left
                x = loc.x + 2 * dir.getXOffset() + dir.onLeft().getXOffset();
                y = loc.y + 2 * dir.getYOffset() + dir.onLeft().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onRight() 
                            /*|| v.getSpeed() == 0*/;
                        if (!isFree) {
                            return false;
                        }
                    }
                }
                //  two cells ahead and two cells right
                x = loc.x + 2 * dir.getXOffset() + 2 * dir.onRight().getXOffset();
                y = loc.y + 2 * dir.getYOffset() + 2 * dir.onRight().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onLeft()
                            || v.getSpeed() == 0;
                        if (!isFree) {
                            return false;
                        }
                    }
                }
                //  two cells ahead and two cells left
                x = loc.x + 2 * dir.getXOffset() + 2 * dir.onLeft().getXOffset();
                y = loc.y + 2 * dir.getYOffset() + 2 * dir.onLeft().getYOffset();
                if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
                    Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                    if (b != null) {
                        Vehicle v = (Vehicle)b.objs[0];
                        isFree = v.getDirection() != dir.onRight() 
                            || v.getSpeed() == 0;
                        if (!isFree) {
                            return false;
                        }
                    }
                }
                break;
        }
        return true;
        //System.out.printf("Vehicle %d stopped because Vehicle %d is in the way.\n",
        //vehicle.idNum, V.idNum);
        //System.out.printf("Vehicle %d stopped because %d, %d is out of bounds.\n",
        //vehicle.idNum, x, y);
    }

    /*
    Intersection getLastIntersection(AgentCity ac, Int2D dest) {
        int cellX = dest.x;
        int cellY = dest.y;
        Direction cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        while (cellDirection != Direction.ALL) {
            cellX += cellDirection.opposite().getXOffset();
            cellY += cellDirection.opposite().getYOffset();
            cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        }
        return ac.intersections[ac.intersectionGrid.get(cellX, cellY) - 1];
    }
    */
    
    /**
     * Gets the intersection ahead of the {@link Vehicle}.
     *
     * @param ac the current state of the simulation.
     * @param loc currrent location of the {@link Vehicle}.
     *
     * @return the intersection ahead of the {@link Vehicle}.
     */
    Intersection getIntersectionAhead(AgentCity ac, Int2D loc) {
        int cellX = loc.x;
        int cellY = loc.y;
        Direction cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        while (cellDirection != Direction.ALL) {
            cellX += cellDirection.getXOffset();
            cellY += cellDirection.getYOffset();
            cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        }
        return ac.intersections[ac.intersectionGrid.get(cellX, cellY)];
    }

    Int2D getRandomDepartureLeg(AgentCity ac, Intersection in, Direction dir) {
        /*
        System.out.println();
        System.out.println(vehicle.idNum);
        System.out.println(vehicle.getLocation(ac));
        System.out.println(in.idNum);
        System.out.println(Arrays.toString(in.getDepartureLegs()));
        */
        Int2D[] departureLegs = in.getDepartureLegs(ac, dir);
        //System.out.println(Arrays.toString(departureLegs));
        return departureLegs[ac.random.nextInt(departureLegs.length)];
    }

    Int2D setTurnCell(AgentCity ac, Int2D leg, Int2D loc, Direction locDir) {
        int cellX;
        int cellY;
        Direction legDir = Direction.byInt(ac.roadGrid.field[leg.x][leg.y]);
        if (legDir == locDir) {
            cellX = Math.abs(locDir.getXOffset()) * leg.x 
                + Math.abs(legDir.getYOffset()) * loc.x;
            cellY = Math.abs(locDir.getYOffset()) * leg.y
                + Math.abs(legDir.getXOffset()) * loc.y;
        } else {
            cellX = Math.abs(locDir.getXOffset()) * leg.x 
                + Math.abs(legDir.getXOffset()) * loc.x;
            cellY = Math.abs(locDir.getYOffset()) * leg.y
                + Math.abs(legDir.getYOffset()) * loc.y;
        }
        /*
        System.out.println();
        System.out.println(leg);
        System.out.println(legDir);
        System.out.println(loc);
        System.out.println(locDir);
        System.out.println(new Int2D(cellX, cellY));
        */
        return new Int2D(cellX, cellY);
    }

    public void step(final SimState state)
    {
        // World state
        AgentCity ac = (AgentCity)state;
        // Current Vehicle position and velocity; 
        Int2D location = vehicle.getLocation(ac);
        Direction direction = vehicle.getDirection();
        int speed = vehicle.getSpeed();

        /*
        // if at destination get a destination from Client
        // random road location for testing
        if (location == destination || destination == null) {
            destination = new Int2D(ac.random.nextInt(ac.gridWidth),
                    ac.random.nextInt(ac.gridHeight));
            while (ac.roadGrid.get(destination.x, destination.y) == 0
                    || ac.roadGrid.get(destination.x, destination.y) == 9) {
                destination = new Int2D(ac.random.nextInt(ac.gridWidth),
                        ac.random.nextInt(ac.gridHeight));
            }
        // Get intersection before destination
        Intersection tmp = getLastIntersection(ac, destination);
        //System.out.printf("Vehicle %d is headed to intersection %d.\n", vehicle.idNum, tmp);
        }
        */
        
        Intersection nextIntersection = getIntersectionAhead(ac, location);
        // Get random turn Direction for next intersection
        Int2D nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
        Int2D nextTurnCell = setTurnCell(ac, nextLeg, location, direction);
        Direction nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
        /*
        System.out.println(vehicle.idNum);
        System.out.println(vehicle.getLocation(ac));
        System.out.println(nextIntersection.idNum);
        System.out.println(nextWaypoint);
        System.out.println(Direction.byInt(ac.roadGrid.get(nextWaypoint.x, nextWaypoint.y)));
        */

        boolean nearTurnCell = location.x + direction.getXOffset() == nextTurnCell.x
            && location.y + direction.getYOffset() == nextTurnCell.y;
        // check next step for hazards and set nextDirective
        if (pathAheadClear(ac, location, direction, speed)) {
            if (nearTurnCell) {
                if (nextDirection == direction.onRight()) {
                    nextDirective = Driver.Directive.TURN_RIGHT;
                } else if (nextDirection == direction.onLeft()) {
                    nextDirective = Driver.Directive.TURN_LEFT;
                } else {
                    System.out.printf("Vehicle %d at (%d, %d) had a problem turning.\n", vehicle.idNum, location.x, location.y);
                    nextDirective = Driver.Directive.STOP;
                }
            } else {
                nextDirective = Driver.Directive.MOVE_FORWARD;
            }
        } else {
            nextDirective = Driver.Directive.STOP;
        }
    }
}
