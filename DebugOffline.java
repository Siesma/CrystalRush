import processing.core.PApplet;

import java.util.Random;

public class DebugOffline extends PApplet {
    private static final int FH = 15;

    Player.Grid map;

    private static final int RADAR_DIST = 4;

    public static void main(String[] args) {
        PApplet.main("DebugOffline", args);
    }

    @Override
    public void settings() {
        size(3500, 1800);
    }

    @Override
    public void setup() {
        int w = 30;
        int h = 15;
        int[][] radarMap = fillNewArrayWithValue(30, 15, 0);
        int[][] trapMap = fillNewArrayWithValue(30, 15, 0);
        int[][] oreMap = fillNewArrayWithValue(30, 15, 0);
        int[][] holeMap = fillNewArrayWithValue(30, 15, 0);
        int[][] radarCoverageMap = fillNewArrayWithValue(30, 15, 0);
        int[][] enemyHoleMap = fillNewArrayWithValue(30, 15, 0);
        int[][] isInsideRadar = fillNewArrayWithValue(30, 15, 0);
        int[][] assignedOreMap = fillNewArrayWithValue(30, 15, 0);
        map = new Player.Grid(oreMap, holeMap, trapMap, assignedOreMap, radarMap, radarCoverageMap, enemyHoleMap, isInsideRadar);
        map.updateSpecific(generate(w, h), Player.MapType.ore);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                radarCoverageMap[x][y] = Player.getAmountOfNewRadarCoverage(map, RADAR_DIST, new Player.Vector(x, y));
                isInsideRadar[x][y] = 0;
            }
        }
        map.updateSpecific(radarCoverageMap, Player.MapType.radarCoverage);

    }


    private double lessen(double max, double exp, double in, double fac, double shift) {
        return max - (Math.pow(exp, in * fac + shift));
    }


    public double dist(int nx, int ny, int x, int y) {
        double m = 2.4;
        return Math.sqrt(Math.pow(Math.abs(nx - x), m) + Math.pow(Math.abs(ny - y), m));
    }

    public boolean cluster(int i, int j, int w, int h) {
        int ni = i - (w / 10);
        int nj = j - (h / 2);
        return lessen(1d, 0.4, Math.max(ni, 0) * getRandomNumber() - Math.abs(nj), 0.5, 2) > 0.75;
    }

    /*
    Returns a random number between 0 and 1
     */
    public double getRandomNumber () {
        return getRandomNumber((new Random()).nextLong());
    }

    private double getRandomNumber(long seed) {
        Random random = new Random();
        random.setSeed(seed);
        return random.nextDouble();
    }

    private int[][] generate(int w, int h) {
        Integer[][] oreMap = new Integer[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (cluster(i, j, w, h)) {
                    double increaseFactor = 2;
                    for (int xo = (int) -(1 + getRandomNumber() * increaseFactor); xo < (int) (1 + getRandomNumber() * increaseFactor); xo++) {
                        for (int yo = (int) (1 + getRandomNumber() * increaseFactor); yo < (int) (1 + getRandomNumber() * increaseFactor); yo++) {
                            int ni, nj;
                            ni = i + xo;
                            nj = j + yo;
                            if (inside(ni, nj, 0, oreMap.length, 0, oreMap[0].length)) {
                                double max = increaseFactor * 2;
//                                oreMap[ni][nj] = (int) Math.ceil(lessen(max, 0.99d, lessen(1, 0.5, getRandomNumber(), increaseFactor, 0) - (increaseFactor * max - increaseFactor), dist(ni, nj, i, j), 0));
                                oreMap[ni][nj] = (int) Math.ceil(lessen(max, 0.84d, lessen(1, 0.5, getRandomNumber(), increaseFactor, 0) - (increaseFactor * max - increaseFactor), dist(ni, nj, i, j), 0));
                            }
                        }
                    }
                }
                if (oreMap[i][j] == null)
                    oreMap[i][j] = 0;
            }
        }

        int[][] out = new int[oreMap.length][oreMap[0].length];
        for (int i = 0; i < oreMap.length; i++) {
            for (int j = 0; j < h; j++) {
                out[i][j] = oreMap[i][j];
            }

        }
        return out;
    }

    public int[][] fillNewArrayWithValue(int w, int h, int v) {
        int[][] out = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                out[i][j] = v;
            }
        }

        return out;
    }

    @Override
    public void mousePressed() {
        if (mouseButton == 37) {
            /*ore(0), hole(1), assigned(3), trap(2), radar(4), radarCoverage(5), enemyHole(6), insideRadar(7);


             */
            map.updateMaps(fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0), fillNewArrayWithValue(30, 15, 0));
            map.updateSpecific(generate(30, 15), Player.MapType.ore);
            int[][] radarCoverageMap = fillNewArrayWithValue(30, 15, 0);
            int[][] isInsideRadar = fillNewArrayWithValue(30, 15, 0);
            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 30; x++) {
                    radarCoverageMap[x][y] = Player.getAmountOfNewRadarCoverage(map, RADAR_DIST, new Player.Vector(x, y));
                    isInsideRadar[x][y] = 0;
                }
            }
            map.updateSpecific(radarCoverageMap, Player.MapType.radarCoverage);
        } else if (mouseButton == 39) {
            long begin = System.currentTimeMillis();
            Player.Vector best = Player.getBestRadarPosition(map);
            int[][] radarMap = map.getMap(Player.MapType.radar);
            if (best.toString().equalsIgnoreCase("-1 -1")) {
                System.out.println("Something went wrong");
                return;
            }
            radarMap[best.getX()][best.getY()] = 1;
            map.updateSpecific(radarMap, Player.MapType.radar);
            Player.updateRadarCoverageMap(map, RADAR_DIST, best);
            long end = System.currentTimeMillis();
            System.out.println("The calculation took " + (end - begin) + " ms" + " " + best.toString());
        }
    }


    public boolean inside(int x, int y, int min_x, int max_x, int min_y, int max_y) {
        return x > min_x && x < max_x && y > min_y && y < max_y;
    }

    @Override
    public void draw() {
        background(64);
        drawMap();
    }

    public void drawMap() {
        int w = 30;
        int h = 15;
        for (int i = w - 1; i >= 0; i--) {
            for (int j = h - 1; j >= 0; j--) {
                Dimension dimension = new Dimension(width / w, height / h);

                int x = i * dimension.getW(), y = j * dimension.getH();
                int squareCol = x == 0 ? color(92, 210, 62) : -1;
                int squareWeight = 1;
                int crossCol = color(54, 05, 95);
                int crossWeight = 2;
                Sprite sprite = new Sprite("testing");

                Type squareType = new Type(squareCol, squareWeight, sprite);
                Type crossType = new Type(crossCol, crossWeight, sprite);
                Type seeType = new Type(color(255, 32, 84), crossWeight, sprite);
                drawSquare(x, y, squareType, dimension);

                if (map.getMap(Player.MapType.radarCoverage)[i][j] > 0) {
//                    drawRadarCoverage(x, y, crossType, dimension, map.getMap(Player.MapType.radarCoverage)[i][j]);
                }
                if (map.getMap(Player.MapType.ore)[i][j] > 0) {
                    drawOre(x, y, crossType, dimension, map.getMap(Player.MapType.ore)[i][j]);
                }
                if (map.getMap(Player.MapType.insideRadar)[i][j] > 0 && map.getMap(Player.MapType.ore)[i][j] > 0) {
                    drawOre(x, y, seeType, dimension, map.getMap(Player.MapType.ore)[i][j]);
                }
                if (map.getMap(Player.MapType.radar)[i][j] > 0) {
                    drawRadar(x, y, crossType, dimension, map.getMap(Player.MapType.radar)[i][j]);
                }


            }
        }

    }

    private void drawRadarCoverage(int x, int y, Type type, Dimension dimension, int amount) {
        push();
        fill(0, 128, 0, 255);
//        rect(x, y, x + dimension.getW(), y + dimension.getH());
        textFont(createFont("Aral", FH));
        String display = "" + amount;

        text(display, x + dimension.getW() / 2 - FH / 4 - 1, y + dimension.getH() / 2 - FH / 2 - FH);
        pop();
    }

    private void drawRadar(int x, int y, Type type, Dimension dimension, int i) {
        push();
        fill(83, 12, 255, 128);
        rect(x, y, dimension.getW() - 1, dimension.getH() - 1);
        pop();
    }


    public void drawSquare(int x, int y, Type type, Dimension dimension) {
        push();
        noFill();
        strokeWeight(type.strokeWeight);
        stroke(type.typeCol);
        line(x, y, x + dimension.getW(), y);
        line(x, y, x, y + dimension.getH());
        line(x, y + dimension.getH(), x + dimension.getW(), y + dimension.getH());
        line(x + dimension.getW(), y, x + dimension.getW(), y + dimension.getH());
        pop();
    }


    public void drawOre(int x, int y, Type t, Dimension dimension, int amount) {
        push();
        noFill();
        if (amount <= 0) {
            pop();
            return;
        }
        // size in pixels
        int size = 10;
        strokeWeight(t.getStrokeWeight());
        fill(t.getTypeCol());
        int centerX = x + dimension.getW() / 2;
        int centerY = y + dimension.getH() / 2;


        line(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2);
        line(centerX + size / 2, centerY - size / 2, centerX - size / 2, centerY + size / 2);
        textFont(createFont("Aral", FH));
        String display = "" + amount;
        text(display, centerX - FH / 4 - 1, centerY - FH / 2);
        pop();
    }


    class Dimension {
        private int w, h;

        public Dimension(int w, int h) {
            this.w = w;
            this.h = h;
        }

        public int getW() {
            return w;
        }

        public Dimension setW(int w) {
            this.w = w;
            return this;
        }

        public int getH() {
            return h;
        }

        public Dimension setH(int h) {
            this.h = h;
            return this;
        }
    }

    class Type {

        private int typeCol;

        private int strokeWeight;

        private Sprite sprite;

        public Type(int col, int weight, Sprite sprite) {
            this.typeCol = col;
            this.strokeWeight = weight;
            this.sprite = sprite;
        }

        public int getTypeCol() {
            return typeCol;
        }

        public Type setTypeCol(int typeCol) {
            this.typeCol = typeCol;
            return this;
        }

        public int getStrokeWeight() {
            return strokeWeight;
        }

        public Type setStrokeWeight(int strokeWeight) {
            this.strokeWeight = strokeWeight;
            return this;
        }

        public Sprite getSprite() {
            return sprite;
        }

        public Type setSprite(Sprite sprite) {
            this.sprite = sprite;
            return this;
        }
    }

    public class Sprite {
        private String pathToFile;

        public Sprite(String pathToFile) {
            this.pathToFile = pathToFile;
        }

        public String getPathToFile() {
            return pathToFile;
        }

        public Sprite setPathToFile(String pathToFile) {
            this.pathToFile = pathToFile;
            return this;
        }
    }


    enum Table {
        ORE(0), ROBOT(1), TRAP(2), HOLE(3), RADAR(4), RADARCOVERAGE(5);
        int v;

        Table(int v) {
            this.v = v;
        }
    }


}
