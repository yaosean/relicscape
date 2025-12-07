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

    void draw(Graphics2D g2, int w, int h){
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(0, 0, new Color(14, 14, 18),
                                             0, h, new Color(6, 6, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, w, h);

        int panelW = w - 120;
        int panelH = Math.min(280, (int)(h * 0.5));
        int panelX = (w - panelW) / 2;
        int panelY = (h - panelH) / 2;
        RoundRectangle2D panel = new RoundRectangle2D.Double(panelX, panelY, panelW, panelH, 12, 12);

        GradientPaint panelPaint = new GradientPaint(0, panelY, new Color(62, 58, 46, 230),
                                                     0, panelY + panelH, new Color(24, 22, 18, 235));
        g2.setPaint(panelPaint);
        g2.fill(panel);
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(230, 214, 184, 150));
        g2.draw(panel);

        String title = "Relicscape";
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(title);
        int tx = (w - tw) / 2;
        int ty = panelY + panelH / 2 - 12;
        g2.setColor(new Color(12, 10, 8, 160));
        g2.drawString(title, tx + 2, ty + 3);
        g2.setColor(new Color(236, 222, 196));
        g2.drawString(title, tx, ty);

        g2.setFont(subtitleFont);
        String sub = "Press any key to begin";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(new Color(210, 202, 180));
        g2.drawString(sub, (w - sw) / 2, ty + 34);

        g2.setFont(hintFont);
        String hint = "WASD / arrows to move   ·   Gather relic fragments   ·   Return to the shrine";
        int hw = g2.getFontMetrics().stringWidth(hint);
        g2.setColor(new Color(186, 178, 156));
        g2.drawString(hint, (w - hw) / 2, ty + 58);

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(120, 110, 90, 120));
        int lineY = panelY + panelH - 36;
        int lineX1 = panelX + 26;
        int lineX2 = panelX + panelW - 26;
        g2.drawLine(lineX1, lineY, lineX2, lineY);

        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(180, 168, 140, 160));
        for(int i=0;i<9;i++){
            int dx = lineX1 + i * (lineX2 - lineX1) / 8;
            g2.drawOval(dx - 3, lineY - 3, 6, 6);
        }
    }
}
