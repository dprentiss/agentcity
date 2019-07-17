/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;

public interface Dispatcher {
    int requestVehicle(Intersection intersection, long step);
}
