/*
 * Copyright 2019 David Prentiss
 */

package sim.app.agentcity;
import ec.util.MersenneTwisterFast;
import sim.display.Console;
import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.field.grid.*;
import java.util.Arrays;
import java.util.Random;
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

    // Simulation constants
    long lastTripStep = 0;
    public final double TRIPGEN_RATE;
    final boolean LANE_POLICY;
    final long seed;
    public final int HOV_MIN;
    public static final boolean SMART_TURNS = true;
    public static final boolean AVOID_CONGESTION = true;
    public static final boolean RESERVATION_PRIORITY = true;
    public static final boolean RESERVATION_PRIORITY_SMALL = false;
    public static final boolean PASSENGER_WARM_START = false;
    public static final double WARM_START_RATE = 0.5;
    public static final boolean CONSOLE_OUT = true;
    public static final boolean FILE_OUT = false;
    public static final int MAX_SPEED = 2;
    public static final int REPORT_INTERVAL = 600;
    //public static final int PASSENGER_POLLING_INTERVAL = 600;
    public static final double SECONDS_PER_STEP = 1;
    public static final double METERS_PER_CELL = 7.5;

    // Utility
    private static final int DEFAULT_HOV_MIN = 4;
    private static final boolean DEFAULT_LANE_USE_POLICY = false;
    private static final double DEFAULT_TRIP_GEN_RATE = 0.2;
    private static final int DEFAULT_VEHICLE_DENSITY = 144;
    private static final int DEFAULT_NUM_GRIDS = 4;
    private final boolean CHECK_FOR_COLLISIONS = false;
    private final boolean isTest;
    private final String filename;
    private FileWriter fw;
    private BufferedWriter bw;
    public PrintWriter fileout;

    // Variables
    private int numVehicles;
    private long step;
    private int tripsCompleted = 0;
    private int tripsCompletedTmp = 0;
    private int passengerSteps = 0;
    private int passengerStepsTmp = 0;

    // Grid dimensions
    public int grids;
    public int density;
    public int gridHeight;
    public int gridWidth;

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
        return travelers.remove(person);
    }
    public boolean addTraveler(Person person) {
        //System.out.print(person.toString());
        //travelersInVehicle.add(person);
        return travelers.add(person);
    }

    /** Constructor default */
    public AgentCity(long seed) {
        this(seed,
             DEFAULT_NUM_GRIDS,
             DEFAULT_VEHICLE_DENSITY,
             DEFAULT_LANE_USE_POLICY,
             "default.json",
             DEFAULT_TRIP_GEN_RATE,
             DEFAULT_HOV_MIN);
    }

    /** Constructor */
    public AgentCity(long seed, int grids, int density, boolean lanePolicy,
                     String outputFileName,
                     Double tripGenRate,
                     int hovMin) {
        // Required by SimState
        super(seed);
        this.seed = seed;
        isTest = true;
        this.grids = grids;
        this.density = density;
        this.LANE_POLICY = lanePolicy;
        this.filename = outputFileName;
        this.TRIPGEN_RATE = tripGenRate;
        this.HOV_MIN = hovMin;
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
        if (agentGrid == null) { return ""; }
        int trips = printAverageTrips();
        int steps = printAverageSteps();
        int stepsPerTrip = (trips > 0 ? steps/trips : 0);
        int stepsWithPassenger = 0;
        int stepsWithoutPassenger = 0;
        int stepsTravelingWithPassenger = 0;
        int stepsTravelingWithoutPassenger = 0;
        int distWithPassenger = 0;
        int distWithoutPassenger = 0;
        Bag vehicles = agentGrid.allObjects;
        for (int i = 0; i < vehicles.numObjs; i++) {
            stepsWithPassenger += ((Vehicle)vehicles.objs[i]).stepsWithPassenger;
            stepsWithoutPassenger += ((Vehicle)vehicles.objs[i]).stepsWithoutPassenger;
            stepsTravelingWithPassenger += ((Vehicle)vehicles.objs[i]).stepsTravelingWithPassenger;
            stepsTravelingWithoutPassenger += ((Vehicle)vehicles.objs[i]).stepsTravelingWithoutPassenger;
            distWithPassenger += ((Vehicle)vehicles.objs[i]).distWithPassenger;
            distWithoutPassenger += ((Vehicle)vehicles.objs[i]).distWithoutPassenger;
        }
        StringBuilder s = new StringBuilder();
        s.append("{")
            .append("\"step\": " + step)
            .append(", ")
            .append("\"timeMins\": " + step * SECONDS_PER_STEP / 60)
            .append(", ")
            .append("\"numTravelers\": " + (travelers==null ? "null" : travelers.numObjs))
            .append(", ");

        s.append("\"averageTrips\": " + trips)
            .append(", ")
            .append("\"averageSteps\": " + steps)
            .append(", ")
            .append("\"averageStepsPerTrip\": " + stepsPerTrip)
            .append(", ")
            .append("\"tripsCompleted\": " + tripsCompleted)
            .append(", ")
            .append("\"passengerSteps\": " + passengerSteps)
            .append(", ")
            .append("\"stepsWithPassenger\": " + stepsWithPassenger)
            .append(", ")
            .append("\"stepsWithoutPassenger\": " + stepsWithoutPassenger)
            .append(", ")
            .append("\"stepsTravelingWithPassenger\": " + stepsTravelingWithPassenger)
            .append(", ")
            .append("\"stepsTravelingWithoutPassenger\": " + stepsTravelingWithoutPassenger)
            .append(", ")
            .append("\"distWithPassenger\": " + distWithPassenger)
            .append(", ")
            .append("\"distWithoutPassenger\": " + distWithoutPassenger)
            .append(", ")
            .append("\"lastTripStep\": " + lastTripStep)
            .append(", ")
            .append("\"lanePolicy\": " + LANE_POLICY)
            .append(", ")
            .append("\"numVehicles\": " + numVehicles)
            .append(", ")
            .append("\"grids\": " + grids)
            .append(", ")
            .append("\"density\": " + density)
            .append(", ")
            .append("\"tripGenRate\": " + TRIPGEN_RATE)
            .append(", ")
            .append("\"hovMin\": " + HOV_MIN)
            .append(", ")
            .append("\"seed\": " + seed)
            .append("},\n");

        return s.toString();
    }

    private int printAverageTrips() {
        int trips = tripsCompleted - tripsCompletedTmp;
        tripsCompletedTmp = tripsCompleted;
        return trips;
    }

    private int printAverageSteps() {
        int steps = passengerSteps - passengerStepsTmp;
        passengerStepsTmp = passengerSteps;
        return steps;
    }

    public void reportTrip(Person person) {
        tripsCompleted++;
        lastTripStep = step;
        passengerSteps += person.getStepsTraveling();
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
                    if (step % REPORT_INTERVAL == 0) {
                        String reportString = ac.toString();
                        if (CONSOLE_OUT) {
                            System.out.print(reportString);
                        }
                        if (FILE_OUT && step > 0) {
                            fileout.println(reportString);
                            //fileout.close();
                        }
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

        if (CHECK_FOR_COLLISIONS) {
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
        numVehicles = NUM_VEHICLES;
        gridHeight = n * 38 + 2;
        gridWidth = gridHeight;

        int numIntersections = 0;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.NONE.toInt());
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

        // make some intersections
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
                new TripGenerator(i, intersections[i], TRIPGEN_RATE,
                                  new MersenneTwisterFast(seed));
            gen.stopper =
                schedule.scheduleRepeating(gen, TRIPGEN_SCHEDULE_NUM, 1);
        }

        // make some vehicles
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
            // add passenger if appropriate
            if (PASSENGER_WARM_START && random.nextFloat() < WARM_START_RATE) {
                Intersection destination =
                    intersections[random.nextInt(intersections.length - 1) + 1];
                Person newPerson = new Person(i, null, destination, newVehicle);
                addTraveler(newPerson);
                // add passenger to schedule
                newPerson.stopper = schedule.scheduleRepeating(newPerson, TRIPGEN_SCHEDULE_NUM, 1);
            }
        }
    }

    /** Main */
    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        //long seed = 1324367672;
        Random random = new Random(seed);
        int numRuns = 64;
        int numMins = 60;
        int stepLimit = numMins * 60 + 1;
        SimState state;
        int grids = 4;
        int density;
        int minDensity = 128;
        int maxDensity = 256;
        // Double tripGenRate;
        Double tripGenRate;
        Double minRate = 0.02;
        Double maxRate = 0.20;
        int hovMin;


        /*
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.systemDefault());
        String dateTimeString = formatter.format(Instant.now());
        String filename = String.format("%s-%d.json",
                                        dateTimeString,
                                        seed);
        */
        String filename = String.format("%drand.json", grids);

        for (int i = 0; i < numRuns; i++) {
            //density = random.nextInt(maxDensity - minDensity) + minDensity;
            density = 144;
            //tripGenRate = random.nextDouble() * (maxRate - minRate) + minRate;
            //hovMin = random.nextInt(4 - 1) + 1;
            tripGenRate = 0.2;
            state = new AgentCity(seed, grids, density, true, filename,
                                  tripGenRate, 1);
            state.start();
            for (int j = 0; j < stepLimit; j++) {
                state.schedule.step(state);
            }
            ((AgentCity)state).fileout.close();
            state.kill();
            state = new AgentCity(seed, grids, density, true, filename,
                                  tripGenRate, 2);
            state.start();
            for (int j = 0; j < stepLimit; j++) {
                state.schedule.step(state);
            }
            ((AgentCity)state).fileout.close();
            state.kill();
            state = new AgentCity(seed, grids, density, false, filename,
                                  tripGenRate, 1);
            state.start();
            for (int j = 0; j < stepLimit; j++) {
                state.schedule.step(state);
            }
            ((AgentCity)state).fileout.close();
            state.kill();
            seed++;
        }
        System.exit(0);
    }
}
