import javax.swing.*;

public class App {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        SpaceInvaders spaceInvaders = new SpaceInvaders();
        frame.add(spaceInvaders);

        frame.pack(); // sizes frame to SpaceInvaders preferred size
        frame.setVisible(true);

        // Request focus so key events work
        spaceInvaders.requestFocusInWindow();
    }
}
