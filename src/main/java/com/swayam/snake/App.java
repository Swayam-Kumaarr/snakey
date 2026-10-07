package com.swayam.snake;

import com.swayam.snake.github.ContributionDay;
import com.swayam.snake.github.GitHubClient;
import com.swayam.snake.grid.ContributionGrid;
import com.swayam.snake.render.Renderer;
import com.swayam.snake.snake.Cell;
import com.swayam.snake.snake.SnakeSolver;

import java.nio.file.Path;
import java.util.List;

/**
 * Entry point: fetch a GitHub user's contribution calendar, compute a snake
 * path through it, and render the result as an animated GIF.
 *
 * Usage: {@code java -cp <classpath> com.swayam.snake.App <github-username> [output.gif]}
 */
public final class App {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: App <github-username> [output.gif]");
            System.exit(1);
        }
        String username = args[0];
        Path outputFile = Path.of(args.length > 1 ? args[1] : "snake.gif");

        System.out.println("Resolving GitHub token...");
        String token = GitHubClient.resolveToken();

        System.out.println("Fetching contribution calendar for " + username + "...");
        GitHubClient client = new GitHubClient();
        List<List<ContributionDay>> weeks = client.fetchContributions(username, token);

        ContributionGrid grid = new ContributionGrid(weeks);
        System.out.println("Grid: " + grid.width() + " weeks x " + grid.height() + " days");

        System.out.println("Solving snake path...");
        List<Cell> path = new SnakeSolver().solve(grid);
        System.out.println("Path length: " + path.size() + " cells");

        System.out.println("Rendering " + outputFile + "...");
        new Renderer().render(grid, path, outputFile);

        System.out.println("Done: " + outputFile.toAbsolutePath());
    }
}
