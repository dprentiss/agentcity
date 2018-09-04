/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public class AgentCity extends SimState {

    // Required for serialization
    private static final long serialVersionUID = 1;

    private boolean isTest = false;

    // Grid dimensions
    public int gridHeight;
    public int gridWidth;

    // Intersection turning movements
    enum TurningMovements {
        NONE,
        STOP,
        STRAIGHT,
        RIGHT,
        LEFT,
        STRAIGHT_RIGHT,
        STRAIGHT_LEFT,
        RIGHT_LEFT,
        ALL,
        STRAIGHT_YEILD,
        RIGHT_YEILD,
        LEFT_YEILD,
        STRAIGHT_RIGHT_YEILD,
        STRAIGHT_LEFT_YEILD,
        RIGHT_LEFT_YEILD,
        ALL_YEILD,
    }

    // Grid of agent locations
    public SparseGrid2D agentGrid;
    // Grids of transportation network feature locations
    public IntGrid2D roadGrid;
    public IntGrid2D walkwayGrid;
    public IntGrid2D parkingGrid;
    public IntGrid2D intersectionGrid;
    public IntGrid2D blockGrid;

    /** Constructor default */
    public AgentCity(long seed) {
        // Required by SimState
        super(seed);

        isTest = true;
    }
    
    /** Constructor */
    public AgentCity(long seed, int height, int width) {
        // Required by SimState
        super(seed);
    }
    
    public void start() {
        super.start();

        if(isTest) {
            makeTestGrids();
        }
    }

    private int labelIntersection(int cellX, int cellY, int num) {
        int label = 0;
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (!checkBounds(cellX + x, cellY + y)) {
                    continue;
                }
                label = intersectionGrid.field[cellX + x][cellY + y];
                if (label != 0) {
                    intersectionGrid.field[cellX][cellY] = label;
                    return 0;
                }
            }
        }
        intersectionGrid.field[cellX][cellY] = num + 1;
        return 1;
    }

    public boolean checkBounds(int x, int y) {
        if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight)
            return true;
        return false;
    }

    public void makeTestGrids() {

        final int NUM_VEHICLES = 32;

        gridHeight = 40;
        gridWidth = 40;

        int numIntersections = 0;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.NONE.toInt());
        walkwayGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        parkingGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        intersectionGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        blockGrid = new IntGrid2D(gridWidth, gridHeight, 1);

        agentGrid = new SparseGrid2D(gridWidth, gridHeight);

        // check the neighbors of each intersection cell for previous labels
        // apply a new iteger label if not

        // Make some roads and blocks
        for (int x = 0; x < gridWidth; x++) { 
            for (int y = 0; y < gridHeight; y++) { 
                if (x == 0 || x == 18 || x == 19 || x == 38) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections += labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.SOUTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (x == 1 || x == 20 || x == 21 || x == 39) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections += labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.NORTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 0 || y == 18 || y == 19 || y == 38) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections += labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.WEST.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 1 || y == 20 || y == 21 || y == 39) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections += labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.EAST.toInt();
                    blockGrid.field[x][y] = 0;
                }
            }
        }

        // Make some agents
        for (int i = 0; i < NUM_VEHICLES; i++) {
            // Get random location on road
            Int2D newLocation = new Int2D(random.nextInt(gridWidth), random.nextInt(gridHeight));
            while (roadGrid.get(newLocation.x, newLocation.y) == 0
                    || roadGrid.get(newLocation.x, newLocation.y) == 9
                    || agentGrid.getObjectsAtLocation(newLocation.x, newLocation.y) != null) {
                newLocation = new Int2D(random.nextInt(gridWidth), random.nextInt(gridHeight));
                    }
            // One Vehicle on a road cell in the correct direction
            Direction newDir = Direction.byInt(roadGrid.get(newLocation.x, newLocation.y));
            Vehicle testCar = new Vehicle(i, newDir);
            agentGrid.setObjectLocation(testCar, newLocation);
            // Add Vehicle to Schedule
            testCar.stopper = schedule.scheduleRepeating(testCar, 1, 1);
            // DriverAgent for Vehicle
            DriverAgent newDriver = new DriverAgent(i);
            testCar.setDriver(newDriver);
            newDriver.setVehicle(testCar);
            // add Driver to Schedule
            newDriver.stopper = schedule.scheduleRepeating(newDriver, 0, 1);
        }
    }

    /** Main */
    public static void main(String[] args) {
        doLoop(AgentCity.class, args);
        System.exit(0);
    }
}
