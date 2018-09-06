/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

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

    /** Constructor */
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
                        hasRightOfWay = v.getSpeed() == 0 && v.idNum < vehicle.idNum;
                        if (!isFree && !hasRightOfWay) {
                            System.out.printf("Vehicle %d stopped because Vehicle %d is on the right.\n",
                                    vehicle.idNum, v.idNum);
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
                            System.out.printf("Vehicle %d stopped because Vehicle %d is on the left.\n",
                                    vehicle.idNum, v.idNum);
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
                            System.out.printf("Vehicle %d stopped at (%d, %d) because Vehicle %d is on the right.\n",
                                    vehicle.idNum, loc.x, loc.y, v.idNum);
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
                            System.out.printf("Vehicle %d stopped at (%d, %d) because Vehicle %d is on the left.\n",
                                    vehicle.idNum, loc.x, loc.y, v.idNum);
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
                            System.out.printf("Vehicle %d stopped at (%d, %d) because Vehicle %d is on the right.\n",
                                    vehicle.idNum, loc.x, loc.y, v.idNum);
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
                            System.out.printf("Vehicle %d stopped at (%d, %d) because Vehicle %d is on the left.\n",
                                    vehicle.idNum, loc.x, loc.y, v.idNum);
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

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
        // Current Vehicle position and velocity; 
        Int2D location = vehicle.getLocation(ac);
        Direction direction = vehicle.getDirection();
        int speed = vehicle.getSpeed();

        // if at destination get a destination from Client
        // random road location for testing
        if (location == destination || destination == null) {
            destination = new Int2D(ac.random.nextInt(ac.gridWidth), ac.random.nextInt(ac.gridHeight));
            while (ac.roadGrid.get(destination.x, destination.y) == 0
                    || ac.roadGrid.get(destination.x, destination.y) == 9) {
                destination = new Int2D(ac.random.nextInt(ac.gridWidth), ac.random.nextInt(ac.gridHeight));
            }
        }

        // get path to destination from RouteFindingService

        // calculate step to next waypoint in path
        // check next step for hazards and set nextDirective
        if (pathAheadClear(ac, location, direction, speed)) {
            nextDirective = Driver.Directive.MOVE_FORWARD;
        } else {
            nextDirective = Driver.Directive.STOP;
        }
    }
}
