package com.club.quiz.model;

import java.util.List;

public class Question {
    public final int id;
    public final String text;
    public final List<String> options;
    public final int correctIndex; // never sent to the frontend before submission
    public final String explanation; // only sent back after the user submits
    public final String history; // a short history-lesson blurb about the topic

    public Question(int id, String text, List<String> options, int correctIndex, String explanation, String history) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        this.history = history;
    }

    // The hardcoded question bank (task allows "any topic - general knowledge is fine").
    // Kept server-side only, so a participant can't read correct answers from network requests
    // until after they've submitted (see QuizResource#submitScore).
    public static final List<Question> ALL = List.of(
        new Question(1, "What does HTML stand for?",
            List.of("Hyper Trainer Marking Language", "HyperText Markup Language",
                    "HighText Machine Language", "Hyperlink Text Markup Language"), 1,
            "HTML stands for HyperText Markup Language - it's the standard markup language used to structure content on the web.",
            "Tim Berners-Lee proposed HTML in 1991 while working at CERN, building on the existing idea of hypertext to let physicists link research documents together. The very first version had just 18 tags. It became a formal W3C standard in 1995, and the version in use today, HTML5, was finalized in 2014 after years of work to support modern audio, video, and app-like web pages."),
        new Question(2, "Which planet is known as the Red Planet?",
            List.of("Venus", "Jupiter", "Mars", "Saturn"), 2,
            "Mars appears red because its surface is covered in iron oxide (rust), giving it a reddish hue visible even from Earth.",
            "Ancient civilizations, including the Egyptians and Babylonians, already associated the planet's blood-red color with war - the Romans named it after their war god, Mars. In 1877, astronomer Giovanni Schiaparelli mapped what he called 'canali' (channels) on its surface, mistranslated into English as 'canals,' fueling a century of speculation about Martian life. NASA's Mariner 4 sent back the first close-up photos in 1965, revealing a cratered, lifeless landscape."),
        new Question(3, "In JavaScript, which keyword declares a block-scoped variable?",
            List.of("var", "let", "define", "func"), 1,
            "'let' declares a block-scoped variable, meaning it's only accessible within the nearest enclosing block ({}). 'var' is function-scoped instead.",
            "JavaScript was created by Brendan Eich at Netscape in just 10 days in May 1995. For nearly 20 years, 'var' was the only way to declare a variable, and its quirky function-scoping rules caused countless bugs. The 'let' and 'const' keywords were introduced in ECMAScript 2015 (ES6) specifically to give developers predictable, block-level scoping and fix those long-standing pain points."),
        new Question(4, "What is the time complexity of binary search on a sorted array?",
            List.of("O(n)", "O(n log n)", "O(log n)", "O(1)"), 2,
            "Binary search halves the search space on every comparison, so the number of steps grows logarithmically with input size: O(log n).",
            "The core idea of binary search dates back to at least 1946, but the algorithm has a surprisingly bug-prone history. In his 1986 book 'Programming Pearls,' computer scientist Jon Bentley noted that most published implementations of binary search contained bugs, and even Java's standard library shipped a version with an integer overflow bug for nearly a decade before it was fixed in 2006."),
        new Question(5, "Which company originally developed React?",
            List.of("Google", "Meta (Facebook)", "Microsoft", "Amazon"), 1,
            "React was created by Jordan Walke at Facebook (now Meta) and first deployed on Facebook's News Feed in 2011.",
            "React grew out of an internal Facebook tool called 'FaxJS.' It was first used in production on Facebook's News Feed in 2011, then on Instagram's website in 2012. Facebook open-sourced React at JSConf US in May 2013, and it went on to become one of the most widely used front-end libraries in the world, influencing the design of countless other frameworks."),
        new Question(6, "What does 'CSS' stand for?",
            List.of("Cascading Style Sheets", "Computer Style Sheets",
                    "Creative Style System", "Colorful Style Sheets"), 0,
            "CSS stands for Cascading Style Sheets - 'cascading' refers to how styles from multiple sources combine, with rules of precedence determining which ones apply.",
            "CSS was proposed by Norwegian developer Håkon Wium Lie in 1994, while he was working alongside Tim Berners-Lee at CERN. Before CSS, there was no standard way to control a web page's look, and browser makers were adding their own conflicting styling tags. The W3C published CSS Level 1 as an official recommendation in December 1996, and Microsoft's Internet Explorer 3, released that same year, was the first browser to support it.")
    );
}
