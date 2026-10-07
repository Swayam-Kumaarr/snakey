package com.swayam.snake.snake;

/** A coordinate on the contribution grid: {@code week} is the column, {@code day} the row (0=Sun..6=Sat). */
public record Cell(int week, int day) {
}
