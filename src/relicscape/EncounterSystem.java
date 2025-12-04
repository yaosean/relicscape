package relicscape;

import java.util.Random;

//Handles random encounters and HP loss.
 
public class EncounterSystem {
    private final Random rand;

    public EncounterSystem(Random rand) {
        this.rand = rand;
    }

    /**
     * Possibly trigger an encounter based on the tile.
     * Returns a message if something happened, otherwise null.
     */
    public String maybeEncounter(TileType tile, Player player) {
        int chance;
        switch (tile) {
            case RUIN_FLOOR:
            case RUBBLE:
            case RUIN_WALL:
                chance = 20;
                break;
            case DUNE:
            case SAND:
            case CACTUS:
                chance = 12;
                break;
            default:
                chance = 6;
        }

         if (rand.nextInt(100) < chance) {
            player.decreaseHp(1);
            if (player.getHp() <= 0) {
                return "A shadow from the ruins drains your last strength...";
            } else {
                return "Something brushes past you. HP: " + player.getHp();
            }
        }
        return null;
    }
}
