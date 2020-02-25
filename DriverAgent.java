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

    // MASON
    private static final long serialVersionUID = 1;
    Stoppable stopper;
    long step;

    // Properties
    public final int idNum;

    // Variables
    private AgentCity ac;

    public Vehicle vehicle = null;
    public Int2D location;
    public Direction direction;
    public int speed;
    public int maxSpeed;

    public Driver.Directive nextDirective = Driver.Directive.NONE;
    public int desiredSpeed = maxSpeed;
    public int maxSafeSpeed = maxSpeed;

    public Int2D[][] reservationPath;
    public long reservationTime;

    public Waypoint[] waypoints;
    public Waypoint nextWaypoint;
    public boolean atWaypoint = false;
    public boolean nearWaypoint = false;
    public int stepsToWaypoint;

    public Int2D destination = null;
    public Intersection nextIntersection;
    public Int2D nextLeg;
    public Int2D nextApproachLeg;
    public Int2D nextTurnCell;
    public Direction nextDirection;

    public boolean nearIntersection = false;
    public boolean nearApproachLeg = false;
    public boolean atApproachLeg = false;
    public boolean hasReservation = false;
    public boolean inIntersection = false;
    public boolean nearNextLeg = false;
    public boolean atNextLeg = false;
     public boolean laneOnLeft = false;
    public boolean laneOnRight = false;

    public boolean hasAssignment = false;

    // Accessors
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle v) {
        vehicle = v;
        maxSpeed = v.MAX_SPEED;
    }
    public Driver.Directive getNextDirective() { return nextDirective; }
    public int getDesiredSpeed() { return desiredSpeed; }
    public int assignVehicle(Person person,
                             Intersection pickUp,
                             Intersection dropOff) {
        return -1;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("DriverAgent: {")
            .append("idNum: " + idNum)
            .append(", ")
            .append("desiredSpeed: " + desiredSpeed)
            .append(", ")
            .append("maxSafeSpeed: " + maxSafeSpeed)
            .append(", ")
            //.append("destination: " + destination)
            //.append(", ")
            .append("hasReservation: " + hasReservation)
            .append(", ")
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
            .append("nearIntersection: " + nearIntersection)
            .append(", ")
            .append("inIntersection: " + inIntersection)
            .append(", ")
            .append("stepsToWaypoint: " + stepsToWaypoint)
            .append(", ")
            .append("nearWaypoint: " + nearWaypoint)
            .append(", ")
            //.append("nearNextLeg: " + nearNextLeg)
            //.append(", ")
            .append("atNextLeg: " + atNextLeg)
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

    /**
     * Gets the get the maximum safe speed for the {@link Vehicle} on the next
     * step.
     *
     * @param ac the current state of the simulation.
     * @param loc currrent location of {@link Vehicle}.
     * @param dir currrent location of {@link Vehicle}.
     *
     * @return the maximum safe speed.
     */
    int getSafeSpeed(AgentCity ac, int locX, int locY, Direction dir,
                     int depth) {

        Int2D cell;

        int MAX_DEPTH = 16;
        if (depth > MAX_DEPTH) {return 0;}

        // for each cell ahead of vehicle, check for obstacles and boundaries.
        for (int i = 0; i < maxSpeed; i++) { // Check cells out to maxSpeed
            cell = getCellAhead(locX, locY, dir, i + 1);
            // check if cell is on grid
            if (!ac.checkBounds(cell.x, cell.y)) { return i; }
            // check if cell is a road cell
            if (ac.roadGrid.get(cell.x, cell.y) == 0) { return i; }
            // get any vehicles on cell
            Bag b = ac.agentGrid.getObjectsAtLocation(cell.x, cell.y);
            if (b != null) { // if there is another vehicle at the cell
                // get the other vehicle
                Vehicle otherVehicle = (Vehicle)b.objs[0];
                // check for gridlock
                if (otherVehicle.equals(this.getVehicle())) {
                    return 1;
                }
                // check if the other vehicle has a reservation
                if (!otherVehicle.hasReservation ||
                    ac.roadGrid.get(cell.x, cell.y) != 9) { return i; }
                // get more information about the other vehicle
                Direction otherDirection = otherVehicle.getDirection();
                int otherSafeSpeed = getSafeSpeed(ac, cell.x, cell.y,
                                                  otherDirection, depth+1);
                // check if other vehicle can move
                if (otherSafeSpeed < 1) { return i; }
                // check if other vehicle is traveling in the same direction
                if (otherDirection == dir) {
                    // get more information about the other vehicle
                    int otherSpeed = otherVehicle.getSpeed();
                    // check if the other vehicle is slow
                    if (i + otherSpeed + 1 < maxSpeed) {
                        return i + otherSpeed + 1;
                    }
                    // check if other vehicle will stop for an obstacle
                    if (i + otherSafeSpeed < maxSpeed) {
                        return i + otherSafeSpeed;
                    }
                    // the path is clear up to maxSpeed
                    break;
                }
            }
        }
        // return maxSpeed since no obstacles or boundaries found
        return maxSpeed;
    }

    int getSafeSpeed(AgentCity ac) {
        return getSafeSpeed(ac, this.location.x, this.location.y,
                            this.direction, 0);
    }

    int getSafeSpeed(AgentCity ac, Int2D loc, Direction dir) {
        return getSafeSpeed(ac, loc.x, loc.y, dir, 4);
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

    /*
      Int2D getNextDepartureLeg(AgentCity ac, Intersection in, Direction dir,
      Direction laneDir) {
      int xOffset = laneDir.getXOffset();
      int yOffset = laneDir.getYOffset();
      Int2D[] departureLegs = in.getDepartureLegs(ac, dir);
      Int2D nextDepartureLeg = departureLegs[0];
      for (int i = 1; i < departureLegs.length; i++) {
      if (offset > 0) {
      int idx = nextDepartureLeg.x * xOffset +
      } else {
      }
      }
      return nextDepartureLeg;
      }
    */

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
        int i = 0;
        Int2D[] path;

        int dist = getGapToCell(cell, loc, dir) + 1;
        if (dist < 0) {
            String errorString = new StringBuilder()
                .append(String.format("Cell %s is not ahead of cell %s headed %s.\n",
                                      cell, loc, dir))
                .append(this.vehicle.toString())
                .append(this.toString())
                .append(Arrays.toString(waypoints))
                .append(vehicle.toString())
                .toString();
            throw new IllegalArgumentException(errorString);
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
        // trim tmp and copy to path
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
        return getPathToCell(cell, location, direction, speed, maxSpeed,
                             stopAtCell);
    }

    Int2D[] getPathToWaypoint(Waypoint waypoint,
                              Int2D loc,
                              Direction dir,
                              int currentSpeed,
                              int desiredSpeed) {

        boolean stopAtCell =
            waypoint.directive != Driver.Directive.MOVE_FORWARD;

        return getPathToCell(waypoint.cell, loc, dir, currentSpeed,
                             desiredSpeed, stopAtCell);
    }

    Int2D[][] getReservationPath(Waypoint[] waypoints) {
        return getReservationPath(waypoints, location, direction,
                                  speed, maxSpeed);
    }

    Int2D[][] getReservationPath(Waypoint[] waypoints, Int2D loc, Direction dir,
                                 int currentSpeed, int desiredSpeed) {
        Int2D[] pathToWaypoint;
        Int2D[][] tmpPath = new Int2D[16][maxSpeed];
        Int2D[][] path;
        Int2D tmpLoc = loc;
        Direction tmpDir = dir;
        int tmpSpeed = currentSpeed;
        int k = 0;
        int dist = 0;

        // loop over waypoints and add to path
        for (int i = 0; i < waypoints.length; i++) {
            if (tmpLoc.equals(waypoints[i].cell)) {
                if (waypoints[i].directive != Driver.Directive.MOVE_FORWARD) {
                    tmpSpeed = 0;
                }
                tmpDir = tmpDir.byDirective(waypoints[i].directive);
                continue;
            }
            pathToWaypoint =
                getPathToWaypoint(waypoints[i], tmpLoc, tmpDir, tmpSpeed,
                                  desiredSpeed).clone();
            for (int j = 0; j < pathToWaypoint.length; j++) {
                dist = getGapToCell(pathToWaypoint[j], tmpLoc, tmpDir) + 1;
                for(int l = dist - 1; l >= 0; l--) {
                    tmpPath[k][l] = getCellAhead(tmpLoc, tmpDir, 1);
                    tmpLoc = tmpPath[k][l];
                }
                if (waypoints[i].directive == Driver.Directive.MOVE_FORWARD) {
                    tmpSpeed = dist;
                } else {
                    tmpSpeed = 0;
                }
                k++;
            }
            tmpDir = tmpDir.byDirective(waypoints[i].directive);
        }
        // copy tmpPath to trimmed path
        int m = 0;
        if (!inIntersection) {
            m = getStepsToCell(getCellAhead(waypoints[0].cell, 1)) - 1;
        }
        path = new Int2D[k - m][maxSpeed];
        for (int i = 0; i < k - m; i++) {
            for (int j = 0; j < maxSpeed; j++) {
                path[i][j] = tmpPath[i + m][j];
            }
        }
        /*
          path = new Int2D[k][maxSpeed];
          for (int i = 0; i < k; i++) {
          for (int j = 0; j < maxSpeed; j++) {
          path[i][j] = tmpPath[i][j];
          }
          }
        */
        return path;
    }

    /*
      Int2D[] getPath(Waypoint[] waypoints, Int2D loc, Direction dir,
      int currentSpeed, int desiredSpeed) {
      Int2D[] path;
      Int2D[] pathToWaypoint;
      Int2D[] tmpPath = new Int2D[16];
      Int2D tmpLoc = loc;
      Direction tmpDir = dir;
      int tmpSpeed = currentSpeed;
      int k = 0;

      // loop over waypoints and add to path
      for (int i = 0; i < waypoints.length; i++) {
      pathToWaypoint =
      getPathToWaypoint(waypoints[i], tmpLoc, tmpDir, tmpSpeed,
      desiredSpeed).clone();
      for (int j = 0; j < pathToWaypoint.length; j++) {
      tmpPath[k] = pathToWaypoint[j];
      if (waypoints[i].directive == Driver.Directive.MOVE_FORWARD) {
      tmpSpeed = getGapToCell(tmpPath[k], tmpLoc, tmpDir) + 1;
      } else {
      tmpSpeed = 0;
      }
      tmpLoc = tmpPath[k];
      k++;
      }
      tmpDir = tmpDir.byDirective(waypoints[i].directive);
      }
      // copy tmpPath to trimmed path
      path = new Int2D[k];
      for (int i = 0; i < k; i++) {
      path[i] = tmpPath[i];
      }
      return path;
      }
    */

    /*
      Int2D[] getPath(Waypoint[] waypoints) {
      return getPath(waypoints, location, direction, speed, maxSpeed);
      }
    */

    /*
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
    */

    /*
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
    */

    Int2D getCellAhead(int cellX, int cellY, Direction dir, int offset) {
        return new Int2D(cellX + offset * dir.getXOffset(),
                         cellY + offset * dir.getYOffset());
    }

    Int2D getCellAhead(Int2D cell, Direction dir, int offset) {
        return getCellAhead(cell.x, cell.y, dir, offset);
    }

    Int2D getCellAhead(Int2D cell, int offset) {
        return getCellAhead(cell.x, cell.y, direction, offset);
    }

    Int2D getCellAhead(int cellX, int cellY, int offset) {
        return getCellAhead(cellX, cellY, this.direction, offset);
    }

    int getGapToCell(Int2D cell, Int2D loc, Direction dir) {
        int x = cell.x - loc.x;
        int y = cell.y - loc.y;
        return x * dir.getXOffset() + y * dir.getYOffset() - 1;
    }

    int getGapToCell(Int2D cell) {
        return getGapToCell(cell, location, direction);
    }

    int getStepsToCell(int gap, int currentSpeed, int desiredSpeed) {
        int dist = gap + 1;
        int steps = 0;
        int nextSpeed = currentSpeed + 1;
        if (nextSpeed > desiredSpeed) nextSpeed = desiredSpeed;
        if (dist <= 0) {
            return steps;
        } else if (currentSpeed >= maxSpeed) {
            steps = dist / maxSpeed + dist % maxSpeed;
        } else if (dist <= currentSpeed - 1 && dist <= desiredSpeed) {
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
        return getStepsToCell(getGapToCell(cell), speed, desiredSpeed);
    }

    Driver.Directive getTurnDirective(Direction dir) {
        if (dir == this.direction) {
            return Driver.Directive.MOVE_FORWARD;
        } else if (dir == this.direction.onRight()) {
            return Driver.Directive.TURN_RIGHT;
        } else if (dir == this.direction.onLeft()) {
            return Driver.Directive.TURN_LEFT;
        }
        return Driver.Directive.STOP;
    }

    void updateReservation() {
        // cancel reservation if reservationTime can't be honored
        if (hasReservation) {
            int timeIndex = (int)(step - reservationTime);
            stepsToWaypoint = getStepsToCell(nextWaypoint.cell);
            /*
              boolean cannotLeave =
              (timeIndex >= 0
              && timeIndex < reservationPath.length
              && !location.equals(reservationPath[timeIndex][0]));
              if (cannotLeave) {
              nextIntersection.cancelReservation(vehicle);
              hasReservation = false;
              vehicle.hasReservation = false;
              }
            */
        }
        // get a reservation if needed
        if (!hasReservation) {
            reservationTime = step + 1;
            if (inIntersection) {
                if (direction == nextDirection) {
                    waypoints = new Waypoint[] {
                        new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
                    };
                } else {
                    waypoints = new Waypoint[] {
                        new Waypoint(nextTurnCell,
                                     getTurnDirective(nextDirection)),
                        new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
                    };
                }
            } else {
                reservationTime = step
                    + getStepsToCell(getCellAhead(nextApproachLeg, 1));
            }
            reservationPath = getReservationPath(waypoints);
            hasReservation =
                nextIntersection.requestReservation(vehicle,
                                                    reservationTime,
                                                    reservationPath);
            vehicle.hasReservation = hasReservation;
        }
    }

    void updateReservation(AgentCity ac) {
        updateState(ac);
        updateReservation();
    }

    void updateState(AgentCity ac) {
        step = ac.schedule.getSteps();
        location = vehicle.getLocation();
        direction = vehicle.getDirection();
        speed = vehicle.getSpeed();
        hasReservation = vehicle.hasReservation;
        inIntersection = ac.roadGrid.field[location.x][location.y] == 9;
    }

    private void checkReservation() {
        // check if Vehicle needs and has a reservation for its next turning
        // movement
        if (!hasReservation) {
            if (nearApproachLeg) {
                desiredSpeed = getGapToCell(nextApproachLeg) + 1;
                nextDirective = Driver.Directive.STOP;
            }
            if (atApproachLeg) {
                desiredSpeed = 0;
                nextDirective = Driver.Directive.STOP;
            }
            if (inIntersection) {
                desiredSpeed = 0;
                nextDirective = Driver.Directive.STOP;
            }
        }
        if (nearApproachLeg) {
            if (nextIntersection.legBlocked(nextLeg)) {
                desiredSpeed = getGapToCell(nextApproachLeg) + 1;
                nextDirective = Driver.Directive.STOP;
                //nextIntersection.cancelReservation(vehicle);
                vehicle.hasReservation = false;
                hasReservation = false;
                if (ac.AVOID_CONGESTION) updateDestination();
            }
        }
        if (atApproachLeg) {
            if (nextIntersection.legBlocked(nextLeg)) {
                desiredSpeed = 0;
                nextDirective = Driver.Directive.STOP;
                //nextIntersection.cancelReservation(vehicle);
                vehicle.hasReservation = false;
                hasReservation = false;
                if (ac.AVOID_CONGESTION) updateDestination();
            }
        }
    }

    public void checkReservation(AgentCity ac) {
        updateState(ac);
        checkReservation();
        // If the directive is move forward and the way is not clear, stop.
        maxSafeSpeed = getSafeSpeed(ac);
        if (maxSafeSpeed < desiredSpeed) {
            desiredSpeed = maxSafeSpeed;
        }

    }

    private void updateDestination() {
        if (nextIntersection != null) {
            nextIntersection.removeApproachVehicle(vehicle);
        }
        nextIntersection = getIntersectionAhead(ac, location);
        nextIntersection.addApproachVehicle(vehicle);

        // chose direction
        if (ac.SMART_TURNS && vehicle.hasPassengers) {
            Intersection destination =
                vehicle.getPassengerDestination();
            if (destination.idNum == nextIntersection.idNum) {
                nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            } else {
                Direction[] directions =
                    nextIntersection.getDirectionsTo(destination);
                Bag legs = new Bag();
                for (int i = 0; i < directions.length; i++) {
                    if (directions[i] != null) {
                        legs.addAll(nextIntersection.getDepartureLegsByDirection(ac, directions[i]));
                    }
                }
                nextLeg = (Int2D)(legs.objs[ac.random.nextInt(legs.numObjs)]);
            }
        } else {
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
        }
        nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);

        // choose departure lane by occupancy policy
        if (ac.LANE_POLICY && nextIntersection.getLanePolicy()) {
            if (vehicle.hasPassengers) {
                /*
                nextLeg =
                    nextIntersection.getDepartureLeg(ac, nextDirection,
                                                     nextDirection.onLeft());
            */
            } else {
                nextLeg =
                    nextIntersection.getDepartureLeg(ac, nextDirection,
                                                     nextDirection.onRight());
            }
        }

        // choose random direction if nextDirection is infeasible
        if (nextDirection == null
            || nextDirection.toInt() == direction.opposite().toInt()) {
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
        }
        // set up next turning movement
        nextApproachLeg = getNextApproachLeg(ac, nextIntersection, location, direction);
        nextTurnCell = setTurnCell(ac, nextLeg, location, direction);
        if (direction == nextDirection) {
            waypoints = new Waypoint[] {
                new Waypoint(nextApproachLeg, Driver.Directive.MOVE_FORWARD),
                new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
            };
        } else {
            waypoints = new Waypoint[] {
                new Waypoint(nextApproachLeg, Driver.Directive.MOVE_FORWARD),
                new Waypoint(nextTurnCell, getTurnDirective(nextDirection)),
                new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
            };
        }
        nextWaypoint = new Waypoint(nextTurnCell,
                                    getTurnDirective(nextDirection));
    }

    /*
    private void updateDestination() {
        nextIntersection = getIntersectionAhead(ac, location);
        if (ac.LANE_POLICY) {
            if (vehicle.hasPassengers) {
                if (ac.SMART_TURNS) {
                    Intersection destination =
                        vehicle.getPassengerDestination();
                    if (destination.idNum == nextIntersection.idNum) {
                        nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
                    } else {
                        Direction[] directions =
                            nextIntersection.getDirectionsTo(destination);
                        Bag legs = new Bag();
                        for (int i = 0; i < directions.length; i++) {
                            if (directions[i] != null) {
                                legs.addAll(nextIntersection.getDepartureLegsByDirection(ac, directions[i]));
                            }
                        }
                        if (legs.numObjs == 0) {
                            System.out.println(this);
                            System.out.println(vehicle);
                            System.out.println(nextIntersection);
                            System.out.println(destination);
                        }
                        nextLeg = (Int2D)(legs.objs[ac.random.nextInt(legs.numObjs)]);
                    }
                } else {
                    nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
                }
                nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
                nextLeg =
                    nextIntersection.getDepartureLeg(ac, nextDirection,
                                                     nextDirection.onLeft());
            } else {
                nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
                nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
                nextLeg =
                    nextIntersection.getDepartureLeg(ac, nextDirection,
                                                     nextDirection.onRight());
            }
        } else {
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
        }
        if (nextDirection == null
            || nextDirection.toInt() == direction.opposite().toInt()) {
            nextLeg = getRandomDepartureLeg(ac, nextIntersection, direction);
            nextDirection = Direction.byInt(ac.roadGrid.field[nextLeg.x][nextLeg.y]);
        }
        nextApproachLeg = getNextApproachLeg(ac, nextIntersection, location, direction);
        nextTurnCell = setTurnCell(ac, nextLeg, location, direction);
        if (direction == nextDirection) {
            waypoints = new Waypoint[] {
                new Waypoint(nextApproachLeg, Driver.Directive.MOVE_FORWARD),
                new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
            };
        } else {
            waypoints = new Waypoint[] {
                new Waypoint(nextApproachLeg, Driver.Directive.MOVE_FORWARD),
                new Waypoint(nextTurnCell, getTurnDirective(nextDirection)),
                new Waypoint(nextLeg, Driver.Directive.MOVE_FORWARD)
            };
        }
        nextWaypoint = new Waypoint(nextTurnCell,
                                    getTurnDirective(nextDirection));
    }
    */

    void updateDestination(AgentCity ac) {
        updateState(ac);
        updateDestination();
    }

    boolean safeMerge(Direction dir) {
        Vehicle vehicle;
        int speed = (this.speed < 1 ? this.speed + 1 : 1);
        int x = location.x + speed * direction.getXOffset() + dir.getXOffset();
        int y = location.y + speed * direction.getYOffset() + dir.getYOffset();
        if (!ac.checkBounds(x, y)) {
            return false;
        }
        if (ac.roadGrid.field[x][y] != direction.toInt()) {
            return false;
        }
        for (int i = 0; i < maxSpeed + 1; i++) {
            //System.out.println(x);
            //System.out.println(y);
            if (ac.agentGrid.numObjectsAtLocation(x, y) > 0) {
                vehicle =
                    (Vehicle)ac.agentGrid.getObjectsAtLocation(x, y).objs[0];
                if (vehicle.getSpeed() >= i - 1) {
                    //System.out.println(this);
                    //System.out.println(vehicle);
                    //System.out.println(x);
                    //System.out.println(y);
                    return false;
                }
            }
            x = x + direction.opposite().getXOffset();
            y = y + direction.opposite().getYOffset();
        }
        return true;
    }

    public void step(final SimState state) {
        ac = (AgentCity)state;

        // Current Vehicle position and velocity;
        updateState(ac);

        // get a new destination if needed
        if (nextIntersection == null
            || nextDirective == Driver.Directive.MERGE_LEFT
            || nextDirective == Driver.Directive.MERGE_RIGHT) {
            updateDestination();
        }

        nearApproachLeg = getStepsToCell(nextApproachLeg) == 1;
        // check if Vehicle is at approach leg
        atApproachLeg = location.equals(nextApproachLeg);
        // check if Vehicle is in intersection
        nearIntersection = nearApproachLeg || atApproachLeg;
        inIntersection = ac.roadGrid.field[location.x][location.y] == 9;

        // check if Vehicle is one cell before destination
        nearNextLeg = getStepsToCell(nextLeg) == 1;
        // check if Vehicle is at destination
        atNextLeg = getGapToCell(nextLeg) < 0;
        // check if Vehicle is at nextWaypoint
        atWaypoint = getGapToCell(nextWaypoint.cell) < 0;


        // get a new destination if needed
        if (atNextLeg) {
            nextIntersection.cancelReservation(vehicle);
            vehicle.hasReservation = false;
            hasReservation = false;
            updateDestination();
        }

        // Default state is move forward at max speed
        desiredSpeed = maxSpeed;
        nextDirective = Driver.Directive.MOVE_FORWARD;

        // check if lane change is required
        if (ac.LANE_POLICY && nextIntersection.getLanePolicy()) {
            if (!inIntersection && !nearIntersection) {
                if (vehicle.hasPassengers) {
                    if (safeMerge(direction.onLeft())) {
                        //System.out.println("MERGING_LEFT");
                        nextDirective = Driver.Directive.MERGE_LEFT;
                        desiredSpeed = 1;
                    }
                } else if (safeMerge(direction.onRight())) {
                    //System.out.println("MERGING_RIGHT");
                    //System.out.print(this);
                    //System.out.print(this.vehicle.toString());
                    nextDirective = Driver.Directive.MERGE_RIGHT;
                    desiredSpeed = 1;
                }
            }
        }

        // check reservation or request as needed
        if (inIntersection || nearIntersection) {
            updateReservation();
        }

        // If the directive is move forward and the way is not clear, stop.
        maxSafeSpeed = getSafeSpeed(ac);
        if (nextDirective != Driver.Directive.MERGE_RIGHT
            && nextDirective != Driver.Directive.MERGE_LEFT) {
            if (maxSafeSpeed < desiredSpeed) {
                desiredSpeed = maxSafeSpeed;
            }
        }

        if (hasReservation && maxSafeSpeed == 0 && !inIntersection) {
            nextIntersection.cancelReservation(vehicle);
            hasReservation = false;
            vehicle.hasReservation = false;
            if (inIntersection) updateReservation();
        }

        // prepare to take appropriate action at Waypoint
        nearWaypoint = getStepsToCell(nextWaypoint.cell) == 1
            && getGapToCell(nextWaypoint.cell) < desiredSpeed;
        stepsToWaypoint = getStepsToCell(nextWaypoint.cell);
        if (nearWaypoint) {
            nextDirective = nextWaypoint.directive;
            // slow down if not moving forward
            if (nextDirective != Driver.Directive.MOVE_FORWARD) {
                desiredSpeed = getGapToCell(nextWaypoint.cell) + 1;
            }
        }

        checkReservation();

        // If the directive is move forward and the way is not clear, stop.
        if (nextDirective != Driver.Directive.MERGE_RIGHT
            && nextDirective != Driver.Directive.MERGE_LEFT) {
            if (maxSafeSpeed < desiredSpeed) {
                desiredSpeed = maxSafeSpeed;
            }
        }
    }
}
