/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

/**
 * @author David Prentiss
 */
public class Vehicle implements Steppable, Driveable {

    // MASON
    private static final long serialVersionUID = 1;
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
    /** A Bag of Person objects that comprises this Vehicle's passengers. */
    private Bag manifest;

    // Variables
    /** True indicates that this Vehicle is clear to enter the relevant
     * intersection.
     */
    public boolean hasReservation = false;
    public boolean hasPassengers = false;
    /** The next Driver.Directive for the Driver of this Vehicle. */
    private Driver.Directive nextDirective;
    /** The speed desired by this Vehicle's Driver. */
    private int desiredSpeed;
    private int hovMin;

    // Physical Variables
    public Int2D location;
    private Direction direction;
    private int speed;

    // Reporting variables
    public int[] stepsMoving;
    public int[] stepsWaiting;
    public int[] distance;
    public int stepsWithPassenger = 0;
    public int stepsWithoutPassenger = 0;
    public int stepsTravelingWithPassenger = 0;
    public int stepsTravelingWithoutPassenger = 0;
    public int distWithPassenger = 0;
    public int distWithoutPassenger = 0;

    // Accessors

    public boolean meetsHovMin() {
        return getNumPassengers() >= hovMin;
    }

    public boolean boardVehicle(Person person) {
        if (manifest.numObjs < passengerCap & driver.allowTrip(person)) {
            manifest.add(person);
            hasPassengers = true;
            return true;
        }
        return false;
    }

    public boolean exitVehicle(Person person) {
        if(manifest.remove(person)) {
            if (manifest.numObjs < 1) {
                hasPassengers = false;
            }
            return true;
        }
        return false;
    }

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
            .append(", ")
            .append("hasPassengers: " + hasPassengers)
            .append(", ")
            .append("numPassengers: " + getNumPassengers())
            .append("}\n")
            .toString();
    }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver newDriver) { driver = newDriver; }

    //public int getIdNum() { return idNum; }
    //public int getLength() { return length; }
    //public int getPassengerCap() { return passengerCap; }
    //public int getDesiredSpeed() { return driver.getDesiredSpeed(); }
    /*
    public Driver removeDriver() {
        Driver currentDriver = driver;
        driver = null;
        return currentDriver;
    }
    */

    /** Gets an array of Person objects that comprise this Vehicle's current
     * passengers.
     */
    public Bag getManifest() { return manifest; }

    /** Returns the current number of passengers */
    public int getNumPassengers() { return manifest.numObjs; }

    /** Returns the most distant destination this Vehicle's current passengers.
     */
    public Intersection[] getPassengerDestinations() {
        if (hasPassengers) {
            Intersection[] destinations = new Intersection[manifest.numObjs];
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = ((Person)manifest.objs[i]).getDestination();
            }
        return destinations;
        }
        return null;
    }

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
        this(id, 1, AgentCity.PASSENGER_CAP, dir);
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
        manifest = new Bag(passengerCap);
        direction = dir;
        stepsMoving = new int[passengerCap+1];
        stepsWaiting = new int[passengerCap+1];
        distance = new int[passengerCap+1];
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
        hovMin = ac.HOV_MIN;

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
            if (speed < MAX_SPEED && speed < desiredSpeed) {
                setSpeed(speed + 1);
            } else if (speed > desiredSpeed) {
                setSpeed(desiredSpeed);
            }
            if (speed > 0) {
                setLocation(ac,
                            location.x
                            + speed * direction.getXOffset()
                            + direction.onRight().getXOffset(),
                            location.y
                            + speed * direction.getYOffset()
                            + direction.onRight().getYOffset()
                            );
            }
            break;
        case MERGE_LEFT:
            if (speed < MAX_SPEED && speed < desiredSpeed) {
                setSpeed(speed + 1);
            } else if (speed > desiredSpeed) {
                setSpeed(desiredSpeed);
            }
            if (speed > 0) {
                setLocation(ac,
                            location.x
                            + speed * direction.getXOffset()
                            + direction.onLeft().getXOffset(),
                            location.y
                            + speed * direction.getYOffset()
                            + direction.onLeft().getYOffset()
                            );
            }
            break;
        }

        // Do some logging
        if (speed > 0) {
            stepsMoving[manifest.numObjs]++;
            distance[manifest.numObjs]++;
        } else {
            stepsWaiting[manifest.numObjs]++;
        }
        //TODO remove old logging variables
        if (hasPassengers) {
            stepsWithPassenger++;
            if (speed > 0) {
                distWithPassenger += speed;
                stepsTravelingWithPassenger++;
            }
        } else {
            stepsWithoutPassenger++;
            if (speed > 0) {
                distWithoutPassenger += speed;
                stepsTravelingWithoutPassenger++;
            }
        }
    }
}
