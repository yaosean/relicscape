package relicscape;

public class World {
    private final int w;
    private final int h;

    private final int[][] rawTileIds;
    private final boolean[][] collision;
    private final TileType[][] painted;

    public World(int width, int height) {
        this.w = width;
        this.h = height;
        this.rawTileIds = new int[height][width];
        this.collision = new boolean[height][width];
        this.painted = new TileType[height][width];

        for (int y = 0; y < height; y++) {
            TileType base = baseForRow(y);
            for (int x = 0; x < width; x++) {
                painted[y][x] = base;
            }
        }
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < w && y >= 0 && y < h;
    }

    public int getTileIndex(int x, int y) { return rawTileIds[y][x]; }
    public void setTileIndex(int x, int y, int idx) { rawTileIds[y][x] = idx; }

    public boolean isBlocked(int x, int y) { return collision[y][x]; }
    public void setBlocked(int x, int y, boolean b) { collision[y][x] = b; }

    public TileType getTile(int x, int y) {
        if (!inBounds(x, y)) return TileType.GRASS;
        return painted[y][x];
    }

    public void setTile(int x, int y, TileType type) {
        if (!inBounds(x, y)) return;
        painted[y][x] = type;

        boolean hardStop = (type == TileType.TREE || type == TileType.ROCK ||
                type == TileType.CACTUS || type == TileType.RUIN_WALL);
        collision[y][x] = hardStop;
    }

    public TileType baseForRow(int y) {
        if (h == 0) return TileType.GRASS;
        double band = (double) y / (double) Math.max(1, h - 1);
        if (band < 0.34) return TileType.GRASS;
        if (band < 0.67) return TileType.SAND;
        return TileType.RUIN_FLOOR;
    }
}
