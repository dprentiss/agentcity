/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class Person implements Steppable, VehicleClient {

    // MASON
    private static final long serialVersionUID = 1;
    Stoppable stopper;
    long step;

    // Properties
    public final int idNum;

    // Variables
    private AgentCity ac;

    private Intersection origin = null;
    private Vehicle vehicle = null;
    private Intersection destination = null;
    private boolean inVehicle = false;
    private boolean atDestination = false;
    private int stepsAlive = 0;
    private int stepsWaiting = 0;
    private int stepsTraveling = 0;
    private long lastStep = -1;
    private long firstStep = -1;

    // Accessors
    public Intersection getOrigin() { return origin; }
    public Vehicle getVehicle() { return vehicle; }
    public Intersection getDestination() { return destination; }
    public boolean inVehicle() { return inVehicle; }
    public boolean atDestination() {
        atDestination =
            ac.intersectionGrid
            .field[vehicle.getLocation().x][vehicle.getLocation().y]
            == destination.idNum;
        return(atDestination);
    }

    /** Constructor */
    public Person(int id,
                  Intersection origin, Intersection destination) {
        idNum = id;
        this.origin = origin;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("{\"PersonAgent\": {")
            .append("\"idNum\": " + idNum)
            .append(", ")
            .append("\"originIdNum\": " + origin.idNum)
            .append(", ")
            .append("\"destinationIdNum\": " + destination.idNum)
            .append(", ")
            .append("\"inVehicle\": " + inVehicle)
            .append(", ")
            .append("\"atDestination\": " + atDestination)
            .append(", ")
            .append("\"stepsAlive\": " + stepsAlive)
            .append(", ")
            .append("\"stepsWaiting\": " + stepsWaiting)
            .append(", ")
            .append("\"stepsTraveling\": " + stepsTraveling)
            .append(", ")
            .append("\"firstStep\": " + firstStep)
            .append(", ")
            .append("\"lastStep\": " + lastStep)
            .append("}},\n")
            .toString();
    }

    private boolean boardVehicle(Vehicle v) {
        if (v.boardVehicle(this)) {
            this.vehicle = v;
            inVehicle = true;
            return true;
        } else {
            return false;
        }
    }

    private boolean exitVehicle() {
        if (vehicle.exitVehicle(this)) {
            this.vehicle = null;
            inVehicle = false;
            return true;
        } else {
            return false;
        }
    }

    public void step(final SimState state) {
        // get Simulation state
        ac = (AgentCity)state;
        if (firstStep < 0) firstStep = ac.schedule.getSteps();
        stepsAlive++;

        if (!inVehicle) {
            stepsWaiting++;
            Bag vehicles = origin.getController().getVehicles();
            Vehicle v;
            for (int i = 0; i < vehicles.numObjs; i++) {
                v = (Vehicle)vehicles.objs[i];
                if (boardVehicle(v)) {
                    break;
                }
            }
        } else {
            stepsTraveling++;
            if (atDestination()) {
                if (exitVehicle()) {
                    lastStep = ac.schedule.getSteps();
                }
                if (ac.removeTraveler(this)) {
                    //if (ac.CONSOLE_OUT) { System.out.print(this); }
                    if (ac.FILE_OUT) { ac.fileout.print(this); }
                    this.stopper.stop();
                }
            }
        }
    }
}
