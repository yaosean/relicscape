package relicscape;

import java.awt.*;

/**
 * Utility helper methods.
 */
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
}
