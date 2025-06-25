import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The main class for the Space Invaders game, containing all other necessary classes as inner or nested classes.
 * It acts as the main window (JFrame) and manages the different game screens (panels)
 * using a CardLayout. It also orchestrates the transitions between game states.
 * ---
 * La classe principale du jeu Space Invaders, contenant toutes les autres classes nécessaires en tant que classes internes ou imbriquées.
 * Elle agit comme la fenêtre principale (JFrame) et gère les différents écrans de jeu (panneaux)
 * à l'aide d'un CardLayout. Elle orchestre également les transitions entre les états du jeu.
 */
public class SpaceInvadersGame extends JFrame {

    // A layout manager that allows switching between different panels.
    // Un gestionnaire de layout qui permet de basculer entre différents panneaux.
    private final CardLayout cardLayout;

    // The main panel that holds all other panels (screens).
    // Le panneau principal qui contient tous les autres panneaux (écrans).
    private final JPanel mainPanel;

    private final GamePanel gamePanel;
    private final GameMenu mainMenu;
    private final GameMenu pauseMenu;
    private final GameMenu highscoreMenu;
    private final JPanel instructionsPanel;
    private final JPanel aboutPanel;

    private final HighScoreManager highScoreManager;

    /**
     * Constructor for the SpaceInvadersGame.
     * Initializes the main window, loads assets, sets up all the panels (menus, game screen),
     * and displays the main menu.
     * ---
     * Constructeur pour SpaceInvadersGame.
     * Initialise la fenêtre principale, charge les ressources, met en place tous les panneaux (menus, écran de jeu),
     * et affiche le menu principal.
     */
    public SpaceInvadersGame() {
        setTitle("Space Invaders - All-In-One Edition");
        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window on screen. / Centrer la fenêtre.
        setResizable(false);

        // Load all assets (images, sounds, fonts) before starting the UI.
        // Charger toutes les ressources (images, sons, polices) avant de lancer l'interface.
        AssetLoader.load();
        SoundManager.init();

        highScoreManager = new HighScoreManager(GameConstants.HIGHSCORE_FILE);
        try {
            highScoreManager.loadHighScores();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading high scores: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Could not load high scores file. It might be corrupted or missing.\n" +
                "A new one will be created.",
                "High Score Error",
                JOptionPane.WARNING_MESSAGE);
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // --- Create all game panels ---
        mainMenu = new GameMenu("SPACE INVADERS", new String[]{
                "New Game", "High Scores", "Instructions", "About", "Quit"
        }, GameState.MAIN_MENU);

        gamePanel = new GamePanel();

        pauseMenu = new GameMenu("Game Paused", new String[]{
                "Resume", "Main Menu", "Quit"
        }, GameState.PAUSE_MENU);

        highscoreMenu = new GameMenu("High Scores", new String[]{}, GameState.HIGHSCORE_MENU);

        instructionsPanel = createInfoPanel("Instructions",
                "<html><center>MOVE the ship with LEFT/RIGHT ARROWS.<br>" +
                        "SHOOT with the SPACE key.<br>" +
                        "PAUSE with ESCAPE or P.<br><br>" +
                        "Destroy all alien waves to score points and climb the leaderboard.<br>" +
                        "Collect <font color='CYAN'>power-ups</font> to enhance your ship!</center></html>"
        );

        aboutPanel = createInfoPanel("About",
                "<html><center>Space Invaders - All-In-One Edition<br><br>" +
                        "Original Concept by Samyn-Antoy ABASSE<br>" +
                        "Refactoring & Design by Gemini AI<br><br>" +
                        "Version 3.1 - Powered by Java Swing</center></html>"
        );

        // --- Add panels to the CardLayout ---
        mainPanel.add(mainMenu, "MAIN_MENU");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(pauseMenu, "PAUSE_MENU");
        mainPanel.add(highscoreMenu, "HIGHSCORE_MENU");
        mainPanel.add(instructionsPanel, "INSTRUCTIONS");
        mainPanel.add(aboutPanel, "ABOUT");

        add(mainPanel);

        // Add a listener to save scores when the window is closed.
        // Ajouter un écouteur pour sauvegarder les scores à la fermeture de la fenêtre.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveScoresOnExit();
            }
        });

        setVisible(true);
        showMainMenu();
    }
    
    /**
     * Creates a generic panel for displaying information like "Instructions" or "About".
     * ---
     * Crée un panneau générique pour afficher des informations comme "Instructions" ou "À Propos".
     *
     * @param title The title to display at the top. / Le titre à afficher en haut.
     * @param text  The HTML-formatted text to display in the center. / Le texte formaté en HTML à afficher au centre.
     * @return A fully configured JPanel. / Un JPanel entièrement configuré.
     */
    private JPanel createInfoPanel(String title, String text) {
        JPanel infoPanel = new JPanel(new BorderLayout()) {
             @Override
             protected void paintComponent(Graphics g) {
                 super.paintComponent(g);
                 g.setColor(Color.BLACK);
                 g.fillRect(0, 0, getWidth(), getHeight());
                 if (mainMenu != null) mainMenu.drawAnimatedBackground(g, this);
             }
        };
        infoPanel.setBackground(Color.BLACK);

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(AssetLoader.fontTitle != null ? AssetLoader.fontTitle.deriveFont(48f) : new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 20, 20, 20));
        infoPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel textLabel = new JLabel(text, JLabel.CENTER);
        textLabel.setFont(AssetLoader.fontUI != null ? AssetLoader.fontUI.deriveFont(20f) : new Font("Arial", Font.PLAIN, 20));
        textLabel.setForeground(Color.LIGHT_GRAY);
        infoPanel.add(textLabel, BorderLayout.CENTER);

        StyledButton backButton = new StyledButton("Back");
        backButton.addActionListener(e -> showMainMenu());

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setOpaque(false);
        southPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 40, 20));
        southPanel.add(backButton);
        infoPanel.add(southPanel, BorderLayout.SOUTH);

        return infoPanel;
    }

    // ===================================================================================
    // SECTION: Game State Management Methods / Méthodes de Gestion de l'État du Jeu
    // ===================================================================================

    public void showMainMenu() {
        SoundManager.playMusic(SoundManager.menuMusic, true);
        cardLayout.show(mainPanel, "MAIN_MENU");
        gamePanel.setGameState(GameState.MAIN_MENU);
        gamePanel.stopGame();
        mainMenu.requestFocusInWindow();
    }

    public void startGame() {
        SoundManager.stopMusic();
        SoundManager.playMusic(SoundManager.gameMusic, true);
        cardLayout.show(mainPanel, "GAME");
        gamePanel.setGameState(GameState.PLAYING);
        gamePanel.resetGame();
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    public void showPauseMenu() {
        SoundManager.pauseMusic();
        gamePanel.setGameState(GameState.PAUSE_MENU);
        cardLayout.show(mainPanel, "PAUSE_MENU");
        pauseMenu.requestFocusInWindow();
    }

    public void resumeGame() {
        SoundManager.resumeMusic();
        cardLayout.show(mainPanel, "GAME");
        gamePanel.setGameState(GameState.PLAYING);
        gamePanel.requestFocusInWindow();
    }

    public void showHighScores() {
        highscoreMenu.updateHighScoresDisplay(highScoreManager.getHighScores());
        cardLayout.show(mainPanel, "HIGHSCORE_MENU");
        highscoreMenu.requestFocusInWindow();
    }

    public void showInstructions() {
        cardLayout.show(mainPanel, "INSTRUCTIONS");
    }

    public void showAbout() {
        cardLayout.show(mainPanel, "ABOUT");
    }

    public void gameOver(int score) {
        SoundManager.stopMusic();
        SoundManager.playSound(SoundManager.gameOverSound);
        
        String playerName = JOptionPane.showInputDialog(this, "Game Over! Your score: " + score + "\nEnter your name:", "Save Score", JOptionPane.PLAIN_MESSAGE);
        if (playerName != null && !playerName.trim().isEmpty()) {
            highScoreManager.addHighScore(playerName.trim(), score);
            try {
                highScoreManager.saveHighScores();
            } catch (IOException e) {
                System.err.println("Error saving score: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Could not save the high score.", "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        showMainMenu();
    }

    public void saveScoresOnExit() {
        try {
            highScoreManager.saveHighScores();
        } catch (IOException ioException) {
            System.err.println("Error saving high scores on exit: " + ioException.getMessage());
        }
    }
    
    public HighScoreManager getHighScoreManager() {
        return highScoreManager;
    }

    // ===================================================================================
    // SECTION: Main Entry Point / Point d'Entrée Principal
    // ===================================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpaceInvadersGame::new);
    }

    // ===================================================================================
    // SECTION: Constants and Enums / Constantes et Énumérations
    // ===================================================================================

    /**
     * A centralized interface for all game constants.
     * ---
     * Une interface centralisée pour toutes les constantes du jeu.
     */
    public interface GameConstants {
        int WINDOW_WIDTH = 800;
        int WINDOW_HEIGHT = 600;
        int PLAYER_SPEED = 6;
        int PLAYER_WIDTH = 50;
        int PLAYER_HEIGHT = 50;
        long PLAYER_FIRE_RATE_MS = 500;
        long PLAYER_RAPID_FIRE_RATE_MS = 150;
        int PLAYER_LASER_SPEED = 10;
        int ALIEN_DROP_SPEED = 15;
        int ALIEN_LASER_SPEED = 5;
        int BACKGROUND_STAR_COUNT = 200;
        long POWERUP_DURATION_MS = 10000;
        int POWERUP_DROP_CHANCE = 15;
        int EXPLOSION_PARTICLE_COUNT = 30;
        int INITIAL_LIVES = 3;
        String HIGHSCORE_FILE = "highscores.dat";
    }

    /**
     * Represents the different states the game can be in.
     * ---
     * Représente les différents états dans lesquels le jeu peut se trouver.
     */
    public enum GameState {
        MAIN_MENU, PLAYING, PAUSE_MENU, HIGHSCORE_MENU, INSTRUCTIONS, ABOUT
    }
    
    // ===================================================================================
    // SECTION: Static Utility Classes / Classes Utilitaires Statiques
    // ===================================================================================

    /**
     * A static utility class for loading all image and font assets.
     * It handles potential loading errors gracefully.
     * ---
     * Une classe utilitaire statique pour charger toutes les ressources d'images et de polices.
     * Elle gère les erreurs de chargement potentielles de manière robuste.
     */
    public static class AssetLoader {
        public static Image playerShip, alien1, alien2, alien3, laserPlayer, laserAlien, powerUpShield, powerUpRapid;
        public static Font fontTitle, fontUI;

        public static void load() {
            playerShip = loadImage("/resources/playerShip.png");
            alien1 = loadImage("/resources/alien1.png");
            alien2 = loadImage("/resources/alien2.png");
            alien3 = loadImage("/resources/alien3.png");
            laserPlayer = loadImage("/resources/laser_player.png");
            laserAlien = loadImage("/resources/laser_alien.png");
            powerUpShield = loadImage("/resources/powerup_shield.png");
            powerUpRapid = loadImage("/resources/powerup_rapid.png");
            fontTitle = loadFont("/resources/kenvector_future.ttf", 64f);
            fontUI = loadFont("/resources/kenvector_future.ttf", 24f);
        }

        private static Image loadImage(String path) {
            try {
                URL url = SpaceInvadersGame.class.getResource(path);
                if (url == null) {
                    System.err.println("Resource file not found: " + path);
                    return null;
                }
                return ImageIO.read(url);
            } catch (IOException e) {
                System.err.println("Failed to load image: " + path);
                return null;
            }
        }

        private static Font loadFont(String path, float size) {
            try (InputStream is = SpaceInvadersGame.class.getResourceAsStream(path)) {
                if(is == null) {
                    System.err.println("Font file not found: " + path + ". Using default font.");
                    return new Font("Arial", Font.BOLD, (int)size);
                }
                return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
            } catch (Exception e) {
                System.err.println("Failed to load font: " + path + ". Using default font. Error: " + e.getMessage());
                return new Font("Arial", Font.BOLD, (int)size);
            }
        }
    }

    /**
     * A static utility class for loading and managing all sound and music clips.
     * It handles potential loading errors gracefully.
     * ---
     * Une classe utilitaire statique pour charger et gérer tous les clips sonores et musicaux.
     * Elle gère les erreurs de chargement potentielles de manière robuste.
     */
    public static class SoundManager {
        public static Clip playerShootSound, explosionSound, hitSound, playerHitSound, powerupSound, clickSound, gameOverSound, alienShootSound;
        public static Clip menuMusic, gameMusic;
        
        public static void init() {
            playerShootSound = loadSound("/resources/laserShoot.wav");
            explosionSound = loadSound("/resources/explosion.wav");
            hitSound = loadSound("/resources/hit.wav");
            playerHitSound = loadSound("/resources/player_hit.wav");
            powerupSound = loadSound("/resources/powerup.wav");
            clickSound = loadSound("/resources/click.wav");
            gameOverSound = loadSound("/resources/gameover.wav");
            alienShootSound = loadSound("/resources/alien_shoot.wav");
            menuMusic = loadSound("/resources/menu_music.wav");
            gameMusic = loadSound("/resources/game_music.wav");
        }
        
        private static Clip loadSound(String path) {
            try {
                URL url = SoundManager.class.getResource(path);
                 if (url == null) {
                    System.err.println("Sound file not found: " + path);
                    return null;
                }
                try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(url)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    return clip;
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("Failed to load sound: " + path + " - " + e.getMessage());
                return null;
            }
        }

        public static void playSound(Clip clip) {
            if (clip != null) {
                clip.setFramePosition(0);
                clip.start();
            }
        }
        
        public static void playMusic(Clip clip, boolean loop) {
            if (clip != null) {
                stopMusic(); // Stop any other music first
                clip.setFramePosition(0);
                if(loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
                else clip.start();
            }
        }
        
        public static void stopMusic() {
            if (menuMusic != null && menuMusic.isRunning()) menuMusic.stop();
            if (gameMusic != null && gameMusic.isRunning()) gameMusic.stop();
        }

        public static void pauseMusic() {
             if (gameMusic != null && gameMusic.isRunning()) gameMusic.stop();
        }

        public static void resumeMusic() {
            if (gameMusic != null && !gameMusic.isRunning()) playMusic(gameMusic, true);
        }
    }

    /**
     * Manages high scores, including adding, sorting, loading, and saving.
     * Uses Java Serialization, which is simple but can be fragile. For a more robust
     * application, a format like JSON or XML would be preferable.
     * ---
     * Gère les meilleurs scores, y compris l'ajout, le tri, le chargement et la sauvegarde.
     * Utilise la Sérialisation Java, qui est simple mais peut être fragile. Pour une application
     * plus robuste, un format comme JSON ou XML serait préférable.
     */
    public static class HighScoreManager {
        private List<ScoreEntry> highScores;
        private final String filename;
        
        public HighScoreManager(String filename) { this.filename = filename; highScores = new ArrayList<>(); }
        
        public void addHighScore(String name, int score) {
            highScores.add(new ScoreEntry(name, score));
            sortHighScores();
            if (highScores.size() > 10) highScores = highScores.subList(0, 10);
        }
        
        public List<ScoreEntry> getHighScores() { return Collections.unmodifiableList(highScores); }
        
        private void sortHighScores() { highScores.sort(Comparator.comparingInt(ScoreEntry::getScore).reversed()); }

        public void saveHighScores() throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) { oos.writeObject(highScores); }
        }
        
        @SuppressWarnings("unchecked") // This is a necessary cast for deserialization
        public void loadHighScores() throws IOException, ClassNotFoundException {
            File file = new File(filename);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) { 
                    highScores = (List<ScoreEntry>) ois.readObject(); 
                    sortHighScores(); 
                }
            }
        }
        
        /**
         * Represents a single high score entry. Must implement Serializable to be saved to a file.
         * ---
         * Représente une seule entrée de meilleur score. Doit implémenter Serializable pour être sauvegardée.
         */
        public static class ScoreEntry implements Serializable {
            // A unique ID for serialization versioning.
            // Un ID unique pour le versioning de la sérialisation.
            private static final long serialVersionUID = 1L;
            
            private final String name; 
            private final int score;
            
            public ScoreEntry(String name, int score) { this.name = name; this.score = score; }
            public String getName() { return name; }
            public int getScore() { return score; }
            @Override public String toString() { return String.format("%s: %d", name, score); }
        }
    }
    
    // ===================================================================================
    // SECTION: UI Inner Classes / Classes Internes de l'Interface Utilisateur
    // ===================================================================================

    /**
     * Inner class representing the main gameplay panel. It contains the game loop and all game logic.
     * As an inner class, it has direct access to the main frame's methods like gameOver().
     * ---
     * Classe interne représentant le panneau de jeu principal. Elle contient la boucle de jeu et toute la logique.
     * En tant que classe interne, elle a un accès direct aux méthodes de la fenêtre principale comme gameOver().
     */
    private class GamePanel extends JPanel implements ActionListener {

        private Timer gameTimer;
        private GameState gameState;
        private PlayerShip player;
        private List<Alien> aliens;
        private List<Laser> playerLasers;
        private List<Laser> alienLasers;
        private List<Particle> particles;
        private List<PowerUp> powerUps;
        private int score;
        private int lives;
        private int currentLevel;
        private int alienMoveDirection = 1;
        private long lastAlienMoveTime;
        private long alienMoveInterval;
        private long alienFireInterval;
        private int alienSpeed;
        private long lastAlienFireTime;
        private final Random random;
        private final List<Point2D.Float> stars;

        public GamePanel() {
            setPreferredSize(new Dimension(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);

            addKeyListener(new GameKeyListener());
            gameTimer = new Timer(16, this); // ~60 FPS
            random = new Random();

            particles = new ArrayList<>();
            powerUps = new ArrayList<>();
            stars = new ArrayList<>();
            for (int i = 0; i < GameConstants.BACKGROUND_STAR_COUNT; i++) {
                stars.add(new Point2D.Float(random.nextInt(GameConstants.WINDOW_WIDTH), random.nextInt(GameConstants.WINDOW_HEIGHT)));
            }
            
            resetGame();
            setGameState(GameState.MAIN_MENU);
        }

        public void resetGame() {
            player = new PlayerShip(GameConstants.WINDOW_WIDTH / 2 - 30, GameConstants.WINDOW_HEIGHT - 80);
            aliens = new ArrayList<>();
            playerLasers = new ArrayList<>();
            alienLasers = new ArrayList<>();
            particles.clear();
            powerUps.clear();
            score = 0;
            lives = GameConstants.INITIAL_LIVES;
            currentLevel = 1;
            alienMoveDirection = 1;
            lastAlienMoveTime = System.currentTimeMillis();
            lastAlienFireTime = System.currentTimeMillis();
            initializeLevel();
        }

        private void initializeLevel() {
            aliens.clear();
            playerLasers.clear();
            alienLasers.clear();
            powerUps.clear();
            player.resetPowerUps();

            alienSpeed = 2 + (currentLevel / 4);
            alienMoveInterval = Math.max(100, 500 - (currentLevel - 1) * 20);
            alienFireInterval = Math.max(200, 1500 - (currentLevel - 1) * 50);

            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 10; col++) {
                    int alienType = row < 2 ? 2 : (row < 4 ? 1 : 0);
                    aliens.add(new Alien(50 + col * 60, 50 + row * 40, alienSpeed, alienType));
                }
            }
        }
        
        public void nextLevel() {
            currentLevel++;
            JOptionPane.showMessageDialog(this, "Congratulations! Proceeding to Level " + currentLevel + "!", "Level Complete", JOptionPane.INFORMATION_MESSAGE);
            initializeLevel();
        }

        public void startGame() { if (!gameTimer.isRunning()) gameTimer.start(); }
        public void stopGame() { if (gameTimer.isRunning()) gameTimer.stop(); }
        public void setGameState(GameState state) {
            this.gameState = state;
            if (state == GameState.PLAYING) startGame(); else stopGame();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            for(Point2D.Float star : stars) g2d.fillOval((int)star.x, (int)star.y, 2, 2);

            if (gameState == GameState.PLAYING || gameState == GameState.PAUSE_MENU) {
                player.draw(g2d);
                for (Alien alien : aliens) alien.draw(g2d);
                for (Laser laser : playerLasers) laser.draw(g2d);
                for (Laser laser : alienLasers) laser.draw(g2d);
                for (PowerUp powerUp : powerUps) powerUp.draw(g2d);
                
                // Draw particles on top. / Dessiner les particules par-dessus.
                // Create a copy to avoid ConcurrentModificationException. / Créer une copie pour éviter ConcurrentModificationException.
                for (Particle particle : new ArrayList<>(particles)) particle.draw(g2d);

                drawHUD(g2d);

                if (gameState == GameState.PAUSE_MENU) drawPauseOverlay(g2d);
            }
        }

        private void drawHUD(Graphics2D g2d) {
            g2d.setColor(Color.CYAN);
            g2d.setFont(AssetLoader.fontUI != null ? AssetLoader.fontUI.deriveFont(20f) : new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, 25);
            g2d.drawString("Lives: " + lives, GameConstants.WINDOW_WIDTH - 100, 25);
            g2d.drawString("Level: " + currentLevel, GameConstants.WINDOW_WIDTH / 2 - 50, 25);
        }

        private void drawPauseOverlay(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(AssetLoader.fontTitle != null ? AssetLoader.fontTitle.deriveFont(50f) : new Font("Arial", Font.BOLD, 50));
            String pauseText = "PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (GameConstants.WINDOW_WIDTH - fm.stringWidth(pauseText)) / 2;
            int y = (GameConstants.WINDOW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(pauseText, x, y);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameState == GameState.PLAYING) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            updatePlayer();
            updateParticles();
            updateBackground();
            updatePlayerLasers();
            updateAliens();
            updateAlienLasers();
            updatePowerUps();
            checkCollisions();
            checkGameConditions();
        }

        private void updatePlayer() { player.update(); }
        private void updateParticles() { particles.removeIf(Particle::isDead); particles.forEach(Particle::update); }
        private void updateBackground() {
            for (Point2D.Float star : stars) {
                star.y += 0.5f;
                if (star.y > GameConstants.WINDOW_HEIGHT) {
                    star.y = 0;
                    star.x = random.nextInt(GameConstants.WINDOW_WIDTH);
                }
            }
        }
        private void updatePlayerLasers() { playerLasers.removeIf(laser -> laser.getY() < 0); playerLasers.forEach(Laser::update); }
        private void updateAlienLasers() { alienLasers.removeIf(laser -> laser.getY() > GameConstants.WINDOW_HEIGHT); alienLasers.forEach(Laser::update); }
        private void updatePowerUps() { powerUps.removeIf(p -> p.getY() > GameConstants.WINDOW_HEIGHT); powerUps.forEach(PowerUp::update); }

        private void updateAliens() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlienMoveTime > alienMoveInterval) {
                boolean moveDown = false;
                for (Alien alien : aliens) {
                    alien.move(alienMoveDirection);
                    if (alien.getX() <= 0 || alien.getX() + alien.getWidth() >= GameConstants.WINDOW_WIDTH) {
                        moveDown = true;
                    }
                }
                if (moveDown) {
                    alienMoveDirection *= -1;
                    for (Alien alien : aliens) alien.moveDown();
                }
                lastAlienMoveTime = currentTime;
            }

            if (currentTime - lastAlienFireTime > alienFireInterval && !aliens.isEmpty()) {
                Alien randomAlien = aliens.get(random.nextInt(aliens.size()));
                alienLasers.add(new Laser(randomAlien.getX() + randomAlien.getWidth() / 2 - 2, randomAlien.getY() + randomAlien.getHeight(), true));
                SoundManager.playSound(SoundManager.alienShootSound);
                lastAlienFireTime = currentTime;
            }
        }
        
        private void checkCollisions() {
            // Player lasers vs Aliens
            Iterator<Laser> playerLaserIter = playerLasers.iterator();
            while(playerLaserIter.hasNext()) {
                Laser laser = playerLaserIter.next();
                Iterator<Alien> alienIter = aliens.iterator();
                while(alienIter.hasNext()) {
                    Alien alien = alienIter.next();
                    if (laser.getBounds().intersects(alien.getBounds())) {
                        playerLaserIter.remove();
                        alien.takeHit();
                        if (alien.isDestroyed()) {
                            alienIter.remove();
                            score += alien.getScoreValue();
                            SoundManager.playSound(SoundManager.explosionSound);
                            createExplosion(alien.getX() + alien.getWidth() / 2, alien.getY() + alien.getHeight() / 2);
                            if (random.nextInt(100) < GameConstants.POWERUP_DROP_CHANCE) {
                                powerUps.add(new PowerUp(alien.getX(), alien.getY()));
                            }
                        } else {
                            SoundManager.playSound(SoundManager.hitSound);
                        }
                        break; // A laser can only hit one alien
                    }
                }
            }
            
            // Alien lasers vs Player
            alienLasers.removeIf(laser -> {
                if (laser.getBounds().intersects(player.getBounds()) && !player.isShieldActive()) {
                    lives--;
                    SoundManager.playSound(SoundManager.playerHitSound);
                    return true;
                }
                return false;
            });
            
            // PowerUps vs Player
            powerUps.removeIf(powerUp -> {
                if (powerUp.getBounds().intersects(player.getBounds())) {
                    player.activatePowerUp(powerUp.getType());
                    SoundManager.playSound(SoundManager.powerupSound);
                    return true;
                }
                return false;
            });
        }
        
        private void checkGameConditions() {
            if (lives <= 0) {
                gameOver(score);
            }
            if (aliens.isEmpty()) {
                nextLevel();
            }
        }

        private void createExplosion(int x, int y) {
            for (int i = 0; i < GameConstants.EXPLOSION_PARTICLE_COUNT; i++) {
                particles.add(new Particle(x, y));
            }
        }

        private class GameKeyListener extends KeyAdapter {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameState == GameState.PLAYING) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT: player.setMovingLeft(true); break;
                        case KeyEvent.VK_RIGHT: player.setMovingRight(true); break;
                        case KeyEvent.VK_SPACE: 
                            List<Laser> newLasers = player.fire();
                            if (!newLasers.isEmpty()) {
                                playerLasers.addAll(newLasers);
                                SoundManager.playSound(SoundManager.playerShootSound);
                            }
                            break;
                        case KeyEvent.VK_ESCAPE:
                        case KeyEvent.VK_P: showPauseMenu(); break;
                    }
                } else if (gameState == GameState.PAUSE_MENU) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_P) {
                        resumeGame();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (gameState == GameState.PLAYING) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) player.setMovingLeft(false);
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) player.setMovingRight(false);
                }
            }
        }
    }

    /**
     * Inner class for all menu screens (Main, Pause, High Scores).
     * It's configurable for different titles, buttons, and behaviors.
     * ---
     * Classe interne pour tous les écrans de menu (Principal, Pause, Meilleurs Scores).
     * Elle est configurable pour différents titres, boutons et comportements.
     */
    private class GameMenu extends JPanel {
        private final GameState menuState;
        private JTextArea highscoreTextArea;
        private final List<Point2D.Float> stars = new ArrayList<>();
        private final Random random = new Random();
        private final Timer animationTimer;

        public GameMenu(String title, String[] menuItems, GameState menuState) {
            this.menuState = menuState;
            setLayout(new GridBagLayout());
            setBackground(Color.BLACK);
            
            for (int i = 0; i < GameConstants.BACKGROUND_STAR_COUNT; i++) stars.add(new Point2D.Float(random.nextInt(GameConstants.WINDOW_WIDTH), random.nextInt(GameConstants.WINDOW_HEIGHT)));
            animationTimer = new Timer(50, e -> {
                for (Point2D.Float star : stars) {
                    star.y += 0.5f;
                    if (star.y > GameConstants.WINDOW_HEIGHT) { star.y = 0; star.x = random.nextInt(GameConstants.WINDOW_WIDTH); }
                }
                repaint();
            });
            animationTimer.start();
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.insets = new Insets(50, 0, 50, 0);

            JLabel titleLabel = new JLabel(title, JLabel.CENTER);
            titleLabel.setFont(AssetLoader.fontTitle != null ? AssetLoader.fontTitle : new Font("Arial", Font.BOLD, 64));
            titleLabel.setForeground(Color.WHITE);
            add(titleLabel, gbc);

            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 0, 10, 0);

            JPanel buttonPanel = new JPanel(new GridBagLayout());
            buttonPanel.setOpaque(false);
            
            if(menuState != GameState.HIGHSCORE_MENU) {
                for (String item : menuItems) {
                    StyledButton button = new StyledButton(item);
                    button.addActionListener(new MenuItemActionListener(item));
                    buttonPanel.add(button, gbc);
                }
            }
            
            gbc.weighty = 1;
            add(buttonPanel, gbc);

            if (menuState == GameState.HIGHSCORE_MENU) {
                setupHighscoreDisplay(gbc);
            }
        }
        
        private void setupHighscoreDisplay(GridBagConstraints gbc) {
            highscoreTextArea = new JTextArea();
            highscoreTextArea.setFont(AssetLoader.fontUI != null ? AssetLoader.fontUI.deriveFont(20f) : new Font("Monospaced", Font.PLAIN, 20));
            highscoreTextArea.setBackground(new Color(0,0,0,128));
            highscoreTextArea.setForeground(Color.WHITE);
            highscoreTextArea.setEditable(false);
            highscoreTextArea.setMargin(new Insets(20, 20, 20, 20));
            JScrollPane scrollPane = new JScrollPane(highscoreTextArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.CYAN));
            
            gbc.weighty = 0;
            gbc.insets = new Insets(0, 0, 20, 0);
            add(scrollPane, gbc);
            
            StyledButton backButton = new StyledButton("Back to Main Menu");
            backButton.addActionListener(e -> {
                SoundManager.playSound(SoundManager.clickSound);
                showMainMenu();
            });
            add(backButton, gbc);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawAnimatedBackground(g, this);
        }

        public void drawAnimatedBackground(Graphics g, Component c) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, c.getWidth(), c.getHeight());
            g2d.setColor(Color.WHITE);
            for (Point2D.Float star : stars) {
                g2d.fillOval((int) star.x, (int) star.y, 2, 2);
            }
        }

        public void updateHighScoresDisplay(List<HighScoreManager.ScoreEntry> scores) {
            if (highscoreTextArea == null) return;
            StringBuilder sb = new StringBuilder();
            if (scores.isEmpty()) {
                sb.append("\n   No scores recorded yet.\n");
            } else {
                sb.append(String.format(" %-4s %-15s %s\n", "Rank", "Name", "Score"));
                sb.append("---------------------------------\n");
                for (int i = 0; i < scores.size(); i++) {
                    sb.append(String.format(" %2d.  %-15s %d\n", i + 1, scores.get(i).getName(), scores.get(i).getScore()));
                }
            }
            highscoreTextArea.setText(sb.toString());
            highscoreTextArea.setCaretPosition(0);
        }
        
        private class MenuItemActionListener implements ActionListener {
            private final String itemName;
            public MenuItemActionListener(String itemName) { this.itemName = itemName; }
            @Override public void actionPerformed(ActionEvent e) {
                SoundManager.playSound(SoundManager.clickSound);
                if (menuState == GameState.MAIN_MENU) handleMainMenuAction();
                else if (menuState == GameState.PAUSE_MENU) handlePauseMenuAction();
            }
            private void handleMainMenuAction() {
                switch (itemName) {
                    case "New Game": startGame(); break;
                    case "High Scores": showHighScores(); break;
                    case "Instructions": showInstructions(); break;
                    case "About": showAbout(); break;
                    case "Quit":
                        saveScoresOnExit();
                        System.exit(0);
                        break;
                }
            }
            private void handlePauseMenuAction() {
                switch (itemName) {
                    case "Resume": resumeGame(); break;
                    case "Main Menu": showMainMenu(); break;
                    case "Quit":
                        saveScoresOnExit();
                        System.exit(0);
                        break;
                }
            }
        }
    }
    
    /**
     * A custom styled JButton for menu items.
     * ---
     * Un JButton personnalisé pour les éléments de menu.
     */
    private class StyledButton extends JButton {
        public StyledButton(String text) {
            super(text);
            setFont(AssetLoader.fontUI != null ? AssetLoader.fontUI : new Font("Arial", Font.BOLD, 24));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setForeground(Color.CYAN); }
                @Override public void mouseExited(MouseEvent e) { setForeground(Color.WHITE); }
            });
        }
    }

    // ===================================================================================
    // SECTION: Game Entity Inner Classes / Classes Internes des Entités du Jeu
    // ===================================================================================

    private class PlayerShip {
        private int x, y;
        private final int width, height;
        private boolean movingLeft, movingRight;
        private long lastFireTime = 0;
        private boolean rapidFireActive = false;
        private boolean shieldActive = false;
        private long rapidFireEndTime;
        private long shieldEndTime;

        public PlayerShip(int x, int y) {
            this.x = x; this.y = y;
            this.width = (AssetLoader.playerShip != null) ? AssetLoader.playerShip.getWidth(null) : GameConstants.PLAYER_WIDTH;
            this.height = (AssetLoader.playerShip != null) ? AssetLoader.playerShip.getHeight(null) : GameConstants.PLAYER_HEIGHT;
        }

        public void update() {
            if (movingLeft) x -= GameConstants.PLAYER_SPEED;
            if (movingRight) x += GameConstants.PLAYER_SPEED;
            x = Math.max(0, Math.min(x, GameConstants.WINDOW_WIDTH - width)); // Clamp position
            
            long currentTime = System.currentTimeMillis();
            if (rapidFireActive && currentTime > rapidFireEndTime) rapidFireActive = false;
            if (shieldActive && currentTime > shieldEndTime) shieldActive = false;
        }

        public void draw(Graphics2D g) {
            if (AssetLoader.playerShip != null) {
                g.drawImage(AssetLoader.playerShip, x, y, width, height, null);
            } else {
                g.setColor(Color.GREEN);
                g.fillRect(x, y, width, height);
            }
            if (shieldActive) {
                long currentTime = System.currentTimeMillis();
                float alpha = (float)(shieldEndTime - currentTime) / GameConstants.POWERUP_DURATION_MS;
                g.setColor(new Color(0, 1, 1, Math.max(0, alpha * 0.5f)));
                g.fillOval(x - 10, y - 10, width + 20, height + 20);
            }
        }

        public List<Laser> fire() {
            List<Laser> lasers = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            long fireRate = rapidFireActive ? GameConstants.PLAYER_RAPID_FIRE_RATE_MS : GameConstants.PLAYER_FIRE_RATE_MS;
            if (currentTime - lastFireTime > fireRate) {
                lasers.add(new Laser(x + width / 2 - 2, y, false));
                lastFireTime = currentTime;
            }
            return lasers;
        }
        
        public void activatePowerUp(PowerUp.PowerUpType type) {
            long currentTime = System.currentTimeMillis();
            if (type == PowerUp.PowerUpType.RAPID_FIRE) {
                rapidFireActive = true;
                rapidFireEndTime = currentTime + GameConstants.POWERUP_DURATION_MS;
            } else if (type == PowerUp.PowerUpType.SHIELD) {
                shieldActive = true;
                shieldEndTime = currentTime + GameConstants.POWERUP_DURATION_MS;
            }
        }
        
        public void resetPowerUps() { rapidFireActive = false; shieldActive = false; }
        public boolean isShieldActive() { return shieldActive; }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
        public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
        public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    }

    private class Alien {
        private int x, y, width, height, speed, type, hp, scoreValue;
        private final Image alienImage;

        public Alien(int x, int y, int speed, int type) {
            this.x = x; this.y = y; this.speed = speed; this.type = type;
            Image baseImage = null;
            switch(type) {
                case 0: hp = 1; scoreValue = 10; baseImage = AssetLoader.alien1; break;
                case 1: hp = 2; scoreValue = 20; baseImage = AssetLoader.alien2; break;
                default: hp = 3; scoreValue = 50; baseImage = AssetLoader.alien3; break;
            }
            this.alienImage = baseImage;
            this.width = (this.alienImage != null) ? this.alienImage.getWidth(null) : 35;
            this.height = (this.alienImage != null) ? this.alienImage.getHeight(null) : 35;
        }

        public void move(int direction) { x += speed * direction; }
        public void moveDown() { y += GameConstants.ALIEN_DROP_SPEED; }

        public void draw(Graphics2D g) {
            if (alienImage != null) {
                g.drawImage(alienImage, x, y, width, height, null);
            } else {
                g.setColor(Color.RED); g.fillRect(x, y, width, height);
            }
        }
        
        public void takeHit() { this.hp--; }
        public boolean isDestroyed() { return this.hp <= 0; }
        public int getScoreValue() { return this.scoreValue; }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    private class Laser {
        private int x, y, width, height, speed;
        private final boolean isAlienLaser;
        private final Image laserImage;

        public Laser(int x, int y, boolean isAlienLaser) {
            this.x = x; this.y = y; 
            this.isAlienLaser = isAlienLaser;
            this.speed = isAlienLaser ? GameConstants.ALIEN_LASER_SPEED : GameConstants.PLAYER_LASER_SPEED;
            this.laserImage = isAlienLaser ? AssetLoader.laserAlien : AssetLoader.laserPlayer;
            this.width = (this.laserImage != null) ? laserImage.getWidth(null) : 4;
            this.height = (this.laserImage != null) ? laserImage.getHeight(null) : 15;
        }

        public void update() { y += isAlienLaser ? speed : -speed; }

        public void draw(Graphics2D g) {
            if (laserImage != null) {
                g.drawImage(laserImage, x, y, width, height, null);
            } else {
                g.setColor(isAlienLaser ? Color.MAGENTA : Color.YELLOW);
                g.fillRect(x, y, width, height);
            }
        }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
        public int getY() { return y; }
    }

    private class PowerUp {
        public enum PowerUpType { SHIELD, RAPID_FIRE }
        private int x, y;
        private final int width = 30, height = 30, speed = 2;
        private final PowerUpType type;
        private final Image image;
        
        public PowerUp(int x, int y) {
            this.x = x; this.y = y;
            this.type = Math.random() < 0.5 ? PowerUpType.SHIELD : PowerUpType.RAPID_FIRE;
            this.image = this.type == PowerUpType.SHIELD ? AssetLoader.powerUpShield : AssetLoader.powerUpRapid;
        }
        
        public void update() { y += speed; }
        public void draw(Graphics2D g) {
            if (image != null) {
                g.drawImage(image, x, y, width, height, null);
            } else {
                g.setColor(Color.CYAN); g.fillRect(x, y, width, height);
                g.setColor(Color.BLACK); g.drawString(type == PowerUpType.SHIELD ? "S" : "R", x+10, y+20);
            }
        }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
        public int getY() { return y; }
        public PowerUpType getType() { return type; }
    }
    
    private class Particle {
        private float x, y, vx, vy;
        private int size;
        private final Color color;
        private int lifetime;
        
        public Particle(int x, int y) {
            this.x = x; this.y = y; this.vx = (float) (Math.random() * 4 - 2); this.vy = (float) (Math.random() * 4 - 2);
            this.size = (int) (Math.random() * 5 + 2); this.color = new Color(1.0f, (float)Math.random() * 0.5f + 0.5f, 0f);
            this.lifetime = (int) (Math.random() * 40 + 20);
        }
        
        public void update() { x += vx; y += vy; vy += 0.05f; lifetime--; }
        public void draw(Graphics2D g) {
            float alpha = Math.max(0, (float)lifetime / 60.0f);
            g.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, alpha));
            g.fillOval((int)x, (int)y, size, size);
        }
        public boolean isDead() { return lifetime <= 0; }
    }
}