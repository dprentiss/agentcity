/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

public class Detector implements Steppable {

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
    AgentCity ac;
    Direction direction;
    long step;
    int stepIdx;
    int nextIdx;
    Vehicle vehicle;
    int bufferSize;
    Bag b = new Bag();
    Bag vehicles = new Bag();
    Bag buffer = new Bag();
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
        for (int j = 0; j < this.width; j++) {
            for (int k = 0; k < this.height; k++) {
                cells[j + k] = new Int2D(j + minX, k + minY);
                System.out.println(cells[j + k]);
            }
        }
    }

    /** Constructor */
    /*
    public Detector(int id, Int2D cell, int numLanes) {
        direction = Direction.byInt(ac.roadGrid.get(cell.x, cell.y));
    }
    */

    public void step(final SimState state) {
        ac = (AgentCity)state;
        int j = 0;

        for (int i = 0; i < cells.length; i++) {
            System.out.printf("i = %d\n", i);
            System.out.println(cells[i]);
            b = ac.agentGrid.getObjectsAtLocation(cells[i]);
            if (b == null) {
                continue;
            }
            vehicle = (Vehicle)b.objs[0];
            buffer.add(vehicle);
            if (vehicles.contains(vehicle)) continue;
            vehicles.add(vehicle);
            System.out.print(vehicle);
        }
        j = 0;
        while (j < vehicles.numObjs) {
            if (buffer.contains(vehicles.objs[j])) {
                j++;
            } else {
                vehicles.remove(vehicles.objs[j]);
            }
        }
    }
}
