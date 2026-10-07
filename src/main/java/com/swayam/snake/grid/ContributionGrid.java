package com.swayam.snake.grid;

import com.swayam.snake.github.ContributionDay;

import java.util.List;
import java.util.Optional;

/**
 * The contribution calendar as a grid: columns are weeks (left = oldest,
 * right = most recent, same as github.com), rows are day-of-week 0=Sun..6=Sat.
 * <p>
 * The first and last columns are often partial (the calendar doesn't start
 * exactly on a Sunday, and "today" isn't necessarily a Saturday), so always
 * go through {@link #dayAt} rather than assuming every column has 7 rows.
 */
public final class ContributionGrid {

    private final List<List<ContributionDay>> weeks;

    public ContributionGrid(List<List<ContributionDay>> weeks) {
        this.weeks = weeks;
    }

    public int width() {
        return weeks.size();
    }

    /** Always 7 — GitHub's calendar is always laid out Sun..Sat. */
    public int height() {
        return 7;
    }

    public Optional<ContributionDay> dayAt(int week, int dayOfWeek) {
        if (week < 0 || week >= weeks.size()) {
            return Optional.empty();
        }
        List<ContributionDay> column = weeks.get(week);
        if (dayOfWeek < 0 || dayOfWeek >= column.size()) {
            return Optional.empty();
        }
        return Optional.of(column.get(dayOfWeek));
    }

    /** True if this cell exists on the calendar and has at least one contribution. */
    public boolean hasContribution(int week, int dayOfWeek) {
        return dayAt(week, dayOfWeek).map(d -> d.count() > 0).orElse(false);
    }
}
