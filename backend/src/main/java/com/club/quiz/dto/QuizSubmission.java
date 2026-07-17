package com.club.quiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class QuizSubmission {

    // No "name" field anymore - the submitter's identity comes from their
    // JWT (see QuizResource), so a score can't be submitted under someone
    // else's name.
    // Map of questionId -> selectedOptionIndex, e.g. {"1": 1, "2": 2, ...}
    @NotNull(message = "Answers are required")
    @Size(min = 1, message = "At least one answer is required")
    public Map<Integer, Integer> answers;
}
