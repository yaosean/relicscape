package relicscape;

/**
 * Represents the player in the world.
 */
public class Player {
    private int x;
    private int y;
    private int hp;
    boolean pendingQuit = false; // used for ESC double-tap quit

    public Player(int x, int y, int startingHp) {
        this.x = x;
        this.y = y;
        this.hp = startingHp;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveBy(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public int getHp() { return hp; }

    public void decreaseHp(int amount) {
        this.hp = Math.max(0, this.hp - amount);
    }

    public void setHp(int hp) {
        this.hp = hp;
    }
}
