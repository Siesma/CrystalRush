import java.util.Scanner;

class Player {

    final static int radarCoverageSize = 4;
    final static int maxMoveSteps = 4;
    final static int spawnThreshold = 1; // could also be tested at '4', although the probability of it is low.
    final static int amountOfRobots = 5;
    static int w, h;

    static int currentTick = 0;

    static Robot[] own_robots = new Robot[amountOfRobots];
    static Robot[] enemy_robots = new Robot[amountOfRobots];


    enum Item {
        Nothing, Radar, Trap, Ore
    }

    enum EntityType {
        Own_Robot, Enemy_Robot, buried_radar, buried_trap
    }


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int width = sc.nextInt();
        int height = sc.nextInt();
        w = width;
        h = height;

        Grid map = new Grid(w, h);


        for (int i = 0; i < own_robots.length; i++) {
            own_robots[i] = new Robot(map, -1, EntityType.Own_Robot, new Vector(0, (height / amountOfRobots) * i), Item.Nothing, 0, 0);
        }
        while (true) {

            int myScore = sc.nextInt();
            int enemyScore = sc.nextInt();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    String oreAmount = sc.next();
                    boolean isHole = sc.nextInt() == 1;
                    map.map[x][y].setAmountOfOre(oreAmount.equalsIgnoreCase("?") ? 0 : Integer.parseInt(oreAmount));
                    map.map[x][y].setIsHole(isHole);
                }
            }

            int entityCount = sc.nextInt();
            int radarCooldown = sc.nextInt();
            int trapCooldown = sc.nextInt();
            int own_robot_index = 0;
            int enemy_robot_index = 0;
            for (int i = 0; i < entityCount; i++) {
                int entityId = sc.nextInt();
                EntityType entityType = EntityType.values()[sc.nextInt()];
                int x = sc.nextInt();
                int y = sc.nextInt();
                Item item = Item.values()[Math.abs(sc.nextInt()) - 1];
                switch (entityType) {
                    case Own_Robot:
//                        own_robots[own_robot_index++] = new Robot(map, entityId, entityType, new Vector(x, y), item, radarCooldown, trapCooldown);
                        own_robots[own_robot_index++].updateInformation(map, entityId, entityType, new Vector(x, y), item, radarCooldown, trapCooldown);
                        break;
                    case Enemy_Robot:
                        enemy_robots[enemy_robot_index++] = new Robot(map, entityId, entityType, new Vector(x, y), item, radarCooldown, trapCooldown);
                        break;

                }
            }
            int it = 0;
            String[] moves = new String[amountOfRobots];
            for (Robot r : own_robots) {
//                String move = r.getMove(it++);
//                System.out.println(move.equalsIgnoreCase("") ? "WAIT" : move);
                moves[it] = r.getMove(it++);
            }
            for (String s : moves) {
                System.out.println(s);
            }
            System.err.println("Amount of visible ore: " + map.getAmountOfVisibleOre());

            if (currentTick > 4000) {
                return;
            }
        }


    }


    static class Robot extends Entity {

        Robot(Grid map, int entityID, EntityType entityType, Vector position, Item item, int radarCountdown, int trapCountdown) {
            super(map, entityID, entityType, position, item, radarCountdown, trapCountdown);
        }


        boolean isDead() {
            return position.toString().equalsIgnoreCase("-1 -1");
        }

        boolean needsRadar(int amountOfVisibleOre, int radarCountdown, int closestRobotMovesToSpawn) {
            boolean anyRobotHasRadar = false;
//            for (Robot r : own_robots) {
//                System.err.println(r.item);
//                if (r.isDead())
//                    continue;
//                if (r.item == Item.Radar || r.move.reason == Reasoning.radar) {
//                    anyRobotHasRadar = true;
//                    break;
//                }
//            }

            for (Robot r : own_robots) {
                if (r.isDead())
                    continue;
                if (r.move.reason == Reasoning.radar) {
                    anyRobotHasRadar = true;
                    break;
                }
            }

            return !anyRobotHasRadar && amountOfVisibleOre < 5 && radarCountdown <= closestRobotMovesToSpawn;
        }

        boolean needsTrap(int trapCountdown, int closestRobotMovesToSpawn) {
            boolean anyRobotHasTrap = false;
            for (Robot r : own_robots) {
                if (r.isDead())
                    continue;
                if (r.item == Item.Trap) {
                    anyRobotHasTrap = true;
                    break;
                }
            }
            return !anyRobotHasTrap && trapCountdown <= closestRobotMovesToSpawn;
        }


        Vector bestRadarPosition() {
            int[] x = {4, 10, 15, 16, 16, 21, 22, 25, 25};
            int[] y = {7, 9, 7, 11, 3, 8, 13, 7, 11};

            for (int i = 0; i < x.length; i++) {
                if (!map.map[x[i]][y[i]].hasRadar) {
                    return new Vector(x[i], y[i]);
                }
            }

            return new Vector(1, 1);
        }

        Vector bestTrapPosition() {
            return null;
        }

        Robot closestRobotToSpawn() {
            int distanceToSpawn = -1;
            Robot closest = null;
            for (Robot r : own_robots) {
                if (r.isDead())
                    continue;

                if (distanceToSpawn == -1 || distanceToSpawn > r.position.getX()) {
                    distanceToSpawn = r.position.getX();
                    closest = r;
                }
            }
            return closest;
        }


        String move_request(String type) {
            return "REQUEST " + type;
        }

        String move_move(Vector position) {
            return "MOVE " + position.toString();
        }

        String move_dig(Vector position) {
            System.err.println("Trying to dig with the following item: " + item);
            return "DIG " + position.toString();
        }

        String move_wait() {
            return "WAIT";
        }

        Vector getNextMove(Vector destination) {

            int xDifToClosest = position.getX() - destination.getX();
            int yDifToClosest = position.getY() - destination.getY();
            int xMovesMade = Math.min(maxMoveSteps, xDifToClosest);
            int maxYMoves = Math.min(maxMoveSteps - xMovesMade, Math.min(maxMoveSteps, yDifToClosest));
            return new Vector(position.getX() - xMovesMade, position.getY() - maxYMoves);
        }

        String getMove(int iterator) {

            if (isDead())
                return move_wait();
            if (item == Item.Ore) {
                return move_move(getNextMove(new Vector(0, position.getY())));
            } else if (item == Item.Radar || item == Item.Trap) {
                if (position.toString().equalsIgnoreCase(move.desiredPosition.toString()))
                    return move_dig(position);
                else
                    return move_move(getNextMove(move.desiredPosition));
            }


            if (move.hasMove()) {
                if (position.toString().equalsIgnoreCase(move.desiredPosition.toString())) {
                    return move_dig(position);
                } else {
                    if (move.reason == Reasoning.radar) {
                        if (item == Item.Nothing && position.getX() != 0) {
                            return move_move(getNextMove(new Vector(0, position.getY())));
                        } else if (item == Item.Nothing && position.getX() == 0) {
                            return move_request("RADAR");
                        }
                    } else {
                        return move_move(getNextMove(move.desiredPosition));
                    }
                }
            }

            Robot closestToSpawn = closestRobotToSpawn();
            for (Robot r : own_robots) {
                if (r.move.hasMove()) {
                    if (r.move.reason == Reasoning.ore) {
                        tickMap.map[r.position.getX()][r.position.getY()].reduceOreAmount(1);
                    }
                }
            }
            int amountOfVisibleOre = tickMap.amountOfVisibleOre;

            if (needsRadar(amountOfVisibleOre, radarCountdown, (int) (Math.ceil((float) closestToSpawn.position.getX() / maxMoveSteps))) && closestToSpawn == this) {
                if (position.getX() == 0) {
                    setMove(new Move(bestRadarPosition(), Reasoning.radar, move_move(getNextMove(bestRadarPosition())), currentTick));
                    return move_request("RADAR");
                } else {

                }
            } else if (amountOfVisibleOre > 0) {
                Vector orePosition = bestPositionForOre(1);
                String curMove = move_move(getNextMove(orePosition));
                setMove(new Move(orePosition, Reasoning.ore, curMove, currentTick));
                return curMove;
            } else {
                if (position.getX() >= spawnThreshold) {
                    Vector up, down, left, right;
                    up = new Vector(position.getX(), position.getY() + 1);
                    down = new Vector(position.getX(), position.getY() - 1);
                    left = new Vector(position.getX() - 1, position.getY());
                    right = new Vector(position.getX() + 1, position.getY());
                    Cell curCell, upCell, downCell, leftCell, rightCell;

                    curCell = tickMap.map[Math.min(0, Math.max(position.getX(), w))][Math.min(0, Math.max(position.getY(), h))];
                    upCell = tickMap.map[Math.min(0, Math.max(up.getX(), w))][Math.min(0, Math.max(up.getY(), h))];
                    downCell = tickMap.map[Math.min(0, Math.max(down.getX(), w))][Math.min(0, Math.max(down.getY(), h))];
                    leftCell = tickMap.map[Math.min(0, Math.max(left.getX(), w))][Math.min(0, Math.max(left.getY(), h))];
                    rightCell = tickMap.map[Math.min(0, Math.max(right.getX(), w))][Math.min(0, Math.max(right.getY(), h))];

                    if (!curCell.isHole && !curCell.visited) {
                        curCell.setVisited(true);
                        return move_dig(position);
                    } else if (!upCell.isHole && !upCell.visited && !up.toString().equalsIgnoreCase(position.toString())) {
                        upCell.setVisited(true);
                        return move_dig(up);
                    } else if (!downCell.isHole && !downCell.visited && !down.toString().equalsIgnoreCase(position.toString())) {
                        downCell.setVisited(true);
                        return move_dig(down);
                    } else if (!leftCell.isHole && !leftCell.visited && !left.toString().equalsIgnoreCase(position.toString())) {
                        leftCell.setVisited(true);
                        return move_dig(left);
                    } else if (!rightCell.isHole && !rightCell.visited && !right.toString().equalsIgnoreCase(position.toString())) {
                        rightCell.setVisited(true);
                        return move_dig(right);
                    } else {
                        return move_move(getNextMove(new Vector(position.getX() + 2, position.getY())));
                    }
                } else {
                    return move_move(getNextMove(new Vector(position.getX() + spawnThreshold, position.getY())));
                }
            }


            return move_wait();
        }


        Vector bestPositionForOre(int maxSteps) {
            int curmoveindex = 0;
            Vector cur = position;
            for (int i = 0; i < (maxMoveSteps * maxMoveSteps) * maxSteps; i++) {
                for (int j = 0; j < i; j++) {
                    cur.add(directions.values()[curmoveindex].add);
                    Cell c = map.map[Math.min(0, Math.max(cur.getX(), w))][Math.min(0, Math.max(cur.getY(), h))];
                    if (c.amountOfOre > 0 && !c.isTrapped) {
                        return cur;
                    }
                }
                curmoveindex++;
                curmoveindex = curmoveindex % directions.values().length;
                for (int j = 0; j < i; j++) {
                    cur.add(directions.values()[curmoveindex].add);
                    Cell c = map.map[Math.min(0, Math.max(cur.getX(), w))][Math.min(0, Math.max(cur.getY(), h))];
                    if (c.amountOfOre > 0 && !c.isTrapped) {
                        return cur;
                    }
                }
                curmoveindex++;
                curmoveindex = curmoveindex % directions.values().length;
            }

            return null;
        }


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

    enum Reasoning {
        ore("GET ORE"), radar("GET RADAR"), random("DIG RANDOM"), trap("GET TRAP"), none("");
        String reason;

        Reasoning(String reason) {
            this.reason = reason;
        }

    }

    static class Move {

        Vector desiredPosition;
        String currentMove;
        Reasoning reason;
        int initTick;

        Move(Vector desiredPosition, Reasoning reason, String currentMove, int initTick) {
            this.desiredPosition = desiredPosition;
            this.reason = reason;
            this.currentMove = currentMove;
            this.initTick = currentTick;
        }

        Move() {
            this.desiredPosition = new Vector(-1, -1);
            this.reason = Reasoning.none;
            this.currentMove = "";
            this.initTick = -1;
        }

        boolean hasMove() {
            return !this.desiredPosition.toString().equalsIgnoreCase("-1 -1") || initTick != -1;
        }


        public String next() {
            return "";
        }
    }

    static abstract class Entity {
        Grid map;
        Grid tickMap;
        int entityID;
        EntityType entityType;
        Vector position;
        Item item;
        int radarCountdown;
        int trapCountdown;
        Move move;

        Entity(Grid map, int entityID, EntityType entityType, Vector position, Item item, int radarCountdown, int trapCountdown) {
            init(map, entityID, entityType, position, item, radarCountdown, trapCountdown);
            tickMap = new Grid(w, h);
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    tickMap.map[i][j] = map.map[i][j];
                }
            }
            move = new Move();
        }


        void updateInformation(Grid map, int entityID, EntityType entityType, Vector position, Item item, int radarCountdown, int trapCountdown) {
            init(map, entityID, entityType, position, item, radarCountdown, trapCountdown);
        }

        private void init(Grid map, int entityID, EntityType entityType, Vector position, Item item, int radarCountdown, int trapCountdown) {
            this.map = map;
            this.entityID = entityID;
            this.entityType = entityType;
            this.position = position;
            this.item = item;
            this.radarCountdown = radarCountdown;
            this.trapCountdown = trapCountdown;
        }

        void setMove(Move move) {
            this.move = move;
        }
    }

    static class Grid {

        Cell[][] map;
        int amountOfVisibleOre;

        Grid(int width, int height) {
            generateEmptyMap(width, height);
            amountOfVisibleOre = 0;
        }

        void generateEmptyMap(int width, int height) {
            this.map = new Cell[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    map[x][y] = new Cell(new Vector(x, y));
                }
            }
        }

        void updateMap(Vector index, Item change, int amount) {
            switch (change) {
                case Ore:
                    updateOreAmount(index, amount);
                    break;
                case Trap:
                    updateTrappedCell(index);
                    break;
                case Radar:
                    updateRadarCell(index);
                    break;
            }
        }

        void updateMapCoverage() {
            for (int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    Cell cur = map[x][y];
                    if (!cur.hasRadar)
                        continue;
                    if (cur.hasRadarCoverage)
                        continue;
                    for (int i = (int) Math.floor((float) radarCoverageSize / 2); i < Math.ceil((float) radarCoverageSize / 2); i++) {
                        for (int j = (int) Math.floor((float) radarCoverageSize / 2); j < Math.ceil((float) radarCoverageSize / 2); j++) {
                            updateRadarCoverage(new Vector(x - i, y - j));
                        }
                    }
                }
            }
        }

        int getAmountOfVisibleOre() {
            return amountOfVisibleOre;
        }

        void updateTotalOreAmount() {
            for (int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    Cell cur = map[x][y];
                    if (!cur.hasRadarCoverage)
                        continue;
                    amountOfVisibleOre += cur.amountOfOre;
                }
            }
        }

        void updateRadarCell(Vector position) {
            map[position.getX()][position.getY()].setHasRadar(true);
            updateMapCoverage();
            updateTotalOreAmount();
        }

        void updateRadarCoverage(Vector position) {
            map[position.getX()][position.getY()].setHasRadarCoverage(true);
        }

        void updateTrappedCell(Vector position) {
            map[position.getX()][position.getY()].setTrapped(true);
        }

        void updateOreAmount(Vector position, int amount) {
            map[position.getX()][position.getY()].setAmountOfOre(amount);
        }


    }


    static class Cell {

        Vector position;
        Robot[] assignedRobots;

        int amountOfOre;
        boolean isHole;
        boolean hasRadar;
        boolean hasRadarCoverage;
        boolean isTrapped;

        boolean visited;


        Cell(Vector position, Robot... assignedRobots) {
            this.position = position;
            this.assignedRobots = assignedRobots;
            updateInformation(0, false, false, false, false, false);
        }

        void updateInformation(int amountOfOre, boolean isHole, boolean hasRadar, boolean hasRadarCoverage, boolean isTrapped, boolean visited) {
            this.amountOfOre = amountOfOre;
            this.isHole = isHole;
            this.hasRadar = hasRadar;
            this.hasRadarCoverage = hasRadarCoverage;
            this.isTrapped = isTrapped;
            this.visited = visited;
        }

        void setIsHole(boolean isHole) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }

        void setAmountOfOre(int amountOfOre) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }

        void setTrapped(boolean isTrapped) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }


        void setHasRadar(boolean hasRadar) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }


        void setHasRadarCoverage(boolean hasRadarCoverage) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }

        void setVisited(boolean visited) {
            updateInformation(amountOfOre, isHole, hasRadar, hasRadarCoverage, isTrapped, visited);
        }

        public void reduceOreAmount(int amount) {
            this.amountOfOre -= amount;
        }
    }


    static int getDistance(Vector a, Vector b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    static class Vector {
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

        @Override
        public String toString() {
            return x + " " + y;
        }

        public void add(Vector v) {
            this.x += v.x;
            this.y += v.y;
        }
    }

}
