package com.questionary.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.ChapterRepository;
import com.questionary.repository.QuestionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository repo;

    @Mock
    private ChapterRepository chapterRepo;

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
        user.setRole(AppUser.ROLE_USER);
    }

    // ---- findFiltered (in-memory filtering) ----

    private Question questionWithStatus(String questionText, QuestionStatus status, int order) {
        Question q = new Question(questionText, "A", order);
        q.setStatus(status);
        return q;
    }

    @Test
    void findFiltered_nullTextAndNullStatus_returnsAllQuestions() {
        Question q1 = questionWithStatus("Q1", QuestionStatus.SUCCESS, 0);
        Question q2 = questionWithStatus("Q2", QuestionStatus.FAILED, 1);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1, q2));
        assertEquals(2, service.findFiltered(null, null, null, user).size());
    }

    @Test
    void findFiltered_blankText_returnsAllQuestions() {
        Question q1 = questionWithStatus("Q1", QuestionStatus.UNANSWERED, 0);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1));
        assertEquals(1, service.findFiltered("   ", null, null, user).size());
    }

    @Test
    void findFiltered_withText_returnsMatchingQuestions() {
        Question q1 = questionWithStatus("Java basics", QuestionStatus.UNANSWERED, 0);
        Question q2 = questionWithStatus("Python basics", QuestionStatus.UNANSWERED, 1);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1, q2));
        List<Question> result = service.findFiltered("java", null, null, user);
        assertEquals(1, result.size());
        assertEquals("Java basics", result.get(0).getQuestionText());
    }

    @Test
    void findFiltered_unansweredStatus_returnsOnlyUnanswered() {
        Question q1 = questionWithStatus("Q1", QuestionStatus.UNANSWERED, 0);
        Question q2 = questionWithStatus("Q2", QuestionStatus.SUCCESS, 1);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1, q2));
        List<Question> result = service.findFiltered(null, QuestionStatus.UNANSWERED, null, user);
        assertEquals(1, result.size());
        assertEquals(QuestionStatus.UNANSWERED, result.get(0).getStatus());
    }

    @Test
    void findFiltered_failedStatus_returnsOnlyFailed() {
        Question q1 = questionWithStatus("Q1", QuestionStatus.FAILED, 0);
        Question q2 = questionWithStatus("Q2", QuestionStatus.SUCCESS, 1);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1, q2));
        List<Question> result = service.findFiltered(null, QuestionStatus.FAILED, null, user);
        assertEquals(1, result.size());
        assertEquals(QuestionStatus.FAILED, result.get(0).getStatus());
    }

    @Test
    void findFiltered_statusWithZeroMatches_returnsEmptyList() {
        Question q1 = questionWithStatus("Q1", QuestionStatus.UNANSWERED, 0);
        when(repo.findAllByUserOrderBySortOrderAsc(user)).thenReturn(List.of(q1));
        assertEquals(0, service.findFiltered(null, QuestionStatus.SUCCESS, null, user).size());
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
