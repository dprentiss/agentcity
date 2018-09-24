/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

/** 
 * @author David Prentiss
 */
public class Vehicle implements Steppable, Driveable {

    /**
     * Required for serialization
     */
    private static final long serialVersionUID = 1;
    
    // Stopper
    Stoppable stopper;

    // Properties
    /** A label for identifying this Vehicle.
     * This number should be unique but this is not enforced.
     */
    public final int idNum;
    /** The length in grid cells of this Vehicle.
     * Length must be one until larger vehicles are supported.
     */
    public final int length;
    /** The of Passengers that may occupy this Vehicle.*/
    public final int passengerCap;
    /** The maximum speed in grid cells per step of this Vehicle.
     * Must be one until faster speeds are supported.
     */
    public final int MAX_SPEED = 1;

    // Agents
    /** The agent responsible for driving this Vehicle. */
    private Driver driver;
    /** An array of Person objects that comprises this Vehicle's passengers. */
    private Person manifest[];

    // Physical Variables
    private Int2D location;
    private Direction direction;
    private int speed;

    // Accessors

    /** Gets the ID number of this Vehicle. */
    public int getIdNum() { return idNum; }

    /** Gets the length of the Vehicle. */
    public int getLength() { return length; }

    /** Gets the ID number of this Vehicle */
    public int getPassengerCap() { return passengerCap; }

    /** Gets the Driver object of this Vehicle.
     * Returns null if there is no driver
     */
    public Driver getDriver() { return driver; }

    /** Sets the Driver object provided as the driver of this Vehicle. */
    public void setDriver(Driver newDriver) { driver = newDriver; }

    /** Removes the current Driver object as the driver of this Vehicle and
     * returns the Driver object.
     */
    public Driver removeDriver() {
        Driver currentDriver = driver;
        driver = null;
        return currentDriver;
    }

    /** Gets an array of Person objects that comprise this Vehicle's current
     * passengers.
     */
    public Person[] getManifest() { return manifest; }

    /** Adds the provide Person object as a passenger of this Vehicle if there
     * is room.
     */
    public void addPassenger(Person passenger) { /*TODO*/ }

    /** Removes the provided Person object as a passenger of this Vehicle.
     *
     * @param passenger the Person object to be added this Vehicle.
     */
    public void removePassenger(Person passenger) { /*TODO*/ }

    // Physical
 
    /** Gets the current grid location of this Vehicle.
     * 
     * @param ac the AgentCity state instance containg this Vehicle.
     */
    public Int2D getLocation(AgentCity ac) { return ac.agentGrid.getObjectLocation(this); }

    /** Gets the last known grid location of this Vehicle.*/
    public Int2D getLocation() { return location; }


    /** Gets the current direction of this Vehicle. */
    public Direction getDirection() { return direction; }

    /** Gets the current speed of this Vehicle. */
    public int getSpeed() { return speed; }

    /** Creates a Vehicle object with the given ID number.
     * The ID number should be unique but this is not enforced.
     * The created vehicle will have the default size of one, passenger
     * capacity of four, and Direction of NONE.
     */
    public Vehicle(int id) {
        this(id, 1, 4, Direction.NONE);
    }

    /** Creates a Vehicle object with the given ID number and initial direction.
     * The ID number should be unique but this is not enforced.
     * The created vehicle will have the default size of one and passenger
     * capacity of four.
     */
    public Vehicle(int id, Direction dir) {
        this(id, 1, 4, dir);
    }

    /** Creates a Vehicle object with the given ID number, passenger capacity
     * and initial direction.
     * The ID number should be unique but this is not enforced.
     * The created vehicle will have the default size of one.
     */
    public Vehicle(int id, final int cap, Direction dir) {
        this(id, 1, cap, dir);
    }

    /** Creates a Vehicle object with the given ID number, length, size, passenger
     * capacity and initial direction.
     * The ID number should be unique but this is not enforced.
     * Length must be one until larger vehicles are supported.
     */
    private Vehicle(int id, final int len, final int cap, Direction dir) {
        idNum = id;
        length = len;
        passengerCap = cap;
        manifest = new Person[passengerCap];
        direction = dir;
    }

    /* 
    private void moveStraightOneCell(AgentCity ac) {
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
    */

    /** Set the location of this Vehicle on the grid in provided state at the
     * provided x and y coordinates.
     */
    private void setLocation(AgentCity ac, int x, int y) {
        // Check if new location x, y is on the grid
        if (ac.checkBounds(x, y)) {
            // Set this Vehicle at the new location
            ac.agentGrid.setObjectLocation(this, x, y);
            // Check if there has been a collsion
            /*
            if (ac.agentGrid.numObjectsAtLocation(x, y) > 1) {
                System.out.printf("Collision at %d, %d.\n", x, y);
            }
            */
            // Update this Vehicle's location variable
            location = new Int2D(x, y);
        }
    }

    /** Set the location of this Vehicle on the grid in provided state at the
     * provided (Int2D) location.
     */
    private void setLocation(AgentCity ac, Int2D loc) {
        setLocation(ac, loc.x, loc.y);
    }
    
    /** Set the direction of this Vehicle. */
    private void setDirection(Direction dir) {
        direction = dir;
    }

    /** Set the speed of this Vehicle. */
    private void setSpeed(int s) {
        speed = s;
    }

    /** Actions this vehicle should take on each step.
     *
     * <p> On each step, this Vehicle should update is location and get the next
     * Directive from its Driver agent. Afterward, it should exectute that
     * Directive.
     * 
     * @param state The (AgentCity) SimState object
     */
    public void step(final SimState state) {
        // The current simulation state
        AgentCity ac = (AgentCity)state;

        // temporary x, y location variables for executing the MERGE_RIGHT and
        // MERGE_LEFT Directives
        int xOffset = 0;
        int yOffset = 0;

        // Get location from state
        location = ac.agentGrid.getObjectLocation(this);

        // Get next directive from Driver
        Driver.Directive nextDirective = driver.getNextDirective();

        // Execute Directive from Driver
        switch (nextDirective) {
            case MOVE_FORWARD:
                if (speed == 0) {
                    setSpeed(MAX_SPEED);
                } else {
                    Int2D nextLocation = new Int2D(location.x + direction.getXOffset(),
                            location.y + direction.getYOffset());
                    setLocation(ac, nextLocation);
                }
                break;
            case STOP:
                if (speed > 0) {
                    Int2D nextLocation = new Int2D(location.x + direction.getXOffset(),
                            location.y + direction.getYOffset());
                    setLocation(ac, nextLocation);
                    setSpeed(0);
                }
                break;
            case TURN_RIGHT:
                if (speed == 0) {
                    setDirection(direction.onRight());
                } else {
                    setLocation(ac, location.x + direction.getXOffset(),
                            location.y + direction.getYOffset());
                    setSpeed(0);
                    setDirection(direction.onRight());
                }
                break;
            case TURN_LEFT:
                if (speed == 0) {
                    setDirection(direction.onLeft());
                } else {
                    setLocation(ac, location.x + direction.getXOffset(),
                            location.y + direction.getYOffset());
                    setSpeed(0);
                    setDirection(direction.onLeft());
                }
                break;
            case MERGE_RIGHT:
                xOffset = location.x + direction.getXOffset() + direction.onRight().getXOffset();
                yOffset = location.y + direction.getYOffset() + direction.onRight().getYOffset();
                setLocation(ac, xOffset, yOffset);
                break;
            case MERGE_LEFT:
                xOffset = location.x + direction.getXOffset() + direction.onLeft().getXOffset();
                yOffset = location.y + direction.getYOffset() + direction.onLeft().getYOffset();
                setLocation(ac, xOffset, yOffset);
                break;
        }
    }
}
