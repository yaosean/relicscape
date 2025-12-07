package relicscape;

public class Player {
    private float positionX;
    private float positionY;
    private float targetPositionX;
    private float targetPositionY;
    private boolean isMoving = false;
    private int currentHearts;
    private int maxHearts;
    boolean pendingQuit = false;
    private boolean invulnerable = false;

    public Player(float startX, float startY, int startingHearts) {
        this.positionX = startX;
        this.positionY = startY;
        this.currentHearts = startingHearts;
        this.maxHearts = startingHearts;
    }

    public void setInvulnerable(boolean invulnerableFlag) { this.invulnerable = invulnerableFlag; }

    public int getTileX() { return (int) positionX; }
    public int getTileY() { return (int) positionY; }
    public float getExactX() { return positionX; }
    public float getExactY() { return positionY; }

    public void setPosition(float x, float y) {
        this.positionX = x;
        this.positionY = y;
        targetPositionX = x;
        targetPositionY = y;
        isMoving = false;
    }

    public void moveByDelta(float deltaX, float deltaY) {
        this.positionX += deltaX;
        this.positionY += deltaY;
        targetPositionX += deltaX;
        targetPositionY += deltaY;
    }

    public void setTargetPosition(float targetX, float targetY) {
        targetPositionX = targetX;
        targetPositionY = targetY;
        isMoving = true;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void update(float speed) {
        if (isMoving) {
            float deltaX = targetPositionX - positionX;
            float deltaY = targetPositionY - positionY;
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (distance < speed) {
                positionX = targetPositionX;
                positionY = targetPositionY;
                isMoving = false;
            } else {
                positionX += (deltaX / distance) * speed;
                positionY += (deltaY / distance) * speed;
            }
        }
    }

    public int getHearts() { return currentHearts; }
    public int getMaxHearts() { return maxHearts; }

    public void takeDamage(int amount) {
        if (invulnerable) return;
        this.currentHearts = Math.max(0, this.currentHearts - amount);
    }

    public void setHearts(int hearts) {
        this.currentHearts = Math.min(maxHearts, hearts);
    }
}
