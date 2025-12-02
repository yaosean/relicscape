package relicscape;

import javax.swing.SwingUtilities;

//Entry point for Relicscape.
public class Game {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
