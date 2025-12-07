package relicscape;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
public class TMXMapLoader {

    private final List<Tileset> tilesets = new ArrayList<>();
    private final List<int[][]> visualLayers = new ArrayList<>();
    private final List<BlackTileset> blackTilesets = new ArrayList<>();
    private String baseDir = ".";

    public World load(String tmxPath) {
        try {
            File tmxFile = new File(tmxPath);
            baseDir = tmxFile.getParentFile() != null ? tmxFile.getParentFile().getAbsolutePath() : ".";

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(tmxFile);
            doc.getDocumentElement().normalize();

            Element map = doc.getDocumentElement();
            int width = Integer.parseInt(map.getAttribute("width"));
            int height = Integer.parseInt(map.getAttribute("height"));

            NodeList tsNodes = map.getElementsByTagName("tileset");
            tilesets.clear();
            blackTilesets.clear();
            for (int i = 0; i < tsNodes.getLength(); i++) {
                Element ts = (Element) tsNodes.item(i);
                int firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
                String source = ts.getAttribute("source");
                if (source == null || source.isEmpty()) {
                    continue;
                }
                loadTsx(firstGid, new File(baseDir, source).getAbsolutePath());
            }

            World world = new World(width, height);

            visualLayers.clear();

            NodeList layers = map.getElementsByTagName("layer");
            for (int i = 0; i < layers.getLength(); i++) {
                Element layer = (Element) layers.item(i);
                String name = layer.getAttribute("name");
                String lname = name == null ? "" : name.toLowerCase();
                String dataText = getCsvData(layer);
                if (dataText == null) continue;

                int[][] gids = parseCsvToGrid(dataText, width, height);

                if (lname.equals("collision") || lname.equals("collision 2")) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            if (gids[y][x] != 0) {
                                world.setBlocked(x, y, true);
                            }
                        }
                    }
                } else {
                    visualLayers.add(gids);
                }
            }

            if (!visualLayers.isEmpty()) {
                int[][] base = visualLayers.get(0);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        world.setTileIndex(x, y, base[y][x]);
                    }
                }
            }

            return world;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TMX: " + e.getMessage(), e);
        }
    }

    /** Returns the tile image for a given global ID, or null if none. */
    public java.awt.image.BufferedImage getTileImage(int gid) {
        if (gid <= 0) return null;
        for (int i = tilesets.size() - 1; i >= 0; i--) {
            Tileset ts = tilesets.get(i);
            java.awt.image.BufferedImage img = ts.getTile(gid);
            if (img != null) return img;
        }
        for (BlackTileset bts : blackTilesets) {
            if (bts.contains(gid)) return bts.blackTile;
        }
        return null;
    }

    /** Return all non-collision layers (bottom-to-top). */
    public java.util.List<int[][]> getVisualLayers() {
        return java.util.Collections.unmodifiableList(visualLayers);
    }

    private static class BlackTileset {
        final int firstGid;
        final int tileCount;
        final BufferedImage blackTile;

        BlackTileset(int firstGid, int tileCount, int tileWidth, int tileHeight) {
            this.firstGid = firstGid;
            this.tileCount = tileCount;
            this.blackTile = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = blackTile.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, tileWidth, tileHeight);
            g.dispose();
        }
        boolean contains(int gid) {
            return gid >= firstGid && gid < firstGid + tileCount;
        }
    }

    private void loadTsx(int firstGid, String tsxPath) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new File(tsxPath));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            int tileWidth = Integer.parseInt(root.getAttribute("tilewidth"));
            int tileHeight = Integer.parseInt(root.getAttribute("tileheight"));
            int spacing = parseIntOrDefault(root.getAttribute("spacing"), 0);
            int margin = parseIntOrDefault(root.getAttribute("margin"), 0);
            int columns = parseIntOrDefault(root.getAttribute("columns"), 1);
            int tileCount = parseIntOrDefault(root.getAttribute("tilecount"), columns);
            String tsName = root.getAttribute("name");

            NodeList imageNodes = root.getElementsByTagName("image");
            if (imageNodes.getLength() == 0) return;
            Element image = (Element) imageNodes.item(0);
            String source = image.getAttribute("source");
            String imgPath = new File(new File(tsxPath).getParentFile(), source).getAbsolutePath();

            try {
                tilesets.add(new Tileset(firstGid, imgPath, tileWidth, tileHeight, spacing, margin, columns));
            } catch (RuntimeException ex) {
                String fileName = new File(tsxPath).getName().toLowerCase();
                String nameLower = tsName == null ? "" : tsName.toLowerCase();
                if (fileName.startsWith("960x0") || nameLower.equals("960x0")) {
                    blackTilesets.add(new BlackTileset(firstGid, tileCount, tileWidth, tileHeight));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TSX: " + tsxPath + " - " + e.getMessage(), e);
        }
    }

    private String getCsvData(Element layer) {
        NodeList dataNodes = layer.getElementsByTagName("data");
        if (dataNodes.getLength() == 0) return null;
        Element dataEl = (Element) dataNodes.item(0);
        String encoding = dataEl.getAttribute("encoding");
        if (encoding != null && !encoding.equalsIgnoreCase("csv")) {
            return null;
        }
        return dataEl.getTextContent().trim();
    }

    private int[][] parseCsvToGrid(String csv, int width, int height) {
        String[] tokens = csv.split("\\s*,\\s*");
        int[][] grid = new int[height][width];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx < tokens.length) {
                    try {
                        grid[y][x] = Integer.parseInt(tokens[idx]);
                    } catch (NumberFormatException nfe) {
                        grid[y][x] = 0;
                    }
                } else {
                    grid[y][x] = 0;
                }
                idx++;
            }
        }
        return grid;
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            if (s == null || s.isEmpty()) return def;
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
