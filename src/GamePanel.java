import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GamePanel extends JPanel implements ActionListener {
    private static final int WIDTH = 480;
    private static final int HEIGHT = 720;
    private static final int ROAD_LEFT = 90;
    private static final int ROAD_RIGHT = 390;
    private static final double POWER_MAX = 100;
    private static final double POWER_COST = 45;
    private static final int POWER_BURST_FRAMES = 48;
    private static final Rectangle DAY_OPTION = new Rectangle(70, 290, 150, 90);
    private static final Rectangle NIGHT_OPTION = new Rectangle(250, 290, 150, 90);
    private static final Rectangle START_BUTTON = new Rectangle(140, 400, 200, 44);
    private static final Rectangle POWER_BUTTON = new Rectangle(WIDTH - 178, HEIGHT - 70, 150, 44);

    enum Place {
        DAY,
        NIGHT
    }

    private final Timer timer;
    private final Random random = new Random();
    private final Car player;
    private final List<Car> enemies = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Scenery> scenery = new ArrayList<>();

    private Place place = Place.NIGHT;
    private boolean inMenu = true;
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
    private double power = POWER_MAX * 0.35;
    private int powerBurst;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(Color.BLACK);
        player = new Car(WIDTH / 2.0 - 21, HEIGHT - 160, new Color(0, 210, 255), new Color(255, 80, 180), true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (inMenu) {
                    handleMenuKey(code);
                    return;
                }
                handleKey(code, true);
                if (code == KeyEvent.VK_SPACE) {
                    tryUsePower();
                }
                if (!running && code == KeyEvent.VK_ENTER) {
                    restart();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!inMenu) {
                    handleKey(e.getKeyCode(), false);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                handleClick(e.getX(), e.getY());
            }
        });

        for (int i = 0; i < 18; i++) {
            scenery.add(randomScenery(-i * 80));
        }

        timer = new Timer(16, this);
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void handleClick(int x, int y) {
        if (inMenu) {
            if (DAY_OPTION.contains(x, y)) {
                place = Place.DAY;
                return;
            }
            if (NIGHT_OPTION.contains(x, y)) {
                place = Place.NIGHT;
                return;
            }
            if (START_BUTTON.contains(x, y)) {
                startRace();
            }
            return;
        }
        if (!running && new Rectangle(98, HEIGHT / 2 + 30, 280, 40).contains(x, y)) {
            restart();
            return;
        }
        if (running && POWER_BUTTON.contains(x, y)) {
            tryUsePower();
        }
    }

    private void handleMenuKey(int code) {
        switch (code) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
            case KeyEvent.VK_1:
                place = Place.DAY;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
            case KeyEvent.VK_2:
                place = Place.NIGHT;
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                startRace();
                break;
            default:
                break;
        }
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

    private void startRace() {
        inMenu = false;
        enemies.clear();
        particles.clear();
        score = 0;
        distance = 0;
        lives = 3;
        speed = 8;
        running = true;
        invincible = 0;
        power = POWER_MAX * 0.35;
        powerBurst = 0;
        left = right = up = down = false;
        player.x = WIDTH / 2.0 - 21;
        player.y = HEIGHT - 160;
        player.tilt = 0;
    }

    private void restart() {
        inMenu = true;
        running = true;
        enemies.clear();
        particles.clear();
        left = right = up = down = false;
        powerBurst = 0;
        player.x = WIDTH / 2.0 - 21;
        player.y = HEIGHT - 160;
        player.tilt = 0;
    }

    private void tryUsePower() {
        if (!running || powerBurst > 0 || power < POWER_COST) {
            return;
        }
        power -= POWER_COST;
        powerBurst = POWER_BURST_FRAMES;
        burst(player.x + player.width / 2, player.y + player.height, new Color(80, 220, 255, 200));
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

        if (inMenu) {
            spawnIdleParticles();
            updateParticles();
            return;
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
            speed = Math.min(14, speed + 0.08);
            player.y = Math.max(80, player.y - 2);
        } else if (down) {
            speed = Math.max(5, speed - 0.18);
            player.y = Math.min(HEIGHT - 100, player.y + 3);
        } else {
            player.y += (HEIGHT - 160 - player.y) * 0.04;
            speed += (10 - speed) * 0.02;
        }

        if (powerBurst > 0) {
            powerBurst--;
            speed = Math.min(18, speed + 0.35);
            player.y = Math.max(70, player.y - 1.5);
        }

        player.x = Math.max(ROAD_LEFT + 8, Math.min(ROAD_RIGHT - player.width - 8, player.x));
        player.tilt += (targetTilt - player.tilt) * 0.25;

        if (powerBurst <= 0) {
            power = Math.min(POWER_MAX, power + 0.08 + speed * 0.01);
        }

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
                it.remove();
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
        powerBurst = 0;
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
        if (up || powerBurst > 0) {
            Color trail = powerBurst > 0
                    ? new Color(255, 200, 60, 200)
                    : new Color(80, 220, 255, 180);
            particles.add(new Particle(
                    player.x + 12 + random.nextInt(16),
                    player.y + player.height,
                    random.nextDouble() - 0.5,
                    6,
                    12,
                    6,
                    trail,
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
            Color idle = place == Place.DAY
                    ? new Color(255, 180, 60, 120)
                    : new Color(255, 80, 180, 120);
            particles.add(new Particle(
                    random.nextInt(WIDTH),
                    HEIGHT,
                    0,
                    -1.5,
                    40,
                    4,
                    idle,
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

        if (!inMenu) {
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
        } else {
            drawMenu(g);
        }
    }

    private void drawSky(Graphics2D g) {
        if (place == Place.DAY) {
            GradientPaint sky = new GradientPaint(0, 0, new Color(110, 180, 255), 0, HEIGHT, new Color(210, 235, 255));
            g.setPaint(sky);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(new Color(255, 220, 80));
            g.fillOval(WIDTH - 110, 36, 54, 54);
            g.setColor(new Color(255, 240, 160, 90));
            g.fillOval(WIDTH - 124, 24, 82, 82);
        } else {
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
    }

    private void drawScenery(Graphics2D g) {
        for (Scenery item : scenery) {
            if (place == Place.DAY) {
                g.setColor(new Color(90, 70, 40));
                g.fillRect((int) item.x + (int) item.size / 2 - 4, (int) item.y + 10, 8, (int) item.height);
                g.setColor(new Color(50, 160, 70));
                g.fillOval((int) item.x, (int) item.y - 10, (int) item.size * 2, (int) item.size * 2);
                g.setColor(new Color(255, 200, 80, 70));
                g.fillOval((int) item.x + 6, (int) item.y, 10, 10);
            } else {
                g.setColor(new Color(20, 80, 70));
                g.fillRect((int) item.x + (int) item.size / 2 - 4, (int) item.y + 10, 8, (int) item.height);
                g.setColor(new Color(40, 180, 120));
                g.fillOval((int) item.x, (int) item.y - 10, (int) item.size * 2, (int) item.size * 2);
                g.setColor(new Color(255, 80, 200, 50));
                g.fillOval((int) item.x + 6, (int) item.y, 10, 10);
            }
        }
    }

    private void drawRoad(Graphics2D g) {
        Color roadA = place == Place.DAY ? new Color(70, 72, 78) : new Color(28, 28, 36);
        Color roadB = place == Place.DAY ? new Color(52, 54, 60) : new Color(18, 18, 26);
        GradientPaint road = new GradientPaint(ROAD_LEFT, 0, roadA, ROAD_RIGHT, 0, roadB);
        g.setPaint(road);
        Path2D path = new Path2D.Double();
        path.moveTo(ROAD_LEFT - 20, HEIGHT);
        path.lineTo(ROAD_LEFT, 0);
        path.lineTo(ROAD_RIGHT, 0);
        path.lineTo(ROAD_RIGHT + 20, HEIGHT);
        path.closePath();
        g.fill(path);

        g.setColor(place == Place.DAY ? new Color(220, 80, 60) : new Color(255, 60, 160));
        g.setStroke(new BasicStroke(6f));
        g.drawLine(ROAD_LEFT, 0, ROAD_LEFT - 20, HEIGHT);
        g.setColor(place == Place.DAY ? new Color(40, 140, 220) : new Color(80, 220, 255));
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
        if (place == Place.NIGHT) {
            g.setColor(new Color(0, 255, 220, (int) (40 * glow)));
            g.fillRect(ROAD_LEFT, 0, ROAD_RIGHT - ROAD_LEFT, HEIGHT);
        } else if (powerBurst > 0) {
            g.setColor(new Color(255, 200, 60, (int) (28 * glow)));
            g.fillRect(ROAD_LEFT, 0, ROAD_RIGHT - ROAD_LEFT, HEIGHT);
        }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(16, 16, 200, 118, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("SCORE  " + score, 28, 42);
        g.drawString("SPEED  " + (int) (speed * 12) + " km/h", 28, 66);
        g.drawString("LIVES  " + lives, 28, 90);
        g.drawString("PLACE  " + place.name(), 28, 114);

        int barX = 28;
        int barY = HEIGHT - 48;
        int barW = 160;
        int barH = 16;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(barX - 8, barY - 22, barW + 16, 48, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("POWER", barX, barY - 6);
        g.setColor(new Color(40, 40, 50));
        g.fillRoundRect(barX, barY, barW, barH, 8, 8);
        int fill = (int) (barW * (power / POWER_MAX));
        Color powerColor = powerBurst > 0
                ? new Color(255, 200, 60)
                : (power >= POWER_COST ? new Color(80, 220, 255) : new Color(120, 130, 150));
        g.setColor(powerColor);
        g.fillRoundRect(barX, barY, Math.max(0, fill), barH, 8, 8);

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(WIDTH - 178, 16, 162, 48, 16, 16);
        g.setColor(new Color(180, 230, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Arrows / WASD", WIDTH - 160, 36);
        g.drawString("or click Use Power", WIDTH - 160, 54);

        boolean canPower = powerBurst <= 0 && power >= POWER_COST;
        if (powerBurst > 0) {
            g.setColor(new Color(255, 200, 60));
        } else if (canPower) {
            g.setColor(new Color(0, 180, 220));
        } else {
            g.setColor(new Color(70, 75, 90));
        }
        g.fillRoundRect(POWER_BUTTON.x, POWER_BUTTON.y, POWER_BUTTON.width, POWER_BUTTON.height, 14, 14);
        g.setColor(canPower || powerBurst > 0 ? Color.WHITE : new Color(160, 165, 180));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(POWER_BUTTON.x, POWER_BUTTON.y, POWER_BUTTON.width, POWER_BUTTON.height, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String powerLabel = powerBurst > 0 ? "POWER ACTIVE" : "USE POWER";
        g.drawString(powerLabel, POWER_BUTTON.x + 18, POWER_BUTTON.y + 28);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(40, 150, WIDTH - 80, 380, 24, 24);

        g.setColor(place == Place.DAY ? new Color(255, 200, 80) : new Color(255, 80, 180));
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString("NEON RUSH", 140, 210);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Choose place", 170, 260);

        drawPlaceOption(g, DAY_OPTION.x, DAY_OPTION.y, "DAY", place == Place.DAY);
        drawPlaceOption(g, NIGHT_OPTION.x, NIGHT_OPTION.y, "NIGHT", place == Place.NIGHT);

        g.setColor(new Color(40, 160, 255));
        g.fillRoundRect(START_BUTTON.x, START_BUTTON.y, START_BUTTON.width, START_BUTTON.height, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("START RACE", START_BUTTON.x + 38, START_BUTTON.y + 28);

        g.setColor(new Color(200, 220, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Click DAY / NIGHT, then START", 118, 470);
        g.drawString("Keys: LEFT/RIGHT + ENTER also work", 108, 494);
    }

    private void drawPlaceOption(Graphics2D g, int x, int y, String label, boolean selected) {
        if (selected) {
            g.setColor(new Color(255, 255, 255, 40));
            g.fillRoundRect(x, y, 150, 90, 16, 16);
            g.setColor(new Color(80, 220, 255));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(x, y, 150, 90, 16, 16);
        } else {
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRoundRect(x, y, 150, 90, 16, 16);
            g.setColor(new Color(255, 255, 255, 50));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, 150, 90, 16, 16);
        }
        g.setColor(selected ? Color.WHITE : new Color(180, 180, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(label, x + 44, y + 52);
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
        g.drawString("Press ENTER for place select", 98, HEIGHT / 2 + 50);
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
