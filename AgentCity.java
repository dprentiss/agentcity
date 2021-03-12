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
    public static final int LANE_SCHEDULE_NUM = 3;
    public static final int DRIVER_SCHEDULE_NUM = 4;
    public static final int COLLISION_SCHEDULE_NUM = 5;
    public static final int TRIPGEN_SCHEDULE_NUM = 6;
    public static final int REPORT_SCHEDULE_NUM = 7;

    // Simulation constants
    public final long seed;
    public final double TRIPGEN_RATE;
    public final boolean LANE_POLICY;
    public final boolean RESERVATION_PRIORITY;
    public final int HOV_MIN;
    public static final boolean SMART_TURNS = true;
    public static final boolean AVOID_CONGESTION = true;
    public static final boolean PASSENGER_WARM_START = false;
    public static final double WARM_START_RATE = 0.5;
    public static final boolean CONSOLE_OUT = true;
    public static final boolean FILE_OUT = true;
    public static final int MAX_SPEED = 2;
    public static final int REPORT_INTERVAL = 600;
    public static final double SECONDS_PER_STEP = 1;
    public static final double METERS_PER_CELL = 7.5;
    public static final int NUM_BLOCKED_LEG_CELLS = 4;
    public static final int MIN_INTERSECTION_CONTROL_SIZE = 0;
    public static final int PASSENGER_CAP = 4;
    public static final long LAST_TRIP_LIMIT = 600;

    // Utility
    private static final int DEFAULT_HOV_MIN = 2;
    private static final boolean DEFAULT_LANE_USE_POLICY = false;
    private static final boolean DEFAULT_RESERVATION_PRIORITY = true;
    private static final double DEFAULT_TRIP_GEN_RATE = 0.2;
    private static final int DEFAULT_VEHICLE_DENSITY = 144;
    private static final int DEFAULT_NUM_GRIDS = 4;
    private final boolean CHECK_FOR_COLLISIONS = true;
    private final boolean isTest;
    private final String filename;
    private int numRoadCells;
    private FileWriter fw;
    private BufferedWriter bw;
    public PrintWriter fileout;

    // Variables
    private long lastTripStep = 0;
    private int numVehicles;
    private long step;
    private int tripsCompleted = 0;
    private int tripsCompletedTmp = 0;
    private int passengerSteps = 0;
    private int passengerStepsTmp = 0;
    private int passengerDist = 0;
    private int passengerDistTmp = 0;

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
    public IntGrid2D laneGrid;
    public IntGrid2D blockGrid;

    // Array of Intersections
    public Intersection[] intersections;
    // Array of Intersection agents
    public IntersectionAgent[] intersectionAgents;
    // Array of Lanes
    public Lane[] lanes;
    // Array of Lane agents
    public LaneAgent[] laneAgents;

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
             DEFAULT_RESERVATION_PRIORITY,
             "default.json",
             DEFAULT_TRIP_GEN_RATE,
             DEFAULT_HOV_MIN);
    }

    /** Constructor */
    public AgentCity(long seed, int grids, int density, boolean lanePolicy,
                     boolean reservationPriority,
                     String outputFileName,
                     Double tripGenRate,
                     int hovMin) {
        // Required by SimState
        super(seed);
        this.seed = seed;

        isTest = true;
        this.grids = grids;
        this.density = density;
        LANE_POLICY = lanePolicy;
        RESERVATION_PRIORITY = reservationPriority;
        filename = outputFileName;
        TRIPGEN_RATE = tripGenRate;
        HOV_MIN = hovMin;
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
        // Travelers
        int trips = getAverageTrips();
        int steps = getAverageSteps();
        int dist = getAverageDist();
        float stepsPerTrip = (trips > 0 ? (float)steps/trips : 0);
        float distPerTrip = (trips > 0 ? (float)dist/trips : 0);
        // Vehicles
        int[] stepsMoving = new int[PASSENGER_CAP+1];
        int[] stepsWaiting = new int[PASSENGER_CAP+1];
        int[] distance = new int[PASSENGER_CAP+1];
        // Vehicles
        int[] reservationsCanceled = new int[PASSENGER_CAP+1];
        int[] reservationsCompleted = new int[PASSENGER_CAP+1];
        int scheduleInvalid = 0;
        int stepsWithPassenger = 0;
        int stepsWithoutPassenger = 0;
        int stepsTravelingWithPassenger = 0;
        int stepsTravelingWithoutPassenger = 0;
        int distWithPassenger = 0;
        int distWithoutPassenger = 0;
        Bag vehicles = agentGrid.allObjects;
        // Vehicle reporting
        for (int i = 0; i < vehicles.numObjs; i++) {
            Vehicle v = ((Vehicle)vehicles.objs[i]);
            for (int k = 0; k < v.stepsMoving.length; k++) {
                stepsMoving[k] += v.stepsMoving[k];
            }
            for (int k = 0; k < v.stepsWaiting.length; k++) {
                stepsWaiting[k] += v.stepsWaiting[k];
            }
            for (int k = 0; k < v.distance.length; k++) {
                distance[k] += v.distance[k];
            }
            stepsWithPassenger += v.stepsWithPassenger;
            stepsWithoutPassenger += v.stepsWithoutPassenger;
            stepsTravelingWithPassenger += v.stepsTravelingWithPassenger;
            stepsTravelingWithoutPassenger += v.stepsTravelingWithoutPassenger;
            distWithPassenger += v.distWithPassenger;
            distWithoutPassenger += v.distWithoutPassenger;
        }
        // IntersectionAgent reporting
        for (int i = 1; i < intersectionAgents.length; i++) {
            IntersectionAgent agent = intersectionAgents[i];
            for (int k = 0; k < agent.reservationsCanceled.length; k++) {
                reservationsCanceled[k] += agent.reservationsCanceled[k];
            }
            for (int k = 0; k < agent.reservationsCompleted.length; k++) {
                reservationsCompleted[k] += agent.reservationsCompleted[k];
            }
            scheduleInvalid += agent.scheduleInvalid;
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
            .append("\"averageDist\": " + dist)
            .append(", ")
            .append("\"averageDistPerTrip\": " + distPerTrip)
            .append(", ")
            .append("\"tripsCompleted\": " + tripsCompleted)
            .append(", ")
            .append("\"passengerSteps\": " + passengerSteps)
            .append(", ")
            .append("\"stepsMoving\": " + Arrays.toString(stepsMoving))
            .append(", ")
            .append("\"stepsWaiting\": " + Arrays.toString(stepsWaiting))
            .append(", ")
            .append("\"distanceTraveled\": " + Arrays.toString(distance))
            .append(", ")
            .append("\"reservationsCanceled\": " + Arrays.toString(reservationsCanceled))
            .append(", ")
            .append("\"reservationsCompleted\": " + Arrays.toString(reservationsCompleted))
            .append(", ")
            .append("\"scheduleInvalid\": " + scheduleInvalid)
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
            .append("\"reservationPriority\": " + RESERVATION_PRIORITY)
            .append(", ")
            .append("\"lanePolicy\": " + LANE_POLICY)
            .append(", ")
            .append("\"numVehicles\": " + numVehicles)
            .append(", ")
            .append("\"numRoadCells\": " + numRoadCells)
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

    private int getAverageTrips() {
        int trips = tripsCompleted - tripsCompletedTmp;
        tripsCompletedTmp = tripsCompleted;
        return trips;
    }

    private int getAverageSteps() {
        int steps = passengerSteps - passengerStepsTmp;
        passengerStepsTmp = passengerSteps;
        return steps;
    }

    private int getAverageDist() {
        int dist = passengerDist - passengerDistTmp;
        passengerDistTmp = passengerDist;
        return dist;
    }

    public void reportTrip(Person person) {
        tripsCompleted++;
        lastTripStep = step;
        passengerSteps += person.getStepsTraveling();
        passengerDist += person.getDist();
    }

    public void start() {
        super.start();
        travelers = new Bag();

        if(isTest) {
            makeTestGrids();
        }

        //System.out.println(filename);

        Steppable report = new Steppable() {
                public void step(final SimState state) {
                    AgentCity ac = (AgentCity)state;
                    step = schedule.getSteps();
                    if (step % REPORT_INTERVAL == 0) {
                        String reportString = ac.toString();
                        if (CONSOLE_OUT) {
                            System.out.println(reportString);
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

    private int labelLanes(int cellX, int cellY, int num) {
        int label = 0;
        Direction dir = Direction.byInt(roadGrid.field[cellX][cellY]);
        int x = dir.getXOffset();
        int y = dir.getYOffset();
        for (int i = -1; i < 2; i++) {
            if (!checkBounds(cellX + x * i, cellY + y * i)) {
                continue;
            }
            label = laneGrid.field[cellX + x * i][cellY + y * i];
            if (label != 0) {
                laneGrid.field[cellX][cellY] = label;
                return 0;
            }
        }
        laneGrid.field[cellX][cellY] = num + 1;
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
        int numLanes = 0;

        roadGrid = new IntGrid2D(gridWidth, gridHeight, Direction.NONE.toInt());
        intersectionGrid = new IntGrid2D(gridWidth, gridHeight, 0);
        laneGrid = new IntGrid2D(gridWidth, gridHeight, 0);
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
                    else {
                        roadGrid.field[x][y] = Direction.SOUTH.toInt();
                        numRoadCells++;
                    }
                    blockGrid.field[x][y] = 0;
                }
                if (x == 1 || (x-2)%38 == 18 || (x-2)%38 == 19 || (x-2)%38 == 37) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else {
                        roadGrid.field[x][y] = Direction.NORTH.toInt();
                        numRoadCells++;
                    }
                    blockGrid.field[x][y] = 0;
                }
                if (y == 0 || (y-2)%38 == 16 || (y-2)%38 == 17 || (y-2)%38 == 36) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else {
                        roadGrid.field[x][y] = Direction.WEST.toInt();
                        numRoadCells++;
                    }
                    blockGrid.field[x][y] = 0;
                }
                if (y == 1 || (y-2)%38 == 18 || (y-2)%38 == 19 || (y-2)%38 == 37) {
                    if (roadGrid.field[x][y] != 0) {
                        numIntersections +=
                            labelIntersection(x, y, numIntersections);
                        roadGrid.field[x][y] = Direction.ALL.toInt();
                    }
                    else {
                        roadGrid.field[x][y] = Direction.EAST.toInt();
                        numRoadCells++;
                    }
                    blockGrid.field[x][y] = 0;
                }
            }
        }
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (roadGrid.field[x][y] != 0 && roadGrid.field[x][y] != 9) {
                    numLanes += labelLanes(x, y, numLanes);
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
            intersectionAgents[i].setPriority(RESERVATION_PRIORITY);
            intersectionAgents[i].stopper =
                schedule.scheduleRepeating(intersectionAgents[i],
                                           INTERSECTION_SCHEDULE_NUM, 1);
            TripGenerator gen =
                new TripGenerator(i, intersections[i], TRIPGEN_RATE,
                                  new MersenneTwisterFast(seed));
            gen.stopper =
                schedule.scheduleRepeating(gen, TRIPGEN_SCHEDULE_NUM, 1);
        }

        // make some lanes
        maxXs = new int[numLanes + 1];
        minXs = new int[numLanes + 1];
        maxYs = new int[numLanes + 1];
        minYs = new int[numLanes + 1];
        Arrays.fill(minXs, gridWidth);
        Arrays.fill(minYs, gridHeight);
        lanes = new Lane[numLanes + 1];
        laneAgents = new LaneAgent[numLanes + 1];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                inter = laneGrid.field[x][y];
                if (inter != 0) {
                    maxXs[inter] = (x > maxXs[inter]) ? x : maxXs[inter];
                    minXs[inter] = (x < minXs[inter]) ? x : minXs[inter];
                    maxYs[inter] = (y > maxYs[inter]) ? y : maxYs[inter];
                    minYs[inter] = (y < minYs[inter]) ? y : minYs[inter];
                }
            }
        }
        for (int i = 1; i < numLanes + 1; i++) {
            lanes[i] =
                new Lane(i, minXs[i], maxXs[i], minYs[i], maxYs[i], this);
            //laneAgents[i] = new LaneAgent(i, lanes[i]);
            //laneAgents[i].stopper =
            //schedule.scheduleRepeating(laneAgents[i],
            //                             LANE_SCHEDULE_NUM, 1);
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

    Int2D getCellAhead(int cellX, int cellY, Direction dir, int offset) {
        return new Int2D(cellX + offset * dir.getXOffset(),
                         cellY + offset * dir.getYOffset());
    }

    Int2D getCellAhead(Int2D cell, Direction dir, int offset) {
        return getCellAhead(cell.x, cell.y, dir, offset);
    }

    interface ScenarioRunner {
        void run(SimState state, long stepLimit);
    }

    /** Main */
    public static void main(String[] args) {

        ScenarioRunner scenarioLoop = (SimState state, long stepLimit) -> {
            state.start();
            for (int i = 0; i < stepLimit; i++) {
                state.schedule.step(state);
                if (i - ((AgentCity)state).lastTripStep >= LAST_TRIP_LIMIT) {
                    break;
                }
            }
            ((AgentCity)state).fileout.close();
            state.kill();
        };

        SimState[]  states;
        long seed = System.currentTimeMillis();
        //long seed = 1324367672;
        Random random = new Random(seed);
        int numRuns = 1;
        int numMins = 60;
        int stepLimit = numMins * 60 + 1;
        //SimState state;
        int grids = 4;
        int density;
        int minDensity = 64;
        int maxDensity = 256;
        // Double tripGenRate;
        Double tripGenRate;
        Double minRate = 0.02;
        Double maxRate = 0.30;
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
        //String filename = String.format("%drand.json", grids);
        String filename = "default.json";

        for (int i = 0; i < numRuns; i++) {
            density = random.nextInt(maxDensity - minDensity) + minDensity;
            //density = 144;
            tripGenRate = random.nextDouble() * (maxRate - minRate) + minRate;
            //tripGenRate = 0.2;
            hovMin = random.nextInt(4 - 1) + 1;
            //hovMin = 3

            states = new SimState[3];
            states[0] = new AgentCity(seed, grids, density, true, true, filename,
                                  tripGenRate, hovMin);
            states[1] = new AgentCity(seed, grids, density, false, true, filename,
                                  tripGenRate, hovMin);
            states[2] = new AgentCity(seed, grids, density, false, false, filename,
                                  tripGenRate, 1);

            for (int j = 0; j < states.length; j++) {
                System.out.format("Run %d of %d, Scenario %d of %d: density = %d, tripGenRate = %f.3"
                                  + " hovMin = %d%n%n",
                                  i+1, numRuns, j+1, states.length, density, tripGenRate, hovMin);

                scenarioLoop.run(states[j], stepLimit);
            }
        }
        System.exit(0);
    }
}
