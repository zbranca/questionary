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
     *   @@Chapter Name          — starts a chapter; all following questions belong to it
     *
     *   #Question line 1
     *   #Question line 2        — consecutive # lines form one multi-line question
     *   #
     *   #```java
     *   #int x = 5;
     *   #```
     *   Answer line 1
     *   @This line is now a normal answer line (single @ no longer means comment)
     *   Answer line 2
     *
     *   #Next question
     *   Answer
     *
     *   @@Next Chapter          — closes previous chapter, starts a new one
     *   #First question of next chapter
     *   Answer
     * </pre>
     *
     * Rules:
     * <ul>
     *   <li>Lines starting with {@code @@} start a new chapter (the {@code @@} is stripped and name is trimmed).</li>
     *   <li>Lines starting with {@code #} begin or continue a question block (the {@code #} is stripped).</li>
     *   <li>Multiple consecutive {@code #} lines combine into a single multi-line question text.</li>
     *   <li>A bare {@code #} line (nothing after {@code #}) embeds a blank line within the question text.</li>
     *   <li>Lines starting with single {@code @} (not {@code @@}) are treated as regular content lines.</li>
     *   <li>Non-blank, non-{@code #}, non-{@code @@} lines after a question block accumulate as the answer.</li>
     *   <li>Blank file-lines are ignored in all states; a new {@code #} line after answer lines starts a new block.</li>
     * </ul>
     */
    public List<Question> parse(InputStream inputStream) throws IOException {
        List<Question> questions = new ArrayList<>();
        List<String> questionLines = new ArrayList<>();
        List<String> answerLines = new ArrayList<>();
        boolean hasActiveBlock = false;
        boolean inQuestion = false;
        String currentChapterName = null;
        int order = 0;

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimLine = line.strip();

                // Chapter header — flush current block, start new chapter
                if (trimLine.startsWith("@@")) {
                    if (hasActiveBlock) {
                        questions.add(
                            build(
                                questionLines,
                                answerLines,
                                order++,
                                currentChapterName
                            )
                        );
                        questionLines.clear();
                        answerLines.clear();
                        hasActiveBlock = false;
                        inQuestion = false;
                    }
                    currentChapterName = trimLine.substring(2).strip();
                    continue;
                }

                if (trimLine.startsWith("#")) {
                    if (hasActiveBlock && !inQuestion) {
                        // answer phase complete — flush and start new block
                        questions.add(
                            build(
                                questionLines,
                                answerLines,
                                order++,
                                currentChapterName
                            )
                        );
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

            // Flush last block
            if (hasActiveBlock) {
                questions.add(
                    build(questionLines, answerLines, order, currentChapterName)
                );
            }
        }

        return questions;
    }

    private Question build(
        List<String> questionLines,
        List<String> answerLines,
        int order,
        String chapterName
    ) {
        String questionText = String.join("\n", questionLines);
        String answerText = String.join("\n", answerLines);
        Question q = new Question(questionText, answerText, order);
        q.setChapterName(chapterName);
        return q;
    }
}
