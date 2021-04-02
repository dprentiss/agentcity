/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
//import sim.util.*;
import sim.engine.*;
//import java.util.Arrays;

/** LaneAgent objects control traffic for a specific, contiguous
 * Lane in the grid.
 *
 * @author David Prentiss
 */
public class LaneAgent implements Steppable {

    /** Required by MASON for serialization. */
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Utility
    /*
    private static final int NONE = 0;
    private static final int SCHEDULE = 1;
    private static final int intIdNum = 0;

    */
    // Properties
    public final int idNum;
    public Lane lane;
    public int width;
    public int height;
    /*
    public int scheduleSize;
    */

    // Variables
    private AgentCity ac; // state
    private long step;
    /*
    private Int2D[][] cells;
    private Int2D[] approachLegs;
    private Vehicle[][][] schedule;
    private Bag vehicles;
    private boolean acceptingReservations;
    private boolean reservationPriority;

    // Reporting variables
    public int[] reservationsCompleted;
    public int[] reservationsCanceled;
    public int scheduleInvalid = 0;

    // Accessors
    public void setPriority(boolean priority) { reservationPriority = priority; }
    public boolean getPriority() { return reservationPriority; }

    public Bag getVehicles() { return vehicles; }
    */

    /** Gets the Lane object controlled by this LaneAgent. */
    public Lane getLane() { return lane; }

    /** Sets the provided Lane object to be controlled by this
     * LaneAgent.
     *
     * @param lane the Lane object to be controlled by this
     * LaneAgent.
     */
    public void setLane(Lane lane) {
        this.lane = lane;
        width = lane.width;
        height = lane.height;
        //TODO remove hard-coded scheduleSize bias
        /*
        scheduleSize = width + height + 1;
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
        */
    }

    /** Creates and LaneAgent object with the provided ID number and
     * associated Lane object.
     *
     * @param id the ID number of this LaneAgent.
     * This number should be unique but this is not enforced.
     */
    public LaneAgent(int id, Lane lane) {
        this.idNum = id;
        /*
        this.reservationsCanceled = new int[ac.PASSENGER_CAP+1];
        this.reservationsCompleted = new int[ac.PASSENGER_CAP+1];
        setPriority(ac.RESERVATION_PRIORITY
                    && width * height >= ac.MIN_INTERSECTION_CONTROL_SIZE);
        */
        setLane(lane);
    }


    /*
    public boolean requestReservation(Vehicle vehicle, long time,
                                      Int2D[][] path) {
        // check path against schedule
        return addVehicleToSchedule(vehicle, time, path);
    }

    public boolean cancelReservation(Vehicle vehicle, boolean complete) {
        boolean canceled = removeVehicleFromSchedule(vehicle);
        if (canceled) {
            DriverAgent driver = (DriverAgent)vehicle.getDriver();
            int numPassengers = vehicle.getNumPassengers();
            vehicle.hasReservation = false;
            driver.hasReservation = false;
            driver.checkReservation(ac);
            if (complete) {
                reservationsCompleted[numPassengers]++;
            } else {
                reservationsCanceled[numPassengers]++;
            }
        }
        return canceled;
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

    private int getVehiclePriority(Vehicle vehicle) {
        int priority = 0;
        DriverAgent driver = (DriverAgent)vehicle.getDriver();
        Int2D leg = driver.nextApproachLeg;
        priority += vehicle.getNumPassengers();
        priority += countVehicles(ac, leg, true);
        //System.out.println(vehicle);
        //System.out.println(priority);
        return priority;
    }

    private int countVehicles(AgentCity ac, Int2D leg, boolean isApproachLeg) {
        int count = 0;
        Direction dir = Direction.byInt(ac.roadGrid.get(leg.x, leg.y));
        Int2D cell = leg;
        Direction cellDir = dir;
        Direction countDir = (isApproachLeg ? dir.opposite(): dir);
        while (cellDir == dir) {
            cell = ac.getCellAhead(cell, countDir, 1);
            cellDir = Direction.byInt(ac.roadGrid.get(cell.x, cell.y));
            if (ac.agentGrid.getObjectsAtLocation(cell.x, cell.y) != null) {
                count++;
            }
        }
        return count;
    }

    private boolean addVehicleToSchedule(Vehicle vehicle,
                                         long time,
                                         Int2D[][] path) {
        int x;
        int y;
        int timeIndex;
        boolean hasPriority;
        Vehicle otherVehicle = null;
        DriverAgent otherDriver = null;
        // check if accepting reservations
        if (!acceptingReservations && !vehicles.contains(vehicle)) return false;
        // Check if Vehicle's desired path is free
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)((time + i) % scheduleSize);
            for (int j = 0; j < path[i].length; j++) {
                if (path[i][j] != null
                    && intersection.inIntersection(path[i][j])) {
                    x = path[i][j].x - intersection.minX;
                    y = path[i][j].y - intersection.minY;
                    // if reservation conflicts with existing vehicle
                    if (schedule[timeIndex][x][y] != null) {
                        if (reservationPriority) {
                            otherVehicle = schedule[timeIndex][x][y];
                            otherDriver = (DriverAgent)otherVehicle.getDriver();
                            otherDriver.updateState(ac);
                            hasPriority = getVehiclePriority(otherVehicle)
                                >= getVehiclePriority(vehicle);
                            if (hasPriority || otherDriver.inIntersection) {
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
                    if (reservationPriority
                        && schedule[timeIndex][x][y] != null) {
                        otherVehicle = schedule[timeIndex][x][y];
                        cancelReservation(otherVehicle, false);
                        //if (otherVehicle.getLocation() == ac.getCellAhead(vehicle.getLocation(),vehicle.getDirection(),1)) {
                            if (false) {
                            System.out.print(vehicle.toString());
                            System.out.print(otherVehicle.toString());
                            System.out.println(getVehiclePriority(vehicle));
                            System.out.println(getVehiclePriority(otherVehicle));
                            System.out.print("\n");
                        }
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
        if (removed) {
            for (int i = 0; i < scheduleSize; i++) {
                for (int j = 0; j < width; j++) {
                    for (int k = 0; k < height; k++) {
                        if (schedule[i][j][k] == vehicle) {
                            schedule[i][j][k] = null;
                        }
                    }
                }
            }
        }
        return removed;
    }

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
            scheduleInvalid++;
        } else {
            acceptingReservations = true;
        }
    }

    public boolean legBlocked(Int2D leg) {
        int x = leg.x;
        int y = leg.y;
        Bag b;
        int numCells = ac.NUM_BLOCKED_LEG_CELLS;
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

    */
    public void step(final SimState state) {
        /*
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
        */
    }
}