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
    protected final int length, idNum, passengerCap;

    // Variables
    //// Agents
    protected Driver driver;
    protected Person manifest[];

    //// Physical
    protected Int2D location;
    protected Direction direction;
    protected int speed = 0;

    // Accessors
    //// Properties
    public int getLength() { return length; }
    public int getIdNum() { return idNum; }
    public int getPassengerCap() { return passengerCap; }
    //// Agents
    public Driver getDriver() { return driver; }
    public void setDriver(Driver newDriver) { driver = newDriver; }
    public void removeDriver() { /*TODO*/ }
    public Person[] getManifest() { return manifest; }
    public void addPassenger(Person passenger) { /*TODO*/ }
    public void removePassenger(Person passenger) { /*TODO*/ }
    //// Physical
    public Int2D getLocation() { return location; }
    public Direction getDirection() { return direction; }
    public int getSpeed() { return speed; }

    /** Constructor */
    public Vehicle(int id) {
        this(id, 1, 4, Direction.NONE);
    }

    /** Constructor */
    public Vehicle(int id, Direction dir) {
        this(id, 1, 4, dir);
    }

    /** Constructor */
    public Vehicle(int id, final int len, final int cap, Direction dir) {
        idNum = id;
        length = len;
        passengerCap = cap;
        manifest = new Person[passengerCap];
        direction = dir;
    }

    private void moveStraightOneCell(AgentCity ac) {
        /*TODO*/
        // Check if Vehicle can move as desired
        Int2D nextLocation = new Int2D(location.x + direction.getXOffset(),
                location.y + direction.getYOffset());
        // Check if cell is in the grid
        if ((nextLocation.x >= 0) && (nextLocation.x < ac.gridWidth)
                && (nextLocation.y >= 0) && (nextLocation.y < ac.gridHeight)) {
            // Check if desired cell is a road, parking, or grid exit cell
            boolean isRoad = ac.roadGrid.get(nextLocation.x, nextLocation.y) != 0;
            //// Check for conflict with other vehicles
            //// Check if Vehicle has pemission to occupy cell
            // Update location of Vehicle in agentGrid
            if (isRoad) {
                setLocation(ac, nextLocation);
            } else {
                if (stopper != null) stopper.stop();
                return;
            }
        }
        // Update location of Driver and Passengers in agentGrid
    }

    private void setLocation(AgentCity ac, int x, int y) {
        if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
            ac.agentGrid.setObjectLocation(this, x, y);
        }
    }

    private void setLocation(AgentCity ac, Int2D loc) {
        setLocation(ac, loc.x, loc.y);
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;

        // Get location from state
        location = ac.agentGrid.getObjectLocation(this);
        moveStraightOneCell(ac);

        // Check if there is a directive from Driver, execute dir
    }
}
