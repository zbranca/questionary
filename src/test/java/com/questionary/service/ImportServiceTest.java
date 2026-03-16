package com.questionary.service;

import com.questionary.entity.Question;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportServiceTest {

    private final ImportService service = new ImportService();

    private List<Question> parse(String input) throws IOException {
        return service.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

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
    void commentLines_startingWithAt_areIgnored() throws IOException {
        String input = "@This is a comment\n#Question\nAnswer\n@Another comment\n";
        List<Question> result = parse(input);
        assertEquals(1, result.size());
        assertEquals("Question", result.get(0).getQuestionText());
        assertEquals("Answer", result.get(0).getAnswerText());
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
}
