import java.util.ArrayList;
import java.util.Scanner;

class Player {
    /*map width, map height */
    static int w = 30, h = 15;

    /*my score, enemy score*/
    static int ms, es;

    private static final int THRESHOLD = 2;
    private static final int MAX_MOVE_DISTANCE = 4;
    private static final int RADAR_DIST = 4;

    //    static int[][] assignedOreMap;
    static Vector[] enemyPos = new Vector[5];
    static Vector[] preEnemyPos = new Vector[5];

    public static void main(String[] args) {


        Scanner sc = new Scanner(System.in);
        w = sc.nextInt();
        h = sc.nextInt();
        int[][] radarMap = new int[w][h];
        int[][] trapMap = new int[w][h];
        int[][] oreMap = new int[w][h];
        int[][] holeMap = new int[w][h];
        int[][] radarCoverageMap = new int[w][h];
        int[][] enemyHoleMap = new int[w][h];
        int[][] isInsideRadar = new int[w][h];
        int[][] assignedOreMap = new int[w][h];
        Grid grid = new Grid(oreMap, holeMap, trapMap, assignedOreMap, radarMap, radarCoverageMap, enemyHoleMap, isInsideRadar);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                radarCoverageMap[x][y] = getAmountOfNewRadarCoverage(grid, RADAR_DIST, new Vector(x, y));
                isInsideRadar[x][y] = 0;
            }
        }
        Robot[] own_robots = new Robot[5];
        own_robots[0] = new Robot(-1, EntityType.OWN_ROBOT, new Vector(0, 2), Item.NOTHING);
        own_robots[1] = new Robot(-1, EntityType.OWN_ROBOT, new Vector(0, 5), Item.NOTHING);
        own_robots[2] = new Robot(-1, EntityType.OWN_ROBOT, new Vector(0, 8), Item.NOTHING);
        own_robots[3] = new Robot(-1, EntityType.OWN_ROBOT, new Vector(0, 11), Item.NOTHING);
        own_robots[4] = new Robot(-1, EntityType.OWN_ROBOT, new Vector(0, 14), Item.NOTHING);
        for (int i = 0; i < enemyPos.length; i++) {
            enemyPos[i] = new Vector(-1, -1);
            preEnemyPos[i] = new Vector(-1, -1);
        }
        while (true) {
            ms = sc.nextInt();
            es = sc.nextInt();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    String oreRead = sc.next();
                    int oreAmount = oreRead.equalsIgnoreCase("?") ? 0 : Integer.parseInt(oreRead);
                    oreMap[x][y] = assignedOreMap[x][y] = oreAmount;
                    holeMap[x][y] = Integer.parseInt(sc.next());
                }
            }

            int entityCount = sc.nextInt();
            int radarCountdown = sc.nextInt();
            int trapCountdown = sc.nextInt();
            int own_robot_count = 0;
            for (int i = 0; i < entityCount; i++) {
                int entityId = sc.nextInt();
                EntityType entityType = EntityType.values()[sc.nextInt()];
                Vector pos = new Vector(sc.nextInt(), sc.nextInt());
                Item carrying = Item.values()[Math.abs(sc.nextInt()) - 1];
                if (entityType == EntityType.BURIED_RADAR) {
                    radarMap[pos.getX()][pos.getY()] = 1;
                    updateRadarCoverageMap(grid, RADAR_DIST, pos);
                    continue;
                } else if (entityType == EntityType.BURIED_TRAP) {
                    trapMap[pos.getX()][pos.getY()] = 1;
                    continue;
                } else if (entityType == EntityType.OWN_ROBOT) {
                    own_robots[own_robot_count++].updateValues(entityId, entityType, pos, carrying);
                }
                Entity entity = new Entity(entityId, entityType, pos, carrying);
            }
            updateEnemyHoleMap(enemyPos, preEnemyPos, grid);
            grid.updateMaps(oreMap, holeMap, trapMap, assignedOreMap, radarMap, radarCoverageMap, enemyHoleMap, isInsideRadar);
            for (Robot r : own_robots) {
                String move = r.getMove(own_robots, radarCountdown, trapCountdown, grid);
                System.out.println(move);
            }
            System.arraycopy(enemyPos, 0, preEnemyPos, 0, enemyPos.length);
        }

    }

    static int getAmountOfNewRadarCoverage(Grid grid, int dist, Vector pos) {
        int[][] copyMap = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                copyMap[x][y] = grid.getMap(MapType.insideRadar)[x][y];
            }
        }
        int amount = 0;
        for (int xi = -dist; xi <= dist; xi++) {
            for (int yi = -dist; yi <= dist; yi++) {
                Vector xyi = new Vector(clamp(pos.getX() + xi, 0, w - 1), clamp(pos.getY() + yi, 0, h - 1));
                if (copyMap[xyi.getX()][xyi.getY()] == 0 && Vector.getDistance(xyi, pos) <= dist) {
                    amount++;
                    copyMap[xyi.getX()][xyi.getY()] = 1;
                }
            }
        }
        return amount;
    }

    static void updateRadarCoverageMap(Grid grid, int dist, Vector destination) {

        for (int xi = -dist; xi <= dist; xi++) {
            for (int yi = -dist; yi <= dist; yi++) {
                Vector xyi = new Vector(clamp(destination.getX() + xi, 0, w - 1), clamp(destination.getY() + yi, 0, h - 1));
                if (Vector.getDistance(destination, xyi) <= dist) {
                    grid.getMap(MapType.insideRadar)[xyi.getX()][xyi.getY()] = 1;
                }
            }
        }
        int[][] newRadarCoverage = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                Vector ij = new Vector(i, j);
                if (Vector.getDistance(destination, ij) <= dist * 2) {
                    newRadarCoverage[i][j] = getAmountOfNewRadarCoverage(grid, dist, ij);
                } else {
                    newRadarCoverage[i][j] = getValueFromTDArrayCapped(grid.getMap(MapType.radarCoverage), ij);
                }
            }
        }
        grid.updateSpecific(newRadarCoverage, MapType.radarCoverage);
    }

//    static Vector getBestRadarPosition(Grid grid) {
//        int[] x = {4, 10, 15, 16, 16, 21, 22, 25, 25};
//        int[] y = {7, 9, 7, 11, 3, 8, 13, 7, 11};
//
//        Vector[] vecs = new Vector[] {
//                new Vector(4, 7),
//                new Vector(10, 9),
//                new Vector(15, 7),
//                new Vector(16, 11),
//                new Vector(16, 3),
//                new Vector(21, 8),
//                new Vector(22, 13),
//                new Vector(25, 7),
//                new Vector(25, 11),
//        };
//
//        for(Vector v : vecs) {
//            if(getValueFromTDArrayCapped(grid.getMap(MapType.radar), v) == 0) {
//                return v;
//            }
//        }
//        return new Vector(-1, -1);
//    }

    static Vector getBestRadarPosition(Grid grid) {
        Vector best = new Vector(-1, -1);
        int amount = 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {

                int yo = (h / 2) + (int) (Math.pow(-1, y) * ((y + 1) / 2));
                Vector xy = new Vector(x, yo);
                int curAmount = getValueFromTDArrayCapped(grid.getMap(MapType.radarCoverage), xy);
                if (amount < curAmount && getValueFromTDArrayCapped(grid.getMap(MapType.radar), xy) == 0 && getValueFromTDArrayCapped(grid.getMap(MapType.enemyHole), xy) == 0) {
                    System.out.println(x + " " + y);
                    best = xy;
                    amount = curAmount;
                }
            }
        }
//        printMap(grid.getMap(MapType.radarCoverage));
        return best;
    }


    static void updateEnemyHoleMap(Vector[] enemyPos, Vector[] preEnemyPos, Grid grid) {
        for (int i = 0; i < enemyPos.length; i++) {
            if (enemyPos[i].toString().equalsIgnoreCase("-1 -1") || preEnemyPos[i].toString().equalsIgnoreCase("-1 -1"))
                continue;
            int dist = Vector.getDistance(enemyPos[i], preEnemyPos[i]);
            if (dist == 0) {
                for (Vector v : getInReachVectors(enemyPos[i])) {
                    int xi = clamp(v.getX(), 0, w - 1);
                    int yi = clamp(v.getY(), 0, h - 1);
                    if (grid.getMap(MapType.hole)[xi][yi] == 1) {
                        grid.getMap(MapType.enemyHole)[xi][yi] = 1;
                    }
                }
            }
        }
    }

    static void printMap(int[][] map) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                System.err.print(map[i][j] + "  ");
            }
            System.err.println();
        }
    }

    static void printBooleanMap(boolean[][] map) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                System.err.print(map[i][j] ? "1" : "0");
            }
            System.err.println();
        }
    }


    public static class Grid {
        ArrayList<MapVar> maps = new ArrayList<>();
        public int width, height;

        public Grid(int[][]... maps) {
            this.width = w;
            this.height = h;
            updateMaps(maps);
        }

        public Grid(boolean empty) {
            this.width = 30;
            this.height = 15;
            if (empty) {
                updateMaps(new int[w][h], new int[w][h], new int[w][h], new int[w][h], new int[w][h], new int[w][h], new int[w][h], new int[w][h]);
            }
        }

        public void updateSpecific(int[][] map, MapType mapType) {
            this.maps.remove(mapType.occurrence);
            this.maps.add(mapType.occurrence, new MapVar(mapType, map));
        }

        public void updateMaps(int[][]... maps) {
            this.maps.clear();
            this.maps.add(new MapVar(MapType.ore, maps[MapType.ore.occurrence]));
            this.maps.add(new MapVar(MapType.hole, maps[MapType.hole.occurrence]));
            this.maps.add(new MapVar(MapType.trap, maps[MapType.trap.occurrence]));
            this.maps.add(new MapVar(MapType.assigned, maps[MapType.assigned.occurrence]));
            this.maps.add(new MapVar(MapType.radar, maps[MapType.radar.occurrence]));
            this.maps.add(new MapVar(MapType.radarCoverage, maps[MapType.radarCoverage.occurrence]));
            this.maps.add(new MapVar(MapType.enemyHole, maps[MapType.enemyHole.occurrence]));
            this.maps.add(new MapVar(MapType.insideRadar, maps[MapType.insideRadar.occurrence]));
        }

        public int[][] getMap(MapType type) {
            for (MapVar cur : maps) {
                if (cur.type.equals(type)) {
                    return cur.map;
                }
            }
            return null;
        }

    }

    static class MapVar {
        public MapType type;
        public int[][] map;

        MapVar(MapType type, int[][] map) {
            this.type = type;
            this.map = map;
        }

    }

    enum MapType {
        ore(0), hole(1), assigned(3), trap(2), radar(4), radarCoverage(5), enemyHole(6), insideRadar(7);
        int occurrence;

        MapType(int occurrence) {
            this.occurrence = occurrence;
        }
    }

    enum Item {
        NOTHING, RADAR, TRAP, ORE
    }

    enum EntityType {
        OWN_ROBOT, ENEMY_ROBOT, BURIED_RADAR, BURIED_TRAP
    }

    public static class Entity {
        protected int entityId;
        protected EntityType entityType;
        protected Vector pos;
        protected Item item;

        public Entity(int entityId, EntityType entityType, Vector pos, Item item) {
            init(entityId, entityType, pos, item);
        }

        void init(int entityId, EntityType entityType, Vector pos, Item item) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.pos = pos;
            this.item = item;
        }

    }

    public static class Robot extends Entity {


        public Robot(int entityId, EntityType entityType, Vector pos, Item item) {
            super(entityId, entityType, pos, item);
        }

        public void updateValues(int entityId, EntityType entityType, Vector pos, Item item) {
            init(entityId, entityType, pos, item);
        }


        private String move__move(Entity source, Vector pos) {
            if (pos.toString().equalsIgnoreCase(this.pos.toString())) {
                return move__dig(source, pos);
            }
            return "MOVE " + pos.toString();
        }

        private String move__dig(Entity source, Vector pos) {
            return "DIG " + pos.toString();
        }

        private String move__request(Entity source, String type) {
            return "REQUEST " + type;
        }

        private String move__wait() {
            return "WAIT";
        }

        /**
         *
         * @param destination
         * @return
         */
        private Vector getNextMove(Vector destination) {
            int xDifToClosest = pos.getX() - destination.getX();
            int yDifToClosest = pos.getY() - destination.getY();
            int xMovesMade = Math.min(MAX_MOVE_DISTANCE, xDifToClosest);
            int maxYMoves = Math.min(MAX_MOVE_DISTANCE - xMovesMade, Math.min(MAX_MOVE_DISTANCE, yDifToClosest));
            return new Vector(pos.getX() - xMovesMade, pos.getY() - maxYMoves);
        }

        public String getMove(Robot[] own_robots, int radarCountdown, int trapCountdown, Grid grid) {
            double radarDesire = getRadarDesire(grid, own_robots, radarCountdown);
            if (pos.toString().equalsIgnoreCase("-1 -1")) {
                return move__wait();
            }

            if (item == Item.RADAR) {
                if (!getBestRadarPosition(grid).toString().equalsIgnoreCase("-1 -1")) {
                    return getRadarMove(grid);
                }
            }
            if (item == Item.ORE) {
                return move__move(this, getNextMove(new Vector(0, pos.getY())));
            }
            Vector firstNonHole = new Vector(-1, -1);
            for (int i = THRESHOLD; i < w; i++) {
                if (grid.getMap(MapType.hole)[i][pos.getY()] == 0) {
                    firstNonHole = new Vector(i, pos.getY());
                    break;
                }
            }

            if (radarDesire > 0.4) {
                Robot closestToSpawn = closestToSpawn(own_robots);
                if (this == closestToSpawn)
                    return getRadar();
            }

            int amountOfOre = getSumOfTDArray(grid.getMap(MapType.assigned));
            if (amountOfOre > 0) {
                Vector bestOreLocation = closestOrePatch(grid.getMap(MapType.assigned), 4, grid);
                if (bestOreLocation == null) {
                    Vector bestNonHole = closestNonHoleVector(grid.getMap(MapType.assigned), 4);
                    if (bestNonHole == null) {
                        return getRandomMove(firstNonHole, grid);
                    }
                    return getRandomMove(bestNonHole, grid);
                }
                if (Vector.getDistance(bestOreLocation, pos) <= 4) {
                    grid.getMap(MapType.assigned)[bestOreLocation.getX()][bestOreLocation.getY()]--;
                    if (Vector.getDistance(bestOreLocation, pos) <= 1) {
                        return move__dig(this, bestOreLocation);
                    }
                }
                return move__move(this, getNextMove(bestOreLocation));
            }
            return getRandomMove(firstNonHole, grid);


        }

        String getRadarMove(Grid grid) {
            Vector bestSpot = getBestRadarPosition(grid);
            return move__move(this, getNextMove(bestSpot));

        }

        String getRadar() {
            if (pos.getX() == 0) {
                return move__request(this, "RADAR");
            } else {
                return move__move(this, getNextMove(new Vector(0, pos.getY())));
            }
        }

        double getRadarDesire(Grid grid, Robot[] own_robots, int countdown) {
            for (Robot r : own_robots) {
                if (r.item == Item.RADAR) {
                    return 0;
                }
            }
            Robot closest = closestToSpawn(own_robots);
            double ticksToSpawn = (int) Math.ceil((double) closest.pos.getX() / (double) countdown);
            return (double) 1 / clamp(getSumOfTDArray(grid.getMap(MapType.assigned)), 1, 4);
        }

        Vector closestOrePatch(int[][] map, int maxSteps, Grid grid) {
            int curmoveindex = 0;
            Vector cur = pos;
            for (int i = 0; i < (MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE) * maxSteps; i++) {
                for (int k = 0; k < 2; k++) {
                    for (int j = 0; j < i; j++) {
                        cur.add(directions.values()[curmoveindex].add);
                        int xi = Math.max(0, Math.min(cur.getX(), w - 1));
                        int yi = Math.max(0, Math.min(cur.getY(), h - 1));

                        if (map[xi][yi] > 0 && grid.getMap(MapType.enemyHole)[xi][yi] == 0) {
                            return new Vector(xi, yi);
                        }
                    }
                    curmoveindex++;
                    curmoveindex = curmoveindex % directions.values().length;
                }
            }

            return null;
        }


        Vector closestNonHoleVector(int[][] map, int maxSteps) {
            int curmoveindex = 0;
            Vector cur = pos;
            for (int i = 0; i < (MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE) * maxSteps; i++) {
                for (int k = 0; k < 2; k++) {
                    for (int j = 0; j < i; j++) {
                        cur.add(directions.values()[curmoveindex].add);
                        int isHole = map[Math.max(0, Math.min(cur.getX(), w - 1))][Math.max(0, Math.min(cur.getY(), h - 1))];
                        if (isHole == 0) {
                            return cur;
                        }
                    }
                    curmoveindex++;
                    curmoveindex = curmoveindex % directions.values().length;
                }
            }

            return null;
        }

        enum directions {
            left("L", new Vector(-1, 0)), up("U", new Vector(0, 1)), right("R", new Vector(1, 0)), down("D", new Vector(0, -1));
            String n;
            Vector add;

            directions(String n, Vector add) {
                this.n = n;
                this.add = add;
            }
        }

        Robot closestToSpawn(Robot[] own_robots) {
            Robot closest = own_robots[0];
            for (Robot r : own_robots) {
                if (r.pos.toString().equalsIgnoreCase("-1 -1")) {
                    continue;
                }
                if (r.pos.getX() < closest.pos.getX()) {
                    closest = r;
                }
            }
            return closest;

        }


        String getRandomMove(Vector firstNonHole, Grid grid) {
            if (pos.getX() >= w - 1 || pos.getX() < THRESHOLD) {
                return move__move(this, new Vector(THRESHOLD, (pos.getY() + 1) % h));
            }
            Vector[] inReach = getInReachVectors(pos);
            for (Vector v : inReach) {
                if (getValueFromTDArrayCapped(grid.getMap(MapType.hole), v) == 0 && getValueFromTDArrayCapped(grid.getMap(MapType.enemyHole), v) == 0) {
                    return move__dig(this, v);
                }
            }
            return move__move(this, firstNonHole);
        }


    }

    static Vector[] getInReachVectors(Vector pos) {
        Vector[] vecs = new Vector[5];
        vecs[0] = pos;
        vecs[1] = new Vector(pos.getX(), pos.getY() + 1);
        vecs[2] = new Vector(pos.getX(), pos.getY() - 1);
        vecs[3] = new Vector(pos.getX() - 1, pos.getY());
        vecs[4] = new Vector(pos.getX() + 1, pos.getY());
        return vecs;
    }

    static int getSumOfTDArray(int[][] map) {
        int amount = 0;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                amount += map[i][j];
            }
        }
        return amount;
    }

    static int getValueFromTDArrayCapped(int[][] array, Vector pos) {
        return array[Math.max(0, Math.min(pos.getX(), array.length - 1))][Math.max(0, Math.min(pos.getY(), array[0].length - 1))];
    }


    public static class Vector implements Comparable<Vector> {
        int x, y;

        public Vector(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        static int getDistance(Vector a, Vector b) {
            return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
        }


        public void add(Vector b) {
            this.x += b.x;
            this.y += b.y;
        }


        @Override
        public String toString() {
            return x + " " + y;
        }

        @Override
        public int compareTo(Vector o) {
            return this.getX() == o.getX() && this.getY() == o.getY() ? 1 : 0;
        }
    }

    static int clamp(int val, int min, int max) {
        return Math.max(Math.min(max, val), min);
    }


}
