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

    private Int2D[] cells;

    // Variables
    private LaneAgent controller;
    private Direction direction;
    private int hovMin;
    private Int2D minNeighborCell;
    private Int2D maxNeighborCell;
    private Object minNeighbor;
    private Object maxNeighbor;

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
            .append(", ")
            .append("minNeighborCell: " + minNeighborCell)
            .append(", ")
            .append("maxNeighborCell: " + maxNeighborCell)
            .append(", ")
            .append("minNeighbor: " + minNeighbor.toString())
            .append(", ")
            .append("maxNeighbor: " + maxNeighbor.toString())
            .append("}\n")
            .toString();
    }

    public int getHovMin() { return hovMin; }

    public void setHovMin(int hovMin) {
        this.hovMin = hovMin;
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

    public Bag getVehicles(AgentCity ac) {
        return null;
    }

    public int countVehicles(AgentCity ac) {
        int numVehicles = 0;
        for (int i = 0; i < cells.length; i++) {
            if (ac.agentGrid.numObjectsAtLocation(cells[i]) > 0) {
                numVehicles++;
            }
        }
        return numVehicles;
    }

    public int countPassengers(AgentCity ac) {
        int numPassengers = 0;
        Bag b;
        Vehicle v;
        for (int i = 0; i < cells.length; i++) {
            b = ac.agentGrid.getObjectsAtLocation(cells[i]);
            if (b != null) {
                v = (Vehicle)b.objs[0];
                numPassengers += v.getNumPassengers();
            }
        }
        return numPassengers;
    }

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
        hovMin = ac.HOV_MIN;
        setCells(ac);
        //System.out.print(this.toString());
    }

    private void setCells(AgentCity ac) {
        cells = new Int2D[length];
        final int x = (width == 1 ? 0 : 1);
        final int y = (height == 1 ? 0 : 1);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Int2D(minX + i * x, minY + i * y);
        }
        minNeighborCell = new Int2D(cells[0].x - 1 * x, cells[0].y - 1 * y);
        if (!ac.checkBounds(minNeighborCell)) {
            String s = String.format("Cell s% is out of bounds",
                                     minNeighborCell);
            throw new IllegalArgumentException(s);
        }
        maxNeighborCell = new Int2D(cells[length-1].x + 1 * x,
                                    cells[length-1].y + 1 * y);
        if (!ac.checkBounds(maxNeighborCell)) {
            String s = String.format("Cell s% is out of bounds",
                                     maxNeighborCell);
            throw new IllegalArgumentException(s);
        }
    }

    public boolean setNeighbors(AgentCity ac) {
        if (minNeighborCell == null || maxNeighborCell == null) {
            return false;}
        int intersectionID =
            ac.intersectionGrid.field[minNeighborCell.x][minNeighborCell.y];
        int laneID =
            ac.laneGrid.field[minNeighborCell.x][minNeighborCell.y];
        minNeighbor =
            (laneID == 0 ? (Intersection)ac.intersections[intersectionID]
             : (Lane)ac.lanes[laneID]);
        intersectionID =
            ac.intersectionGrid.field[maxNeighborCell.x][maxNeighborCell.y];
        laneID =
            ac.laneGrid.field[maxNeighborCell.x][maxNeighborCell.y];
        maxNeighbor =
            (laneID == 0 ? (Intersection)ac.intersections[intersectionID]
             : (Lane)ac.lanes[laneID]);
        return true;
    }

    private void setController(LaneAgent controller) {
        this.controller = controller;
    }
}
