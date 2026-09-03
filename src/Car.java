import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

public class Car {
    public double x;
    public double y;
    public double width = 42;
    public double height = 72;
    public Color bodyColor;
    public Color accentColor;
    public double speed;
    public double tilt;
    public boolean player;

    public Car(double x, double y, Color bodyColor, Color accentColor, boolean player) {
        this.x = x;
        this.y = y;
        this.bodyColor = bodyColor;
        this.accentColor = accentColor;
        this.player = player;
    }

    public void draw(Graphics2D g, double wheelSpin) {
        AffineTransform old = g.getTransform();
        g.translate(x + width / 2.0, y + height / 2.0);
        g.rotate(tilt);

        double w = width;
        double h = height;
        double left = -w / 2.0;
        double top = -h / 2.0;

        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(left + 6, top + 10, w, h, 16, 16));

        g.setColor(new Color(20, 20, 24));
        g.fillRoundRect((int) left - 4, (int) (top + 10), 8, 16, 4, 4);
        g.fillRoundRect((int) (left + w - 4), (int) (top + 10), 8, 16, 4, 4);
        g.fillRoundRect((int) left - 4, (int) (top + h - 26), 8, 16, 4, 4);
        g.fillRoundRect((int) (left + w - 4), (int) (top + h - 26), 8, 16, 4, 4);

        g.setColor(new Color(70, 70, 76));
        int spoke = (int) (wheelSpin * 8) % 8;
        g.fillRect((int) left - 2, (int) (top + 12 + spoke), 4, 3);
        g.fillRect((int) (left + w - 2), (int) (top + 12 + spoke), 4, 3);

        g.setColor(bodyColor);
        g.fill(new RoundRectangle2D.Double(left, top, w, h, 18, 18));

        g.setColor(accentColor);
        g.fillRoundRect((int) (left + 6), (int) (top + 8), (int) (w - 12), 10, 8, 8);
        g.fillRoundRect((int) (left + 8), (int) (top + h - 18), (int) (w - 16), 8, 6, 6);

        g.setColor(new Color(40, 80, 120, 180));
        g.fillRoundRect((int) (left + 8), (int) (top + 22), (int) (w - 16), 22, 10, 10);

        g.setColor(new Color(180, 220, 255, 90));
        g.fillRoundRect((int) (left + 10), (int) (top + 24), (int) (w - 22), 8, 6, 6);

        if (player) {
            g.setColor(new Color(255, 230, 120));
            g.fillOval((int) (left + 8), (int) (top + h - 10), 8, 6);
            g.fillOval((int) (left + w - 16), (int) (top + h - 10), 8, 6);
        } else {
            g.setColor(new Color(255, 80, 80));
            g.fillOval((int) (left + 8), (int) (top + 4), 8, 6);
            g.fillOval((int) (left + w - 16), (int) (top + 4), 8, 6);
        }

        g.setColor(new Color(255, 255, 255, 40));
        g.fillRoundRect((int) (left + 4), (int) (top + 6), 8, (int) (h - 14), 8, 8);

        g.setTransform(old);
    }

    public boolean intersects(Car other) {
        return x < other.x + other.width - 8
                && x + width > other.x + 8
                && y < other.y + other.height - 10
                && y + height > other.y + 10;
    }
}
