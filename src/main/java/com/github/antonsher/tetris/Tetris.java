package com.github.antonsher.tetris;

import com.github.antonsher.tetris.TetrisModel.State;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    int CELL_SIZE = 32;
    Color[] COLORS = {Color.BLACK, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.RED, Color.CYAN, Color.ORANGE};
    public int height = CELL_SIZE * 20 + 21;
    public int boardWidth = CELL_SIZE * 10 + 11;
    public int statsWidth = 120;
    public int width = boardWidth + statsWidth;
    public ReentrantLock lock = new ReentrantLock();
    public Condition condition = lock.newCondition();

    public TetrisModel model = new TetrisModel();

    public void runGame() throws InterruptedException {
        JFrame tetris = new JFrame("Tetris");
        tetris.setLayout(null);
        tetris.setBackground(Color.GRAY);

        JPanel boardPanel = new BoardPanel(model, COLORS, CELL_SIZE);
        boardPanel.setBackground(Color.BLACK);

        tetris.add(boardPanel);

        JPanel statsPanel = new JPanel(null);
        tetris.add(statsPanel);

        JPanel nextPanel = new NextPanel(model, COLORS);
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
        highScoresArea.setText(makeScoresText(model.highScores));
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

            public void updateBounds() {
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
                        awaken();
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
                        model.moveLeft();
                        break;
                    case KeyEvent.VK_RIGHT:
                        model.moveRight();
                        break;
                    case KeyEvent.VK_UP:
                        model.rotate();
                        break;
                    case KeyEvent.VK_DOWN:
                        model.moveDown();
                        break;
                    case KeyEvent.VK_SPACE:
                        model.drop();
                        awaken();
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

        model.generateNext();
        while (true) {
            switch (model.state) {
                case PLACE_NEXT:
                    if (model.canPlace(model.next)) {
                        for (int i = 0; i < model.next.length; i++) {
                            int[] tile = model.next[i];
                            model.tetra[i][0] = tile[0];
                            model.tetra[i][1] = tile[1];
                        }
                        model.generateNext();
                        model.state = State.LET_USER_MOVE;
                    } else {
                        model.set(model.next);
                        model.state = State.LOST;
                        SwingUtilities.invokeLater(() -> {
                            if (model.haveHighScore()) {
                                String name = (String) JOptionPane.showInputDialog(tetris, "Enter your name", "High score!", JOptionPane.QUESTION_MESSAGE, null, null, null);
                                model.saveHighScore(name);
                                highScoresArea.setText(makeScoresText(model.highScores));
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
                        condition.await(1000 / (model.level + 1), TimeUnit.MILLISECONDS);
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
                    model.state = State.MOVE_DOWN;
                    break;
                case MOVE_DOWN:
                    if (model.canMoveDown()) {
                        for (int i = 0; i < 4; i++) {
                            int[] tile = model.tetra[i];
                            tile[1]--;
                        }
                        model.state = State.LET_USER_MOVE;
                    } else {
                        model.freezeAndClearFullLines();
                        model.state = State.PLACE_NEXT;

                        SwingUtilities.invokeLater(() -> {
                            levelField.setText("" + model.level);
                            scoreField.setText("" + model.score);
                            linesField.setText("" + model.lines);
                            boundsUpdater.run();
                            tetris.repaint();
                        });
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
                                model.setBoardTile(x, y, 0);
                            }
                        }
                        model.generateNext();
                        model.state = State.PLACE_NEXT;
                        model.score = 0;
                        model.lines = 0;
                        model.level = 0;
                        model.tillNext = model.LINES_PER_LEVEL;
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

    private void awaken() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public String makeScoresText(List<Object[]> highScores) {
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

}
