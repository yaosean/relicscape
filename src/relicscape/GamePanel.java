package relicscape;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

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

    private String lastMessage = "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver=false;
    private boolean gameWon = false;
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

    private boolean[][] discovered;
    private BufferedImage[] soldierWalkFrames;
    private BufferedImage[] soldierIdleFrames;
    private BufferedImage[] soldierHurtFrames;
    private BufferedImage[] soldierDeathFrames;
    private final StartScreenRenderer startScreen;
    private final List<RelicDrop> looseShinies = new ArrayList<>();
    private enum MonsterType { EYE, JELLY }
    private final List<Monster> monsters = new ArrayList<>();
    private BufferedImage[] monsterEyeFrames;
    private BufferedImage[] monsterJellyFrames;

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
        loadPlayerLook();
        loadMonsterSprites();
        scatterRelicPics();
        syncRelicGoal();

        startScreen = new StartScreenRenderer();

        timer = new Timer(tickMs, e->{
            updateGame();
            repaint();
        });
        timer.start();

        lastMessage = "Explore the world. Find " + relicBag.goalCount() + " relic fragments and return to the central shrine.";
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(thirdRelicCutsceneActive && thirdRelicCutsceneAwaitingContinue){
            completeThirdRelicCutscene();
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

    @Override
    public void mouseClicked(MouseEvent e) {
        if(thirdRelicCutsceneActive && thirdRelicCutsceneAwaitingContinue){
            completeThirdRelicCutscene();
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
        if(gameOver){
            return;
        }
        long now = System.currentTimeMillis();
        if(now-lastMoveMs<MOVE_GAP_MS){
            return;
        }
        int newX=player.getX()+dx;
        int newY=player.getY()+dy;

        if(!world.inBounds(newX,newY)){
            lastMessage="You feel the edge of the world.";
            return;
        }

        if(world.isBlocked(newX,newY)){
            lastMessage="You can't move through that.";
            return;
        }

        player.dropAt(newX, newY);
        lastMoveMs=now;
        moving=true;

        if(dx!=0){
            facingRight = dx > 0;
        }

        peelFog(newX,newY);
        TileType tile = world.getTile(newX,newY);
        feelTile(tile);
        pickupLooseRelic();
        checkIfDone();
    }

    private void feelTile(TileType tile) {
        if(tile==TileType.RELIC){
            relicBag.stashOne();
            world.setTile(player.getX(),player.getY(),world.baseForRow(player.getY()));
            lastMessage="You found a relic fragment! ("+
                    relicBag.bagCount()+"/"+relicBag.goalCount()+")";
            if(!firstRelicCutsceneStarted){
                triggerFirstRelicCutscene();
            }
            startCorruptionIfReady();
            maybeTriggerSecondCutscene();
            maybeTriggerThirdCutscene();
        } else if(tile==TileType.SHRINE){
            if(relicBag.doneGathering()){
                gameWon=true;
                lastMessage="Light erupts from the shrine as the land begins to heal.";
            } else {
                lastMessage="The shrine hums softly. It needs more relics.";
            }
        } else if(tile==TileType.DUNE){
            lastMessage="Your feet sink in the shifting sand.";
        } else if(tile==TileType.RUBBLE){
            lastMessage="Broken stones crunch underfoot.";
        } else {
            lastMessage="You move onward.";
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
            return;
        }

        if(relicBag.doneGathering()){
            // Let the shrine resolve message handle the win when standing on it.
            lastMessage="You feel the relics hum. The shrine must be close.";
        }
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
        int[][] base = mapLoader.getVisualLayers().isEmpty()?null:mapLoader.getVisualLayers().get(0);

        for(BufferedImage pic : pics){
            for(int tries=0; tries<4000; tries++){
                int x = rand.nextInt(world.getWidth());
                int y = rand.nextInt(world.getHeight());

                if(base!=null && base[y][x]==0) continue; // needs a painted tile
                if(mapLoader.isNoSpawn(x,y)) continue;
                if(world.isBlocked(x,y)) continue;
                if(x==player.getX() && y==player.getY()) continue;

                if(tooCloseToOtherDrops(x,y,12)) continue;
                if(Math.abs(x-player.getX()) + Math.abs(y-player.getY()) < 10) continue;

                looseShinies.add(new RelicDrop(x,y,pic));
                break;
            }
        }
    }

    private List<BufferedImage> loadRelicPics(){
        List<BufferedImage> pics = new ArrayList<>();
        File dir = new File("relics");
        if(!dir.exists() || !dir.isDirectory()){
            return pics;
        }
        File[] files = dir.listFiles((d,name)-> name.toLowerCase().endsWith(".png"));
        if(files==null) return pics;
        for(File f: files){
            try{
                BufferedImage img = ImageIO.read(f);
                if(img!=null){
                    pics.add(img);
                }
            }catch(Exception ignored){ }
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
            BufferedImage sheet = ImageIO.read(new File(path));
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

        int viewLeft = (int)Math.round(player.getX()-(viewWidthTiles-1)/2.0);
        int viewTop  = (int)Math.round(player.getY()-(viewHeightTiles-1)/2.0);

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
                boolean isPlayerHere=(worldX==player.getX()&&worldY==player.getY());

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

        drawVignette(g2);
        if(corruptionTintActive && !firstRelicCutsceneActive && !secondRelicCutsceneActive){
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

        g2.translate(-shakeX, -shakeY);

        if(gameOver||gameWon){
            drawGameOverOverlay(g2);
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

    private void drawVignette(Graphics2D g2){
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
        g2.setColor(new Color(0,0,0));
        int w = getWidth();
        int h = getHeight();
        int margin = 32;
        g2.fillRect(0,0,w,margin);
        g2.fillRect(0,h-margin,w,margin);
        g2.fillRect(0,0,margin,h);
        g2.fillRect(w-margin,0,margin,h);
        g2.setComposite(old);
    }

    private void drawGameOverOverlay(Graphics2D g2){
        String msg;
        if(gameWon){
            msg = "YOU RESTORED THE WORLD";
        } else {
            msg = "YOU FELL IN THE RUINS";
        }

        g2.setFont(new Font("Consolas",Font.BOLD,26));
        g2.setColor(new Color(255,255,255,230));
        FontMetrics fm = g2.getFontMetrics();
        int tw=fm.stringWidth(msg);
        int th=fm.getAscent();

        int cx=getWidth()/2-tw/2;
        int cy=getHeight()/2-th/2;
        g2.drawString(msg, cx, cy);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        String sub = "Press ESC to quit.";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, getWidth()/2 - sw/2, cy+30);
    }

    private void drawTile(Graphics2D g2,boolean blocked,int px,int py,
                          boolean isPlayerHere,int worldX,int worldY,int tileSize,double corruptionStrength){

        Color bg=new Color(20,25,28);
        g2.setColor(bg);
        g2.fillRect(px,py,tileSize,tileSize);

        if(mapLoader!=null){
            java.util.List<int[][]> layers=mapLoader.getVisualLayers();
            for(int l=0;l<layers.size();l++){
                int[][] layer=layers.get(l);
                int gid=layer[worldY][worldX];
                if(gid<=0) continue;
                java.awt.image.BufferedImage img=mapLoader.getTileImage(gid);
                if(img!=null){
                    g2.drawImage(img,px,py,tileSize,tileSize,null);
                }
            }
        }

        for(RelicDrop drop : looseShinies){
            if(drop.x==worldX && drop.y==worldY && drop.pic!=null){
                int dw=Math.max(18, tileSize-32);
                int dh=Math.max(18, tileSize-32);
                int ox = px + (tileSize - dw)/2;
                int oy = py + (tileSize - dh)/2;

                // subtle halo just on this tile and under fog
                double pulse = Math.sin(System.currentTimeMillis()/780.0)*0.04;
                int halo = (int)(tileSize*(0.92 + pulse));
                int hx = px + (tileSize - halo)/2;
                int hy = py + (tileSize - halo)/2;
                g2.setColor(new Color(255, 230, 180, 52));
                g2.fillOval(hx, hy, halo, halo);
                g2.setColor(new Color(255, 245, 220, 36));
                g2.fillOval(hx+2, hy+2, halo-4, halo-4);

                int bob = (int)(Math.sin(System.currentTimeMillis()/520.0) * 3);
                g2.drawImage(drop.pic, ox, oy + bob, dw, dh, null);
                break;
            }
        }

        if(corruptionStrength>0.01){
            int alpha=(int)Math.min(230,Math.round(230*corruptionStrength));
            g2.setColor(new Color(80,40,120,alpha));
            g2.fillRect(px,py,tileSize,tileSize);
        }

        if(isPlayerHere){
            boolean walking = moving && (System.currentTimeMillis()-lastMoveMs) < 320L;
            boolean hurt = hurtAnimStartMs > 0 && (System.currentTimeMillis()-hurtAnimStartMs) < hurtAnimDurationMs;
            boolean dying = gameOver && deathAnimStartMs > 0;
            BufferedImage frame = pickFace(walking, hurt, dying);
            if(frame!=null){
                int sw = frame.getWidth()*5;
                int sh = frame.getHeight()*5;
                int ox = px + (tileSize - sw)/2;
                int oy = py + (tileSize - sh)/2;
                if(facingRight){
                    g2.drawImage(frame, ox, oy, sw, sh, null);
                } else {
                    g2.drawImage(frame, ox+sw, oy, -sw, sh, null);
                }
            } else {
                g2.setColor(new Color(240,240,255));
                int inset=Math.max(4,tileSize/8);
                g2.fillRect(px+inset,py+inset,tileSize-inset*2,tileSize-inset*2);
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
        final float speedTilesPerSec;
        final long frameTimeMs = 140L;
        float moveBuffer = 0f;
        final MonsterType type;
        boolean facingRight = true;
        Monster(float x,float y,BufferedImage[] frames,float speedTilesPerSec, MonsterType type){
            this.x=x; this.y=y; this.frames=frames; this.speedTilesPerSec=speedTilesPerSec; this.type=type;
        }
        BufferedImage pickFrame(){
            BufferedImage[] active = frames;
            if(active==null || active.length==0) return null;
            long ticks = System.currentTimeMillis() / frameTimeMs;
            int idx = (int)(ticks % active.length);
            return active[idx];
        }
    }

    private void wakeFog() {
        discovered = new boolean[world.getHeight()][world.getWidth()];
        peelFog(player.getX(), player.getY());
    }

    private void peelFog(int cx, int cy){
        if(discovered == null){
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
            if(d.x==player.getX() && d.y==player.getY()){
                looseShinies.remove(i);
                relicBag.stashOne();
                world.setTile(player.getX(),player.getY(),world.baseForRow(player.getY()));
                lastMessage="You found a relic fragment! ("+
                        relicBag.bagCount()+"/"+relicBag.goalCount()+")";
                if(!firstRelicCutsceneStarted){
                    triggerFirstRelicCutscene();
                }
                startCorruptionIfReady();
                maybeTriggerSecondCutscene();
                maybeTriggerThirdCutscene();
                return;
            }
        }
    }

    private void startCorruptionIfReady(){
        if(corruptionStartMs >= 0L) return;
        if(relicBag.bagCount() >= 1){
            corruptionStartMs = System.currentTimeMillis();
            corruptionTintActive = true;
            lastMessage = "A creeping corruption spreads...";
        }
    }

    private void maybeTriggerSecondCutscene(){
        if(secondRelicCutsceneActive || secondRelicCutsceneDone) return;
        if(firstRelicCutsceneActive) return;
        if(relicBag.bagCount() >= 2){
            triggerSecondRelicCutscene();
        }
    }

    private void maybeTriggerThirdCutscene(){
        if(thirdRelicCutsceneActive || thirdRelicCutsceneDone) return;
        if(firstRelicCutsceneActive || secondRelicCutsceneActive) return;
        if(relicBag.bagCount() >= 3){
            triggerThirdRelicCutscene();
        }
    }

    private void loadMonsterSprites(){
        monsterEyeFrames = loadStrip("monsters/OcularWatcher.png");
        monsterJellyFrames = loadStrip("monsters/DeathSlime.png");
    }

    private void updateGame(){
        if(onStartScreen){
            return;
        }
        if(gameOver || gameWon){
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

        if(firstRelicCutsceneDone){
            handleMonsterSpawns();
            updateMonsters();
            checkMonsterContacts();
        }

        applyCorruptionDamage();
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
        lastMessage = "The corruption surges.";
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
            if(x==player.getX() && y==player.getY()) continue;
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
        int px = player.getX();
        int py = player.getY();
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
            int guard = 0;
            while(m.moveBuffer >= 1f && guard < 4){
                guard++;
                int cx = (int)Math.floor(m.x);
                int cy = (int)Math.floor(m.y);
                int dx = Integer.compare(player.getX(), cx);
                int dy = Integer.compare(player.getY(), cy);

                boolean moved = false;
                if(Math.abs(player.getX() - cx) >= Math.abs(player.getY() - cy)){
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
                } else {
                    m.moveBuffer = 0f;
                }
            }
        }
    }

    private float mTickTiles(){
        return tickMs / 1000f;
    }

    private boolean attemptMonsterStep(Monster m, int tx, int ty){
        if(!canWalk(tx, ty)) return false;
        m.x = tx + 0.5f;
        m.y = ty + 0.5f;
        return true;
    }

    private void checkMonsterContacts(){
        int px = player.getX();
        int py = player.getY();
        long now = System.currentTimeMillis();
        for(Monster m: monsters){
            int mx = (int)Math.floor(m.x);
            int my = (int)Math.floor(m.y);
            if(mx==px && my==py){
                if(now - lastDamageMs >= damageCooldownMs){
                    lastDamageMs = now;
                    hurtAnimStartMs = now;
                    int dmg;
                    if(m.type == MonsterType.EYE || m.type == MonsterType.JELLY){
                        dmg = 15;
                    } else {
                        dmg = 10;
                    }
                    player.nickHearts(dmg);
                    knockPlayerFrom(mx,my);
                    lastMessage = "You are struck by a lurking horror!";
                    checkIfDone();
                }
                break;
            }
        }
    }

    private void knockPlayerFrom(int mx,int my){
        int dx = Integer.compare(player.getX(), mx);
        int dy = Integer.compare(player.getY(), my);
        int targetX = player.getX() + dx;
        int targetY = player.getY() + dy;
        if(canWalk(targetX, targetY)){
            player.dropAt(targetX, targetY);
            peelFog(targetX, targetY);
        }
    }

    private void drawMonsters(Graphics2D g2, int viewLeft, int viewTop, int tileSize, int viewWidthTiles, int viewHeightTiles){
        for(Monster m: monsters){
            int gx = (int)Math.floor(m.x);
            int gy = (int)Math.floor(m.y);
            if(gx < viewLeft || gx >= viewLeft + viewWidthTiles) continue;
            if(gy < viewTop || gy >= viewTop + viewHeightTiles) continue;

            double screenX = (m.x - viewLeft) * tileSize;
            double screenY = (m.y - viewTop) * tileSize;

            BufferedImage frame = m.pickFrame();
            if(frame != null){
                double ratio = frame.getHeight() / (double)Math.max(1, frame.getWidth());
                double scale = 0.88;
                int sw = (int)Math.round(tileSize * scale);
                int sh = (int)Math.round(sw * ratio);
                int dx = (int)Math.round(screenX - sw/2.0);
                int dy = topPad + (int)Math.round(screenY - sh/2.0);

                int fog = fogAlphaForTile(gx, gy);
                double corr = moodHaziness(gx, gy);
                float factorFog = (float)(1.0 - Math.min(0.82, fog/255.0 * 0.9));
                float factorCorr = (float)(1.0 - Math.min(0.55, corr * 0.7));
                float factor = Math.max(0f, Math.min(1f, factorFog * factorCorr));

                if(factor < 0.999f){
                    java.awt.image.RescaleOp op = new java.awt.image.RescaleOp(
                            new float[]{factor, factor, factor, 1f},
                            new float[]{0f,0f,0f,0f}, null);
                    BufferedImage scaled = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig = scaled.createGraphics();
                    ig.drawImage(frame, 0, 0, null);
                    ig.dispose();
                    scaled = op.filter(scaled, null);
                    drawFlipped(g2, scaled, dx, dy, sw, sh, m.facingRight);
                } else {
                    drawFlipped(g2, frame, dx, dy, sw, sh, m.facingRight);
                }
            }
        }
    }

    private void drawFlipped(Graphics2D g2, BufferedImage img, int x, int y, int w, int h, boolean facingRight){
        if(facingRight){
            g2.drawImage(img, x, y, w, h, null);
        } else {
            g2.drawImage(img, x + w, y, -w, h, null);
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
        String l1 = "The first relic has been obtained.";
        String l2 = "Something slumbering is stirring...";
        FontMetrics fm = g2.getFontMetrics();
        int cy = h/2 - fm.getHeight();
        int x1 = (w - fm.stringWidth(l1))/2;
        g2.setColor(new Color(235,230,222));
        g2.drawString(l1, x1, cy);
        int x2 = (w - fm.stringWidth(l2))/2;
        g2.drawString(l2, x2, cy + fm.getHeight()+12);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The map will start getting corrupted. Collect all the relics before it's too late.";
        FontMetrics wf = g2.getFontMetrics();
        int wx = (w - wf.stringWidth(warn))/2;
        int wy = h - 70;
        g2.setColor(new Color(210,72,72,240));
        g2.drawString(warn, wx, wy);

        String prompt = firstRelicCutsceneAwaitingContinue ? "Click or press any key to steel yourself." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 18));
        FontMetrics pf = g2.getFontMetrics();
        int px = (w - pf.stringWidth(prompt))/2;
        int py = h - 36;
        g2.setColor(new Color(240,234,224,230));
        g2.drawString(prompt, px, py);
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
        String l1 = "The second relic trembles.";
        String l2 = "Something awakens...";
        FontMetrics fm = g2.getFontMetrics();
        int cy = h/2 - fm.getHeight();
        int x1 = (w - fm.stringWidth(l1))/2;
        g2.setColor(new Color(255,236,228));
        g2.drawString(l1, x1, cy);
        int x2 = (w - fm.stringWidth(l2))/2;
        g2.setColor(new Color(255,90,110));
        g2.drawString(l2, x2, cy + fm.getHeight()+14);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The corruption quickens. It will spread faster now.";
        FontMetrics wf = g2.getFontMetrics();
        int wx = (w - wf.stringWidth(warn))/2;
        int wy = h - 78;
        g2.setColor(new Color(230,70,90,245));
        g2.drawString(warn, wx, wy);

        String prompt = secondRelicCutsceneAwaitingContinue ? "Click or press any key to face it." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 19));
        FontMetrics pf = g2.getFontMetrics();
        int px = (w - pf.stringWidth(prompt))/2;
        int py = h - 40;
        g2.setColor(new Color(240,234,224,235));
        g2.drawString(prompt, px, py);
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
        String l1 = "The third relic shatters the silence.";
        String l2 = "Blades sing in the dark...";
        FontMetrics fm = g2.getFontMetrics();
        int cy = h/2 - fm.getHeight();
        int x1 = (w - fm.stringWidth(l1))/2;
        g2.setColor(new Color(255,240,232));
        g2.drawString(l1, x1, cy);
        int x2 = (w - fm.stringWidth(l2))/2;
        g2.setColor(new Color(255,110,140));
        g2.drawString(l2, x2, cy + fm.getHeight()+16);

        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        String warn = "The corruption peaks. Brace yourself.";
        FontMetrics wf = g2.getFontMetrics();
        int wx = (w - wf.stringWidth(warn))/2;
        int wy = h - 86;
        g2.setColor(new Color(240,70,90,245));
        g2.drawString(warn, wx, wy);

        String prompt = thirdRelicCutsceneAwaitingContinue ? "Click or press any key to brace for the blade." : "...";
        g2.setFont(new Font("Garamond", Font.BOLD, 20));
        FontMetrics pf = g2.getFontMetrics();
        int px = (w - pf.stringWidth(prompt))/2;
        int py = h - 44;
        g2.setColor(new Color(240,234,224,240));
        g2.drawString(prompt, px, py);
    }

    private int fogAlphaForTile(int worldX, int worldY){
        if(!isDiscovered(worldX, worldY)){
            return 235;
        }

        double dist = Math.hypot(worldX - player.getX(), worldY - player.getY());

        if(dist <= clearRing){
            return 0;
        }

        double span = Math.max(1e-3,(fogEdge-clearRing));
        double t = (dist-clearRing)/span;
        if(t > 1.0) t = 1.0;
        if(t < 0.0) t = 0.0;

        double smooth = t*t*(3-2*t);
        int baseAlpha = 18;
        int maxAlpha  = 170;
        return (int)Math.round(baseAlpha + (maxAlpha-baseAlpha) * smooth);
    }

    private double moodHaziness(int worldX,int worldY){
        if(corruptionStartMs < 0L){
            return 0.0;
        }
        long elapsed = System.currentTimeMillis() - corruptionStartMs;
        double creep = Math.min(1.0, elapsed / (double)corruptionSpanMs);

        double upwardBias = 1.0 - (worldY / Math.max(1.0, (world.getHeight()-1)));
        double staticJitter = (Util.scrappyPick(worldX, worldY, 7, 100) - 50) / 520.0; // small, fixed per tile

        double threshold = clamp01(upwardBias + staticJitter + 0.03);

        double spread = 0.45;
        double strength = clamp01((creep - threshold) / spread);
        double eased = strength*strength * (3 - 2*strength);
        return eased;
    }

    private void applyCorruptionDamage(){
        if(corruptionStartMs < 0L) return;
        if(player == null) return;
        if(player.getHearts() <= 0) return;
        if(player.getX() < 0 || player.getY() < 0) return;
        if(!world.inBounds(player.getX(), player.getY())) return;

        double strength = moodHaziness(player.getX(), player.getY());
        boolean inCorruption = strength > 0.05;

        if(!inCorruption){
            corruptionExposureStartMs = -1L;
            corruptionDamageRemainder = 0.0;
            return;
        }

        long now = System.currentTimeMillis();
        if(corruptionExposureStartMs < 0L){
            corruptionExposureStartMs = now;
            corruptionDamageRemainder = 0.0;
            lastMessage = "The corruption crackles—get out now!";
            return;
        }

        long exposureMs = now - corruptionExposureStartMs;
        if(exposureMs < 3000L) return; // grace period

        double seconds = tickMs / 1000.0;
        double dps = 6.0 + (strength * 24.0); // up to ~30 per second at darkest
        corruptionDamageRemainder += dps * seconds;

        int whole = (int)Math.floor(corruptionDamageRemainder);
        if(whole > 0){
            corruptionDamageRemainder -= whole;
            player.nickHearts(whole);
            lastMessage = "The corruption sears you!";
            checkIfDone();
        }
    }

    private double clamp01(double v){
        if(v < 0.0) return 0.0;
        if(v > 1.0) return 1.0;
        return v;
    }
}
