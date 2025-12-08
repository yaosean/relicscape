package relicscape;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private final int tileSizeHint = 96;
    private final int hudBarHeight = 80;
    private final int tickMs = 33;
    private final double clearRing = 1.0;
    private final double revealRing = 4.5;
    private final double fogEdge = 11.5;
    private final int topPad = 30;
    private long corruptionSpanMs = 5 * 60 * 1000L;
    private final long corruptionSpanFastMs = 3 * 60 * 1000L;

    private final World world;
    private TMXMapLoader mapLoader;
    private final Player player;
    private final RelicManager relicBag;
    private final Random rand = new Random(System.currentTimeMillis());
    private long lastMoveMs=0L;
    private static final long MOVE_GAP_MS = 200;
    private static boolean bootIntoEndless = false;
    private boolean endlessMode = false;
    private boolean postWinChoice = false;
    private boolean monstersHeal = false;
    private boolean noCorruption = false;
    private boolean noFog = false;
    private int relicScatterMultiplier = 1;
        private static final String[] RELIC_RESOURCE_NAMES = {
            "book1.png",
            "book2.png",
            "book3.png",
            "book4.png"
        };
        private Clip corruptionLoopClip;
        private Clip deathClip;
        private Clip portalClip;
        private Clip relicClip;
        private Clip footstepClip;
        private Clip preRelicBgm;
        private Clip relic1Bgm;
        private Clip relic2Bgm;
        private Clip relic3Bgm;
        private Clip relic4Bgm;
        private Clip victoryClip;
        private Clip hurtClip;
        private long lastFootstepPlayMs = 0L;
        private final long footstepCooldownMs = 260L;

    private String lastMessage = "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver=false;
    private boolean gameWon = false;
    private boolean escapedWin = false;
    private long escapedWinStartMs = 0L;
    private int endingMinX = -1;
    private int endingMinY = -1;
    private int endingMaxX = -1;
    private int endingMaxY = -1;
    private boolean facingRight = true;
    private boolean moving=false;
    private boolean onStartScreen=true;
    private boolean startFading=false;
    private boolean fadeDone=false;
    private boolean waitingForContinue=false;
    private long fadeStartMs=0L;
    private final long fadeDurationMs=2300L;

    private long corruptionStartMs = -1L;
    private boolean corruptionTintActive = false;
    private boolean corruptionPhaseTwo = false;
    private long corruptionExposureStartMs = -1L;
    private double corruptionDamageRemainder = 0.0;
    private final double corruptionEntryThreshold = 0.04;
    private final double corruptionExitThreshold = 0.02;
    private boolean inCorruptionZone = false;

    private boolean[][] discovered;
    private BufferedImage[] soldierWalkFrames;
    private BufferedImage[] soldierIdleFrames;
    private BufferedImage[] soldierHurtFrames;
    private BufferedImage[] soldierDeathFrames;
    private final StartScreenRenderer startScreen;
    private final List<RelicDrop> looseShinies = new ArrayList<>();
    private final Set<Long> unlockedRelicKeys = new HashSet<>();
    private boolean mathActive = false;
    private String mathQuestion = "";
    private String mathAnswer = "";
    private StringBuilder mathInput = new StringBuilder();
    private int mathAttempts = 0;
    private final int mathMaxAttempts = 3;
    private long pendingRelicKey = -1L;
    private int pendingRelicX = -1;
    private int pendingRelicY = -1;
    private boolean pendingLooseRelic = false;
    private enum MonsterType { EYE, JELLY, GOLEM, NECRO }
    private final List<Monster> monsters = new ArrayList<>();
    private BufferedImage[] monsterEyeFrames;
    private BufferedImage[] monsterJellyFrames;
    private BufferedImage[] golemWalkFrames;
    private BufferedImage[] golemIdleFrames;
    private BufferedImage[] golemAttackFrames;
    private BufferedImage[] necroIdleFrames;
    private BufferedImage[] necroWalkFrames;
    private BufferedImage[] necroAttackFrames;
    private BufferedImage[] necroAttackFxFrames;
    private BufferedImage[] necroSpawnFrames;

    private boolean firstRelicCutsceneStarted = false;
    private boolean firstRelicCutsceneActive = false;
    private boolean firstRelicCutsceneDone = false;
    private long firstRelicCutsceneStartMs = 0L;
    private final long firstRelicCutsceneDurationMs = 3200L;
    private boolean firstRelicCutsceneAwaitingContinue = false;
    private boolean secondRelicCutsceneActive = false;
    private boolean secondRelicCutsceneDone = false;
    private boolean secondRelicCutsceneAwaitingContinue = false;
    private long secondRelicCutsceneStartMs = 0L;
    private final long secondRelicCutsceneDurationMs = 4200L;
        private boolean thirdRelicCutsceneActive = false;
        private boolean thirdRelicCutsceneDone = false;
        private boolean thirdRelicCutsceneAwaitingContinue = false;
        private long thirdRelicCutsceneStartMs = 0L;
        private final long thirdRelicCutsceneDurationMs = 4800L;
    private boolean necroCutsceneActive = false;
    private boolean necroCutsceneDone = false;
    private boolean necroCutsceneAwaitingContinue = false;
    private long necroCutsceneStartMs = 0L;
    private final long necroCutsceneDurationMs = 5200L;
    private long nextMonsterSpawnMs = Long.MAX_VALUE;
    private final int maxMonsters = 12;
    private final int maxPerType = 2;
    private long lastDamageMs = 0L;
    private final long damageCooldownMs = 900L;
    private long hurtAnimStartMs = -1L;
    private final long hurtAnimDurationMs = 400L;
    private long deathAnimStartMs = -1L;

    private final Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 900));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        TMXMapLoader loader = new TMXMapLoader();
        world = loader.load("images/dreams.tmx");
        this.mapLoader=loader;
        computeEndingBounds();

        relicBag = new RelicManager(0);

        int spawnX = Math.max(1, (int)Math.round(world.getWidth()*0.45));
        int spawnY = Math.max(1, world.getHeight()-3);
        boolean found=false;
        
        for(int r=0;r<Math.max(world.getWidth(),world.getHeight());r++){
            for(int dy=-r;dy<=r;dy++){
                for(int dx=-r; dx<=r; dx++){
                    int cx = spawnX+dx;
                    int cy=spawnY+dy;
                    if(!world.inBounds(cx,cy)) continue;
                    if(!world.isBlocked(cx,cy)){
                        spawnX=cx;
                        spawnY=cy;
                        found=true;
                        break;
                    }
                }
                if(found) break;
            }
            if(found) break;
        }
        player = new Player(spawnX, spawnY, 100);
        wakeFog();

        if(bootIntoEndless){
            configureEndlessMode();
        }
        loadPlayerLook();
        loadMonsterSprites();
        loadSounds();
        startPreRelicBgm();
        scatterRelicPics();
        syncRelicGoal();

        startScreen = new StartScreenRenderer();

        timer = new Timer(tickMs, e->{
            updateGame();
            repaint();
        });
        timer.start();

        lastMessage = "Explore the world. Find " + relicBag.goalCount() + " relic fragments and return to the central shrine.";
        if(endlessMode){
            lastMessage = "Peaceful run: collect countless relics, monsters heal, shrine restarts or ESC exits.";
        }
    }

    private void configureEndlessMode(){
        bootIntoEndless = false;
        endlessMode = true;
        monstersHeal = true;
        noCorruption = true;
        noFog = true;
        relicScatterMultiplier = 4;

        onStartScreen = false;
        startFading = false;
        fadeDone = false;
        waitingForContinue = false;
        gameOver = false;
        gameWon = false;
        postWinChoice = false;
        escapedWin = false;

        corruptionStartMs = -1L;
        corruptionTintActive = false;
        corruptionPhaseTwo = false;
        corruptionExposureStartMs = -1L;
        corruptionDamageRemainder = 0.0;
        inCorruptionZone = false;

        firstRelicCutsceneStarted = true;
        firstRelicCutsceneActive = false;
        firstRelicCutsceneAwaitingContinue = false;
        firstRelicCutsceneDone = true;
        secondRelicCutsceneActive = false;
        secondRelicCutsceneAwaitingContinue = false;
        secondRelicCutsceneDone = true;
        thirdRelicCutsceneActive = false;
        thirdRelicCutsceneAwaitingContinue = false;
        thirdRelicCutsceneDone = true;
        necroCutsceneActive = false;
        necroCutsceneAwaitingContinue = false;
        necroCutsceneDone = true;
        nextMonsterSpawnMs = System.currentTimeMillis() + 300L;

        wakeFog();
        lastMessage = "Peaceful relic hunt: monsters heal you, corruption is gone. Shrine restarts the loop.";
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(thirdRelicCutsceneActive && thirdRelicCutsceneAwaitingContinue){
            completeThirdRelicCutscene();
            return;
        }
        if(necroCutsceneActive && necroCutsceneAwaitingContinue){
            completeNecroCutscene();
            return;
        }
        if(secondRelicCutsceneActive && secondRelicCutsceneAwaitingContinue){
            completeSecondRelicCutscene();
            return;
        }
        if(firstRelicCutsceneActive && firstRelicCutsceneAwaitingContinue){
            completeFirstRelicCutscene();
            return;
        }
        if(onStartScreen){
            if(waitingForContinue && fadeDone && !startFading){
                startFromFade();
            } else if(!startFading){
                beginStartFade();
            }
            return;
        }

        if(postWinChoice){
            handleWinChoiceKey(e);
            repaint();
            return;
        }

        if(mathActive){
            handleMathKey(e);
            repaint();
            return;
        }

        if(gameOver||gameWon){
            return;
        }

        switch(e.getKeyCode()){
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                nudgePlayer(0, -1);
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                nudgePlayer(0, 1);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                nudgePlayer(-1, 0);
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                nudgePlayer(1, 0);
                break;
            case KeyEvent.VK_H:
                lastMessage="WASD / arrows to move. Find 3 relics (✶) and return to the shrine (⌘).";
                break;
            case KeyEvent.VK_ESCAPE:
                lastMessage="Press ESC again to quit.";
                if(player.pendingQuit){
                    System.exit(0);
                }
                player.pendingQuit=true;
                return;
            default:
                return;
        }

        player.pendingQuit = false;
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        switch(e.getKeyCode()){
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                moving=false;
                break;
            default:
                break;
        }
    }

    private void handleWinChoiceKey(KeyEvent e){
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
            System.exit(0);
        } else if(e.getKeyCode() == KeyEvent.VK_ENTER){
            restartIntoEndless();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(thirdRelicCutsceneActive && thirdRelicCutsceneAwaitingContinue){
            completeThirdRelicCutscene();
            return;
        }
        if(necroCutsceneActive && necroCutsceneAwaitingContinue){
            completeNecroCutscene();
            return;
        }
        if(secondRelicCutsceneActive && secondRelicCutsceneAwaitingContinue){
            completeSecondRelicCutscene();
            return;
        }
        if(firstRelicCutsceneActive && firstRelicCutsceneAwaitingContinue){
            completeFirstRelicCutscene();
            return;
        }
        if(onStartScreen){
            if(waitingForContinue && fadeDone && !startFading){
                startFromFade();
            } else if(!startFading){
                beginStartFade();
            }
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

    private void nudgePlayer(int dx, int dy) {
        if(onStartScreen){
            return;
        }
        if(firstRelicCutsceneActive || secondRelicCutsceneActive || thirdRelicCutsceneActive){
            return;
        }
        if(mathActive){
            return;
        }
        if(gameOver || postWinChoice || gameWon){
            return;
        }
        long now = System.currentTimeMillis();
        if(now-lastMoveMs<MOVE_GAP_MS){
            return;
        }
        int newX=player.getTileX()+dx;
        int newY=player.getTileY()+dy;

        if(!world.inBounds(newX,newY)){
            lastMessage="You feel the edge of the world.";
            return;
        }

        if(world.isBlocked(newX,newY)){
            lastMessage="You can't move through that.";
            return;
        }

        player.setPosition(newX, newY);
        lastMoveMs=now;
        moving=true;
        playFootstep();

        if(dx!=0){
            facingRight = dx > 0;
        }

        peelFog(newX,newY);
        TileType tile = world.getTile(newX,newY);
        feelTile(tile);
        maybeTriggerEscape();
        pickupLooseRelic();
        checkIfDone();
    }

    private void feelTile(TileType tile) {
        if(tile==TileType.RELIC){
            long key = encodePoint(player.getTileX(), player.getTileY());
            if(unlockedRelicKeys.contains(key)){
                lastMessage = "The relic's seal is already broken.";
            } else if(mathActive){
                lastMessage = "Finish the puzzle before touching another relic.";
            } else {
                startRelicMinigame(key, player.getTileX(), player.getTileY());
            }
        } else if(tile==TileType.SHRINE){
            if(endlessMode){
                enterPostWinChoice("The shrine hums—press ENTER to restart this calm run or ESC to leave.");
            } else if(relicBag.doneGathering()){
                enterPostWinChoice("Light erupts from the shrine. Press ENTER to begin a peaceful relic hunt or ESC to leave.");
            } else {
                lastMessage="The shrine hums softly. It needs more relics.";
            }
        } else if(tile==TileType.DUNE){
            lastMessage="Your feet sink in the shifting sand.";
        } else if(tile==TileType.RUBBLE){
            lastMessage="Broken stones crunch underfoot.";
        } else {
            if(!noCorruption && tileIsCorrupted(player.getTileX(), player.getTileY())){
                lastMessage="The corruption crackles—get out now!";
            } else {
                lastMessage="You move onward.";
            }
        }
    }

    private void checkIfDone() {
        if(gameOver||gameWon){
            return;
        }

        if(player.getHearts()<=0){
            gameOver=true;
            lastMessage = "You collapse in the dust.";
            deathAnimStartMs = System.currentTimeMillis();
            playOnce(deathClip);
            stopClip(corruptionLoopClip);
            lowerBackgroundMusicVolumeForDeath();
            return;
        }

        if(relicBag.doneGathering()){
            // Let the shrine resolve message handle the win when standing on it.
            lastMessage="You feel the relics hum. The shrine must be close.";
        }
    }

    private void enterPostWinChoice(String message){
        gameWon = true;
        postWinChoice = true;
        lastMessage = message;
        stopAllClips();
        playOnce(victoryClip);
    }

    private long encodePoint(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private void startRelicMinigame(long key, int x, int y){
        pendingRelicKey = key;
        pendingRelicX = x;
        pendingRelicY = y;
        stopClip(footstepClip);
        if(mathActive){
            return;
        }
        lastMessage = "Solve this math problem:";
        startMathQuiz();
    }

    private void startMathQuiz(){
        mathActive = true;
        mathInput = new StringBuilder();
        mathAttempts = 0;
        generateMathProblem();
    }

    private void generateMathProblem(){
        int a = rand.nextInt(16) + 5;
        int b = rand.nextInt(16) + 5;
        int c = rand.nextInt(5) + 2;
        int d = rand.nextInt(10) + 3;

        int part1 = a + b;
        int part2 = part1 * c;
        int part3 = Math.max(part2 - d, 1);

        List<Integer> divisors = new ArrayList<>();
        for(int i=1; i<=Math.sqrt(part3); i++){
            if(part3 % i == 0){
                divisors.add(i);
                int pair = part3 / i;
                if(pair != i) divisors.add(pair);
            }
        }
        if(divisors.isEmpty()) divisors.add(1);
        int e = divisors.get(rand.nextInt(divisors.size()));

        int answer = part3 / e;
        mathQuestion = "((" + a + " + " + b + ") * " + c + " - " + d + ") / " + e + " = ?";
        mathAnswer = String.valueOf(answer);
    }

    private void handleMathKey(KeyEvent e){
        int code = e.getKeyCode();
        if(code == KeyEvent.VK_ESCAPE){
            mathActive = false;
            pendingRelicKey = -1L;
            pendingRelicX = pendingRelicY = -1;
            pendingLooseRelic = false;
            lastMessage = "You step away from the relic's seal.";
            return;
        }
        if(code == KeyEvent.VK_BACK_SPACE){
            if(mathInput.length() > 0) mathInput.deleteCharAt(mathInput.length() - 1);
            return;
        }
        if(code == KeyEvent.VK_ENTER){
            if(mathInput.length() == 0) return;
            mathAttempts++;
            String guess = normalizeMathString(mathInput.toString());
            String ans = normalizeMathString(mathAnswer);
            if(guess.equals(ans)){
                mathActive = false;
                lastMessage = "Seal broken. The relic is yours.";
                completeRelicUnlock();
            } else if(mathAttempts >= mathMaxAttempts){
                mathActive = false;
                pendingRelicKey = -1L;
                pendingRelicX = pendingRelicY = -1;
                pendingLooseRelic = false;
                lastMessage = "Seal holds. Answer was " + mathAnswer + ".";
            } else {
                lastMessage = "Incorrect. Attempts left: " + (mathMaxAttempts - mathAttempts);
                mathInput = new StringBuilder();
            }
            return;
        }
        char c = e.getKeyChar();
        if(isAllowedMathChar(c) && mathInput.length() < 32){
            mathInput.append(c);
        }
    }

    private void completeRelicUnlock(){
        if(pendingLooseRelic){
            for(int i=0;i<looseShinies.size();i++){
                RelicDrop d = looseShinies.get(i);
                if(d.x==pendingRelicX && d.y==pendingRelicY){
                    looseShinies.remove(i);
                    break;
                }
            }
        } else {
            unlockedRelicKeys.add(pendingRelicKey);
        }
        relicBag.stashOne();
        playOnce(relicClip);
        handleRelicMilestones();
        stopClip(footstepClip);
        world.setTile(pendingRelicX,pendingRelicY,world.baseForRow(pendingRelicY));
        removeMapTileVisual(pendingRelicX, pendingRelicY);
        lastMessage="You solved the puzzle and claimed the relic! ("+
                relicBag.bagCount()+"/"+relicBag.goalCount()+")";
        if(!endlessMode){
            if(!firstRelicCutsceneStarted){
                triggerFirstRelicCutscene();
            }
            startCorruptionIfReady();
            maybeTriggerSecondCutscene();
            maybeTriggerThirdCutscene();
            maybeTriggerNecroCutscene();
        }
        pendingRelicKey = -1L;
        pendingRelicX = -1;
        pendingRelicY = -1;
        pendingLooseRelic = false;
    }

    private void removeMapTileVisual(int x, int y){
        if(mapLoader == null) return;
        mapLoader.removeTileFromTilesetLayers(x, y);
    }

    private String normalizeMathString(String s){
        String out = s.replaceAll("\\s+", "");
        out = out.replace("*", "");
        out = out.replaceAll("([0-9]+)\\^1", "$1");
        out = out.replaceAll("x\\^1", "x");
        out = out.replaceAll("X\\^1", "x");
        out = out.replaceAll("([0-9]+)\\^0", "1");
        out = out.replaceAll("x\\^0", "1");
        out = out.replaceAll("X\\^0", "1");
        out = out.replaceAll("\\+0", "");
        return out.toLowerCase();
    }

    private boolean isAllowedMathChar(char c){
        String allowed = "0123456789xX^+-*/() ";
        return allowed.indexOf(c) >= 0;
    }

    private void maybeTriggerEscape(){
        if(endlessMode) return;
        if(gameOver || gameWon) return;
        if(mapLoader == null) return;
        if(!relicBag.doneGathering()) return;
        int px = player.getTileX();
        int py = player.getTileY();
        if(!mapLoader.hasTile("ending", px, py)) return;
        escapedWin = true;
        enterPostWinChoice("You step into the radiant rift. Press ENTER to begin a peaceful relic hunt or ESC to leave.");
    }

    private void loadPlayerLook(){
        soldierIdleFrames = loadStrip("char/Soldier with shadows/soldier-idle.png");
        soldierWalkFrames = loadStrip("char/Soldier with shadows/soldier-walk.png");
        soldierHurtFrames = loadStrip("char/Soldier with shadows/Soldier-Hurt.png");
        soldierDeathFrames = loadStrip("char/Soldier with shadows/Soldier-death.png");
    }

    private void scatterRelicPics(){
        List<BufferedImage> pics = loadRelicPics();
        if(pics.isEmpty() || mapLoader==null){
            return;
        }

        Random rand = new Random(System.currentTimeMillis());
        int[][] base = mapLoader.getLayer("tile layer 1");

        int copies = Math.max(1, relicScatterMultiplier);
        for(int copy=0; copy<copies; copy++){
            for(BufferedImage pic : pics){
                for(int tries=0; tries<4000; tries++){
                    int x = rand.nextInt(world.getWidth());
                    int y = rand.nextInt(world.getHeight());

                    if(base==null) continue; // require tile layer 1 present
                    if(y < 0 || y >= base.length || x < 0 || x >= base[0].length) continue;
                    if(base[y][x]==0) continue; // needs a painted tile
                    if(mapLoader.isNoSpawn(x,y)) continue;
                    if(world.isBlocked(x,y)) continue;
                    if(mapLoader.hasTile("walls", x, y)) continue;
                    if(mapLoader.hasTile("wall_vert", x, y)) continue;
                    if(mapLoader.hasTile("objects", x, y)) continue;
                    if(mapLoader.hasTile("extra", x, y)) continue;
                    if(x==player.getTileX() && y==player.getTileY()) continue;

                    if(tooCloseToOtherDrops(x,y,12)) continue;
                    if(Math.abs(x-player.getTileX()) + Math.abs(y-player.getTileY()) < 10) continue;

                    looseShinies.add(new RelicDrop(x,y,pic));
                    break;
                }
            }
        }
    }

    private List<BufferedImage> loadRelicPics(){
        List<BufferedImage> pics = new ArrayList<>();
        for(String name : RELIC_RESOURCE_NAMES){
            BufferedImage img = readImage("relics/" + name);
            if(img != null){
                pics.add(img);
            }
        }
        return pics;
    }

    private int countMapRelics(){
        int count = 0;
        for(int y=0; y<world.getHeight(); y++){
            for(int x=0; x<world.getWidth(); x++){
                if(world.getTile(x,y) == TileType.RELIC){
                    count++;
                }
            }
        }
        return count;
    }

    private void syncRelicGoal(){
        int total = countMapRelics() + looseShinies.size();
        relicBag.setGoalPieces(total);
    }

    private BufferedImage[] loadStrip(String path){
        try{
            BufferedImage sheet = readImage(path);
            return sliceHorizontal(sheet);
        } catch(Exception ex){
            return null;
        }
    }

    private BufferedImage[] sliceHorizontal(BufferedImage sheet){
        if(sheet==null) return null;
        int h = sheet.getHeight();
        if(h<=0) return null;
        int frames = sheet.getWidth()/h;
        if(frames<=0) return new BufferedImage[]{sheet};
        BufferedImage[] out = new BufferedImage[frames];
        for(int i=0;i<frames;i++){
            out[i] = sheet.getSubimage(i*h, 0, h, h);
        }
        return out;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if(onStartScreen || startFading || waitingForContinue){
            startScreen.draw(g2, getWidth(), getHeight());
            if(startFading || fadeDone){
                long elapsed = System.currentTimeMillis() - fadeStartMs;
                double t = fadeDone ? 1.0 : Math.min(1.0, elapsed/(double)fadeDurationMs);
                int alpha = (int)Math.round(255*t);
                g2.setColor(new Color(0,0,0, alpha));
                g2.fillRect(0,0,getWidth(),getHeight());

                drawFadeWords(g2, elapsed, fadeDone);

                if(!fadeDone && t>=1.0){
                    fadeDone=true;
                    startFading=false;
                    waitingForContinue=true;
                }
            }
            return;
        }

        int availableWidth = getWidth();
        int availableHeight=getHeight();

        int hudSpace = hudBarHeight;
        int topMargin = topPad;

        int tileSize = Math.max(72, Math.min(144, tileSizeHint));

        int usableHeight= Math.max(0, availableHeight-hudSpace-topMargin);
        int viewWidthTiles = Math.max(1,(int)Math.ceil(availableWidth/(double)tileSize));
        int viewHeightTiles =  Math.max(1,(int)Math.ceil(usableHeight/(double)tileSize));

        int viewLeft = (int)Math.round(player.getTileX()-(viewWidthTiles-1)/2.0);
        int viewTop  = (int)Math.round(player.getTileY()-(viewHeightTiles-1)/2.0);

        if(viewLeft<0) viewLeft=0;
        if(viewTop<0){
            viewTop=0;
        }
        if(viewLeft+viewWidthTiles>world.getWidth())
            viewLeft=world.getWidth()-viewWidthTiles;
        if(viewTop+viewHeightTiles>world.getHeight())
            viewTop=world.getHeight()-viewHeightTiles;

        int shakeX = 0;
        int shakeY = 0;
        if(firstRelicCutsceneActive || secondRelicCutsceneActive || thirdRelicCutsceneActive){
            long elapsed = firstRelicCutsceneActive
                ? (System.currentTimeMillis() - firstRelicCutsceneStartMs)
                : (secondRelicCutsceneActive
                ? (System.currentTimeMillis() - secondRelicCutsceneStartMs)
                : (System.currentTimeMillis() - thirdRelicCutsceneStartMs));
            double duration = firstRelicCutsceneActive ? firstRelicCutsceneDurationMs
                : (secondRelicCutsceneActive ? secondRelicCutsceneDurationMs : thirdRelicCutsceneDurationMs);
            double t = Math.min(1.0, elapsed / Math.max(1.0, duration));
            double decay = 1.0 - t;
            double intensity = thirdRelicCutsceneActive ? 2.0 : (secondRelicCutsceneActive ? 1.6 : 1.0);
            shakeX = (int)Math.round(Math.sin(System.currentTimeMillis()*0.032) * 4 * decay * intensity);
            shakeY = (int)Math.round(Math.cos(System.currentTimeMillis()*0.040) * 6 * decay * intensity);
        }
        g2.translate(shakeX, shakeY);

        for(int y=0;y<viewHeightTiles;y++){
            int worldY=viewTop+y;
            if(worldY<0||worldY>=world.getHeight()){
                continue;
            }

            for(int x=0;x<viewWidthTiles;x++){
                int worldX=viewLeft+x;
                if(worldX<0||worldX>=world.getWidth()){
                    continue;
                }

                boolean blocked=world.isBlocked(worldX,worldY);
                boolean isPlayerHere=(worldX==player.getTileX()&&worldY==player.getTileY());

                int px=x*tileSize;
                int py = topPad + y*tileSize;

                double corruptionStrength = moodHaziness(worldX, worldY);
                drawTile(g2,blocked,px,py,isPlayerHere,worldX,worldY,tileSize,corruptionStrength);

                int fogAlpha=fogAlphaForTile(worldX,worldY);
                if(fogAlpha>0){
                    g2.setColor(new Color(0,0,0,fogAlpha));
                    g2.fillRect(px,py,tileSize,tileSize);
                }

            }
        }

        drawMonsters(g2, viewLeft, viewTop, tileSize, viewWidthTiles, viewHeightTiles);

        // Single halo over the ending portal when all relics are gathered
        if(relicBag.doneGathering() && endingMinX >= 0 && mapLoader != null){
            int haloLeftTiles = endingMinX - viewLeft;
            int haloTopTiles = endingMinY - viewTop;
            int haloWtiles = endingMaxX - endingMinX + 1;
            int haloHtiles = endingMaxY - endingMinY + 1;
            if(haloLeftTiles < viewWidthTiles && haloTopTiles < viewHeightTiles && haloLeftTiles + haloWtiles > 0 && haloTopTiles + haloHtiles > 0){
                double pulse = Math.sin(System.currentTimeMillis()/320.0)*0.25 + 0.75;
                int haloW = (int)Math.round(haloWtiles * tileSize * 1.2);
                int haloH = (int)Math.round(haloHtiles * tileSize * 1.2);
                int haloX = (int)Math.round((haloLeftTiles * tileSize) + (haloWtiles*tileSize - haloW)/2.0);
                int haloY = topPad + (int)Math.round((haloTopTiles * tileSize) + (haloHtiles*tileSize - haloH)/2.0);
                int alphaOuter = (int)(60 * pulse);
                int alphaInner = (int)(40 * pulse);
                g2.setColor(new Color(120, 220, 255, alphaOuter));
                g2.fillOval(haloX, haloY, haloW, haloH);
                g2.setColor(new Color(255, 255, 255, alphaInner));
                g2.fillOval(haloX + 10, haloY + 10, Math.max(10, haloW - 20), Math.max(10, haloH - 20));
            }
        }

        drawVignette(g2);
        if(!noCorruption && corruptionTintActive && !firstRelicCutsceneActive && !secondRelicCutsceneActive){
            Color tint = corruptionPhaseTwo ? new Color(150,40,90,70) : new Color(90,60,130,40);
            g2.setColor(tint);
            g2.fillRect(0,0,getWidth(),getHeight());
        }
        paintHud(g2, tileSize, viewHeightTiles);
        paintWhisper(g2);

        if(firstRelicCutsceneActive){
            drawFirstRelicCutsceneOverlay(g2);
        }
        if(secondRelicCutsceneActive){
            drawSecondRelicCutsceneOverlay(g2);
        }
        if(thirdRelicCutsceneActive){
            drawThirdRelicCutsceneOverlay(g2);
        }
        if(necroCutsceneActive){
            drawNecroCutsceneOverlay(g2);
        }

        g2.translate(-shakeX, -shakeY);

        if(mathActive){
            drawMathOverlay(g2);
            return;
        }

        if(gameOver){
            drawGameOverOverlay(g2);
        } else if(gameWon){
            if(postWinChoice){
                drawWinChoiceOverlay(g2);
            } else if(escapedWin){
                drawEscapeWinOverlay(g2);
            } else {
                drawGameOverOverlay(g2);
            }
        }
    }

    private void beginStartFade(){
        if(startFading) return;
        startFading=true;
        fadeDone=false;
        waitingForContinue=false;
        fadeStartMs=System.currentTimeMillis();
        repaint();
    }

    private void startFromFade(){
        onStartScreen=false;
        startFading=false;
        fadeDone=false;
        waitingForContinue=false;
        repaint();
    }

    private void drawStartScreen(Graphics2D g2){
        startScreen.draw(g2, getWidth(), getHeight());
    }

    private void restartIntoEndless(){
        postWinChoice = false;
        stopAllClips();
        if(timer != null){
            timer.stop();
        }
        bootIntoEndless = true;
        SwingUtilities.invokeLater(() -> {
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            if(w != null){
                w.dispose();
            }
            GameFrame gf = new GameFrame();
            gf.setVisible(true);
        });
    }

    private void drawFadeWords(Graphics2D g2, long elapsed, boolean hold){
        String[] lines = {
                "HELLO, TRAVELER",
                "WELCOME TO RELICSCAPE",
                "FIND THE RELICS AND ESCAPE"
        };

        long per = 880;
        int cx = getWidth()/2;
        int cy = getHeight()/2;

        for(int i=0;i<lines.length;i++){
            long local = elapsed - i*per;
            if(local < 0) continue;
            double t = hold ? 1.0 : Math.min(1.0, local/(double)per);
            double easeIn = t*t*t;
            double easeOut = 1 - Math.pow(1-t,3);
            double ease = 0.55*easeIn + 0.45*easeOut;
            float alpha = (float)Math.min(1.0, ease);
            int size = 56 + (int)(18*ease);

            g2.setFont(new Font("Garamond", Font.BOLD, size));
            String text = lines[i];
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            int dy = (i-1) * (th+10);
            int bob = (int)(Math.sin((elapsed/360.0)+(i*0.6)) * 4 * ease);

            g2.setColor(new Color(12,10,8,(int)(alpha*200)));
            g2.drawString(text, cx - tw/2 + 2, cy + dy + bob + 3);
            g2.setColor(new Color(235,230,222,(int)(alpha*235)));
            g2.drawString(text, cx - tw/2, cy + dy + bob);
        }
    }

    private void paintHud(Graphics2D g2,int tileSize,int viewHeightTiles){
        int pad = 20;
        int boxH = 96;
        int hudTop = Math.max(0, getHeight() - boxH - 12);

        GradientPaint gp = new GradientPaint(0, hudTop, new Color(32,26,20,238), 0, hudTop+boxH, new Color(18,14,12,242));
        g2.setPaint(gp);
        g2.fillRoundRect(pad, hudTop, getWidth()-pad*2, boxH, 14, 14);
        g2.setColor(new Color(170,150,118,190));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(pad, hudTop, getWidth()-pad*2, boxH, 14, 14);
        g2.setColor(new Color(255,228,190,35));
        g2.drawRoundRect(pad+3, hudTop+3, getWidth()-pad*2-6, boxH-6, 12, 12);

        g2.setColor(new Color(94,72,48,120));
        g2.fillRoundRect(pad+10, hudTop+10, getWidth()-pad*2-20, 6, 8, 8);

        int barW = 460;
        int barH = 26;
        int hp = player.getHearts();
        int hpMax = Math.max(1, player.getMaxHearts());
        float hpPct = Math.max(0f, Math.min(1f, hp/(float)hpMax));
        int barX = pad+18;
        int barY = hudTop + 22;
        g2.setColor(new Color(28,18,18,245));
        g2.fillRoundRect(barX, barY, barW, barH, 14, 14);
        g2.setColor(new Color(160,34,44,245));
        g2.fillRoundRect(barX, barY, (int)(barW*hpPct), barH, 14, 14);
        g2.setColor(new Color(220,198,150,240));
        g2.setStroke(new BasicStroke(2.4f));
        g2.drawRoundRect(barX, barY, barW, barH, 14, 14);
        g2.setFont(new Font("Garamond",Font.BOLD,18));
        g2.setColor(new Color(245,238,228));
        g2.drawString("HP " + hp + "/" + hpMax, barX+14, barY+barH-6);

        g2.setFont(new Font("Garamond",Font.BOLD,17));
        g2.setColor(new Color(228,214,190,235));
        g2.drawString("Relics: " + relicBag.bagCount() + "/" + relicBag.goalCount(), barX + barW + 38, barY + barH - 6);
    }

    private void paintWhisper(Graphics2D g2){
        int h = 56;
        int pad = 20;
        int hudH = 96;
        int hudTop = Math.max(0, getHeight() - hudH - 12);
        int y = Math.max(10, hudTop - h - 12);
        GradientPaint gp = new GradientPaint(0,y,new Color(38,30,22,236),0,y+h,new Color(20,16,12,236));
        g2.setPaint(gp);
        g2.fillRoundRect(pad-2, y, getWidth()-(pad-2)*2, h, 12, 12);
        g2.setColor(new Color(172,148,110,180));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(pad-2, y, getWidth()-(pad-2)*2, h, 12, 12);
        g2.setColor(new Color(255,228,190,40));
        g2.drawRoundRect(pad+2, y+3, getWidth()-(pad+2)*2, h-6, 10, 10);
        g2.setColor(new Color(240,234,224));
        g2.setFont(new Font("Garamond", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int tx = pad + 10;
        int ty = y + (h+fm.getAscent())/2 - 4;
        g2.drawString(lastMessage, tx, ty);
    }

    private void drawVignette(Graphics2D shadePen){
        Composite oldInk = shadePen.getComposite();
        shadePen.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
        shadePen.setColor(new Color(0,0,0));
        int screenWide = getWidth();
        int screenTall = getHeight();
        int borderFat = 32;
        shadePen.fillRect(0,0,screenWide,borderFat);
        shadePen.fillRect(0,screenTall-borderFat,screenWide,borderFat);
        shadePen.fillRect(0,0,borderFat,screenTall);
        shadePen.fillRect(screenWide-borderFat,0,borderFat,screenTall);
        shadePen.setComposite(oldInk);
    }

    private void drawMathOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int boxW = 600;
        int boxH = 120;
        int boxX = getWidth() / 2 - boxW / 2;
        int boxY = getHeight() / 2 - boxH / 2 - 50;

        g2.setColor(new Color(50, 50, 50));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);

        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        FontMetrics fmQ = g2.getFontMetrics();
        g2.drawString(mathQuestion, boxX + 20, boxY + 30);

        String inputStr = mathInput.toString();
        g2.setColor(Color.YELLOW);
        FontMetrics fmIn = g2.getFontMetrics();
        g2.drawString(inputStr, boxX + 12, boxY + (boxH + fmIn.getAscent() - fmIn.getDescent()) / 2);

        int kbHeight = drawMathKeyboard(g2, boxY + boxH + 20);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(Color.WHITE);
        String hint = "Type digits/symbols, Enter=submit, Backspace=erase, ESC=exit.";
        int sw = g2.getFontMetrics().stringWidth(hint);
        int hintY = boxY + boxH + kbHeight + 60;
        g2.drawString(hint, getWidth() / 2 - sw / 2, hintY);
    }

    private int drawMathKeyboard(Graphics2D g2, int startY) {
        String[] rows = {"1234567890", "xX^+-*/", "()"};
        int keyW = 34, keyH = 38, gap = 6;
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int rowWidth = row.length() * (keyW + gap) - gap;
            int x = getWidth() / 2 - rowWidth / 2;
            int y = startY + r * (keyH + gap);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                int kx = x + c * (keyW + gap);
                g2.setColor(new Color(35, 35, 35));
                g2.fillRoundRect(kx, y, keyW, keyH, 6, 6);
                g2.setColor(new Color(200, 200, 200));
                g2.drawRoundRect(kx, y, keyW, keyH, 6, 6);
                String s = String.valueOf(ch);
                FontMetrics fm = g2.getFontMetrics();
                int tx = kx + (keyW - fm.stringWidth(s)) / 2;
                int ty = y + (keyH + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, tx, ty);
            }
        }
        return rows.length * (keyH + gap) - gap;
    }

    private void drawGameOverOverlay(Graphics2D overPen){
        String bigShout;
        if(gameWon){
            bigShout = "YOU RESTORED THE WORLD";
        } else {
            bigShout = "YOU FELL IN THE RUINS";
        }

        long elapsed = deathAnimStartMs > 0 ? Math.max(0L, System.currentTimeMillis() - deathAnimStartMs) : 0L;
        float t = Math.min(1f, elapsed / 2200f);
        int red = Math.min(255, 140 + (int)(115 * t));
        int green = (int)(20 * (1f - t));
        int alpha = Math.min(255, 60 + (int)(190 * t));
        overPen.setColor(new Color(red, Math.max(0, green), Math.max(0, green), alpha));
        overPen.fillRect(0, 0, getWidth(), getHeight());

        overPen.setFont(new Font("Consolas",Font.BOLD,26));
        overPen.setColor(new Color(255,255,255,230));
        FontMetrics wordSizer = overPen.getFontMetrics();
        int shoutWide=wordSizer.stringWidth(bigShout);
        int shoutTall=wordSizer.getAscent();

        int centerX=getWidth()/2-shoutWide/2;
        int centerY=getHeight()/2-shoutTall/2;
        overPen.drawString(bigShout, centerX, centerY);

        overPen.setFont(new Font("Consolas", Font.PLAIN, 16));
        String lilNote = postWinChoice ? "ESC = quit, ENTER = restart peaceful run." : "Press ESC to quit.";
        int lilWide = overPen.getFontMetrics().stringWidth(lilNote);
        overPen.drawString(lilNote, getWidth()/2 - lilWide/2, centerY+30);
    }

    private void drawWinChoiceOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(0, 0, 0, 205));
        g2.fillRect(0,0,w,h);

        g2.setFont(new Font("Consolas", Font.BOLD, 26));
        String title = endlessMode ? "THE SHRINE IS STABLE" : "THE LAND IS HEALED";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title))/2;
        int ty = h/2 - fm.getHeight();
        g2.setColor(new Color(230, 250, 255));
        g2.drawString(title, tx, ty);

        g2.setFont(new Font("Consolas", Font.PLAIN, 18));
        String line1 = "ENTER = restart with many relics, no corruption, monsters heal you.";
        String line2 = "ESC = exit the adventure.";
        int l1x = (w - g2.getFontMetrics().stringWidth(line1))/2;
        int l2x = (w - g2.getFontMetrics().stringWidth(line2))/2;
        g2.setColor(new Color(210, 235, 240));
        g2.drawString(line1, l1x, ty + 42);
        g2.drawString(line2, l2x, ty + 72);
    }

    private void drawEscapeWinOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        long elapsed = System.currentTimeMillis() - escapedWinStartMs;
        float t = Math.min(1f, elapsed / 3200f);
        float pulse = (float)(0.6 + 0.4*Math.sin(elapsed/180.0));

        // Radiant gradient backdrop
        Color top = new Color(30, 10, 50, 240);
        Color mid = new Color(90, 40, 140, 200);
        Color bot = new Color(10, 90, 120, 200);
        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bot, true);
        g2.setPaint(gp);
        g2.fillRect(0,0,w,h);

        // Concentric light rings
        g2.setColor(new Color(255, 230, 200, 80));
        for(int i=0;i<7;i++){
            int r = (int)(120 + i*46 + 12*Math.sin((elapsed/260.0)+i));
            int cx = w/2;
            int cy = h/2;
            g2.drawOval(cx - r, cy - r, r*2, r*2);
        }

        // Star field sparkle
        java.util.Random randy = new java.util.Random(42);
        g2.setColor(new Color(255, 245, 230, 160));
        for(int i=0;i<140;i++){
            int sx = randy.nextInt(w);
            int sy = randy.nextInt(h);
            int sz = 1 + randy.nextInt(2);
            g2.fillRect(sx, sy, sz, sz);
        }

        // Center glow
        int glowR = (int)(260 + 40*pulse);
        g2.setColor(new Color(255, 240, 220, 120));
        g2.fillOval(w/2 - glowR/2, h/2 - glowR/2, glowR, glowR);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(w/2 - glowR/4, h/2 - glowR/4, glowR/2, glowR/2);

        // Text
        g2.setColor(new Color(255, 248, 240));
        g2.setFont(new Font("Garamond", Font.BOLD, 40));
        String title = "YOU ESCAPED WITH THE RELICS";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (w - tw)/2, h/2 - 40);

        g2.setFont(new Font("Consolas", Font.PLAIN, 20));
        String line1 = "Light folds around you. The corruption dissolves.";
        String line2 = "The relics hum — their promise kept.";
        int l1w = g2.getFontMetrics().stringWidth(line1);
        int l2w = g2.getFontMetrics().stringWidth(line2);
        g2.drawString(line1, (w - l1w)/2, h/2 + 6);
        g2.drawString(line2, (w - l2w)/2, h/2 + 32);

        g2.setFont(new Font("Garamond", Font.BOLD, 22));
        String prompt = "Press ESC to leave the restored world.";
        int pw = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (w - pw)/2, h - 60);
    }

    private void computeEndingBounds(){
        if(mapLoader == null) return;
        int[][] ending = mapLoader.getLayer("ending");
        if(ending == null) return;
        int h = ending.length;
        int w = h > 0 ? ending[0].length : 0;
        int minX=Integer.MAX_VALUE, minY=Integer.MAX_VALUE, maxX=-1, maxY=-1;
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                if(ending[y][x]!=0){
                    if(x<minX) minX=x;
                    if(y<minY) minY=y;
                    if(x>maxX) maxX=x;
                    if(y>maxY) maxY=y;
                }
            }
        }
        if(maxX>=minX && maxY>=minY){
            endingMinX = minX;
            endingMinY = minY;
            endingMaxX = maxX;
            endingMaxY = maxY;
        }
    }

    private void drawTile(Graphics2D tilePen,boolean blocked,int paintX,int paintY,
                          boolean isPlayerHere,int worldX,int worldY,int tileSize,double corruptionStrength){

        Color floorInk=new Color(20,25,28);
        tilePen.setColor(floorInk);
        tilePen.fillRect(paintX,paintY,tileSize,tileSize);

        if(mapLoader!=null){
            java.util.List<int[][]> layers=mapLoader.getVisualLayers();
            for(int layerIndex=0;layerIndex<layers.size();layerIndex++){
                int[][] layerGrid=layers.get(layerIndex);
                int gid=layerGrid[worldY][worldX];
                if(gid<=0) continue;
                // Skip rendering ending tiles; they are portal markers only
                if(mapLoader.getLayer("ending") == layerGrid) continue;
                java.awt.image.BufferedImage imgTile=mapLoader.getTileImage(gid);
                if(imgTile!=null){
                    tilePen.drawImage(imgTile,paintX,paintY,tileSize,tileSize,null);
                }
            }
        }

        for(RelicDrop drop : looseShinies){
            if(drop.x==worldX && drop.y==worldY && drop.pic!=null){
                int dropW=Math.max(18, tileSize-32);
                int dropH=Math.max(18, tileSize-32);
                int dropX = paintX + (tileSize - dropW)/2;
                int dropY = paintY + (tileSize - dropH)/2;

                // subtle halo just on this tile and under fog
                double pulse = Math.sin(System.currentTimeMillis()/780.0)*0.04;
                int haloSize = (int)(tileSize*(0.92 + pulse));
                int haloX = paintX + (tileSize - haloSize)/2;
                int haloY = paintY + (tileSize - haloSize)/2;
                tilePen.setColor(new Color(255, 230, 180, 52));
                tilePen.fillOval(haloX, haloY, haloSize, haloSize);
                tilePen.setColor(new Color(255, 245, 220, 36));
                tilePen.fillOval(haloX+2, haloY+2, haloSize-4, haloSize-4);

                int bob = (int)(Math.sin(System.currentTimeMillis()/520.0) * 3);
                tilePen.drawImage(drop.pic, dropX, dropY + bob, dropW, dropH, null);
                break;
            }
        }

        if(corruptionStrength>0.01){
            int spookyAlpha=(int)Math.min(230,Math.round(230*corruptionStrength));
            tilePen.setColor(new Color(80,40,120,spookyAlpha));
            tilePen.fillRect(paintX,paintY,tileSize,tileSize);
        }

        if(isPlayerHere){
            boolean walking = moving && (System.currentTimeMillis()-lastMoveMs) < 320L;
            boolean hurt = hurtAnimStartMs > 0 && (System.currentTimeMillis()-hurtAnimStartMs) < hurtAnimDurationMs;
            boolean dying = gameOver && deathAnimStartMs > 0;
            BufferedImage frame = pickFace(walking, hurt, dying);
            if(frame!=null){
                int faceW = frame.getWidth()*5;
                int faceH = frame.getHeight()*5;
                int faceX = paintX + (tileSize - faceW)/2;
                int faceY = paintY + (tileSize - faceH)/2;
                if(facingRight){
                    tilePen.drawImage(frame, faceX, faceY, faceW, faceH, null);
                } else {
                    tilePen.drawImage(frame, faceX+faceW, faceY, -faceW, faceH, null);
                }
            } else {
                tilePen.setColor(new Color(240,240,255));
                int inset=Math.max(4,tileSize/8);
                tilePen.fillRect(paintX+inset,paintY+inset,tileSize-inset*2,tileSize-inset*2);
            }
        }
    }

    private boolean tooCloseToOtherDrops(int x,int y,int minManhattan){
        for(RelicDrop d: looseShinies){
            int dist = Math.abs(d.x - x) + Math.abs(d.y - y);
            if(dist < minManhattan){
                return true;
            }
        }
        return false;
    }

    private BufferedImage pickFace(boolean walking, boolean hurt, boolean dying){
        if(dying && soldierDeathFrames!=null && soldierDeathFrames.length>0){
            long elapsed = System.currentTimeMillis() - deathAnimStartMs;
            long frameTime = 140L;
            int idx = (int)Math.min(soldierDeathFrames.length-1, elapsed / frameTime);
            return soldierDeathFrames[idx];
        }

        BufferedImage[] frames = hurt && soldierHurtFrames!=null ? soldierHurtFrames
                : (walking && soldierWalkFrames != null)
                ? soldierWalkFrames
                : soldierIdleFrames;
        if(frames == null || frames.length == 0) return null;

        long frameTime = walking ? 120L : 220L;
        long ticks = System.currentTimeMillis() / frameTime;
        int idx = (int)(ticks % frames.length);
        return frames[idx];
    }

    private static class RelicDrop{
        final int x;
        final int y;
        final BufferedImage pic;
        RelicDrop(int x,int y,BufferedImage pic){
            this.x=x; this.y=y; this.pic=pic;
        }
    }

    private static class Monster{
        float x;
        float y;
        final BufferedImage[] frames;
        BufferedImage[] idleFrames;
        BufferedImage[] walkFrames;
        BufferedImage[] attackFrames;
        BufferedImage[] closeAttackFrames;
        final float speedTilesPerSec;
        final long frameTimeMs = 140L;
        float moveBuffer = 0f;
        final MonsterType type;
        boolean facingRight = true;
        boolean movedLastTick = false;
        long attackAnimStartMs = -1L;
        long closeAttackAnimStartMs = -1L;
        long nextAttackReadyMs = 0L;
        float headingX = 0f;
        float headingY = 0f;
        float targetX = 0f;
        float targetY = 0f;
        long nextRetargetMs = 0L;
        float velX = 0f;
        float velY = 0f;
        float orbitAngleRad = 0f;
        int orbitDir = 1;
        long nextOrbitFlipMs = 0L;
        Monster(float x,float y,BufferedImage[] frames,float speedTilesPerSec, MonsterType type){
            this.x=x; this.y=y; this.frames=frames; this.speedTilesPerSec=speedTilesPerSec; this.type=type;
            this.walkFrames = frames;
        }
        BufferedImage pickFrame(){
            if(type == MonsterType.GOLEM){
                if(attackFrames != null && attackAnimStartMs > 0L){
                    long elapsed = System.currentTimeMillis() - attackAnimStartMs;
                    long frameTime = 110L;
                    int idx = (int)Math.min(attackFrames.length-1, elapsed / frameTime);
                    if(elapsed >= attackFrames.length * frameTime){
                        attackAnimStartMs = -1L;
                    }
                    return attackFrames[idx];
                }
                BufferedImage[] active = movedLastTick && walkFrames != null ? walkFrames
                        : (idleFrames != null ? idleFrames : frames);
                if(active==null || active.length==0) return null;
                long ticks = System.currentTimeMillis() / frameTimeMs;
                int idx = (int)(ticks % active.length);
                return active[idx];
            }

            if(type == MonsterType.NECRO){
                if(closeAttackFrames != null && closeAttackAnimStartMs > 0L){
                    long elapsed = System.currentTimeMillis() - closeAttackAnimStartMs;
                    long frameTime = 60L;
                    int idx = (int)Math.min(closeAttackFrames.length-1, elapsed / frameTime);
                    if(elapsed >= closeAttackFrames.length * frameTime){
                        closeAttackAnimStartMs = -1L;
                    }
                    return closeAttackFrames[idx];
                }
                if(attackFrames != null && attackAnimStartMs > 0L){
                    long elapsed = System.currentTimeMillis() - attackAnimStartMs;
                    long frameTime = 55L;
                    int idx = (int)Math.min(attackFrames.length-1, elapsed / frameTime);
                    if(elapsed >= attackFrames.length * frameTime){
                        attackAnimStartMs = -1L;
                    }
                    return attackFrames[idx];
                }
                BufferedImage[] active = movedLastTick && walkFrames != null ? walkFrames
                        : (idleFrames != null ? idleFrames : frames);
                if(active==null || active.length==0) return null;
                long ticks = System.currentTimeMillis() / frameTimeMs;
                int idx = (int)(ticks % active.length);
                return active[idx];
            }

            BufferedImage[] active = frames;
            if(active==null || active.length==0) return null;
            long ticks = System.currentTimeMillis() / frameTimeMs;
            int idx = (int)(ticks % active.length);
            return active[idx];
        }
    }

    private static class AttackEffect{
        float x;
        float y;
        float dx;
        float dy;
        final BufferedImage[] frames;
        final long startMs;
        final long lifeMs = 2200L;
        boolean facingRight;
        AttackEffect(float x,float y,float dx,float dy,BufferedImage[] frames,boolean facingRight){
            this.x=x; this.y=y; this.dx=dx; this.dy=dy; this.frames=frames; this.startMs=System.currentTimeMillis();
            this.facingRight = facingRight;
        }
        boolean expired(){
            return System.currentTimeMillis() - startMs > lifeMs;
        }
        BufferedImage pickFrame(){
            if(frames==null || frames.length==0) return null;
            long ticks = (System.currentTimeMillis() - startMs) / 40L;
            int idx = (int)(ticks % frames.length);
            return frames[idx];
        }
    }

    private final java.util.List<AttackEffect> spookyBlasts = new java.util.ArrayList<>();

    private void wakeFog() {
        discovered = new boolean[world.getHeight()][world.getWidth()];
        if(noFog){
            for(int y=0;y<discovered.length;y++){
                for(int x=0;x<discovered[0].length;x++){
                    discovered[y][x] = true;
                }
            }
        } else {
            peelFog(player.getTileX(), player.getTileY());
        }
    }

    private void peelFog(int cx, int cy){
        if(discovered == null){
            return;
        }
        if(noFog){
            return;
        }

        int r = (int)Math.ceil(revealRing);
        double r2 = revealRing * revealRing;

        for(int y = cy - r; y <= cy + r; y++){
            for(int x = cx - r; x <= cx + r; x++){
                if(!world.inBounds(x, y)) continue;
                double dx = x - cx;
                double dy = y - cy;
                if(dx*dx + dy*dy <= r2 + 0.25){
                    discovered[y][x] = true;
                }
            }
        }
    }

    private boolean isDiscovered(int x, int y){
        return discovered != null
                && y >= 0 && y < discovered.length
                && x >= 0 && x < discovered[0].length
                && discovered[y][x];
    }

    private boolean hasDrop(int x,int y){
        for(RelicDrop d: looseShinies){
            if(d.x==x && d.y==y) return true;
        }
        return false;
    }

    private void pickupLooseRelic(){
        for(int i=0;i<looseShinies.size();i++){
            RelicDrop d = looseShinies.get(i);
            if(d.x==player.getTileX() && d.y==player.getTileY()){
                pendingLooseRelic = true;
                pendingRelicX = player.getTileX();
                pendingRelicY = player.getTileY();
                startRelicMinigame(-1L, pendingRelicX, pendingRelicY);
                return;
            }
        }
    }

    private void startCorruptionIfReady(){
        if(noCorruption) return;
        if(corruptionStartMs >= 0L) return;
        if(relicBag.bagCount() >= 1){
            handleRelicMilestones();
            corruptionStartMs = System.currentTimeMillis();
            corruptionTintActive = true;
            lastMessage = "A creeping corruption spreads...";
        }
    }

    private void maybeTriggerSecondCutscene(){
        if(noCorruption) return;
        if(secondRelicCutsceneActive || secondRelicCutsceneDone) return;
        if(firstRelicCutsceneActive) return;
        if(relicBag.bagCount() >= 2){
            triggerSecondRelicCutscene();
        }
    }

    private void maybeTriggerThirdCutscene(){
        if(noCorruption) return;
        if(thirdRelicCutsceneActive || thirdRelicCutsceneDone) return;
        if(firstRelicCutsceneActive || secondRelicCutsceneActive) return;
        if(relicBag.bagCount() >= 3){
            triggerThirdRelicCutscene();
        }
    }

    private void maybeTriggerNecroCutscene(){
        if(noCorruption) return;
        if(necroCutsceneActive || necroCutsceneDone) return;
        if(firstRelicCutsceneActive || secondRelicCutsceneActive || thirdRelicCutsceneActive) return;
        if(relicBag.doneGathering()){
            triggerNecroCutscene();
        }
    }

    private void loadMonsterSprites(){
        monsterEyeFrames = loadStrip("monsters/OcularWatcher.png");
        monsterJellyFrames = loadStrip("monsters/DeathSlime.png");
        golemIdleFrames = loadStripFixed("monsters/Blue/White_Swoosh_VFX/Golem_1_idle.png", 8);
        golemWalkFrames = loadStripFixed("monsters/Blue/White_Swoosh_VFX/Golem_1_walk.png", 10);
        golemAttackFrames = loadStripFixed("monsters/Blue/White_Swoosh_VFX/Golem_1_attack.png", 11);
        necroIdleFrames = loadStripFixed("monsters/Necromancer/Idle/spr_NecromancerIdle_strip50.png", 50);
        necroWalkFrames = loadStripFixed("monsters/Necromancer/Walk/spr_NecromancerWalk_strip10.png", 10);
        necroAttackFrames = loadStripFixed("monsters/Necromancer/Attack/spr_NecromancerAttackWithoutEffect_strip47.png", 47);
        necroAttackFxFrames = loadStripFixed("monsters/Necromancer/Attack/spr_NecromancerAttackEffect_strip47.png", 47);
        necroSpawnFrames = loadStripFixed("monsters/Necromancer/Spawn/spr_NecromancerSpawn_strip20.png", 20);
    }

    private void loadSounds(){
        corruptionLoopClip = loadClip("sounds/Corrupted Area Corruption Pit Loop - Sound Effect (HD).wav");
        deathClip = loadClip("sounds/Death sound effect.wav");
        setClipGain(deathClip, 6f);
        portalClip = loadClip("sounds/Portal escape ending Sound Effect.wav");
        relicClip = loadClip("sounds/relic unlock.wav");
        footstepClip = loadClip("sounds/Footsteps Sound Effect.wav");
        preRelicBgm = loadClip("sounds/bgm/Restless_Melody_02.wav");
        relic1Bgm = loadClip("sounds/bgm/Dark_Ambient.wav");
        relic2Bgm = loadClip("sounds/bgm/Dark_Pulsating_Ambient.wav");
        relic3Bgm = loadClip("sounds/bgm/Long_Distorted_Ambient.wav");
        relic4Bgm = loadClip("sounds/bgm/amnesia_the_dark_descent___brute_theme_extended.wav");
        victoryClip = loadClip("sounds/victory_music___sound_effect_for_editing.wav");
        hurtClip = loadClip("sounds/undertale_damage_sound_effect.wav");

        setClipGain(footstepClip, -6f); // half-ish volume
    }

    private BufferedImage[] loadStripFixed(String path, int frames){
        try{
            BufferedImage sheet = readImage(path);
            if(sheet==null || frames<=0) return null;
            int h = sheet.getHeight();
            int frameW = Math.max(1, sheet.getWidth()/frames);
            BufferedImage[] out = new BufferedImage[frames];
            for(int i=0;i<frames;i++){
                int fx = Math.min(sheet.getWidth()-1, i*frameW);
                int fw = (i==frames-1) ? sheet.getWidth()-fx : frameW;
                fw = Math.max(1, fw);
                out[i] = sheet.getSubimage(fx, 0, fw, h);
            }
            return out;
        } catch(Exception ex){
            return null;
        }
    }

    private BufferedImage readImage(String path){
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try(InputStream in = GamePanel.class.getClassLoader().getResourceAsStream(normalized)){
            if(in != null){
                return ImageIO.read(in);
            }
        } catch(Exception ignored){ }
        try{
            File f = new File(path);
            if(f.exists()){
                return ImageIO.read(f);
            }
            File alt = new File(normalized);
            if(alt.exists()){
                return ImageIO.read(alt);
            }
        } catch(Exception ignored){ }
        return null;
    }

    private Clip loadClip(String path){
        Clip clip = tryLoadClip(path);
        if(clip == null && path.toLowerCase().endsWith(".mp3")){
            String wavTwin = path.substring(0, path.length()-4) + ".wav";
            clip = tryLoadClip(wavTwin);
        }
        return clip;
    }

    private Clip tryLoadClip(String path){
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        AudioInputStream ais = null;
        AudioInputStream pcmStream = null;
        try{
            InputStream raw = GamePanel.class.getClassLoader().getResourceAsStream(normalized);
            if(raw != null){
                ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            } else {
                File f = new File(path);
                if(!f.exists()){
                    f = new File(normalized);
                }
                if(f.exists()){
                    ais = AudioSystem.getAudioInputStream(f);
                }
            }
            if(ais == null) return null;

            AudioFormat base = ais.getFormat();
            AudioFormat decoded = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false);

            pcmStream = AudioSystem.getAudioInputStream(decoded, ais);
            Clip c = AudioSystem.getClip();
            c.open(pcmStream);
            return c;
        } catch(Exception ignored){
            return null;
        } finally {
            if(pcmStream != null){ try{ pcmStream.close(); } catch(Exception ignored){} }
            if(ais != null){ try{ ais.close(); } catch(Exception ignored){} }
        }
    }

    private void setClipGain(Clip clip, float gainDb){
        if(clip == null) return;
        try{
            javax.sound.sampled.FloatControl ctrl = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), gainDb));
            ctrl.setValue(clamped);
        } catch(Exception ignored){ }
    }

    private void lowerBackgroundMusicVolumeForDeath(){
        float quietGain = -22f;
        setClipGain(preRelicBgm, quietGain);
        setClipGain(relic1Bgm, quietGain);
        setClipGain(relic2Bgm, quietGain);
        setClipGain(relic3Bgm, quietGain);
        setClipGain(relic4Bgm, quietGain);
    }

    private void playOnce(Clip clip){
        if(clip == null) return;
        try{
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch(Exception ignored){ }
    }

    private void ensureLooping(Clip clip){
        if(clip == null) return;
        try{
            if(clip.isRunning()) return;
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch(Exception ignored){ }
    }

    private void stopClip(Clip clip){
        if(clip == null) return;
        try{
            clip.stop();
            clip.setFramePosition(0);
        } catch(Exception ignored){ }
    }

    private void stopAllClips(){
        stopClip(footstepClip);
        stopClip(preRelicBgm);
        stopClip(relic1Bgm);
        stopClip(relic2Bgm);
        stopClip(relic3Bgm);
        stopClip(relic4Bgm);
        stopClip(corruptionLoopClip);
        stopClip(deathClip);
        stopClip(portalClip);
        stopClip(relicClip);
        stopClip(victoryClip);
        stopClip(hurtClip);
    }

    private void playFootstep(){
        long now = System.currentTimeMillis();
        lastFootstepPlayMs = now;
        ensureLooping(footstepClip);
    }

    private void updateCorruptionLoop(){
        if(noCorruption){
            stopClip(corruptionLoopClip);
            return;
        }
        boolean shouldPlay = inCorruptionZone
                && (System.currentTimeMillis() - lastMoveMs) < 400L
                && !gameOver
                && !gameWon;
        if(shouldPlay){
            ensureLooping(corruptionLoopClip);
        } else {
            stopClip(corruptionLoopClip);
        }
    }

    private void updateFootstepLoop(){
        boolean pausedForCutscene = onStartScreen || startFading || waitingForContinue
                || firstRelicCutsceneActive || secondRelicCutsceneActive || thirdRelicCutsceneActive
                || necroCutsceneActive || firstRelicCutsceneAwaitingContinue || secondRelicCutsceneAwaitingContinue
                || thirdRelicCutsceneAwaitingContinue || necroCutsceneAwaitingContinue;
        boolean shouldPlay = (System.currentTimeMillis() - lastMoveMs) < 350L
                && !gameOver
                && !gameWon
                && !pausedForCutscene;
        if(shouldPlay){
            ensureLooping(footstepClip);
        } else {
            stopClip(footstepClip);
        }
    }

    private void switchBgm(Clip current, Clip next){
        if(current != null){
            stopClip(current);
        }
        if(next != null){
            ensureLooping(next);
        }
    }

    private void handleRelicMilestones(){
        if(endlessMode){
            // In peaceful overworld, lock music to Restless_Melody_02 regardless of relic count
            stopClip(relic1Bgm);
            stopClip(relic2Bgm);
            stopClip(relic3Bgm);
            stopClip(relic4Bgm);
            ensureLooping(preRelicBgm);
            return;
        }
        int count = relicBag.bagCount();
        if(count >= 4){
            switchBgm(relic3Bgm, relic4Bgm);
        } else if(count >= 3){
            switchBgm(relic2Bgm, relic3Bgm);
        } else if(count >= 2){
            switchBgm(relic1Bgm, relic2Bgm);
        } else if(count >= 1){
            switchBgm(preRelicBgm, relic1Bgm);
        }
    }

    private void updateGame(){
        if(onStartScreen){
            return;
        }
        if(mathActive){
            return;
        }
        if(gameOver || gameWon){
            if(gameWon && !postWinChoice && relicBag.doneGathering()){
                enterPostWinChoice("The shrine beckons—press ENTER to begin a peaceful relic hunt or ESC to leave.");
            }
            return;
        }

        if(firstRelicCutsceneActive){
            long elapsed = System.currentTimeMillis() - firstRelicCutsceneStartMs;
            if(elapsed >= firstRelicCutsceneDurationMs){
                firstRelicCutsceneAwaitingContinue = true;
            }
            return;
        }

        if(secondRelicCutsceneActive){
            long elapsed = System.currentTimeMillis() - secondRelicCutsceneStartMs;
            if(elapsed >= secondRelicCutsceneDurationMs){
                secondRelicCutsceneAwaitingContinue = true;
            }
            return;
        }

        if(thirdRelicCutsceneActive){
            long elapsed = System.currentTimeMillis() - thirdRelicCutsceneStartMs;
            if(elapsed >= thirdRelicCutsceneDurationMs){
                thirdRelicCutsceneAwaitingContinue = true;
            }
            return;
        }

        if(necroCutsceneActive){
            long elapsed = System.currentTimeMillis() - necroCutsceneStartMs;
            if(elapsed >= necroCutsceneDurationMs){
                necroCutsceneAwaitingContinue = true;
            }
            return;
        }

        if(firstRelicCutsceneDone){
            handleMonsterSpawns();
            updateMonsters();
            checkMonsterContacts();
        } else {
            // Allow the necromancer to chase even before the first relic cutscene completes
            updateNecroOnly();
            checkMonsterContacts();
        }

        if(!noCorruption){
            applyCorruptionDamage();
            updateCorruptionLoop();
        }
        updateFootstepLoop();
    }

    private void triggerFirstRelicCutscene(){
        firstRelicCutsceneStarted = true;
        firstRelicCutsceneActive = true;
        firstRelicCutsceneAwaitingContinue = false;
        firstRelicCutsceneStartMs = System.currentTimeMillis();
        lastMessage = "The first relic has been obtained. Something slumbering is stirring...";
        player.setInvulnerable(true);
    }

    private void completeFirstRelicCutscene(){
        firstRelicCutsceneActive = false;
        firstRelicCutsceneAwaitingContinue = false;
        firstRelicCutsceneDone = true;
        lastMessage = "Something slumbering has stirred...";
        player.setInvulnerable(false);
        nextMonsterSpawnMs = System.currentTimeMillis() + 500L;
    }

    private void triggerSecondRelicCutscene(){
        if(secondRelicCutsceneActive || secondRelicCutsceneDone) return;
        secondRelicCutsceneActive = true;
        secondRelicCutsceneAwaitingContinue = false;
        secondRelicCutsceneStartMs = System.currentTimeMillis();
        lastMessage = "Something awakens...";
        player.setInvulnerable(true);
    }

    private void completeSecondRelicCutscene(){
        secondRelicCutsceneActive = false;
        secondRelicCutsceneAwaitingContinue = false;
        secondRelicCutsceneDone = true;
        player.setInvulnerable(false);
        startCorruptionIfReady();
        corruptionSpanMs = corruptionSpanFastMs;
        corruptionPhaseTwo = true;
        lastMessage = "Something awakens... The corruption quickens!";
        spawnAwakeningWave();
        switchBgm(relic1Bgm, relic2Bgm);
    }

    private void triggerThirdRelicCutscene(){
        if(thirdRelicCutsceneActive || thirdRelicCutsceneDone) return;
        thirdRelicCutsceneActive = true;
        thirdRelicCutsceneAwaitingContinue = false;
        thirdRelicCutsceneStartMs = System.currentTimeMillis();
        lastMessage = "Blades whisper in the corruption...";
        player.setInvulnerable(true);
    }

    private void completeThirdRelicCutscene(){
        thirdRelicCutsceneActive = false;
        thirdRelicCutsceneAwaitingContinue = false;
        thirdRelicCutsceneDone = true;
        player.setInvulnerable(false);
        lastMessage = "A stone golem emerges from the corruption.";
        spawnGolemHuntersNearPlayer(2);
        switchBgm(relic2Bgm, relic3Bgm);
    }

    private void triggerNecroCutscene(){
        if(necroCutsceneActive || necroCutsceneDone) return;
        necroCutsceneActive = true;
        necroCutsceneAwaitingContinue = false;
        necroCutsceneStartMs = System.currentTimeMillis();
        lastMessage = "The relics shriek—something dire arrives.";
        player.setInvulnerable(true);
    }

    private void completeNecroCutscene(){
        necroCutsceneActive = false;
        necroCutsceneAwaitingContinue = false;
        necroCutsceneDone = true;
        player.setInvulnerable(false);
        lastMessage = "A necromancer tears through reality!";
        spawnNecroNearPlayer();
    }

    private void handleMonsterSpawns(){
        long now = System.currentTimeMillis();
        if(monsters.size() >= maxMonsters) return;
        if(now < nextMonsterSpawnMs) return;

        boolean spawned = spawnMonster();
        long delay = 2400L + rand.nextInt(2600);
        nextMonsterSpawnMs = now + delay;
        if(!spawned){
            nextMonsterSpawnMs = now + 1800L;
        }
    }

    private boolean spawnMonster(){
        if(mapLoader == null || mapLoader.getVisualLayers().isEmpty()) return false;
        int[][] base = mapLoader.getVisualLayers().get(0);
        int tries = 0;
        int width = world.getWidth();
        int height = world.getHeight();
        while(tries < 2000){
            tries++;
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            if(base[y][x]==0) continue;
            if(mapLoader.isNoSpawn(x,y)) continue;
            if(world.isBlocked(x,y)) continue;
            if(x==player.getTileX() && y==player.getTileY()) continue;
            if(hasDrop(x,y)) continue;
            if(monsterAt(x,y)) continue;

            MonsterType type = pickSpawnType();
            if(type==null) return false;
            BufferedImage[] frames = (type==MonsterType.EYE) ? monsterEyeFrames : monsterJellyFrames;
            if(frames==null || frames.length==0) continue;

            monsters.add(new Monster(x+0.5f, y+0.5f, frames, speedFor(type), type));
            return true;
        }
        return false;
    }

    private void spawnAwakeningWave(){
        for(int i=0;i<2;i++){
            spawnAwakeningType(MonsterType.JELLY);
        }
        for(int i=0;i<2;i++){
            spawnAwakeningType(MonsterType.EYE);
        }
    }

    private void spawnGolemHuntersNearPlayer(int count){
        if(golemWalkFrames == null || golemWalkFrames.length == 0) return;
        java.util.Set<String> used = new java.util.HashSet<>();
        for(int i=0;i<count;i++){
            Point spot = pickGolemSpotNearPlayer(4, 10, used);
            if(spot == null) break;
            used.add(spot.x+","+spot.y);
            Monster m = new Monster(spot.x + 0.5f, spot.y + 0.5f, golemWalkFrames, speedFor(MonsterType.GOLEM), MonsterType.GOLEM);
            m.walkFrames = golemWalkFrames;
            m.idleFrames = (golemIdleFrames != null && golemIdleFrames.length > 0) ? golemIdleFrames : golemWalkFrames;
            m.attackFrames = golemAttackFrames;
            m.facingRight = player.getTileX() >= m.x;
            monsters.add(m);
        }
    }

    private boolean spawnAwakeningType(MonsterType type){
        Point spot = pickAwakeningSpot();
        if(spot == null) return false;
        BufferedImage[] frames = (type==MonsterType.EYE) ? monsterEyeFrames : monsterJellyFrames;
        if(frames==null || frames.length==0) return false;
        monsters.add(new Monster(spot.x + 0.5f, spot.y + 0.5f, frames, speedFor(type), type));
        return true;
    }

    private Point pickAwakeningSpot(){
        if(mapLoader == null || mapLoader.getVisualLayers().isEmpty()) return null;
        int[][] base = mapLoader.getVisualLayers().get(0);
        int px = player.getTileX();
        int py = player.getTileY();
        int minR = 5;
        int maxR = 10;
        for(int tries=0; tries<220; tries++){
            int dx = rand.nextInt(maxR*2+1) - maxR;
            int dy = rand.nextInt(maxR*2+1) - maxR;
            if(Math.abs(dx)+Math.abs(dy) < minR) continue;
            int x = px + dx;
            int y = py + dy;
            if(!world.inBounds(x,y)) continue;
            if(base[y][x]==0) continue;
            if(world.isBlocked(x,y)) continue;
            if(mapLoader.isNoSpawn(x,y)) continue;
            if(monsterAt(x,y)) continue;
            if(hasDrop(x,y)) continue;
            if(x==px && y==py) continue;
            if(isDiscovered(x,y)) return new Point(x,y);
        }
        return null;
    }

    private void startPreRelicBgm(){
        ensureLooping(preRelicBgm);
    }

    private Point pickGolemSpotNearPlayer(int minR, int maxR, java.util.Set<String> disallow){
        if(mapLoader == null || mapLoader.getVisualLayers().isEmpty()) return null;
        int[][] base = mapLoader.getLayer("tile layer 1");
        if(base == null) return null;
        int px = player.getTileX();
        int py = player.getTileY();
        for(int tries=0; tries<260; tries++){
            int dx = rand.nextInt(maxR*2+1) - maxR;
            int dy = rand.nextInt(maxR*2+1) - maxR;
            if(Math.abs(dx)+Math.abs(dy) < minR) continue;
            int x = px + dx;
            int y = py + dy;
            if(disallow != null && disallow.contains(x+","+y)) continue;
            if(!world.inBounds(x,y)) continue;
            if(y < 0 || y >= base.length || x < 0 || x >= base[0].length) continue;
            if(base[y][x]==0) continue;
            if(world.isBlocked(x,y)) continue;
            if(mapLoader.isNoSpawn(x,y)) continue;
            if(mapLoader.hasTile("walls", x, y)) continue;
            if(mapLoader.hasTile("wall_vert", x, y)) continue;
            if(mapLoader.hasTile("objects", x, y)) continue;
            if(mapLoader.hasTile("extra", x, y)) continue;
            if(monsterAt(x,y)) continue;
            if(hasDrop(x,y)) continue;
            if(!isDiscovered(x,y)) continue;
            return new Point(x,y);
        }
        return null;
    }

    private Point pickGolemSpot(){
        if(mapLoader == null || mapLoader.getVisualLayers().isEmpty()) return null;
        int[][] base = mapLoader.getVisualLayers().get(0);
        int px = player.getTileX();
        int py = player.getTileY();
        int minR = 6;
        int maxR = 12;
        for(int tries=0; tries<260; tries++){
            int dx = rand.nextInt(maxR*2+1) - maxR;
            int dy = rand.nextInt(maxR*2+1) - maxR;
            if(Math.abs(dx)+Math.abs(dy) < minR) continue;
            int x = px + dx;
            int y = py + dy;
            if(!world.inBounds(x,y)) continue;
            if(base[y][x]==0) continue;
            if(world.isBlocked(x,y)) continue;
            if(mapLoader.isNoSpawn(x,y)) continue;
            if(monsterAt(x,y)) continue;
            if(hasDrop(x,y)) continue;
            if(!isDiscovered(x,y)) continue;
            return new Point(x,y);
        }
        return null;
    }

    private void spawnNecroNearPlayer(){
        if(necroWalkFrames == null || necroWalkFrames.length == 0) return;
        Point spot = pickNecroSpotNearPlayer(6, 12);
        if(spot == null){
            spot = new Point(player.getTileX()+6, player.getTileY());
        }
        Monster necro = new Monster(spot.x + 0.5f, spot.y + 0.5f, necroWalkFrames, speedFor(MonsterType.NECRO), MonsterType.NECRO);
        necro.walkFrames = necroWalkFrames;
        necro.idleFrames = necroIdleFrames;
        necro.attackFrames = necroAttackFrames;
        necro.closeAttackFrames = necroSpawnFrames;
        necro.walkFrames = null; // no walk animation; stay on idle/attack
        necro.facingRight = player.getTileX() >= necro.x;
        necro.headingX = necro.facingRight ? 1f : -1f;
        necro.headingY = 0f;
        necro.orbitAngleRad = (float)Math.atan2(necro.y - player.getTileY(), necro.x - player.getTileX());
        necro.orbitDir = rand.nextBoolean() ? 1 : -1;
        necro.nextOrbitFlipMs = System.currentTimeMillis() + 1800L + rand.nextInt(1400);
        necro.nextAttackReadyMs = System.currentTimeMillis() + 1200L; // prevent instant spam on spawn
        monsters.add(necro);
    }

    private Point pickNecroSpotNearPlayer(int minR, int maxR){
        if(mapLoader == null || mapLoader.getVisualLayers().isEmpty()) return null;
        int[][] base = mapLoader.getVisualLayers().get(0);
        int px = player.getTileX();
        int py = player.getTileY();
        for(int tries=0; tries<280; tries++){
            int dx = rand.nextInt(maxR*2+1) - maxR;
            int dy = rand.nextInt(maxR*2+1) - maxR;
            if(Math.abs(dx)+Math.abs(dy) < minR) continue;
            int x = px + dx;
            int y = py + dy;
            if(!world.inBounds(x,y)) continue;
            if(base[y][x]==0) continue;
            if(mapLoader.isNoSpawn(x,y)) continue;
            if(hasDrop(x,y)) continue;
            if(monsterAt(x,y)) continue;
            if(!isDiscovered(x,y)) continue;
            return new Point(x,y);
        }
        return null;
    }

    private MonsterType pickSpawnType(){
        int eye = countType(MonsterType.EYE);
        int jel = countType(MonsterType.JELLY);
        boolean canEye = eye < maxPerType && monsterEyeFrames != null && monsterEyeFrames.length>0;
        boolean canJel = jel < maxPerType && monsterJellyFrames != null && monsterJellyFrames.length>0;
        if(!canEye && !canJel) return null;
        if(canEye && canJel){
            return rand.nextBoolean() ? MonsterType.EYE : MonsterType.JELLY;
        }
        return canEye ? MonsterType.EYE : MonsterType.JELLY;
    }

    private int countType(MonsterType t){
        int c=0;
        for(Monster m: monsters){
            if(m.type==t) c++;
        }
        return c;
    }

    private float speedFor(MonsterType t){
        switch(t){
            case EYE: return 2.7f; // slightly under player speed for 0.9x pacing
            case JELLY: return 2.7f;
            case GOLEM: return 5.0f; // match player speed (~5 tiles/s)
            case NECRO: return 18.0f; // even faster to ensure overtaking
            default: return 2.7f;
        }
    }

    private boolean monsterAt(int x,int y){
        for(Monster m: monsters){
            int mx = (int)Math.floor(m.x);
            int my = (int)Math.floor(m.y);
            if(mx==x && my==y) return true;
        }
        return false;
    }

    private boolean canWalk(int gx,int gy){
        if(!world.inBounds(gx, gy)) return false;
        if(world.isBlocked(gx, gy)) return false;
        if(mapLoader != null && !mapLoader.getVisualLayers().isEmpty()){
            int[][] base = mapLoader.getVisualLayers().get(0);
            if(base[gy][gx]==0) return false; // requires a painted tile
        }
        return true;
    }

    private void updateMonsters(){
        float tilesPerTick = mTickTiles();
        for(Monster m: monsters){
            m.moveBuffer += tilesPerTick * m.speedTilesPerSec;

            if(m.type == MonsterType.NECRO){
                handleNecroStep(m, tilesPerTick);
                continue;
            }

            int guard = 0;
            while(m.moveBuffer >= 1f && guard < 4){
                guard++;
                int cx = (int)Math.floor(m.x);
                int cy = (int)Math.floor(m.y);
                int dx = Integer.compare(player.getTileX(), cx);
                int dy = Integer.compare(player.getTileY(), cy);

                if(m.type == MonsterType.GOLEM){
                    if(dx != 0){
                        m.facingRight = dx > 0;
                    } else {
                        m.facingRight = player.getTileX() >= m.x;
                    }
                    double dist = Math.hypot(player.getTileX() - cx, player.getTileY() - cy);
                    if(dist <= 1.2){
                        if(m.attackFrames != null && m.attackAnimStartMs < 0L){
                            m.attackAnimStartMs = System.currentTimeMillis();
                        }
                        m.moveBuffer = 0f;
                        m.movedLastTick = false;
                        break;
                    }
                }

                boolean moved = false;
                if(Math.abs(player.getTileX() - cx) >= Math.abs(player.getTileY() - cy)){
                    moved = attemptMonsterStep(m, cx+dx, cy);
                    if(!moved && dy!=0){
                        moved = attemptMonsterStep(m, cx, cy+dy);
                    }
                } else {
                    moved = attemptMonsterStep(m, cx, cy+dy);
                    if(!moved && dx!=0){
                        moved = attemptMonsterStep(m, cx+dx, cy);
                    }
                }

                if(moved){
                    m.moveBuffer -= 1f;
                    m.movedLastTick = true;
                } else {
                    m.moveBuffer = 0f;
                    m.movedLastTick = false;
                }

                if(m.type == MonsterType.GOLEM){
                    m.facingRight = player.getTileX() >= m.x;
                }
            }
        }
    }

    private void handleNecroStep(Monster necro, float tilesPerTick){
        double dx = player.getTileX() - necro.x;
        double dy = player.getTileY() - necro.y;
        double dist = Math.hypot(dx, dy);
        long now = System.currentTimeMillis();

        // Orbit around the player with smooth acceleration/deceleration and periodic direction flips.
        if(now >= necro.nextOrbitFlipMs){
            if(rand.nextDouble() < 0.35){
                necro.orbitDir *= -1;
            }
            necro.nextOrbitFlipMs = now + 1600L + rand.nextInt(1400);
        }

        // Advance orbit angle
        float orbitSpeed = 7.2f; // radians per second
        necro.orbitAngleRad += necro.orbitDir * orbitSpeed * tilesPerTick;

        float orbitRadius = 3.0f;
        necro.targetX = player.getTileX() + (float)Math.cos(necro.orbitAngleRad) * orbitRadius;
        necro.targetY = player.getTileY() + (float)Math.sin(necro.orbitAngleRad) * orbitRadius;

        double tdx = necro.targetX - necro.x;
        double tdy = necro.targetY - necro.y;
        double tdist = Math.hypot(tdx, tdy);
        if(tdist > 1e-4){
            double dirX = tdx / tdist;
            double dirY = tdy / tdist;

            // Acceleration toward target with gentle drag for decel
            float accel = 24.0f; // tiles/s^2
            float drag = 0.92f; // velocity retention per tick to sustain speed
            necro.velX = (float)(necro.velX * drag + accel * dirX * tilesPerTick);
            necro.velY = (float)(necro.velY * drag + accel * dirY * tilesPerTick);

            // Clamp speed to max
            double speed = Math.hypot(necro.velX, necro.velY);
            double maxSpeed = necro.speedTilesPerSec;
            if(speed > maxSpeed && speed > 1e-4){
                necro.velX *= maxSpeed / speed;
                necro.velY *= maxSpeed / speed;
            }

            necro.x += necro.velX * tilesPerTick;
            necro.y += necro.velY * tilesPerTick;
            necro.facingRight = necro.velX >= 0;
            necro.movedLastTick = Math.hypot(necro.velX, necro.velY) > 0.1;
        } else {
            necro.velX *= 0.85f;
            necro.velY *= 0.85f;
            necro.movedLastTick = Math.hypot(necro.velX, necro.velY) > 0.1;
        }

        boolean animating = (necro.attackAnimStartMs > 0L) || (necro.closeAttackAnimStartMs > 0L);
        boolean readyToCast = !animating && now >= necro.nextAttackReadyMs;
        boolean closeRange = dist < 2.8 && necro.closeAttackFrames != null;
        if(readyToCast){
            necro.nextAttackReadyMs = now + (closeRange ? 2400L : 2000L);
            if(closeRange){
                necro.closeAttackAnimStartMs = now;
                if(necroAttackFxFrames != null){
                    // Short, slower lunge toward the player
                    double elen = Math.max(1e-4, Math.hypot(dx, dy));
                    float ndx = (float)(dx/elen);
                    float ndy = (float)(dy/elen);
                    float projSpeed = 8.0f;
                    spookyBlasts.add(new AttackEffect(necro.x, necro.y, ndx*projSpeed, ndy*projSpeed, necroAttackFxFrames, ndx>=0));
                }
            } else if(dist < 8.2 && necro.attackFrames != null){
                necro.attackAnimStartMs = now;
                if(necroAttackFxFrames != null){
                    double elen = Math.max(1e-4, Math.hypot(dx, dy));
                    float ndx = (float)(dx/elen);
                    float ndy = (float)(dy/elen);
                    float projSpeed = 10.5f;
                    spookyBlasts.add(new AttackEffect(necro.x, necro.y, ndx*projSpeed, ndy*projSpeed, necroAttackFxFrames, ndx>=0));
                }
            } else {
                // Too far; wait and try again soon
                necro.nextAttackReadyMs = now + 700L;
            }
        }
    }

    private void updateNecroOnly(){
        float tilesPerTick = mTickTiles();
        for(Monster m : monsters){
            if(m.type != MonsterType.NECRO) continue;
            handleNecroStep(m, tilesPerTick);
        }
    }

    private float mTickTiles(){
        return tickMs / 1000f;
    }

    private boolean attemptMonsterStep(Monster m, int tx, int ty){
        if(m.type == MonsterType.NECRO){
            m.x = tx + 0.5f;
            m.y = ty + 0.5f;
            return true;
        }
        if(!canWalk(tx, ty)) return false;
        m.x = tx + 0.5f;
        m.y = ty + 0.5f;
        return true;
    }

    private void checkMonsterContacts(){
        int px = player.getTileX();
        int py = player.getTileY();
        long now = System.currentTimeMillis();

        // Only attack effects hurt from necro; use radius overlap so fast projectiles still hit
        for(AttackEffect blast : new java.util.ArrayList<>(spookyBlasts)){
            double dx = blast.x - player.getTileX();
            double dy = blast.y - player.getTileY();
            double dist = Math.hypot(dx, dy);
            if(dist <= 0.45){ // tightened radius
                if(now - lastDamageMs >= damageCooldownMs){
                    lastDamageMs = now;
                    hurtAnimStartMs = now;
                    if(monstersHeal){
                        healPlayer(24);
                        lastMessage = "A violet spark mends your wounds.";
                    } else {
                        playOnce(hurtClip);
                        player.takeDamage(30);
                        lastMessage = "Necrotic blades rip through you!";
                        checkIfDone();
                    }
                }
                break;
            }
        }

        for(Monster m: monsters){
            if(m.type == MonsterType.NECRO) continue; // necro contact is ignored; effect handles damage
            int mx = (int)Math.floor(m.x);
            int my = (int)Math.floor(m.y);
            if(mx==px && my==py){
                if(now - lastDamageMs >= damageCooldownMs){
                    lastDamageMs = now;
                    hurtAnimStartMs = now;
                    if(monstersHeal){
                        healPlayer(14);
                        lastMessage = "The creature shares its vitality.";
                    } else {
                        int dmg;
                        if(m.type == MonsterType.GOLEM){
                            dmg = 22;
                        } else if(m.type == MonsterType.EYE || m.type == MonsterType.JELLY){
                            dmg = 15;
                        } else {
                            dmg = 10;
                        }
                        playOnce(hurtClip);
                        player.takeDamage(dmg);
                        knockPlayerFrom(mx,my);
                        lastMessage = "You are struck by a lurking horror!";
                        checkIfDone();
                    }
                }
                break;
            }
        }
    }

    private void healPlayer(int amount){
        if(player == null) return;
        if(amount <= 0) return;
        int healed = Math.min(player.getMaxHearts(), player.getHearts() + amount);
        player.setHearts(healed);
    }

    private void knockPlayerFrom(int mx,int my){
        int dx = Integer.compare(player.getTileX(), mx);
        int dy = Integer.compare(player.getTileY(), my);
        int targetX = player.getTileX() + dx;
        int targetY = player.getTileY() + dy;
        if(canWalk(targetX, targetY)){
            player.setPosition(targetX, targetY);
            peelFog(targetX, targetY);
        }
    }

    private void drawMonsters(Graphics2D monsterCrayon, int viewLeft, int viewTop, int tileSize, int viewWidthTiles, int viewHeightTiles){
        for(Monster beast: monsters){
            int gridX = (int)Math.floor(beast.x);
            int gridY = (int)Math.floor(beast.y);
            if(gridX < viewLeft || gridX >= viewLeft + viewWidthTiles) continue;
            if(gridY < viewTop || gridY >= viewTop + viewHeightTiles) continue;

            double canvasX = (beast.x - viewLeft) * tileSize;
            double canvasY = (beast.y - viewTop) * tileSize;

            BufferedImage facePic = beast.pickFrame();
            if(facePic != null){
                double squishRatio = facePic.getHeight() / (double)Math.max(1, facePic.getWidth());
                double blobbyScale;
                int spriteWide;
                int spriteTall;
                if(beast.type == MonsterType.NECRO){
                    blobbyScale = 8.0; // fixed necromancer size
                    spriteWide = (int)Math.round(tileSize * blobbyScale);
                    spriteTall = spriteWide; // lock height to width to prevent frame-based size pops
                    // center all frames identically to avoid idle/attack size pops
                    double maxDim = Math.max(facePic.getWidth(), facePic.getHeight());
                    double normW = facePic.getWidth() / Math.max(1.0, maxDim);
                    double normH = facePic.getHeight() / Math.max(1.0, maxDim);
                    spriteWide = (int)Math.round(tileSize * blobbyScale * normW);
                    spriteTall = (int)Math.round(tileSize * blobbyScale * normH);
                } else if(beast.type == MonsterType.GOLEM){
                    blobbyScale = 3.0;
                    spriteWide = (int)Math.round(tileSize * blobbyScale);
                    spriteTall = (int)Math.round(spriteWide * squishRatio);
                } else {
                    blobbyScale = 0.88;
                    spriteWide = (int)Math.round(tileSize * blobbyScale);
                    spriteTall = (int)Math.round(spriteWide * squishRatio);
                }
                int paintX = (int)Math.round(canvasX - spriteWide/2.0);
                int paintY = topPad + (int)Math.round(canvasY - spriteTall/2.0);

                int fogShade = fogAlphaForTile(gridX, gridY);
                double purpleMood = moodHaziness(gridX, gridY);
                float fogFade = (float)(1.0 - Math.min(0.82, fogShade/255.0 * 0.9));
                float moodFade = (float)(1.0 - Math.min(0.55, purpleMood * 0.7));
                float finalFade = Math.max(0f, Math.min(1f, fogFade * moodFade));

                if(finalFade < 0.999f){
                    java.awt.image.RescaleOp rescale = new java.awt.image.RescaleOp(
                            new float[]{finalFade, finalFade, finalFade, 1f},
                            new float[]{0f,0f,0f,0f}, null);
                    BufferedImage ghostPic = new BufferedImage(facePic.getWidth(), facePic.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ghostPen = ghostPic.createGraphics();
                    ghostPen.drawImage(facePic, 0, 0, null);
                    ghostPen.dispose();
                    ghostPic = rescale.filter(ghostPic, null);
                    drawFlipped(monsterCrayon, ghostPic, paintX, paintY, spriteWide, spriteTall, beast.facingRight);
                } else {
                    drawFlipped(monsterCrayon, facePic, paintX, paintY, spriteWide, spriteTall, beast.facingRight);
                }
            }
        }

        // draw necro attack effects
        java.util.List<AttackEffect> expired = new java.util.ArrayList<>();
        for(AttackEffect fx : spookyBlasts){
            if(fx.expired()){
                expired.add(fx);
                continue;
            }
            fx.x += fx.dx * (tickMs/1000f);
            fx.y += fx.dy * (tickMs/1000f);

            int gx = (int)Math.floor(fx.x);
            int gy = (int)Math.floor(fx.y);
            if(gx < viewLeft || gx >= viewLeft + viewWidthTiles) continue;
            if(gy < viewTop || gy >= viewTop + viewHeightTiles) continue;

            double screenX = (fx.x - viewLeft) * tileSize;
            double screenY = (fx.y - viewTop) * tileSize;
            BufferedImage frame = fx.pickFrame();
            if(frame != null){
                double ratio = frame.getHeight() / (double)Math.max(1, frame.getWidth());
                double scale = 9.5; // even larger for visibility
                int sw = (int)Math.round(tileSize * scale);
                int sh = (int)Math.round(sw * ratio);
                int dx = (int)Math.round(screenX - sw/2.0);
                int dy = topPad + (int)Math.round(screenY - sh/2.0);
                drawFlipped(monsterCrayon, frame, dx, dy, sw, sh, fx.facingRight);
            }
        }
        spookyBlasts.removeAll(expired);
    }

    private void drawFlipped(Graphics2D flipPen, BufferedImage spritePic, int paintX, int paintY, int paintW, int paintH, boolean lookRight){
        if(lookRight){
            flipPen.drawImage(spritePic, paintX, paintY, paintW, paintH, null);
        } else {
            flipPen.drawImage(spritePic, paintX + paintW, paintY, -paintW, paintH, null);
        }
    }

    private void drawFirstRelicCutsceneOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,w,h);

        GradientPaint gp = new GradientPaint(0,0,new Color(10,0,20,110),0,h,new Color(0,0,0,190));
        Paint old = g2.getPaint();
        g2.setPaint(gp);
        g2.fillRect(0,0,w,h);
        g2.setPaint(old);

        g2.setFont(new Font("Garamond", Font.BOLD, 32));
        String lineMain = "The first relic has been obtained.";
        String lineSub = "Something slumbering is stirring...";
        FontMetrics textMetrics = g2.getFontMetrics();
        int centerY = h/2 - textMetrics.getHeight();
        int lineMainX = (w - textMetrics.stringWidth(lineMain))/2;
        g2.setColor(new Color(235,230,222));
        g2.drawString(lineMain, lineMainX, centerY);
        int lineSubX = (w - textMetrics.stringWidth(lineSub))/2;
        g2.drawString(lineSub, lineSubX, centerY + textMetrics.getHeight()+12);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The map will start getting corrupted. Collect all the relics before it's too late.";
        FontMetrics warnMetrics = g2.getFontMetrics();
        int warnX = (w - warnMetrics.stringWidth(warn))/2;
        int warnY = h - 70;
        g2.setColor(new Color(210,72,72,240));
        g2.drawString(warn, warnX, warnY);

        String prompt = firstRelicCutsceneAwaitingContinue ? "Click or press any key to steel yourself." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 18));
        FontMetrics promptMetrics = g2.getFontMetrics();
        int promptX = (w - promptMetrics.stringWidth(prompt))/2;
        int promptY = h - 36;
        g2.setColor(new Color(240,234,224,230));
        g2.drawString(prompt, promptX, promptY);
    }

    private void drawSecondRelicCutsceneOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(40,0,0,190));
        g2.fillRect(0,0,w,h);

        GradientPaint gp = new GradientPaint(0,0,new Color(120,20,40,160),0,h,new Color(50,0,70,210));
        Paint old = g2.getPaint();
        g2.setPaint(gp);
        g2.fillRect(0,0,w,h);
        g2.setPaint(old);

        g2.setFont(new Font("Garamond", Font.BOLD, 34));
        String lineMain = "The second relic trembles.";
        String lineSub = "Something awakens...";
        FontMetrics textMetrics = g2.getFontMetrics();
        int centerY = h/2 - textMetrics.getHeight();
        int lineMainX = (w - textMetrics.stringWidth(lineMain))/2;
        g2.setColor(new Color(255,236,228));
        g2.drawString(lineMain, lineMainX, centerY);
        int lineSubX = (w - textMetrics.stringWidth(lineSub))/2;
        g2.setColor(new Color(255,90,110));
        g2.drawString(lineSub, lineSubX, centerY + textMetrics.getHeight()+14);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The corruption quickens. It will spread faster now.";
        FontMetrics warnMetrics = g2.getFontMetrics();
        int warnX = (w - warnMetrics.stringWidth(warn))/2;
        int warnY = h - 78;
        g2.setColor(new Color(230,70,90,245));
        g2.drawString(warn, warnX, warnY);

        String prompt = secondRelicCutsceneAwaitingContinue ? "Click or press any key to face it." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 19));
        FontMetrics promptMetrics = g2.getFontMetrics();
        int promptX = (w - promptMetrics.stringWidth(prompt))/2;
        int promptY = h - 40;
        g2.setColor(new Color(240,234,224,235));
        g2.drawString(prompt, promptX, promptY);
    }

    private void drawThirdRelicCutsceneOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(80,0,20,210));
        g2.fillRect(0,0,w,h);
        GradientPaint gp = new GradientPaint(0,0,new Color(180,20,60,180),0,h,new Color(60,0,90,220));
        Paint old = g2.getPaint();
        g2.setPaint(gp);
        g2.fillRect(0,0,w,h);
        g2.setPaint(old);

        g2.setFont(new Font("Garamond", Font.BOLD, 36));
        String lineMain = "The third relic shatters the silence.";
        String lineSub = "Blades sing in the dark...";
        FontMetrics textMetrics = g2.getFontMetrics();
        int centerY = h/2 - textMetrics.getHeight();
        int lineMainX = (w - textMetrics.stringWidth(lineMain))/2;
        g2.setColor(new Color(255,240,232));
        g2.drawString(lineMain, lineMainX, centerY);
        int lineSubX = (w - textMetrics.stringWidth(lineSub))/2;
        g2.setColor(new Color(255,110,140));
        g2.drawString(lineSub, lineSubX, centerY + textMetrics.getHeight()+16);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The corruption peaks. Brace yourself.";
        FontMetrics warnMetrics = g2.getFontMetrics();
        int warnX = (w - warnMetrics.stringWidth(warn))/2;
        int warnY = h - 86;
        g2.setColor(new Color(240,70,90,245));
        g2.drawString(warn, warnX, warnY);

        String prompt = thirdRelicCutsceneAwaitingContinue ? "Click or press any key to brace for the blade." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 20));
        FontMetrics promptMetrics = g2.getFontMetrics();
        int promptX = (w - promptMetrics.stringWidth(prompt))/2;
        int promptY = h - 44;
        g2.setColor(new Color(240,234,224,240));
        g2.drawString(prompt, promptX, promptY);
    }

    private void drawNecroCutsceneOverlay(Graphics2D g2){
        int w = getWidth();
        int h = getHeight();
        long elapsed = System.currentTimeMillis() - necroCutsceneStartMs;
        float flash = (float)(0.5 + 0.5*Math.sin(elapsed/60.0));

        g2.setColor(new Color(120,0,0,230));
        g2.fillRect(0,0,w,h);
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,w,h);

        for(int i=0;i<8;i++){
            int alpha = (int)(80 + 120*Math.sin(elapsed/90.0 + i));
            g2.setColor(new Color(200,30,30, Math.max(0, Math.min(255, alpha))));
            int pad = 12 + i*8;
            g2.drawRect(pad, pad, w-pad*2, h-pad*2);
        }

        g2.setFont(new Font("Consolas", Font.BOLD, 38));
        String warning = "THE NECROMANCER HUNTS. FLEE.";
        FontMetrics fm = g2.getFontMetrics();
        int wx = (w - fm.stringWidth(warning))/2;
        int wy = h/2 - fm.getHeight();
        g2.setColor(new Color(255, 240, 240, 240));
        g2.drawString(warning, wx, wy);

        g2.setFont(new Font("Consolas", Font.BOLD, 24));
        String sub = "All relics gathered. The exit is your only hope.";
        int sx = (w - g2.getFontMetrics().stringWidth(sub))/2;
        g2.setColor(new Color(255, 80, 80, 230));
        g2.drawString(sub, sx, wy + 44);

        g2.setFont(new Font("Garamond", Font.BOLD, 22));
        String prompt = necroCutsceneAwaitingContinue ? "Click or press any key to face the doom." : "...";
        int px = (w - g2.getFontMetrics().stringWidth(prompt))/2;
        g2.setColor(new Color(255, 220, 220, 235));
        g2.drawString(prompt, px, h - 40);

        g2.setColor(new Color(255, 0, 0, (int)(120*flash)));
        g2.fillRect(0,0,w,h);
    }

    private int fogAlphaForTile(int worldX, int worldY){
        if(noFog){
            return 0;
        }
        if(!isDiscovered(worldX, worldY)){
            return 235;
        }

        double fogGap = Math.hypot(worldX - player.getTileX(), worldY - player.getTileY());

        if(fogGap <= clearRing){
            return 0;
        }

        double fogSpan = Math.max(1e-3,(fogEdge-clearRing));
        double fogBlend = (fogGap-clearRing)/fogSpan;
        if(fogBlend > 1.0) fogBlend = 1.0;
        if(fogBlend < 0.0) fogBlend = 0.0;

        double fogSquish = fogBlend*fogBlend*(3-2*fogBlend);
        int fogSoft = 18;
        int fogThick  = 170;
        return (int)Math.round(fogSoft + (fogThick-fogSoft) * fogSquish);
    }

    private double moodHaziness(int worldX,int worldY){
        if(noCorruption){
            return 0.0;
        }
        if(corruptionStartMs < 0L){
            return 0.0;
        }
        long spookyClock = System.currentTimeMillis() - corruptionStartMs;
        double creepJuice = Math.min(1.0, spookyClock / (double)corruptionSpanMs);

        double topTilt = 1.0 - (worldY / Math.max(1.0, (world.getHeight()-1)));
        double wiggleNoise = (Util.scrappyPick(worldX, worldY, 7, 100) - 50) / 520.0; // small, fixed per tile

        double spookGate = clamp01(topTilt + wiggleNoise + 0.03);

        double spill = 0.45;
        double purplePunch = clamp01((creepJuice - spookGate) / spill);
        double softenedPunch = purplePunch*purplePunch * (3 - 2*purplePunch);
        return softenedPunch;
    }

    private void applyCorruptionDamage(){
        if(noCorruption){
            return;
        }
        if(corruptionStartMs < 0L) return;
        if(player == null) return;

        double purpleHeat = moodHaziness(player.getTileX(), player.getTileY());
        long scaryClock = System.currentTimeMillis();

        boolean hoppingIn = !inCorruptionZone && purpleHeat >= corruptionEntryThreshold;
        boolean hoppingOut = inCorruptionZone && purpleHeat < corruptionExitThreshold;

        if(hoppingIn){
            inCorruptionZone = true;
            if(corruptionExposureStartMs < 0L){
                corruptionExposureStartMs = scaryClock;
                corruptionDamageRemainder = 0.0;
            }
            lastMessage = "The corruption crackles—get out now!";
        } else if(hoppingOut){
            inCorruptionZone = false;
            corruptionExposureStartMs = -1L;
            corruptionDamageRemainder = 0.0;
            return;
        }

        if(!inCorruptionZone){
            return;
        }

        if(corruptionExposureStartMs < 0L){
            corruptionExposureStartMs = scaryClock;
            corruptionDamageRemainder = 0.0;
        }

        long stayTime = scaryClock - corruptionExposureStartMs;
        if(stayTime < 3000L){
            lastMessage = "The corruption crackles—get out now!";
            return; // grace period per entry
        }

        lastMessage = "The corruption crackles—get out now!";

        double tickSeconds = tickMs / 1000.0;
        double spicyFactor = Math.pow(clamp01(purpleHeat), 1.25);
        double hurtPerSecond = 8.0 + (spicyFactor * 32.0); // heavier damage at higher intensity
        corruptionDamageRemainder += hurtPerSecond * tickSeconds;

        int wholeHurts = (int)Math.floor(corruptionDamageRemainder);
        if(wholeHurts > 0){
            corruptionDamageRemainder -= wholeHurts;
            player.takeDamage(wholeHurts);
            lastMessage = "The corruption sears you!";
            checkIfDone();
        }
    }

    private double clamp01(double wiggly){
        if(wiggly < 0.0) return 0.0;
        if(wiggly > 1.0) return 1.0;
        return wiggly;
    }

    private boolean tileIsCorrupted(int worldX, int worldY){
        if(noCorruption){
            return false;
        }
        return moodHaziness(worldX, worldY) >= corruptionEntryThreshold;
    }
}
