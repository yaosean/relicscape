package relicscape;

import java.util.Random;
public class WorldGenerator {

    public void generate(World realm, RelicManager shinies, Random dice) {
        int wide = realm.getWidth();
        int tall = realm.getHeight();
        int forestEnd = tall / 3;
        int desertEnd = 2 * tall / 3;

        for (int loopyY = 0; loopyY < tall; loopyY++) {
            for (int loopyX = 0; loopyX < wide; loopyX++) {
                if (loopyY < forestEnd) {
                    realm.setTile(loopyX, loopyY, TileType.GRASS);
                } else if (loopyY < desertEnd) {
                    realm.setTile(loopyX, loopyY, TileType.SAND);
                } else {
                    realm.setTile(loopyX, loopyY, TileType.RUIN_FLOOR);
                }
            }
        }

        generateClusters(realm, dice, TileType.TREE, 40, 3, 6, 0, forestEnd - 1);
        generateClusters(realm, dice, TileType.ROCK, 18, 2, 4, 0, forestEnd - 1);
        generateClusters(realm, dice, TileType.FLOWER, 35, 2, 5, 0, forestEnd - 1);

        generateClusters(realm, dice, TileType.DUNE, 45, 3, 7, forestEnd, desertEnd - 1);
        generateClusters(realm, dice, TileType.CACTUS, 20, 2, 4, forestEnd, desertEnd - 1);
        generateClusters(realm, dice, TileType.ROCK, 15, 2, 4, forestEnd, desertEnd - 1);

        generateClusters(realm, dice, TileType.RUIN_WALL, 30, 3, 6, desertEnd, tall - 1);
        generateClusters(realm, dice, TileType.RUBBLE, 35, 2, 5, desertEnd, tall - 1);

        carvePathHoriz(realm, tall / 2);
        carvePathVert(realm, wide / 2);

        int shineX = wide / 2;
        int shineY = tall / 2;
        realm.setTile(shineX, shineY, TileType.SHRINE);
        shinies.markShrine(shineX, shineY);

        placeRelicInBand(realm, shinies, dice, 0, forestEnd - 1);
        placeRelicInBand(realm, shinies, dice, forestEnd, desertEnd - 1);
        placeRelicInBand(realm, shinies, dice, desertEnd, tall - 1);
    }

    private void generateClusters(World realm, Random dice, TileType feature, int blobCount,
                                  int minRadius, int maxRadius, int startRow, int endRow) {
        int wide = realm.getWidth();
        int tall = realm.getHeight();
        if (startRow < 0) startRow = 0;
        if (endRow >= tall) endRow = tall - 1;

        for (int blob = 0; blob < blobCount; blob++) {
            int blobX = dice.nextInt(wide);
            int blobY = startRow + dice.nextInt(Math.max(1, endRow - startRow + 1));
            int blobRadius = minRadius + dice.nextInt(Math.max(1, maxRadius - minRadius + 1));

            for (int loopyY = blobY - blobRadius; loopyY <= blobY + blobRadius; loopyY++) {
                for (int loopyX = blobX - blobRadius; loopyX <= blobX + blobRadius; loopyX++) {
                    if (!realm.inBounds(loopyX, loopyY)) continue;
                    double blobbyDist = Math.hypot(loopyX - blobX, loopyY - blobY);
                    if (blobbyDist <= blobRadius && dice.nextDouble() < 0.8) {
                        TileType base = realm.getTile(loopyX, loopyY);
                        if (isCompatible(base, feature)) {
                            realm.setTile(loopyX, loopyY, feature);
                        }
                    }
                }
            }
        }
    }

    private boolean isCompatible(TileType baseTile, TileType guestTile) {
        switch (guestTile) {
            case TREE:
            case ROCK:
            case FLOWER:
                return baseTile == TileType.GRASS;
            case DUNE:
            case CACTUS:
                return baseTile == TileType.SAND;
            case RUIN_WALL:
            case RUBBLE:
                return baseTile == TileType.RUIN_FLOOR;
            default:
                return true;
        }
    }

    private void carvePathHoriz(World realm, int laneY) {
        if (laneY < 0 || laneY >= realm.getHeight()) return;
        for (int laneX = 0; laneX < realm.getWidth(); laneX++) {
            realm.setTile(laneX, laneY, realm.baseForRow(laneY));
        }
    }

    private void carvePathVert(World realm, int laneX) {
        if (laneX < 0 || laneX >= realm.getWidth()) return;
        for (int laneY = 0; laneY < realm.getHeight(); laneY++) {
            realm.setTile(laneX, laneY, realm.baseForRow(laneY));
        }
    }

    private void placeRelicInBand(World realm, RelicManager shinies, Random dice,
                                  int startRow, int endRow) {
        int wide = realm.getWidth();
        int tall = realm.getHeight();
        if (startRow < 0) startRow = 0;
        if (endRow >= tall) endRow = tall - 1;

        for (int tries = 0; tries < 2000; tries++) {
            int dropX = dice.nextInt(wide);
            int dropY = startRow + dice.nextInt(Math.max(1, endRow - startRow + 1));
            TileType tilePeek = realm.getTile(dropX, dropY);
            if (tilePeek != TileType.SHRINE && tilePeek != TileType.RELIC && isWalkable(tilePeek)) {
                realm.setTile(dropX, dropY, TileType.RELIC);
                return;
            }
        }
    }

    private boolean isWalkable(TileType stepTile) {
        switch (stepTile) {
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
