/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class Intersection implements Steppable {

    // Required for serialization
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Properties
    public final int idNum;

    // Variables
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private Int2D[] approachLegs;
    private Int2D[] departureLegs;

    // Accessors
    public int getID() { return idNum; }
    public Int2D[] getDepartureLegs() { return departureLegs; }
    public Int2D[] getApproachLegs() { return approachLegs; }

    /** Constructor */
    public Intersection(int id, int minX, int maxX, int minY, int maxY, AgentCity ac) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        setLegs(ac);
    }

    /** Constructor */
    public Intersection(int id, int minX, int maxX, int minY, int maxY) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** Constructor */
    public Intersection(int id) {
        this(id, 0, 0, 0, 0);
    }

    private void setLegs(AgentCity ac) {
        int width = maxX - minX;
        int height = maxY - minY;

        Int2D[] legs = new Int2D[2 * width + 2 * height];
        int index = 0;
        if (minY - 1 > 0) {
            for (int x = minX; x <= maxX; x++) {
                legs[index] = new Int2D(x, minY - 1);
                index++;
            }
        }
        if (maxY + 1 < ac.gridHeight) {
            for (int x = minX; x <= maxX; x++) {
                legs[index] = new Int2D(x, maxY + 1);
                index++;
            }
        }
    }

    // Get all departure legs execpt Direction dir
    public Int2D[] getDepartureLegs(AgentCity ac, Direction dir) {
        int numLegs = 0; // numeber of relevant departure legs
        Int2D[] legs; // array to return
        // Count relevant departure legs
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y) 
                    != dir.toInt()) {
                numLegs++;
            }
        }
        // Fill new array with relevant departure legs
        legs = new Int2D[numLegs];
        for (int i = 0; i < departureLegs.length; i++) {
            int nextIndex = 0;
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y) 
                    != dir.toInt()) {
                legs[nextIndex] = departureLegs[i];
                nextIndex++;
            }
        }
        return legs;
    }

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
    }
}
