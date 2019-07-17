/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

/**
 * Generic agent for dispatching {@link Vehicle} objects to {@link Person}
 * objects.
 */

public class DispatchAgent implements Steppable, Dispatcher {

    // MASON
    private static final long serialVersionUID = 1;
    Stoppable stopper;
    long step;

    // Properties
    public final int idNum;

    // Variables
    private Bag vehiclePool;
    private Bag idleVehicles;

    // Accessors
    public int requestVehicle(Intersection intersection, long step) {
        return 0;
    }

    public int addVehicleToPool(Vehicle vehicle) {
        if(vehiclePool.add(vehicle)) {
            idleVehicles.add(vehicle);
            return 0;
        }
        return -1;
    }

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

    public DispatchAgent(int id, int poolSize) {
        idNum = id;
        vehiclePool = new Bag(poolSize);
        idleVehicles = new Bag(poolSize);
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;
        step = ac.schedule.getSteps();
    }
}
