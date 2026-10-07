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
    private int aiCooldown;

    public FightPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        player = new Fighter(180, true, new Color(40, 160, 255), new Color(255, 210, 40), "BLAZE");
        enemy = new Fighter(720, false, new Color(255, 70, 90), new Color(180, 40, 255), "RAZOR");

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                setKey(e.getKeyCode(), true);
                if (matchOver && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    restartMatch();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
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
        player.reset(180);
        enemy.reset(720);
        particles.clear();
        roundTimer = 99 * 60;
        roundActive = true;
        matchOver = false;
        freezeFrames = 0;
        shake = 0;
        flash = 0;
        banner = "ROUND " + round;
        bannerTimer = 90;
    }

    private void restartMatch() {
        round = 1;
        playerWins = 0;
        enemyWins = 0;
        startRound();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
            hitY = attacker.y - 70;
            damage = 8;
            isKick = false;
            radius = 28;
        } else if (attacker.kickActive()) {
            hitX = attacker.kickHitX();
            hitY = attacker.y - 40;
            damage = 14;
            isKick = true;
            radius = 34;
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
            shake = blocked ? 4 : (isKick ? 10 : 7);
            flash = blocked ? 0.15 : 0.35;
            freezeFrames = blocked ? 2 : (isKick ? 6 : 4);
        }
    }

    private void spawnHitFx(double x, double y, Color color, boolean heavy, boolean blocked) {
        int count = blocked ? 8 : (heavy ? 22 : 14);
        for (int i = 0; i < count; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double spd = 2 + random.nextDouble() * (heavy ? 8 : 5);
            particles.add(new HitParticle(
                    x, y,
                    Math.cos(ang) * spd,
                    Math.sin(ang) * spd - 2,
                    18 + random.nextInt(16),
                    4 + random.nextDouble() * 8,
                    blocked ? Color.WHITE : color,
                    random.nextBoolean()));
        }
        // impact ring particles
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3;
            particles.add(new HitParticle(
                    x, y,
                    Math.cos(ang) * 6,
                    Math.sin(ang) * 6,
                    12,
                    10,
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
            // schedule next round via banner timer ending
            javax.swing.Timer t = new javax.swing.Timer(2200, ev -> {
                ((javax.swing.Timer) ev.getSource()).stop();
                if (!matchOver) {
                    startRound();
                }
            });
            t.setRepeats(false);
            t.start();
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
            g2.setColor(new Color(255, 255, 255, (int) (flash * 160)));
            g2.fillRect(-20, -20, WIDTH + 40, HEIGHT + 40);
        }

        if (bannerTimer > 0 || matchOver) {
            drawBanner(g2);
        }

        g2.dispose();
    }

    private void drawArena(Graphics2D g) {
        // sky
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(28, 18, 58),
                0, HEIGHT, new Color(90, 30, 70));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // neon sun
        double sunPulse = 18 + Math.sin(bgPulse) * 6;
        g.setColor(new Color(255, 120, 60, 80));
        g.fillOval(WIDTH / 2 - 70, 70, 140, 140);
        g.setColor(new Color(255, 200, 80, 200));
        g.fillOval((int) (WIDTH / 2 - sunPulse), (int) (90 - sunPulse * 0.1), (int) (sunPulse * 2), (int) (sunPulse * 2));

        // city silhouettes
        g.setColor(new Color(15, 10, 30));
        int[] buildingW = {70, 50, 90, 40, 110, 60, 80, 55, 95, 45};
        int bx = 20;
        for (int w : buildingW) {
            int h = 80 + (w * 2) % 120;
            g.fillRect(bx, (int) Fighter.GROUND_Y - h + 20, w, h);
            g.setColor(new Color(255, 200, 80, 40 + (int) (Math.sin(bgPulse + bx) * 20)));
            for (int yy = (int) Fighter.GROUND_Y - h + 35; yy < Fighter.GROUND_Y; yy += 18) {
                for (int xx = bx + 8; xx < bx + w - 8; xx += 14) {
                    g.fillRect(xx, yy, 6, 8);
                }
            }
            g.setColor(new Color(15, 10, 30));
            bx += w + 12;
        }

        // ground
        GradientPaint floor = new GradientPaint(
                0, (float) Fighter.GROUND_Y, new Color(50, 25, 55),
                0, HEIGHT, new Color(20, 10, 25));
        g.setPaint(floor);
        g.fillRect(0, (int) Fighter.GROUND_Y + 10, WIDTH, HEIGHT);

        // neon floor lines
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(0, 255, 220, 120));
        g.drawLine(60, (int) Fighter.GROUND_Y + 12, WIDTH - 60, (int) Fighter.GROUND_Y + 12);
        g.setColor(new Color(255, 40, 120, 100));
        g.drawLine(80, (int) Fighter.GROUND_Y + 28, WIDTH - 80, (int) Fighter.GROUND_Y + 28);

        // arena platform edge glow
        g.setColor(new Color(120, 60, 255, 90));
        g.fillRoundRect(40, (int) Fighter.GROUND_Y + 8, WIDTH - 80, 18, 12, 12);
    }

    private void drawHud(Graphics2D g) {
        drawHealthBar(g, 40, 28, 360, player, true);
        drawHealthBar(g, WIDTH - 400, 28, 360, enemy, false);

        // timer
        g.setFont(new Font("Impact", Font.BOLD, 42));
        String time = String.valueOf(Math.max(0, roundTimer / 60));
        int tw = g.getFontMetrics().stringWidth(time);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(WIDTH / 2 - 40, 18, 80, 48, 12, 12);
        g.setColor(Color.WHITE);
        g.drawString(time, WIDTH / 2 - tw / 2, 54);

        // round markers
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 255, 255, 180));
        g.drawString("BEST OF 3", WIDTH / 2 - 34, 82);
        for (int i = 0; i < 2; i++) {
            g.setColor(i < playerWins ? player.accent : new Color(255, 255, 255, 50));
            g.fillOval(WIDTH / 2 - 50 - i * 18, 92, 12, 12);
            g.setColor(i < enemyWins ? enemy.accent : new Color(255, 255, 255, 50));
            g.fillOval(WIDTH / 2 + 38 + i * 18, 92, 12, 12);
        }

        // controls hint
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(255, 255, 255, 140));
        g.drawString("A/D move   W jump   J punch   K kick   L block", 20, HEIGHT - 16);
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
        g.setColor(new Color(255, 230, 80, (int) (pulse * 255)));
        int tw = g.getFontMetrics().stringWidth(banner);
        g.drawString(banner, WIDTH / 2 - tw / 2, HEIGHT / 2 + 14);

        if (matchOver) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            String hint = "Press ENTER to fight again";
            int hw = g.getFontMetrics().stringWidth(hint);
            g.drawString(hint, WIDTH / 2 - hw / 2, HEIGHT / 2 + 44);
        }
    }
}
