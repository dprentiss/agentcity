/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

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
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        Int2D[] tmpDep = new Int2D[2 * width + 2 * height];
        Int2D[] tmpApp = new Int2D[2 * width + 2 * height];
        int index = 0;
        // look around intersection for approach and departure legs
        if (minY - 1 >= 0) {
            for (int x = minX; x <= maxX; x++) {
                if (ac.roadGrid.field[x][minY - 1]
                        == Direction.SOUTH.toInt()) {
                    tmpApp[index] = new Int2D(x, minY - 1);
                } else if (ac.roadGrid.field[x][minY - 1]
                        == Direction.NORTH.toInt()) {
                    tmpDep[index] = new Int2D(x, minY - 1);
                }
                index++;
            }
        }
        if (maxY + 1 < ac.gridHeight) {
            for (int x = minX; x <= maxX; x++) {
                if (ac.roadGrid.field[x][maxY + 1]
                        == Direction.NORTH.toInt()) {
                    tmpApp[index] = new Int2D(x, maxY + 1);
                } else if (ac.roadGrid.field[x][maxY + 1]
                        == Direction.SOUTH.toInt()) {
                    tmpDep[index] = new Int2D(x, maxY + 1);
                }
                index++;
            }
        }
        if (minX - 1 >= 0) {
            for (int y = minY; y <= maxY; y++) {
                if (ac.roadGrid.field[minX - 1][y]
                        == Direction.EAST.toInt()) {
                    tmpApp[index] = new Int2D(minX - 1, y);
                } else if (ac.roadGrid.field[minX - 1][y]
                        == Direction.WEST.toInt()) {
                    tmpDep[index] = new Int2D(minX - 1, y);
                }
                index++;
            }
        }
        if (maxX + 1 < ac.gridHeight) {
            for (int y = minY; y <= maxY; y++) {
                if (ac.roadGrid.field[maxX + 1][y]
                        == Direction.WEST.toInt()) {
                    tmpApp[index] = new Int2D(maxX + 1, y);
                } else if (ac.roadGrid.field[maxX + 1][y]
                        == Direction.EAST.toInt()) {
                    tmpDep[index] = new Int2D(maxX + 1, y);
                }
                index++;
            }
        }

        // Count approach and departure legs
        int numApp = 0;
        int numDep = 0;
        for (int i = 0; i < tmpApp.length; i++) {
            if (tmpApp[i] != null) numApp++;
            if (tmpDep[i] != null) numDep++;
        }

        // Populate arrays with legs
        approachLegs = new Int2D[numApp];
        departureLegs = new Int2D[numDep];
        index = 0;
        for (int i = 0; i < tmpApp.length; i++) {
            if (tmpApp[i] != null) {
                approachLegs[index] = tmpApp[i];
                index++;
            }
        }
        index = 0;
        for (int i = 0; i < tmpDep.length; i++) {
            if (tmpDep[i] != null) {
                departureLegs[index] = tmpDep[i];
                index++;
            }
        }
        System.out.printf("id: %d, minX: %d, maxX: %d, minY: %d, maxY: %d\n", idNum, minX, maxX, minY, maxY);
        System.out.printf("Intesection %d, width %d, height %d\n", idNum, width, height);
        System.out.println("Departure legs:");
        System.out.println(departureLegs.length);
        System.out.println(Arrays.toString(departureLegs));
        System.out.println("Approach legs:");
        System.out.println(approachLegs.length);
        System.out.println(Arrays.toString(approachLegs));
        System.out.println(" ");
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
