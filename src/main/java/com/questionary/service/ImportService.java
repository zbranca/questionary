package com.questionary.service;

import com.questionary.entity.Question;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    /**
     * Parses a .txt InputStream into Question objects (not persisted).
     *
     * Format:
     * <pre>
     *   #Question text
     *   Answer line 1
     *   Answer line 2
     *
     *   #Next question
     *   Answer
     *
     *   @This line is a comment and is ignored entirely
     * </pre>
     *
     * Rules:
     * <ul>
     *   <li>Lines starting with {@code #} begin a new question block (the {@code #} is stripped).</li>
     *   <li>Lines starting with {@code @} are comment lines and are skipped.</li>
     *   <li>Non-blank lines below a {@code #} line accumulate as the answer.</li>
     *   <li>Blank lines are ignored; only a new {@code #} line closes a block.</li>
     * </ul>
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
                String trimLine = line.strip();

                if (!trimLine.startsWith("@")) {
                    if (trimLine.startsWith("#")) {
                        if (currentQuestion != null) {
                            questions.add(build(currentQuestion, answerLines, order++));
                            answerLines.clear();
                        }
                        currentQuestion = trimLine.substring(1).trim();
                    } else if (!trimLine.isBlank()) {
                        answerLines.add(line);
                    }
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
        String answer = String.join("\n", answerLines);
        return new Question(questionText, answer, order);
    }
}
