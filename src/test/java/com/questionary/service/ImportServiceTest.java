package com.questionary.service;

import static org.junit.jupiter.api.Assertions.*;

import com.questionary.entity.Question;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportServiceTest {

    private final ImportService service = new ImportService();

    private List<Question> parse(String input) throws IOException {
        return service.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    // ---- existing tests (backward-compatibility regression suite) ----

    @Test
    void emptyInput_returnsEmptyList() throws IOException {
        assertTrue(parse("").isEmpty());
    }

    @Test
    void singleQuestion_withAnswer() throws IOException {
        List<Question> result = parse("#What is 2+2?\n4\n");
        assertEquals(1, result.size());
        assertEquals("What is 2+2?", result.get(0).getQuestionText());
        assertEquals("4", result.get(0).getAnswerText());
        assertEquals(0, result.get(0).getSortOrder());
    }

    @Test
    void singleQuestion_withNoAnswer() throws IOException {
        List<Question> result = parse("#Question with no answer\n");
        assertEquals(1, result.size());
        assertEquals("Question with no answer", result.get(0).getQuestionText());
        assertEquals("", result.get(0).getAnswerText());
    }

    @Test
    void multipleBlocks_parsedInOrder() throws IOException {
        String input = "#Q1\nA1\n\n#Q2\nA2a\nA2b\n";
        List<Question> result = parse(input);
        assertEquals(2, result.size());
        assertEquals("Q1", result.get(0).getQuestionText());
        assertEquals("A1", result.get(0).getAnswerText());
        assertEquals(0, result.get(0).getSortOrder());
        assertEquals("Q2", result.get(1).getQuestionText());
        assertEquals("A2a\nA2b", result.get(1).getAnswerText());
        assertEquals(1, result.get(1).getSortOrder());
    }

    @Test
    void atLines_areNowRegularContent_notComments() throws IOException {
        // Single @ lines are no longer comments — they are treated as regular answer lines
        String input = "@This is not a comment\n#Question\nAnswer\n@Another line\n";
        List<Question> result = parse(input);
        // The first @-line appears before any question block, so it is ignored (no active block)
        assertEquals(1, result.size());
        assertEquals("Question", result.get(0).getQuestionText());
        assertEquals("Answer\n@Another line", result.get(0).getAnswerText());
    }

    @Test
    void blankLinesInAnswer_areIgnored() throws IOException {
        String input = "#Q\nLine1\n\nLine2\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Line1\nLine2", result.get(0).getAnswerText());
    }

    @Test
    void leadingHashIsStrippedAndQuestionIsTrimmed() throws IOException {
        List<Question> result = parse("#  Trimmed question  \nAnswer\n");
        assertEquals("Trimmed question", result.get(0).getQuestionText());
    }

    @Test
    void lastBlock_withoutTrailingNewline_isFlushed() throws IOException {
        List<Question> result = parse("#Last question\nFinal answer");
        assertEquals(1, result.size());
        assertEquals("Last question", result.get(0).getQuestionText());
        assertEquals("Final answer", result.get(0).getAnswerText());
    }

    // ---- new tests: multi-line question text ----

    @Test
    void multilineQuestion_consecutiveHashLines_merged() throws IOException {
        String input = "#Line one\n#Line two\nAnswer\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Line one\nLine two", result.get(0).getQuestionText());
        assertEquals("Answer", result.get(0).getAnswerText());
    }

    @Test
    void multilineQuestion_bareHashLine_producesBlankLineInText() throws IOException {
        String input = "#Q line1\n#\n#Q line2\nAnswer\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Q line1\n\nQ line2", result.get(0).getQuestionText());
    }

    @Test
    void multilineQuestion_withCodeBlock_parsedCorrectly() throws IOException {
        String input =
            "#What is the output?\n" + "#\n" + "#```java\n" + "#int x = 5;\n" + "#```\n" + "The output is 10.\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("What is the output?\n\n```java\nint x = 5;\n```", result.get(0).getQuestionText());
        assertEquals("The output is 10.", result.get(0).getAnswerText());
    }

    @Test
    void multilineQuestion_followedByAnotherQuestion_bothParsed() throws IOException {
        String input = "#Q1 line1\n#Q1 line2\nA1\n\n#Q2\nA2\n";
        List<Question> result = parse(input);
        assertEquals(2, result.size());
        assertEquals("Q1 line1\nQ1 line2", result.get(0).getQuestionText());
        assertEquals("A1", result.get(0).getAnswerText());
        assertEquals("Q2", result.get(1).getQuestionText());
        assertEquals("A2", result.get(1).getAnswerText());
    }

    @Test
    void multilineQuestion_blankFileLinesIgnored_blockContinues() throws IOException {
        String input = "#Q1 line1\n\n#Q1 line2\nAnswer\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Q1 line1\nQ1 line2", result.get(0).getQuestionText());
    }

    @Test
    void atLineBetweenHashLines_endsQuestionAccumulation() throws IOException {
        // A single @ line is now a regular answer line, so it ends the question accumulation
        // and starts the answer phase — this produces two separate questions
        String input = "#Q line1\n@not-a-comment\n#Q line2\nAnswer\n";
        List<Question> result = parse(input);
        // @not-a-comment ends Q1's question phase (becomes Q1's answer), then #Q line2 starts Q2
        assertEquals(2, result.size());
        assertEquals("Q line1", result.get(0).getQuestionText());
        assertEquals("@not-a-comment", result.get(0).getAnswerText());
        assertEquals("Q line2", result.get(1).getQuestionText());
        assertEquals("Answer", result.get(1).getAnswerText());
    }

    @Test
    void chapterHeader_setsChapterNameOnFollowingQuestions() throws IOException {
        String input = "@@Java Basics\n#What is Java?\nA compiled language.\n\n#What is JVM?\nA virtual machine.\n";
        List<Question> result = parse(input);
        assertEquals(2, result.size());
        assertEquals("Java Basics", result.get(0).getChapterName());
        assertEquals("Java Basics", result.get(1).getChapterName());
    }

    @Test
    void multipleChapters_questionsGetCorrectChapterNames() throws IOException {
        String input = "@@Chapter One\n#Q1\nA1\n\n@@Chapter Two\n#Q2\nA2\n";
        List<Question> result = parse(input);
        assertEquals(2, result.size());
        assertEquals("Chapter One", result.get(0).getChapterName());
        assertEquals("Chapter Two", result.get(1).getChapterName());
    }

    @Test
    void noChapterHeader_chapterNameIsNull() throws IOException {
        String input = "#Q1\nA1\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertNull(result.get(0).getChapterName());
    }

    @Test
    void chapterName_isTrimmed() throws IOException {
        String input = "@@  Trimmed Chapter  \n#Q\nA\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Trimmed Chapter", result.get(0).getChapterName());
    }

    @Test
    void doubleAtPrefix_isChapter_singleAtIsContent() throws IOException {
        // @@ = chapter, single @ = regular answer line
        String input = "@@My Chapter\n#Q\nAnswer\n@regular line\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("My Chapter", result.get(0).getChapterName());
        assertEquals("Answer\n@regular line", result.get(0).getAnswerText());
    }
}
