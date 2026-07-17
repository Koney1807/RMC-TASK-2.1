package com.club.quiz.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuizResourceTest {

    // Mirrors Question.ALL in com.club.quiz.model.Question: questionId -> correct option id.
    // The correct option id is stable regardless of shuffled display order
    // (see QuestionDTO), so this map is valid no matter how a given
    // request's options came back shuffled.
    private static final Map<Integer, Integer> CORRECT_ANSWERS = Map.of(
            1, 1,
            2, 2,
            3, 1,
            4, 2,
            5, 1,
            6, 0
    );

    private String freshToken() {
        String username = "quizzer_" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {"username": "%s", "email": "%s@example.com", "password": "correcthorse"}
                """.formatted(username, username);
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/signup")
                .then().statusCode(201)
                .extract().path("token");
    }

    @Test
    void questionsComeBackWithoutRevealingTheAnswer() {
        given().when().get("/api/questions")
                .then().statusCode(200)
                .body("size()", equalTo(6))
                .body("options[0].size()", equalTo(4));
    }

    @Test
    void submittingAllCorrectAnswersScoresSixOutOfSix() {
        String token = freshToken();
        Map<String, Integer> answers = new HashMap<>();
        CORRECT_ANSWERS.forEach((qId, optId) -> answers.put(String.valueOf(qId), optId));

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(Map.of("answers", answers))
                .when().post("/api/scores")
                .then().statusCode(201)
                .body("score", equalTo(6))
                .body("totalQuestions", equalTo(6));
    }

    @Test
    void submittingAllWrongAnswersScoresZero() {
        String token = freshToken();
        Map<String, Integer> answers = new HashMap<>();
        // Pick an option id that's never correct (there are 4 options per
        // question: ids 0-3; shifting the correct id by 1 mod 4 is always wrong).
        CORRECT_ANSWERS.forEach((qId, optId) -> answers.put(String.valueOf(qId), (optId + 1) % 4));

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(Map.of("answers", answers))
                .when().post("/api/scores")
                .then().statusCode(201)
                .body("score", equalTo(0));
    }

    @Test
    void secondSubmissionFromTheSameAccountIsRejected() {
        String token = freshToken();
        Map<String, Integer> answers = new HashMap<>();
        CORRECT_ANSWERS.forEach((qId, optId) -> answers.put(String.valueOf(qId), optId));
        Map<String, Object> body = Map.of("answers", answers);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
                .when().post("/api/scores")
                .then().statusCode(201);

        // Retaking the quiz and submitting again should be blocked.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
                .when().post("/api/scores")
                .then().statusCode(409);
    }

    @Test
    void leaderboardIsPublicAndPaginated() {
        String token = freshToken();
        Map<String, Integer> answers = new HashMap<>();
        CORRECT_ANSWERS.forEach((qId, optId) -> answers.put(String.valueOf(qId), optId));

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(Map.of("answers", answers))
                .when().post("/api/scores")
                .then().statusCode(201);

        given().when().get("/api/leaderboard")
                .then().statusCode(200)
                .header("X-Total-Count", notNullValue())
                .body("size()", greaterThanOrEqualTo(1));
    }
}
