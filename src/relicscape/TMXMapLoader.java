package relicscape;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
public class TMXMapLoader {

    private final List<Tileset> tileStacks = new ArrayList<>();
    private final List<int[][]> paintLayers = new ArrayList<>();
    private final List<BlackTileset> shadowStacks = new ArrayList<>();
    private final java.util.Map<String, int[][]> petLayers = new java.util.HashMap<>();
    private boolean[][] nopeGrid;
    private String homeNest = ".";
    private int[][] baseVisualLayer;

    public World load(String tmxPath) {
        try {
            String normalized = normalizePath(tmxPath);
            homeNest = dirName(normalized);
            try (InputStream tmxStream = openStream(normalized)) {
                if (tmxStream == null) {
                    throw new RuntimeException("Missing TMX resource or file: " + tmxPath);
                }
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(tmxStream);
                doc.getDocumentElement().normalize();

                Element map = doc.getDocumentElement();
                int width = Integer.parseInt(map.getAttribute("width"));
                int height = Integer.parseInt(map.getAttribute("height"));

                NodeList tsNodes = map.getElementsByTagName("tileset");
                tileStacks.clear();
                shadowStacks.clear();
                for (int i = 0; i < tsNodes.getLength(); i++) {
                    Element ts = (Element) tsNodes.item(i);
                    int firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
                    String source = ts.getAttribute("source");
                    if (source == null || source.isEmpty()) {
                        continue;
                    }
                    String resolved = resolvePath(homeNest, source);
                    loadTsx(firstGid, resolved);
                }

                World world = new World(width, height);

                paintLayers.clear();
                petLayers.clear();
                nopeGrid = null;
                baseVisualLayer = null;

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
                    } else if (lname.equals("nospawn")) {
                        ensureNoSpawn(width, height);
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                if (gids[y][x] != 0) {
                                    nopeGrid[y][x] = true;
                                }
                            }
                        }
                    } else {
                        paintLayers.add(gids);
                        if (baseVisualLayer == null) {
                            baseVisualLayer = gids;
                        }
                        if(lname != null && !lname.isEmpty()){
                            petLayers.put(lname, gids);
                        }
                    }
                }

                if (!paintLayers.isEmpty()) {
                    int[][] base = paintLayers.get(0);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            world.setTileIndex(x, y, base[y][x]);
                        }
                    }
                }

                return world;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TMX: " + e.getMessage(), e);
        }
    }

    /** Returns the tile image for a given global ID, or null if none. */
    public java.awt.image.BufferedImage getTileImage(int gid) {
        if (gid <= 0) return null;
        for (int i = tileStacks.size() - 1; i >= 0; i--) {
            Tileset ts = tileStacks.get(i);
            java.awt.image.BufferedImage img = ts.getTile(gid);
            if (img != null) return img;
        }
        for (BlackTileset bts : shadowStacks) {
            if (bts.contains(gid)) return bts.blackTile;
        }
        return null;
    }

    /** Return all non-collision layers (bottom-to-top). */
    public java.util.List<int[][]> getVisualLayers() {
        return java.util.Collections.unmodifiableList(paintLayers);
    }

    /** Return the grid for a named layer (case-insensitive), or null if missing. */
    public int[][] getLayer(String name){
        if(name==null) return null;
        return petLayers.get(name.toLowerCase());
    }

    public void removeTileFromTilesetLayers(int x, int y) {
        if(x < 0 || y < 0) return;
        for(int[][] layer : paintLayers){
            if(layer == null || layer == baseVisualLayer) continue;
            if(y >= layer.length || x >= layer[0].length) continue;
            layer[y][x] = 0;
        }
        for(int[][] layer : petLayers.values()){
            if(layer == null || layer == baseVisualLayer) continue;
            if(y >= layer.length || x >= layer[0].length) continue;
            layer[y][x] = 0;
        }
    }

    /** True if the named layer has a non-zero tile at (x,y). */
    public boolean hasTile(String name,int x,int y){
        int[][] grid = getLayer(name);
        if(grid==null) return false;
        if(y < 0 || y >= grid.length || x < 0 || x >= grid[0].length) return false;
        return grid[y][x] != 0;
    }

    public boolean isNoSpawn(int x, int y) {
        if (nopeGrid == null) return false;
        if (y < 0 || y >= nopeGrid.length || x < 0 || x >= nopeGrid[0].length) return false;
        return nopeGrid[y][x];
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
        try (InputStream tsxStream = openStream(tsxPath)) {
            if (tsxStream == null) {
                throw new RuntimeException("Missing TSX resource or file: " + tsxPath);
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(tsxStream);
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
            String imgPath = resolvePath(dirName(tsxPath), source);

            try {
                tileStacks.add(new Tileset(firstGid, imgPath, tileWidth, tileHeight, spacing, margin, columns));
            } catch (RuntimeException ex) {
                String fileName = baseName(tsxPath).toLowerCase();
                String nameLower = tsName == null ? "" : tsName.toLowerCase();
                if (fileName.startsWith("960x0") || nameLower.equals("960x0")) {
                    shadowStacks.add(new BlackTileset(firstGid, tileCount, tileWidth, tileHeight));
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

    private InputStream openStream(String path) {
        String normalized = normalizePath(path);
        ClassLoader cl = TMXMapLoader.class.getClassLoader();
        InputStream in = cl.getResourceAsStream(normalized.startsWith("/") ? normalized.substring(1) : normalized);
        if (in != null) return in;
        try {
            File f = normalized.startsWith("/") ? new File(normalized) : new File(normalized);
            if (f.exists()) {
                return new FileInputStream(f);
            }
        } catch (Exception ignored) { }
        try {
            File f = new File(homeNest, normalized);
            if (f.exists()) {
                return new FileInputStream(f);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String normalizePath(String p) {
        if (p == null) return "";
        return p.replace('\\', '/');
    }

    private String dirName(String path) {
        String norm = normalizePath(path);
        int slash = norm.lastIndexOf('/');
        if (slash < 0) return "";
        return norm.substring(0, slash);
    }

    private String baseName(String path) {
        String norm = normalizePath(path);
        int slash = norm.lastIndexOf('/');
        return slash >= 0 ? norm.substring(slash + 1) : norm;
    }

    private String resolvePath(String baseDir, String rel) {
        if (rel == null || rel.isEmpty()) return normalizePath(baseDir);
        if (rel.startsWith("/") || new File(rel).isAbsolute()) return normalizePath(rel);
        if (baseDir == null || baseDir.isEmpty()) return normalizePath(rel);
        return normalizePath(baseDir + "/" + rel);
    }

    private void ensureNoSpawn(int width, int height) {
        if (nopeGrid == null) {
            nopeGrid = new boolean[height][width];
        }
    }
}
