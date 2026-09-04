import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class GameWindow extends JFrame {
    private static final Color[][] PALETTE = {
            {new Color(0, 210, 255), new Color(255, 80, 180)},
            {new Color(255, 70, 90), new Color(255, 200, 80)},
            {new Color(120, 255, 90), new Color(40, 180, 120)},
            {new Color(255, 190, 40), new Color(255, 100, 40)},
            {new Color(180, 90, 255), new Color(255, 120, 220)}
    };

    public GameWindow() {
        setTitle("Neon Rush — Car Racing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();

        JPanel colourBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        colourBar.setBackground(new Color(12, 8, 40));
        colourBar.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel label = new JLabel("Colour:");
        label.setForeground(new Color(180, 230, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        colourBar.add(label);

        for (Color[] pair : PALETTE) {
            JButton swatch = new JButton();
            swatch.setPreferredSize(new Dimension(36, 28));
            swatch.setBackground(pair[0]);
            swatch.setOpaque(true);
            swatch.setBorderPainted(true);
            swatch.setFocusable(false);
            swatch.setToolTipText("Choose car colour");
            swatch.addActionListener(e -> {
                gamePanel.setPlayerColors(pair[0], pair[1]);
                gamePanel.requestFocusInWindow();
            });
            colourBar.add(swatch);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.add(colourBar, BorderLayout.NORTH);
        root.add(gamePanel, BorderLayout.CENTER);
        add(root);
        pack();
        setLocationRelativeTo(null);
        gamePanel.requestFocusInWindow();
    }
}
