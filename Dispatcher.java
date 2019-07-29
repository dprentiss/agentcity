/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;

public interface Dispatcher {
    int requestVehicle(Intersection location,
                       Intersection destination,
                       Person person,
                       long step);
}
