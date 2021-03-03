/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

public class Intersection {

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

    // Variables
    private IntersectionAgent controller;
    private Int2D[] approachLegs;
    private int[] approachLegLength;
    private Int2D[] departureLegs;

    // Accessors
    public Int2D[] getDepartureLegs() { return departureLegs; }
    public Int2D[] getApproachLegs() { return approachLegs; }
    public int getNumApproachLegs(AgentCity ac, Direction direction) {
        Int2D cell;
        int num = 0;
        for (int i = 0; i < approachLegs.length; i++) {
            cell = approachLegs[i];
            if (ac.roadGrid.get(cell.x, cell.y) == direction.toInt()) num++;
        }
        return num;
    }
    public void setController(IntersectionAgent controller) {
        this.controller = controller;
    }
    public IntersectionAgent getController() { return controller; }
    public int getNumCells() {
        return (maxX - minX + 1) * (maxY - minY + 1);
    }

    public Int2D getOrigin() { return new Int2D(minX, minY); }

    public Direction[] getDirectionsTo(Intersection other) {
        Direction[] directions = new Direction[2];
        int xOffset = other.minX - this.minX;
        int yOffset = other.minY - this.minY;
        if (xOffset < 0) {
            directions[0] = Direction.WEST;
        } else if (xOffset > 0) {
            directions[0] = Direction.EAST;
        }
        if (yOffset < 0) {
            directions[1] = Direction.NORTH;
        } else if (yOffset > 0) {
            directions[1] = Direction.SOUTH;
        }
        return directions;
    }

    public boolean legBlocked(Int2D leg) {
        return controller.legBlocked(leg);
    }

    public boolean requestReservation(Vehicle vehicle, long time, Int2D[][] path) {
        return controller.requestReservation(vehicle, time, path);
    }
    public boolean cancelReservation(Vehicle vehicle, boolean complete) {
        return controller.cancelReservation(vehicle, complete);
    }

    /** Constructor */
    public Intersection(int id, int minX, int maxX, int minY, int maxY, AgentCity ac) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        setLegs(ac);
    }

    /** Constructor */
    public Intersection(int id, int minX, int maxX, int minY, int maxY) {
        idNum = id;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean inIntersection(int x, int y) {
        return x <= maxX
            && x >= minX
            && y <= maxY
            && y >= minY;
    }
    public boolean inIntersection(Int2D cell) {
        return inIntersection(cell.x, cell.y);
    }

    private void setLegs(AgentCity ac) {
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        Int2D[] tmpDep = new Int2D[2 * width + 2 * height];
        Int2D[] tmpApp = new Int2D[2 * width + 2 * height];
        int index = 0;
        // look around intersection for approach and departure legs
        if (minY - 1 >= 0) {
            for (int x = minX; x <= maxX; x++) {
                if (ac.roadGrid.field[x][minY - 1]
                        == Direction.SOUTH.toInt()) {
                    tmpApp[index] = new Int2D(x, minY - 1);
                } else if (ac.roadGrid.field[x][minY - 1]
                        == Direction.NORTH.toInt()) {
                    tmpDep[index] = new Int2D(x, minY - 1);
                }
                index++;
            }
        }
        if (maxY + 1 < ac.gridHeight) {
            for (int x = minX; x <= maxX; x++) {
                if (ac.roadGrid.field[x][maxY + 1]
                        == Direction.NORTH.toInt()) {
                    tmpApp[index] = new Int2D(x, maxY + 1);
                } else if (ac.roadGrid.field[x][maxY + 1]
                        == Direction.SOUTH.toInt()) {
                    tmpDep[index] = new Int2D(x, maxY + 1);
                }
                index++;
            }
        }
        if (minX - 1 >= 0) {
            for (int y = minY; y <= maxY; y++) {
                if (ac.roadGrid.field[minX - 1][y]
                        == Direction.EAST.toInt()) {
                    tmpApp[index] = new Int2D(minX - 1, y);
                } else if (ac.roadGrid.field[minX - 1][y]
                        == Direction.WEST.toInt()) {
                    tmpDep[index] = new Int2D(minX - 1, y);
                }
                index++;
            }
        }
        if (maxX + 1 < ac.gridHeight) {
            for (int y = minY; y <= maxY; y++) {
                if (ac.roadGrid.field[maxX + 1][y]
                        == Direction.WEST.toInt()) {
                    tmpApp[index] = new Int2D(maxX + 1, y);
                } else if (ac.roadGrid.field[maxX + 1][y]
                        == Direction.EAST.toInt()) {
                    tmpDep[index] = new Int2D(maxX + 1, y);
                }
                index++;
            }
        }

        // Count approach and departure legs
        int numApp = 0;
        int numDep = 0;
        for (int i = 0; i < tmpApp.length; i++) {
            if (tmpApp[i] != null) numApp++;
            if (tmpDep[i] != null) numDep++;
        }

        // Populate arrays with legs
        approachLegs = new Int2D[numApp];
        departureLegs = new Int2D[numDep];
        index = 0;
        for (int i = 0; i < tmpApp.length; i++) {
            if (tmpApp[i] != null) {
                approachLegs[index] = tmpApp[i];
                index++;
            }
        }
        index = 0;
        for (int i = 0; i < tmpDep.length; i++) {
            if (tmpDep[i] != null) {
                departureLegs[index] = tmpDep[i];
                index++;
            }
        }

        // Count cells on legs
        approachLegLength = new int[approachLegs.length];
        for (int i = 0; i < approachLegLength.length; i++) {
            approachLegLength[i] = getLegLength(ac, approachLegs[i], true);
        }
    }

    private int getLegLength(AgentCity ac, Int2D leg, boolean isApproachLeg) {
        int count = 0;
        Direction dir = Direction.byInt(ac.roadGrid.get(leg.x, leg.y));
        Int2D cell = leg;
        Direction cellDir = dir;
        Direction countDir = (isApproachLeg ? dir.opposite(): dir);
        while (cellDir == dir) {
            cell = ac.getCellAhead(cell, countDir, 1);
            cellDir = Direction.byInt(ac.roadGrid.get(cell.x, cell.y));
            count++;
        }
        return count;
    }

    public Int2D[] getDepartureLegsByDirection(AgentCity ac, Direction dir) {
        int numLegs = 0; // number of relevant departure legs
        Int2D[] legs; // array to return
        // Count relevant departure legs
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y)
                == dir.toInt()) {
                numLegs++;
            }
        }
        // Fill new array with relevant departure legs
        legs = new Int2D[numLegs];
        int nextIndex = 0;
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y)
                == dir.toInt()) {
                legs[nextIndex] = departureLegs[i];
                nextIndex++;
            }
        }
        return legs;
    }

    // Get all departure legs execpt Direction dir
    public Int2D[] getDepartureLegs(AgentCity ac, Direction dir) {
        int numLegs = 0; // number of relevant departure legs
        Int2D[] legs; // array to return
        // Count relevant departure legs
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y)
                    != dir.opposite().toInt()) {
                numLegs++;
            }
        }
        // Fill new array with relevant departure legs
        legs = new Int2D[numLegs];
        int nextIndex = 0;
        for (int i = 0; i < departureLegs.length; i++) {
            if (ac.roadGrid.get(departureLegs[i].x, departureLegs[i].y)
                    != dir.opposite().toInt()) {
                legs[nextIndex] = departureLegs[i];
                nextIndex++;
            }
        }
        return legs;
    }

    public Int2D getDepartureLeg(AgentCity ac, Direction dir,
                                   Direction laneDir) {
        Int2D[] departureLegs = getDepartureLegsByDirection(ac, dir);
        int[] departureInt = new int[departureLegs.length];
        int nextDepartureIdx = 0;
        int xOffset = laneDir.getXOffset();
        int yOffset = laneDir.getYOffset();
        for (int i = 0; i < departureLegs.length; i++) {
            departureInt[i] =
                departureLegs[i].x * xOffset + departureLegs[i].y * yOffset;
        }
        for (int j = 1; j < departureInt.length; j++) {
            if (departureInt[j] > departureInt[nextDepartureIdx]) {
                nextDepartureIdx = j;
            }
        }
        return departureLegs[nextDepartureIdx];
    }
}
