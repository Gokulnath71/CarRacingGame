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
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class FightPanel extends JPanel implements ActionListener {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    private final Timer timer;
    private final Random random = new Random();
    private final Fighter player;
    private final Fighter enemy;
    private final List<HitParticle> particles = new ArrayList<>();

    private boolean left;
    private boolean right;
    private boolean up;
    private boolean punch;
    private boolean kick;
    private boolean block;

    private int round = 1;
    private int playerWins;
    private int enemyWins;
    private int roundTimer = 99 * 60; // frames
    private String banner = "";
    private int bannerTimer;
    private int freezeFrames;
    private double shake;
    private double flash;
    private double bgPulse;
    private boolean matchOver;
    private boolean roundActive = true;
    private boolean paused;
    private int aiCooldown;
    private javax.swing.Timer roundDelayTimer;

    public FightPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        player = new Fighter(180, true, new Color(32, 48, 58), new Color(200, 55, 48), "KAGE");
        enemy = new Fighter(720, false, new Color(22, 28, 36), new Color(196, 168, 96), "RENN");

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_P) {
                    if (!matchOver) {
                        togglePause();
                    }
                    return;
                }
                if (paused) {
                    return;
                }
                setKey(code, true);
                if (matchOver && code == KeyEvent.VK_ENTER) {
                    restartMatch();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (paused) {
                    return;
                }
                setKey(e.getKeyCode(), false);
            }
        });

        startRound();
        timer = new Timer(16, this);
        timer.start();
    }

    private void setKey(int code, boolean pressed) {
        switch (code) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                left = pressed;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                right = pressed;
                break;
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_SPACE:
                up = pressed;
                break;
            case KeyEvent.VK_J:
            case KeyEvent.VK_Z:
                punch = pressed;
                break;
            case KeyEvent.VK_K:
            case KeyEvent.VK_X:
                kick = pressed;
                break;
            case KeyEvent.VK_L:
            case KeyEvent.VK_C:
            case KeyEvent.VK_SHIFT:
                block = pressed;
                break;
            default:
                break;
        }
    }

    private void startRound() {
        cancelRoundDelay();
        player.reset(180);
        enemy.reset(720);
        particles.clear();
        roundTimer = 99 * 60;
        roundActive = true;
        matchOver = false;
        paused = false;
        freezeFrames = 0;
        shake = 0;
        flash = 0;
        clearInputs();
        banner = "ROUND " + round;
        bannerTimer = 90;
    }

    private void restartMatch() {
        round = 1;
        playerWins = 0;
        enemyWins = 0;
        startRound();
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            clearInputs();
            if (roundDelayTimer != null && roundDelayTimer.isRunning()) {
                roundDelayTimer.stop();
            }
        } else if (roundDelayTimer != null && !roundActive && !matchOver && bannerTimer > 0) {
            // resume waiting for next round if we paused during the inter-round delay
            roundDelayTimer.start();
        }
    }

    private void clearInputs() {
        left = false;
        right = false;
        up = false;
        punch = false;
        kick = false;
        block = false;
    }

    private void cancelRoundDelay() {
        if (roundDelayTimer != null) {
            roundDelayTimer.stop();
            roundDelayTimer = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused) {
            bgPulse += 0.02;
            repaint();
            return;
        }

        bgPulse += 0.04;

        if (freezeFrames > 0) {
            freezeFrames--;
            updateParticlesOnly();
            repaint();
            return;
        }

        if (bannerTimer > 0) {
            bannerTimer--;
            // hold fighters still during intro banner after first frames
            if (bannerTimer > 40 && banner.startsWith("ROUND")) {
                player.faceToward(enemy);
                enemy.faceToward(player);
                updateParticlesOnly();
                repaint();
                return;
            }
        }

        if (shake > 0.2) {
            shake *= 0.88;
        } else {
            shake = 0;
        }
        if (flash > 0) {
            flash *= 0.9;
        }

        if (roundActive && !matchOver) {
            roundTimer--;
            player.faceToward(enemy);
            enemy.faceToward(player);

            player.update(left, right, up, punch, kick, block);
            updateAI();
            resolveAttacks();
            keepApart();

            if (player.hp <= 0 || enemy.hp <= 0 || roundTimer <= 0) {
                endRound();
            }
        } else if (!matchOver && bannerTimer <= 0) {
            // brief pause then next round already triggered in endRound
        }

        updateParticlesOnly();
        repaint();
    }

    private void updateParticlesOnly() {
        Iterator<HitParticle> it = particles.iterator();
        while (it.hasNext()) {
            if (!it.next().update()) {
                it.remove();
            }
        }
    }

    private void updateAI() {
        double dist = enemy.x - player.x;
        boolean aiLeft = false;
        boolean aiRight = false;
        boolean aiJump = false;
        boolean aiPunch = false;
        boolean aiKick = false;
        boolean aiBlock = false;

        if (aiCooldown > 0) {
            aiCooldown--;
        }

        if (enemy.state == Fighter.State.KO || enemy.state == Fighter.State.HURT) {
            enemy.update(false, false, false, false, false, false);
            return;
        }

        // approach or retreat
        if (Math.abs(dist) > 140) {
            if (dist > 0) {
                aiLeft = true;
            } else {
                aiRight = true;
            }
        } else if (Math.abs(dist) < 70) {
            if (dist > 0) {
                aiRight = true;
            } else {
                aiLeft = true;
            }
        }

        if (player.punchActive() || player.kickActive()) {
            if (random.nextDouble() < 0.55) {
                aiBlock = true;
            } else if (random.nextDouble() < 0.25) {
                aiJump = true;
            }
        }

        if (!aiBlock && aiCooldown == 0 && Math.abs(dist) < 130) {
            double r = random.nextDouble();
            if (r < 0.35) {
                aiPunch = true;
                aiCooldown = 18 + random.nextInt(20);
            } else if (r < 0.55) {
                aiKick = true;
                aiCooldown = 24 + random.nextInt(22);
            } else if (r < 0.62) {
                aiJump = true;
                aiCooldown = 30;
            }
        }

        // occasional hop when far
        if (Math.abs(dist) > 200 && random.nextDouble() < 0.01) {
            aiJump = true;
        }

        enemy.update(aiLeft, aiRight, aiJump, aiPunch, aiKick, aiBlock);
    }

    private void resolveAttacks() {
        checkHit(player, enemy);
        checkHit(enemy, player);
    }

    private void checkHit(Fighter attacker, Fighter defender) {
        if (attacker.attackHit) {
            return;
        }
        double hitX;
        double hitY;
        int damage;
        boolean isKick;
        double radius;

        if (attacker.punchActive()) {
            hitX = attacker.punchHitX();
            hitY = attacker.y - 72;
            damage = 9;
            isKick = false;
            radius = 30;
        } else if (attacker.kickActive()) {
            hitX = attacker.kickHitX();
            hitY = attacker.y - 42;
            damage = 15;
            isKick = true;
            radius = 36;
        } else {
            return;
        }

        double dx = hitX - (defender.x + Fighter.WIDTH / 2);
        double dy = hitY - (defender.y - 55);
        if (dx * dx + dy * dy > radius * radius * 2.2) {
            // also AABB fallback for reliability
            double defL = defender.x;
            double defR = defender.x + Fighter.WIDTH;
            double defT = defender.y - Fighter.HEIGHT;
            double defB = defender.y;
            if (hitX < defL - 10 || hitX > defR + 10 || hitY < defT || hitY > defB + 10) {
                return;
            }
        }

        double knock = attacker.facingRight ? 1 : -1;
        boolean blocked = defender.blocking;
        if (defender.takeHit(knock, damage, isKick)) {
            attacker.attackHit = true;
            spawnHitFx(hitX, hitY, attacker.accent, isKick, blocked);
            shake = blocked ? 5 : (isKick ? 12 : 8);
            flash = blocked ? 0.12 : 0.28;
            freezeFrames = blocked ? 3 : (isKick ? 8 : 5);
        }
    }

    private void spawnHitFx(double x, double y, Color color, boolean heavy, boolean blocked) {
        // steel sparks
        int sparks = blocked ? 10 : (heavy ? 20 : 14);
        for (int i = 0; i < sparks; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double spd = 3 + random.nextDouble() * (heavy ? 9 : 6);
            particles.add(new HitParticle(
                    x, y,
                    Math.cos(ang) * spd,
                    Math.sin(ang) * spd - 2.5,
                    14 + random.nextInt(12),
                    2 + random.nextDouble() * 4,
                    blocked ? new Color(230, 230, 240) : color,
                    true));
        }
        // smoke puffs
        int smoke = blocked ? 4 : (heavy ? 10 : 7);
        for (int i = 0; i < smoke; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double spd = 0.6 + random.nextDouble() * 2.2;
            particles.add(new HitParticle(
                    x, y,
                    Math.cos(ang) * spd,
                    Math.sin(ang) * spd - 1.2,
                    22 + random.nextInt(18),
                    10 + random.nextDouble() * 14,
                    new Color(40, 42, 48),
                    false));
        }
        // slash streak
        double dir = random.nextBoolean() ? 1 : -1;
        for (int i = 0; i < 5; i++) {
            particles.add(new HitParticle(
                    x + dir * i * 4,
                    y - 6 + i * 2,
                    dir * (4 + i),
                    -1 + i * 0.3,
                    10,
                    8,
                    Color.WHITE,
                    true));
        }
    }

    private void keepApart() {
        double gap = (player.x + Fighter.WIDTH) - enemy.x;
        if (gap > 0) {
            player.x -= gap / 2;
            enemy.x += gap / 2;
        }
    }

    private void endRound() {
        roundActive = false;
        String winner;
        if (player.hp > enemy.hp) {
            playerWins++;
            winner = player.name + " WINS";
        } else if (enemy.hp > player.hp) {
            enemyWins++;
            winner = enemy.name + " WINS";
        } else {
            winner = "DRAW";
        }

        if (playerWins >= 2 || enemyWins >= 2) {
            matchOver = true;
            banner = playerWins >= 2 ? player.name + " — VICTORY!" : enemy.name + " — VICTORY!";
            bannerTimer = 9999;
            shake = 12;
            flash = 0.5;
        } else {
            banner = winner;
            bannerTimer = 120;
            round++;
            cancelRoundDelay();
            roundDelayTimer = new javax.swing.Timer(2200, ev -> {
                ((javax.swing.Timer) ev.getSource()).stop();
                roundDelayTimer = null;
                if (!matchOver && !paused) {
                    startRound();
                }
            });
            roundDelayTimer.setRepeats(false);
            roundDelayTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double ox = (shake > 0) ? (random.nextDouble() - 0.5) * shake * 2 : 0;
        double oy = (shake > 0) ? (random.nextDouble() - 0.5) * shake * 2 : 0;
        g2.translate(ox, oy);

        drawArena(g2);
        for (HitParticle p : particles) {
            p.draw(g2);
        }
        player.draw(g2);
        enemy.draw(g2);
        drawHud(g2);

        if (flash > 0.05) {
            g2.setColor(new Color(255, 240, 220, (int) (flash * 140)));
            g2.fillRect(-20, -20, WIDTH + 40, HEIGHT + 40);
        }

        if (bannerTimer > 0 || matchOver) {
            drawBanner(g2);
        }

        if (paused) {
            drawPauseOverlay(g2);
        }

        g2.dispose();
    }

    private void drawArena(Graphics2D g) {
        // night sky
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(8, 12, 28),
                0, (float) Fighter.GROUND_Y, new Color(28, 36, 52));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // distant mist mountains
        g.setColor(new Color(18, 26, 40));
        int[] mx = {0, 120, 220, 340, 480, 620, 760, 900, 960};
        int[] my = {320, 250, 290, 210, 270, 230, 300, 260, 320};
        g.fillPolygon(mx, my, mx.length);
        g.setColor(new Color(24, 34, 48));
        int[] mx2 = {0, 160, 300, 460, 600, 780, 960};
        int[] my2 = {360, 300, 330, 280, 320, 290, 360};
        g.fillPolygon(mx2, my2, mx2.length);

        // moon + soft glow
        double moonPulse = 2 + Math.sin(bgPulse * 0.7);
        g.setColor(new Color(180, 200, 230, 35));
        g.fillOval(700 - (int) (moonPulse * 10), 40 - (int) moonPulse, 110 + (int) (moonPulse * 20), 110 + (int) (moonPulse * 20));
        g.setColor(new Color(230, 235, 245, 230));
        g.fillOval(720, 55, 78, 78);
        g.setColor(new Color(200, 210, 225, 80));
        g.fillOval(738, 72, 18, 16);
        g.fillOval(758, 88, 12, 10);

        // stars
        g.setColor(new Color(220, 230, 255, 160));
        int[][] stars = {{60, 40}, {140, 70}, {210, 30}, {400, 55}, {520, 25}, {610, 80}, {880, 45}, {940, 90}};
        for (int[] s : stars) {
            double tw = 0.55 + 0.45 * Math.sin(bgPulse * 2 + s[0]);
            g.setColor(new Color(220, 230, 255, (int) (80 + tw * 140)));
            g.fillOval(s[0], s[1], 2, 2);
        }

        // shrine rooftop silhouettes
        g.setColor(new Color(12, 16, 24));
        drawPagoda(g, 70, (int) Fighter.GROUND_Y - 10, 0.85);
        drawPagoda(g, 820, (int) Fighter.GROUND_Y - 10, 0.75);

        // paper lanterns
        drawLantern(g, 150, 160, new Color(200, 70, 50));
        drawLantern(g, 780, 150, new Color(200, 70, 50));

        // tiled rooftop platform
        int gy = (int) Fighter.GROUND_Y;
        GradientPaint floor = new GradientPaint(
                0, gy - 20, new Color(42, 38, 36),
                0, HEIGHT, new Color(18, 16, 18));
        g.setPaint(floor);
        g.fillRect(0, gy + 4, WIDTH, HEIGHT - gy);

        // wood plank lines
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(70, 58, 48, 160));
        for (int y = gy + 10; y < HEIGHT; y += 16) {
            g.drawLine(0, y, WIDTH, y);
        }
        for (int x = 0; x < WIDTH; x += 48) {
            g.drawLine(x, gy + 4, x + 8, HEIGHT);
        }

        // roof edge / ridge
        g.setColor(new Color(28, 24, 22));
        g.fillRect(0, gy - 6, WIDTH, 12);
        g.setColor(new Color(160, 50, 42, 180));
        g.fillRect(30, gy - 4, WIDTH - 60, 4);
        g.setColor(new Color(210, 190, 140, 90));
        g.drawLine(40, gy + 2, WIDTH - 40, gy + 2);

        // moonlight wash on floor
        g.setColor(new Color(160, 190, 230, 18));
        g.fillOval(WIDTH / 2 - 220, gy - 10, 440, 70);
    }

    private void drawPagoda(Graphics2D g, int x, int baseY, double scale) {
        int w = (int) (70 * scale);
        int h = (int) (110 * scale);
        g.fillRect(x - w / 4, baseY - h, w / 2, h);
        for (int i = 0; i < 3; i++) {
            int yw = w + i * 8;
            int yy = baseY - h + i * (int) (28 * scale);
            g.fillPolygon(
                    new int[]{x - yw / 2, x, x + yw / 2},
                    new int[]{yy + 12, yy - 8, yy + 12},
                    3);
        }
    }

    private void drawLantern(Graphics2D g, int x, int y, Color glow) {
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(60, 50, 40));
        g.drawLine(x, y - 20, x, y);
        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 50));
        g.fillOval(x - 18, y - 4, 36, 40);
        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 200));
        g.fillRoundRect(x - 10, y, 20, 26, 8, 8);
        g.setColor(new Color(40, 30, 24));
        g.fillRect(x - 11, y - 2, 22, 4);
        g.fillRect(x - 11, y + 24, 22, 4);
    }

    private void drawHud(Graphics2D g) {
        drawHealthBar(g, 40, 28, 360, player, true);
        drawHealthBar(g, WIDTH - 400, 28, 360, enemy, false);

        // timer
        g.setFont(new Font("Impact", Font.BOLD, 42));
        String time = String.valueOf(Math.max(0, roundTimer / 60));
        int tw = g.getFontMetrics().stringWidth(time);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(WIDTH / 2 - 40, 18, 80, 48, 8, 8);
        g.setColor(new Color(230, 220, 190));
        g.drawString(time, WIDTH / 2 - tw / 2, 54);

        // round markers
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(210, 200, 170, 200));
        g.drawString("DUEL — BEST OF 3", WIDTH / 2 - 52, 82);
        for (int i = 0; i < 2; i++) {
            g.setColor(i < playerWins ? player.accent : new Color(255, 255, 255, 40));
            g.fillOval(WIDTH / 2 - 50 - i * 18, 92, 11, 11);
            g.setColor(i < enemyWins ? enemy.accent : new Color(255, 255, 255, 40));
            g.fillOval(WIDTH / 2 + 38 + i * 18, 92, 11, 11);
        }

        // controls hint
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(210, 205, 190, 150));
        g.drawString("A/D move   W jump   J slash   K sweep   L guard   Esc/P pause", 20, HEIGHT - 16);
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(6, 10, 18, 190));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(200, 55, 48, 110));
        g.drawLine(WIDTH / 2 - 230, HEIGHT / 2 - 6, WIDTH / 2 - 70, HEIGHT / 2 - 40);
        g.setColor(new Color(196, 168, 96, 110));
        g.drawLine(WIDTH / 2 + 70, HEIGHT / 2 + 34, WIDTH / 2 + 230, HEIGHT / 2 + 6);

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(WIDTH / 2 - 160, HEIGHT / 2 - 56, 320, 112, 10, 10);

        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(200, 55, 48, 180));
        g.drawRoundRect(WIDTH / 2 - 160, HEIGHT / 2 - 56, 320, 112, 10, 10);

        float pulse = 0.88f + (float) (Math.sin(bgPulse * 4) * 0.12);
        g.setFont(new Font("Impact", Font.BOLD, 52));
        g.setColor(new Color(230, 220, 190, (int) (pulse * 255)));
        String title = "PAUSED";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, WIDTH / 2 - tw / 2, HEIGHT / 2 + 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(220, 210, 190, 210));
        String hint = "Press Esc or P to resume";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, WIDTH / 2 - hw / 2, HEIGHT / 2 + 36);
    }

    private void drawHealthBar(Graphics2D g, int x, int y, int w, Fighter f, boolean leftAlign) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new RoundRectangle2D.Double(x - 4, y - 4, w + 8, 36, 12, 12));

        g.setFont(new Font("Impact", Font.PLAIN, 18));
        g.setColor(f.accent);
        if (leftAlign) {
            g.drawString(f.name, x, y - 8);
        } else {
            int nw = g.getFontMetrics().stringWidth(f.name);
            g.drawString(f.name, x + w - nw, y - 8);
        }

        g.setColor(new Color(40, 40, 40));
        g.fill(new RoundRectangle2D.Double(x, y, w, 22, 10, 10));

        double pct = f.hp / (double) f.maxHp;
        Color bar = pct > 0.5 ? new Color(60, 220, 120) : (pct > 0.25 ? new Color(255, 200, 40) : new Color(255, 60, 70));
        int bw = (int) (w * pct);
        if (bw > 0) {
            if (leftAlign) {
                g.setPaint(new GradientPaint(x, y, bar, x + bw, y, bar.brighter()));
                g.fill(new RoundRectangle2D.Double(x, y, bw, 22, 10, 10));
            } else {
                g.setPaint(new GradientPaint(x + w - bw, y, bar.brighter(), x + w, y, bar));
                g.fill(new RoundRectangle2D.Double(x + w - bw, y, bw, 22, 10, 10));
            }
        }

        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, w, 22, 10, 10));
    }

    private void drawBanner(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, HEIGHT / 2 - 50, WIDTH, 100);

        float pulse = 1f;
        if (!matchOver) {
            pulse = 0.85f + (float) (Math.sin(bgPulse * 3) * 0.15);
        }
        g.setFont(new Font("Impact", Font.BOLD, matchOver ? 48 : 40));
        g.setColor(new Color(230, 215, 170, (int) (pulse * 255)));
        int tw = g.getFontMetrics().stringWidth(banner);
        g.drawString(banner, WIDTH / 2 - tw / 2, HEIGHT / 2 + 14);

        if (matchOver) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(220, 210, 190));
            String hint = "Press ENTER to duel again";
            int hw = g.getFontMetrics().stringWidth(hint);
            g.drawString(hint, WIDTH / 2 - hw / 2, HEIGHT / 2 + 44);
        }
    }
}
