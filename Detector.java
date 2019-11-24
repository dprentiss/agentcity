/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

public class Detector {

    // Required for serialization
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Properties
    public final int idNum;
    public final int minX;
    public final int maxX;
    public final int minY;
    public final int maxY;

    // Variables
    private int width;
    private int height;
    private Int2D[] cells;

    // Accessors

    /** Constructor */
    public Detector(int id, int minX, int maxX, int minY, int maxY) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;

        this.width = maxX - minX + 1;
        this.height = maxY - minY + 1;

        cells = new Int2D[this.width * this.height];

        int k = 0;
        for (int i = this.minX; i < this.width; i++) {
            for (int j = this.minY; j < this.height; j++) {
                cells[k] = new Int2D(i, j);
            }
        }
    }

    /** Constructor */
    /*
    public Detector(int id, Int2D cell, AgentCity ac) {
        idNum = id;
        direction = Direction.byInt(ac.roadGrid.get(cell.x, cell.y));
    }
    */

    public void step(final SimState state) {
        /*
        AgentCity ac = (AgentCity)state;
        Bag b;
        for (int i = 0; i < cells.length; i++) {
            b = ac.agentGrid.getObjectsAtLocation(cells[i]);
            if(vehicles != null) {

            }
        }
        */
    }
}
