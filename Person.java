/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class Person implements Steppable, VehicleClient, Driver {

    // Required for serialization
    private static final long serialVersionUID = 1;

    // Properties
    public final int idNum;

    // Variables
    public Int2D location;
    public Vehicle currentVehicle = null;

    // Accessors
    public Int2D getLocation() { return location; }

    /** Constructor */
    public Person(int id) {
        idNum = id;
    }

    public Driver.Directive getNextDirective() {
        return Driver.Directive.STOP;
    }

    public int getDesiredSpeed() {
        return 0;
    }

    /** Choose trip */
    // A Person is either travelling or stationary at her location. 
    // A Person that is traveling must have at least a current location, a
    // destination, and start time.

    /** Get travel information */
    // A traveler must have enough information to plan a route. 
    // A traveler may request a recommendation from a RouteFindingService.

    /** Choose mode and route */
    // A Person chooses a mode based on her destination, and available travel
    // information.
    // The Person class must implement at least a weighted least-cost
    // optimization. 
    // Route choice is typically chosen with mode. However, route choice is
    // dynamic and can change many times during the trip.
    // Mode choice may also change during the trip
    // The Person class must implement at least one choice at the start of the
    // trip.
    // The Person class may also check for criteria that trigger a new route
    // choice.

    /** Walk */
    // A Person may travels by foot to her destination or to the start of the
    // next leg of the trip.

    /** Request vehicle */
    // A Person may request a Vehicle at her location or at some other waypoint.

    /** Enter vehicle */
    // A Person may choose to enter a Vehicle if she is authorized, both are in
    // the same location, and the Vehicle is stopped.

    /** Exit vehicle */
    // A Person may choose to exit a Vehicle if the vehicle is stopped.

    public void step(final SimState state) {
    }
}
