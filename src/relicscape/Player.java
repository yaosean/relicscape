package relicscape;

public class Player {
    private float x;
    private float y;
    private float targetX;
    private float targetY;
    private boolean moving = false;
    private int hp;
    private int maxHp;
    boolean pendingQuit = false;
    private boolean invulnerable = false;

    public Player(float x, float y, int startingHp) {
        this.x = x;
        this.y = y;
        this.hp = startingHp;
        this.maxHp = startingHp;
    }

    public void setInvulnerable(boolean inv) { this.invulnerable = inv; }

    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public float getFloatX() { return x; }
    public float getFloatY() { return y; }

    public void dropAt(float x, float y) {
        this.x = x;
        this.y = y;
        targetX = x;
        targetY = y;
        moving = false;
    }

    public void moveBy(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        targetX += dx;
        targetY += dy;
    }

    public void setTarget(float tx, float ty) {
        targetX = tx;
        targetY = ty;
        moving = true;
    }

    public boolean isMoving() {
        return moving;
    }

    public void update(float speed) {
        if (moving) {
            float dx = targetX - x;
            float dy = targetY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < speed) {
                x = targetX;
                y = targetY;
                moving = false;
            } else {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
        }
    }

    public int getHearts() { return hp; }
    public int getMaxHearts() { return maxHp; }

    public void nickHearts(int amount) {
        if (invulnerable) return;
        this.hp = Math.max(0, this.hp - amount);
    }

    public void setHearts(int hp) {
        this.hp = Math.min(maxHp, hp);
    }
}
