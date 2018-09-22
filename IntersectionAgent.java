/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

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
    public int scheduleSize;
    public int width;
    public int height;
    public Intersection intersection;

    // Variables
    private Int2D[] approachLegs;
    private Int2D[][] cells;
    private int[][][] schedule;

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
        scheduleSize = width + height + 3;
        cells = new Int2D[width][height];
        approachLegs = intersection.getApproachLegs();
        schedule = new int[scheduleSize][width][height];
        for (int i = 0; i < scheduleSize; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    schedule[i][j][k] = -1;
                }
            }
        }
        intersection.setController(this);
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
    public int requestReservation(Vehicle vehicle, long time, Int2D[] path) {
        boolean vehicleAdded;
        // check path against schedule
        vehicleAdded = addVehicleToSchedule(vehicle, time, path);
        System.out.print(toString(SCHEDULE));
        // return -1 (denied) always for testing TODO
        return -1;
    }

    public String toString(int option) {
        String s = "";
        s += String.format("IntersectionAgent[%d]\n", idNum);
        switch (option) {
            case SCHEDULE:
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < 7; j++) {
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

    private boolean addVehicleToSchedule(Vehicle vehicle, long time, Int2D[] path) {
        int pathX;
        int pathY;
        int timeIndex;
        System.out.println();
        System.out.println(vehicle.idNum);
        System.out.println(vehicle.getLocation());
        System.out.println(time);
        System.out.println(scheduleSize);
        System.out.println(time % scheduleSize);
        for (int i = 0; i < path.length; i++) {
            timeIndex = (int)(i % scheduleSize);
            pathX = path[i].x % width;
            pathY = path[i].y % height;
            System.out.println();
            System.out.println(schedule.length);
            System.out.println((int)(time % scheduleSize));
            System.out.println(schedule[0].length);
            System.out.println(pathX);
            System.out.println(schedule[1].length);
            System.out.println(pathY);
            if (schedule[timeIndex][pathX][pathY] == -1) {
                schedule[timeIndex][pathX][pathY] = vehicle.idNum;
            }
        }
        return false;
    }

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
    }
}
