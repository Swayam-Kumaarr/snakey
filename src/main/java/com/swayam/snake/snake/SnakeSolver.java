package com.swayam.snake.snake;

import com.swayam.snake.grid.ContributionGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the order in which the snake visits every existing cell on the grid.
 * <p>
 * This is the simplest correct solution: a boustrophedon ("lawnmower") sweep —
 * straight down column 0, straight up column 1, down column 2, and so on. It
 * only takes one orthogonal step between consecutive cells, so it's a valid
 * snake move at every point, and it visits every cell on the calendar exactly
 * once.
 * <p>
 * It is deliberately not the "best" path — the real
 * <a href="https://github.com/Platane/snk">Platane/snk</a> project solves for
 * a shorter, more natural-looking route with a proper search/optimization
 * algorithm. Swapping this method out for something smarter (e.g. only
 * visiting cells that actually have contributions, or minimizing backtracking)
 * is a good next step once this MVP is working end to end.
 */
public final class SnakeSolver {

    public List<Cell> solve(ContributionGrid grid) {
        List<Cell> path = new ArrayList<>();
        for (int week = 0; week < grid.width(); week++) {
            boolean topToBottom = week % 2 == 0;
            if (topToBottom) {
                for (int day = 0; day < grid.height(); day++) {
                    if (grid.dayAt(week, day).isPresent()) {
                        path.add(new Cell(week, day));
                    }
                }
            } else {
                for (int day = grid.height() - 1; day >= 0; day--) {
                    if (grid.dayAt(week, day).isPresent()) {
                        path.add(new Cell(week, day));
                    }
                }
            }
        }
        return path;
    }
}
