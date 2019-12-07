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
    public static final int VEHICLE_SCHEDULE_NUM = 0;
    public static final int DETECTOR_SCHEDULE_NUM = 1;
    public static final int INTERSECTION_SCHEDULE_NUM = 2;
    public static final int DRIVER_SCHEDULE_NUM = 3;
    public static final int COLLISION_SCHEDULE_NUM = 4;
    public static final int TRIPGEN_SCHEDULE_NUM = 5;
    public static final int REPORT_SCHEDULE_NUM = 6;

    // Units;
    public static final double METERS_PER_CELL = 7.5;
    public static final double SECONDS_PER_STEP = 1;

    // Utility
    public static final boolean LANE_POLICY = true;
    public static final boolean SMART_TURNS = true;
    public static final boolean AVOID_CONGESTION = true;
    public static final boolean RESERVATION_PRIORITY = true;
    public static final boolean PASSENGER_WARM_START = true;
    public static final boolean CONSOLE_OUT = true;
    public static final boolean FILE_OUT = true;
    public static final int MAX_SPEED = 5;
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
    public int onRampStart;

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
        this(seed, 2, 128);
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
        filename = String.format("%s-%d.json",
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
            .append("\"timeMins\": " + step * SECONDS_PER_STEP / 60)
            .append(", ")
            .append("\"numTravelers\": " + (travelers==null ? "null" : travelers.numObjs))
            .append("}},\n")
            .toString();
    }

    public void start() {
        super.start();
        travelers = new Bag();

        if(isTest) {
            makeTestGrids(2, 1024);
        }

        System.out.println(filename);

        Steppable report = new Steppable() {
                public void step(final SimState state) {
                    AgentCity ac = (AgentCity)state;
                    step = schedule.getSteps();
                    if (CONSOLE_OUT) {
                        if (step % 60 == 0) {
                            System.out.print(ac);
                        }
                    }
                    if (FILE_OUT) {
                        //ac.fileout.print(ac);
                    }
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

    public void makeTestGrids(int numLanes, int width) {
        int detectorInterval = 60;
        int downStreamLength = 10;
        int onRampLengthMeters = 1000;
        int onRampLength = (int)(onRampLengthMeters / METERS_PER_CELL);
        int onRampEnd;
        int onRampCenter;

        gridHeight = numLanes;
        gridWidth = width;
        onRampEnd = width - downStreamLength;
        onRampStart = onRampEnd - onRampLength;
        onRampCenter = onRampStart + onRampLength / 2;


        int numIntersections = 0;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.EAST.toInt());
        agentGrid = new SparseGrid2D(gridWidth, gridHeight);

        // make a temporary detectors
        Int2D detectorLocation;
        int[] detectorOffsets = new int[] {1000, 3000, 5000, 7000};
        for (int i = 0; i < detectorOffsets.length; i++) {
            detectorLocation =
                new Int2D(onRampCenter - (int)(detectorOffsets[i] / METERS_PER_CELL), 1);
            Detector tmpDetector = new Detector(this, i, detectorLocation, 2,
                                                detectorInterval);
            tmpDetector.stopper =
                schedule.scheduleRepeating(tmpDetector,
                                           DETECTOR_SCHEDULE_NUM, 1);
        }

        // make an obstacle
        roadGrid.field[onRampEnd][0] = 0;

        // make some vehicles
        /*
        VehicleGenerator testGen1 =
            new VehicleGenerator(1, new Int2D(0,0), 0.15, Direction.EAST,
                                 //random.nextInt(MAX_SPEED) + 1);
                                 3);
        testGen1.stopper =
            schedule.scheduleRepeating(testGen1, TRIPGEN_SCHEDULE_NUM, 1);
        */
        VehicleGenerator testGen2 =
            new VehicleGenerator(2, new Int2D(0,1), 0.8, Direction.EAST,
                                 //random.nextInt(MAX_SPEED) + 1);
                                 5);
        testGen2.stopper =
            schedule.scheduleRepeating(testGen2, TRIPGEN_SCHEDULE_NUM, 1);
    }

    /** Main */
    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        //long seed = 1324367672;
        //long seed = 1324367673;

        SimState state = new AgentCity(seed);
        state.start();
        do {
        } while (state.schedule.step(state));
        System.exit(0);
    }
}
