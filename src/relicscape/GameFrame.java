package relicscape;

import java.awt.Frame;
import javax.swing.JFrame;

//Top-level window for Relicscape.

public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("Relicscape");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH); // maximize to fill screen
        setLocationRelativeTo(null);
        setState(Frame.NORMAL);
        toFront();
    }
}
