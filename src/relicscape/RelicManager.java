package relicscape;

//Relic collection & tracker
public class RelicManager {
    private int relicsToCollect;
    private int relicsCollected;
    private int shrineX;
    private int shrineY;

    public RelicManager(int relicsToCollect) {
        this.relicsToCollect = relicsToCollect;
        this.relicsCollected = 0;
    }

    public int getRelicsToCollect() { return relicsToCollect; }

    public int getRelicsCollected() { return relicsCollected; }

    public void collectRelic() { relicsCollected++; }

    public boolean hasAllRelics() { return relicsCollected >= relicsToCollect; }

    public void setShrinePosition(int x, int y) {
        this.shrineX = x;
        this.shrineY = y;
    }

    public int getShrineX() { return shrineX; }
    public int getShrineY() { return shrineY; }
}
