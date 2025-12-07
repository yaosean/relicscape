package relicscape;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
public class Tileset {
    public final int firstTid;
    public final BufferedImage bigPic;
    public final int chunkWide;
    public final int chunkTall;
    public final int gapPix;
    public final int edgePix;
    public final int colCount;

    public Tileset(int firstGid,
                   String imagePath,
                   int tileWidth,
                   int tileHeight,
                   int spacing,
                   int margin,
                   int columns) {
        this.firstTid = firstGid;
        this.chunkWide = tileWidth;
        this.chunkTall = tileHeight;
        this.gapPix = spacing;
        this.edgePix = margin;
        this.colCount = columns;
        this.bigPic = grabImageLoose(imagePath);
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
                BufferedImage imgGuess = ImageIO.read(f);
                if (imgGuess != null) return imgGuess;
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
        int localId = gid - firstTid;
        if (localId < 0) return null;

        int col = localId % colCount;
        int row = localId / colCount;

        int x = edgePix + col * (chunkWide + gapPix);
        int y = edgePix + row * (chunkTall + gapPix);

        if (x + chunkWide > bigPic.getWidth() || y + chunkTall > bigPic.getHeight()) {
            return null;
        }
        return bigPic.getSubimage(x, y, chunkWide, chunkTall);
    }
}
