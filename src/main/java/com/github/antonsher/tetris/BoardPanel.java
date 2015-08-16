package com.github.antonsher.tetris;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class BoardPanel extends JPanel {
    private final TetrisModel model;
    private final Color[] COLORS;
    private final int CELL_SIZE;

    BoardPanel(TetrisModel model, Color[] colors, int cell_size) {
        this.model = model;
        COLORS = colors;
        CELL_SIZE = cell_size;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        for (int i = 0; i < 4; i++) {
            final int[][] tetra = model.tetra;
            int x = model.getTetraX(i, tetra);
            int y = model.getTetraY(i, tetra);
            g.setColor(COLORS[model.getTetraKind(tetra)]);
            g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
        }
        for (int x = 0; x < model.getBoardWidth(); x++) {
            for (int y = 0; y < model.getBoardHeight(); y++) {
                if (model.getBoardTile(x, y) > 0) {
                    g.setColor(COLORS[model.getBoardTile(x, y)]);
                    g.fillRect(1 + x * (CELL_SIZE + 1), 1 + (19 - y) * (CELL_SIZE + 1), CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
