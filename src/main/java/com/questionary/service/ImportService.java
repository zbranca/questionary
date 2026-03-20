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
     *   #Question line 1
     *   #Question line 2 (consecutive # lines form one multi-line question)
     *   #
     *   #```java
     *   #int x = 5;
     *   #```
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
     *   <li>Lines starting with {@code #} begin or continue a question block (the {@code #} is stripped).</li>
     *   <li>Multiple consecutive {@code #} lines combine into a single multi-line question text.</li>
     *   <li>A bare {@code #} line (nothing after {@code #}) embeds a blank line within the question text.</li>
     *   <li>Lines starting with {@code @} are comment lines and are skipped.</li>
     *   <li>Non-blank, non-{@code #} lines after a question block accumulate as the answer.</li>
     *   <li>Blank file-lines are ignored in all states; a new {@code #} line after answer lines starts a new block.</li>
     * </ul>
     */
    public List<Question> parse(InputStream inputStream) throws IOException {
        List<Question> questions = new ArrayList<>();
        List<String> questionLines = new ArrayList<>();
        List<String> answerLines = new ArrayList<>();
        boolean hasActiveBlock = false;
        boolean inQuestion = false;
        int order = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimLine = line.strip();

                if (trimLine.startsWith("@")) {
                    continue; // comment line — skip
                }

                if (trimLine.startsWith("#")) {
                    if (hasActiveBlock && !inQuestion) {
                        // answer phase complete — flush and start new block
                        questions.add(build(questionLines, answerLines, order++));
                        questionLines.clear();
                        answerLines.clear();
                    }
                    hasActiveBlock = true;
                    inQuestion = true;
                    questionLines.add(trimLine.substring(1).strip());
                } else if (!trimLine.isBlank()) {
                    if (hasActiveBlock && inQuestion) {
                        inQuestion = false; // first answer line ends question accumulation
                    }
                    if (hasActiveBlock) {
                        answerLines.add(line); // preserve original indentation
                    }
                }
                // blank lines are ignored in all states
            }

        flushLastBlock(hasActiveBlock, questions, questionLines, answerLines, order);
        }

        return questions;
    }

  private void flushLastBlock(
      boolean hasActiveBlock,
      List<Question> questions,
      List<String> questionLines,
      List<String> answerLines,
      int order) {
    // Flush last block
    if (hasActiveBlock) {
      questions.add(build(questionLines, answerLines, order));
    }
  }

  private Question build(List<String> questionLines, List<String> answerLines, int order) {
        String questionText = String.join("\n", questionLines);
        String answerText = String.join("\n", answerLines);
        return new Question(questionText, answerText, order);
    }
}
