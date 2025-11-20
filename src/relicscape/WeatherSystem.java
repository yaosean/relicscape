package relicscape;

import java.awt.*;
import java.util.Random;

/**
 * Responsible for drawing simple weather particles over tiles.
 */
public class WeatherSystem {
    private final Random rand;

    public WeatherSystem(Random rand) {
        this.rand = rand;
    }

    /**
     * Occasionally draw a weather particle on this tile.
     */
    public void drawWeather(Graphics2D g2, TileType biomeBase, int px, int py, int tileSize) {
        if (rand.nextInt(120) != 0) {
            return; // sparse particles
        }

        char particle;
        Color pc;
        if (biomeBase == TileType.GRASS) {
            particle = '*';
            pc = new Color(230, 240, 255, 180); // snowflake
        } else if (biomeBase == TileType.SAND) {
            particle = '.';
            pc = new Color(240, 220, 150, 180); // dust
        } else { // ruins
            particle = '.';
            pc = new Color(200, 200, 230, 150); // motes
        }
        g2.setColor(pc);
        int xOff = px + 2 + rand.nextInt(Math.max(1, tileSize - 4));
        int yOff = py - 6 + rand.nextInt(Math.max(1, tileSize - 4));
        g2.drawString(String.valueOf(particle), xOff, yOff);
    }
}
