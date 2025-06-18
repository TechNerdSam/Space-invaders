import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceInvadersGame extends JFrame {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private GamePanel gamePanel;
    private GameMenu mainMenu;
    private GameMenu pauseMenu;
    private GameMenu highscoreMenu;

    private HighScoreManager highScoreManager;

    public SpaceInvadersGame() {
        setTitle("Space Invaders");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrer la fenêtre

        highScoreManager = new HighScoreManager("highscores.dat");
        try {
            highScoreManager.loadHighScores();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement des high scores: " + e.getMessage());
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Menu Principal
        mainMenu = new GameMenu(this, "Menu Principal", new String[]{
                "Nouvelle Partie",
                "High Scores",
                "Instructions",
                "À Propos",
                "Quitter"
        }, GameState.MAIN_MENU);
        mainPanel.add(mainMenu, "MAIN_MENU");

        // Panneau de Jeu
        gamePanel = new GamePanel(this);
        mainPanel.add(gamePanel, "GAME");

        // Menu de Pause
        pauseMenu = new GameMenu(this, "Jeu en Pause", new String[]{
                "Reprendre",
                "Menu Principal",
                "Quitter"
        }, GameState.PAUSE_MENU);
        mainPanel.add(pauseMenu, "PAUSE_MENU");

        // Menu High Scores
        highscoreMenu = new GameMenu(this, "Meilleurs Scores", new String[]{
                "Retour au Menu Principal"
        }, GameState.HIGHSCORE_MENU);
        mainPanel.add(highscoreMenu, "HIGHSCORE_MENU");

        add(mainPanel);
        setVisible(true);

        // Gérer la sauvegarde des scores lors de la fermeture de la fenêtre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    highScoreManager.saveHighScores();
                } catch (IOException ioException) {
                    System.err.println("Erreur lors de la sauvegarde des high scores: " + ioException.getMessage());
                }
            }
        });

        showMainMenu();
    }

    public void showMainMenu() {
        cardLayout.show(mainPanel, "MAIN_MENU");
        gamePanel.setGameState(GameState.MAIN_MENU);
        gamePanel.stopGame(); // Assurez-vous que le jeu est arrêté
        mainMenu.requestFocusInWindow();
    }

    public void startGame() {
        cardLayout.show(mainPanel, "GAME");
        gamePanel.setGameState(GameState.PLAYING);
        gamePanel.resetGame(); // Réinitialise le jeu, y compris le niveau
        gamePanel.startGame();
        gamePanel.requestFocusInWindow(); // Donne le focus au GamePanel pour les entrées clavier
    }

    public void showPauseMenu() {
        gamePanel.setGameState(GameState.PAUSE_MENU);
        cardLayout.show(mainPanel, "PAUSE_MENU");
        pauseMenu.requestFocusInWindow();
    }

    public void resumeGame() {
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
        JOptionPane.showMessageDialog(this,
                "Déplacez le vaisseau avec les flèches Gauche/Droite.\nTirez avec la touche Espace.\n" +
                        "Mettez en pause le jeu avec Échap ou P.\n" +
                        "Détruisez tous les aliens pour gagner !",
                "Instructions",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Space Invaders en Java SE\nCréé par Samyn-Antoy ABASSE\nVersion 1.0",
                "À Propos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void gameOver(int score) {
        String pseudo = JOptionPane.showInputDialog(this, "Game Over! Votre score: " + score + "\nEntrez votre pseudo:", "Enregistrer le Score", JOptionPane.PLAIN_MESSAGE);
        if (pseudo != null && !pseudo.trim().isEmpty()) {
            highScoreManager.addHighScore(pseudo.trim(), score);
            try {
                highScoreManager.saveHighScores();
            } catch (IOException e) {
                System.err.println("Erreur lors de la sauvegarde du score: " + e.getMessage());
            }
        }
        showMainMenu();
    }

    public HighScoreManager getHighScoreManager() {
        return highScoreManager;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpaceInvadersGame::new);
    }

    // --- Classes internes du jeu ---

    // GamePanel
    private class GamePanel extends JPanel implements ActionListener {

        private SpaceInvadersGame gameFrame;
        private Timer gameTimer;
        private GameState gameState;

        // Entités du jeu
        private PlayerShip player;
        private List<Alien> aliens;
        private List<Laser> playerLasers;
        private List<Laser> alienLasers;

        private int score;
        private int lives;
        private int currentLevel; // Nouveau: Niveau actuel du jeu
        private int alienMoveDirection = 1; // 1 pour droite, -1 pour gauche
        private long lastAlienMoveTime;
        private long alienMoveInterval; // Intervalle de déplacement des aliens (dépend du niveau)
        private long alienFireInterval; // Intervalle de tir des aliens (dépend du niveau)
        private int alienSpeed; // Vitesse de déplacement des aliens (dépend du niveau)
        private long lastAlienFireTime;
        private Random random;

        public GamePanel(SpaceInvadersGame gameFrame) {
            this.gameFrame = gameFrame;
            setPreferredSize(new Dimension(SpaceInvadersGame.WIDTH, SpaceInvadersGame.HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow(); // Important pour que le JPanel reçoive les événements clavier

            addKeyListener(new GameKeyListener());
            gameTimer = new Timer(10, this); // 10 ms pour un rafraîchissement rapide
            random = new Random();

            resetGame(); // Initialiser l'état du jeu au démarrage
            setGameState(GameState.MAIN_MENU); // Commence au menu principal
        }

        public void resetGame() {
            player = new PlayerShip(SpaceInvadersGame.WIDTH / 2 - 30, SpaceInvadersGame.HEIGHT - 80);
            aliens = new ArrayList<>();
            playerLasers = new ArrayList<>();
            alienLasers = new ArrayList<>();
            score = 0; // Le score est réinitialisé lors d'une nouvelle partie, mais le highscore garde les meilleurs scores.
            lives = 3;
            currentLevel = 1; // Démarre toujours au niveau 1
            alienMoveDirection = 1;
            lastAlienMoveTime = System.currentTimeMillis();
            lastAlienFireTime = System.currentTimeMillis();

            initializeLevel(); // Initialise les aliens et la difficulté pour le niveau 1
        }

        private void initializeLevel() {
            aliens.clear(); // Efface les anciens aliens
            playerLasers.clear();
            alienLasers.clear();

            // Ajuster la difficulté en fonction du niveau
            // Plus le niveau est élevé, plus l'intervalle est petit (plus rapide) et la vitesse est grande
            alienSpeed = 2 + (currentLevel / 4); // Augmente la vitesse toutes les 4 niveaux
            alienMoveInterval = Math.max(100, 500 - (currentLevel - 1) * 20); // Min 100ms
            alienFireInterval = Math.max(200, 1500 - (currentLevel - 1) * 50); // Min 200ms

            // Re-initialiser les aliens pour le nouveau niveau
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 10; col++) {
                    aliens.add(new Alien(50 + col * 60, 50 + row * 50, alienSpeed)); // Passe la vitesse aux aliens
                }
            }
        }

        public void nextLevel() {
            if (currentLevel < 20) {
                currentLevel++;
            } else {
                currentLevel = 1; // Recommence la boucle des niveaux
                // Le score n'est PAS réinitialisé ici, il continue de compter pour le highscore
            }
            JOptionPane.showMessageDialog(this, "Félicitations! Vous passez au niveau " + currentLevel + "!", "Niveau Complété", JOptionPane.INFORMATION_MESSAGE);
            initializeLevel(); // Prépare le nouveau niveau
        }


        public void startGame() {
            if (!gameTimer.isRunning()) {
                gameTimer.start();
            }
        }

        public void stopGame() {
            if (gameTimer.isRunning()) {
                gameTimer.stop();
            }
        }

        public void setGameState(GameState state) {
            this.gameState = state;
            if (state == GameState.PLAYING) {
                startGame();
            } else {
                stopGame();
            }
            repaint(); // Redessiner pour refléter le nouvel état (ex: afficher le menu de pause)
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            if (gameState == GameState.PLAYING || gameState == GameState.PAUSE_MENU) {
                // Dessiner les éléments du jeu
                player.draw(g2d);
                for (Alien alien : aliens) {
                    alien.draw(g2d);
                }
                for (Laser laser : playerLasers) {
                    laser.draw(g2d);
                }
                for (Laser laser : alienLasers) {
                    laser.draw(g2d);
                }

                // Afficher le score, les vies et le niveau
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Score: " + score, 10, 25);
                g2d.drawString("Vies: " + lives, SpaceInvadersGame.WIDTH - 100, 25);
                g2d.drawString("Niveau: " + currentLevel, SpaceInvadersGame.WIDTH / 2 - 50, 25); // Affichage du niveau

                if (gameState == GameState.PAUSE_MENU) {
                    g2d.setColor(new Color(0, 0, 0, 150)); // Semi-transparent overlay
                    g2d.fillRect(0, 0, SpaceInvadersGame.WIDTH, SpaceInvadersGame.HEIGHT);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 50));
                    String pauseText = "PAUSE";
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (SpaceInvadersGame.WIDTH - fm.stringWidth(pauseText)) / 2;
                    int y = (SpaceInvadersGame.HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
                    g2d.drawString(pauseText, x, y);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameState == GameState.PLAYING) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            // Mettre à jour le joueur
            player.update();

            // Mettre à jour les lasers du joueur
            Iterator<Laser> playerLaserIterator = playerLasers.iterator();
            while (playerLaserIterator.hasNext()) {
                Laser laser = playerLaserIterator.next();
                laser.update();
                if (laser.getY() < 0) {
                    playerLaserIterator.remove();
                } else {
                    // Collision laser du joueur avec les aliens
                    Iterator<Alien> alienIterator = aliens.iterator();
                    while (alienIterator.hasNext()) {
                        Alien alien = alienIterator.next();
                        if (laser.getBounds().intersects(alien.getBounds())) {
                            playerLaserIterator.remove();
                            alienIterator.remove();
                            score += 10; // Augmenter le score
                            break;
                        }
                    }
                }
            }

            // Mettre à jour et faire bouger les aliens
            boolean moveDown = false;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlienMoveTime > alienMoveInterval) { // Utilise l'intervalle dynamique
                for (Alien alien : aliens) {
                    alien.move(alienMoveDirection);
                    if (alien.getX() < 0 || alien.getX() + alien.getWidth() > SpaceInvadersGame.WIDTH) {
                        moveDown = true;
                    }
                }
                if (moveDown) {
                    alienMoveDirection *= -1; // Changer de direction
                    for (Alien alien : aliens) {
                        alien.moveDown();
                    }
                }
                lastAlienMoveTime = currentTime;
            }

            // Les aliens tirent
            if (currentTime - lastAlienFireTime > alienFireInterval && !aliens.isEmpty()) { // Utilise l'intervalle dynamique
                Alien randomAlien = aliens.get(random.nextInt(aliens.size()));
                alienLasers.add(new Laser(randomAlien.getX() + randomAlien.getWidth() / 2 - 2, randomAlien.getY() + randomAlien.getHeight(), 5, true));
                lastAlienFireTime = currentTime;
            }

            // Mettre à jour les lasers des aliens
            Iterator<Laser> alienLaserIterator = alienLasers.iterator();
            while (alienLaserIterator.hasNext()) {
                Laser laser = alienLaserIterator.next();
                laser.update();
                if (laser.getY() > SpaceInvadersGame.HEIGHT) {
                    alienLaserIterator.remove();
                } else {
                    // Collision laser alien avec le joueur
                    if (laser.getBounds().intersects(player.getBounds())) {
                        alienLaserIterator.remove();
                        lives--;
                        if (lives <= 0) {
                            gameFrame.gameOver(score);
                        }
                    }
                }
            }

            // Vérifier si tous les aliens sont détruits pour passer au niveau suivant
            if (aliens.isEmpty()) {
                nextLevel(); // Passe au niveau suivant
            }
        }

        private class GameKeyListener extends KeyAdapter {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameState == GameState.PLAYING) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            player.setMovingLeft(true);
                            break;
                        case KeyEvent.VK_RIGHT:
                            player.setMovingRight(true);
                            break;
                        case KeyEvent.VK_SPACE:
                            playerLasers.add(player.fire());
                            break;
                        case KeyEvent.VK_ESCAPE:
                        case KeyEvent.VK_P:
                            gameFrame.showPauseMenu();
                            break;
                    }
                } else if (gameState == GameState.PAUSE_MENU) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_P) {
                        gameFrame.resumeGame();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (gameState == GameState.PLAYING) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            player.setMovingLeft(false);
                            break;
                        case KeyEvent.VK_RIGHT:
                            player.setMovingRight(false);
                            break;
                    }
                }
            }
        }
    }

    // PlayerShip
    private class PlayerShip {
        private int x, y;
        private final int width = 60;
        private final int height = 30;
        private final int speed = 5;
        private boolean movingLeft, movingRight;

        public PlayerShip(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            if (movingLeft) {
                x -= speed;
            }
            if (movingRight) {
                x += speed;
            }

            // Limiter le mouvement du joueur à l'intérieur de la fenêtre
            if (x < 0) {
                x = 0;
            }
            if (x + width > SpaceInvadersGame.WIDTH) {
                x = SpaceInvadersGame.WIDTH - width;
            }
        }

        public void draw(Graphics2D g) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, width, height);
        }

        public Laser fire() {
            // Le laser part du centre du vaisseau
            return new Laser(x + width / 2 - 2, y, 10, false);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void setMovingLeft(boolean movingLeft) {
            this.movingLeft = movingLeft;
        }

        public void setMovingRight(boolean movingRight) {
            this.movingRight = movingRight;
        }
    }

    // Alien
    private class Alien {
        private int x, y;
        private final int width = 40;
        private final int height = 30;
        private int speed; // La vitesse dépendra du niveau

        private final int dropSpeed = 10;

        public Alien(int x, int y, int speed) { // Ajout du paramètre de vitesse
            this.x = x;
            this.y = y;
            this.speed = speed; // Initialise la vitesse de l'alien
        }

        public void move(int direction) {
            x += speed * direction;
        }

        public void moveDown() {
            y += dropSpeed;
        }

        public void draw(Graphics2D g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    // Laser
    private class Laser {
        private int x, y;
        private final int width = 4;
        private final int height = 10;
        private final int speed;
        private boolean isAlienLaser;

        public Laser(int x, int y, int speed, boolean isAlienLaser) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.isAlienLaser = isAlienLaser;
        }

        public void update() {
            if (isAlienLaser) {
                y += speed; // Les lasers aliens descendent
            } else {
                y -= speed; // Les lasers du joueur montent
            }
        }

        public void draw(Graphics2D g) {
            g.setColor(isAlienLaser ? Color.MAGENTA : Color.YELLOW);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getY() {
            return y;
        }
    }

    // GameState (Enum)
    private enum GameState {
        MAIN_MENU,
        PLAYING,
        PAUSE_MENU,
        HIGHSCORE_MENU,
        INSTRUCTIONS,
        ABOUT
    }

    // HighScoreManager
    private class HighScoreManager {
        private List<ScoreEntry> highScores;
        private String filename;

        public HighScoreManager(String filename) {
            this.filename = filename;
            highScores = new ArrayList<>();
        }

        public void addHighScore(String pseudo, int score) {
            highScores.add(new ScoreEntry(pseudo, score));
            sortHighScores();
            // Garder seulement les 10 meilleurs scores (ou plus si vous voulez)
            if (highScores.size() > 10) {
                highScores = highScores.subList(0, 10);
            }
        }

        public List<ScoreEntry> getHighScores() {
            return Collections.unmodifiableList(highScores);
        }

        private void sortHighScores() {
            Collections.sort(highScores, Comparator.comparingInt(ScoreEntry::getScore).reversed());
        }

        public void saveHighScores() throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(highScores);
            }
        }

        public void loadHighScores() throws IOException, ClassNotFoundException {
            File file = new File(filename);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                    highScores = (List<ScoreEntry>) ois.readObject();
                    sortHighScores();
                }
            }
        }

        // ScoreEntry (Classe interne statique pour sérialisation)
        public static class ScoreEntry implements Serializable {
            private String pseudo;
            private int score;

            public ScoreEntry(String pseudo, int score) {
                this.pseudo = pseudo;
                this.score = score;
            }

            public String getPseudo() {
                return pseudo;
            }

            public int getScore() {
                return score;
            }

            @Override
            public String toString() {
                return String.format("%s: %d", pseudo, score);
            }
        }
    }

    // GameMenu
    private class GameMenu extends JPanel {
        private SpaceInvadersGame gameFrame;
        private String title;
        private String[] menuItems;
        private GameState menuState; // L'état de jeu associé à ce menu

        private JLabel titleLabel;
        private JPanel buttonPanel;
        private JTextArea highscoreTextArea; // Pour le menu highscore

        public GameMenu(SpaceInvadersGame gameFrame, String title, String[] menuItems, GameState menuState) {
            this.gameFrame = gameFrame;
            this.title = title;
            this.menuItems = menuItems;
            this.menuState = menuState;

            setLayout(new BorderLayout());
            setBackground(Color.BLACK);

            titleLabel = new JLabel(title, JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
            titleLabel.setForeground(Color.WHITE);
            add(titleLabel, BorderLayout.NORTH);

            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
            buttonPanel.setBackground(Color.BLACK);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0)); // Espacement du haut

            for (String item : menuItems) {
                JButton button = new JButton(item);
                button.setFont(new Font("Arial", Font.BOLD, 24));
                button.setBackground(Color.DARK_GRAY);
                button.setForeground(Color.WHITE);
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                button.setMaximumSize(new Dimension(300, 50));
                button.setFocusPainted(false); // Enlève le cadre de focus par défaut
                button.addActionListener(new MenuItemActionListener(item));
                buttonPanel.add(button);
                buttonPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Espacement entre les boutons
            }
            add(buttonPanel, BorderLayout.CENTER);

            // Configuration spécifique pour le menu highscore
            if (menuState == GameState.HIGHSCORE_MENU) {
                buttonPanel.removeAll(); // Supprime les boutons par défaut
                highscoreTextArea = new JTextArea();
                highscoreTextArea.setFont(new Font("Monospaced", Font.PLAIN, 20));
                highscoreTextArea.setBackground(Color.BLACK);
                highscoreTextArea.setForeground(Color.WHITE);
                highscoreTextArea.setEditable(false);
                highscoreTextArea.setMargin(new Insets(20, 20, 20, 20));
                JScrollPane scrollPane = new JScrollPane(highscoreTextArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));
                scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

                JPanel highscorePanel = new JPanel();
                highscorePanel.setLayout(new BoxLayout(highscorePanel, BoxLayout.Y_AXIS));
                highscorePanel.setBackground(Color.BLACK);
                highscorePanel.add(Box.createVerticalGlue()); // Pousse le contenu vers le centre
                highscorePanel.add(scrollPane);
                highscorePanel.add(Box.createRigidArea(new Dimension(0, 30))); // Espace sous le tableau
                buttonPanel.add(highscorePanel);

                JButton backButton = new JButton("Retour au Menu Principal");
                backButton.setFont(new Font("Arial", Font.BOLD, 24));
                backButton.setBackground(Color.DARK_GRAY);
                backButton.setForeground(Color.WHITE);
                backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                backButton.setMaximumSize(new Dimension(300, 50));
                backButton.setFocusPainted(false);
                backButton.addActionListener(e -> gameFrame.showMainMenu());
                buttonPanel.add(backButton);
                buttonPanel.add(Box.createVerticalGlue()); // Pousse le contenu vers le centre
            }
        }

        // Méthode pour mettre à jour l'affichage des high scores
        public void updateHighScoresDisplay(List<HighScoreManager.ScoreEntry> scores) {
            if (highscoreTextArea != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("   Pseudo\tScore\n");
                sb.append("-----------------------\n");
                if (scores.isEmpty()) {
                    sb.append("   Aucun score enregistré.\n");
                } else {
                    for (int i = 0; i < scores.size(); i++) {
                        sb.append(String.format("%2d. %-10s\t%d\n", i + 1, scores.get(i).getPseudo(), scores.get(i).getScore()));
                    }
                }
                highscoreTextArea.setText(sb.toString());
                highscoreTextArea.setCaretPosition(0); // Remettre le scroll en haut
            }
        }

        private class MenuItemActionListener implements ActionListener {
            private String itemName;

            public MenuItemActionListener(String itemName) {
                this.itemName = itemName;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                switch (menuState) {
                    case MAIN_MENU:
                        handleMainMenuAction();
                        break;
                    case PAUSE_MENU:
                        handlePauseMenuAction();
                        break;
                    case HIGHSCORE_MENU:
                        // Le bouton de retour est géré séparément dans le constructeur
                        break;
                }
            }

            private void handleMainMenuAction() {
                switch (itemName) {
                    case "Nouvelle Partie":
                        gameFrame.startGame();
                        break;
                    case "High Scores":
                        gameFrame.showHighScores();
                        break;
                    case "Instructions":
                        gameFrame.showInstructions();
                        break;
                    case "À Propos":
                        gameFrame.showAbout();
                        break;
                    case "Quitter":
                        try {
                            gameFrame.getHighScoreManager().saveHighScores();
                        } catch (IOException ioException) {
                            System.err.println("Erreur lors de la sauvegarde des high scores à la fermeture: " + ioException.getMessage());
                        }
                        System.exit(0);
                        break;
                }
            }

            private void handlePauseMenuAction() {
                switch (itemName) {
                    case "Reprendre":
                        gameFrame.resumeGame();
                        break;
                    case "Menu Principal":
                        gameFrame.showMainMenu();
                        break;
                    case "Quitter":
                        try {
                            gameFrame.getHighScoreManager().saveHighScores();
                        } catch (IOException ioException) {
                            System.err.println("Erreur lors de la sauvegarde des high scores à la fermeture: " + ioException.getMessage());
                        }
                        System.exit(0);
                        break;
                }
            }
        }
    }
}