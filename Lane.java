/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

public class Lane {

    // Required for serialization
    private static final long serialVersionUID = 1;

    // Stopper
    Stoppable stopper;

    // Properties
    public final int idNum;
    public final int minX;
    public final int maxX;
    public final int minY;
    public final int maxY;
    public final int height;
    public final int width;
    public final int length;
    public Int2D[] cells;

    // Variables
    private LaneAgent controller;

    // Accessors
    @Override
    public String toString() {
        return new StringBuilder()
            .append("Lane: {")
            .append("idNum: " + idNum)
            .append(", ")
            .append("length: " + length)
            .append(", ")
            .append("height: " + height)
            .append(", ")
            .append("width: " + width)
            .append(", ")
            .append("cells: " + Arrays.toString(cells))
            .append("}\n")
            .toString();
    }

    public void setController(LaneAgent controller) {
        this.controller = controller;
    }
    public LaneAgent getController() { return controller; }

    /*
    public boolean requestReservation(Vehicle vehicle, long time, Int2D[][] path) {
        return controller.requestReservation(vehicle, time, path);
    }
    public boolean cancelReservation(Vehicle vehicle, boolean complete) {
        return controller.cancelReservation(vehicle, complete);
    }
    */

    public Int2D[] getCells() { return cells; }

    /** Constructor */
    public Lane(int id, int minX, int maxX, int minY, int maxY, AgentCity ac) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        height = maxY - minY + 1;
        width = maxX - minX + 1;
        length = height + width - 1;
        setCells(ac);
    }

    private void setCells(AgentCity ac) {
        cells = new Int2D[length];
        final int x = (width == 1 ? 0 : 1);
        final int y = (height == 1 ? 0 : 1);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Int2D(minX + i * x, minY + i * y);
        }
    }
}
