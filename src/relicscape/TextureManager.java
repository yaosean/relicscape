package relicscape;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads external texture images (tilesets/props) and provides scaled
 * images for `TileType` rendering. It attempts to locate a `Texture`
 * folder in common sibling locations so your uploaded assets are used.
 */
public class TextureManager {

    private final Map<String, BufferedImage> raw = new HashMap<>();

    public TextureManager() {
        File texDir = findTextureDirectory();
        if (texDir != null) {
            loadDirectory(texDir);
        }
    }

    private File findTextureDirectory() {
        String cwd = System.getProperty("user.dir");
        String[] candidates = new String[] {
            cwd + File.separator + "Texture",
            cwd + File.separator + "Texture" + File.separator + "Extra",
            cwd + File.separator + ".." + File.separator + "Pixel Art Top Down - Basic v1.2.2" + File.separator + "Texture",
            cwd + File.separator + ".." + File.separator + "Pixel Art Top Down - Basic v1.2.2" + File.separator + "Texture" + File.separator + "Extra",
            cwd + File.separator + ".." + File.separator + "Pixel Art Top Down - Basic v1.2.2-20251202T185846Z-1-001" + File.separator + "Pixel Art Top Down - Basic v1.2.2" + File.separator + "Texture",
            cwd + File.separator + ".." + File.separator + "Pixel Art Top Down - Basic v1.2.2-20251202T185846Z-1-001" + File.separator + "Pixel Art Top Down - Basic v1.2.2" + File.separator + "Texture" + File.separator + "Extra",
        };

        for (String p : candidates) {
            File f = new File(p);
            if (f.exists() && f.isDirectory()) return f;
        }

        // Fall back to system property if user explicitly set it
        String prop = System.getProperty("relicscape.textures.dir");
        if (prop != null) {
            File f = new File(prop);
            if (f.exists() && f.isDirectory()) return f;
        }

        return null;
    }

    private void loadDirectory(File dir) {
        File[] files = dir.listFiles((d, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
        });
        if (files == null) return;
        for (File f : files) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) raw.put(f.getName(), img);
            } catch (IOException ignored) {
            }
        }
        if (!raw.isEmpty()) {
            System.out.println("TextureManager: loaded textures -> " + raw.keySet());
        } else {
            System.out.println("TextureManager: no textures found in " + dir.getAbsolutePath());
        }
    }

    private BufferedImage getRaw(String name) {
        if (raw.containsKey(name)) return raw.get(name);
        // try case-insensitive match
        for (String k : raw.keySet()) {
            if (k.equalsIgnoreCase(name)) return raw.get(k);
        }
        return null;
    }

    /**
     * Public accessor for raw loaded images by filename. Returns null if not found.
     */
    public BufferedImage getRawImage(String name) {
        return getRaw(name);
    }

    private BufferedImage centerCrop(BufferedImage src) {
        int s = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - s) / 2;
        int y = (src.getHeight() - s) / 2;
        return src.getSubimage(x, y, s, s);
    }

    private BufferedImage scaleToTile(BufferedImage src, int tileSize) {
        BufferedImage dst = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, tileSize, tileSize, null);
        g.dispose();
        return dst;
    }

    /**
     * Return a scaled image suitable for filling a tile background or drawing an object.
     * If no matching texture is found, returns null so callers can fall back to color/glyphs.
     */
    public BufferedImage getTextureFor(TileType type, int tileSize) {
        // Prefer specific files if present
        String[] tryNames;
        switch (type) {
            case GRASS:
                tryNames = new String[]{"TX Tileset Grass.png", "TX Tileset Stone Ground.png"};
                break;
            case SAND:
            case DUNE:
                tryNames = new String[]{"TX Tileset Stone Ground.png", "TX Tileset Grass.png"};
                break;
            case RUIN_FLOOR:
            case RUIN_WALL:
            case RUBBLE:
                tryNames = new String[]{"TX Struct.png", "TX Tileset Wall.png", "TX Tileset Stone Ground.png"};
                break;
            case TREE:
            case FLOWER:
            case CACTUS:
                tryNames = new String[]{"TX Plant with Shadow.png", "TX Plant.png", "TX Props with Shadow.png", "TX Props.png"};
                break;
            case ROCK:
                tryNames = new String[]{"TX Tileset Stone Ground.png", "TX Props.png"};
                break;
            case RELIC:
            case SHRINE:
                tryNames = new String[]{"TX Props.png", "TX Props with Shadow.png"};
                break;
            default:
                tryNames = new String[]{"TX Props.png"};
        }

        for (String n : tryNames) {
            BufferedImage r = getRaw(n);
            if (r != null) {
                BufferedImage cropped = centerCrop(r);
                return scaleToTile(cropped, tileSize);
            }
        }

        return null;
    }
}
