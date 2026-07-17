package com.club.quiz.dto;

import com.club.quiz.model.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Returned right after a user submits the quiz. Unlike QuestionDTO (used
// before submission), this is allowed to reveal correct answers and
// explanations, since the user has already locked in their one attempt.
public class ScoreResultDTO {
    public String name;
    public int score;
    public int totalQuestions;
    public int attemptsUsed;
    public int attemptsRemaining;
    public List<QuestionReview> review;

    public static class QuestionReview {
        public int questionId;
        public String questionText;
        public String yourAnswer;   // null if the question was left unanswered
        public String correctAnswer;
        public boolean correct;
        public String explanation;
        public String history;
    }

    public static ScoreResultDTO build(String name, int score, Map<Integer, Integer> answers,
                                        int attemptsUsed, int maxAttempts) {
        ScoreResultDTO dto = new ScoreResultDTO();
        dto.name = name;
        dto.score = score;
        dto.totalQuestions = Question.ALL.size();
        dto.attemptsUsed = attemptsUsed;
        dto.attemptsRemaining = Math.max(0, maxAttempts - attemptsUsed);
        dto.review = new ArrayList<>();

        for (Question q : Question.ALL) {
            QuestionReview r = new QuestionReview();
            r.questionId = q.id;
            r.questionText = q.text;
            r.correctAnswer = q.options.get(q.correctIndex);
            r.explanation = q.explanation;
            r.history = q.history;

            Integer selected = answers.get(q.id);
            r.yourAnswer = (selected != null && selected >= 0 && selected < q.options.size())
                    ? q.options.get(selected)
                    : null;
            r.correct = selected != null && selected == q.correctIndex;

            dto.review.add(r);
        }

        return dto;
    }
}
