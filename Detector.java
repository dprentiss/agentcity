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
    public final Int2D cell;
    public final int INTERVAL;
    public final int numLanes;
    public final Direction direction;
    public final String orientation;
    public final int maxSpeed;
    public final int[] laneIdx;

    // Variables
    AgentCity ac;
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
    private Int2D[][] cells;

    private int[] vehicleCount;
    private int[] previousCount;
    private int[] previousSpeed;
    private long[] previousTime;
    private int densityCount;
    private int distanceHeadway;
    private int[] distanceHeadways;
    private double[] flow;
    private double[] density;
    private double totalFlow;
    private double totalDensity;

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("{\"Detector\": {")
            .append("\"idNum\": " + idNum)
            .append(", ")
            //.append("\"cell\": " + cell)
            //.append(", ")
            .append("\"orientation\": \"" + orientation + "\"")
            .append(", ")
            .append("\"step\": " + step)
            .append(", ")
            .append("\"timeMins\": " + step * AgentCity.SECONDS_PER_STEP / 60)
            .append(", ")
            .append(String.format("\"totalFlow\": %.0f", totalFlow))
            .append(", ")
            .append(String.format("\"totalDensity\": %.2f", totalDensity))
            .append(", ")
            .append("\"lanes\": [")
            .append("\n");
        for (int i = 0; i < numLanes; i++) {
            s.append("\t{")
                .append("\"laneNum\": " + i)
                .append(", ")
                //.append("\"cell\": " + cells[i][0])
                //.append(", ")
                .append(String.format("\"flow\": %.0f", flow[i]))
                .append(", ")
                .append(String.format("\"density\": %.2f", density[i]))
                .append("}");
            if (i < numLanes - 1) {
                s.append(", \n");
            } else {
                s.append("]");
            }
        }
        s.append("}},\n");

        return s.toString();
    }

    /** Constructor */
    public Detector(AgentCity ac, int id, Int2D cell, int numLanes, int
                    interval) {
        idNum = id;
        this.cell = cell;
        INTERVAL = interval;
        this.numLanes = numLanes;
        maxSpeed = AgentCity.MAX_SPEED;
        laneIdx = new int[numLanes];
        cells = new Int2D[numLanes][maxSpeed];
        vehicleCount = new int[numLanes];
        previousCount = new int[numLanes];
        previousSpeed = new int[numLanes];
        previousTime = new long[numLanes];
        densityCount = 0;
        distanceHeadway = 1;
        distanceHeadways = new int[numLanes];
        flow = new double[numLanes];
        density = new double[numLanes];
        direction = Direction.byInt(ac.roadGrid.get(cell.x, cell.y));
        if (direction == Direction.EAST || direction == Direction.WEST) {
            orientation = "x";
        } else {
            orientation = "y";
        }

        int x = 0;
        int y = 0;

        for (int i = 0; i < numLanes; i++) {
            laneIdx[i] = cell.y
                + i * direction.onLeft().getXOffset()
                + i * direction.onLeft().getYOffset();
            vehicleCount[i] = 0;
            previousCount[i] = 0;
            previousSpeed[i] = 0;
            previousTime[i] = 0;
            distanceHeadways[i] = 1;
            flow[i] = 0;
            density[i] = 0;
            for (int j = 0; j < maxSpeed; j++) {
                x = cell.x + i * direction.onLeft().getXOffset()
                    + j * direction.getXOffset();
                y = cell.y + i * direction.onLeft().getYOffset()
                    + j * direction.getYOffset();
                cells[i][j] = new Int2D(x, y);
            }
        }
    }

    /** Constructor */
    public Detector(AgentCity ac, int id, Int2D cell, int numLanes) {
        this(ac, id, cell, numLanes, 600);
    }

    public void step(final SimState state) {
        ac = (AgentCity)state;
        step = ac.schedule.getSteps();


        for (int i = 0; i < numLanes; i++) {
            densityCount = 0;
            for (int j = 0; j < maxSpeed; j++) {
                // get Vehicle at cell
                b = ac.agentGrid.getObjectsAtLocation(cells[i][j]);
                if (b == null) { continue; } // skip empty cells
                densityCount += 1;
                vehicle = (Vehicle)b.objs[0];
                buffer.add(vehicle); // add Vehicle to buffer
                if (vehicles.contains(vehicle)) continue;
                // if Vehicle not already counted
                vehicles.add(vehicle);
                vehicleCount[i]++;
                distanceHeadway =
                    previousSpeed[i] * (int)(step - previousTime[i]);
                distanceHeadways[i] += distanceHeadway;
                previousTime[i] = step;
                previousSpeed[i] = vehicle.getSpeed();
            }
            density[i]
                += densityCount / 5.0 / INTERVAL / ac.METERS_PER_CELL * 1000;
        }
        int j = 0;
        while (j < vehicles.numObjs) {
            if (buffer.contains(vehicles.objs[j])) {
                j++;
            } else {
                vehicles.remove(vehicles.objs[j]);
            }
        }
        buffer.clear();

        totalDensity = 0;
        totalFlow = 0;
        if ((int)step % INTERVAL == 0) {
            for (int i = 0; i < numLanes; i++) {
                flow[i] =
                    (double)(vehicleCount[i] - previousCount[i])
                    / INTERVAL * 3600;
                /*
                density[i] =
                    (double)(vehicleCount[i] - previousCount[i])
                    / distanceHeadways[i] / 7.5 * 1000;
                */
                totalFlow += flow[i];
                totalDensity += density[i];
                previousCount[i] = vehicleCount[i];
                distanceHeadways[i] = 0;
            }
            if (ac.CONSOLE_OUT) {
                System.out.print(this);
            }
            if (ac.FILE_OUT) {
                ac.fileout.print(this);
            }
            for (int i = 0; i < numLanes; i++) {
                density[i] = 0;
            }
        }
    }
}
