package relicscape;

public class RelicManager {
    private int goalPieces;
    private int heldPieces;
    private int shrineX;
    private int shrineY;

    public RelicManager(int goalPieces) {
        this.goalPieces = goalPieces;
        this.heldPieces = 0;
    }

    public int goalCount() { return goalPieces; }
    public void setGoalPieces(int goalPieces) { this.goalPieces = Math.max(0, goalPieces); }

    public int bagCount() { return heldPieces; }

    public void stashOne() { heldPieces++; }

    public boolean doneGathering() { return heldPieces >= goalPieces; }

    public void markShrine(int x, int y) {
        this.shrineX = x;
        this.shrineY = y;
    }

    public int shrineX() { return shrineX; }
    public int shrineY() { return shrineY; }
}
