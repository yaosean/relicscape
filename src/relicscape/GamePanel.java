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

    // Tile grid (viewport shows small portion of the map)
    private static final int TILE_SIZE   = 32;
    private static final int VIEW_WIDTH  = 40;   // columns visible
    private static final int VIEW_HEIGHT = 24;   // rows visible

    private static final int HUD_HEIGHT      = 80;
    private static final int FRAME_DELAY_MS  = 33; // ~30 FPS
    private static final int WORLD_TOP_MARGIN = 30;

    // ===================== STATE =========================

    private final World world;
    private TMXMapLoader tmxLoader;
    private final Player player;
    private final RelicManager relicManager;
    private final EncounterSystem encounterSystem;
    private final WeatherSystem weatherSystem;
    private final Random rand = new Random();

    private String  lastMessage =
            "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver = false;
    private boolean gameWon  = false;

    private final Timer timer;

    // ===================== CONSTRUCTOR ===================

    public GamePanel() {
        setPreferredSize(new Dimension(VIEW_WIDTH * TILE_SIZE,
                                       VIEW_HEIGHT * TILE_SIZE + HUD_HEIGHT));
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

        timer = new Timer(FRAME_DELAY_MS, e -> repaint());
        timer.start();
    }

    // ===================== INPUT =========================

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                tryMove(0, -1);
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                tryMove(0, 1);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                tryMove(-1, 0);
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                tryMove(1, 0);
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

        // Camera centered on player, clamped to world
        int viewLeft = player.getX() - VIEW_WIDTH / 2;
        int viewTop  = player.getY() - VIEW_HEIGHT / 2;

        if (viewLeft < 0) viewLeft = 0;
        if (viewTop  < 0) viewTop  = 0;
        if (viewLeft + VIEW_WIDTH  > world.getWidth())
            viewLeft = world.getWidth() - VIEW_WIDTH;
        if (viewTop + VIEW_HEIGHT > world.getHeight())
            viewTop = world.getHeight() - VIEW_HEIGHT;

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));

        // --- World tiles from TMX (all non-collision layers) ---
        for (int y = 0; y < VIEW_HEIGHT; y++) {
            int worldY = viewTop + y;
            if (worldY < 0 || worldY >= world.getHeight()) continue;

            for (int x = 0; x < VIEW_WIDTH; x++) {
                int worldX = viewLeft + x;
                if (worldX < 0 || worldX >= world.getWidth()) continue;

                boolean blocked = world.isBlocked(worldX, worldY);
                boolean isPlayerHere = (worldX == player.getX() && worldY == player.getY());

                int px = x * TILE_SIZE;
                int py = WORLD_TOP_MARGIN + y * TILE_SIZE;

                drawTile(g2, blocked, px, py, isPlayerHere, worldX, worldY);
            }
        }

        // --- HUD ---
        drawHud(g2);

        // --- Win / lose overlay ---
        if (gameOver || gameWon) {
            drawGameOverOverlay(g2);
        }
    }

    private void drawHud(Graphics2D g2) {
        int hudY = VIEW_HEIGHT * TILE_SIZE + 25;

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
                          boolean isPlayerHere, int worldX, int worldY) {

        // Fallback background
        Color bg = new Color(20, 25, 28);
        g2.setColor(bg);
        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // Draw all non-collision visual layers from bottom to top
        if (tmxLoader != null) {
            java.util.List<int[][]> layers = tmxLoader.getVisualLayers();
            for (int l = 0; l < layers.size(); l++) {
                int[][] layer = layers.get(l);
                int gid = layer[worldY][worldX];
                if (gid <= 0) continue;
                java.awt.image.BufferedImage img = tmxLoader.getTileImage(gid);
                if (img != null) {
                    g2.drawImage(img, px, py, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }

        // Collision invisible per request

        // Player rectangle
        if (isPlayerHere) {
            g2.setColor(new Color(240, 240, 255));
            g2.fillRect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);
        }

        // Weather disabled for clarity
    }

    /**
     * Smooth gradient background per biome band.
     */
    private Color computeBiomeBackground(TileType base, int worldY) {
        return new Color(20, 25, 28);
    }
}
