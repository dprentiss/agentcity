/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

/** IntersectionAgent objects control traffic for a specific, contiguous
 * Intersection in the grid.
 *
 * @author David Prentiss
 */
public class IntersectionAgent implements Steppable {

    /** Required by MASON for serialization. */
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Utility
    private static final int NONE = 0;
    private static final int SCHEDULE = 1;

    // Properties
    public final int idNum;
    public Intersection intersection;
    public int width;
    public int height;
    public int scheduleSize;

    // Variables
    private Int2D[][] cells;
    private Int2D[] approachLegs;
    private int[][][] schedule;
    private Bag vehicles;
    private boolean acceptingReservations;

    // Accessors

    /** Gets the Intersection object controlled by this IntersectionAgent. */
    public Intersection getIntersection() { return intersection; }

    /** Sets the provided Intersection object to be controlled by this
     * IntersectionAgent.
     *
     * @param intersection the Intersection object to be controlled by this
     * IntersectionAgent.
     */
    public void setIntersection(Intersection intersection) {
        this.intersection = intersection;
        this.width = intersection.maxX - intersection.minX + 1;
        this.height = intersection.maxY - intersection.minY + 1;
        scheduleSize = width + height + 4;
        vehicles = new Bag((width + 2) * (height + 2));
        approachLegs = intersection.getApproachLegs();
        schedule = new int[scheduleSize][width][height];
        cells = new Int2D[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                cells[i][j] = new Int2D(intersection.minX + i,
                                        intersection.minY + j);
            }
        }
        intersection.setController(this);
        clearSchedule();
    }

    /** Creates and IntersectionAgent object with the provided ID number and
     * associated Intersection object.
     *
     * @param id the ID number of this IntersectionAgent.
     * This number should be unique but this is not enforced.
     */
    public IntersectionAgent(int id, Intersection intersection) {
        this.idNum = id;
        setIntersection(intersection);
    }

    /** Considers a request from an approaching Vehicle object and returns
     * whether or not the the request is approved
     */
    public boolean requestReservation(Vehicle vehicle, long time, Int2D[] path) {
        // check path against schedule
        return addVehicleToSchedule(vehicle, time, path);
    }

    public String toString(int option) {
        String s = "";
        s += String.format("IntersectionAgent[%d]\n", idNum);
        switch (option) {
        case SCHEDULE:
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < scheduleSize; j++) {
                    for (int k = 0; k < width; k++) {
                        s += String.format("%4d", schedule[j][k][i]);
                    }
                    s += "|";
                }
                s += "\n";
            }
        }
        return s;
    }

    public String toString() {
        return toString(NONE);
    }

    /** Handles reservation requests for the Intersection controlled by this
     * intersection agent.
     *
     * @param vehicle Vehicle requesting reservation
     * @param time
     */
    private boolean addVehicleToSchedule(Vehicle vehicle,
                                         long time,
                                         Int2D[] path) {
        int x;
        int y;
        int timeIndex;
        // check if accepting reservations
        if (!acceptingReservations && !vehicles.contains(vehicle)) return false;
        // Check if Vehicles desired path is free
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)((time + i) % scheduleSize);
            x = path[i].x - intersection.minX;
            y = path[i].y - intersection.minY;
            if (schedule[timeIndex][x][y] != -1) {
                return false;
            }
        }
        // Place vehicle on schedule
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)((time + i) % scheduleSize);
            x = path[i].x - intersection.minX;
            y = path[i].y - intersection.minY;
            schedule[timeIndex][x][y] = vehicle.idNum;
        }
        vehicles.add(vehicle);
        return true;
    }

    /** Checks the schedule against a tally of vehicles actually present in the
     * intersection.
     *
     * @param ac Current state of the simulation
     *
     * @return True if vehicles present match schedule
     */
    private boolean scheduleValid(AgentCity ac) {
        int x;
        int y;
        int vehicleNum;
        Vehicle vehicle;
        Bag bag;
        long steps = ac.schedule.getSteps();
        int step = (int)(steps % scheduleSize);
        // Loop over locations in intersection and check if vehicles match
        // schedule
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                x = cells[i][j].x;
                y = cells[i][j].y;
                vehicleNum = schedule[step][i][j];
                bag = ac.agentGrid.getObjectsAtLocation(x, y);
                if (bag != null) {
                    vehicle = (Vehicle)bag.objs[0];
                    if (vehicleNum != vehicle.idNum) {
                        return false;
                    }
                } else if (vehicleNum != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void allowPriorityReservations(AgentCity ac) {
        int x;
        int y;
        Vehicle vehicle;
        Bag bag;
        long steps = ac.schedule.getSteps();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                x = cells[i][j].x;
                y = cells[i][j].y;
                bag = ac.agentGrid.getObjectsAtLocation(x, y);
                if (bag == null) continue;
                vehicle = (Vehicle)bag.objs[0];
                schedule[(int)(steps % scheduleSize)][i][j] = vehicle.idNum;
                vehicles.add(vehicle);
                /*
                if (intersection.idNum == 5) {
                    System.out.println(vehicles.numObjs);
                    System.out.println(vehicle);
                }
                */
            }
        }
    }

    private void checkSchedule(AgentCity ac) {
        int x;
        int y;
        Vehicle vehicle;
        Driver driver;
        Bag bag;
        long steps = ac.schedule.getSteps();
        // If schedule is wrong revoke all reservations and clear schedule
        if (!scheduleValid(ac)) {
            acceptingReservations = false;
            clearSchedule();
            // Add Vehicles already in the intersection to schedule and bag
            allowPriorityReservations(ac);
        } else {
            acceptingReservations = true;
        }
        /*
        for (int k = 0; k < vehicles.numObjs; k++) {
            vehicle = (Vehicle)vehicles.objs[k];
            driver = (Driver)vehicle.getDriver();
            if (driver.getNextDirective() == Driver.Directive.STOP) {
                clearSchedule();
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        x = cells[i][j].x;
                        y = cells[i][j].y;
                        bag = ac.agentGrid.getObjectsAtLocation(x, y);
                        if (bag == null) continue;
                        vehicle = (Vehicle)bag.objs[0];
                        schedule[(int)(steps % scheduleSize)][i][j]
                            = vehicle.idNum;
                        vehicles.add(vehicle);
                    }
                }
            }
        */
    }

    private void clearSchedule() {
        Vehicle v;
        for (int i = 0; i < scheduleSize; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    schedule[i][j][k] = -1;
                }
            }
        }
        while (vehicles.numObjs > 0) {
            v = (Vehicle)vehicles.pop();
            v.hasReservation = false;
            ((DriverAgent)v.getDriver()).hasReservation = false;
        }
    }

    private void trimSchedule(AgentCity ac) {
        Vehicle v;
        long steps = ac.schedule.getSteps();
        int step = (int)((scheduleSize + steps - 1) % scheduleSize);
        for (int j = 0; j < width; j++) {
            for (int k = 0; k < height; k++) {
                schedule[step][j][k] = -1;
            }
        }
        // remove vehicles without reservations from Bag
        for (int i = 0; i < vehicles.numObjs; i++) {
            v = (Vehicle)vehicles.objs[i];
            if (!v.hasReservation) {
                vehicles.removeNondestructively(i);
            }
            vehicles.shrink(0);
        }
    }

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
        trimSchedule(ac);
        checkSchedule(ac);
        if (intersection.idNum == 5) {
            System.out.println();
            System.out.printf("Step %d, schedule %d/%d\n",
                              ac.schedule.getSteps(),
                              ac.schedule.getSteps() % scheduleSize,
                              scheduleSize);
            System.out.print(toString(SCHEDULE));
        }
    }
}
