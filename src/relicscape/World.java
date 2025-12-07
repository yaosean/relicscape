package relicscape;

public class World {
    private final int wide;
    private final int tall;

    private final int[][] tileNumbers;
    private final boolean[][] bonkGrid;
    private final TileType[][] prettyTiles;

    public World(int wide, int tall) {
        this.wide = wide;
        this.tall = tall;
        this.tileNumbers = new int[tall][wide];
        this.bonkGrid = new boolean[tall][wide];
        this.prettyTiles = new TileType[tall][wide];

        for (int loopyY = 0; loopyY < tall; loopyY++) {
            TileType base = baseForRow(loopyY);
            for (int loopyX = 0; loopyX < wide; loopyX++) {
                prettyTiles[loopyY][loopyX] = base;
            }
        }
    }

    public int getWidth() { return wide; }
    public int getHeight() { return tall; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < wide && y >= 0 && y < tall;
    }

    public int getTileIndex(int x, int y) { return tileNumbers[y][x]; }
    public void setTileIndex(int x, int y, int idx) { tileNumbers[y][x] = idx; }

    public boolean isBlocked(int x, int y) { return bonkGrid[y][x]; }
    public void setBlocked(int x, int y, boolean bonked) { bonkGrid[y][x] = bonked; }

    public TileType getTile(int x, int y) {
        if (!inBounds(x, y)) return TileType.GRASS;
        return prettyTiles[y][x];
    }

    public void setTile(int x, int y, TileType type) {
        if (!inBounds(x, y)) return;
        prettyTiles[y][x] = type;

        boolean hardStop = (type == TileType.TREE || type == TileType.ROCK ||
                type == TileType.CACTUS || type == TileType.RUIN_WALL);
        bonkGrid[y][x] = hardStop;
    }

    public TileType baseForRow(int y) {
        if (tall == 0) return TileType.GRASS;
        double band = (double) y / (double) Math.max(1, tall - 1);
        if (band < 0.34) return TileType.GRASS;
        if (band < 0.67) return TileType.SAND;
        return TileType.RUIN_FLOOR;
    }
}
