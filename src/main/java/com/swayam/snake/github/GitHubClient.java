package com.swayam.snake.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pulls a GitHub user's contribution calendar via the official GraphQL API.
 *
 * Needs a token with at least {@code read:user} scope. We don't hardcode or
 * store one anywhere: pass it in, or leave it null and {@link #resolveToken()}
 * will try the GITHUB_TOKEN env var first, then fall back to asking the
 * locally installed `gh` CLI for the token it's already authenticated with.
 */
public final class GitHubClient {

    private static final String GRAPHQL_ENDPOINT = "https://api.github.com/graphql";

    private static final String QUERY = """
            query($userName: String!) {
              user(login: $userName) {
                contributionsCollection {
                  contributionCalendar {
                    weeks {
                      contributionDays {
                        date
                        contributionCount
                        contributionLevel
                      }
                    }
                  }
                }
              }
            }
            """;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Tries GITHUB_TOKEN, then `gh auth token`. Throws if neither works. */
    public static String resolveToken() throws IOException, InterruptedException {
        String env = System.getenv("GITHUB_TOKEN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        ProcessBuilder pb = new ProcessBuilder("gh", "auth", "token");
        pb.redirectErrorStream(false);
        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.readLine();
        }
        int exit = process.waitFor();
        if (exit != 0 || output == null || output.isBlank()) {
            throw new IllegalStateException(
                    "No GitHub token available. Set GITHUB_TOKEN, or run `gh auth login` first.");
        }
        return output.trim();
    }

    /**
     * Fetches the last 12 months of {@code username}'s contribution calendar,
     * grouped into weeks exactly as GitHub lays them out (each inner list is
     * one column on the calendar, Sunday first).
     */
    public List<List<ContributionDay>> fetchContributions(String username, String token)
            throws IOException, InterruptedException {

        JsonObject variables = new JsonObject();
        variables.addProperty("userName", username);

        JsonObject body = new JsonObject();
        body.addProperty("query", QUERY);
        body.add("variables", variables);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("User-Agent", "snake-contribution-art")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.has("errors")) {
            throw new IOException("GitHub GraphQL errors: " + root.get("errors"));
        }

        JsonObject calendar = root.getAsJsonObject("data")
                .getAsJsonObject("user")
                .getAsJsonObject("contributionsCollection")
                .getAsJsonObject("contributionCalendar");

        List<List<ContributionDay>> weeksOut = new ArrayList<>();
        JsonArray weeks = calendar.getAsJsonArray("weeks");
        for (JsonElement weekEl : weeks) {
            JsonArray contributionDays = weekEl.getAsJsonObject().getAsJsonArray("contributionDays");
            List<ContributionDay> week = new ArrayList<>();
            for (JsonElement dayEl : contributionDays) {
                JsonObject day = dayEl.getAsJsonObject();
                LocalDate date = LocalDate.parse(day.get("date").getAsString());
                int count = day.get("contributionCount").getAsInt();
                int level = levelToInt(day.get("contributionLevel").getAsString());
                week.add(new ContributionDay(date, count, level));
            }
            weeksOut.add(week);
        }
        return weeksOut;
    }

    private static int levelToInt(String level) {
        return switch (level) {
            case "NONE" -> 0;
            case "FIRST_QUARTILE" -> 1;
            case "SECOND_QUARTILE" -> 2;
            case "THIRD_QUARTILE" -> 3;
            case "FOURTH_QUARTILE" -> 4;
            default -> 0;
        };
    }
}
