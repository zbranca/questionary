package com.questionary.controller;

import com.questionary.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionService questionService;

    @Test
    void adminPage_returnsAdminViewWithModel() throws Exception {
        when(questionService.findFiltered(any(), any())).thenReturn(List.of());
        when(questionService.countTotal()).thenReturn(0L);
        when(questionService.countUnanswered()).thenReturn(0L);
        when(questionService.countSuccess()).thenReturn(0L);
        when(questionService.countFailed()).thenReturn(0L);

        mockMvc.perform(get("/admin"))
               .andExpect(status().isOk())
               .andExpect(view().name("admin"))
               .andExpect(model().attributeExists("questions", "totalCount", "filterText"));
    }

    @Test
    void importFile_validTxtFile_flashesSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.txt", "text/plain", "#Q\nA\n".getBytes());
        when(questionService.importFromFile(any())).thenReturn(1);

        mockMvc.perform(multipart("/admin/import").file(file))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void importFile_nonTxtFile_flashesError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/admin/import").file(file))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("errorMessage"));

        verify(questionService, never()).importFromFile(any());
    }

    @Test
    void importFile_emptyFile_flashesError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/admin/import").file(file))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin"))
               .andExpect(flash().attributeExists("errorMessage"));
    }
}
