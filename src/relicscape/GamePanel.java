package relicscape;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

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

    private static final int WORLD_WIDTH  = 80;
    private static final int WORLD_HEIGHT = 45;

    // Tile grid
    private static final int TILE_SIZE   = 20;
    private static final int VIEW_WIDTH  = 40;
    private static final int VIEW_HEIGHT = 24;

    private static final int HUD_HEIGHT      = 80;
    private static final int FRAME_DELAY_MS  = 33; // ~30 FPS
    private static final int WORLD_TOP_MARGIN = 30;

    // ===================== STATE =========================

    private final World world;
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

        world        = new World(WORLD_WIDTH, WORLD_HEIGHT);
        relicManager = new RelicManager(3);

        WorldGenerator generator = new WorldGenerator();
        generator.generate(world, relicManager, rand);

        player          = new Player(world.getWidth() / 2, world.getHeight() / 2, 10);
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

        TileType tile = world.getTile(newX, newY);
        if (!isWalkable(tile)) {
            lastMessage = "You can't move through that.";
            return;
        }

        player.setPosition(newX, newY);
        resolveTile(tile);

        String encounterMsg = encounterSystem.maybeEncounter(tile, player);
        if (encounterMsg != null) {
            lastMessage = encounterMsg;
            if (player.getHp() <= 0) {
                gameOver = true;
            }
        }

        if (!gameOver) {
            checkWinCondition();
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

        // --- World tiles ---
        for (int y = 0; y < VIEW_HEIGHT; y++) {
            int worldY = viewTop + y;
            if (worldY < 0 || worldY >= world.getHeight()) continue;

            for (int x = 0; x < VIEW_WIDTH; x++) {
                int worldX = viewLeft + x;
                if (worldX < 0 || worldX >= world.getWidth()) continue;

                TileType tile = world.getTile(worldX, worldY);
                boolean isPlayerHere = (worldX == player.getX() && worldY == player.getY());

                int px = x * TILE_SIZE;
                int py = WORLD_TOP_MARGIN + y * TILE_SIZE;

                drawTile(g2, tile, px, py, isPlayerHere, worldX, worldY);
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
    private void drawTile(Graphics2D g2, TileType tile, int px, int py,
                          boolean isPlayerHere, int worldX, int worldY) {

        // Background
        TileType biomeBase = world.baseForRow(worldY);
        Color bg = computeBiomeBackground(biomeBase, worldY);
        g2.setColor(bg);
        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // Shadow under tall tiles
        if (!isPlayerHere &&
            (tile == TileType.TREE || tile == TileType.CACTUS || tile == TileType.RUIN_WALL)) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval(px + 2, py + TILE_SIZE - 6, TILE_SIZE - 4, 4);
        }

        char glyph = ' ';
        Color fg   = Color.WHITE;

        // Iconic glyphs for objects. Ground tiles are blank.
        switch (tile) {
            case GRASS:
            case SAND:
            case RUIN_FLOOR:
                glyph = ' '; // just color
                break;

            case TREE:
                glyph = 'T';
                fg = new Color(70, 220, 120);
                break;

            case ROCK:
                glyph = '^';
                fg = new Color(210, 210, 220);
                break;

            case FLOWER:
                glyph = '*';
                fg = new Color(255, 190, 215);
                break;

            case DUNE:
                glyph = '~';
                fg = new Color(225, 195, 145);
                break;

            case CACTUS:
                glyph = 'I';
                fg = new Color(90, 210, 120);
                break;

            case RUIN_WALL:
                glyph = '#';
                fg = new Color(200, 190, 210);
                break;

            case RUBBLE:
                glyph = 'x';
                fg = new Color(175, 155, 155);
                break;

            case RELIC:
                glyph = '✶';
                fg = new Color(255, 235, 140);
                break;

            case SHRINE:
                glyph = '⌘';
                fg = new Color(190, 230, 255);
                break;

            default:
                glyph = '?';
                fg = Color.WHITE;
        }

        // Player overrides glyph
        if (isPlayerHere) {
            glyph = '@';
            fg = new Color(255, 255, 255);
        }

        // Draw glyph if not blank
        if (glyph != ' ') {
            g2.setColor(fg);
            int baselineY = py + TILE_SIZE - 4;
            g2.drawString(String.valueOf(glyph), px + 4, baselineY);
        }

        // Weather disabled for clarity (uncomment later if you want movement)
        // weatherSystem.drawWeather(g2, biomeBase, px, py, TILE_SIZE);
    }

    /**
     * Smooth gradient background per biome band.
     */
    private Color computeBiomeBackground(TileType base, int worldY) {
        int h         = world.getHeight();
        int forestEnd = h / 3;
        int desertEnd = 2 * h / 3;

        if (base == TileType.GRASS) {
            float t = Util.clamp01(worldY / (float) Math.max(1, forestEnd - 1));
            Color top    = new Color(3, 25, 10);
            Color bottom = new Color(15, 70, 30);
            return Util.lerpColor(top, bottom, t);
        } else if (base == TileType.SAND) {
            float t = Util.clamp01((worldY - forestEnd) /
                                   (float) Math.max(1, desertEnd - forestEnd - 1));
            Color top    = new Color(40, 35, 15);
            Color bottom = new Color(80, 60, 25);
            return Util.lerpColor(top, bottom, t);
        } else { // RUINS
            float t = Util.clamp01((worldY - desertEnd) /
                                   (float) Math.max(1, h - desertEnd - 1));
            Color top    = new Color(25, 20, 40);
            Color bottom = new Color(5, 5, 15);
            return Util.lerpColor(top, bottom, t);
        }
    }
}
