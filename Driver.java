/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;

public interface Driver {
    enum Directive {
        MOVE_FORWARD,
        STOP,
        TURN_RIGHT,
        TURN_LEFT,
        MERGE_RIGHT,
        MERGE_LEFT,
        NONE
    }

    Directive getNextDirective();
    int getDesiredSpeed();
}
