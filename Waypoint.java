/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;

public class Waypoint {
    public Int2D cell;
    public Driver.Directive directive;

    public Waypoint(Int2D cell, Driver.Directive directive) {
        this.cell = cell;
        this.directive = directive;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append(cell)
            .append(directive)
            .toString();
    }
}
