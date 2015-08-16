package com.github.antonsher.tetris;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TetrisModel {
    File SCORES_FILE = new File(System.getProperty("user.home"), "tetris-highscores.txt");
    Pattern SCORE_LINE = Pattern.compile("(\\d{1,9});(.*)");
    public List<Object[]> highScores = readScores();

    public Random random = new Random();

    public int score = 0;
    public int lines = 0;
    public int level = 0;
    public int LINES_PER_LEVEL = 20;
    public int tillNext = LINES_PER_LEVEL;
    public State state = State.PLACE_NEXT;

    boolean haveHighScore() {
        return score > 0 && (highScores.size() < 10 || (int) highScores.get(0)[0] < score);
    }

    void saveHighScore(String playerName) {
        highScores.add(0, new Object[]{score, playerName == null ? "" : playerName});
        sortScores(highScores);
        if (highScores.size() > 10) {
            highScores.remove(0);
        }
        writeScores(highScores);
    }

    void moveLeft() {
        if (canMoveLeft()) {
            for (int i = 0; i < 4; i++) {
                int[] tile = tetra[i];
                tile[0]--;
            }
        }
    }

    void moveRight() {
        if (canMoveRight()) {
            for (int i = 0; i < 4; i++) {
                int[] tile = tetra[i];
                tile[0]++;
            }
        }
    }

    void rotate() {
        int[][] rotated = rotate(tetra);
        if (canPlace(rotated)) {
            System.arraycopy(rotated, 0, tetra, 0, 4);
        }
    }

    void moveDown() {
        if (canMoveDown()) {
            for (int i = 0; i < 4; i++) {
                int[] tile = tetra[i];
                tile[1]--;
            }
        }
    }

    void drop() {
        while (canMoveDown()) {
            for (int i = 0; i < 4; i++) {
                int[] tile = tetra[i];
                tile[1]--;
            }
        }
    }


    public enum State {
        PLACE_NEXT,
        LET_USER_MOVE,
        MOVE_DOWN,
        LOST,
    }

    public void freezeAndClearFullLines() {
        set(tetra);
        int y = 0;
        while (y < 20) {
            boolean full = true;
            for (int x = 0; x < 10; x++) {
                if (getBoardTile(x, y) == 0) {
                    full = false;
                    break;
                }
            }
            int atOnce = 0;
            if (full) {
                atOnce++;
                for (int x = 0; x < 10; x++) {
                    setBoardTile(x, y, 0);
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
            }
            if (tillNext <= 0) {
                tillNext += LINES_PER_LEVEL;
                level++;
            }
        }
    }

    public void setBoardTile(int x, int y, int value) {
        board[x][y] = value;
    }

    public int getBoardTile(int x, int y) {
        return board[x][y];
    }

    public int getBoardHeight() {
        return board[0].length;
    }

    public int getBoardWidth() {
        return board.length;
    }

    public int[][] board = new int[10][20];

    public boolean canMoveLeft() {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            if (isOutsideTheBoard(tile) || tile[0] == 0 || board[tile[0] - 1][tile[1]] > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveRight() {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            if (isOutsideTheBoard(tile) || tile[0] == 9 || board[tile[0] + 1][tile[1]] > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveDown() {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            if (isOutsideTheBoard(tile) || tile[1] == 0 || board[tile[0]][tile[1] - 1] > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean canPlace(int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            if (isOutsideTheBoard(tile) || board[tile[0]][tile[1]] > 0) {
                return false;
            }
        }

        return true;
    }

    public boolean isOutsideTheBoard(int[] tile) {
        return tile[0] < 0 ||
                tile[1] < 0 ||
                tile[0] > 9 ||
                tile[1] > 19;
    }

    int[] SCORES = {40, 100, 300, 1200};
    public int[][] next = new int[5][2];
    public int[][] tetra = new int[5][2];


    public void set(int[][] tetra) {
        for (int i = 0; i < 4; i++) {
            int[] tile = tetra[i];
            board[tile[0]][tile[1]] = getTetraKind(tetra);
        }
    }


    public int getNextTetraY(int i) {
        return getTetraY(i, next);
    }

    public int getNextTetraX(int i) {
        return getTetraX(i, next);
    }

    public int getTetraY(int i, int[][] tetra) {
        return tetra[i][1];
    }

    public int getTetraKind(int[][] tetra) {
        return tetra[4][0];
    }

    public int getTetraX(int i, int[][] tetra) {
        return tetra[i][0];
    }

    public void generateNext() {
        int[][] tetra1 = TETRAS[random.nextInt(TETRAS.length)];

        for (int i = 0; i < 5; i++) {
            next[i][0] = getTetraX(i, tetra1);
            next[i][1] = getTetraY(i, tetra1);
        }
    }

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


    public List<Object[]> readScores() {
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

    public void sortScores(List<Object[]> result) {
        result.sort((o1, o2) -> Integer.compare((int) o1[0], (int) o2[0]));
    }

    public void writeScores(List<Object[]> scores) {
        try (PrintStream ps = new PrintStream(SCORES_FILE)) {
            for (Object[] e : scores) {
                ps.println(e[0] + ";" + e[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int[][] rotate(int[][] tetra) {
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
        setTetraKind(rotated, getTetraKind(tetra));

        return rotated;
    }

    public void setTetraKind(int[][] rotated, int kind) {
        rotated[4][0] = kind;
    }
}
