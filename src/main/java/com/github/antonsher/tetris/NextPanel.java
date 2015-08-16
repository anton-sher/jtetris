package com.github.antonsher.tetris;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class NextPanel extends JPanel {
    private final TetrisModel model;
    private final Color[] COLORS;

    NextPanel(TetrisModel model, Color[] colors) {
        this.model = model;
        COLORS = colors;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(model.getNextTetraX(i), minX);
            maxX = Math.max(model.getNextTetraX(i), maxX);
            maxY = Math.max(model.getNextTetraY(i), maxY);
        }
        g.setColor(COLORS[model.getTetraKind(model.next)]);
        for (int i = 0; i < 4; i++) {
            int x = model.getNextTetraX(i) - minX;
            int y = maxY - model.getNextTetraY(i);
            if (maxX - minX < 2) {
                x++;
            }
            g.fillRect(1 + x * 21, 1 + y * 21, 20, 20);
        }
    }
}
