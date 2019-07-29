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
    private Bag clients;
    private Bag idleVehicles;

    // Accessors
    public int requestVehicle(Intersection location,
                              Intersection destination,
                              Person person,
                              long step) {
        if (idleVehicles.numObjs > 0) {
            assignVehicle((Vehicle)idleVehicles.pop(),
                          person,
                          location,
                          destination);
        }
        return -1;
    }

    public int addVehicleToPool(Vehicle vehicle) {
        if(vehiclePool.add(vehicle)) {
            idleVehicles.add(vehicle);
            return 0;
        }
        return -1;
    }

    private int assignVehicle(Vehicle vehicle,
                              Person person,
                              Intersection start,
                              Intersection end) {
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
