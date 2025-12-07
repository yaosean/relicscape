package relicscape;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
public class Tileset {
    public final int firstGid;
    public final BufferedImage image;
    public final int tileWidth;
    public final int tileHeight;
    public final int spacing;
    public final int margin;
    public final int columns;

    public Tileset(int firstGid,
                   String imagePath,
                   int tileWidth,
                   int tileHeight,
                   int spacing,
                   int margin,
                   int columns) {
        this.firstGid = firstGid;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.spacing = spacing;
        this.margin = margin;
        this.columns = columns;
        this.image = grabImageLoose(imagePath);
    }

    private BufferedImage grabImageLoose(String imagePath) {
        java.util.LinkedHashSet<String> options = new java.util.LinkedHashSet<>();
        options.add(imagePath);

        int dot = imagePath.lastIndexOf('.');
        if (dot > 0) {
            String base = imagePath.substring(0, dot);
            String ext = imagePath.substring(dot + 1).toLowerCase();
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                options.add(base + ".png");
            } else if (ext.equals("png")) {
                options.add(base + ".jpg");
                options.add(base + ".jpeg");
            }
        }

        Exception last = null;
        for (String cand : options) {
            try {
                File f = new File(cand);
                if (!f.exists()) continue;
                BufferedImage img = ImageIO.read(f);
                if (img != null) return img;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("Couldn't read tileset image: " + imagePath, last);
    }

    /**
     * Returns the subimage for a given global tile id, or null if not within this tileset.
     */
    public BufferedImage getTile(int gid) {
        int localId = gid - firstGid;
        if (localId < 0) return null;

        int col = localId % columns;
        int row = localId / columns;

        int x = margin + col * (tileWidth + spacing);
        int y = margin + row * (tileHeight + spacing);

        if (x + tileWidth > image.getWidth() || y + tileHeight > image.getHeight()) {
            return null;
        }
        return image.getSubimage(x, y, tileWidth, tileHeight);
    }
}
