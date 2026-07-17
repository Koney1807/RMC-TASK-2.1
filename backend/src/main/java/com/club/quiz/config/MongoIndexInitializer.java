package com.club.quiz.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The application already refuses to sign up a username that's taken
 * (User.usernameTaken), but that's a check-then-act that two concurrent
 * signups could both pass. A unique index on `users.username` makes Mongo
 * itself the source of truth, closing that race; AuthResource#signup catches
 * the resulting duplicate-key error and turns it into a normal 409 response.
 *
 * Also adds a unique index on `scores.name` enforcing "one leaderboard
 * entry per account" at the database level - QuizResource#submitScore's
 * ScoreEntry.hasSubmitted() check is a check-then-act on its own and can't
 * fully guarantee that alone if two submissions from the same user raced.
 */
@ApplicationScoped
public class MongoIndexInitializer {

    private static final Logger LOG = Logger.getLogger(MongoIndexInitializer.class);

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String databaseName;

    void onStart(@Observes StartupEvent ev) {
        try {
            MongoCollection<Document> users = mongoClient
                    .getDatabase(databaseName)
                    .getCollection("users");
            users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

            MongoCollection<Document> scores = mongoClient
                    .getDatabase(databaseName)
                    .getCollection("scores");
            scores.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
        } catch (Exception e) {
            LOG.warn("Could not ensure unique indexes", e);
        }
    }
}
