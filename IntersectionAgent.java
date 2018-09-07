/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;

public class IntersectionAgent implements Steppable {

    // Required for serialization
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Properties
    public final int idNum;

    // Variables

    // Accessors

    /** Constructor */
    public IntersectionAgent(int id) {
        idNum = id;
    }

    public void step(final SimState state) {
        // World state
        AgentCity ac = (AgentCity)state;
    }
}
