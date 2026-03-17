package com.questionary.service;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setRole("USER");
    }

    // ---- findFiltered ----

    @Test
    void findFiltered_nullTextAndNullStatus_callsNoStatusQueryWithNullPattern() {
        when(repo.findFilteredNoStatus(user, null)).thenReturn(List.of());
        service.findFiltered(null, null, user);
        verify(repo).findFilteredNoStatus(user, null);
    }

    @Test
    void findFiltered_blankText_treatedAsNoFilter() {
        when(repo.findFilteredNoStatus(user, null)).thenReturn(List.of());
        service.findFiltered("   ", null, user);
        verify(repo).findFilteredNoStatus(user, null);
    }

    @Test
    void findFiltered_withText_passesLowercaseWildcardPattern() {
        when(repo.findFilteredNoStatus(user, "%java%")).thenReturn(List.of());
        service.findFiltered("Java", null, user);
        verify(repo).findFilteredNoStatus(user, "%java%");
    }

    @Test
    void findFiltered_unansweredStatus_callsUnansweredVariant() {
        when(repo.findFilteredUnanswered(user, null, QuestionStatus.UNANSWERED)).thenReturn(List.of());
        service.findFiltered(null, QuestionStatus.UNANSWERED, user);
        verify(repo).findFilteredUnanswered(user, null, QuestionStatus.UNANSWERED);
    }

    @Test
    void findFiltered_failedStatus_callsStatusVariant() {
        when(repo.findFilteredByStatus(user, null, QuestionStatus.FAILED)).thenReturn(List.of());
        service.findFiltered(null, QuestionStatus.FAILED, user);
        verify(repo).findFilteredByStatus(user, null, QuestionStatus.FAILED);
    }

    // ---- importFromFile ----

    @Test
    void importFromFile_appliesSortOrderOffsetFromExistingCount() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(repo.countByUser(user)).thenReturn(5L);
        Question q = new Question("Q", "A", 0);
        when(importService.parse(any())).thenReturn(List.of(q));

        service.importFromFile(file, user);

        assertEquals(5, q.getSortOrder());
        assertEquals(user, q.getUser());
        verify(repo).saveAll(List.of(q));
    }

    // ---- resetAllStatuses ----

    @Test
    void resetAllStatuses_setsAllQuestionsToUnanswered() {
        Question q1 = new Question("Q1", "A1", 0);
        q1.setStatus(QuestionStatus.SUCCESS);
        Question q2 = new Question("Q2", "A2", 1);
        q2.setStatus(QuestionStatus.FAILED);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1, q2));

        service.resetAllStatuses(user);

        assertEquals(QuestionStatus.UNANSWERED, q1.getStatus());
        assertEquals(QuestionStatus.UNANSWERED, q2.getStatus());
        verify(repo).saveAll(List.of(q1, q2));
    }

    // ---- markStatus ----

    @Test
    void markStatus_existingQuestion_setsStatusAndSaves() {
        Question q = new Question("Q", "A", 0);
        when(repo.findByIdAndUser(1L, user)).thenReturn(Optional.of(q));

        service.markStatus(1L, QuestionStatus.SUCCESS, user);

        assertEquals(QuestionStatus.SUCCESS, q.getStatus());
        verify(repo).save(q);
    }

    @Test
    void markStatus_nonExistentId_isNoOp() {
        when(repo.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        service.markStatus(99L, QuestionStatus.SUCCESS, user);

        verify(repo, never()).save(any());
    }
}
