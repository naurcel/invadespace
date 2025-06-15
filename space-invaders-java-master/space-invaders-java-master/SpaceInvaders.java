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
    int shipX = xOffset + tileSize * columns / 2 - tileSize;
    int shipY = yOffset + tileSize * rows - tileSize * 2;
    int shipVelocityX = tileSize;
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
    int score = 0;

    SpaceInvaders() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        backgroundImg = new ImageIcon(getClass().getResource("./background.png")).getImage();
        shipImg = new ImageIcon(getClass().getResource("./ship.png")).getImage();

        alienImg = new ImageIcon(getClass().getResource("./alien.png")).getImage();
        alienCyanImg = new ImageIcon(getClass().getResource("./alien-cyan.png")).getImage();
        alienMagentaImg = new ImageIcon(getClass().getResource("./alien-magenta.png")).getImage();
        alienYellowImg = new ImageIcon(getClass().getResource("alien-yellow.png")).getImage();

        alienImgArray = new ArrayList<>();
        alienImgArray.add(alienImg);
        alienImgArray.add(alienCyanImg);
        alienImgArray.add(alienMagentaImg);
        alienImgArray.add(alienYellowImg);

        ship = new Block(shipX, shipY, shipWidth, shipHeight, shipImg);
        alienArray = new ArrayList<>();
        bulletArray = new ArrayList<>();

        gameLoop = new Timer(1000 / 60, this); // 60 FPS
        createAliens();
        gameLoop.start();
    }

    public void paintComponent(Graphics g) {
        int margin = 40;
        int gameAreaWidth = tileSize * columns;
        int gameAreaHeight = boardHeight - margin * 2;

        int xOffset = (boardWidth - gameAreaWidth) / 2;
        int yOffset = margin;

        super.paintComponent(g);
        draw(g);
        Graphics2D g2d = (Graphics2D) g;

        // Set border color
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(xOffset, yOffset, gameAreaWidth, gameAreaHeight);

        // Outer black border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRect(xOffset - 2, yOffset - 2, gameAreaWidth + 4, gameAreaHeight + 4);

        // Inner white border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(xOffset, yOffset, gameAreaWidth, gameAreaHeight);
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

        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + score, 10, 35);
        } else {
            g.drawString("Score: " + score, 10, 35);
        }
    }

    public void moveAliens() {
        boolean reverse = false;

        Random rand = new Random();
        for (Block alien : alienArray) {
            if (alien.alive && rand.nextDouble() < 0.05) { // 5% chance per frame per alien
                Block bullet = new Block(alien.x + alien.width / 2, alien.y + alien.height, bulletWidth, bulletHeight, null);
                alienBulletArray.add(bullet);
            }
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
            System.out.println("Ship was hit!"); // Debug
            gameOver = true;
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
        score += alienColumns * alienRows * 100;
        alienColumns = Math.min(alienColumns + 1, columns / 2 - 2);
        alienRows = Math.min(alienRows + 1, rows - 6);
        alienArray.clear();
        bulletArray.clear();
        createAliens();
    }
}
    
    public void move() {
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
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            ship.x = shipX;
            bulletArray.clear();
            alienArray.clear();
            gameOver = false;
            score = 0;
            alienColumns = 3;
            alienRows = 2;
            alienVelocityX = tileSize;
            alienMoveTimer = 0;
            createAliens();
            gameLoop.start();
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && ship.x - shipVelocityX >= xOffset) {
            ship.x -= shipVelocityX;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && ship.x + shipVelocityX + ship.width <= xOffset + gameAreaWidth) {
            ship.x += shipVelocityX;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            Block bullet = new Block(ship.x + shipWidth * 15 / 32, ship.y, bulletWidth, bulletHeight, null);
            bulletArray.add(bullet);
        }
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
    }
}