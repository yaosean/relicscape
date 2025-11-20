package relicscape;

/**
 * Holds the grid of tiles for Relicscape.
 */
public class World {
    private final int width;
    private final int height;
    private final TileType[][] tiles;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileType[height][width];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public TileType getTile(int x, int y) {
        return tiles[y][x];
    }

    public void setTile(int x, int y, TileType type) {
        tiles[y][x] = type;
    }

    /**
     * Returns the base ground type for a given row, based on biome bands.
     */
    public TileType baseForRow(int y) {
        int forestEnd = height / 3;
        int desertEnd = 2 * height / 3;
        if (y < forestEnd) return TileType.GRASS;
        else if (y < desertEnd) return TileType.SAND;
        else return TileType.RUIN_FLOOR;
    }
}
