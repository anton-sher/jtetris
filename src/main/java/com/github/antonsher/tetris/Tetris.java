package com.github.antonsher.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class Tetris {
    int[][][] TETRAS = new int[][][]{
            {{3, 19}, {4, 19}, {5, 19}, {6, 19}, {1, 0}}, // hor. stick
            {{5, 16}, {5, 17}, {5, 18}, {5, 19}, {1, 0}}, // ver. stick
            {{4, 19}, {5, 19}, {4, 18}, {5, 18}, {2, 0}}, // square
            {{4, 19}, {5, 19}, {6, 19}, {5, 18}, {3, 0}}, // pin 1
            {{4, 18}, {5, 18}, {6, 18}, {5, 19}, {3, 0}}, // pin 2
            {{5, 17}, {5, 18}, {5, 19}, {4, 18}, {3, 0}}, // pin 3
            {{4, 17}, {4, 18}, {4, 19}, {5, 18}, {3, 0}}, // pin 4
            {{4, 17}, {4, 18}, {4, 19}, {5, 17}, {4, 0}}, // right leg 1
            {{4, 18}, {5, 18}, {6, 18}, {6, 19}, {4, 0}}, // right leg 2
            {{5, 17}, {5, 18}, {5, 19}, {4, 19}, {4, 0}}, // right leg 3
            {{4, 19}, {5, 19}, {6, 19}, {4, 18}, {4, 0}}, // right leg 4
            {{5, 17}, {5, 18}, {5, 19}, {4, 17}, {5, 0}}, // left leg 1
            {{4, 19}, {5, 19}, {6, 19}, {6, 18}, {5, 0}}, // left leg 2
            {{4, 17}, {4, 18}, {4, 19}, {5, 19}, {5, 0}}, // left leg 3
            {{4, 18}, {5, 18}, {6, 18}, {4, 19}, {5, 0}}, // left leg 4
            {{4, 18}, {5, 18}, {5, 19}, {6, 19}, {6, 0}}, // z 1
            {{4, 18}, {4, 19}, {5, 17}, {5, 18}, {6, 0}}, // z 2
            {{4, 19}, {5, 19}, {5, 18}, {6, 18}, {7, 0}}, // reverse z 1
            {{4, 17}, {4, 18}, {5, 18}, {5, 19}, {7, 0}}, // reverse z 2
    };

    int CELL_SIZE = 32;
    Color[] COLORS = {Color.BLACK, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.RED, Color.CYAN, Color.ORANGE};
    int[] SCORES = {40, 100, 300, 1200};
    File SCORES_FILE = new File(System.getProperty("user.home"), "tetris-highscores.txt");
    Pattern SCORE_LINE = Pattern.compile("(\\d{1,9});(.*)");
    private int[][] board = new int[10][20];
    private int[][] next = new int[5][2];
    private int[][] tetra = new int[5][2];
    private int height = CELL_SIZE * 20 + 21;
    private int boardWidth = CELL_SIZE * 10 + 11;
    private int statsWidth = 120;
    private int width = boardWidth + statsWidth;
    private Random random = new Random();
    private List<Object[]> highScores = readScores();
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private int score = 0;
    private int lines = 0;
    private int level = 0;
    private int LINES_PER_LEVEL = 20;
    private int tillNext = LINES_PER_LEVEL;
    private State state = State.PLACE_NEXT;

    enum State {
        PLACE_NEXT,
        LET_USER_MOVE,
        MOVE_DOWN,
        LOST,
    }

    public void runGame() throws InterruptedException {
        JFrame tetris = new JFrame("Tetris");
        tetris.setLayout(null);
        tetris.setBackground(Color.GRAY);

        JPanel boardPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                for (int i = 0; i < 4; i++) {
                    int[] tile = tetra[i];
                    int x = tile[0];
                    int y = tile[1];
                    g.setColor(COLORS[tetra[4][0]]);
                    g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
                }
                for (int x = 0; x < board.length; x++) {
                    for (int y = 0; y < board[0].length; y++) {
                        if (board[x][y] > 0) {
                            g.setColor(COLORS[board[x][y]]);
                            g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
                        }
                    }
                }
            }
        };
        boardPanel.setBackground(Color.BLACK);

        tetris.add(boardPanel);

        JPanel statsPanel = new JPanel(null);
        tetris.add(statsPanel);

        JPanel nextPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (int i = 0; i < 4; i++) {
                    minX = Math.min(next[i][0], minX);
                    maxX = Math.max(next[i][0], maxX);
                    maxY = Math.max(next[i][1], maxY);
                }
                g.setColor(COLORS[next[4][0]]);
                for (int i = 0; i < 4; i++) {
                    int x = next[i][0] - minX;
                    int y = maxY - next[i][1];
                    if (maxX - minX < 2)
                        x++;
                    g.fillRect(1 + x * 21, 1 + y * 21, 20, 20);
                }
            }
        };
        statsPanel.add(nextPanel);

        JLabel scoreLabel = new JLabel("Score");
        statsPanel.add(scoreLabel);
        JTextField scoreField = new JTextField("0");
        scoreField.setEditable(false);
        statsPanel.add(scoreField);
        JLabel linesLabel = new JLabel("Lines");
        statsPanel.add(linesLabel);
        JTextField linesField = new JTextField("0");
        linesField.setEditable(false);
        statsPanel.add(linesField);
        JLabel levelLabel = new JLabel("Level");
        statsPanel.add(levelLabel);
        JTextField levelField = new JTextField("0");
        levelField.setEditable(false);
        statsPanel.add(levelField);
        JLabel gameOverLabel = new JLabel("Game over");
        JLabel pauseLabel = new JLabel("Pause");
        JButton newGameButton = new JButton("New game");
        JLabel highScoresLabel = new JLabel("High scores");
        statsPanel.add(highScoresLabel);
        JTextArea highScoresArea = new JTextArea();
        highScoresArea.setEditable(false);
        highScoresArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        highScoresArea.setText(makeScoresText(highScores));
        statsPanel.add(highScoresArea);
        AtomicBoolean newGame = new AtomicBoolean();
        AtomicBoolean pause = new AtomicBoolean();
        newGameButton.addActionListener(e -> {
            lock.lock();
            try {
                newGame.set(true);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });

        Runnable boundsUpdater = () -> {
            boardPanel.setBounds(0, 0, boardWidth, height);
            statsPanel.setBounds(boardWidth, 0, statsWidth, height);
            int y = 10;
            nextPanel.setBounds(10, y, 85, 85);
            y += 90;
            scoreLabel.setBounds(10, y, 80, 24);
            y += 30;
            scoreField.setBounds(10, y, 80, 24);
            y += 30;
            linesLabel.setBounds(10, y, 80, 24);
            y += 30;
            linesField.setBounds(10, y, 80, 24);
            y += 30;
            levelLabel.setBounds(10, y, 80, 24);
            y += 30;
            levelField.setBounds(10, y, 80, 24);
            y += 30;
            gameOverLabel.setBounds(10, y, 80, 24);
            pauseLabel.setBounds(10, y, 80, 24);
            y += 30;
            newGameButton.setBounds(10, y, 100, 24);
            y += 30;
            highScoresLabel.setBounds(10, y, 100, 24);
            y += 30;
            highScoresArea.setBounds(10, y, 100, 160);
            tetris.setSize(new Dimension(width, height + 20));
        };

        tetris.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                updateBounds();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                super.componentMoved(e);
                updateBounds();
            }

            private void updateBounds() {
                boundsUpdater.run();
            }
        });

        boardPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    pause.set(!pause.get());
                    if (!pause.get()) {
                        statsPanel.remove(pauseLabel);
                        lock.lock();
                        try {
                            condition.signalAll();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        statsPanel.add(pauseLabel);
                    }
                    tetris.repaint();
                }
                if (pause.get()) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if (canMoveLeft(board, tetra)) {
                            for (int i = 0; i < 4; i++) {
                                int[] tile = tetra[i];
                                tile[0]--;
                            }
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (canMoveRight(board, tetra)) {
                            for (int i = 0; i < 4; i++) {
                                int[] tile = tetra[i];
                                tile[0]++;
                            }
                        }
                        break;
                    case KeyEvent.VK_UP:
                        int[][] rotated = rotate(tetra);
                        if (canPlace(board, rotated)) {
                            System.arraycopy(rotated, 0, tetra, 0, 4);
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (canMoveDown(board, tetra)) {
                            for (int i = 0; i < 4; i++) {
                                int[] tile = tetra[i];
                                tile[1]--;
                            }
                        }
                        break;
                    case KeyEvent.VK_SPACE:
                        while (canMoveDown(board, tetra)) {
                            for (int i = 0; i < 4; i++) {
                                int[] tile = tetra[i];
                                tile[1]--;
                            }
                        }
                        lock.lock();
                        try {
                            condition.signalAll();
                        } finally {
                            lock.unlock();
                        }
                        break;
                }
                tetris.repaint();
            }
        });

        boardPanel.setFocusable(true);
        boardPanel.setFocusTraversalKeysEnabled(false);
        boardPanel.requestFocus();

        tetris.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        tetris.setLocation(dim.width / 2 - width / 2, dim.height / 2 - height / 2);

        tetris.setVisible(true);

        generateNext();
        while (true) {
            switch (state) {
                case PLACE_NEXT:
                    if (canPlace(board, next)) {
                        for (int i = 0; i < next.length; i++) {
                            int[] tile = next[i];
                            tetra[i][0] = tile[0];
                            tetra[i][1] = tile[1];
                        }
                        generateNext();
                        state = State.LET_USER_MOVE;
                    } else {
                        set(board, next);
                        state = State.LOST;
                        SwingUtilities.invokeLater(() -> {
                            if (score > 0 && (highScores.size() < 10 || (int) highScores.get(0)[0] < score)) {
                                String name = (String) JOptionPane.showInputDialog(tetris, "Enter your name", "High score!", JOptionPane.QUESTION_MESSAGE, null, null, null);
                                highScores.add(0, new Object[]{score, name == null ? "" : name});
                                sortScores(highScores);
                                if (highScores.size() > 10) {
                                    highScores.remove(0);
                                }
                                highScoresArea.setText(makeScoresText(highScores));
                                writeScores(highScores);
                            }

                            statsPanel.add(gameOverLabel);
                            statsPanel.add(newGameButton);
                            boundsUpdater.run();
                        });
                        SwingUtilities.invokeLater(tetris::repaint);
                    }
                    break;
                case LET_USER_MOVE:
                    boardPanel.requestFocus();
                    lock.lock();
                    try {
                        condition.await(1000 / (level + 1), TimeUnit.MILLISECONDS);
                        while (pause.get()) {
                            lock.lock();
                            try {
                                condition.await(50, TimeUnit.MILLISECONDS);
                            } finally {
                                lock.unlock();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                    state = State.MOVE_DOWN;
                    break;
                case MOVE_DOWN:
                    if (canMoveDown(board, tetra)) {
                        for (int i = 0; i < 4; i++) {
                            int[] tile = tetra[i];
                            tile[1]--;
                        }
                        state = State.LET_USER_MOVE;
                    } else {
                        set(board, tetra);
                        int y = 0;
                        while (y < 20) {
                            boolean full = true;
                            for (int x = 0; x < 10; x++) {
                                if (board[x][y] == 0) {
                                    full = false;
                                    break;
                                }
                            }
                            int atOnce = 0;
                            if (full) {
                                atOnce++;
                                for (int x = 0; x < 10; x++) {
                                    board[x][y] = 0;
                                    System.arraycopy(board[x], y + 1, board[x], y, 19 - y);
                                    board[x][19] = 0;
                                }
                            } else {
                                y++;
                            }
                            if (atOnce > 0) {
                                lines += atOnce;
                                tillNext -= atOnce;
                                score += SCORES[atOnce - 1] * (level + 1);
                                SwingUtilities.invokeLater(() -> {
                                    scoreField.setText("" + score);
                                    linesField.setText("" + lines);
                                    boundsUpdater.run();
                                    tetris.repaint();
                                });
                            }
                            if (tillNext <= 0) {
                                tillNext += LINES_PER_LEVEL;
                                level++;
                                SwingUtilities.invokeLater(() -> {
                                    levelField.setText("" + level);
                                    boundsUpdater.run();
                                    tetris.repaint();
                                });
                            }
                        }
                        state = State.PLACE_NEXT;
                    }
                    break;
                case LOST:
                    lock.lock();
                    try {
                        condition.await(500, TimeUnit.MILLISECONDS);
                    } finally {
                        lock.unlock();
                    }
                    if (newGame.get()) {
                        for (int x = 0; x < 10; x++) {
                            for (int y = 0; y < 20; y++) {
                                board[x][y] = 0;
                            }
                        }
                        generateNext();
                        state = State.PLACE_NEXT;
                        score = 0;
                        lines = 0;
                        level = 0;
                        tillNext = LINES_PER_LEVEL;
                        SwingUtilities.invokeLater(() -> {
                            scoreField.setText("0");
                            linesField.setText("0");
                            levelField.setText("0");
                            statsPanel.remove(gameOverLabel);
                            statsPanel.remove(newGameButton);
                            boundsUpdater.run();
                            tetris.repaint();
                        });
                        newGame.set(false);
                        boardPanel.requestFocus();
                    }
                    break;
            }
            SwingUtilities.invokeLater(boardPanel::repaint);
        }
    }

    private void generateNext() {
        int[][] tetra1 = TETRAS[random.nextInt(TETRAS.length)];

        for (int i = 0; i < 5; i++) {
            next[i][0] = tetra1[i][0];
            next[i][1] = tetra1[i][1];
        }
    }

    private String makeScoresText(List<Object[]> highScores) {
        if (highScores.isEmpty()) {
            return "";
        }
        int max = 0;
        for (Object[] highScore : highScores) {
            max = Math.max((int) highScore[0], max);
        }

        String format = "%" + (Integer.toString(max).length()) + "d %s\n";
        String scores = "";
        for (Object[] score : highScores) {
            scores = String.format(format, score[0], score[1]) + scores;
        }

        return scores;
    }

    private List<Object[]> readScores() {
        List<Object[]> result = new ArrayList<>();
        if (!SCORES_FILE.exists()) {
            return result;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(SCORES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = SCORE_LINE.matcher(line);
                if (m.matches()) {
                    result.add(new Object[]{Integer.valueOf(m.group(1)), m.group(2)});
                    sortScores(result);
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
    }

    private void sortScores(List<Object[]> result) {
        result.sort((o1, o2) -> Integer.compare((int) o1[0], (int) o2[0]));
    }

    private void writeScores(List<Object[]> scores) {
        try (PrintStream ps = new PrintStream(SCORES_FILE)) {
            for (Object[] e : scores) {
                ps.println(e[0] + ";" + e[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private int[][] rotate(int[][] tetra) {
        int xMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            xMin = Math.min(xMin, tile[0]);
            xMax = Math.max(xMax, tile[0]);
            yMin = Math.min(yMin, tile[1]);
            yMax = Math.max(yMax, tile[1]);
        }
        int xCenter = (xMin + xMax + 1) / 2;
        int yCenter = (yMin + yMax) / 2;

        int[][] rotated = new int[5][2];
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            rotated[i][0] = xCenter + yCenter - tile[1];
            rotated[i][1] = yCenter + tile[0] - xCenter;
        }
        rotated[4][0] = tetra[4][0];

        return rotated;
    }

    private void set(
            int[][] board, int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            board[tile[0]][tile[1]] = tetra[4][0];
        }
    }

    private boolean canMoveLeft(
            int[][] board, int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] points = tetra[i];
            if (points[0] == 0 || board[points[0] - 1][points[1]] > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveRight(
            int[][] board, int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] points = tetra[i];
            if (points[0] == 9 || board[points[0] + 1][points[1]] > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveDown(
            int[][] board, int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] points = tetra[i];
            if (points[1] == 0 || board[points[0]][points[1] - 1] > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canPlace(
            int[][] board, int[][] next) {
        for (int i = 0; i < 4; i++) {
            int[] points = next[i];
            if (points[0] < 0 ||
                    points[1] < 0 ||
                    points[0] > 9 ||
                    points[1] > 19 ||
                    board[points[0]][points[1]] > 0) {
                return false;
            }
        }

        return true;
    }

}
