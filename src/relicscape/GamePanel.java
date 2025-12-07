package relicscape;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * GamePanel:
 * - Handles keyboard input
 * - Updates player + encounters
 * - Renders the world, HUD, and win/lose overlay
 *
 * Visual goals:
 * - Smooth biome gradients
 * - Ground = pure color (no clutter)
 * - Clear, iconic glyphs for objects only
 */
public class GamePanel extends JPanel implements KeyListener {

    // ===================== CONSTANTS =====================

    // Tile grid
    private static final int TILE_SIZE   = 96;   // desired tile size; actual may clamp based on window

    private static final int HUD_HEIGHT      = 80;
    private static final int FRAME_DELAY_MS  = 33; // ~30 FPS
    private static final double CLEAR_RADIUS_TILES = 1.0; // fully visible within this radius
    private static final double BLACK_RADIUS_TILES = 5.0; // pure black at and beyond this radius
    private static final double FOG_REVEAL_RADIUS_TILES = 4.5; // how far we permanently reveal
    private static final double FOG_MAX_RADIUS_TILES    = 11.5; // how far current vision softly fades to dark
    private static final int WORLD_TOP_MARGIN = 30;
    private static final long CORRUPTION_DURATION_MS = 10 * 60 * 1000L; // 10 minutes to fully tint

    // ===================== STATE =========================

    private final World world;
    private TMXMapLoader tmxLoader;
    private final Player player;
    private final RelicManager relicManager;
    private final EncounterSystem encounterSystem;
    private final WeatherSystem weatherSystem;
    private final Random rand = new Random();
    private long lastMoveMs = 0L;
    private static final long MOVE_COOLDOWN_MS = 200; // 2x slower movement

    private String  lastMessage =
            "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver = false;
    private boolean gameWon  = false;

        private final long corruptionStartMs = System.currentTimeMillis();

    private boolean[][] discovered; // fog-of-war memory: [y][x]

    

    private final Timer timer;

    // ===================== CONSTRUCTOR ===================

    public GamePanel() {
        setPreferredSize(new Dimension(960, 720));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Load TMX map and keep a handle to tilesets for rendering
        TMXMapLoader loader = new TMXMapLoader();
        world = loader.load("images/dreams.tmx");
        this.tmxLoader = loader;

        relicManager = new RelicManager(0); // disable relics in TMX mode

        // Spawn near the center on a free tile
        int spawnX = Math.max(1, world.getWidth() / 2);
        int spawnY = Math.max(1, world.getHeight() / 2);
        // Find nearest non-blocked tile by expanding radius
        boolean found = false;
        for (int r = 0; r < Math.max(world.getWidth(), world.getHeight()); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int cx = spawnX + dx;
                    int cy = spawnY + dy;
                    if (!world.inBounds(cx, cy)) continue;
                    if (!world.isBlocked(cx, cy)) { spawnX = cx; spawnY = cy; found = true; break; }
                }
                if (found) break;
            }
            if (found) break;
        }
        player          = new Player(spawnX, spawnY, 10);
        encounterSystem = new EncounterSystem(rand);
        weatherSystem   = new WeatherSystem(rand);

        initializeFog();

        timer = new Timer(FRAME_DELAY_MS, e -> repaint());
        timer.start();
    }

    // ===================== INPUT =========================

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) return;

        long now = System.currentTimeMillis();
        boolean isMoveKey = (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP ||
                             e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN ||
                             e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT ||
                             e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT);
        if (isMoveKey && (now - lastMoveMs) < MOVE_COOLDOWN_MS) {
            return; // enforce slower movement
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                tryMove(0, -1);
                lastMoveMs = now;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                tryMove(0, 1);
                lastMoveMs = now;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                tryMove(-1, 0);
                lastMoveMs = now;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                tryMove(1, 0);
                lastMoveMs = now;
                break;
            case KeyEvent.VK_H:
                lastMessage = "WASD / arrows to move. Find 3 relics (✶) and return to the shrine (⌘).";
                break;
            case KeyEvent.VK_ESCAPE:
                lastMessage = "Press ESC again to quit.";
                if (player.pendingQuit) {
                    System.exit(0);
                }
                player.pendingQuit = true;
                return;
            default:
                return;
        }

        player.pendingQuit = false;
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }

    // ===================== GAME LOGIC ====================

    private void tryMove(int dx, int dy) {
        long now = System.currentTimeMillis();
        if (now - lastMoveMs < MOVE_COOLDOWN_MS) {
            return;
        }
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (!world.inBounds(newX, newY)) {
            lastMessage = "You feel the edge of the world.";
            return;
        }

        if (world.isBlocked(newX, newY)) {
            lastMessage = "You can't move through that.";
            return;
        }

        player.setPosition(newX, newY);
        lastMoveMs = now;

        revealAround(newX, newY);

        // Encounters disabled in TMX mode

        if (!gameOver) {
            checkWinCondition();
        }
    }

    private boolean isWalkable(TileType t) { return true; }

    private void resolveTile(TileType tile) {
        if (tile == TileType.RELIC) {
            relicManager.collectRelic();
            world.setTile(player.getX(), player.getY(), world.baseForRow(player.getY()));
            lastMessage = "You found a relic fragment! (" +
                    relicManager.getRelicsCollected() + "/" + relicManager.getRelicsToCollect() + ")";
        } else if (tile == TileType.SHRINE) {
            if (relicManager.hasAllRelics()) {
                gameWon = true;
                lastMessage = "Light erupts from the shrine as the land begins to heal.";
            } else {
                lastMessage = "The shrine hums softly. It needs more relics.";
            }
        } else if (tile == TileType.DUNE) {
            lastMessage = "Your feet sink in the shifting sand.";
        } else if (tile == TileType.RUBBLE) {
            lastMessage = "Broken stones crunch underfoot.";
        } else {
            lastMessage = "You move onward.";
        }
    }

    private void checkWinCondition() {
        // Overlay is drawn in paintComponent when gameWon is true.
    }

    // ===================== RENDERING =====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int availableWidth = getWidth();
        int availableHeight = getHeight();

        int hudSpace = HUD_HEIGHT;
        int topMargin = WORLD_TOP_MARGIN;

        // Keep tiles readable: clamp tile size to a reasonable band (bigger tiles)
        int tileSize = Math.max(72, Math.min(144, TILE_SIZE));

        int usableHeight = Math.max(0, availableHeight - hudSpace - topMargin);
        int viewWidthTiles = Math.max(1, (int)Math.ceil(availableWidth / (double) tileSize));
        int viewHeightTiles = Math.max(1, (int)Math.ceil(usableHeight / (double) tileSize));

        // Camera centered on player, clamped to world
        int viewLeft = (int)Math.round(player.getX() - (viewWidthTiles - 1) / 2.0);
        int viewTop  = (int)Math.round(player.getY() - (viewHeightTiles - 1) / 2.0);

        if (viewLeft < 0) viewLeft = 0;
        if (viewTop  < 0) viewTop  = 0;
        if (viewLeft + viewWidthTiles  > world.getWidth())
            viewLeft = world.getWidth() - viewWidthTiles;
        if (viewTop + viewHeightTiles > world.getHeight())
            viewTop = world.getHeight() - viewHeightTiles;

        // --- World tiles from TMX (all non-collision layers) ---
        for (int y = 0; y < viewHeightTiles; y++) {
            int worldY = viewTop + y;
            if (worldY < 0 || worldY >= world.getHeight()) continue;

            for (int x = 0; x < viewWidthTiles; x++) {
                int worldX = viewLeft + x;
                if (worldX < 0 || worldX >= world.getWidth()) continue;

                boolean blocked = world.isBlocked(worldX, worldY);
                boolean isPlayerHere = (worldX == player.getX() && worldY == player.getY());

                int px = x * tileSize;
                int py = WORLD_TOP_MARGIN + y * tileSize;

                double corruptionStrength = computeCorruptionStrength(worldX, worldY);
                drawTile(g2, blocked, px, py, isPlayerHere, worldX, worldY, tileSize, corruptionStrength);

                // Fog-of-war overlay per tile
                int fogAlpha = fogAlphaForTile(worldX, worldY);
                if (fogAlpha > 0) {
                    g2.setColor(new Color(0, 0, 0, fogAlpha));
                    g2.fillRect(px, py, tileSize, tileSize);
                }

            }
        }

        // Tile-level fog applied above; no additional radial overlay

        // --- HUD ---
        drawHud(g2, tileSize, viewHeightTiles);

        // --- Win / lose overlay ---
        if (gameOver || gameWon) {
            drawGameOverOverlay(g2);
        }
    }

    private void drawHud(Graphics2D g2, int tileSize, int viewHeightTiles) {
        int hudY = WORLD_TOP_MARGIN + viewHeightTiles * tileSize + 25;

        g2.setColor(new Color(10, 10, 10, 230));
        g2.fillRoundRect(10, hudY - 20, getWidth() - 20, 60, 15, 15);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.drawString("HP: " + player.getHp() +
                      "   Relics: " + relicManager.getRelicsCollected() + "/" +
                      relicManager.getRelicsToCollect(), 20, hudY);

        g2.setFont(new Font("Consolas", Font.PLAIN, 13));
        Util.drawWrappedText(g2, lastMessage, 20, hudY + 18, getWidth() - 40);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        String msg = gameWon ? "YOU RESTORED THE WORLD" : "YOU FELL IN THE RUINS";

        g2.setFont(new Font("Consolas", Font.BOLD, 26));
        g2.setColor(new Color(255, 255, 255, 230));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int th = fm.getAscent();

        int cx = getWidth() / 2 - tw / 2;
        int cy = getHeight() / 2 - th / 2;
        g2.drawString(msg, cx, cy);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        String sub = "Press ESC to quit.";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, getWidth() / 2 - sw / 2, cy + 30);
    }

    /**
     * Draw a single tile:
     * - Background: biome gradient
     * - Glyph: only for objects (trees, ruins, relics, etc.)
     */
    private void drawTile(Graphics2D g2, boolean blocked, int px, int py,
                          boolean isPlayerHere, int worldX, int worldY, int tileSize, double corruptionStrength) {

        // Fallback background
        Color bg = new Color(20, 25, 28);
        g2.setColor(bg);
        g2.fillRect(px, py, tileSize, tileSize);

        // Draw all non-collision visual layers from bottom to top
        if (tmxLoader != null) {
            java.util.List<int[][]> layers = tmxLoader.getVisualLayers();
            for (int l = 0; l < layers.size(); l++) {
                int[][] layer = layers.get(l);
                int gid = layer[worldY][worldX];
                if (gid <= 0) continue;
                java.awt.image.BufferedImage img = tmxLoader.getTileImage(gid);
                if (img != null) {
                    g2.drawImage(img, px, py, tileSize, tileSize, null);
                }
            }
        }

        // Corruption tint (purple) applied per tile based on corruption strength
        if (corruptionStrength > 0.01) {
            int alpha = (int) Math.min(230, Math.round(230 * corruptionStrength));
            // Darker, bleaker purple tint
            g2.setColor(new Color(80, 40, 120, alpha));
            g2.fillRect(px, py, tileSize, tileSize);
        }

        // Collision invisible per request

        // Player rectangle
        if (isPlayerHere) {
            g2.setColor(new Color(240, 240, 255));
            int inset = Math.max(4, tileSize / 8);
            g2.fillRect(px + inset, py + inset, tileSize - inset * 2, tileSize - inset * 2);
        }

        // Weather disabled for clarity
    }

    // --- Fog of war helpers ---

    private void initializeFog() {
        discovered = new boolean[world.getHeight()][world.getWidth()];
        revealAround(player.getX(), player.getY());
    }

    private void revealAround(int cx, int cy) {
        if (discovered == null) return;
        int r = (int) Math.ceil(FOG_REVEAL_RADIUS_TILES);
        double r2 = FOG_REVEAL_RADIUS_TILES * FOG_REVEAL_RADIUS_TILES;
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (!world.inBounds(x, y)) continue;
                double dx = x - cx;
                double dy = y - cy;
                if (dx * dx + dy * dy <= r2 + 0.25) {
                    discovered[y][x] = true;
                }
            }
        }
    }

    private boolean isDiscovered(int x, int y) {
        return discovered != null && y >= 0 && y < discovered.length && x >= 0 && x < discovered[0].length && discovered[y][x];
    }

    private int fogAlphaForTile(int worldX, int worldY) {
        if (!isDiscovered(worldX, worldY)) {
            return 235; // very dark, but keep a hint of gradient softening
        }

        double dist = Math.hypot(worldX - player.getX(), worldY - player.getY());

        if (dist <= CLEAR_RADIUS_TILES) return 0;

        double span = Math.max(1e-3, (FOG_MAX_RADIUS_TILES - CLEAR_RADIUS_TILES));
        double t = (dist - CLEAR_RADIUS_TILES) / span;
        if (t > 1.0) t = 1.0;
        if (t < 0.0) t = 0.0;

        double smooth = t * t * (3 - 2 * t); // smoothstep
        int baseAlpha = 18;   // softer penumbra right after the clear core
        int maxAlpha = 170;   // explored-but-distant stays dim without going pitch black
        return (int) Math.round(baseAlpha + (maxAlpha - baseAlpha) * smooth);
    }

    private double computeCorruptionStrength(int worldX, int worldY) {
        long elapsed = System.currentTimeMillis() - corruptionStartMs;
        double progress = Math.min(1.0, elapsed / (double) CORRUPTION_DURATION_MS);

        // Bottom-up bias: tiles lower in the map corrupt sooner
        double heightBias = 1.0 - (worldY / Math.max(1.0, (world.getHeight() - 1)));

        // Deterministic noise per tile for organic spread
        double noise = (hash(worldX, worldY) % 1000) / 1000.0; // 0..0.999
        double jitter = (noise - 0.5) * 0.25; // +/-0.125

        double threshold = Math.max(0.0, Math.min(1.0, heightBias + jitter));

        // Soften ramp: corruption rises when progress exceeds threshold
        double spread = 0.25; // wider spread for gradual takeover
        double strength = (progress - threshold) / spread;
        if (strength < 0) strength = 0;
        if (strength > 1) strength = 1;
        return strength;
    }

    private int hash(int x, int y) {
        int h = x * 73428767 ^ y * 9122713;
        h ^= (h << 13);
        h ^= (h >>> 17);
        h ^= (h << 5);
        return h & 0x7fffffff;
    }

    /**
     * Smooth gradient background per biome band.
     */
    private Color computeBiomeBackground(TileType base, int worldY) {
        return new Color(20, 25, 28);
    }
}
