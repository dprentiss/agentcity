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
    private static final int intIdNum = 0;

    // Properties
    public final int idNum;
    public Intersection intersection;
    public int width;
    public int height;
    public int scheduleSize;

    // Variables
    private AgentCity ac; // state
    private Int2D[][] cells;
    private Int2D[] approachLegs;
    private Vehicle[][][] schedule;
    private Bag vehicles;
    private boolean acceptingReservations;
    private boolean passengerPriority;
    private long step;

    // Accessors
    public void setPriority(boolean priority) { passengerPriority = priority; }
    public boolean getPriority() { return passengerPriority; }

    public Bag getVehicles() { return vehicles; }

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
        scheduleSize = width + height;
        vehicles = new Bag((width + 2) * (height + 2));
        approachLegs = intersection.getApproachLegs();
        schedule = new Vehicle[scheduleSize][width][height];
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
        setPriority(ac.RESERVATION_PRIORITY);
        setIntersection(intersection);
    }


    /** Considers a request from an approaching Vehicle object and returns
     * whether or not the the request is approved
     */
    public boolean requestReservation(Vehicle vehicle, long time,
                                      Int2D[][] path) {
        // check path against schedule
        return addVehicleToSchedule(vehicle, time, path);
    }

    public boolean cancelReservation(Vehicle vehicle) {
        return removeVehicleFromSchedule(vehicle);
    }

    public String toString(int option) {
        int digits = 4;
        StringBuilder s = new StringBuilder();
        s.append(String.format("IntersectionAgent[%d]", idNum));
        s.append(String.format(" step[%d] schedule[%d/%d]",
                               step, step % scheduleSize, scheduleSize - 1));
        s.append("\n");
        switch (option) {
        case SCHEDULE:
            for (int i = 0; i < height; i++) {
                if (step % scheduleSize == 0) {
                    s.append("\u2551");
                } else {
                    s.append("\u2502");
                }
                for (int j = 0; j < scheduleSize; j++) {
                    for (int k = 0; k < width; k++) {
                        if (schedule[j][k][i] != null) {
                            s.append(String.format("%"+digits+"d",
                                                   schedule[j][k][i].idNum));
                        } else {
                            s.append(String.format("%"+digits+"d", -1));
                        }
                    }
                    if (j == step % scheduleSize
                        || j == step % scheduleSize - 1) {
                        s.append("\u2551");
                    } else {
                        s.append("\u2502");
                    }
                }
                s.append("\n");
            }
        }
        return s.toString();
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
                                         Int2D[][] path) {
        int x;
        int y;
        int timeIndex;
        Vehicle otherVehicle = null;
        DriverAgent otherDriver = null;
        // check if accepting reservations
        if (!acceptingReservations && !vehicles.contains(vehicle)) return false;
        // Check if Vehicles desired path is free
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)((time + i) % scheduleSize);
            for (int j = 0; j < path[i].length; j++) {
                if (path[i][j] != null
                    && intersection.inIntersection(path[i][j])) {
                    x = path[i][j].x - intersection.minX;
                    y = path[i][j].y - intersection.minY;
                    if (schedule[timeIndex][x][y] != null) {
                        if (passengerPriority && height * width > 4) {
                            if (vehicle.meetsHovMin()) {
                                otherVehicle = schedule[timeIndex][x][y];
                                otherDriver = (DriverAgent)otherVehicle.getDriver();
                                otherDriver.updateState(ac);
                                if (otherVehicle.meetsHovMin()
                                    || otherDriver.inIntersection) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }
        }
        // Place vehicle on schedule
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)((time + i) % scheduleSize);
            for (int j = 0; j < path[i].length; j++) {
                if (path[i][j] != null
                    && intersection.inIntersection(path[i][j])) {
                    x = path[i][j].x - intersection.minX;
                    y = path[i][j].y - intersection.minY;
                    if (passengerPriority
                        && schedule[timeIndex][x][y] != null
                        && height * width > 4) {
                        otherVehicle = schedule[timeIndex][x][y];
                        otherDriver = (DriverAgent)otherVehicle.getDriver();
                        removeVehicleFromSchedule(otherVehicle);
                        otherVehicle.hasReservation = false;
                        otherDriver.hasReservation = false;
                        otherDriver.checkReservation(ac);
                        //System.out.print(vehicle.toString());
                        //System.out.print(otherVehicle.toString());
                    }
                    schedule[timeIndex][x][y] = vehicle;
                }
            }
        }
        if (!vehicles.contains(vehicle)) {
        	vehicles.add(vehicle);
        }
        if (intersection.idNum == intIdNum) {
            System.out.println();
            System.out.print(toString(SCHEDULE));
        }
        return true;
    }

    boolean removeVehicleFromSchedule(Vehicle vehicle) {
        boolean removed = vehicles.remove(vehicle);
        for (int i = 0; i < scheduleSize; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    if (schedule[i][j][k] == vehicle) {
                        schedule[i][j][k] = null;
                    }
                }
            }
        }
        return removed;
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
        Vehicle vehicle;
        Bag bag;
        long steps = ac.schedule.getSteps();
        int step = (int)(steps % scheduleSize);
        // Loop over Vehicle with reservations and check if they are on
        // schedule
        for (int i = 0; i < vehicles.numObjs; i++) {
            vehicle = (Vehicle)vehicles.objs[i];
            DriverAgent driver = (DriverAgent)vehicle.getDriver();
            long reservationTime = driver.reservationTime;
            int timeIndex = (int)(steps - reservationTime);
            boolean cannotLeave =
                (timeIndex >= 0
                 && timeIndex < driver.reservationPath.length
                 && !vehicle.location.equals(driver.reservationPath[timeIndex][0]));
            if (cannotLeave) {
                return false;
            }
        }
        // Loop over locations in intersection and check if vehicles match
        // schedule
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                x = cells[i][j].x;
                y = cells[i][j].y;
                vehicle = schedule[step][i][j];
                bag = ac.agentGrid.getObjectsAtLocation(x, y);
                if (bag != null) {
                    if (vehicle != (Vehicle)bag.objs[0]) {
                        return false;
                    }
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
        vehicles.clear();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                x = cells[i][j].x;
                y = cells[i][j].y;
                bag = ac.agentGrid.getObjectsAtLocation(x, y);
                if (bag == null) continue;
                vehicle = (Vehicle)bag.objs[0];
                schedule[(int)(steps % scheduleSize)][i][j] = vehicle;
                vehicles.add(vehicle);
            }
        }
        vehicles.shuffle(ac.random);
        for (int k = 0; k < vehicles.numObjs; k++) {
            vehicle = (Vehicle)vehicles.objs[k];
            ((DriverAgent)vehicle.getDriver()).updateReservation(ac);
        }
        if (intersection.getNumCells() <= 4) {
            acceptingReservations = true;
            return;
        }
    }

    private void checkSchedule() {
        int x;
        int y;
        Vehicle vehicle;
        Driver driver;
        Bag bag;
        long steps = ac.schedule.getSteps();
        // If schedule is wrong revoke all reservations and clear schedule
        if (!scheduleValid(ac)) {
            if (this.idNum == intIdNum) {
                System.out.printf("*** Schedule not valid at intersection %d\n",
                                  this.idNum);

                System.out.print(this.toString(SCHEDULE));
            }
            acceptingReservations = false;
            clearSchedule();
            // Add Vehicles already in the intersection to schedule and bag
            allowPriorityReservations(ac);
        } else {
            acceptingReservations = true;
        }
    }

    public boolean legBlocked(Int2D leg) {
        int x = leg.x;
        int y = leg.y;
        Bag b;
        int numCells = 5;
        Direction dir = Direction.byInt(ac.roadGrid.get(x, y));
            for (int i = 0; i < numCells; i++) {
                b = ac.agentGrid.getObjectsAtLocation(x, y);
            if (b != null) {
                Vehicle vehicle = (Vehicle)b.objs[0];
                if (vehicle.getSpeed() == 0) {
                    return true;
                }
            }
            x = x + dir.getXOffset();
            y = y + dir.getYOffset();
        }
        return false;
    }

    private void clearSchedule() {
        Vehicle v;
        // clear every cell of the schedule
        for (int i = 0; i < scheduleSize; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    schedule[i][j][k] = null;
                }
            }
        }
        // cancel all reservations
        for (int i = 0; i < vehicles.numObjs; i++) {
            v = (Vehicle)vehicles.objs[i];
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
                schedule[step][j][k] = null;
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
        ac = (AgentCity)state;
        step = ac.schedule.getSteps();
        trimSchedule(ac);
        checkSchedule();
        if (intersection.idNum == intIdNum) {
            System.out.println();
            System.out.printf("Step %d, schedule %d/%d\n",
                              step,
                              step % scheduleSize,
                              scheduleSize);
            System.out.print(toString(SCHEDULE));
        }
    }
}
