package sim.app.agentcity;

public interface Driveable {

    enum Directive {
        MOVE_FORWARD,
        STOP,
        MERGE_RIGHT,
        MERGE_LEFT,
        TURN_RIGHT,
        TURN_LEFT
    }
}
