package relicscape;

import java.awt.*;
public class Util {

    /**
     * Draw text wrapped to a maximum width.
     */
    public static void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;
        for (String w : words) {
            String test = line + (line.length() == 0 ? "" : " ") + w;
            if (fm.stringWidth(test) > maxWidth) {
                g2.drawString(line.toString(), x, curY);
                line = new StringBuilder(w);
                curY += fm.getHeight();
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(w);
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, curY);
        }
    }

    /**
     * Clamp a float to [0,1].
     */
    public static float clamp01(float t) {
        if (t < 0f) return 0f;
        if (t > 1f) return 1f;
        return t;
    }

    /**
     * Linear interpolation between two colors.
     */
    public static Color lerpColor(Color a, Color b, float t) {
        t = clamp01(t);
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

    // Small, hand-made jitter so repeated tiles differ without looking procedural.
    public static int scrappyPick(int x, int y, int salt, int mod) {
        if (mod <= 0) return 0;
        int h = x * 41 + y * 17 + salt * 13;
        h = (h ^ (h << 3)) ^ (h >> 2);
        return Math.abs(h) % mod;
    }
}
