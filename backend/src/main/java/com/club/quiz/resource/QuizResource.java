package com.club.quiz.resource;

import com.club.quiz.dto.QuestionDTO;
import com.club.quiz.dto.QuizSubmission;
import com.club.quiz.dto.ScoreResultDTO;
import com.club.quiz.model.Question;
import com.club.quiz.model.ScoreEntry;
import com.mongodb.MongoWriteException;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuizResource {

    private static final Logger LOG = Logger.getLogger(QuizResource.class);
    private static final int DEFAULT_LEADERBOARD_SIZE = 50;
    private static final int MAX_LEADERBOARD_SIZE = 200;

    @GET
    @Path("/questions")
    @PermitAll
    public List<QuestionDTO> getQuestions() {
        // A fresh QuestionDTO (with freshly shuffled option order) is built
        // per call, so every attempt gets its own shuffle.
        return Question.ALL.stream().map(QuestionDTO::new).collect(Collectors.toList());
    }

    private static final int MAX_ATTEMPTS = 3;

    // Submitting a score now requires being logged in (real account) - the
    // participant's name comes straight from their JWT, so nobody can submit
    // a score under someone else's name.
    @POST
    @Path("/scores")
    @RolesAllowed("user")
    public Response submitScore(@Valid QuizSubmission submission, @Context SecurityContext ctx) {
        String username = ctx.getUserPrincipal().getName();

        // Up to MAX_ATTEMPTS attempts per account. One document per user is
        // kept in the scores collection; the leaderboard always reflects
        // their single best score across all attempts so far.
        ScoreEntry existing = ScoreEntry.findByName(username);
        if (existing != null && existing.attempts >= MAX_ATTEMPTS) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message",
                            "You've used all " + MAX_ATTEMPTS + " attempts for this quiz."))
                    .build();
        }

        // Scoring happens here, server-side, using the question bank's real
        // correct answers - the frontend never sees them, so it can't cheat.
        // Note: submission.answers is keyed by the *option id* the frontend
        // was given (see QuestionDTO), not by display position, so this
        // still scores correctly regardless of how that user's options were
        // shuffled.
        int correct = 0;
        for (Question q : Question.ALL) {
            Integer selected = submission.answers.get(q.id);
            if (selected != null && selected == q.correctIndex) {
                correct++;
            }
        }

        try {
            if (existing == null) {
                ScoreEntry entry = new ScoreEntry();
                entry.name = username;
                entry.score = correct;
                entry.totalQuestions = Question.ALL.size();
                entry.attempts = 1;
                entry.persist();
            } else {
                existing.attempts += 1;
                if (correct > existing.score) {
                    existing.score = correct;
                    existing.submittedAt = java.time.Instant.now();
                }
                existing.update();
            }
        } catch (MongoWriteException e) {
            // The findByName()-based check above is a check-then-act; the
            // unique index on scores.name (see MongoIndexInitializer) is
            // the real guarantee against two concurrent first-attempts from
            // the same user both slipping through as "no existing entry".
            if (e.getError().getCode() == 11000) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", "That attempt couldn't be saved - please try again."))
                        .build();
            }
            LOG.error("Unexpected error persisting score", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Could not save your score. Please try again.")).build();
        }

        // Now that this attempt is locked in, it's safe to reveal correct
        // answers + explanations for every question.
        int attemptsUsed = (existing == null) ? 1 : existing.attempts;
        ScoreResultDTO result = ScoreResultDTO.build(username, correct, submission.answers, attemptsUsed, MAX_ATTEMPTS);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    @Path("/attempts")
    @RolesAllowed("user")
    public Response getAttempts(@Context SecurityContext ctx) {
        String username = ctx.getUserPrincipal().getName();
        ScoreEntry existing = ScoreEntry.findByName(username);
        int used = existing == null ? 0 : existing.attempts;
        return Response.ok(Map.of(
                "attemptsUsed", used,
                "attemptsRemaining", Math.max(0, MAX_ATTEMPTS - used),
                "maxAttempts", MAX_ATTEMPTS
        )).build();
    }

    @GET
    @Path("/leaderboard")
    @PermitAll
    public Response getLeaderboard(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("" + DEFAULT_LEADERBOARD_SIZE) int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = DEFAULT_LEADERBOARD_SIZE;
        if (size > MAX_LEADERBOARD_SIZE) size = MAX_LEADERBOARD_SIZE;

        List<ScoreEntry> results = ScoreEntry.leaderboardPage(page, size);
        long total = ScoreEntry.count();

        return Response.ok(results)
                .header("X-Total-Count", total)
                .header("X-Page", page)
                .header("X-Page-Size", size)
                .build();
    }
}
