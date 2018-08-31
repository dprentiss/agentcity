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
    private Vehicle currentVehicle = null;
    private Driver.Directive nextDirective = Driver.Directive.NONE;

    // Accessors
    public Vehicle getCurrentVehicle() { return currentVehicle; }
    public Driver.Directive getNextDirective() { return nextDirective; }

    /** Constructor */
    public DriverAgent(int id) {
        idNum = id;
    }

    public void step(final SimState state) {
        // get a destination from Client
        // get path to destination from RouteFindingService
        // calculate steps to next waypoint in path
        // check next step for hazards
        // set nextDirective
    nextDirective = Driver.Directive.MOVE_FORWARD;
    }
}
