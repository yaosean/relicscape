package relicscape;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Loads a single map artwork and optional mask, splits it into base and overlay
 * layers to allow depth occlusion and provides a simple walkability mask.
 *
 * Expected filenames (in the same Texture directory used by TextureManager):
 * - `map_art.png` : the full artwork image
 * - `map_mask.png`: optional mask where white=walkable, black=blocked, gray=partial
 */
public class MapManager {

    private final BufferedImage art;
    private final BufferedImage mask;
    private final BufferedImage baseLayer;
    private final BufferedImage overlayLayer;
    private final int widthPx;
    private final int heightPx;
    private boolean[][] walkableGrid = null;

    public MapManager(TextureManager tex) {
        this.art = tex.getRawImage("map_art.png");
        BufferedImage loadedMask = tex.getRawImage("map_mask.png");

        if (art == null) {
            widthPx = 0; heightPx = 0; baseLayer = null; overlayLayer = null; this.mask = null; return;
        }

        widthPx = art.getWidth();
        heightPx = art.getHeight();

        // Generate mask if missing
        if (loadedMask == null) {
            this.mask = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < heightPx; y++) {
                for (int x = 0; x < widthPx; x++) {
                    int argb = art.getRGB(x, y);
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = argb & 0xff;
                    int brightness = (r + g + b) / 3;
                    int color = brightness > 110 ? 0xFFFFFF : 0x000000; // white = walkable, black = blocked
                    this.mask.setRGB(x, y, color);
                }
            }
            // Save the generated mask
            try {
                File maskFile = new File("Texture/map_mask.png");
                maskFile.getParentFile().mkdirs();
                ImageIO.write(this.mask, "png", maskFile);
            } catch (IOException e) {
                // Ignore
            }
        } else {
            this.mask = loadedMask;
        }

        // Build overlay by selecting darker pixels (likely walls/objects)
        overlayLayer = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
        baseLayer = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < heightPx; y++) {
            for (int x = 0; x < widthPx; x++) {
                int argb = art.getRGB(x, y);
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                int brightness = (r + g + b) / 3;

                // If a mask exists, prefer mask for occlusion/walkability
                boolean overlayPixel = false;
                if (mask != null) {
                    int m = mask.getRGB(x, y);
                    int mm = (((m >> 16) & 0xff) + ((m >> 8) & 0xff) + (m & 0xff)) / 3;
                    // mask: darker = blocked/occluding
                    overlayPixel = mm < 128;
                } else {
                    // heuristic: dark pixels (stone/wall shadows) are overlay
                    overlayPixel = brightness < 110;
                }

                if (overlayPixel) {
                    overlayLayer.setRGB(x, y, argb);
                    // make base transparent here
                    baseLayer.setRGB(x, y, 0);
                } else {
                    baseLayer.setRGB(x, y, argb);
                    overlayLayer.setRGB(x, y, 0);
                }
            }
        }
    }

    /**
     * Build a walkability grid aligned to the game's world tile coordinates.
     * Each grid cell corresponds to one world tile; if a mask exists it is used,
     * otherwise brightness is used. This creates an invisible tilemap for movement.
     */
    public void buildWalkableGrid(int worldWidth, int worldHeight, int tileSize, int threshold) {
        if (art == null) return;
        walkableGrid = new boolean[worldWidth][worldHeight];

        double scaleX = (double) widthPx / (worldWidth * tileSize);
        double scaleY = (double) heightPx / (worldHeight * tileSize);

        for (int wy = 0; wy < worldHeight; wy++) {
            for (int wx = 0; wx < worldWidth; wx++) {
                // sample a block of pixels in the art corresponding to this tile
                int cx = (int) ((wx * tileSize + tileSize / 2) * scaleX);
                int cy = (int) ((wy * tileSize + tileSize / 2) * scaleY);
                // clamp
                if (cx < 0) cx = 0; if (cy < 0) cy = 0;
                if (cx >= widthPx) cx = widthPx - 1; if (cy >= heightPx) cy = heightPx - 1;

                boolean walkable;
                if (mask != null) {
                    int m = mask.getRGB(cx, cy);
                    int r = (m >> 16) & 0xff;
                    walkable = r > 128;
                } else {
                    int a = art.getRGB(cx, cy);
                    int r = (a >> 16) & 0xff;
                    int g = (a >> 8) & 0xff;
                    int b = a & 0xff;
                    int brightness = (r + g + b) / 3;
                    walkable = brightness > threshold;
                }

                walkableGrid[wx][wy] = walkable;
            }
        }
    }

    /**
     * Query the precomputed walkable grid. Returns true if the tile is walkable.
     * If the grid is not built, falls back to sampling the art directly.
     */
    public boolean isWalkableTile(int worldX, int worldY) {
        if (walkableGrid != null) {
            if (worldX < 0 || worldY < 0 || worldX >= walkableGrid.length || worldY >= walkableGrid[0].length)
                return false;
            return walkableGrid[worldX][worldY];
        }
        // fallback: sample art directly using previous heuristic
        return isWalkable(worldX, worldY, 80, 45, 20);
    }

    public boolean hasMap() {
        return art != null;
    }

    public int getMapWidth() { return widthPx; }
    public int getMapHeight() { return heightPx; }

    /**
     * Check walkability at a world tile coordinate. We sample the mask (if present)
     * or the brightness of the art at the center of the tile. Returns true if walkable.
     */
    public boolean isWalkable(int worldX, int worldY, int worldWidth, int worldHeight, int tileSize) {
        if (art == null) return true;
        // Map logical world to art pixel coordinates by scaling
        double scaleX = (double) widthPx / (worldWidth * tileSize);
        double scaleY = (double) heightPx / (worldHeight * tileSize);
        int px = (int) ((worldX * tileSize + tileSize / 2) * scaleX);
        int py = (int) ((worldY * tileSize + tileSize / 2) * scaleY);
        if (px < 0 || py < 0 || px >= widthPx || py >= heightPx) return false;

        if (mask != null) {
            int c = mask.getRGB(px, py);
            int r = (c >> 16) & 0xff;
            return r > 128;
        }

        int c = art.getRGB(px, py);
        int r = (c >> 16) & 0xff;
        int g = (c >> 8) & 0xff;
        int b = c & 0xff;
        int brightness = (r + g + b) / 3;
        // treat darker pixels as blocking
        return brightness > 110;
    }

    /**
     * Draw the base (under) layer portion for the view window.
     * Uses the precomputed `baseLayer` which respects the mask (white = walkable),
     * so only ground / castle floor pixels are drawn here. The mask is authoritative
     * (black pixels = barriers/walls/trees, white = walkable).
     */
    public void drawBase(Graphics2D g2, float viewLeft, float viewTop, int viewWidth, int viewHeight,
                         int tileSize, int topMargin) {
        if (art == null || baseLayer == null) return;
        // target rectangle in pixels
        int dx1 = 0;
        int dy1 = topMargin;
        int dx2 = viewWidth * tileSize;
        int dy2 = topMargin + viewHeight * tileSize;

        // source rectangle from baseLayer (aligned with art)
        int sx1 = (int) (viewLeft * tileSize);
        int sy1 = (int) (viewTop * tileSize);
        int sx2 = (int) ((viewLeft + viewWidth) * tileSize);
        int sy2 = (int) ((viewTop + viewHeight) * tileSize);

        // clamp to image size
        if (sx1 < 0) sx1 = 0;
        if (sy1 < 0) sy1 = 0;
        if (sx2 > widthPx) sx2 = widthPx;
        if (sy2 > heightPx) sy2 = heightPx;

        g2.drawImage(baseLayer, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
    }

    /**
     * Draw overlay in two parts split by player's screen Y so occlusion works: first draw overlay
     * pixels with screen Y < playerScreenY (back overlay), then later the remaining overlay pixels
     * will be drawn after the player to occlude them.
     */
    public void drawOverlayPortion(Graphics2D g2, float viewLeft, float viewTop, int viewWidth, int viewHeight,
                                   int tileSize, int topMargin, int playerScreenY, boolean drawTopPart) {
        if (art == null) return;

        // draw scaled overlay portion to view and then clip to top/bottom depending on drawTopPart
        int dx1 = 0;
        int dy1 = topMargin;
        int dx2 = viewWidth * tileSize;
        int dy2 = topMargin + viewHeight * tileSize;

        int sx1 = (int) (viewLeft * tileSize);
        int sy1 = (int) (viewTop * tileSize);
        int sx2 = (int) ((viewLeft + viewWidth) * tileSize);
        int sy2 = (int) ((viewTop + viewHeight) * tileSize);

        // clamp
        if (sx1 < 0) sx1 = 0;
        if (sy1 < 0) sy1 = 0;
        if (sx2 > widthPx) sx2 = widthPx;
        if (sy2 > heightPx) sy2 = heightPx;

        // create a clip region: for top part draw area from dy1..playerScreenY-1, for bottom part draw playerScreenY..dy2
        Shape prevClip = g2.getClip();
        if (drawTopPart) {
            g2.setClip(0, dy1, viewWidth * tileSize, playerScreenY - dy1);
        } else {
            g2.setClip(0, playerScreenY, viewWidth * tileSize, dy2 - playerScreenY);
        }

        g2.drawImage(overlayLayer, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        g2.setClip(prevClip);
    }
}
