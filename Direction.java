/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import java.util.Map;

enum Direction {

    NONE(0, 0, 0),
    NORTH(1, 0, -1),
    NORTH_EAST(2, 1, -1),
    EAST(3, 1, 0),
    SOUTH_EAST(4, 1, 1),
    SOUTH(5, 0, 1),
    SOUTH_WEST(6, -1, 1),
    WEST(7, -1, 0),
    NORTH_WEST(8, -1, -1),
    ALL(9, 0, 0);

    private final int dirNum;
    private final int xOffset;
    private final int yOffset;

    private final static Map<Integer, Direction> map =
        stream(Direction.values()).collect(toMap(dir -> dir.dirNum, dir -> dir));

    private Direction(final int dir, final int x, final int y) {
        this.dirNum = dir;
        this.xOffset = x;
        this.yOffset = y;
    }

    public int toInt() { return dirNum; }
    public int getXOffset() { return xOffset; }
    public int getYOffset() { return yOffset; }

    public static Direction byInt(int dirNum) {
        return map.get(dirNum);
    }

    public Direction onRight() {
        if (dirNum > 0 && dirNum < 9) { 
            return this.byInt((dirNum + 2) % 8);
        } else {
            return this;
        }
    }

    public Direction onLeft() {
        if (dirNum > 0 && dirNum < 9) { 
            return this.byInt((dirNum + 6) % 8);
        } else {
            return this;
        }
    }
}
