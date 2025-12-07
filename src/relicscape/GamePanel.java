package relicscape;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel implements KeyListener {

    private final int tileSizeHint = 96;
    private final int hudBarHeight = 80;
    private final int tickMs = 33;
    private final double clearRing = 1.0;
    private final double revealRing = 4.5;
    private final double fogEdge = 11.5;
    private final int topPad = 30;
    private final long corruptionSpanMs = 10 * 60 * 1000L;

    private final World world;
    private TMXMapLoader mapLoader;
    private final Player player;
    private final RelicManager relicBag;
    private long lastMoveMs=0L;
    private static final long MOVE_GAP_MS = 200;

    private String lastMessage = "Explore the world. Find 3 relic fragments and return to the central shrine.";
    private boolean gameOver=false;
    private boolean gameWon = false;
    private boolean facingRight = true;
    private boolean moving=false;
    private boolean onStartScreen=true;

    private final long corruptionStartMs = System.currentTimeMillis();

    private boolean[][] discovered;
    private BufferedImage[] soldierWalkFrames;
    private BufferedImage[] soldierIdleFrames;
    private final StartScreenRenderer startScreen;

    private final Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 900));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

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
        player = new Player(spawnX, spawnY, 10);
        wakeFog();
        loadPlayerLook();

        startScreen = new StartScreenRenderer();

        timer = new Timer(tickMs, e->repaint());
        timer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(onStartScreen){
            onStartScreen=false;
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

    private void nudgePlayer(int dx, int dy) {
        if(onStartScreen){
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
        feelTile(world.getTile(newX,newY));
        checkIfDone();
    }

    private void feelTile(TileType tile) {
        if(tile==TileType.RELIC){
            relicBag.stashOne();
            world.setTile(player.getX(),player.getY(),world.baseForRow(player.getY()));
            lastMessage="You found a relic fragment! ("+
                    relicBag.bagCount()+"/"+relicBag.goalCount()+")";
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

        drawVignette(g2);
        paintHud(g2, tileSize, viewHeightTiles);
        paintWhisper(g2);

        if(onStartScreen){
            startScreen.draw(g2, getWidth(), getHeight());
            return;
        }

        if(gameOver||gameWon){
            drawGameOverOverlay(g2);
        }
    }

    private void drawStartScreen(Graphics2D g2){
        startScreen.draw(g2, getWidth(), getHeight());
    }

    private void paintHud(Graphics2D g2,int tileSize,int viewHeightTiles){
        int hudY = topPad + viewHeightTiles*tileSize + 30;

        int pad = 20;
        int boxH = 72;
        GradientPaint gp = new GradientPaint(0, hudY-24, new Color(26,26,28,230), 0, hudY+boxH, new Color(12,12,14,230));
        g2.setPaint(gp);
        g2.fillRoundRect(pad, hudY-24, getWidth()-pad*2, boxH, 14, 14);
        g2.setColor(new Color(160,150,130,140));
        g2.drawRoundRect(pad, hudY-24, getWidth()-pad*2, boxH, 14, 14);

        g2.setColor(new Color(236,234,228));
        g2.setFont(new Font("Consolas",Font.BOLD,15));
          g2.drawString("HP: " + player.getHearts() +
              "   Relics: " + relicBag.bagCount() + "/" +
              relicBag.goalCount(), pad+10, hudY);

        g2.setFont(new Font("Consolas",Font.PLAIN,13));
        g2.setColor(new Color(210,208,200));
        Util.drawWrappedText(g2,lastMessage,pad+10,hudY+20,getWidth()-pad*2-20);
    }

    private void paintWhisper(Graphics2D g2){
        int h = 50;
        int y = getHeight()-h-16;
        GradientPaint gp = new GradientPaint(0,y,new Color(18,18,20,230),0,y+h,new Color(8,8,10,230));
        g2.setPaint(gp);
        g2.fillRoundRect(14, y, getWidth()-28, h, 12, 12);
        g2.setColor(new Color(150,140,120,150));
        g2.drawRoundRect(14, y, getWidth()-28, h, 12, 12);
        g2.setColor(new Color(220,218,210));
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        int tx = 24;
        int ty = y + (h+fm.getAscent())/2 - 3;
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

        if(corruptionStrength>0.01){
            int alpha=(int)Math.min(230,Math.round(230*corruptionStrength));
            g2.setColor(new Color(80,40,120,alpha));
            g2.fillRect(px,py,tileSize,tileSize);
        }

        if(isPlayerHere){
            boolean walking = moving && (System.currentTimeMillis()-lastMoveMs) < 320L;
            BufferedImage frame = pickFace(walking);
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

    private BufferedImage pickFace(boolean walking){
        BufferedImage[] frames = (walking && soldierWalkFrames != null)
                ? soldierWalkFrames
                : soldierIdleFrames;
        if(frames == null || frames.length == 0) return null;

        long frameTime = walking ? 120L : 220L;
        long ticks = System.currentTimeMillis() / frameTime;
        int idx = (int)(ticks % frames.length);
        return frames[idx];
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
        long elapsed=System.currentTimeMillis()-corruptionStartMs;
        double duskProgress=Math.min(1.0,elapsed/(double)corruptionSpanMs);

        double latitudePull = 1.0-(worldY/Math.max(1.0, (world.getHeight()-1)));
        double drift = Math.sin((elapsed/1200.0) + worldX*0.35 + worldY*0.27) * 0.12;
        double pulse = Math.sin(elapsed/4200.0 + (worldX+worldY)*0.08) * 0.08;

        double threshold = clamp01(latitudePull + drift + pulse + 0.05);

        double spread = 0.35;
        double strength = clamp01((duskProgress-threshold)/spread);
        double eased = strength*strength * (3 - 2*strength);
        return eased;
    }

    private double clamp01(double v){
        if(v < 0.0) return 0.0;
        if(v > 1.0) return 1.0;
        return v;
    }
}
