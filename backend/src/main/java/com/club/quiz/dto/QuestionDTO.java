package com.club.quiz.dto;

import com.club.quiz.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Sent to the frontend - deliberately excludes correctIndex so answers
// can't be read out of the network tab.
//
// Options are shuffled into a random display order on every fetch (so two
// people looking at the same question side by side don't see identical
// option positions, and the correct answer can't be memorized by position
// from a shared screenshot). Each option keeps its original `id` (the index
// it has in Question.options) regardless of display position, so the
// frontend submits answers by that stable id rather than by position -
// which means scoring server-side (QuizResource#submitScore) doesn't need
// to remember which shuffle a particular user was shown.
public class QuestionDTO {
    public int id;
    public String text;
    public List<OptionDTO> options;

    public QuestionDTO(Question q) {
        this.id = q.id;
        this.text = q.text;
        List<OptionDTO> shuffled = new ArrayList<>();
        for (int i = 0; i < q.options.size(); i++) {
            shuffled.add(new OptionDTO(i, q.options.get(i)));
        }
        Collections.shuffle(shuffled);
        this.options = shuffled;
    }

    public static class OptionDTO {
        public int id;
        public String text;

        public OptionDTO(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}
