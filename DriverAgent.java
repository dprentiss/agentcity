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

    // Accessors
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle v) { vehicle = v; }
    public Driver.Directive getNextDirective() { return nextDirective; }

    /** Constructor */
    public DriverAgent(int id) {
        idNum = id;
    }

    boolean pathAheadClear(AgentCity ac, Int2D loc, Direction dir) {
        int x = loc.x + 2 * dir.getXOffset();
        int y = loc.y + 2 * dir.getYOffset();
        boolean isRoad, isFree;
        if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
            isRoad = ac.roadGrid.get(x, y) != 0;
            Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
            if (b != null) {
                Vehicle V = (Vehicle)b.get(0);
                isFree = V.getSpeed() != 0;
                System.out.printf("Vehicle %d stopped because Vehicle %d is in the way.\n",
                        vehicle.idNum, V.idNum);
            } else {
                isFree = true;
            }
        } else {
            System.out.printf("Vehicle %d stopped because %d, %d is out of bounds.\n",
                    vehicle.idNum, x, y);
            return false;
        }
        if (isRoad && isFree) {
            return true;
        } else {
            return false;
        }
    }

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
        // Current Vehicle position and velocity; 
        Int2D location = vehicle.getLocation(ac);
        Direction direction = vehicle.getDirection();
        int speed = vehicle.getSpeed();

        // get a destination from Client
        // get path to destination from RouteFindingService
        // calculate step to next waypoint in path
        // check next step for hazards and set nextDirective
        if (pathAheadClear(ac, location, direction)) {
            nextDirective = Driver.Directive.MOVE_FORWARD;
        } else {
            nextDirective = Driver.Directive.STOP;
            stopper.stop();
        }
    }
}
