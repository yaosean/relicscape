package relicscape;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

/**
 * Main game panel: handles rendering and input.
 */
public class GamePanel extends JPanel implements KeyListener {

    private static final int WORLD_WIDTH = 80;
    private static final int WORLD_HEIGHT = 45;
    private static final int TILE_SIZE = 18; // pixels per tile
    private static final int VIEW_WIDTH = 40; // tiles
    private static final int VIEW_HEIGHT = 24; // tiles

    private final World world;
    private final Player player;
    private final RelicManager relicManager;
    private final EncounterSystem encounterSystem;
    private final WeatherSystem weatherSystem;
    private final Random rand = new Random();

    private String lastMessage = "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver = false;
    private boolean gameWon = false;

    private final Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(VIEW_WIDTH * TILE_SIZE, VIEW_HEIGHT * TILE_SIZE + 80));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        world = new World(WORLD_WIDTH, WORLD_HEIGHT);
        relicManager = new RelicManager(3);
        WorldGenerator generator = new WorldGenerator();
        generator.generate(world, relicManager, rand);

        player = new Player(world.getWidth() / 2, world.getHeight() / 2, 10);
        encounterSystem = new EncounterSystem(rand);
        weatherSystem = new WeatherSystem(rand);

        timer = new Timer(33, e -> repaint()); // ~30 FPS
        timer.start();
    }

    // ===================== INPUT =====================

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) return;
        int code = e.getKeyCode();
        switch (code) {
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
        if (gameWon) {
            // nothing else; overlay is drawn in paintComponent
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }

    // ======================= RENDERING =======================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int viewLeft = player.getX() - VIEW_WIDTH / 2;
        int viewTop = player.getY() - VIEW_HEIGHT / 2;
        if (viewLeft < 0) viewLeft = 0;
        if (viewTop < 0) viewTop = 0;
        if (viewLeft + VIEW_WIDTH > world.getWidth()) viewLeft = world.getWidth() - VIEW_WIDTH;
        if (viewTop + VIEW_HEIGHT > world.getHeight()) viewTop = world.getHeight() - VIEW_HEIGHT;

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        int offsetY = 20;

        for (int y = 0; y < VIEW_HEIGHT; y++) {
            int worldY = viewTop + y;
            if (worldY < 0 || worldY >= world.getHeight()) continue;
            for (int x = 0; x < VIEW_WIDTH; x++) {
                int worldX = viewLeft + x;
                if (worldX < 0 || worldX >= world.getWidth()) continue;

                TileType tile = world.getTile(worldX, worldY);
                boolean isPlayerHere = (worldX == player.getX() && worldY == player.getY());
                drawTile(g2, tile, x * TILE_SIZE, offsetY + y * TILE_SIZE, isPlayerHere, worldY);
            }
        }

        // HUD background
        int hudY = VIEW_HEIGHT * TILE_SIZE + 25;
        g2.setColor(new Color(10, 10, 10, 230));
        g2.fillRoundRect(10, hudY - 20, getWidth() - 20, 60, 15, 15);

        // HUD text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.drawString(
                "HP: " + player.getHp() + "   Relics: " +
                        relicManager.getRelicsCollected() + "/" + relicManager.getRelicsToCollect(),
                20, hudY);
        g2.setFont(new Font("Consolas", Font.PLAIN, 13));
        Util.drawWrappedText(g2, lastMessage, 20, hudY + 18, getWidth() - 40);

        if (gameOver || gameWon) {
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
    }

    private void drawTile(Graphics2D g2, TileType tile, int px, int py,
                          boolean isPlayerHere, int worldY) {
        char ch;
        Color fg;

        if (isPlayerHere) {
            ch = '@';
            fg = new Color(255, 250, 240);
        } else {
            switch (tile) {
                case GRASS:
                    ch = '·';
                    fg = new Color(120, 200, 140);
                    break;
                case TREE:
                    ch = '♣';
                    fg = new Color(40, 160, 80);
                    break;
                case ROCK:
                    ch = '^';
                    fg = new Color(180, 180, 190);
                    break;
                case FLOWER:
                    ch = '*';
                    fg = new Color(255, 170, 200);
                    break;
                case SAND:
                    ch = '.';
                    fg = new Color(220, 200, 140);
                    break;
                case DUNE:
                    ch = '~';
                    fg = new Color(210, 180, 120);
                    break;
                case CACTUS:
                    ch = 'I';
                    fg = new Color(60, 180, 90);
                    break;
                case RUIN_FLOOR:
                    ch = '·';
                    fg = new Color(140, 130, 140);
                    break;
                case RUIN_WALL:
                    ch = '#';
                    fg = new Color(180, 170, 180);
                    break;
                case RUBBLE:
                    ch = 'x';
                    fg = new Color(160, 140, 140);
                    break;
                case RELIC:
                    ch = '✶';
                    fg = new Color(255, 230, 120);
                    break;
                case SHRINE:
                    ch = '⌘';
                    fg = new Color(180, 220, 255);
                    break;
                default:
                    ch = '?';
                    fg = Color.WHITE;
            }
        }

        // background tint per biome (based on row)
        TileType base = world.baseForRow(worldY);
        Color bg;
        switch (base) {
            case GRASS:
                bg = new Color(4, 30, 12);
                break;
            case SAND:
                bg = new Color(35, 30, 10);
                break;
            case RUIN_FLOOR:
            default:
                bg = new Color(20, 18, 30);
                break;
        }
        g2.setColor(bg);
        g2.fillRect(px, py - 14, TILE_SIZE, TILE_SIZE + 4);

        g2.setColor(fg);
        g2.drawString(String.valueOf(ch), px + 3, py + TILE_SIZE - 6);

        // small weather particle occasionally
        weatherSystem.drawWeather(g2, base, px, py, TILE_SIZE);
    }
}
