import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
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
    public double scarfWave;
    public boolean attackHit;

    public static final double WIDTH = 54;
    public static final double HEIGHT = 110;
    public static final double GROUND_Y = 420;

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
        anim += 0.22;
        scarfWave = Math.sin(anim * 1.4) * 6;
        bob = Math.sin(anim) * 2.2;

        if (invincible > 0) {
            invincible--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (hurtFlash > 0) {
            hurtFlash *= 0.82;
            if (hurtFlash < 0.05) {
                hurtFlash = 0;
            }
        }

        if (state == State.KO) {
            stateTimer++;
            vy += 0.95;
            y += vy;
            if (y > GROUND_Y) {
                y = GROUND_Y;
                vy = 0;
            }
            punchReach *= 0.75;
            kickReach *= 0.75;
            return;
        }

        if (state == State.HURT) {
            stateTimer--;
            x += vx;
            vx *= 0.88;
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
                punchReach = easeOut(1.0 - stateTimer / 14.0) * 58;
                kickReach *= 0.65;
                // blade lunge
                x += (facingRight ? 1.1 : -1.1);
            } else {
                kickReach = easeOut(1.0 - stateTimer / 18.0) * 70;
                punchReach *= 0.65;
            }
            applyGravity();
            clampBounds();
            if (stateTimer <= 0) {
                state = onGround ? State.IDLE : State.JUMP;
                punchReach = 0;
                kickReach = 0;
                attackHit = false;
                attackCooldown = 7;
            }
            return;
        }

        blocking = block && onGround && attackCooldown == 0;
        if (blocking) {
            state = State.BLOCK;
            vx *= 0.55;
            punchReach = 0;
            kickReach = 0;
            applyGravity();
            clampBounds();
            return;
        }

        if (onGround && punch && attackCooldown == 0) {
            state = State.PUNCH;
            stateTimer = 14;
            attackHit = false;
            vx = facingRight ? 3.5 : -3.5;
            return;
        }
        if (onGround && kick && attackCooldown == 0) {
            state = State.KICK;
            stateTimer = 18;
            attackHit = false;
            vx = facingRight ? 2.2 : -2.2;
            return;
        }

        double speed = 5.1;
        if (left) {
            vx = -speed;
            facingRight = false;
            state = onGround ? State.WALK : State.JUMP;
        } else if (right) {
            vx = speed;
            facingRight = true;
            state = onGround ? State.WALK : State.JUMP;
        } else {
            vx *= 0.78;
            if (Math.abs(vx) < 0.2) {
                vx = 0;
            }
            if (onGround) {
                state = State.IDLE;
            }
        }

        if (jump && onGround) {
            vy = -15.2;
            onGround = false;
            state = State.JUMP;
        }

        x += vx;
        applyGravity();
        clampBounds();
        punchReach *= 0.65;
        kickReach *= 0.65;
    }

    private void applyGravity() {
        vy += 0.9;
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
            vx = knockDir * 2.8;
            hurtFlash = 0.45;
            return true;
        }
        hp = Math.max(0, hp - damage);
        hurtFlash = 1;
        invincible = 11;
        if (hp <= 0) {
            state = State.KO;
            stateTimer = 0;
            vx = knockDir * 7;
            vy = -9;
            onGround = false;
        } else {
            state = State.HURT;
            stateTimer = isKick ? 16 : 12;
            vx = knockDir * (isKick ? 8 : 5.5);
            vy = isKick ? -5 : -2.5;
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
        return facingRight ? attackOriginX() + punchReach : attackOriginX() - punchReach;
    }

    public double kickHitX() {
        return facingRight ? attackOriginX() + kickReach : attackOriginX() - kickReach;
    }

    public boolean punchActive() {
        return state == State.PUNCH && stateTimer <= 11 && stateTimer >= 5 && punchReach > 22;
    }

    public boolean kickActive() {
        return state == State.KICK && stateTimer <= 13 && stateTimer >= 5 && kickReach > 30;
    }

    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        double cx = x + WIDTH / 2;
        double baseY = y + bob * (state == State.IDLE || state == State.WALK ? 1 : 0.15);
        int dir = facingRight ? 1 : -1;

        if (invincible > 0 && invincible % 4 < 2 && state != State.KO) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
        }

        // soft ground shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fill(new Ellipse2D.Double(cx - 26, GROUND_Y + 6, 52, 12));

        Color cloth = blend(primary, Color.WHITE, hurtFlash * 0.55);
        Color trim = blend(accent, Color.WHITE, hurtFlash * 0.4);
        Color mask = blend(new Color(28, 24, 32), Color.WHITE, hurtFlash * 0.5);

        double legSwing = 0;
        if (state == State.WALK) {
            legSwing = Math.sin(anim * 2.6) * 12;
        } else if (state == State.KICK) {
            legSwing = dir * kickReach * 0.4;
        } else if (state == State.JUMP) {
            legSwing = 8;
        }

        // flowing scarf / sash behind
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(trim.getRed(), trim.getGreen(), trim.getBlue(), 180));
        double scarfX = cx - dir * (18 + Math.abs(vx) * 0.8);
        g2.drawLine((int) (cx - dir * 6), (int) (baseY - 78),
                (int) (scarfX - dir * 10), (int) (baseY - 55 + scarfWave));
        g2.drawLine((int) (cx - dir * 4), (int) (baseY - 74),
                (int) (scarfX - dir * 18), (int) (baseY - 40 + scarfWave * 0.6));

        // legs — wrapped shin style
        g2.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(cloth);
        g2.drawLine((int) cx, (int) (baseY - 34), (int) (cx - dir * 9 - legSwing * 0.4), (int) (baseY + 2));
        if (state == State.KICK) {
            g2.setColor(trim);
            g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) cx, (int) (baseY - 34),
                    (int) (cx + dir * (20 + kickReach * 0.72)), (int) (baseY - 28));
            // kick arc trail
            drawSlashArc(g2, cx + dir * (10 + kickReach * 0.35), baseY - 40, dir, kickReach * 0.55, trim, false);
        } else {
            g2.drawLine((int) cx, (int) (baseY - 34), (int) (cx + dir * 9 + legSwing * 0.5), (int) (baseY + 2));
        }

        // torso wrap
        g2.setColor(cloth);
        g2.fill(new RoundRectangle2D.Double(cx - 15, baseY - 88, 30, 54, 10, 10));
        // obi belt
        g2.setColor(trim);
        g2.fill(new RoundRectangle2D.Double(cx - 16, baseY - 48, 32, 8, 4, 4));
        // chest straps
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(trim.getRed(), trim.getGreen(), trim.getBlue(), 160));
        g2.drawLine((int) (cx - 10), (int) (baseY - 82), (int) (cx + 10), (int) (baseY - 58));
        g2.drawLine((int) (cx + 10), (int) (baseY - 82), (int) (cx - 10), (int) (baseY - 58));

        // arms / blade slash
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(cloth);
        double armY = baseY - 72;
        if (state == State.BLOCK) {
            // crossed forearm guard + short blade
            g2.drawLine((int) (cx - 8), (int) armY, (int) (cx + dir * 4), (int) (armY - 20));
            g2.drawLine((int) (cx + 8), (int) armY, (int) (cx - dir * 2), (int) (armY - 18));
            drawBlade(g2, cx + dir * 6, armY - 28, dir, 0.55, trim);
        } else if (state == State.PUNCH) {
            g2.drawLine((int) (cx - dir * 10), (int) armY, (int) (cx - dir * 16), (int) (armY + 14));
            double tipX = cx + dir * (16 + punchReach);
            g2.setColor(trim);
            g2.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) cx, (int) armY, (int) tipX, (int) (armY + 2));
            drawBlade(g2, tipX - dir * 8, armY, dir, 1.0, Color.WHITE);
            drawSlashArc(g2, cx + dir * (8 + punchReach * 0.4), armY - 6, dir, punchReach * 0.7, accent, true);
        } else {
            double armBob = Math.sin(anim + 1) * 3;
            g2.drawLine((int) (cx - 12), (int) armY, (int) (cx - 18), (int) (armY + 20 + armBob));
            g2.drawLine((int) (cx + 12), (int) armY, (int) (cx + 18), (int) (armY + 20 - armBob));
            // sheathed blade on back
            drawBlade(g2, cx - dir * 14, baseY - 70, -dir, 0.7, trim);
        }

        // head + hood + mask
        g2.setColor(mask);
        g2.fill(new Ellipse2D.Double(cx - 15, baseY - 116, 30, 30));
        // hood
        Path2D hood = new Path2D.Double();
        hood.moveTo(cx - 17, baseY - 102);
        hood.curveTo(cx - 20, baseY - 128, cx + 20, baseY - 128, cx + 17, baseY - 102);
        hood.lineTo(cx + 12, baseY - 96);
        hood.curveTo(cx, baseY - 108, cx, baseY - 108, cx - 12, baseY - 96);
        hood.closePath();
        g2.setColor(cloth);
        g2.fill(hood);
        g2.setColor(trim);
        g2.fill(new RoundRectangle2D.Double(cx - 16, baseY - 108, 32, 5, 3, 3));

        // eyes through mask
        g2.setColor(state == State.HURT ? new Color(255, 80, 80) : accent);
        if (state == State.KO) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(200, 200, 200));
            double eyeX = cx + dir * 3;
            g2.drawLine((int) (eyeX - 7), (int) (baseY - 102), (int) (eyeX - 2), (int) (baseY - 97));
            g2.drawLine((int) (eyeX - 2), (int) (baseY - 102), (int) (eyeX - 7), (int) (baseY - 97));
            g2.drawLine((int) (eyeX + 2), (int) (baseY - 102), (int) (eyeX + 7), (int) (baseY - 97));
            g2.drawLine((int) (eyeX + 7), (int) (baseY - 102), (int) (eyeX + 2), (int) (baseY - 97));
        } else {
            g2.fill(new RoundRectangle2D.Double(cx + dir * 2 - 8, baseY - 103, 7, 3.5, 2, 2));
            g2.fill(new RoundRectangle2D.Double(cx + dir * 2 + 2, baseY - 103, 7, 3.5, 2, 2));
            // eye gleam
            g2.setColor(new Color(255, 255, 255, 160));
            g2.fill(new Ellipse2D.Double(cx + dir * 2 - 6, baseY - 102.5, 2, 2));
            g2.fill(new Ellipse2D.Double(cx + dir * 2 + 4, baseY - 102.5, 2, 2));
        }

        // lower face wrap
        g2.setColor(new Color(mask.getRed(), mask.getGreen(), mask.getBlue(), 230));
        g2.fill(new RoundRectangle2D.Double(cx - 12, baseY - 96, 24, 10, 6, 6));

        if (state == State.KO && stateTimer > 18) {
            g2.setColor(accent);
            for (int i = 0; i < 3; i++) {
                double a = anim + i * 2.1;
                drawShuriken(g2, cx + Math.cos(a) * 26, baseY - 128 + Math.sin(a * 1.3) * 8, 5);
            }
        }

        g2.dispose();
    }

    private static void drawBlade(Graphics2D g, double x, double y, int dir, double scale, Color color) {
        Path2D blade = new Path2D.Double();
        double len = 28 * scale;
        blade.moveTo(x, y);
        blade.lineTo(x + dir * len, y - 3 * scale);
        blade.lineTo(x + dir * (len + 4 * scale), y);
        blade.lineTo(x + dir * len, y + 3 * scale);
        blade.closePath();
        g.setColor(new Color(220, 230, 240, 230));
        g.fill(blade);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(blade);
        // guard
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x - 3, y - 5 * scale, 6, 10 * scale, 2, 2));
    }

    private static void drawSlashArc(Graphics2D g, double x, double y, int dir, double reach, Color color, boolean bright) {
        if (reach < 8) {
            return;
        }
        g.setStroke(new BasicStroke(bright ? 3.2f : 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), bright ? 200 : 140));
        Path2D arc = new Path2D.Double();
        arc.moveTo(x - dir * reach * 0.2, y - reach * 0.35);
        arc.curveTo(x + dir * reach * 0.15, y - reach * 0.55,
                x + dir * reach * 0.55, y - reach * 0.1,
                x + dir * reach * 0.75, y + reach * 0.25);
        g.draw(arc);
        if (bright) {
            g.setColor(new Color(255, 255, 255, 160));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(arc);
        }
    }

    private static void drawShuriken(Graphics2D g, double x, double y, double r) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            double ox = Math.cos(a) * r;
            double oy = Math.sin(a) * r;
            if (i == 0) {
                star.moveTo(x + ox, y + oy);
            } else {
                star.lineTo(x + ox, y + oy);
            }
            star.lineTo(x + Math.cos(a + Math.PI / 4) * r * 0.28, y + Math.sin(a + Math.PI / 4) * r * 0.28);
        }
        star.closePath();
        g.fill(star);
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
