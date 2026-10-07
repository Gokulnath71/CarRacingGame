import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class Fighter {
    public enum State {
        IDLE, WALK, JUMP, PUNCH, KICK, BLOCK, HURT, KO
    }

    public double x;
    public double y;
    public double vx;
    public double vy;
    public final boolean facingRightDefault;
    public boolean facingRight;
    public final Color primary;
    public final Color accent;
    public final String name;

    public int maxHp = 100;
    public int hp = 100;
    public State state = State.IDLE;
    public int stateTimer;
    public int attackCooldown;
    public int invincible;
    public boolean onGround = true;
    public boolean blocking;

    public double anim;
    public double punchReach;
    public double kickReach;
    public double hurtFlash;
    public double bob;
    public boolean attackHit; // already connected this swing

    public static final double WIDTH = 54;
    public static final double HEIGHT = 110;
    public static final double GROUND_Y = 520;

    public Fighter(double x, boolean faceRight, Color primary, Color accent, String name) {
        this.x = x;
        this.y = GROUND_Y;
        this.facingRightDefault = faceRight;
        this.facingRight = faceRight;
        this.primary = primary;
        this.accent = accent;
        this.name = name;
    }

    public void reset(double startX) {
        x = startX;
        y = GROUND_Y;
        vx = 0;
        vy = 0;
        hp = maxHp;
        state = State.IDLE;
        stateTimer = 0;
        attackCooldown = 0;
        invincible = 0;
        onGround = true;
        blocking = false;
        punchReach = 0;
        kickReach = 0;
        hurtFlash = 0;
        attackHit = false;
        facingRight = facingRightDefault;
    }

    public void update(boolean left, boolean right, boolean jump, boolean punch, boolean kick, boolean block) {
        anim += 0.18;
        bob = Math.sin(anim) * 3;

        if (invincible > 0) {
            invincible--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (hurtFlash > 0) {
            hurtFlash *= 0.85;
            if (hurtFlash < 0.05) {
                hurtFlash = 0;
            }
        }

        if (state == State.KO) {
            stateTimer++;
            vy += 0.9;
            y += vy;
            if (y > GROUND_Y) {
                y = GROUND_Y;
                vy = 0;
            }
            punchReach *= 0.8;
            kickReach *= 0.8;
            return;
        }

        if (state == State.HURT) {
            stateTimer--;
            x += vx;
            vx *= 0.9;
            applyGravity();
            clampBounds();
            if (stateTimer <= 0) {
                state = State.IDLE;
                vx = 0;
            }
            return;
        }

        if (state == State.PUNCH || state == State.KICK) {
            stateTimer--;
            if (state == State.PUNCH) {
                punchReach = easeOut(1.0 - stateTimer / 16.0) * 48;
                kickReach *= 0.7;
            } else {
                kickReach = easeOut(1.0 - stateTimer / 20.0) * 62;
                punchReach *= 0.7;
            }
            applyGravity();
            clampBounds();
            if (stateTimer <= 0) {
                state = onGround ? State.IDLE : State.JUMP;
                punchReach = 0;
                kickReach = 0;
                attackHit = false;
                attackCooldown = 8;
            }
            return;
        }

        blocking = block && onGround && attackCooldown == 0;
        if (blocking) {
            state = State.BLOCK;
            vx *= 0.6;
            punchReach = 0;
            kickReach = 0;
            applyGravity();
            clampBounds();
            return;
        }

        if (onGround && punch && attackCooldown == 0) {
            state = State.PUNCH;
            stateTimer = 16;
            attackHit = false;
            vx *= 0.3;
            return;
        }
        if (onGround && kick && attackCooldown == 0) {
            state = State.KICK;
            stateTimer = 20;
            attackHit = false;
            vx *= 0.2;
            return;
        }

        double speed = 4.2;
        if (left) {
            vx = -speed;
            facingRight = false;
            state = onGround ? State.WALK : State.JUMP;
        } else if (right) {
            vx = speed;
            facingRight = true;
            state = onGround ? State.WALK : State.JUMP;
        } else {
            vx *= 0.75;
            if (Math.abs(vx) < 0.2) {
                vx = 0;
            }
            if (onGround) {
                state = State.IDLE;
            }
        }

        if (jump && onGround) {
            vy = -14.5;
            onGround = false;
            state = State.JUMP;
        }

        x += vx;
        applyGravity();
        clampBounds();
        punchReach *= 0.7;
        kickReach *= 0.7;
    }

    private void applyGravity() {
        vy += 0.85;
        y += vy;
        if (y >= GROUND_Y) {
            y = GROUND_Y;
            vy = 0;
            onGround = true;
        } else {
            onGround = false;
        }
    }

    private void clampBounds() {
        if (x < 40) {
            x = 40;
        }
        if (x > 860) {
            x = 860;
        }
    }

    public void faceToward(Fighter other) {
        if (state == State.IDLE || state == State.WALK || state == State.BLOCK) {
            facingRight = other.x >= x;
        }
    }

    public boolean takeHit(double knockDir, int damage, boolean isKick) {
        if (invincible > 0 || state == State.KO) {
            return false;
        }
        if (blocking && ((knockDir > 0 && !facingRight) || (knockDir < 0 && facingRight))) {
            hp = Math.max(0, hp - damage / 4);
            vx = knockDir * 2.5;
            hurtFlash = 0.4;
            return true;
        }
        hp = Math.max(0, hp - damage);
        hurtFlash = 1;
        invincible = 12;
        if (hp <= 0) {
            state = State.KO;
            stateTimer = 0;
            vx = knockDir * 6;
            vy = -8;
            onGround = false;
        } else {
            state = State.HURT;
            stateTimer = isKick ? 18 : 14;
            vx = knockDir * (isKick ? 7 : 5);
            vy = isKick ? -4 : -2;
            onGround = false;
        }
        punchReach = 0;
        kickReach = 0;
        return true;
    }

    public double attackOriginX() {
        return facingRight ? x + WIDTH * 0.55 : x + WIDTH * 0.45;
    }

    public double punchHitX() {
        double reach = punchReach;
        return facingRight ? attackOriginX() + reach : attackOriginX() - reach;
    }

    public double kickHitX() {
        double reach = kickReach;
        return facingRight ? attackOriginX() + reach : attackOriginX() - reach;
    }

    public boolean punchActive() {
        return state == State.PUNCH && stateTimer <= 12 && stateTimer >= 6 && punchReach > 20;
    }

    public boolean kickActive() {
        return state == State.KICK && stateTimer <= 14 && stateTimer >= 6 && kickReach > 28;
    }

    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        double cx = x + WIDTH / 2;
        double baseY = y + bob * (state == State.IDLE || state == State.WALK ? 1 : 0.2);

        if (invincible > 0 && invincible % 4 < 2 && state != State.KO) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
        }

        // shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new Ellipse2D.Double(cx - 28, GROUND_Y + 8, 56, 14));

        int dir = facingRight ? 1 : -1;
        Color body = blend(primary, Color.WHITE, hurtFlash * 0.7);
        Color trim = blend(accent, Color.WHITE, hurtFlash * 0.5);

        // legs
        double legSwing = 0;
        if (state == State.WALK) {
            legSwing = Math.sin(anim * 2.2) * 10;
        } else if (state == State.KICK) {
            legSwing = dir * kickReach * 0.35;
        } else if (state == State.JUMP) {
            legSwing = 6;
        }

        g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(body);
        // back leg
        g2.drawLine((int) cx, (int) (baseY - 35), (int) (cx - dir * 8 - legSwing * 0.4), (int) (baseY + 2));
        // front leg / kick
        if (state == State.KICK) {
            g2.setColor(trim);
            g2.drawLine((int) cx, (int) (baseY - 35), (int) (cx + dir * (18 + kickReach * 0.7)), (int) (baseY - 20));
            g2.setColor(body);
        } else {
            g2.drawLine((int) cx, (int) (baseY - 35), (int) (cx + dir * 8 + legSwing * 0.5), (int) (baseY + 2));
        }

        // torso
        g2.setColor(body);
        g2.fill(new RoundRectangle2D.Double(cx - 16, baseY - 88, 32, 55, 14, 14));
        g2.setColor(trim);
        g2.fill(new RoundRectangle2D.Double(cx - 12, baseY - 78, 24, 12, 8, 8));

        // arms
        g2.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(body);
        double armY = baseY - 72;
        if (state == State.BLOCK) {
            g2.drawLine((int) (cx - 10), (int) armY, (int) (cx - 6), (int) (armY - 22));
            g2.drawLine((int) (cx + 10), (int) armY, (int) (cx + 6), (int) (armY - 22));
            g2.setColor(trim);
            g2.fill(new RoundRectangle2D.Double(cx - 18, armY - 34, 36, 16, 10, 10));
        } else if (state == State.PUNCH) {
            g2.drawLine((int) (cx - dir * 12), (int) armY, (int) (cx - dir * 20), (int) (armY + 18));
            g2.setColor(trim);
            g2.setStroke(new BasicStroke(11, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) cx, (int) armY, (int) (cx + dir * (22 + punchReach)), (int) (armY + 4));
            // fist
            g2.fill(new Ellipse2D.Double(cx + dir * (18 + punchReach) - 8, armY - 4, 16, 16));
        } else {
            double armBob = Math.sin(anim + 1) * 4;
            g2.drawLine((int) (cx - 14), (int) armY, (int) (cx - 22), (int) (armY + 22 + armBob));
            g2.drawLine((int) (cx + 14), (int) armY, (int) (cx + 22), (int) (armY + 22 - armBob));
        }

        // head
        g2.setColor(new Color(255, 214, 170));
        g2.fill(new Ellipse2D.Double(cx - 16, baseY - 118, 32, 32));
        g2.setColor(trim);
        g2.fill(new RoundRectangle2D.Double(cx - 18, baseY - 118, 36, 10, 6, 6)); // hair/band

        // eyes
        g2.setColor(Color.BLACK);
        double eyeX = cx + dir * 4;
        if (state == State.KO) {
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int) (eyeX - 8), (int) (baseY - 104), (int) (eyeX - 2), (int) (baseY - 98));
            g2.drawLine((int) (eyeX - 2), (int) (baseY - 104), (int) (eyeX - 8), (int) (baseY - 98));
            g2.drawLine((int) (eyeX + 2), (int) (baseY - 104), (int) (eyeX + 8), (int) (baseY - 98));
            g2.drawLine((int) (eyeX + 8), (int) (baseY - 104), (int) (eyeX + 2), (int) (baseY - 98));
        } else if (state == State.HURT) {
            g2.fill(new Ellipse2D.Double(eyeX - 7, baseY - 103, 5, 3));
            g2.fill(new Ellipse2D.Double(eyeX + 2, baseY - 103, 5, 3));
        } else {
            g2.fill(new Ellipse2D.Double(eyeX - 6, baseY - 104, 4, 5));
            g2.fill(new Ellipse2D.Double(eyeX + 3, baseY - 104, 4, 5));
        }

        // KO tilt
        if (state == State.KO && stateTimer > 20) {
            // already drawn flat-ish via gravity pose; extra stars
            g2.setColor(accent);
            for (int i = 0; i < 3; i++) {
                double a = anim + i * 2.1;
                double sx = cx + Math.cos(a) * 28;
                double sy = baseY - 130 + Math.sin(a * 1.3) * 10;
                drawStar(g2, sx, sy, 5);
            }
        }

        g2.dispose();
    }

    private static void drawStar(Graphics2D g, double x, double y, double r) {
        int[] xs = new int[10];
        int[] ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            double rad = (i % 2 == 0) ? r : r * 0.45;
            xs[i] = (int) (x + Math.cos(ang) * rad);
            ys[i] = (int) (y + Math.sin(ang) * rad);
        }
        g.fillPolygon(xs, ys, 10);
    }

    private static double easeOut(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t);
    }

    private static Color blend(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
