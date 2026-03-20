package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.security.AppUserDetails;
import com.questionary.security.AppUserDetailsService;
import com.questionary.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

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
    void adminPage_returnsAdminViewWithModel() throws Exception {
        when(questionService.findFiltered(any(), any(), any())).thenReturn(List.of());
        when(questionService.countTotal(any())).thenReturn(0L);
        when(questionService.countUnanswered(any())).thenReturn(0L);
        when(questionService.countSuccess(any())).thenReturn(0L);
        when(questionService.countFailed(any())).thenReturn(0L);

        mockMvc.perform(get("/admin").with(user(principal)))
               .andExpect(status().isOk())
               .andExpect(view().name("admin"))
               .andExpect(model().attributeExists("questions", "totalCount", "filterText"));
    }

    @Test
    void importFile_validTxtFile_flashesSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.txt", "text/plain", "#Q\nA\n".getBytes());
        when(questionService.importFromFile(any(), any())).thenReturn(1);

        mockMvc.perform(multipart("/admin/import").file(file).with(user(principal)).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void importFile_nonTxtFile_flashesError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/admin/import").file(file).with(user(principal)).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("errorMessage"));

        verify(questionService, never()).importFromFile(any(), any());
    }

    @Test
    void importFile_emptyFile_flashesError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/admin/import").file(file).with(user(principal)).with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("errorMessage"));
    }
}
