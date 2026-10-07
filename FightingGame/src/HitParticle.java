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
        if (spark) {
            vy += 0.12;
            vx *= 0.97;
        } else {
            // smoke drifts up and expands feel
            vy -= 0.04;
            vx *= 0.96;
            size += 0.12;
        }
        life -= 1;
        return life > 0;
    }

    public void draw(Graphics2D g) {
        float alpha = (float) Math.max(0, Math.min(1, life / maxLife));
        if (spark) {
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 230));
            g.setColor(c);
            g.setStroke(new java.awt.BasicStroke(2.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.drawLine((int) x, (int) y, (int) (x - vx * 2.4), (int) (y - vy * 2.4));
        } else {
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 90));
            g.setColor(c);
            double s = size * (0.7 + 0.5 * (1 - alpha));
            g.fill(new Ellipse2D.Double(x - s / 2, y - s / 2, s, s));
        }
    }
}
