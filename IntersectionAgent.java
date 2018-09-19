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

    // Properties
    public final int idNum;
    public int scheduleSize;
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
        int width = intersection.maxX - intersection.minX + 1;
        int height = intersection.maxY - intersection.minY + 1;
        scheduleSize = width + height + 3;
        cells = new Int2D[width][height];
        approachLegs = intersection.getApproachLegs();
        schedule = new int[scheduleSize][width][height];
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

    private int[][] getSchedule(int t) {
        return schedule[t % scheduleSize];
    } 

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
    }
}
