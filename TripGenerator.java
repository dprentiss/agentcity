/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.util.distribution.*;
import sim.engine.*;
import ec.util.MersenneTwisterFast;
import java.util.Arrays;

/**
 * Generates Person objects at an Intersection with a Poisson distribution.
 */

public class TripGenerator implements Steppable {

    // MASON
    private static final long serialVersionUID = 1;
    Stoppable stopper;
    long step;

    // Properties
    public final int idNum;
    public final Intersection intersection;
    public final Poisson distribution;

    // Variables
    private final double mean;

    // Accessors

    /*
    @Override
    public String toString() {
        return new StringBuilder()
            .append("DriverAgent: {")
            .append("idNum: " + idNum)
            .append(", ")
            .append("desiredSpeed: " + desiredSpeed)
            .append(", ")
            //.append("destination: " + destination)
            //.append(", ")
            .append("hasReservation: " + hasReservation)
            .append(", ")
            .append("nextApproachLeg: " + nextApproachLeg)
            .append(", ")
            .append("nextTurnCell: " + nextTurnCell)
            .append(", ")
            .append("nextLeg: " + nextLeg)
            .append(", ")
            .append("nextDirection: " + nextDirection)
            .append(", ")
            //.append("nearIntersection: " + nearIntersection)
            //.append(", ")
            .append("nearApproachLeg: " + nearApproachLeg)
            .append(", ")
            .append("atApproachLeg: " + atApproachLeg)
            .append(", ")
            .append("nearIntersection: " + nearIntersection)
            .append(", ")
            .append("inIntersection: " + inIntersection)
            .append(", ")
            .append("stepsToWaypoint: " + stepsToWaypoint)
            .append(", ")
            .append("nearWaypoint: " + nearWaypoint)
            .append(", ")
            //.append("nearNextLeg: " + nearNextLeg)
            //.append(", ")
            .append("atNextLeg: " + atNextLeg)
            .append("}\n")
            .toString();
    }
    */

    /** Constructor
     *
     * @param id (required) int label for this class. Should be unique but
     * uniqueness is not checked.
     */
    public TripGenerator(int id, Intersection intersection, double mean,
                         MersenneTwisterFast randomGenerator) {
        idNum = id;
        this.intersection = intersection;
        this.mean = mean;
        distribution = new Poisson(mean, randomGenerator);
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;
        step = ac.schedule.getSteps();
        int numPersons = distribution.nextInt();
        Person newPerson;
        for (int i = numPersons; i > 0; i--) {
            Intersection destination =
                ac.intersections[ac.random.nextInt(ac.intersections.length - 1) + 1];
            while (destination == intersection) {
                destination =
                    ac.intersections[ac.random.nextInt(ac.intersections.length - 1) + 1];
            }
            int newId = (int)step % 100 * 10000 + 100 * intersection.idNum + destination.idNum;
            newPerson = new Person(newId, intersection, destination);
            ac.travelers.add(newPerson);
            //System.out.println(ac.travelers.numObjs);
            newPerson.stopper = ac.schedule.scheduleRepeating(newPerson, 6, 1);
        }
    }
}
