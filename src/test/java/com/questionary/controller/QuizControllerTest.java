package com.questionary.controller;

import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionService questionService;

    @Test
    void quizHome_whenNoQuestions_redirectsToAdmin() throws Exception {
        when(questionService.countTotal()).thenReturn(0L);

        mockMvc.perform(get("/quiz"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void quizHome_whenUnansweredExists_redirectsToFirstQuestion() throws Exception {
        when(questionService.countTotal()).thenReturn(3L);
        when(questionService.findNextUnanswered()).thenReturn(Optional.of(questionWithId(2L)));

        mockMvc.perform(get("/quiz"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/2"));
    }

    @Test
    void quizHome_whenAllAnswered_redirectsToDone() throws Exception {
        when(questionService.countTotal()).thenReturn(3L);
        when(questionService.findNextUnanswered()).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/done"));
    }

    @Test
    void mark_persistsStatusAndRedirectsToNextUnanswered() throws Exception {
        when(questionService.findNextUnanswered()).thenReturn(Optional.of(questionWithId(5L)));

        mockMvc.perform(post("/quiz/1/mark").param("status", "SUCCESS"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/5"));

        verify(questionService).markStatus(1L, QuestionStatus.SUCCESS);
    }

    @Test
    void mark_whenNoneRemaining_redirectsToDone() throws Exception {
        when(questionService.findNextUnanswered()).thenReturn(Optional.empty());

        mockMvc.perform(post("/quiz/1/mark").param("status", "FAILED"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/done"));
    }

    private Question questionWithId(Long id) {
        Question q = new Question("Q", "A", 0);
        try {
            var field = Question.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(q, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return q;
    }
}
