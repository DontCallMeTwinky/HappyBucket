import java.io.ByteArrayOutputStream;
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
    private final GameAudio gameAudio = new GameAudio();

    private int daysLeft = 5;
    private int cupcakes = 0;
    private int energy = 3;
    private int coins = 0;
    private int chapter = 1;
    private boolean escaped = false;
    private boolean gameOver = false;
    private String ending = "";
    private String objective = "Collect 3 cupcakes and reach the gate.";

    private JFrame frame;
    private JTextArea logArea;
    private JLabel titleLabel;
    private JLabel statLabel;
    private JLabel objectiveLabel;
    private JLabel inventoryLabel;
    private JLabel dialogueSpeakerLabel;
    private JLabel dialogueTextLabel;
    private JPanel dialoguePortraitPanel;
    private PixelScenePanel sceneCanvas;
    private final Map<String, WorldNode> worldMap = new HashMap<>();
    private final String[] worldOrder = {"Bucket Hollow", "Mossmarket", "Cinder Keep", "Crumb Dungeon", "Twinkleberry Lair", "Sun Gate"};
    private String currentLocation = "Bucket Hollow";
    private int worldIndex = 0;
    private int playerMapX = 0;
    private int playerMapY = 0;
    private int travelProgress = 0;
    private int travelStartX = 0;
    private int travelStartY = 0;
    private int travelTargetX = 0;
    private int travelTargetY = 0;
    private boolean travelAnimating = false;
    private String pendingTravelTarget = null;
    private WorldNode pendingTravelNode = null;
    private int animationFrame = 0;
    private int effectTimer = 0;
    private boolean introActive = true;
    private int introFrames = 0;
    private int goblinHealth = 100;
    private int playerHealth = 100;
    private int level = 1;
    private int xp = 0;
    private int xpToNext = 12;
    private int statPoints = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new HappyBucketMain().buildWindow();
        });
    }

    private byte[] createToneData(int frequency, int durationMs, double volume) {
        return gameAudio.createToneData(frequency, durationMs, volume);
    }

    private byte[] createBackgroundLoop() {
        return gameAudio.createBackgroundLoop();
    }

    private void playToneEffect(int frequency, int durationMs, double volume) {
        gameAudio.playToneEffect(frequency, durationMs, volume);
    }

    private void startBackgroundMusic() {
        gameAudio.startBackgroundMusic();
    }

    private void stopBackgroundMusic() {
        gameAudio.stopBackgroundMusic();
    }

    private void playSfxForChoice(int choice) {
        switch (choice) {
            case 1:
                playToneEffect(440, 90, 0.16);
                break;
            case 2:
                playToneEffect(320, 110, 0.18);
                break;
            case 3:
                playToneEffect(640, 100, 0.2);
                break;
            case 4:
                playToneEffect(180, 140, 0.22);
                break;
            case 5:
                playToneEffect(540, 75, 0.18);
                break;
            case 6:
                playToneEffect(200, 180, 0.3);
                break;
            default:
                playToneEffect(390, 80, 0.14);
                break;
        }
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
        sceneCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMapClick(e.getX(), e.getY());
            }
        });
        scenePanel.add(sceneCanvas);

        statLabel = new JLabel();
        statLabel.setFont(statLabel.getFont().deriveFont(14f));
        statLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        objectiveLabel = new JLabel();
        objectiveLabel.setFont(objectiveLabel.getFont().deriveFont(13f));
        objectiveLabel.setForeground(new Color(255, 212, 120));

        inventoryLabel = new JLabel();
        inventoryLabel.setFont(inventoryLabel.getFont().deriveFont(14f));

        JPanel dialoguePanel = new JPanel(new java.awt.BorderLayout(12, 0));
        dialoguePanel.setBorder(BorderFactory.createTitledBorder("Dialogue"));
        dialoguePanel.setBackground(new Color(44, 38, 49));

        dialoguePortraitPanel = new JPanel();
        dialoguePortraitPanel.setPreferredSize(new Dimension(72, 72));
        dialoguePortraitPanel.setBackground(new Color(90, 80, 75));
        dialoguePortraitPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 170, 110), 2));

        JPanel dialogueTextWrap = new JPanel();
        dialogueTextWrap.setLayout(new BoxLayout(dialogueTextWrap, BoxLayout.Y_AXIS));
        dialogueTextWrap.setOpaque(false);

        dialogueSpeakerLabel = new JLabel("Bucket");
        dialogueSpeakerLabel.setFont(dialogueSpeakerLabel.getFont().deriveFont(Font.BOLD, 13f));
        dialogueSpeakerLabel.setForeground(new Color(255, 220, 150));

        dialogueTextLabel = new JLabel("The bucket hums in the dark. The wasteland listens.");
        dialogueTextLabel.setFont(dialogueTextLabel.getFont().deriveFont(12f));
        dialogueTextLabel.setForeground(new Color(240, 235, 220));
        dialogueTextLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        dialogueTextWrap.add(dialogueSpeakerLabel);
        dialogueTextWrap.add(dialogueTextLabel);
        dialoguePanel.add(dialoguePortraitPanel, java.awt.BorderLayout.WEST);
        dialoguePanel.add(dialogueTextWrap, java.awt.BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new java.awt.GridLayout(2, 5, 8, 8));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        JButton searchButton = new JButton("Search Ruins");
        JButton restButton = new JButton("Rest");
        JButton shopButton = new JButton("Bakery Shop");
        JButton fightButton = new JButton("Fight Goblin");
        JButton inspectButton = new JButton("Inspect Gear");
        JButton escapeButton = new JButton("Escape");
        JButton craftButton = new JButton("Craft Gear");
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");
        JButton newGameButton = new JButton("New Game");

        searchButton.addActionListener(e -> handleChoice(1));
        restButton.addActionListener(e -> handleChoice(2));
        shopButton.addActionListener(e -> handleChoice(3));
        fightButton.addActionListener(e -> handleChoice(4));
        inspectButton.addActionListener(e -> handleChoice(5));
        escapeButton.addActionListener(e -> handleChoice(6));
        craftButton.addActionListener(e -> handleChoice(7));
        saveButton.addActionListener(e -> saveGameAndMessage());
        loadButton.addActionListener(e -> loadGameAndMessage());
        newGameButton.addActionListener(e -> startNewGame());

        buttonPanel.add(searchButton);
        buttonPanel.add(restButton);
        buttonPanel.add(shopButton);
        buttonPanel.add(fightButton);
        buttonPanel.add(inspectButton);
        buttonPanel.add(escapeButton);
        buttonPanel.add(craftButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
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
        root.add(dialoguePanel);
        root.add(statLabel);
        root.add(objectiveLabel);
        root.add(inventoryLabel);
        root.add(buttonPanel);
        root.add(scrollPane);

        frame.setContentPane(root);
        frame.setVisible(true);
        startBackgroundMusic();
        startNewGame();
    }

    private void showDialogue(String speaker, String text) {
        if (dialogueSpeakerLabel != null) {
            dialogueSpeakerLabel.setText(speaker);
        }
        if (dialogueTextLabel != null) {
            dialogueTextLabel.setText(text);
        }
        if (dialoguePortraitPanel != null) {
            dialoguePortraitPanel.removeAll();
            JPanel face = new JPanel();
            face.setBackground(getPortraitColor(speaker));
            face.setBorder(BorderFactory.createLineBorder(new Color(255, 222, 150), 2));
            face.setPreferredSize(new Dimension(60, 60));
            dialoguePortraitPanel.add(face);
            dialoguePortraitPanel.revalidate();
            dialoguePortraitPanel.repaint();
        }
    }

    private Color getPortraitColor(String speaker) {
        if (speaker == null) {
            return new Color(120, 110, 100);
        }
        switch (speaker.toLowerCase()) {
            case "bucket":
                return new Color(150, 145, 160);
            case "twinkleberry":
                return new Color(130, 180, 120);
            case "trader":
                return new Color(145, 120, 90);
            case "guard":
                return new Color(110, 130, 180);
            default:
                return new Color(150, 120, 110);
        }
    }

    private void startNewGame() {
        resetGame();
        initializeWorldMap();
        currentLocation = "Bucket Hollow";
        worldIndex = 0;
        playerMapX = 0;
        playerMapY = 0;
        travelAnimating = false;
        pendingTravelTarget = null;
        pendingTravelNode = null;
        introActive = true;
        introFrames = 0;
        showDialogue("Bucket", "The bucket hums in the dark. The wasteland listens.");
        objective = "Collect 3 cupcakes and reach the gate.";
        appendLog("You wake up beneath a rusted steel bucket. The world is over. The bucket is hungry.");
        appendLog("You need 3 cupcakes before you can escape.");
        appendLog("The ruined kingdom stretches outward as a clickable isometric map of towns and dungeons.");
        appendLog("Search for scraps, rest, buy supplies, craft gear, and outwit Twinkleberry.");
        startBackgroundMusic();
        playToneEffect(520, 180, 0.18);
        refreshStats();
        refreshScene();
    }

    private void initializeWorldMap() {
        worldMap.clear();
        worldMap.put("Bucket Hollow", new WorldNode("Bucket Hollow", "town", 0, 0, "The bucket camp where you started."));
        worldMap.put("Mossmarket", new WorldNode("Mossmarket", "town", 1, 0, "A mossy market with bakers and wandering traders."));
        worldMap.put("Cinder Keep", new WorldNode("Cinder Keep", "town", 2, 1, "A ruined fortress full of heat, beer, and rumor."));
        worldMap.put("Crumb Dungeon", new WorldNode("Crumb Dungeon", "dungeon", 2, 2, "A descending bakery ruin packed with traps and cake monsters."));
        worldMap.put("Twinkleberry Lair", new WorldNode("Twinkleberry Lair", "dungeon", 3, 1, "A glittering lair where the donkey queen rules the ruin."));
        worldMap.put("Sun Gate", new WorldNode("Sun Gate", "exit", 4, 1, "The final gate to the outer world."));
    }

    private void handleMapClick(int mouseX, int mouseY) {
        if (travelAnimating) {
            return;
        }

        for (Map.Entry<String, WorldNode> entry : worldMap.entrySet()) {
            WorldNode node = entry.getValue();
            int[] screen = projectToScreen(node.getX(), node.getY());
            int dx = mouseX - screen[0];
            int dy = mouseY - screen[1];
            if (dx * dx + dy * dy <= 225) {
                travelToNode(entry.getKey());
                return;
            }
        }
    }

    private void travelToNode(String target) {
        if (travelAnimating) {
            return;
        }

        if (target.equals(currentLocation)) {
            appendLog("You are already standing in " + target + ".");
            return;
        }

        WorldNode node = worldMap.get(target);
        if (node == null) {
            return;
        }

        travelStartX = playerMapX;
        travelStartY = playerMapY;
        travelTargetX = node.getX();
        travelTargetY = node.getY();
        travelProgress = 0;
        travelAnimating = true;
        pendingTravelTarget = target;
        pendingTravelNode = node;
        appendLog("You begin traveling toward " + node.getName() + ".");
    }

    private void advanceTravelAnimation() {
        if (!travelAnimating) {
            return;
        }

        travelProgress++;
        double t = Math.min(1.0, travelProgress / 10.0);
        double eased = 1.0 - Math.pow(1.0 - t, 3);
        playerMapX = (int) Math.round(travelStartX + (travelTargetX - travelStartX) * eased);
        playerMapY = (int) Math.round(travelStartY + (travelTargetY - travelStartY) * eased);

        if (travelProgress >= 10) {
            currentLocation = pendingTravelTarget;
            for (int i = 0; i < worldOrder.length; i++) {
                if (worldOrder[i].equals(pendingTravelTarget)) {
                    worldIndex = i;
                    break;
                }
            }
            appendLog("You arrive in " + pendingTravelNode.getName() + ".");
            appendLog(pendingTravelNode.getDescription());
            triggerLocationEncounter(pendingTravelNode);
            travelAnimating = false;
            pendingTravelTarget = null;
            pendingTravelNode = null;
            refreshStats();
        }
    }

    private void triggerLocationEncounter(WorldNode node) {
        if ("Bucket Hollow".equals(node.getName())) {
            energy = Math.min(5, energy + 1);
            playerHealth = Math.min(100, playerHealth + 8);
            showDialogue("Bucket", "The campfire crackles. You are still here, still hungry, and still moving.");
            appendLog("You settle by the bucket campfire and recover a little strength.");
            return;
        }

        if ("Mossmarket".equals(node.getName())) {
            coins += 2;
            if (!inventory.contains("Map of Exit")) {
                inventory.add("Map of Exit");
                showDialogue("Trader", "The mossy trader grins. 'The road out is crooked, but it is still a road.'");
                appendLog("A mossy trader slips you a route map and a few extra coins.");
            } else {
                showDialogue("Trader", "'Warm bread for the road, and a coin for your troubles.'");
                appendLog("The market stalls are quiet, but the locals still share a warm loaf and 2 coins.");
            }
            return;
        }

        if ("Cinder Keep".equals(node.getName())) {
            int reward = random.nextInt(3) + 1;
            coins += reward;
            if (reward >= 2) {
                inventory.add("Frosting Shield");
                appendLog("The ruined keep gifts you a heated frosting shield and " + reward + " coins.");
            } else {
                appendLog("You warm your hands at the cinder hearth and pocket " + reward + " coins.");
            }
            return;
        }

        if ("Crumb Dungeon".equals(node.getName())) {
            energy = Math.max(1, energy - 1);
            int event = random.nextInt(3);
            if (event == 0) {
                addCupcake();
                appendLog("You crack open a sugar vault and recover a cupcake from the dungeon wall.");
            } else if (event == 1) {
                inventory.add("Sugar Bomb");
                appendLog("A hidden bakery bomb cache is discovered. You pocket a Sugar Bomb.");
            } else {
                gainXp(6);
                appendLog("The dungeon tests your grit, but you leave with a strange sense of purpose.");
            }
            return;
        }

        if ("Twinkleberry Lair".equals(node.getName())) {
            energy = Math.max(1, energy - 1);
            goblinHealth = Math.max(30, goblinHealth);
            showDialogue("Twinkleberry", "'Bray! Your cupcakes belong to the glitter throne.'");
            appendLog("Twinkleberry's lair erupts in glitter flashes. The donkey queen is waiting for combat.");
            return;
        }

        if ("Sun Gate".equals(node.getName())) {
            objective = "Reach the gate with enough cupcakes to leave the wasteland behind.";
            appendLog("The Sun Gate glows with a bitter, hopeful light.");
            if (cupcakes >= MAX_CUPCAKES) {
                appendLog("The gate recognizes your victory. Your route home is almost open.");
            }
        }
    }

    private void handleChoice(int choice) {
        if (gameOver || escaped) {
            appendLog("The run is already over. Start a new game to play again.");
            return;
        }

        if (choice == 7) {
            craftGear();
            playSfxForChoice(7);
            return;
        }

        playSfxForChoice(choice);
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
        statLabel.setText("Level " + level + " | XP " + xp + "/" + xpToNext + " | Chapter " + chapter + " | Location: " + currentLocation + " | Days left: " + daysLeft + " | Cupcakes: " + cupcakes + "/" + MAX_CUPCAKES + " | Energy: " + energy + " | Health: " + playerHealth + " | Coins: " + coins);
        objectiveLabel.setText("Objective: " + objective + (statPoints > 0 ? " | Unspent upgrades: " + statPoints : ""));
        inventoryLabel.setText("Inventory: " + (inventory.isEmpty() ? "empty" : String.join(", ", inventory)) + " | Zone: " + currentLocation);
    }

    private void refreshScene() {
        if (sceneCanvas != null) {
            if (travelAnimating) {
                advanceTravelAnimation();
            }
            if (introActive) {
                introFrames++;
                if (introFrames >= 90) {
                    introActive = false;
                }
            }
            animationFrame = (animationFrame + 1) % 8;
            if (effectTimer > 0) {
                effectTimer--;
            }
            sceneCanvas.repaint();
        }
    }

    private int[] projectToScreen(int gridX, int gridY) {
        int tileW = 30;
        int tileH = 18;
        int originX = 120;
        int originY = 60;
        int x = originX + (gridX - gridY) * tileW / 2;
        int y = originY + (gridX + gridY) * tileH / 2;
        return new int[] {x, y};
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
            ".GHHHHHGG.",
            "..GIIIGG..",
            "..GJJJGG..",
            ".KKLLLKK..",
            "..M...M...",
            ".N.....N.."
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
            if (introActive) {
                drawIntroOverlay(g2);
            }

            g2.dispose();
        }

        private void drawIntroOverlay(Graphics2D g2) {
            int alpha = Math.max(0, 255 - introFrames * 2);
            g2.setColor(new Color(15, 12, 18, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 220, 120, alpha));
            g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 26));
            g2.drawString("HAPPY BUCKET:", 250, 70);
            g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 18));
            g2.drawString("LAST CUPCAKE", 335, 95);

            g2.setColor(new Color(240, 235, 220, alpha));
            g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
            g2.drawString("The wasteland has opened. The bucket has a hunger. The cupcakes are the key.", 100, 130);
            g2.drawString("Explore towns, gather scraps, and survive the donkey queen.", 170, 150);
        }

        private void drawMapNodeLabel(Graphics2D g2, WorldNode node, int x, int y, boolean active) {
            g2.setColor(active ? new Color(255, 234, 170) : new Color(220, 220, 220));
            g2.drawString(node.getName(), x + 12, y - 12);
            if (active) {
                g2.setColor(new Color(255, 218, 110));
                g2.fillOval(x - 3, y - 3, 6, 6);
            }
        }

        private void drawBackground(Graphics2D g2) {
            int tileW = 30;
            int tileH = 18;
            int originX = 120;
            int originY = 60;

            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 9; col++) {
                    int x = originX + (col - row) * tileW / 2;
                    int y = originY + (col + row) * tileH / 2;
                    Color top = ((col + row) % 2 == 0) ? new Color(81, 93, 78) : new Color(68, 78, 65);
                    Color left = new Color(52, 63, 52);
                    Color right = new Color(97, 110, 94);
                    drawIsoTile(g2, x, y, tileW, tileH, top, left, right);
                }
            }

            g2.setColor(new Color(255, 200, 100));
            g2.fillRect(200, 18, 180, 10);
            g2.setColor(new Color(110, 220, 120));
            g2.fillRect(200, 18, 180 * playerHealth / 100, 10);

            g2.setColor(new Color(255, 200, 100));
            g2.fillRect(560, 18, 180, 10);
            g2.setColor(new Color(220, 90, 90));
            g2.fillRect(560, 18, 180 * goblinHealth / 100, 10);
        }

        private void drawIsoTile(Graphics2D g2, int x, int y, int w, int h, Color top, Color left, Color right) {
            int[] xs = {x, x + w / 2, x + w, x + w / 2, x};
            int[] ys = {y, y + h / 2, y, y - h / 2, y};
            g2.setColor(top);
            g2.fillPolygon(xs, ys, 5);

            int[] leftXs = {x, x + w / 2, x + w / 2, x};
            int[] leftYs = {y, y + h / 2, y + h / 2 + h / 2, y + h};
            g2.setColor(left);
            g2.fillPolygon(leftXs, leftYs, 4);

            int[] rightXs = {x + w, x + w, x + w / 2, x + w / 2};
            int[] rightYs = {y, y + h, y + h / 2 + h / 2, y + h / 2};
            g2.setColor(right);
            g2.fillPolygon(rightXs, rightYs, 4);
        }

        private void drawBucket(Graphics2D g2) {
            int[] bucketScreen = projectToScreen(0, 0);
            int bounce = (animationFrame % 4 == 0) ? 4 : 0;
            drawIsoMarker(g2, bucketScreen[0], bucketScreen[1] + bounce, 32, 20, new Color(160, 155, 170), new Color(110, 105, 120), new Color(75, 70, 85));
        }

        private void drawPlayer(Graphics2D g2) {
            int[] playerScreen = projectToScreen(playerMapX, playerMapY);
            int offsetX = (animationFrame % 2 == 0) ? 0 : 4;
            drawIsoMarker(g2, playerScreen[0] + offsetX, playerScreen[1] - 6, 26, 18, new Color(255, 210, 120), new Color(110, 90, 255), new Color(255, 145, 95));
        }

        private void drawGoblin(Graphics2D g2) {
            int[] goblinScreen = projectToScreen(3, 1);
            int offsetX = (animationFrame % 2 == 0) ? 0 : 5;
            drawIsoMarker(g2, goblinScreen[0] + offsetX, goblinScreen[1] - 6, 26, 18, new Color(120, 220, 110), new Color(90, 120, 75), new Color(150, 55, 35));
        }

        private void drawCupcakes(Graphics2D g2) {
            int[] xs = { 320, 380, 445 };
            for (int i = 0; i < Math.min(cupcakes, MAX_CUPCAKES); i++) {
                int bob = (animationFrame + i) % 3 == 0 ? 3 : 0;
                drawIsoMarker(g2, xs[i], 105 + bob, 16, 14, new Color(255, 245, 215), new Color(255, 175, 220), new Color(255, 210, 85));
            }
        }

        private void drawIsoMarker(Graphics2D g2, int x, int y, int w, int h, Color body, Color accent, Color trim) {
            int halfW = w / 2;
            int halfH = h / 2;
            int[] diamondX = {x, x + halfW, x, x - halfW};
            int[] diamondY = {y - halfH, y, y + halfH, y};
            g2.setColor(body);
            g2.fillPolygon(diamondX, diamondY, 4);
            g2.setColor(accent);
            g2.fillOval(x - 5, y - 5, 10, 10);
            g2.setColor(trim);
            g2.fillRect(x - 3, y + 5, 6, 8);
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
                    if (paletteIndex >= 0 && paletteIndex < palette.length) {
                        g2.setColor(palette[paletteIndex]);
                    } else {
                        continue;
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
        objective = "Collect 3 cupcakes and reach the gate.";
        effectTimer = 0;
        goblinHealth = 100;
        playerHealth = 100;
        level = 1;
        xp = 0;
        xpToNext = 12;
        statPoints = 0;
        inventory.clear();
        logArea.setText("");
    }

    private boolean hasItem(String itemName) {
        return inventory.contains(itemName);
    }

    private void saveGameAndMessage() {
        saveGame();
        appendLog("Game saved successfully.");
    }

    private void loadGameAndMessage() {
        if (!loadGame()) {
            appendLog("No saved game was found.");
            return;
        }
        appendLog("You return to the last saved moment beneath the bucket.");
        refreshStats();
        refreshScene();
        appendLog("Game loaded successfully.");
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
            case 7:
                craftGear();
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
        if (event == 1 || event == 5) {
            playToneEffect(660, 110, 0.25);
        }

        switch (event) {
            case 0:
                appendLog("You found a dusty cupcake wrapper. Not food, just sad history.");
                gainXp(2);
                break;
            case 1:
                appendLog("You found a cupcake hidden beneath a busted toaster!");
                addCupcake();
                gainXp(5);
                break;
            case 2:
                appendLog("You found a rusty map fragment. The route out looks a little less impossible.");
                inventory.add("Map of Exit");
                gainXp(4);
                break;
            case 3:
                appendLog("You dug up a magical frosting shield. It hums like a tiny engine.");
                inventory.add("Frosting Shield");
                gainXp(6);
                break;
            case 4:
                appendLog("A pigeon stole your snack budget. You lose 1 coin.");
                coins = Math.max(0, coins - 1);
                gainXp(1);
                break;
            case 5:
                appendLog("You found a hidden stash of frosting and coins.");
                coins += 3;
                addCupcake();
                gainXp(7);
                break;
            case 6:
                appendLog("You found a bakery key hidden in a cracked lunchbox.");
                inventory.add("Bakery Key");
                gainXp(4);
                break;
            default:
                appendLog("You came back empty-handed, but a little wiser and slightly haunted.");
                coins += 1;
                gainXp(2);
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
        playToneEffect(300, 200, 0.2);
        gainXp(3);
        appendLog("You tuck yourself into the bucket and recover " + heal + " energy and 10 health.");
    }

    private void shop() {
        appendLog("The bakery booth flickers in the dark like a haunted storefront.");
        appendLog("You can buy a cupcake, a power snack, a Map of Exit, or a Frosting Shield.");

        if (coins >= 2 && cupcakes < MAX_CUPCAKES) {
            coins -= 2;
            addCupcake();
            gainXp(6);
            appendLog("You bought a cupcake. It smells like victory and sugar.");
        } else if (coins >= 2 && cupcakes >= MAX_CUPCAKES) {
            appendLog("You already have enough cupcakes to escape. The booth is disappointed, but practical.");
        } else {
            appendLog("You don't have enough coins for a cupcake.");
        }

        if (coins >= 3 && energy < 5) {
            coins -= 3;
            energy += 2;
            if (energy > 5) {
                energy = 5;
            }
            gainXp(4);
            appendLog("You bought a power snack. Your energy jumps back to life.");
        } else if (coins >= 3) {
            appendLog("You are already brimming with energy. The snack cabinet closes on a sigh.");
        }

        if (coins >= 5 && !inventory.contains("Map of Exit")) {
            coins -= 5;
            inventory.add("Map of Exit");
            gainXp(8);
            appendLog("You bought the Map of Exit. The route out becomes clearer.");
        } else if (coins >= 5) {
            appendLog("The Map of Exit is already yours. The bakery won't sell a duplicate.");
        }

        if (coins >= 4 && !inventory.contains("Frosting Shield")) {
            coins -= 4;
            inventory.add("Frosting Shield");
            gainXp(8);
            appendLog("You bought the Frosting Shield. It glows with sugary strength.");
        } else if (coins >= 4) {
            appendLog("The Frosting Shield is already humming at your side.");
        }
    }

    private void craftGear() {
        if (energy <= 0) {
            appendLog("You are too tired to craft.");
            return;
        }

        if (inventory.contains("Sugar Bomb")) {
            appendLog("Your workbench is already loaded with Sugar Bombs.");
            return;
        }

        if (coins < 2) {
            appendLog("You need 2 coins and some scrap to craft a Sugar Bomb.");
            return;
        }

        energy--;
        coins -= 2;
        inventory.add("Sugar Bomb");
        playerHealth = Math.min(100, playerHealth + 15);
        gainXp(9);
        objective = "Gather the final cupcake and reach the gate.";

        appendLog("You hammer together a Sugar Bomb from scrap metal and icing jars.");
        appendLog("It crackles with sugar-fueled power. Your health rises a little.");
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
            gainXp(10);
            if (goblinHealth == 0) {
                appendLog("Twinkleberry poofs into harmless glitter and a very offended bray.");
            }
        } else {
            playerHealth = Math.max(0, playerHealth - 20);
            appendLog("Twinkleberry bonks you with a sparkly cupcake wand.");
            energy--;
            coins = Math.max(0, coins - 1);
            gainXp(3);
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
        appendLog("Current objective: " + objective);
    }

    private void attemptEscape() {
        if (cupcakes < MAX_CUPCAKES) {
            appendLog("You don't have enough cupcakes. The exit stays sealed.");
            objective = "Collect 3 cupcakes and reach the gate.";
            return;
        }

        objective = "Escape the bucket with your spoils.";
        appendLog("The bucket door groans open. The outside world is waiting.");
        playToneEffect(780, 300, 0.25);

        if (inventory.contains("Map of Exit") && inventory.contains("Frosting Shield") && inventory.contains("Sugar Bomb")) {
            ending = "perfect";
            escaped = true;
        } else if (inventory.contains("Map of Exit") && inventory.contains("Frosting Shield")) {
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

    private void gainXp(int amount) {
        if (amount <= 0) {
            return;
        }

        xp += amount;
        while (xp >= xpToNext) {
            xp -= xpToNext;
            level++;
            xpToNext = 12 + (level - 1) * 8;
            statPoints++;
            playerHealth = Math.min(100 + level * 5, playerHealth + 15);
            energy = Math.min(5 + level / 2, energy + 1);
            appendLog("LEVEL UP! You are now level " + level + ". Health and energy surge forward.");
        }
        refreshStats();
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
            values.put("objective", objective);
            values.put("level", String.valueOf(level));
            values.put("xp", String.valueOf(xp));
            values.put("xpToNext", String.valueOf(xpToNext));
            values.put("statPoints", String.valueOf(statPoints));
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
            objective = values.getOrDefault("objective", "Collect 3 cupcakes and reach the gate.");
            level = Integer.parseInt(values.getOrDefault("level", "1"));
            xp = Integer.parseInt(values.getOrDefault("xp", "0"));
            xpToNext = Integer.parseInt(values.getOrDefault("xpToNext", "12"));
            statPoints = Integer.parseInt(values.getOrDefault("statPoints", "0"));
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
