package relicscape;

public class RelicManager {
    private int goalBits;
    private int pocketBits;
    private int shrineSpotX;
    private int shrineSpotY;

    public RelicManager(int goalPieces) {
        this.goalBits = goalPieces;
        this.pocketBits = 0;
    }

    public int goalCount() { return goalBits; }
    public void setGoalPieces(int goalPieces) { this.goalBits = Math.max(0, goalPieces); }

    public int bagCount() { return pocketBits; }

    public void stashOne() { pocketBits++; }

    public boolean doneGathering() { return pocketBits >= goalBits; }

    public void markShrine(int x, int y) {
        this.shrineSpotX = x;
        this.shrineSpotY = y;
    }

    public int shrineX() { return shrineSpotX; }
    public int shrineY() { return shrineSpotY; }
}
