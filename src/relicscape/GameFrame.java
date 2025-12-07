package relicscape;

import java.awt.Frame;
import javax.swing.JFrame;
public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("Relicscape");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel playBox = new GamePanel();
        setContentPane(playBox);
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setState(Frame.NORMAL);
        toFront();
    }
}
