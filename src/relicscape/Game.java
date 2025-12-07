package relicscape;

import javax.swing.SwingUtilities;
public class Game {
    public static void main(String[] launchWords) {
        SwingUtilities.invokeLater(() -> {
            GameFrame windowBuddy = new GameFrame();
            windowBuddy.setVisible(true);
        });
    }
}
