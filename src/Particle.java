import java.awt.Color;
import java.awt.Graphics2D;

public class Particle {
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double life;
    public double maxLife;
    public float size;
    public Color color;
    public boolean circle;

    public Particle(double x, double y, double vx, double vy, double life, float size, Color color, boolean circle) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.size = size;
        this.color = color;
        this.circle = circle;
    }

    public boolean update() {
        x += vx;
        y += vy;
        life -= 1;
        return life <= 0;
    }

    public void draw(Graphics2D g) {
        float alpha = (float) Math.max(0, life / maxLife);
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * color.getAlpha()));
        g.setColor(c);
        if (circle) {
            g.fillOval((int) x, (int) y, (int) size, (int) size);
        } else {
            g.fillRect((int) x, (int) y, 2, (int) size);
        }
    }
}
