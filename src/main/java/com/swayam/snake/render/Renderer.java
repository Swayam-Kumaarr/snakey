package com.swayam.snake.render;

import com.swayam.snake.github.ContributionDay;
import com.swayam.snake.grid.ContributionGrid;
import com.swayam.snake.snake.Cell;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Draws the grid + the snake eating through it, frame by frame, into an animated GIF. */
public final class Renderer {

    private static final int CELL_SIZE = 15;
    private static final int GAP = 4;
    private static final int PITCH = CELL_SIZE + GAP;
    private static final int MARGIN = 16;
    private static final int ARC = 4;
    private static final int SNAKE_TAIL_LENGTH = 4;
    // GIF delays are stored in 10ms units, so this is 7 ticks (was 5)
    private static final int FRAME_DELAY_MS = 70;
    private static final int HOLD_FRAMES_AT_END = 25;

    // GitHub's dark theme palette
    private static final Color BACKGROUND = new Color(0x0D, 0x11, 0x17);
    private static final Color[] LEVEL_COLORS = {
            new Color(0x16, 0x1B, 0x22),
            new Color(0x0E, 0x44, 0x29),
            new Color(0x00, 0x6D, 0x32),
            new Color(0x26, 0xA6, 0x41),
            new Color(0x39, 0xD3, 0x53),
    };
    private static final Color SNAKE_HEAD = new Color(0xA3, 0x71, 0xF7);

    public void render(ContributionGrid grid, List<Cell> path, Path outputFile) throws IOException {
        Map<Cell, Integer> pathIndex = new HashMap<>();
        for (int i = 0; i < path.size(); i++) {
            pathIndex.put(path.get(i), i);
        }

        int width = MARGIN * 2 + grid.width() * PITCH - GAP;
        int height = MARGIN * 2 + grid.height() * PITCH - GAP;

        try (GifEncoder encoder = new GifEncoder(outputFile, FRAME_DELAY_MS, true)) {
            for (int step = 0; step < path.size(); step++) {
                encoder.addFrame(drawFrame(grid, path, pathIndex, width, height, step));
            }
            // hold the fully-eaten (empty) board for a moment before the gif loops back to the start
            BufferedImage finalFrame = drawFrame(grid, path, pathIndex, width, height, path.size() - 1);
            for (int i = 0; i < HOLD_FRAMES_AT_END; i++) {
                encoder.addFrame(finalFrame);
            }
        }
    }

    private BufferedImage drawFrame(
            ContributionGrid grid,
            List<Cell> path,
            Map<Cell, Integer> pathIndex,
            int width,
            int height,
            int step) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, width, height);

        for (int week = 0; week < grid.width(); week++) {
            for (int day = 0; day < grid.height(); day++) {
                Optional<ContributionDay> contributionDay = grid.dayAt(week, day);
                if (contributionDay.isPresent()) {
                    drawCell(g, week, day, contributionDay.get(), pathIndex, step);
                }
            }
        }

        drawSnake(g, path, step);

        g.dispose();
        return image;
    }

    private void drawCell(
            Graphics2D g,
            int week,
            int day,
            ContributionDay contributionDay,
            Map<Cell, Integer> pathIndex,
            int step) {

        Integer visitedAt = pathIndex.get(new Cell(week, day));
        boolean eaten = visitedAt != null && visitedAt <= step;
        // subtractive: cells start at their real contribution level and are wiped to empty once eaten
        int level = eaten ? 0 : contributionDay.level();

        int x = MARGIN + week * PITCH;
        int y = MARGIN + day * PITCH;
        g.setColor(LEVEL_COLORS[level]);
        g.fillRoundRect(x, y, CELL_SIZE, CELL_SIZE, ARC, ARC);
    }

    private void drawSnake(Graphics2D g, List<Cell> path, int step) {
        if (path.isEmpty()) {
            return;
        }
        int from = Math.max(0, step - SNAKE_TAIL_LENGTH + 1);
        for (int i = from; i <= step; i++) {
            Cell cell = path.get(i);
            // fades from fully-opaque head to translucent tail
            float fraction = (i - from + 1f) / (step - from + 1f);
            int alpha = Math.round(90 + fraction * 165);
            Color color = new Color(SNAKE_HEAD.getRed(), SNAKE_HEAD.getGreen(), SNAKE_HEAD.getBlue(), alpha);

            int x = MARGIN + cell.week() * PITCH;
            int y = MARGIN + cell.day() * PITCH;
            g.setColor(color);
            g.fillRoundRect(x, y, CELL_SIZE, CELL_SIZE, ARC, ARC);
        }
    }
}
