package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.security.AppUserDetails;
import com.questionary.security.AppUserDetailsService;
import com.questionary.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionService questionService;

    @MockBean
    private AppUserDetailsService userDetailsService;

    private AppUserDetails principal;

    @BeforeEach
    void setUp() {
        AppUser testUser = new AppUser();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRole(AppUser.ROLE_USER);
        principal = new AppUserDetails(testUser);
    }

    @Test
    void quizHome_whenNoQuestions_redirectsToAdmin() throws Exception {
        when(questionService.countTotal(any())).thenReturn(0L);

        mockMvc.perform(get("/quiz").with(user(principal)))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void quizHome_whenUnansweredExists_redirectsToFirstQuestion() throws Exception {
        when(questionService.countTotal(any())).thenReturn(3L);
        when(questionService.findNextUnanswered(any())).thenReturn(Optional.of(questionWithId(2L)));

        mockMvc.perform(get("/quiz").with(user(principal)))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/2"));
    }

    @Test
    void quizHome_whenAllAnswered_redirectsToDone() throws Exception {
        when(questionService.countTotal(any())).thenReturn(3L);
        when(questionService.findNextUnanswered(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz").with(user(principal)))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/done"));
    }

    @Test
    void mark_persistsStatusAndRedirectsToNextUnanswered() throws Exception {
        when(questionService.findNextUnanswered(any())).thenReturn(Optional.of(questionWithId(5L)));

        mockMvc.perform(post("/quiz/1/mark").param("status", "SUCCESS").with(user(principal)).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/quiz/5"));

        verify(questionService).markStatus(eq(1L), eq(QuestionStatus.SUCCESS), any());
    }

    @Test
    void mark_whenNoneRemaining_redirectsToDone() throws Exception {
        when(questionService.findNextUnanswered(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/quiz/1/mark").param("status", "FAILED").with(user(principal)).with(csrf()))
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
