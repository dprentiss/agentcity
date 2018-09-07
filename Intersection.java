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

    /** Constructor */
    public Intersection(int id, int minX, int maxX, int minY, int maxY) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public Int2D[] getDepartureLegs(AgentCity ac, Direction dir) {
        int numLegs = 0;
        Int2D[] legs = new Int2D[numLegs];
        // Count relevant departure legs
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y) 
                    == dir.toInt()) {
                numLegs++;
            }
        }
        // Fill new array with departure legs
        for (int i = 0; i < departureLegs.length; i++) {
            int nextIndex = 0;
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y) 
                    == dir.toInt()) {
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
