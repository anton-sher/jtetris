package com.github.antonsher.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class Tetris {
	public static final int[][][] TETRAS = new int[][][] {
																												{ {3, 19}, {4, 19}, {5, 19}, {6, 19}, {1, 0}}, // hor. stick
																												{ {5, 16}, {5, 17}, {5, 18}, {5, 19}, {1, 0}}, // ver. stick
																												{ {4, 19}, {5, 19}, {4, 18}, {5, 18}, {2, 0}}, // square
																												{ {4, 19}, {5, 19}, {6, 19}, {5, 18}, {3, 0}}, // pin 1
																												{ {4, 18}, {5, 18}, {6, 18}, {5, 19}, {3, 0}}, // pin 2
																												{ {5, 17}, {5, 18}, {5, 19}, {4, 18}, {3, 0}}, // pin 3
																												{ {4, 17}, {4, 18}, {4, 19}, {5, 18}, {3, 0}}, // pin 4
																												{ {4, 17}, {4, 18}, {4, 19}, {5, 17}, {4, 0}}, // right leg 1
																												{ {4, 18}, {5, 18}, {6, 18}, {6, 19}, {4, 0}}, // right leg 2
																												{ {5, 17}, {5, 18}, {5, 19}, {4, 19}, {4, 0}}, // right leg 3
																												{ {4, 19}, {5, 19}, {6, 19}, {4, 18}, {4, 0}}, // right leg 4
																												{ {5, 17}, {5, 18}, {5, 19}, {4, 17}, {5, 0}}, // left leg 1
																												{ {4, 19}, {5, 19}, {6, 19}, {6, 18}, {5, 0}}, // left leg 2
																												{ {4, 17}, {4, 18}, {4, 19}, {5, 19}, {5, 0}}, // left leg 3
																												{ {4, 18}, {5, 18}, {6, 18}, {4, 19}, {5, 0}}, // left leg 4
																												{ {4, 18}, {5, 18}, {5, 19}, {6, 19}, {6, 0}}, // z 1
																												{ {4, 18}, {4, 19}, {5, 17}, {5, 18}, {6, 0}}, // z 2
																												{ {4, 19}, {5, 19}, {5, 18}, {6, 18}, {7, 0}}, // reverse z 1
																												{ {4, 17}, {4, 18}, {5, 18}, {5, 19}, {7, 0}}, // reverse z 2
	};

	public static final Color[] COLORS = {Color.BLACK, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.RED, Color.CYAN, Color.ORANGE};

	enum State {
		PLACE_NEXT,
		LET_USER_MOVE,
		MOVE_DOWN,
		LOST,
	}

	public static final int CELL_SIZE = 32;
	public static final Color TILE_COLOR = Color.BLUE;

	public static void main(String[] args) throws Exception {
		final ReentrantLock lock = new ReentrantLock();
		final Condition condition = lock.newCondition();

		final int height = CELL_SIZE * 20 + 21;
		final int boardWidth = CELL_SIZE * 10 + 11;
		final int statsWidth = 120;
		final int width = boardWidth + statsWidth;

		final int[][] board = new int[10][20];
		final int[][] next = new int[5][2];
		final int[][] tetra = new int[5][2];

		final JFrame tetris = new JFrame("Tetris");

		tetris.setBackground(Color.GRAY);

		final JPanel boardPanel = new JPanel() {
			@Override
			public void paintComponent(final Graphics g) {
				super.paintComponent(g);
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, width, height);
				for (int x = 0; x < board.length; x++) {
					for (int y = 0; y < board[0].length; y++) {
						if (board[x][y] > 0) {
							g.setColor(COLORS[board[x][y]]);
							g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
						}
					}
				}
				for (int i = 0; i < 4; i++) {
					final int[] tile = tetra[i];
					final int x = tile[0];
					final int y = tile[1];
					g.setColor(COLORS[tetra[4][0]]);
					g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
				}
			}
		};
		boardPanel.setBackground(Color.BLACK);

		tetris.add(boardPanel);

		final JPanel statsPanel = new JPanel();
		tetris.add(statsPanel);

		final JLabel scoreLabel = new JLabel("Score");
		statsPanel.add(scoreLabel);
		final JTextField scoreField = new JTextField("0");
		scoreField.setEditable(false);
		statsPanel.add(scoreField);
		final JLabel gameOverLabel = new JLabel("");
		gameOverLabel.setText("Game over");

		final Runnable boundsUpdater = () -> {
			boardPanel.setBounds(0, 0, boardWidth, height);
			statsPanel.setBounds(boardWidth, 0, statsWidth, height);
			scoreLabel.setBounds(10, 10, 80, 24);
			scoreField.setBounds(10, 40, 80, 24);
			gameOverLabel.setBounds(10, 70, 80, 24);
			tetris.setSize(new Dimension(width, height + 20));
		};

		tetris.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				super.componentResized(e);
				updateBounds();
			}

			@Override
			public void componentMoved(final ComponentEvent e) {
				super.componentMoved(e);
				updateBounds();
			}

			private void updateBounds() {
				boundsUpdater.run();
			}
		});

		boardPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_LEFT:
						if (canMoveLeft(board, tetra)) {
							for (int i = 0; i < 4; i++) {
								final int[] tile = tetra[i];
								tile[0]--;
							}
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_RIGHT:
						if (canMoveRight(board, tetra)) {
							for (int i = 0; i < 4; i++) {
								final int[] tile = tetra[i];
								tile[0]++;
							}
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_UP:
						int[][] rotated = rotate(tetra);
						if (canPlace(board, rotated)) {
							System.arraycopy(rotated, 0, tetra, 0, 4);
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_DOWN:
						if (canMoveDown(board, tetra)) {
							for (int i = 0; i < 4; i++) {
								final int[] tile = tetra[i];
								tile[1]--;
							}
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_SPACE:
						while (canMoveDown(board, tetra)) {
							for (int i = 0; i < 4; i++) {
								final int[] tile = tetra[i];
								tile[1]--;
							}
						}
						lock.lock();
						try {
							condition.signalAll();
						}
						finally {
							lock.unlock();
						}
						tetris.repaint();
				}
			}
		});

		boardPanel.setFocusable(true);
		boardPanel.setFocusTraversalKeysEnabled(false);
		boardPanel.requestFocus();

		tetris.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		tetris.setLocation(dim.width / 2 - width / 2, dim.height / 2 - height / 2);

		tetris.setVisible(true);

		final Random random = new Random();
		gen(random, next);
		State state = State.PLACE_NEXT;
		int score = 0;
		while (true) {
			switch (state) {
				case PLACE_NEXT:
					if (canPlace(board, next)) {
						for (int i = 0; i < next.length; i++) {
							final int[] tile = next[i];
							tetra[i][0] = tile[0];
							tetra[i][1] = tile[1];
						}
						gen(random, next);
						state = State.LET_USER_MOVE;
					} else {
						state = State.LOST;
						SwingUtilities.invokeLater(() -> {
							statsPanel.add(gameOverLabel);
							boundsUpdater.run();
							statsPanel.repaint();
						});
						SwingUtilities.invokeLater(tetris::repaint);
					}
					break;
				case LET_USER_MOVE:
					lock.lock();
					try {
						condition.await(500, TimeUnit.MILLISECONDS);
					}
					finally {
						lock.unlock();
					}
					state = State.MOVE_DOWN;
					break;
				case MOVE_DOWN:
					if (canMoveDown(board, tetra)) {
						for (int i = 0; i < 4; i++) {
							final int[] tile = tetra[i];
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
							if (full) {
								score++;
								final int finalScore = score;
								SwingUtilities.invokeLater(() -> {
									scoreField.setText("" + finalScore);
									boundsUpdater.run();
									scoreField.repaint();
								});
								for (int x = 0; x < 10; x++) {
									board[x][y] = 0;
									System.arraycopy(board[x], y + 1, board[x], y, 19 - y);
									board[x][19] = 0;
								}
							} else {
								y++;
							}
						}
						state = State.PLACE_NEXT;
					}
					break;
				case LOST:
					lock.lock();
					try {
						condition.await(500, TimeUnit.MILLISECONDS);
					}
					finally {
						lock.unlock();
					}
					break;
			}
			SwingUtilities.invokeLater(boardPanel::repaint);
		}
	}

	private static int[][] rotate(final int[][] tetra) {
		int xCenter = 0;
		int yCenter = 0;
		for (int i = 0; i < 4; i++) {
			final int[] tile = tetra[i];
			xCenter += tile[0];
			yCenter += tile[1];
		}
		xCenter /= 4;
		yCenter /= 4;

		final int[][] rotated = new int[5][2];
		for (int i = 0; i < 4; i++) {
			final int[] tile = tetra[i];
			rotated[i][0] = xCenter + yCenter - tile[1];
			rotated[i][1] = yCenter + tile[0] - xCenter;
		}
		rotated[4][0] = tetra[4][0];

		return rotated;
	}

	private static void set(final int[][] board, final int[][] tetra) {
		for (int i = 0; i < 4; i++) {
			final int[] tile = tetra[i];
			board[tile[0]][tile[1]] = tetra[4][0];
		}
	}

	private static boolean canMoveLeft(	final int[][] board,
																			final int[][] tetra) {
		for (int i = 0; i < 4; i++) {
			final int[] points = tetra[i];
			if (points[0] == 0 || board[points[0] - 1][points[1]] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canMoveRight(final int[][] board,
																			final int[][] tetra) {
		for (int i = 0; i < 4; i++) {
			final int[] points = tetra[i];
			if (points[0] == 9 || board[points[0] + 1][points[1]] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canMoveDown(	final int[][] board,
																			final int[][] tetra) {
		for (int i = 0; i < 4; i++) {
			final int[] points = tetra[i];
			if (points[1] == 0 || board[points[0]][points[1] - 1] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canPlace(final int[][] board,
																	final int[][] next) {
		for (int i = 0; i < 4; i++) {
			final int[] points = next[i];
			if (board[points[0]][points[1]] > 0) {
				return false;
			}
		}

		return true;
	}

	private static void gen(final Random random,
													final int[][] next) {
		final int[][] tetra = TETRAS[random.nextInt(TETRAS.length)];

		for (int i = 0; i < 5; i++) {
			next[i][0] = tetra[i][0];
			next[i][1] = tetra[i][1];
		}
	}
}
