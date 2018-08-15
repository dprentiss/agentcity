/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class Vehicle implements Steppable, Driveable {

    // Required for serialization
    private static final long serialVersionUID = 1;

    Stoppable stopper;

    // Properties
    public final int length, idNum, passengerCap;

    // Variables
    //// Agents
    public boolean isStopped = true;
    public Driver driver = null;
    public Person manifest[];

    //// Physical
    public Int2D location;
    public AgentCity.Direction direction;
    public int speed = 0;

    // Accessors
    //// Properties
    public int getLength() { return length; }
    public int getIdNum() { return idNum; }
    public int getPassengerCap() { return passengerCap; }
    //// Agents
    public Driver getDriver(Driver newDriver) { return driver; }
    public void setDriver(Driver newDriver) { driver = newDriver; }
    public void removeDriver(Driver oldDriver) { /*TODO*/ }
    public Person[] getManifest() { return manifest; }
    public void addPassenger(Person passenger) { /*TODO*/ }
    public void removePassenger(Person passenger) { /*TODO*/ }
    //// Physical
    public Int2D getLocation() { return location; }
    public AgentCity.Direction getDirection() { return direction; }
    public int getSpeed() { return speed; }

    /** Constructor */
    public Vehicle(int id) {
        this(id, 1, 4, AgentCity.Direction.NONE);
    }

    /** Constructor */
    public Vehicle(int id, AgentCity.Direction dir) {
        this(id, 1, 4, dir);
    }

    /** Constructor */
    public Vehicle(int id, final int len, final int cap, AgentCity.Direction dir) {
        idNum = id;
        length = len;
        passengerCap = cap;
        manifest = new Person[passengerCap];
        direction = dir;
    }

    /** Move Vehicle realistically according to Driver directive*/
    public void move(AgentCity.Direction dir, SimState state) {
        /*TODO*/
        // Check if cell in Direction dir is valid from networkGrid
        //// Check if desired cell is a road, parking, or grid exit cell
        //// Check if desired cell has room for Vehicle
        //// Check if Vehicle has pemission to occupy cell
        // Update location of Vehicle in agentGrid
        // Update location of Driver and Passengers in agentGrid
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;

        // Get location from state of not set
        location = ac.agentGrid.getObjectLocation(this);

        // Check if there is a directive from Driver, execute dir
    }
}
