import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel implements ActionListener {
    private static final int WIDTH = 480;
    private static final int HEIGHT = 720;
    private static final int ROAD_LEFT = 90;
    private static final int ROAD_RIGHT = 390;

    private final Timer timer;
    private final Random random = new Random();
    private final Car player;
    private final List<Car> enemies = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Scenery> scenery = new ArrayList<>();

    private double roadOffset;
    private double dashOffset;
    private double wheelSpin;
    private double speed = 8;
    private int score;
    private int distance;
    private int lives = 3;
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private boolean running = true;
    private int spawnCooldown;
    private int invincible;
    private double nitroPulse;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        player = new Car(WIDTH / 2.0 - 21, HEIGHT - 160, new Color(0, 210, 255), new Color(255, 80, 180), true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode(), true);
                if (!running && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    restart();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKey(e.getKeyCode(), false);
            }
        });

        for (int i = 0; i < 18; i++) {
            scenery.add(randomScenery(-i * 80));
        }

        timer = new Timer(16, this);
        timer.start();
    }

    private void handleKey(int code, boolean pressed) {
        switch (code) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                left = pressed;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                right = pressed;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                up = pressed;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                down = pressed;
                break;
            default:
                break;
        }
    }

    private void restart() {
        enemies.clear();
        particles.clear();
        score = 0;
        distance = 0;
        lives = 3;
        speed = 8;
        running = true;
        invincible = 0;
        player.x = WIDTH / 2.0 - 21;
        player.y = HEIGHT - 160;
        player.tilt = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    private void updateGame() {
        nitroPulse += 0.18;
        wheelSpin += speed * 0.4;
        roadOffset += speed;
        dashOffset += speed * 1.6;
        if (roadOffset > 80) {
            roadOffset -= 80;
        }

        for (Scenery item : scenery) {
            item.y += speed * 0.85;
            if (item.y > HEIGHT + 40) {
                item.recycle(-80 - random.nextInt(120));
            }
        }

        if (!running) {
            spawnIdleParticles();
            updateParticles();
            return;
        }

        double targetTilt = 0;
        if (left) {
            player.x -= 7;
            targetTilt = -0.28;
        }
        if (right) {
            player.x += 7;
            targetTilt = 0.28;
        }
        if (up) {
            speed = Math.min(18, speed + 0.12);
            player.y = Math.max(80, player.y - 3);
        } else if (down) {
            speed = Math.max(5, speed - 0.18);
            player.y = Math.min(HEIGHT - 100, player.y + 3);
        } else {
            player.y += (HEIGHT - 160 - player.y) * 0.04;
            speed += (10 - speed) * 0.02;
        }

        player.x = Math.max(ROAD_LEFT + 8, Math.min(ROAD_RIGHT - player.width - 8, player.x));
        player.tilt += (targetTilt - player.tilt) * 0.25;

        spawnExhaust();
        spawnSpeedLines();

        spawnCooldown--;
        if (spawnCooldown <= 0) {
            spawnEnemy();
            spawnCooldown = Math.max(28, 70 - distance / 400);
        }

        Iterator<Car> it = enemies.iterator();
        while (it.hasNext()) {
            Car enemy = it.next();
            enemy.y += enemy.speed + speed * 0.25;
            enemy.tilt = Math.sin((enemy.y + distance) / 40.0) * 0.05;
            if (enemy.y > HEIGHT + 80) {
                it.remove();
                score += 10;
            } else if (invincible <= 0 && player.intersects(enemy)) {
                crash(enemy);
            }
        }

        if (invincible > 0) {
            invincible--;
        }

        distance += (int) speed;
        score += (int) (speed / 8);
        updateParticles();
    }

    private void crash(Car enemy) {
        burst(player.x + player.width / 2, player.y + player.height / 2, new Color(255, 140, 40, 220));
        burst(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, new Color(255, 60, 60, 220));
        lives--;
        invincible = 90;
        speed = 7;
        enemies.remove(enemy);
        if (lives <= 0) {
            running = false;
        }
    }

    private void spawnEnemy() {
        double lane = ROAD_LEFT + 18 + random.nextInt(3) * 90;
        Color[] bodies = {
                new Color(255, 70, 90),
                new Color(255, 190, 40),
                new Color(120, 255, 90),
                new Color(180, 90, 255)
        };
        Color body = bodies[random.nextInt(bodies.length)];
        Car enemy = new Car(lane, -90, body, body.brighter(), false);
        enemy.speed = 3 + random.nextDouble() * 5;
        enemies.add(enemy);
    }

    private void spawnExhaust() {
        if (random.nextInt(2) == 0) {
            particles.add(new Particle(
                    player.x + 8 + random.nextInt(20),
                    player.y + player.height - 4,
                    random.nextDouble() * 1.2 - 0.6,
                    3 + random.nextDouble() * 2,
                    18 + random.nextInt(12),
                    8 + random.nextInt(8),
                    new Color(80, 90, 110, 140),
                    true
            ));
        }
        if (up) {
            particles.add(new Particle(
                    player.x + 12 + random.nextInt(16),
                    player.y + player.height,
                    random.nextDouble() - 0.5,
                    6,
                    12,
                    6,
                    new Color(80, 220, 255, 180),
                    true
            ));
        }
    }

    private void spawnSpeedLines() {
        if (speed > 11 && random.nextInt(3) == 0) {
            particles.add(new Particle(
                    ROAD_LEFT + 10 + random.nextInt(ROAD_RIGHT - ROAD_LEFT - 20),
                    -20,
                    0,
                    speed * 2.2,
                    24,
                    18 + random.nextInt(20),
                    new Color(255, 255, 255, 70),
                    false
            ));
        }
    }

    private void spawnIdleParticles() {
        if (random.nextInt(4) == 0) {
            particles.add(new Particle(
                    random.nextInt(WIDTH),
                    HEIGHT,
                    0,
                    -1.5,
                    40,
                    4,
                    new Color(255, 80, 180, 120),
                    true
            ));
        }
    }

    private void burst(double x, double y, Color color) {
        for (int i = 0; i < 28; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double mag = 2 + random.nextDouble() * 5;
            particles.add(new Particle(
                    x,
                    y,
                    Math.cos(angle) * mag,
                    Math.sin(angle) * mag,
                    20 + random.nextInt(16),
                    5 + random.nextInt(8),
                    color,
                    true
            ));
        }
    }

    private void updateParticles() {
        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            if (pit.next().update()) {
                pit.remove();
            }
        }
    }

    private Scenery randomScenery(double y) {
        boolean leftSide = random.nextBoolean();
        double x = leftSide ? 12 + random.nextInt(50) : ROAD_RIGHT + 12 + random.nextInt(50);
        return new Scenery(x, y, 16 + random.nextInt(18), 40 + random.nextInt(50), leftSide);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g);
        drawScenery(g);
        drawRoad(g);

        for (Particle particle : particles) {
            particle.draw(g);
        }
        for (Car enemy : enemies) {
            enemy.draw(g, wheelSpin);
        }

        if (invincible == 0 || (invincible / 6) % 2 == 0) {
            player.draw(g, wheelSpin);
        }

        drawHud(g);
        if (!running) {
            drawGameOver(g);
        }
    }

    private void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(12, 8, 40), 0, HEIGHT, new Color(40, 10, 70));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 255, 255, 50));
        for (int i = 0; i < 24; i++) {
            int sx = (i * 97 + (int) (distance * 0.02)) % WIDTH;
            int sy = (i * 53) % (HEIGHT / 2);
            g.fillOval(sx, sy, 2, 2);
        }
    }

    private void drawScenery(Graphics2D g) {
        for (Scenery item : scenery) {
            g.setColor(new Color(20, 80, 70));
            g.fillRect((int) item.x + (int) item.size / 2 - 4, (int) item.y + 10, 8, (int) item.height);
            g.setColor(new Color(40, 180, 120));
            g.fillOval((int) item.x, (int) item.y - 10, (int) item.size * 2, (int) item.size * 2);
            g.setColor(new Color(255, 80, 200, 50));
            g.fillOval((int) item.x + 6, (int) item.y, 10, 10);
        }
    }

    private void drawRoad(Graphics2D g) {
        GradientPaint road = new GradientPaint(
                ROAD_LEFT, 0, new Color(28, 28, 36),
                ROAD_RIGHT, 0, new Color(18, 18, 26)
        );
        g.setPaint(road);
        Path2D path = new Path2D.Double();
        path.moveTo(ROAD_LEFT - 20, HEIGHT);
        path.lineTo(ROAD_LEFT, 0);
        path.lineTo(ROAD_RIGHT, 0);
        path.lineTo(ROAD_RIGHT + 20, HEIGHT);
        path.closePath();
        g.fill(path);

        g.setColor(new Color(255, 60, 160));
        g.setStroke(new BasicStroke(6f));
        g.drawLine(ROAD_LEFT, 0, ROAD_LEFT - 20, HEIGHT);
        g.setColor(new Color(80, 220, 255));
        g.drawLine(ROAD_RIGHT, 0, ROAD_RIGHT + 20, HEIGHT);

        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {28f, 22f}, (float) dashOffset));
        g.setColor(new Color(255, 230, 120));
        g.drawLine(WIDTH / 2, -20, WIDTH / 2, HEIGHT + 20);

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {12f, 18f}, (float) (dashOffset * 1.4)));
        g.setColor(new Color(255, 255, 255, 80));
        int lane1 = ROAD_LEFT + 100;
        int lane2 = ROAD_LEFT + 200;
        g.drawLine(lane1, 0, lane1 - 6, HEIGHT);
        g.drawLine(lane2, 0, lane2 + 6, HEIGHT);

        double glow = 0.45 + 0.25 * Math.sin(nitroPulse);
        g.setColor(new Color(0, 255, 220, (int) (40 * glow)));
        g.fillRect(ROAD_LEFT, 0, ROAD_RIGHT - ROAD_LEFT, HEIGHT);
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(16, 16, 200, 92, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("SCORE  " + score, 28, 42);
        g.drawString("SPEED  " + (int) (speed * 12) + " km/h", 28, 66);
        g.drawString("LIVES  " + lives, 28, 90);

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(WIDTH - 168, 16, 152, 48, 16, 16);
        g.setColor(new Color(180, 230, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Arrows / WASD", WIDTH - 150, 36);
        g.drawString("Hold UP to boost", WIDTH - 150, 54);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(8, 0, 20, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(255, 80, 180));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("CRASHED!", 140, HEIGHT / 2 - 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Score: " + score, 180, HEIGHT / 2 + 16);
        g.drawString("Press ENTER to race again", 112, HEIGHT / 2 + 50);
    }

    private static class Scenery {
        double x;
        double y;
        double size;
        double height;
        boolean leftSide;

        Scenery(double x, double y, double size, double height, boolean leftSide) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.height = height;
            this.leftSide = leftSide;
        }

        void recycle(double newY) {
            y = newY;
            size = 16 + Math.random() * 18;
            height = 40 + Math.random() * 50;
        }
    }
}
