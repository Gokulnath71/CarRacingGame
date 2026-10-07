import javax.swing.SwingUtilities;

public class FightMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FightWindow window = new FightWindow();
            window.setVisible(true);
        });
    }
}
