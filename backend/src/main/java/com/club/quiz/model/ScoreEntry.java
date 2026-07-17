package com.club.quiz.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.time.Instant;
import java.util.List;

@MongoEntity(collection = "scores")
public class ScoreEntry extends PanacheMongoEntity {

    public String name;
    public int score;
    public int totalQuestions;
    public int attempts = 0;
    public Instant submittedAt = Instant.now();

    // Leaderboard order: highest score first; ties broken by whoever submitted earliest.
    public static List<ScoreEntry> leaderboard() {
        Sort sort = Sort.by("score", Sort.Direction.Descending)
                .and("submittedAt", Sort.Direction.Ascending);
        return findAll(sort).list();
    }

    public static List<ScoreEntry> leaderboardPage(int page, int size) {
        Sort sort = Sort.by("score", Sort.Direction.Descending)
                .and("submittedAt", Sort.Direction.Ascending);
        return findAll(sort).page(Page.of(page, size)).list();
    }

    public static boolean hasSubmitted(String username) {
        return find("name", username).firstResultOptional().isPresent();
    }

    public static ScoreEntry findByName(String username) {
        return find("name", username).firstResult();
    }
}
