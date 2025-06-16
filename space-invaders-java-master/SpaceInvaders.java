import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class SpaceInvaders extends JPanel implements ActionListener, KeyListener {
    // Tile and board settings
    int tileSize = 32;
    int rows = 20;
    int columns = 16;

    int gameAreaWidth = tileSize * columns;  // 512
    int gameAreaHeight = 512;    // 512

    int boardWidth = 1366;
    int boardHeight = 768;

    // Center offset to draw the game in the center
    int xOffset = (boardWidth - gameAreaWidth) / 2;
    int yOffset = 80;

    Image backgroundImg;
    Image shipImg;
    Image alienImg;
    Image alienCyanImg;
    Image alienMagentaImg;
    Image alienYellowImg;
    Image playerBulletImg;
    Image alienBulletImg;
    ArrayList<Image> alienImgArray;
    ArrayList<Block> alienBulletArray = new ArrayList<>();
    int alienBulletVelocityY = 5;

    class Block {
        int x, y, width, height;
        Image img;
        boolean alive = true;
        boolean used = false;

        Block(int x, int y, int width, int height, Image img) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.img = img;
        }
    }

    // Ship
    int shipWidth = tileSize * 2;
    int shipHeight = tileSize;
    double shipX = xOffset + tileSize * columns / 2.0 - tileSize;
    int shipY = yOffset + tileSize * rows - tileSize * 2;
    double shipVelocityX = 4.0; // Lower value for smoother movement
    double shipSpeed = 0;
    double shipAcceleration = 0.7;
    double shipFriction = 0.85;
    Block ship;

    // Aliens
    ArrayList<Block> alienArray;
    int alienWidth = tileSize * 2;
    int alienHeight = tileSize;
    int alienX = xOffset + tileSize;
    int alienY = yOffset + tileSize;
    int alienRows = 2;
    int alienColumns = 3;
    int alienCount = 0;
    int alienVelocityX = tileSize; // Move by tile for choppy effect

    // Choppy movement timer
    int alienMoveTimer = 0;
    int alienMoveDelay = 30; // Number of game ticks before aliens move

    // Bullets
    ArrayList<Block> bulletArray;
    int bulletWidth = tileSize / 8;
    int bulletHeight = tileSize / 2;
    int bulletVelocityY = -5;

    Timer gameLoop;
    boolean gameOver = false;
    boolean menuActive = false;
    boolean startMenu = true;
    boolean paused = false;
    int score = 0;
    long lastBulletTime = 0;
    long bulletCooldown = 250; // milliseconds
    int wave = 1;

    boolean leftPressed = false;
    boolean rightPressed = false;

    SpaceInvaders() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        backgroundImg = new ImageIcon(getClass().getResource("assets/models/background.png")).getImage();
        shipImg = new ImageIcon(getClass().getResource("assets/models/ship.png")).getImage();

        alienImg = new ImageIcon(getClass().getResource("assets/models/alien.png")).getImage();
        alienCyanImg = new ImageIcon(getClass().getResource("assets/models/alien-cyan.png")).getImage();
        alienMagentaImg = new ImageIcon(getClass().getResource("assets/models/alien-magenta.png")).getImage();
        alienYellowImg = new ImageIcon(getClass().getResource("assets/models/alien-yellow.png")).getImage();

        // Audio files are now in assets/audio/
        // ...existing code...

        alienImgArray = new ArrayList<>();
        alienImgArray.add(alienImg);
        alienImgArray.add(alienCyanImg);
        alienImgArray.add(alienMagentaImg);
        alienImgArray.add(alienYellowImg);

        ship = new Block((int)shipX, shipY, shipWidth, shipHeight, shipImg);
        alienArray = new ArrayList<>();
        bulletArray = new ArrayList<>();

        gameLoop = new Timer(1000 / 60, this); // 60 FPS
        createAliens();
        gameLoop.stop(); // Stop the game loop on launch
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);
        g.drawImage(ship.img, ship.x, ship.y, ship.width, ship.height, null);

        for (Block alien : alienArray) {
            if (alien.alive) {
                g.drawImage(alien.img, alien.x, alien.y, alien.width, alien.height, null);
            }
        }

        g.setColor(Color.RED);
        for (Block bullet : alienBulletArray) {
            if (!bullet.used) {
                g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }   
        }

        g.setColor(Color.white);
        for (Block bullet : bulletArray) {
            if (!bullet.used) {
                g.drawRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }
        }

        // Always show score and wave at the top left
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g.drawString("Wave: " + wave, 10, 70);
        g.drawString("Score: " + score, 10, 35);

        if (startMenu) {
            g.setColor(new Color(0,0,0,220));
            int overlayW = 500;
            int overlayH = 250;
            int overlayX = (getWidth() - overlayW) / 2;
            int overlayY = (getHeight() - overlayH) / 2;
            g.fillRect(overlayX, overlayY, overlayW, overlayH);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String title = "SPACE INVADERS";
            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, getWidth()/2 - titleWidth/2, getHeight()/2 - 40);
            g.setFont(new Font("Arial", Font.PLAIN, 32));
            String startText = "Press ENTER to Start";
            int startWidth = g.getFontMetrics().stringWidth(startText);
            g.drawString(startText, getWidth()/2 - startWidth/2, getHeight()/2 + 30);
            return;
        }

        if (paused) {
            g.setColor(new Color(0,0,0,180));
            int overlayW = 400;
            int overlayH = 160;
            int overlayX = (getWidth() - overlayW) / 2;
            int overlayY = (getHeight() - overlayH) / 2;
            g.fillRect(overlayX, overlayY, overlayW, overlayH);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String pauseText = "PAUSED";
            int pauseWidth = g.getFontMetrics().stringWidth(pauseText);
            g.drawString(pauseText, getWidth()/2 - pauseWidth/2, getHeight()/2 + 10);
            g.setFont(new Font("Arial", Font.PLAIN, 28));
            String resumeText = "Press ESC to Resume";
            int resumeWidth = g.getFontMetrics().stringWidth(resumeText);
            g.drawString(resumeText, getWidth()/2 - resumeWidth/2, getHeight()/2 + 60);
            return;
        }

        if (gameOver || menuActive) {
            // Draw menu overlay on top of everything
            g.setColor(new Color(0,0,0,220));
            int overlayW = 440;
            int overlayH = 220;
            int overlayX = (getWidth() - overlayW) / 2;
            int overlayY = (getHeight() - overlayH) / 2;
            g.fillRect(overlayX, overlayY, overlayW, overlayH);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String gameOverText = "GAME OVER";
            int textWidth = g.getFontMetrics().stringWidth(gameOverText);
            int textX = getWidth()/2 - textWidth/2;
            int textY = getHeight()/2 - 20;
            g.drawString(gameOverText, textX, textY);
            g.setFont(new Font("Arial", Font.PLAIN, 32));
            String restartText = "Press ENTER to Restart";
            int restartWidth = g.getFontMetrics().stringWidth(restartText);
            int restartX = getWidth()/2 - restartWidth/2;
            int restartY = getHeight()/2 + 50;
            g.drawString(restartText, restartX, restartY);
        }
    }

    public void moveAliens() {
        boolean reverse = false;

        Random rand = new Random();
        for (Block alien : alienArray) {
            if (alien.alive && rand.nextDouble() < 0.05) // 5% chance per frame per alien
                alienBulletArray.add(new Block(alien.x + alien.width / 2, alien.y + alien.height, bulletWidth, bulletHeight, null));
        }

        for (Block alien : alienArray) {
            if (alien.alive) {
                alien.x += alienVelocityX;
                if (alien.x + alien.width >= xOffset + gameAreaWidth || alien.x <= xOffset) {
                    reverse = true;
                }
            }
        }

        if (reverse) {
            alienVelocityX *= -1;
            for (Block alien : alienArray) {
                alien.y += alienHeight;
            }
        }

        for (Block alien : alienArray) {
            if (alien.alive && alien.y >= ship.y) {
                gameOver = true;
                return;
            }
        }
    }

    public void moveBullets() {
        // Move enemy bullets
        for (Block bullet : alienBulletArray) {
            bullet.y += alienBulletVelocityY;
            if (!bullet.used && detectCollision(bullet, ship)) {
                System.out.println("Ship was hit!");
                gameOver = true;
                menuActive = true;
                bullet.used = true;
            }
        }

        // Remove off-screen or used bullets
        alienBulletArray.removeIf(b -> b.used || b.y > ship.y + ship.height);

        // Move player bullets
        for (Block bullet : bulletArray) {
            bullet.y += bulletVelocityY;

            for (Block alien : alienArray) {
                if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                    bullet.used = true;
                    alien.alive = false;
                    alienCount--;
                    score += 100;
                }
            }
        }

        // Remove off-screen or used player bullets
        bulletArray.removeIf(b -> b.used || b.y < yOffset);

        // Respawn aliens if all are dead
        if (alienCount == 0) {
            wave++;
            score += alienColumns * alienRows * 100;
            // Increase difficulty with each wave
            alienColumns = Math.min(alienColumns + 1, columns / 2 - 2 + wave / 2);
            alienRows = Math.min(alienRows + 1, rows - 6 + wave / 3);
            alienMoveDelay = Math.max(10, alienMoveDelay - 2); // Aliens move faster
            alienBulletVelocityY = Math.min(alienBulletVelocityY + 1, 15); // Enemy bullets faster
            bulletVelocityY = Math.max(bulletVelocityY - 1, -15); // Player bullets faster
            alienArray.clear();
            bulletArray.clear();
            createAliens();
        }
    }
    
    public void move() {
        // Block all game logic if in menu, pause, or game over
        if (startMenu || paused || gameOver || menuActive) return;
        if (leftPressed) shipSpeed -= shipAcceleration;
        if (rightPressed) shipSpeed += shipAcceleration;
        shipSpeed *= shipFriction;
        shipSpeed = Math.max(Math.min(shipSpeed, shipVelocityX), -shipVelocityX);
        shipX += shipSpeed;
        if (shipX < xOffset) { shipX = xOffset; shipSpeed = 0; }
        if (shipX + shipWidth > xOffset + gameAreaWidth) { shipX = xOffset + gameAreaWidth - shipWidth; shipSpeed = 0; }
        ship.x = (int)shipX;

        // Aliens move every alienMoveDelay frames
        alienMoveTimer++;
        if (alienMoveTimer >= alienMoveDelay) {
            moveAliens();
            alienMoveTimer = 0;
        }

        // Bullets move every frame
        moveBullets();
    }

    public void createAliens() {
        Random rand = new Random();
        for (int c = 0; c < alienColumns; c++) {
            for (int r = 0; r < alienRows; r++) {
                int index = rand.nextInt(alienImgArray.size());
                Block alien = new Block(
                    alienX + c * alienWidth,
                    alienY + r * alienHeight,
                    alienWidth,
                    alienHeight,
                    alienImgArray.get(index)
                );
                alienArray.add(alien);
            }
        }
        alienCount = alienArray.size();
    }

    public boolean detectCollision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x &&
            a.y < b.y + b.height && a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Only run game logic if not in menu, pause, or game over
        if (!startMenu && !paused && !gameOver && !menuActive) {
            move();
        }
        repaint();
        // Only stop the game loop if game over
        if (gameOver && gameLoop.isRunning()) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Start menu logic
        if (startMenu && e.getKeyCode() == KeyEvent.VK_ENTER) {
            startMenu = false;
            gameOver = false;
            menuActive = false;
            paused = false;
            score = 0;
            wave = 1;
            alienColumns = 3;
            alienRows = 2;
            alienVelocityX = tileSize;
            alienMoveTimer = 0;
            alienMoveDelay = 30;
            alienBulletVelocityY = 5;
            bulletVelocityY = -5;
            shipX = xOffset + tileSize * columns / 2.0 - tileSize;
            ship.x = (int)shipX;
            bulletArray.clear();
            alienBulletArray.clear();
            alienArray.clear();
            createAliens();
            if (!gameLoop.isRunning()) gameLoop.start();
            repaint();
            return;
        }
        // Pause logic
        if (!startMenu && !gameOver && !menuActive && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            paused = !paused;
            if (paused) {
                if (gameLoop.isRunning()) gameLoop.stop();
            } else {
                if (!gameLoop.isRunning()) gameLoop.start();
            }
            repaint();
            return;
        }
        // Game over menu logic
        if ((gameOver || menuActive) && e.getKeyCode() == KeyEvent.VK_ENTER) {
            shipX = xOffset + tileSize * columns / 2.0 - tileSize;
            ship.x = (int)shipX;
            bulletArray.clear();
            alienBulletArray.clear();
            alienArray.clear();
            gameOver = false;
            menuActive = false;
            score = 0;
            wave = 1;
            alienColumns = 3;
            alienRows = 2;
            alienVelocityX = tileSize;
            alienMoveTimer = 0;
            alienMoveDelay = 30;
            alienBulletVelocityY = 5;
            bulletVelocityY = -5;
            createAliens();
            if (!gameLoop.isRunning()) gameLoop.start();
            repaint();
            return;
        }
        // Only allow movement if not in menu, pause, or game over
        if (!(gameOver || menuActive || startMenu || paused)) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!(gameOver || menuActive || startMenu || paused)) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                long now = System.currentTimeMillis();
                if (now - lastBulletTime >= bulletCooldown) {
                    Block bullet = new Block(ship.x + shipWidth * 15 / 32, ship.y, bulletWidth, bulletHeight, null);
                    bulletArray.add(bullet);
                    lastBulletTime = now;
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Required by KeyListener, but not used
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        SpaceInvaders gamePanel = new SpaceInvaders();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(gamePanel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // Always start with menu, game loop stopped
        if (gamePanel.gameLoop.isRunning()) gamePanel.gameLoop.stop();
        gamePanel.repaint();
    }
}