package relicscape;

/**
 * World holds TMX-derived map state including collisions.
 */
public class World {
    private final int width;
    private final int height;

    // Optional tile indices from a base layer (for future rendering)
    private final int[][] tiles;

    // Collision flags combined from "collision" and "collision 2" layers
    private final boolean[][] collision;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[height][width];
        this.collision = new boolean[height][width];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public int getTileIndex(int x, int y) { return tiles[y][x]; }
    public void setTileIndex(int x, int y, int idx) { tiles[y][x] = idx; }

    public boolean isBlocked(int x, int y) { return collision[y][x]; }
    public void setBlocked(int x, int y, boolean b) { collision[y][x] = b; }

    // Backwards-compat helpers for existing code paths
    public TileType getTile(int x, int y) { return TileType.GRASS; }
    public void setTile(int x, int y, TileType type) { /* no-op for TMX mode */ }
    public TileType baseForRow(int y) { return TileType.GRASS; }
}
