/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import java.util.Map;

public class AgentCity extends SimState {

    // Required for serialization
    private static final long serialVersionUID = 1;

    private boolean isTest = false;

    // Grid dimensions
    public int gridHeight;
    public int gridWidth;

    // Grid directions
    enum Direction {
        NONE(0),
        NORTH(1),
        NORTH_EAST(2),
        EAST(3),
        SOUTH_EAST(4),
        SOUTH(5),
        SOUTH_WEST(6),
        WEST(7),
        NORTH_WEST(8),
        ALL(9);

        private final int dirNum;

        private final static Map<Integer, Direction> map =
            stream(Direction.values()).collect(toMap(dir -> dir.dirNum, dir -> dir));
        
        private Direction(final int dir) {
            this.dirNum = dir;
        }

        public int toInt() { return dirNum; }

        public static Direction byInt(int dirNum) {
            return map.get(dirNum);
        }
    }

    // Intersection turning movements
    enum LaneSignalState {
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

    public void makeTestGrids() {

        gridHeight = 40;
        gridWidth = 40;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.NONE.toInt());
        walkwayGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        parkingGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        intersectionGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        blockGrid = new IntGrid2D(gridWidth, gridHeight, 1);

        agentGrid = new SparseGrid2D(gridWidth, gridHeight);

        // Make some roads and blocks
        for (int x = 0; x < gridWidth; x++) { 
            for (int y = 0; y < gridHeight; y++) { 
                if (x == 0 || x == 18 || x == 19 || x == 38) {
                    if (roadGrid.field[x][y] != 0) {
                        intersectionGrid.field[x][y] = 1;
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.SOUTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (x == 1 || x == 20 || x == 21 || x == 39) {
                    if (roadGrid.field[x][y] != 0) {
                        intersectionGrid.field[x][y] = 1;
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.NORTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 0 || y == 18 || y == 19 || y == 38) {
                    if (roadGrid.field[x][y] != 0) {
                        intersectionGrid.field[x][y] = 1;
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.WEST.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 1 || y == 20 || y == 21 || y == 39) {
                    if (roadGrid.field[x][y] != 0) {
                        intersectionGrid.field[x][y] = 1;
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.EAST.toInt();
                    blockGrid.field[x][y] = 0;
                }
            }
        }

        // Make some agents
        // Get random location on road
        Int2D newLocation = new Int2D(random.nextInt(gridWidth), random.nextInt(gridHeight));
        while (roadGrid.get(newLocation.x, newLocation.y) == 0) {
            newLocation = new Int2D(random.nextInt(gridWidth), random.nextInt(gridHeight));
        }
        // One Vehicle on a road cell in the correct direction
        Direction newDir = Direction.byInt(roadGrid.get(newLocation.x, newLocation.y));
        Vehicle testCar = new Vehicle(0, newDir);
        agentGrid.setObjectLocation(testCar, newLocation);
        testCar.stopper = schedule.scheduleRepeating(testCar);
    }

    /** Main */
    public static void main(String[] args) {
        doLoop(AgentCity.class, args);
        System.exit(0);
    }
}
