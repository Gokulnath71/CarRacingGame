import javax.swing.JFrame;

public class FightWindow extends JFrame {
    public FightWindow() {
        setTitle("Neon Brawl — Fighting Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new FightPanel());
        pack();
        setLocationRelativeTo(null);
    }
}
