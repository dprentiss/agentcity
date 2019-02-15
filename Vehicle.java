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
    /** The number of Passengers that may occupy this Vehicle.*/
    public final int passengerCap;
    /** The maximum speed in grid cells per step of this Vehicle.
     * Must be one until faster speeds are supported.
     */
    public final int MAX_SPEED = 2;

    // Agents
    /** The agent responsible for driving this Vehicle. */
    private Driver driver;
    /** An array of Person objects that comprises this Vehicle's passengers. */
    private Person manifest[];

    // Variables
    /** True indicates that this Vehicle is clear to enter the relevant
     * intersection.
     */
    public boolean hasReservation = false;
    /** The next Driver.Directive for the Driver of this Vehicle. */
    private Driver.Directive nextDirective;
    /** The speed desired by this Vehicle's Driver. */
    private int desiredSpeed;

    // Physical Variables
    private Int2D location;
    private Direction direction;
    private int speed;

    // Accessors

    /** Returns a string representation of this Vehicle */
    @Override
    public String toString() {
        return new StringBuilder()
                .append("Vehicle: {")
                .append("idNum: " + idNum)
                .append(", ")
                .append("location: " + location)
                .append(", ")
                .append("direction: " + direction)
                .append(", ")
                .append("speed: " + speed)
                .append("}\n")
                .toString();
    }

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

    /** Gets the Driver's desired speed. */
    public int getDesiredSpeed() { return driver.getDesiredSpeed(); }

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
    public Int2D getLocation(AgentCity ac) {
        return ac.agentGrid.getObjectLocation(this);
    }

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

    /** Creates a Vehicle object with the given ID number, length, size,
     * passenger capacity and initial direction.
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

    /** Set the location of this Vehicle on the grid in provided state at the
     * provided x and y coordinates.
     */
    private void setLocation(AgentCity ac, int x, int y) {
        // Check if new location x, y is on the grid
        if (ac.checkBounds(x, y)) {
            // Set this Vehicle at the new location
            ac.agentGrid.setObjectLocation(this, x, y);
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

        // Get location from state
        location = ac.agentGrid.getObjectLocation(this);

        // Get next directive from Driver
        Driver.Directive nextDirective = driver.getNextDirective();
        // Get desired Speed from Driver
        int desiredSpeed = driver.getDesiredSpeed();

        // Execute Directive from Driver
        switch (nextDirective) {
        case MOVE_FORWARD:
            if (speed < MAX_SPEED && speed < desiredSpeed) {
                setSpeed(speed + 1);
            } else if (speed > desiredSpeed) {
                setSpeed(desiredSpeed);
            }
            if (speed > 0) {
                setLocation(ac,
                            location.x + speed * direction.getXOffset(),
                            location.y + speed * direction.getYOffset()
                            );
            }
            break;
        case STOP:
            if (desiredSpeed > 0) {
                setLocation(ac,
                            location.x + desiredSpeed * direction.getXOffset(),
                            location.y + desiredSpeed * direction.getYOffset()
                            );
            }
            setSpeed(0);
            break;
        case TURN_RIGHT:
            if (desiredSpeed > 0) {
                setLocation(ac,
                            location.x + desiredSpeed * direction.getXOffset(),
                            location.y + desiredSpeed * direction.getYOffset()
                            );
            }
            setDirection(direction.onRight());
            setSpeed(0);
            break;
        case TURN_LEFT:
            if (desiredSpeed > 0) {
                setLocation(ac,
                            location.x + desiredSpeed * direction.getXOffset(),
                            location.y + desiredSpeed * direction.getYOffset()
                            );
            }
            setDirection(direction.onLeft());
            setSpeed(0);
            break;
        case MERGE_RIGHT:
            setLocation(ac,
                        location.x
                        + desiredSpeed * direction.getXOffset()
                        + direction.onRight().getXOffset(),
                        location.y
                        + desiredSpeed * direction.getYOffset()
                        + direction.onRight().getYOffset()
                        );
            break;
        case MERGE_LEFT:
            setLocation(ac,
                        location.x
                        + desiredSpeed * direction.getXOffset()
                        + direction.onLeft().getXOffset(),
                        location.y
                        + desiredSpeed * direction.getYOffset()
                        + direction.onLeft().getYOffset()
                        );
            break;
        }
    }
}
