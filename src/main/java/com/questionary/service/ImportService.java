package com.questionary.service;

import com.questionary.entity.Question;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportService {

    /**
     * Parses a .txt InputStream into Question objects (not persisted).
     *
     * Format:
     *   #Question text
     *   Answer line 1
     *   Answer line 2
     *
     *   #Next question
     *   Answer
     *
     * Lines starting with '#' begin a new block.
     * Non-blank lines below accumulate as answer.
     * Blank lines are ignored; only a new '#' line closes a block.
     */
    public List<Question> parse(InputStream inputStream) throws IOException {
        List<Question> questions = new ArrayList<>();
        List<String> answerLines = new ArrayList<>();
        String currentQuestion = null;
        int order = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String cleanLine = line.strip();
                if (cleanLine.startsWith("#")) {
                    if (currentQuestion != null) {
                        questions.add(build(currentQuestion, answerLines, order++));
                        answerLines.clear();
                    }
                    currentQuestion = cleanLine.substring(1).trim();
                } else if (!cleanLine.isBlank()) {
                    answerLines.add(cleanLine);
                }
            }

            // Flush last block
            if (currentQuestion != null) {
                questions.add(build(currentQuestion, answerLines, order));
            }
        }

        return questions;
    }

    private Question build(String questionText, List<String> answerLines, int order) {
        String answer = String.join("\n", answerLines).strip();
        return new Question(questionText, answer, order);
    }
}
