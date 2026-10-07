package com.swayam.snake.github;

import java.time.LocalDate;

/**
 * One square on the GitHub contribution calendar.
 *
 * @param date  the calendar date this square represents
 * @param count number of contributions on this date
 * @param level GitHub's own bucket for this date (0 = none .. 4 = most),
 *              used only for coloring the square
 */
public record ContributionDay(LocalDate date, int count, int level) {
}
