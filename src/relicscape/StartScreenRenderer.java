package relicscape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

class StartScreenRenderer {

    private final Font titleFont = new Font("Garamond", Font.BOLD, 56);
    private final Font subtitleFont = new Font("Garamond", Font.PLAIN, 20);
    private final Font hintFont = new Font("Garamond", Font.PLAIN, 16);

    void draw(Graphics2D splashPen, int wide, int tall){
        splashPen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint skyPaint = new GradientPaint(0, 0, new Color(14, 14, 18),
                                             0, tall, new Color(6, 6, 10));
        splashPen.setPaint(skyPaint);
        splashPen.fillRect(0, 0, wide, tall);

        splashPen.setColor(new Color(0, 0, 0, 140));
        splashPen.fillRect(0, 0, wide, tall);

        int boxWide = wide - 120;
        int boxTall = Math.min(280, (int)(tall * 0.5));
        int boxX = (wide - boxWide) / 2;
        int boxY = (tall - boxTall) / 2;
        RoundRectangle2D panel = new RoundRectangle2D.Double(boxX, boxY, boxWide, boxTall, 12, 12);

        GradientPaint panelPaint = new GradientPaint(0, boxY, new Color(62, 58, 46, 230),
                                                     0, boxY + boxTall, new Color(24, 22, 18, 235));
        splashPen.setPaint(panelPaint);
        splashPen.fill(panel);
        splashPen.setStroke(new BasicStroke(1.4f));
        splashPen.setColor(new Color(230, 214, 184, 150));
        splashPen.draw(panel);

        String bigWord = "Relicscape";
        splashPen.setFont(titleFont);
        FontMetrics letterSizer = splashPen.getFontMetrics();
        int bigW = letterSizer.stringWidth(bigWord);
        int bigX = (wide - bigW) / 2;
        int bigY = boxY + boxTall / 2 - 12;
        splashPen.setColor(new Color(12, 10, 8, 160));
        splashPen.drawString(bigWord, bigX + 2, bigY + 3);
        splashPen.setColor(new Color(236, 222, 196));
        splashPen.drawString(bigWord, bigX, bigY);

        splashPen.setFont(subtitleFont);
        String tinyWord = "Press any key to begin";
        int tinyW = splashPen.getFontMetrics().stringWidth(tinyWord);
        splashPen.setColor(new Color(210, 202, 180));
        splashPen.drawString(tinyWord, (wide - tinyW) / 2, bigY + 34);

        splashPen.setFont(hintFont);
        String hintWords = "WASD / arrows to move   ·   Gather relic fragments   ·   Return to the shrine";
        int hintW = splashPen.getFontMetrics().stringWidth(hintWords);
        splashPen.setColor(new Color(186, 178, 156));
        splashPen.drawString(hintWords, (wide - hintW) / 2, bigY + 58);

        splashPen.setStroke(new BasicStroke(2f));
        splashPen.setColor(new Color(120, 110, 90, 120));
        int lineY = boxY + boxTall - 36;
        int lineX1 = boxX + 26;
        int lineX2 = boxX + boxWide - 26;
        splashPen.drawLine(lineX1, lineY, lineX2, lineY);

        splashPen.setStroke(new BasicStroke(1.2f));
        splashPen.setColor(new Color(180, 168, 140, 160));
        for(int dot=0; dot<9; dot++){
            int dotX = lineX1 + dot * (lineX2 - lineX1) / 8;
            splashPen.drawOval(dotX - 3, lineY - 3, 6, 6);
        }
    }
}
