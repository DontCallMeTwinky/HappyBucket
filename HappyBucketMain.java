import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class HappyBucketMain {
    private static final Path SAVE_FILE = Paths.get("happybucket_save.txt");
    private static final int MAX_CUPCAKES = 3;

    private final Random random = new Random();
    private final Set<String> inventory = new LinkedHashSet<>();

    private int daysLeft = 5;
    private int cupcakes = 0;
    private int energy = 3;
    private int coins = 0;
    private int chapter = 1;
    private boolean escaped = false;
    private boolean gameOver = false;
    private String ending = "";

    private JFrame frame;
    private JTextArea logArea;
    private JLabel titleLabel;
    private JLabel statLabel;
    private JLabel inventoryLabel;
    private PixelScenePanel sceneCanvas;
    private int animationFrame = 0;
    private int effectTimer = 0;
    private int goblinHealth = 100;
    private int playerHealth = 100;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new HappyBucketMain().buildWindow();
        });
    }

    private void buildWindow() {
        frame = new JFrame("Happy Bucket: Last Cupcake");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 720);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        titleLabel = new JLabel("HAPPY BUCKET: LAST CUPCAKE", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(24f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JPanel scenePanel = new JPanel();
        scenePanel.setBorder(BorderFactory.createTitledBorder("Scene"));
        scenePanel.setBackground(new java.awt.Color(34, 32, 39));
        scenePanel.setPreferredSize(new java.awt.Dimension(900, 180));

        sceneCanvas = new PixelScenePanel();
        sceneCanvas.setPreferredSize(new Dimension(900, 180));
        sceneCanvas.setBackground(new Color(34, 32, 39));
        scenePanel.add(sceneCanvas);

        statLabel = new JLabel();
        statLabel.setFont(statLabel.getFont().deriveFont(14f));
        statLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        inventoryLabel = new JLabel();
        inventoryLabel.setFont(inventoryLabel.getFont().deriveFont(14f));

        JPanel buttonPanel = new JPanel(new java.awt.GridLayout(2, 4, 8, 8));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        JButton searchButton = new JButton("Search Ruins");
        JButton restButton = new JButton("Rest");
        JButton shopButton = new JButton("Bakery Shop");
        JButton fightButton = new JButton("Fight Goblin");
        JButton inspectButton = new JButton("Inspect Gear");
        JButton escapeButton = new JButton("Escape");
        JButton saveButton = new JButton("Save");
        JButton newGameButton = new JButton("New Game");

        searchButton.addActionListener(e -> handleChoice(1));
        restButton.addActionListener(e -> handleChoice(2));
        shopButton.addActionListener(e -> handleChoice(3));
        fightButton.addActionListener(e -> handleChoice(4));
        inspectButton.addActionListener(e -> handleChoice(5));
        escapeButton.addActionListener(e -> handleChoice(6));
        saveButton.addActionListener(e -> saveGameAndMessage());
        newGameButton.addActionListener(e -> startNewGame());

        buttonPanel.add(searchButton);
        buttonPanel.add(restButton);
        buttonPanel.add(shopButton);
        buttonPanel.add(fightButton);
        buttonPanel.add(inspectButton);
        buttonPanel.add(escapeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(newGameButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setRows(18);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Story"));

        root.add(titleLabel);
        root.add(scenePanel);
        root.add(statLabel);
        root.add(inventoryLabel);
        root.add(buttonPanel);
        root.add(scrollPane);

        frame.setContentPane(root);
        frame.setVisible(true);

        startNewGame();
    }

    private void startNewGame() {
        resetGame();
        appendLog("You wake up beneath a rusted steel bucket. The world is over. The bucket is hungry.");
        appendLog("You need 3 cupcakes before you can escape.");
        appendLog("Search for scraps, rest, buy supplies, and outwit Twinkleberry.");
        refreshStats();
        refreshScene();
    }

    private void handleChoice(int choice) {
        if (gameOver || escaped) {
            appendLog("The run is already over. Start a new game to play again.");
            return;
        }

        if (choice == 7) {
            saveGameAndMessage();
            return;
        }

        resolveChoice(choice);
        if (energy <= 0 && !escaped) {
            gameOver = true;
            ending = "exhaustion";
            appendLog("You collapse from exhaustion. The bucket wins.");
        }

        if (playerHealth <= 0 && !escaped) {
            gameOver = true;
            ending = "boss";
            appendLog("The goblin tears through your last defenses. The bucket wins.");
        }

        if (daysLeft > 0) {
            daysLeft--;
        }

        if (daysLeft == 0 && cupcakes < MAX_CUPCAKES && !escaped && !gameOver) {
            gameOver = true;
            ending = "time";
            appendLog("The final day ends before you collect enough cupcakes.");
        }

        if (escaped) {
            refreshScene();
            appendLog(printEndingText());
        } else if (gameOver) {
            refreshScene();
            appendLog(printBadEndingText());
        }

        refreshStats();
        refreshScene();
    }

    private void refreshStats() {
        statLabel.setText("Chapter " + chapter + " | Days left: " + daysLeft + " | Cupcakes: " + cupcakes + "/" + MAX_CUPCAKES + " | Energy: " + energy + " | Health: " + playerHealth + " | Coins: " + coins);
        inventoryLabel.setText("Inventory: " + (inventory.isEmpty() ? "empty" : String.join(", ", inventory)));
    }

    private void refreshScene() {
        if (sceneCanvas != null) {
            animationFrame = (animationFrame + 1) % 8;
            if (effectTimer > 0) {
                effectTimer--;
            }
            sceneCanvas.repaint();
        }
    }

    private class PixelScenePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final String[] PLAYER_SPRITE = {
            "....AA....",
            "...ABBA...",
            "...ACCA...",
            "..ADDDDA..",
            "..AEEEA...",
            "....FF....",
            "...F..F...",
            "...F..F..."
        };

        private final String[] GOBLIN_SPRITE = {
            "..GG..GG..",
            ".GGGGGGGG.",
            ".GHGGGHGG.",
            "..GHHHGG..",
            "..GIIIGG..",
            "...JJJ....",
            "..K...K...",
            ".K.....K.."
        };

        private final String[] CUPCAKE_SPRITE = {
            "....LL....",
            "...LMML...",
            "...LMML...",
            "....NN....",
            "...OOO....",
            "...OPQ....",
            "....QQ....",
            ".........."
        };

        private final String[] BUCKET_SPRITE = {
            "RRRRRRRRRR",
            "RSSSSSSSRR",
            "RSSSSSSSRR",
            "RSSSSSSSRR",
            "RSSSSSSSRR",
            "RRRRRRRRRR",
            "..TTTT....",
            ".........."
        };

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(new Color(28, 24, 34));
            g2.fillRect(0, 0, getWidth(), getHeight());

            drawBackground(g2);
            drawBucket(g2);
            drawPlayer(g2);
            drawCupcakes(g2);
            if (!gameOver && !escaped) {
                drawGoblin(g2);
                drawBossBar(g2);
            }
            if (escaped || gameOver) {
                drawEndingText(g2);
            }

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            int tile = 16;
            for (int y = 0; y < getHeight(); y += tile) {
                for (int x = 0; x < getWidth(); x += tile) {
                    if ((x / tile + y / tile) % 2 == 0) {
                        g2.setColor(new Color(61, 58, 72));
                    } else {
                        g2.setColor(new Color(48, 44, 58));
                    }
                    g2.fillRect(x, y, tile, tile);
                }
            }

            g2.setColor(new Color(70, 60, 45));
            g2.fillRect(0, 118, getWidth(), 80);
            g2.setColor(new Color(86, 72, 54));
            for (int i = 0; i < 20; i++) {
                g2.fillRect(i * 48, 135 + (i % 3) * 6, 18, 8);
            }

            g2.setColor(new Color(55, 70, 90));
            g2.fillRect(0, 145, getWidth(), 8);

            g2.setColor(new Color(255, 180, 70));
            g2.fillRect(220, 20, 180, 10);
            g2.setColor(new Color(90, 210, 110));
            g2.fillRect(220, 20, 180 * playerHealth / 100, 10);

            g2.setColor(new Color(255, 180, 70));
            g2.fillRect(560, 20, 180, 10);
            g2.setColor(new Color(220, 90, 90));
            g2.fillRect(560, 20, 180 * goblinHealth / 100, 10);
        }

        private void drawBucket(Graphics2D g2) {
            int bounce = (animationFrame % 4 == 0) ? 2 : 0;
            drawSprite(g2, BUCKET_SPRITE, 72, 46 + bounce, 4, new Color[] {
                new Color(0, 0, 0, 0), new Color(160, 155, 170), new Color(110, 105, 120),
                new Color(75, 70, 85), new Color(200, 195, 210), new Color(0, 0, 0, 0)
            }, "RSGT" );
        }

        private void drawPlayer(Graphics2D g2) {
            int offsetX = (animationFrame % 2 == 0) ? 0 : 2;
            drawSprite(g2, PLAYER_SPRITE, 270 + offsetX, 56, 4, new Color[] {
                new Color(0, 0, 0, 0), new Color(255, 210, 120), new Color(110, 90, 255),
                new Color(255, 145, 95), new Color(200, 180, 140), new Color(120, 80, 35)
            }, "ABCD E F" );
        }

        private void drawGoblin(Graphics2D g2) {
            int offsetX = (animationFrame % 2 == 0) ? 0 : 3;
            drawSprite(g2, GOBLIN_SPRITE, 610 + offsetX, 56, 4, new Color[] {
                new Color(0, 0, 0, 0), new Color(120, 220, 110), new Color(90, 120, 75),
                new Color(150, 55, 35), new Color(200, 170, 70), new Color(255, 165, 0)
            }, "GHIJK" );
        }

        private void drawCupcakes(Graphics2D g2) {
            int[] xs = { 420, 470, 520 };
            for (int i = 0; i < Math.min(cupcakes, MAX_CUPCAKES); i++) {
                int bob = (animationFrame + i) % 3 == 0 ? 2 : 0;
                drawSprite(g2, CUPCAKE_SPRITE, xs[i], 72 + bob, 3, new Color[] {
                    new Color(0, 0, 0, 0), new Color(255, 245, 215), new Color(255, 175, 220),
                    new Color(120, 70, 55), new Color(255, 123, 220), new Color(255, 255, 255),
                    new Color(255, 210, 85)
                }, "LMNOPQ" );
            }
        }

        private void drawBossBar(Graphics2D g2) {
            g2.setColor(new Color(60, 50, 45));
            g2.fillRect(560, 12, 180, 12);
            g2.setColor(new Color(220, 80, 60));
            g2.fillRect(560, 12, 180 * Math.max(0, goblinHealth) / 100, 12);
            g2.setColor(new Color(255, 220, 180));
            g2.drawString("TWINKLEBERRY", 590, 10);
        }

        private void drawEndingText(Graphics2D g2) {
            g2.setColor(new Color(255, 240, 200));
            g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
            if (escaped) {
                g2.drawString("FREEDOM!", 410, 30);
            } else {
                g2.drawString("THE BUCKET WINS", 390, 30);
            }
        }

        private void drawSprite(Graphics2D g2, String[] sprite, int x, int y, int scale, Color[] palette, String map) {
            for (int row = 0; row < sprite.length; row++) {
                for (int col = 0; col < sprite[row].length(); col++) {
                    char c = sprite[row].charAt(col);
                    if (c == '.') {
                        continue;
                    }

                    int paletteIndex = map.indexOf(c);
                    if (paletteIndex >= 0) {
                        g2.setColor(palette[paletteIndex]);
                    } else {
                        g2.setColor(Color.WHITE);
                    }
                    int px = x + col * scale;
                    int py = y + row * scale;
                    g2.fillRect(px, py, scale, scale);
                }
            }
        }
    }

    private void appendLog(String text) {
        logArea.append(text + System.lineSeparator() + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void resetGame() {
        daysLeft = 5;
        cupcakes = 0;
        energy = 3;
        coins = 0;
        chapter = 1;
        escaped = false;
        gameOver = false;
        ending = "";
        effectTimer = 0;
        goblinHealth = 100;
        playerHealth = 100;
        inventory.clear();
        logArea.setText("");
    }

    private void saveGameAndMessage() {
        saveGame();
        JOptionPane.showMessageDialog(frame, "Game saved successfully.");
    }

    private void resolveChoice(int choice) {
        checkChapterProgress();
        switch (choice) {
            case 1:
                searchRuins();
                break;
            case 2:
                rest();
                break;
            case 3:
                shop();
                break;
            case 4:
                fightBoss();
                break;
            case 5:
                inspectGear();
                break;
            case 6:
                attemptEscape();
                break;
            default:
                appendLog("That is not a valid option.");
        }
    }

    private void checkChapterProgress() {
        if (daysLeft <= 4 && chapter < 2) {
            chapter = 2;
            appendLog("Chapter 2: The Bucket Wakes");
            appendLog("The walls whisper. Something hungry is pacing below you.");
        }
        if (daysLeft <= 3 && chapter < 3) {
            chapter = 3;
            appendLog("Chapter 3: Ruins and Rust");
            appendLog("Broken vending machines and bakery signs litter the ruins.");
        }
        if (daysLeft <= 2 && chapter < 4) {
            chapter = 4;
            appendLog("Chapter 4: The Goblin's Shadow");
            appendLog("Twinkleberry is close. Her candy-star crown glows in the dark.");
        }
        if (daysLeft <= 1 && chapter < 5) {
            chapter = 5;
            appendLog("Chapter 5: Final Gate");
            appendLog("The bucket door is shaking. Freedom is near, but only if you have all the cupcakes.");
        }
    }

    private void searchRuins() {
        if (energy <= 0) {
            appendLog("You are too exhausted to move.");
            return;
        }

        energy--;
        int event = random.nextInt(8);

        switch (event) {
            case 0:
                appendLog("You found a dusty cupcake wrapper. Not food, just sad history.");
                break;
            case 1:
                appendLog("You found a cupcake hidden beneath a busted toaster!");
                addCupcake();
                break;
            case 2:
                appendLog("You found a rusty map fragment. The route out looks a little less impossible.");
                inventory.add("Map of Exit");
                break;
            case 3:
                appendLog("You dug up a magical frosting shield. It hums like a tiny engine.");
                inventory.add("Frosting Shield");
                break;
            case 4:
                appendLog("A pigeon stole your snack budget. You lose 1 coin.");
                coins = Math.max(0, coins - 1);
                break;
            case 5:
                appendLog("You found a hidden stash of frosting and coins.");
                coins += 3;
                addCupcake();
                break;
            case 6:
                appendLog("You found a bakery key hidden in a cracked lunchbox.");
                inventory.add("Bakery Key");
                break;
            default:
                appendLog("You came back empty-handed, but a little wiser and slightly haunted.");
                coins += 1;
                break;
        }
    }

    private void rest() {
        int heal = random.nextInt(2) + 1;
        energy += heal;
        playerHealth = Math.min(100, playerHealth + 10);
        if (energy > 5) {
            energy = 5;
        }
        appendLog("You tuck yourself into the bucket and recover " + heal + " energy and 10 health.");
    }

    private void shop() {
        appendLog("The bakery booth flickers in the dark like a haunted storefront.");
        appendLog("You can buy a cupcake, a power snack, a Map of Exit, or a Frosting Shield.");

        if (coins >= 2) {
            coins -= 2;
            addCupcake();
            appendLog("You bought a cupcake. It smells like victory and sugar.");
        } else {
            appendLog("You don't have enough coins for a cupcake.");
        }

        if (coins >= 3) {
            coins -= 3;
            energy += 2;
            if (energy > 5) {
                energy = 5;
            }
            appendLog("You bought a power snack. Your energy jumps back to life.");
        }

        if (coins >= 5) {
            coins -= 5;
            inventory.add("Map of Exit");
            appendLog("You bought the Map of Exit. The route out becomes clearer.");
        }

        if (coins >= 4) {
            coins -= 4;
            inventory.add("Frosting Shield");
            appendLog("You bought the Frosting Shield. It glows with sugary strength.");
        }
    }

    private void fightBoss() {
        if (cupcakes >= MAX_CUPCAKES && inventory.contains("Map of Exit")) {
            appendLog("Twinkleberry is already defeated. Your route to freedom is open.");
            return;
        }

        effectTimer = 8;

        int goblinPower = 2 + random.nextInt(3);
        int playerPower = 1 + random.nextInt(3) + (cupcakes > 0 ? 1 : 0) + (inventory.contains("Frosting Shield") ? 1 : 0);

        appendLog("Twinkleberry, the Donkey Death Goddess, prances into view in a puff of glitter.");
        appendLog("She demands a cupcake tribute! Power: " + playerPower + " vs Twinkleberry: " + goblinPower);

        if (playerPower >= goblinPower) {
            goblinHealth = Math.max(0, goblinHealth - 35);
            appendLog("You outwit Twinkleberry and recover a cupcake from her enchanted altar.");
            addCupcake();
            coins += 3;
            if (goblinHealth == 0) {
                appendLog("Twinkleberry poofs into harmless glitter and a very offended bray.");
            }
        } else {
            playerHealth = Math.max(0, playerHealth - 20);
            appendLog("Twinkleberry bonks you with a sparkly cupcake wand.");
            energy--;
            coins = Math.max(0, coins - 1);
        }

        if (playerHealth <= 0) {
            gameOver = true;
            ending = "boss";
            appendLog("Your health gives out under the goblin's attack. The bucket wins.");
        }

        if (goblinHealth <= 0) {
            appendLog("The boss is gone. You can now push for the exit.");
        }
    }

    private void inspectGear() {
        appendLog("You inspect your gear carefully.");
        appendLog("Cupcakes needed to escape: " + MAX_CUPCAKES + " | Energy: " + energy + " | Coins: " + coins);
        if (inventory.isEmpty()) {
            appendLog("No special items found yet.");
        } else {
            appendLog("Special items: " + String.join(", ", inventory));
        }
    }

    private void attemptEscape() {
        if (cupcakes < MAX_CUPCAKES) {
            appendLog("You don't have enough cupcakes. The exit stays sealed.");
            return;
        }

        appendLog("The bucket door groans open. The outside world is waiting.");

        if (inventory.contains("Map of Exit") && inventory.contains("Frosting Shield")) {
            ending = "perfect";
            escaped = true;
        } else if (inventory.contains("Map of Exit")) {
            ending = "good";
            escaped = true;
        } else if (inventory.contains("Frosting Shield")) {
            ending = "survival";
            escaped = true;
        } else {
            ending = "bare";
            escaped = true;
        }
    }

    private void addCupcake() {
        if (cupcakes < MAX_CUPCAKES) {
            cupcakes++;
        }
    }

    private String printEndingText() {
        StringBuilder sb = new StringBuilder();
        switch (ending) {
            case "perfect":
                sb.append("PERFECT ENDING\n");
                sb.append("You use the Map of Exit and the Frosting Shield to break through the bucket's pitted door.\n");
                sb.append("The world outside is still ruined, but it is finally yours to walk.\n");
                sb.append("You escape with the cupcakes and a story too strange to forget.");
                break;
            case "good":
                sb.append("GOOD ENDING\n");
                sb.append("You slip through the exit with enough cupcakes to keep going.\n");
                sb.append("The bucket is behind you. The road ahead is still yours to choose.");
                break;
            case "survival":
                sb.append("SURVIVAL ENDING\n");
                sb.append("You get out alive, shield in hand and cupcakes in your pockets.\n");
                sb.append("You are bruised, exhausted, and weirdly glowing with victory.");
                break;
            case "bare":
                sb.append("BARE SURVIVAL ENDING\n");
                sb.append("You make it out with the cupcakes, but not much else.\n");
                sb.append("The world outside is still cold and strange, but the bucket does not follow.");
                break;
            default:
                sb.append("ENDING\n");
                sb.append("You escape the bucket and live to tell the tale.");
                break;
        }
        sb.append("\nCongratulations, mortal. The happy bucket did not win today.");
        return sb.toString();
    }

    private String printBadEndingText() {
        if (ending.equals("exhaustion")) {
            return "BAD ENDING: EXHAUSTED\nYour energy gives out. The bucket grows quiet... then hungry again.";
        }
        if (ending.equals("time")) {
            return "BAD ENDING: TIME UP\nThe days run out. The bucket swallows your chance at freedom.";
        }
        if (ending.equals("boss")) {
            return "BAD ENDING: BONKED\nTwinkleberry overwhelms you with enchanted cupcake magic. The bucket escape ends in a harmless puff of glitter.";
        }
        return "BAD ENDING\nThe bucket wins. The apocalypse continues without you.";
    }

    private void saveGame() {
        try {
            Map<String, String> values = new HashMap<>();
            values.put("daysLeft", String.valueOf(daysLeft));
            values.put("cupcakes", String.valueOf(cupcakes));
            values.put("energy", String.valueOf(energy));
            values.put("coins", String.valueOf(coins));
            values.put("chapter", String.valueOf(chapter));
            values.put("escaped", String.valueOf(escaped));
            values.put("gameOver", String.valueOf(gameOver));
            values.put("ending", ending);
            values.put("goblinHealth", String.valueOf(goblinHealth));
            values.put("playerHealth", String.valueOf(playerHealth));
            values.put("inventory", String.join(";", inventory));

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append(System.lineSeparator());
            }

            Files.write(
                SAVE_FILE,
                sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            appendLog("Save failed: " + e.getMessage());
        }
    }

    private boolean loadGame() {
        if (!Files.exists(SAVE_FILE)) {
            return false;
        }

        try {
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(SAVE_FILE, StandardCharsets.UTF_8)) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    values.put(line.substring(0, idx), line.substring(idx + 1));
                }
            }

            if (values.isEmpty()) {
                return false;
            }

            daysLeft = Integer.parseInt(values.getOrDefault("daysLeft", "5"));
            cupcakes = Integer.parseInt(values.getOrDefault("cupcakes", "0"));
            energy = Integer.parseInt(values.getOrDefault("energy", "3"));
            coins = Integer.parseInt(values.getOrDefault("coins", "0"));
            chapter = Integer.parseInt(values.getOrDefault("chapter", "1"));
            escaped = Boolean.parseBoolean(values.getOrDefault("escaped", "false"));
            gameOver = Boolean.parseBoolean(values.getOrDefault("gameOver", "false"));
            ending = values.getOrDefault("ending", "");
            goblinHealth = Integer.parseInt(values.getOrDefault("goblinHealth", "100"));
            playerHealth = Integer.parseInt(values.getOrDefault("playerHealth", "100"));

            inventory.clear();
            String inventoryLine = values.getOrDefault("inventory", "");
            if (!inventoryLine.isEmpty()) {
                String[] items = inventoryLine.split(";");
                for (String item : items) {
                    if (!item.isBlank()) {
                        inventory.add(item.trim());
                    }
                }
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }
}
