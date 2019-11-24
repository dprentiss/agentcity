/*
 * Copyright 2019 David Prentiss
 */

package sim.app.agentcity;
import sim.display.Console;
import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.field.grid.*;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.io.*;

public class AgentCity extends SimState {

    // MASON
    Console console;
    private static final long serialVersionUID = 1;

    // Schedule order
    private static final int VEHICLE_SCHEDULE_NUM = 0;
    private static final int DETECTOR_SCHEDULE_NUM = 1;
    private static final int INTERSECTION_SCHEDULE_NUM = 2;
    private static final int DRIVER_SCHEDULE_NUM = 3;
    private static final int COLLISION_SCHEDULE_NUM = 4;
    private static final int TRIPGEN_SCHEDULE_NUM = 5;
    private static final int REPORT_SCHEDULE_NUM = 6;

    // Utility
    public final boolean LANE_POLICY = true;
    public final boolean SMART_TURNS = true;
    public final boolean CONSOLE_OUT = true;
    public final boolean FILE_OUT = false;
    private final boolean checkForCollisions;
    private final boolean isTest;
    private long step;
    private final String filename;
    FileWriter fw;
    BufferedWriter bw;
    public PrintWriter fileout;

    // Grid dimensions
    public int grids;
    public int density;
    public int gridHeight;
    public int gridWidth;

    // Intersection turning movements
    /*
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
        ALL_YEILD
    }
    */

    // Grid of agent locations
    public SparseGrid2D agentGrid;
    // Grids of transportation network feature locations
    public IntGrid2D roadGrid;
    public IntGrid2D walkwayGrid;
    public IntGrid2D parkingGrid;
    public IntGrid2D intersectionGrid;
    public IntGrid2D blockGrid;

    // Array of Intersections
    public Intersection[] intersections;
    // Array of Intersection agents
    public IntersectionAgent[] intersectionAgents;

    // Travelers
    private Bag travelers;
    public boolean removeTraveler(Person person) {
        //travelersInVehicle.remove(person);
        return travelers.remove(person);
    }
    public boolean addTraveler(Person person) {
        //System.out.print(person.toString());
        //travelersInVehicle.add(person);
        return travelers.add(person);
    }

    // Temporary dispatcher
    //public DispatchAgent dispatcher;

    /** Constructor default */
    public AgentCity(long seed) {
        this(seed, 8, 64);
    }

    /** Constructor */
    public AgentCity(long seed, int grids, int density) {
        // Required by SimState
        super(seed);
        isTest = true;
        checkForCollisions = true;
        this.grids = grids;
        this.density = density;
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.systemDefault());
        String dateTimeString = formatter.format(Instant.now());
        filename = String.format("%s-%d.csv",
                                 dateTimeString,
                                 seed);
        if (FILE_OUT) {
            try {
                fw = new FileWriter(filename, true);
                bw = new BufferedWriter(fw);
                fileout = new PrintWriter(bw);
            } catch (IOException e) { }
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("{\"AgentCity\": {")
            .append("\"step\": " + step)
            .append(", ")
            .append("\"numTravelers\": " + (travelers==null ? "null" : travelers.numObjs))
            .append("}},\n")
            .toString();
    }

    public void start() {
        super.start();
        travelers = new Bag();

        if(isTest) {
            makeTestGrids();
        }

        System.out.println(filename);

        Steppable report = new Steppable() {
                public void step(final SimState state) {
                    AgentCity ac = (AgentCity)state;
                    step = schedule.getSteps();
                    System.out.print(ac);
                    if (FILE_OUT) { ac.fileout.print(ac); }
                }
            };

        Steppable collisionCheck = new Steppable() {
                public void step(final SimState state) {
                    AgentCity ac = (AgentCity)state;
                    SparseGrid2D grid = ac.agentGrid;
                    Vehicle v;
                    Bag b;
                    boolean collision = false;
                    for (int i = 0; i < grid.allObjects.numObjs; i++) {
                        v = (Vehicle)grid.allObjects.objs[i];
                        b = grid.getObjectsAtLocationOfObject(v);
                        if (b.numObjs > 1) {
                            System.out.printf("Collision at [%d, %d]\n",
                                              v.getLocation().x, v.getLocation().y);
                            System.out.print(v.toString());
                            System.out.print(v.getDriver().toString());
                            System.out.println();
                            collision = true;
                        }
                    }
                    if (collision) {ac.console.pressPause();}
                }
            };

        if (checkForCollisions) {
            schedule.scheduleRepeating(collisionCheck,
                                       COLLISION_SCHEDULE_NUM, 1);
        }

        schedule.scheduleRepeating(report, REPORT_SCHEDULE_NUM, 1);
    }

    // check the neighbors of each intersection cell for previous labels
    // apply a new integer label if not
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
        makeTestGrids(this.grids, this.density);
    }

    public void makeTestGrids(int grids, int density) {

        int n = grids;
        int vehicleDensity = density;

        final int NUM_VEHICLES = n * n * vehicleDensity;
        gridHeight = n * 38 + 2;
        gridWidth = gridHeight;

        int numIntersections = 0;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.NONE.toInt());
        //walkwayGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        //parkingGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        intersectionGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        blockGrid = new IntGrid2D(gridWidth, gridHeight, 1);

        agentGrid = new SparseGrid2D(gridWidth, gridHeight);

        // Make some roads and blocks
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (x == 0 || (x-2)%38 == 16 || (x-2)%38 == 17 || (x-2)%38 == 36) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.SOUTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (x == 1 || (x-2)%38 == 18 || (x-2)%38 == 19 || (x-2)%38 == 37) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.NORTH.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 0 || (y-2)%38 == 16 || (y-2)%38 == 17 || (y-2)%38 == 36) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.WEST.toInt();
                    blockGrid.field[x][y] = 0;
                }
                if (y == 1 || (y-2)%38 == 18 || (y-2)%38 == 19 || (y-2)%38 == 37) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else roadGrid.field[x][y] = Direction.EAST.toInt();
                    blockGrid.field[x][y] = 0;
                }
            }
        }

        // Make a DispatchAgent
        //dispatcher = new DispatchAgent(0, NUM_VEHICLES);
        //dispatcher.stopper = schedule.scheduleRepeating(dispatcher,
        //DISPATCH_SCHEDULE_NUM, 1);

        // Make some Vehicle and Driver agents
        for (int i = 0; i < NUM_VEHICLES; i++) {
            // Get random location on road
            Int2D newLocation = new Int2D(random.nextInt(gridWidth),
                                          random.nextInt(gridHeight));
            while (roadGrid.get(newLocation.x, newLocation.y) == 0
                   || roadGrid.get(newLocation.x, newLocation.y) == 9
                   || agentGrid.getObjectsAtLocation(newLocation.x,
                                                     newLocation.y) != null) {
                newLocation = new Int2D(random.nextInt(gridWidth),
                                        random.nextInt(gridHeight));
            }
            // One Vehicle on a road cell in the correct direction
            Direction newDir = Direction.byInt(roadGrid.get(newLocation.x,
                                                            newLocation.y));
            Vehicle newVehicle = new Vehicle(i, newDir);
            // add Vehicle to DispatchAgent pool
            //dispatcher.addVehicleToPool(testCar);
            agentGrid.setObjectLocation(newVehicle, newLocation);
            // Add Vehicle to Schedule
            newVehicle.stopper =
                schedule.scheduleRepeating(newVehicle, VEHICLE_SCHEDULE_NUM, 1);
            // DriverAgent for Vehicle
            DriverAgent newDriver = new DriverAgent(i);
            newVehicle.setDriver(newDriver);
            newDriver.setVehicle(newVehicle);
            // add Driver to Schedule
            newDriver.stopper =
                schedule.scheduleRepeating(newDriver, DRIVER_SCHEDULE_NUM, 1);
        }

        // Make some Intersections, IntersectionAgents, and TripGenerators
        int maxXs[] = new int[numIntersections + 1];
        int minXs[] = new int[numIntersections + 1];
        int maxYs[] = new int[numIntersections + 1];
        int minYs[] = new int[numIntersections + 1];
        Arrays.fill(minXs, gridWidth);
        Arrays.fill(minYs, gridHeight);
        intersections = new Intersection[numIntersections + 1];
        intersectionAgents = new IntersectionAgent[numIntersections + 1];
        int inter;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                inter = intersectionGrid.field[x][y];
                if (inter != 0) {
                    maxXs[inter] = (x > maxXs[inter]) ? x : maxXs[inter];
                    minXs[inter] = (x < minXs[inter]) ? x : minXs[inter];
                    maxYs[inter] = (y > maxYs[inter]) ? y : maxYs[inter];
                    minYs[inter] = (y < minYs[inter]) ? y : minYs[inter];
                }
            }
        }
        for (int i = 1; i < numIntersections + 1; i++) {
            intersections[i] =
                new Intersection(i, minXs[i], maxXs[i], minYs[i], maxYs[i],
                                 this);
            intersectionAgents[i] = new IntersectionAgent(i, intersections[i]);
            intersectionAgents[i].stopper =
                schedule.scheduleRepeating(intersectionAgents[i],
                                           INTERSECTION_SCHEDULE_NUM, 1);
            TripGenerator gen =
                new TripGenerator(i, intersections[i], 0.020, random);
            gen.stopper =
                schedule.scheduleRepeating(gen, TRIPGEN_SCHEDULE_NUM, 1);
        }
    }

    /** Main */
    public static void main(String[] args) {
        //long seed = System.currentTimeMillis();
        //long seed = 1324367672;
        long seed = 1324367673;

        SimState state = new AgentCity(seed);
        state.start();
        do {
        } while (state.schedule.step(state));
        System.exit(0);
    }
}
