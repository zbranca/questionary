package com.questionary.service;

import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository repo;

    @Mock
    private ImportService importService;

    @InjectMocks
    private QuestionService service;

    // ---- findFiltered ----

    @Test
    void findFiltered_nullTextAndNullStatus_callsNoStatusQueryWithNullPattern() {
        when(repo.findFilteredNoStatus(null)).thenReturn(List.of());
        service.findFiltered(null, null);
        verify(repo).findFilteredNoStatus(null);
    }

    @Test
    void findFiltered_blankText_treatedAsNoFilter() {
        when(repo.findFilteredNoStatus(null)).thenReturn(List.of());
        service.findFiltered("   ", null);
        verify(repo).findFilteredNoStatus(null);
    }

    @Test
    void findFiltered_withText_passesLowercaseWildcardPattern() {
        when(repo.findFilteredNoStatus("%java%")).thenReturn(List.of());
        service.findFiltered("Java", null);
        verify(repo).findFilteredNoStatus("%java%");
    }

    @Test
    void findFiltered_unansweredStatus_callsUnansweredVariant() {
        when(repo.findFilteredUnanswered(null, QuestionStatus.UNANSWERED)).thenReturn(List.of());
        service.findFiltered(null, QuestionStatus.UNANSWERED);
        verify(repo).findFilteredUnanswered(null, QuestionStatus.UNANSWERED);
    }

    @Test
    void findFiltered_failedStatus_callsStatusVariant() {
        when(repo.findFilteredByStatus(null, QuestionStatus.FAILED)).thenReturn(List.of());
        service.findFiltered(null, QuestionStatus.FAILED);
        verify(repo).findFilteredByStatus(null, QuestionStatus.FAILED);
    }

    // ---- importFromFile ----

    @Test
    void importFromFile_appliesSortOrderOffsetFromExistingCount() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(repo.count()).thenReturn(5L);
        Question q = new Question("Q", "A", 0);
        when(importService.parse(any())).thenReturn(List.of(q));

        service.importFromFile(file);

        assertEquals(5, q.getSortOrder());
        verify(repo).saveAll(List.of(q));
    }

    // ---- resetAllStatuses ----

    @Test
    void resetAllStatuses_setsAllQuestionsToUnanswered() {
        Question q1 = new Question("Q1", "A1", 0);
        q1.setStatus(QuestionStatus.SUCCESS);
        Question q2 = new Question("Q2", "A2", 1);
        q2.setStatus(QuestionStatus.FAILED);
        when(repo.findAll()).thenReturn(List.of(q1, q2));

        service.resetAllStatuses();

        assertEquals(QuestionStatus.UNANSWERED, q1.getStatus());
        assertEquals(QuestionStatus.UNANSWERED, q2.getStatus());
        verify(repo).saveAll(List.of(q1, q2));
    }

    // ---- markStatus ----

    @Test
    void markStatus_existingQuestion_setsStatusAndSaves() {
        Question q = new Question("Q", "A", 0);
        when(repo.findById(1L)).thenReturn(Optional.of(q));

        service.markStatus(1L, QuestionStatus.SUCCESS);

        assertEquals(QuestionStatus.SUCCESS, q.getStatus());
        verify(repo).save(q);
    }

    @Test
    void markStatus_nonExistentId_isNoOp() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.markStatus(99L, QuestionStatus.SUCCESS);

        verify(repo, never()).save(any());
    }
}
