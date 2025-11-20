package relicscape;

import javax.swing.JFrame;

/**
 * Top-level window for Relicscape.
 */
public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("Relicscape");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null); // center on screen
    }
}
