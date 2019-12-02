/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.util.distribution.*;
import sim.engine.*;
import ec.util.MersenneTwisterFast;
import java.util.Arrays;

/**
 * Generates Vehicle objects at a cell with a Poisson distribution.
 */

public class VehicleGenerator implements Steppable {

    // MASON
    private static final long serialVersionUID = 1;
    Stoppable stopper;
    long step;

    // Properties
    public final int idNum;
    public final Int2D cell;
    public final Direction direction;
    public final int speed;
    public final double rampRate;

    // Variables
    private final double mean;

    // Accessors

    /*
    @Override
    public String toString() {
        return new StringBuilder()
            .append("DriverAgent: {")
            .append("idNum: " + idNum)
            .append(", ")
            .append("desiredSpeed: " + desiredSpeed)
            .append(", ")
            //.append("destination: " + destination)
            //.append(", ")
            .append("hasReservation: " + hasReservation)
            .append(", ")
            .append("nextApproachLeg: " + nextApproachLeg)
            .append(", ")
            .append("nextTurnCell: " + nextTurnCell)
            .append(", ")
            .append("nextLeg: " + nextLeg)
            .append(", ")
            .append("nextDirection: " + nextDirection)
            .append(", ")
            //.append("nearIntersection: " + nearIntersection)
            //.append(", ")
            .append("nearApproachLeg: " + nearApproachLeg)
            .append(", ")
            .append("atApproachLeg: " + atApproachLeg)
            .append(", ")
            .append("nearIntersection: " + nearIntersection)
            .append(", ")
            .append("inIntersection: " + inIntersection)
            .append(", ")
            .append("stepsToWaypoint: " + stepsToWaypoint)
            .append(", ")
            .append("nearWaypoint: " + nearWaypoint)
            .append(", ")
            //.append("nearNextLeg: " + nearNextLeg)
            //.append(", ")
            .append("atNextLeg: " + atNextLeg)
            .append("}\n")
            .toString();
    }
    */

    /** Constructor
     *
     * @param id (required) int label for this class. Should be unique but
     * uniqueness is not checked.
     */
    public VehicleGenerator(int id, Int2D cell, double mean,
                            Direction direction) {
        this.idNum = id;
        this.cell = cell;
        this.mean = mean;
        this.direction = direction;
        this.speed = 0;
        this.rampRate = 0;
    }

    public VehicleGenerator(int id, Int2D cell, double mean,
                            Direction direction, int speed) {
        this(id, cell, mean, direction, speed, 0);
    }

    public VehicleGenerator(int id, Int2D cell, double mean,
                            Direction direction, int speed, double rampRate) {
        this.idNum = id;
        this.cell = cell;
        this.mean = mean;
        this.direction = direction;
        this.speed = speed;
        this.rampRate = rampRate;
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;
        step = ac.schedule.getSteps();
        if (ac.random.nextFloat() < mean + step * rampRate
            && ac.agentGrid.getObjectsAtLocation(cell) == null) {
            Vehicle newVehicle = new Vehicle((int)step*10 + cell.y, direction, speed);
            ac.agentGrid.setObjectLocation(newVehicle, cell);
            newVehicle.stopper =
                ac.schedule.scheduleRepeating(newVehicle, ac.VEHICLE_SCHEDULE_NUM, 1);
            // DriverAgent for Vehicle
            DriverAgent newDriver = new DriverAgent((int)step*10 + cell.y);
            newVehicle.setDriver(newDriver);
            newDriver.setVehicle(newVehicle);
            newVehicle.updateState(ac);
            newDriver.updateState(ac);
            // add Driver to Schedule
            newDriver.stopper =
                ac.schedule.scheduleRepeating(newDriver, ac.DRIVER_SCHEDULE_NUM, 1);
        }
    }
}
