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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class Tetris {

	public static final int[][][] SHAPES = new int[][][] {{ {3, 19}, {4, 19}, {5, 19}, {6, 19}},};

	enum State {
		PLACE_NEXT,
		LET_USER_MOVE,
		MOVE_DOWN,
		LOST,
	}

	public static final int CELL_SIZE = 32;
	public static final Color TILE_COLOR = Color.BLUE;

	public static void main(String[] args) throws Exception {
		final int height = CELL_SIZE * 20 + 21;
		final int boardWidth = CELL_SIZE * 10 + 11;
		final int statsWidth = 120;
		final int width = boardWidth + statsWidth;

		final int[][] board = new int[10][20];
		final int[][] next = new int[4][2];
		final int[][] tetra = new int[4][2];

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
							g.setColor(TILE_COLOR);
							g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
						}
					}
				}
				for (int[] tile : tetra) {
					final int x = tile[0];
					final int y = tile[1];
					g.setColor(TILE_COLOR);
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
		statsPanel.add(gameOverLabel);

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
				boardPanel.setBounds(0, 0, boardWidth, height);
				statsPanel.setBounds(boardWidth, 0, statsWidth, height);
				scoreLabel.setBounds(10, 10, 80, 24);
				scoreField.setBounds(10, 40, 80, 24);
				gameOverLabel.setBounds(10, 70, 80, 24);
				tetris.setSize(new Dimension(width, height + 20));
			}
		});

		boardPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_LEFT:
						if (canMoveLeft(board, tetra)) {
							for (int[] tile : tetra) {
								tile[0]--;
							}
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_RIGHT:
						if (canMoveRight(board, tetra)) {
							for (int[] tile : tetra) {
								tile[0]++;
							}
							tetris.repaint();
						}
						break;
					case KeyEvent.VK_DOWN:
						if (canMoveDown(board, tetra)) {
							for (int[] tile : tetra) {
								tile[1]--;
							}
							tetris.repaint();
						}
						break;
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
						gameOverLabel.setText("Game over");
						state = State.LOST;
					}
					break;
				case LET_USER_MOVE:
					Thread.sleep(1000);
					state = State.MOVE_DOWN;
					break;
				case MOVE_DOWN:
					if (canMoveDown(board, tetra)) {
						for (int[] tile : tetra) {
							tile[1]--;
						}
						state = State.LET_USER_MOVE;
					} else {
						set(board, tetra, 1);
						state = State.PLACE_NEXT;
					}
					break;
				case LOST:
					break;
			}
			SwingUtilities.invokeLater(boardPanel::repaint);
		}
	}

	private static void set(final int[][] board,
													final int[][] tetra,
													final int value) {
		for (int[] tile : tetra) {
			board[tile[0]][tile[1]] = value;
		}
	}

	private static boolean canMoveLeft(	final int[][] board,
																			final int[][] tetra) {
		for (int[] points : tetra) {
			if (points[0] == 0 || board[points[0] - 1][points[1]] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canMoveRight(final int[][] board,
																			final int[][] tetra) {
		for (int[] points : tetra) {
			if (points[0] == 9 || board[points[0] + 1][points[1]] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canMoveDown(	final int[][] board,
																			final int[][] tetra) {
		for (int[] points : tetra) {
			if (points[1] == 0 || board[points[0]][points[1] - 1] > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean canPlace(final int[][] board,
																	final int[][] next) {
		for (int[] points : next) {
			if (board[points[0]][points[1]] > 0) {
				return false;
			}
		}

		return true;
	}

	private static void gen(final Random random,
													final int[][] next) {
		final int[][] shape = SHAPES[random.nextInt(SHAPES.length)];

		for (int i = 0; i < 4; i++) {
			next[i][0] = shape[i][0];
			next[i][1] = shape[i][1];
		}
	}
}
