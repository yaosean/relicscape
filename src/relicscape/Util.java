package relicscape;

import java.awt.*;
public class Util {

    /**
     * Draw text wrapped to a maximum width.
     */
    public static void drawWrappedText(Graphics2D doodlePen, String chatter, int left, int top, int wrapWidth) {
        FontMetrics shapeSizer = doodlePen.getFontMetrics();
        String[] bubbleWords = chatter.split(" ");
        StringBuilder bubbleLine = new StringBuilder();
        int bubbleY = top;
        for (String bubble : bubbleWords) {
            String testLine = bubbleLine + (bubbleLine.length() == 0 ? "" : " ") + bubble;
            if (shapeSizer.stringWidth(testLine) > wrapWidth) {
                doodlePen.drawString(bubbleLine.toString(), left, bubbleY);
                bubbleLine = new StringBuilder(bubble);
                bubbleY += shapeSizer.getHeight();
            } else {
                if (bubbleLine.length() > 0) bubbleLine.append(' ');
                bubbleLine.append(bubble);
            }
        }
        if (bubbleLine.length() > 0) {
            doodlePen.drawString(bubbleLine.toString(), left, bubbleY);
        }
    }

    /**
     * Clamp a float to [0,1].
     */
    public static float clamp01(float squish) {
        if (squish < 0f) return 0f;
        if (squish > 1f) return 1f;
        return squish;
    }

    /**
     * Linear interpolation between two colors.
     */
    public static Color lerpColor(Color leftColor, Color rightColor, float blendy) {
        blendy = clamp01(blendy);
        int r = (int) (leftColor.getRed() + (rightColor.getRed() - leftColor.getRed()) * blendy);
        int g = (int) (leftColor.getGreen() + (rightColor.getGreen() - leftColor.getGreen()) * blendy);
        int bl = (int) (leftColor.getBlue() + (rightColor.getBlue() - leftColor.getBlue()) * blendy);
        int al = (int) (leftColor.getAlpha() + (rightColor.getAlpha() - leftColor.getAlpha()) * blendy);
        return new Color(r, g, bl, al);
    }

    // Small, hand-made jitter so repeated tiles differ without looking procedural.
    public static int scrappyPick(int xx, int yy, int sprinkle, int chunk) {
        if (chunk <= 0) return 0;
        int h = xx * 41 + yy * 17 + sprinkle * 13;
        h = (h ^ (h << 3)) ^ (h >> 2);
        return Math.abs(h) % chunk;
    }
}
