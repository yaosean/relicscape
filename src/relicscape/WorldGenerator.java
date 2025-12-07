package relicscape;

import java.util.Random;
public class WorldGenerator {

    public void generate(World world, RelicManager relicManager, Random rand) {
        int width = world.getWidth();
        int height = world.getHeight();
        int forestEnd = height / 3;
        int desertEnd = 2 * height / 3;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y < forestEnd) {
                    world.setTile(x, y, TileType.GRASS);
                } else if (y < desertEnd) {
                    world.setTile(x, y, TileType.SAND);
                } else {
                    world.setTile(x, y, TileType.RUIN_FLOOR);
                }
            }
        }

        generateClusters(world, rand, TileType.TREE, 40, 3, 6, 0, forestEnd - 1);
        generateClusters(world, rand, TileType.ROCK, 18, 2, 4, 0, forestEnd - 1);
        generateClusters(world, rand, TileType.FLOWER, 35, 2, 5, 0, forestEnd - 1);

        generateClusters(world, rand, TileType.DUNE, 45, 3, 7, forestEnd, desertEnd - 1);
        generateClusters(world, rand, TileType.CACTUS, 20, 2, 4, forestEnd, desertEnd - 1);
        generateClusters(world, rand, TileType.ROCK, 15, 2, 4, forestEnd, desertEnd - 1);

        generateClusters(world, rand, TileType.RUIN_WALL, 30, 3, 6, desertEnd, height - 1);
        generateClusters(world, rand, TileType.RUBBLE, 35, 2, 5, desertEnd, height - 1);

        carvePathHoriz(world, height / 2);
        carvePathVert(world, width / 2);

        int shrineX = width / 2;
        int shrineY = height / 2;
        world.setTile(shrineX, shrineY, TileType.SHRINE);
        relicManager.markShrine(shrineX, shrineY);

        placeRelicInBand(world, relicManager, rand, 0, forestEnd - 1);
        placeRelicInBand(world, relicManager, rand, forestEnd, desertEnd - 1);
        placeRelicInBand(world, relicManager, rand, desertEnd, height - 1);
    }

    private void generateClusters(World world, Random rand, TileType feature, int clusterCount,
                                  int minRadius, int maxRadius, int yStart, int yEnd) {
        int width = world.getWidth();
        int height = world.getHeight();
        if (yStart < 0) yStart = 0;
        if (yEnd >= height) yEnd = height - 1;

        for (int i = 0; i < clusterCount; i++) {
            int cx = rand.nextInt(width);
            int cy = yStart + rand.nextInt(Math.max(1, yEnd - yStart + 1));
            int radius = minRadius + rand.nextInt(Math.max(1, maxRadius - minRadius + 1));

            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int x = cx - radius; x <= cx + radius; x++) {
                    if (!world.inBounds(x, y)) continue;
                    double dist = Math.hypot(x - cx, y - cy);
                    if (dist <= radius && rand.nextDouble() < 0.8) {
                        TileType base = world.getTile(x, y);
                        if (isCompatible(base, feature)) {
                            world.setTile(x, y, feature);
                        }
                    }
                }
            }
        }
    }

    private boolean isCompatible(TileType base, TileType feature) {
        switch (feature) {
            case TREE:
            case ROCK:
            case FLOWER:
                return base == TileType.GRASS;
            case DUNE:
            case CACTUS:
                return base == TileType.SAND;
            case RUIN_WALL:
            case RUBBLE:
                return base == TileType.RUIN_FLOOR;
            default:
                return true;
        }
    }

    private void carvePathHoriz(World world, int y) {
        if (y < 0 || y >= world.getHeight()) return;
        for (int x = 0; x < world.getWidth(); x++) {
            world.setTile(x, y, world.baseForRow(y));
        }
    }

    private void carvePathVert(World world, int x) {
        if (x < 0 || x >= world.getWidth()) return;
        for (int y = 0; y < world.getHeight(); y++) {
            world.setTile(x, y, world.baseForRow(y));
        }
    }

    private void placeRelicInBand(World world, RelicManager relicManager, Random rand,
                                  int yStart, int yEnd) {
        int width = world.getWidth();
        int height = world.getHeight();
        if (yStart < 0) yStart = 0;
        if (yEnd >= height) yEnd = height - 1;

        for (int attempts = 0; attempts < 2000; attempts++) {
            int x = rand.nextInt(width);
            int y = yStart + rand.nextInt(Math.max(1, yEnd - yStart + 1));
            TileType t = world.getTile(x, y);
            if (t != TileType.SHRINE && t != TileType.RELIC && isWalkable(t)) {
                world.setTile(x, y, TileType.RELIC);
                return;
            }
        }
    }

    private boolean isWalkable(TileType t) {
        switch (t) {
            case TREE:
            case ROCK:
            case CACTUS:
            case RUIN_WALL:
                return false;
            default:
                return true;
        }
    }
}
