/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import sim.util.*;
import sim.engine.*;
import java.util.Arrays;

/**
 * Generic agent for controlling {@link Vehicle} objects.
 */

public class DriverAgent implements Steppable, Driver {

    // Required for serialization
    private static final long serialVersionUID = 1;
    Stoppable stopper;

    // Properties
    public final int idNum;
    public Intersection nextIntersection;
    public Int2D nextLeg;
    public Int2D nextApproachLeg;
    public Int2D nextTurnCell;
    public Direction nextDirection;

    // Variables
    public Vehicle vehicle = null;
    public Driver.Directive nextDirective = Driver.Directive.NONE;
    public int desiredSpeed;
    public int maxSpeed;
    public Int2D destination = null;
    public boolean nearIntersection = false;
    public boolean nearApproachLeg = false;
    public boolean atApproachLeg = false;
    public boolean hasReservation = false;
    public boolean inIntersection = false;
    public boolean nearTurnCell = false;
    public boolean nearNextLeg = false;
    public boolean atNextLeg = false;
    public Int2D[] path;
    public Int2D location;
    public Direction direction;
    public int speed;

    // Accessors
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle v) {
        vehicle = v;
        maxSpeed = v.MAX_SPEED;
    }
    public Driver.Directive getNextDirective() { return nextDirective; }
    public int getDesiredSpeed() { return desiredSpeed; }

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
            //.append("hasReservation: " + hasReservation)
            //.append(", ")
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
            .append("inIntersection: " + inIntersection)
            .append(", ")
            .append("nearTurnCell: " + nearTurnCell)
            .append(", ")
            //.append("nearNextLeg: " + nearNextLeg)
            //.append(", ")
            //.append("atNextLeg: " + atNextLeg)
            .append("}\n")
            .toString();
    }

    /** Constructor
     *
     * @param id (required) int label for this class. Should be unique but 
     * uniqueness is not checked.
     */
    public DriverAgent(int id) {
        idNum = id;
    }

    boolean pathAheadClear(AgentCity ac, Int2D loc, Direction dir, int speed) {
        int x;
        int y;
        boolean isRoad = true;
        boolean isFree = true;
        boolean hasRightOfWay = true;
        Driver.Directive directive;
        //743508623

        switch (speed) {
        case 0:
            // one cell ahead
            x = loc.x + dir.getXOffset();
            y = loc.y + dir.getYOffset();
            if (ac.checkBounds(x,y)) {
                isRoad = ac.roadGrid.get(x, y) != 0;
                Bag b = ac.agentGrid.getObjectsAtLocation(x, y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getSpeed() != 0;
                }
                if (!isRoad || !isFree) { return false; }
            } else {
                return false;
            }
            // two cells ahead
            x = loc.x + 2 * dir.getXOffset();
            y = loc.y + 2 * dir.getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.opposite();
                }
                hasRightOfWay = hasReservation;
                if (!isFree && !hasRightOfWay) { return false; }
            }
            //  one cell ahead and one cell right
            x = loc.x + dir.getXOffset() + dir.onRight().getXOffset();
            y = loc.y + dir.getYOffset() + dir.onRight().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.onLeft();
                    hasRightOfWay =
                        v.getSpeed() == 0 && v.idNum < vehicle.idNum;
                }
                hasRightOfWay = hasRightOfWay || hasReservation;
                if (!isFree && !hasRightOfWay) {
                    return false;
                }
            }
            //  one cell ahead and one cell left
            x = loc.x + dir.getXOffset() + dir.onLeft().getXOffset();
            y = loc.y + dir.getYOffset() + dir.onLeft().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.onRight();
                    hasRightOfWay = 
                        v.getSpeed() == 0 && v.idNum < vehicle.idNum;
                }
                hasRightOfWay = hasRightOfWay || hasReservation;
                if (!isFree && !hasRightOfWay) {
                    return false;
                }
            }
            break;
        case 1:
            // one cell ahead
            x = loc.x + dir.getXOffset();
            y = loc.y + dir.getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir;
                    hasRightOfWay = v.hasReservation;
                }
                hasRightOfWay = hasRightOfWay && hasReservation;
                if (!isFree && !hasRightOfWay) { return false; }
            }
            // two cells ahead
            x = loc.x + 2 * dir.getXOffset();
            y = loc.y + 2 * dir.getYOffset();
            if (ac.checkBounds(x, y)) {
                isRoad = ac.roadGrid.get(x, y) != 0;
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getSpeed() != 0;
                    hasRightOfWay = v.hasReservation;
                }
                hasRightOfWay = hasRightOfWay && hasReservation;
                if (!isRoad || (!isFree && !hasRightOfWay)) {
                    return false;
                }
            } else {
                return false;
            }
            // three cells ahead
            x = loc.x + 3 * dir.getXOffset();
            y = loc.y + 3 * dir.getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.opposite();
                }
                hasRightOfWay = hasReservation;
                if (!isFree && !hasRightOfWay) { return false; }
            }
            //  two cells ahead and one cell right
            x = loc.x + 2 * dir.getXOffset() + dir.onRight().getXOffset();
            y = loc.y + 2 * dir.getYOffset() + dir.onRight().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    directive = v.getDriver().getNextDirective();
                    isFree = v.getDirection() != dir.onLeft()
                        || (v.getSpeed() == 0
                            && directive != Directive.MOVE_FORWARD)
                        || (v.getSpeed() == 1
                            && directive != Directive.STOP);
                    if (vehicle.idNum == 163) {
                        System.out.print(this.toString());
                        System.out.print(((DriverAgent)v.getDriver()).toString());
                    }

                }
                if (!isFree) { return false; }
            }
            //  two cells ahead and one cell left
            x = loc.x + 2 * dir.getXOffset() + dir.onLeft().getXOffset();
            y = loc.y + 2 * dir.getYOffset() + dir.onLeft().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.onRight();
                }
                hasRightOfWay = hasReservation;
                if (!isFree && !hasRightOfWay) { return false; }
            }
            //  two cells ahead and two cells right
            x = loc.x + 2 * dir.getXOffset() + 2 * dir.onRight().getXOffset();
            y = loc.y + 2 * dir.getYOffset() + 2 * dir.onRight().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.onLeft()
                        || v.getSpeed() == 0;
                }
                hasRightOfWay = hasReservation;
                if (!isFree && !hasRightOfWay) {
                    return false;
                }
            }
            //  two cells ahead and two cells left
            x = loc.x + 2 * dir.getXOffset() + 2 * dir.onLeft().getXOffset();
            y = loc.y + 2 * dir.getYOffset() + 2 * dir.onLeft().getYOffset();
            if (ac.checkBounds(x, y)) {
                Bag b = ac.agentGrid.getObjectsAtLocation(x,y);
                if (b != null) {
                    Vehicle v = (Vehicle)b.objs[0];
                    isFree = v.getDirection() != dir.onRight() 
                        || v.getSpeed() == 0;
                }
                hasRightOfWay = hasReservation;
                if (!isFree && !hasRightOfWay) {
                    return false;
                }
            }
            break;
        }
        return true;
    }

    /**
     * Gets the intersection ahead of the {@link Vehicle}.
     *
     * @param ac the current state of the simulation.
     * @param loc currrent location of the {@link Vehicle}.
     *
     * @return the intersection ahead of the {@link Vehicle}.
     */
    Intersection getIntersectionAhead(AgentCity ac, Int2D loc) {
        int cellX = loc.x;
        int cellY = loc.y;
        Direction cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        while (cellDirection != Direction.ALL) {
            cellX += cellDirection.getXOffset();
            cellY += cellDirection.getYOffset();
            cellDirection = Direction.byInt(ac.roadGrid.get(cellX, cellY));
        }
        return ac.intersections[ac.intersectionGrid.get(cellX, cellY)];
    }

    Int2D getRandomDepartureLeg(AgentCity ac, Intersection in, Direction dir) {
        Int2D[] departureLegs = in.getDepartureLegs(ac, dir);
        return departureLegs[ac.random.nextInt(departureLegs.length)];
    }

    Int2D getNextApproachLeg(AgentCity ac, Intersection in, Int2D loc, Direction dir) {
        int cellX = loc.x;
        int cellY = loc.y;
        int nextX = cellX + dir.getXOffset();
        int nextY = cellY + dir.getYOffset();
        Direction cellDirection = Direction.byInt(ac.roadGrid.field[nextX][nextY]);
        while (cellDirection != Direction.ALL) {
            cellX += dir.getXOffset();
            cellY += dir.getYOffset();
            nextX = cellX + dir.getXOffset();
            nextY = cellY + dir.getYOffset();
            cellDirection = Direction.byInt(ac.roadGrid.field[nextX][nextY]);
        }
        return new Int2D(cellX, cellY);
        /*
          Int2D[] legs = in.getApproachLegs();
          Direction legDir;
          for (int i = 0; i < legs.length; i++) {
          legDir = Direction.byInt(ac.roadGrid.field[legs[i].x][legs[i].y]);
          if (dir == legDir && (loc.x == legs[i].x || loc.y == legs[i].y)) {
          return legs[i];
          }
          }
          return null;
        */
    }

    Int2D setTurnCell(AgentCity ac, Int2D leg, Int2D loc, Direction locDir) {
        int cellX;
        int cellY;
        Direction legDir = Direction.byInt(ac.roadGrid.field[leg.x][leg.y]);
        if (legDir == locDir) {
            cellX = Math.abs(locDir.getXOffset()) * leg.x 
                + Math.abs(legDir.getYOffset()) * loc.x;
            cellY = Math.abs(locDir.getYOffset()) * leg.y
                + Math.abs(legDir.getXOffset()) * loc.y;
            nextLeg = new Int2D(cellX, cellY);
        } else {
            cellX = Math.abs(locDir.getXOffset()) * leg.x 
                + Math.abs(legDir.getXOffset()) * loc.x;
            cellY = Math.abs(locDir.getYOffset()) * leg.y
                + Math.abs(legDir.getYOffset()) * loc.y;
        }
        return new Int2D(cellX, cellY);
    }

    Int2D[] getPathToCell(Int2D cell,
                          Int2D loc,
                          Direction dir,
                          int currentSpeed,
                          int desiredSpeed,
                          boolean stopAtCell) {

        final int TMP_LEN = 16;
        Int2D[] tmp = new Int2D[TMP_LEN];
        int speed = currentSpeed;
        int x = loc.x;
        int y = loc.y;
        int i = 1;
        Int2D[] path;

        tmp[0] = loc;
        int dist = getGapToCell(cell, loc, dir) + 1;
        if (dist < 0) {
            throw new IllegalArgumentException("Cell is not ahead of Vehicle.");
        }
        while (dist > 0) {
            if (speed < desiredSpeed) speed++;
            speed = (dist < speed && stopAtCell) ? dist : speed;
            tmp[i] = getCellAhead(x, y, dir, speed);
            x = tmp[i].x;
            y = tmp[i].y;
            dist -= speed;
            i++;
        }
        path = new Int2D[i];
        for (int j = 0; j < path.length; j++) {
            path[j] = tmp[j];
        }
        return path;
    }

    Int2D[] getPathToCell(Int2D cell) {
        return getPathToCell(cell,
                             location,
                             direction,
                             speed,
                             maxSpeed,
                             true);
    }

    Int2D[] getPathToCell(Int2D cell, boolean stopAtCell) {
        return getPathToCell(cell,
                             location,
                             direction,
                             speed,
                             maxSpeed,
                             stopAtCell);
    }

    Int2D[] getPath(AgentCity ac, Int2D loc, Direction dir) {
        Int2D[] tmpPath = new Int2D[16];
        Int2D[] returnPath;
        Int2D tmpCell = new Int2D();
        int speed = 2;
        int desiredSpeed = 2;
        int i = 0;
        // start at current location
        int cellX = loc.x;
        int cellY = loc.y;
        // advance to intersection
        while (ac.roadGrid.field[cellX][cellY] != 9) {
            tmpCell = getCellAhead(cellX, cellY, dir, 1);
            cellX = tmpCell.x;
            cellY = tmpCell.y;
        }
        // advance to turn cell and add cells to path
        while (cellX != nextTurnCell.x || cellY != nextTurnCell.y) {
            tmpPath[i] = new Int2D(cellX, cellY);
            i++;
            tmpCell = getCellAhead(cellX, cellY, dir, 1);
            cellX = tmpCell.x;
            cellY = tmpCell.y;
        }
        // add turning time to path if turning
        if (dir != nextDirection) {
            tmpPath[i] = new Int2D(cellX, cellY);
            i++;
        }
        // advance out of intersection and add cells to path
        while (ac.roadGrid.field[cellX][cellY] == 9) {
            tmpPath[i] = new Int2D(cellX, cellY);
            i++;
            tmpCell = getCellAhead(cellX, cellY, nextDirection, 1);
            cellX = tmpCell.x;
            cellY = tmpCell.y;
        }
        returnPath = new Int2D[i];
        for (int j = 0; j < returnPath.length; j++) {
            returnPath[j] = tmpPath[j];
        }
        return returnPath;
    }

    Int2D[] getUpdatedPath(Int2D location) {
        int i = 0;
        Int2D[] updatedPath;
        while (i < path.length && !location.equals(path[i])) {
            path[i] = null;
            i++;
        }
        updatedPath = new Int2D[path.length - i];
        for (int j = 0; j < updatedPath.length; j++) {
            updatedPath[j] = path[i + j];
        }
        return updatedPath;
    }

    Int2D getCellAhead(int cellX, int cellY, Direction dir, int offset) {
        return new Int2D(cellX + offset * dir.getXOffset(),
                         cellY + offset * dir.getYOffset());
    }

    Int2D getCellAhead(Int2D cell, Direction dir, int offset) {
        return getCellAhead(cell.x, cell.y, dir, offset);
    }

    int getGapToCell(Int2D cell, Int2D loc, Direction dir) {
        int x = cell.x - loc.x;
        int y = cell.y - loc.y;
        /*
        if (x != 0 && y != 0) {
            throw new IllegalArgumentException("Cell is not ahead of Vehicle.");
        }
        */
        return x * dir.getXOffset() + y * dir.getYOffset() - 1;
    }

    int getGapToCell(Int2D cell) {
        return getGapToCell(cell, location, direction);
    }

    int getStepsToCell(int gap, int currentSpeed, int desiredSpeed) {
        int dist = gap + 1;
        int steps = 0;
        if (dist < 0) {
            //throw new IllegalArgumentException("Cell is not ahead of Vehicle.");
        } else if (dist == 0) {
            steps = 0;
        } else if (currentSpeed >= desiredSpeed) {
            steps = dist / desiredSpeed + dist % desiredSpeed;
        } else if (dist + 1 <= currentSpeed) {
            steps = 1;
        } else {
            steps = steps
                + getStepsToCell(dist - currentSpeed,
                                 currentSpeed + 1,
                                 desiredSpeed);
        }
        return steps;
    }

    int getStepsToCell(Int2D cell) {
        return getStepsToCell(getGapToCell(cell), speed, maxSpeed);
    }

    public void step(final SimState state) {
        AgentCity ac = (AgentCity)state;

        // Current Vehicle position and velocity;
        location = vehicle.getLocation(ac);
        direction = vehicle.getDirection();
        speed = vehicle.getSpeed();
        hasReservation = vehicle.hasReservation;

        // get a new destination if needed
        if (nextIntersection == null) {
            nextIntersection = getIntersectionAhead(ac, location);
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            nextApproachLeg = getNextApproachLeg(ac, nextIntersection, location, direction);
            nextTurnCell = setTurnCell(ac, nextLeg, location, direction);
            nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
        }

        // check if Vehicle is near enough to an intersection to request a
        // reservation
        nearIntersection =
            location.x + 2 * direction.getXOffset()
            == nextApproachLeg.x
            && location.y + 2 * direction.getYOffset()
            == nextApproachLeg.y;
        nearApproachLeg = getStepsToCell(nextApproachLeg) == 1;
        // check if Vehicle is at approach leg
        atApproachLeg = getStepsToCell(nextApproachLeg) < 1;
        // check if Vehicle is in intersection
        inIntersection = ac.roadGrid.field[location.x][location.y] == 9;
        nearTurnCell = getStepsToCell(nextTurnCell) == 1;

        // check if Vehicle is one cell before destination
        nearNextLeg =
            location.x + direction.getXOffset()
            == nextLeg.x
            && location.y + direction.getYOffset()
            == nextLeg.y;
        // check if Vehicle is at destination
        atNextLeg = getStepsToCell(nextLeg) < 1;

        // get a new destination if needed
        if (atNextLeg) {
            nextIntersection = getIntersectionAhead(ac, location);
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            nextApproachLeg = getNextApproachLeg(ac, nextIntersection, location, direction);
            nextTurnCell = setTurnCell(ac, nextLeg, location, direction);
            nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
            vehicle.hasReservation = false;
            hasReservation = false;
        }

        // Default state is move forward at max speed
        desiredSpeed = maxSpeed;
        nextDirective = Driver.Directive.MOVE_FORWARD;

        // request a reservation if needed
        /*
          if (inIntersection && !hasReservation) {
          hasReservation = nextIntersection.requestReservation(
          vehicle, ac.schedule.getSteps() + 1 + speed,
          getUpdatedPath(location));
          vehicle.hasReservation = hasReservation;
          } else if (speed == 1 && nearIntersection) {
          path = getPath(ac, location, direction);
          hasReservation = nextIntersection.requestReservation(
          vehicle, ac.schedule.getSteps() + 3,
          getPath(ac, location, direction));
          vehicle.hasReservation = hasReservation;
          } else if (speed == 0 && atApproachLeg) {
          path = getPath(ac, location, direction);
          hasReservation = nextIntersection.requestReservation(
          vehicle, ac.schedule.getSteps() + 2,
          getPath(ac, location, direction));
          vehicle.hasReservation = hasReservation;
          }
        */

        // If if near cell
        if (nearTurnCell) {
            desiredSpeed = getGapToCell(nextTurnCell) + 1;
            // ...get direction to turn or go straight then...
            if (nextDirection == direction.onRight()) {
                // ...turn right or
                nextDirective = Driver.Directive.TURN_RIGHT;
            } else if (nextDirection == direction.onLeft()) {
                // ...turn left or
                nextDirective = Driver.Directive.TURN_LEFT;
            } else if (nextDirection == direction) {
                // ...go straight or
                nextDirective = Driver.Directive.MOVE_FORWARD;
            } else {
                // ...report there was a problem and stop.
                System.out.printf("Vehicle %d at (%d, %d) had a problem turning.\n",
                                  vehicle.idNum, location.x, location.y);
                System.out.println(nextIntersection.idNum);
                System.out.println(nextLeg);
                System.out.println(nextDirection);
                desiredSpeed = 0;
                nextDirective = Driver.Directive.STOP;
            }
        }

        // check if Vehicle needs and has a reservation for its next turning
        // movement
        /*
        if (nearApproachLeg && !hasReservation) {
            desiredSpeed = getGapToCell(nextApproachLeg) + 1;
            nextDirective = Driver.Directive.STOP;
        }
        if (atApproachLeg && !hasReservation) {
            desiredSpeed = 0;
            nextDirective = Driver.Directive.STOP;
        }
        */

        // If the directive is move forward and the way is not clear, stop.
        if (!pathAheadClear(ac, location, direction, speed)
            && nextDirective == Driver.Directive.MOVE_FORWARD) {
            desiredSpeed = 0;
            nextDirective = Driver.Directive.STOP;
        }
        System.out.print(this.toString());
        System.out.println(Arrays.toString(getPathToCell(nextTurnCell)));
        System.out.print(this.vehicle.toString());
    }
}
