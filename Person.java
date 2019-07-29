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
    private Intersection location = null;
    private Vehicle vehicle = null;
    private Intersection destination = null;
    private boolean inVehicle = false;
    private boolean atDestination = false;

    // Accessors
    public Intersection getLocation() { return location; }
    public Vehicle getVehicle() { return vehicle; }
    public Intersection getDestination() { return destination; }

    /** Constructor */
    public Person(int id,
                  Intersection location, Intersection destination) {
        idNum = id;
        this.location = location;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("PersonAgent: {")
            .append("idNum: " + idNum)
            .append(", ")
            .append("location: " + location.idNum)
            .append(", ")
            .append("destination: " + destination.idNum)
            .append("}\n")
            .toString();
    }

    public void step(final SimState state) {
        // get Simulation state
        AgentCity ac = (AgentCity)state;
        if (!inVehicle) {
            Bag vehicles = location.getController().getVehicles();
            for (int i = 0; i < vehicles.numObjs; i++) {
                if (!((Vehicle)vehicles.objs[i]).hasPassengers) {
                    if (((Vehicle)vehicles.objs[i]).boardVehicle(this) > 0) {
                        vehicle = (Vehicle)vehicles.objs[i];
                        inVehicle = true;
                        System.out.println("Boarded:");
                        System.out.print(this);
                        System.out.print(vehicle);
                        break;
                    }
                }
            }
        } else {
            if (ac.intersectionGrid
                .field[vehicle.getLocation().x][vehicle.getLocation().y]
                == destination.idNum) {
                vehicle.exitVehicle(this);
                System.out.println("Arrived:");
                System.out.print(this);
                System.out.print(vehicle);
                this.stopper.stop();
            }
        }
    }
}
