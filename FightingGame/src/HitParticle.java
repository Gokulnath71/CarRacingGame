import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

public class HitParticle {
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double life;
    public double maxLife;
    public double size;
    public Color color;
    public boolean spark;

    public HitParticle(double x, double y, double vx, double vy, double life, double size, Color color, boolean spark) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.size = size;
        this.color = color;
        this.spark = spark;
    }

    public boolean update() {
        x += vx;
        y += vy;
        vy += spark ? 0.15 : 0.35;
        vx *= 0.98;
        life -= 1;
        return life > 0;
    }

    public void draw(Graphics2D g) {
        float alpha = (float) Math.max(0, Math.min(1, life / maxLife));
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 220));
        g.setColor(c);
        double s = size * (0.5 + 0.5 * alpha);
        if (spark) {
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawLine((int) x, (int) y, (int) (x - vx * 2), (int) (y - vy * 2));
        } else {
            g.fill(new Ellipse2D.Double(x - s / 2, y - s / 2, s, s));
        }
    }
}
