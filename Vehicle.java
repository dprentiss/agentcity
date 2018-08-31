/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class Vehicle implements Steppable, Driveable {

    // Required by MASON for serialization
    private static final long serialVersionUID = 1;
    
    // Stopper
    Stoppable stopper;

    // Properties
    public final int length, idNum, passengerCap;
    public final int MAX_SPEED = 1;

    // Agents
    private Driver driver;
    private Person manifest[];

    // Physical Variables
    private Int2D location;
    private Direction direction;
    private int speed = 0;

    // Accessors
    // Properties
    public int getLength() { return length; }
    public int getIdNum() { return idNum; }
    public int getPassengerCap() { return passengerCap; }
    // Agents
    public Driver getDriver() { return driver; }
    public void setDriver(Driver newDriver) { driver = newDriver; }
    public void removeDriver() { /*TODO*/ }
    public Person[] getManifest() { return manifest; }
    public void addPassenger(Person passenger) { /*TODO*/ }
    public void removePassenger(Person passenger) { /*TODO*/ }
    // Physical
    public Int2D getLocation(AgentCity ac) { return ac.agentGrid.getObjectLocation(this); }
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

    // classes for changing location, direction, and speed
    private void setLocation(AgentCity ac, int x, int y) {
        if (x >= 0 && x < ac.gridWidth && y >= 0 && y < ac.gridHeight) {
            ac.agentGrid.setObjectLocation(this, x, y);
            location = new Int2D(x, y);
        }
    }

    private void setLocation(AgentCity ac, Int2D loc) {
        setLocation(ac, loc.x, loc.y);
    }
    
    private void setDirection(Direction dir) {
        direction = dir;
    }

    private void setSpeed(int s) {
        speed = s;
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;
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
                    setSpeed(1);
                }
                break;
            case TURN_LEFT:
                if (speed == 0) {
                    setDirection(direction.onLeft());
                    setSpeed(MAX_SPEED);
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

        // Check if there is a directive from Driver, execute dir
    }
}
