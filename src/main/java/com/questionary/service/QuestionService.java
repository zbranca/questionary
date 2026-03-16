package com.questionary.service;

import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private final QuestionRepository repo;
    private final ImportService importService;

    public QuestionService(QuestionRepository repo, ImportService importService) {
        this.repo = repo;
        this.importService = importService;
    }

    public Optional<Question> findNextUnanswered() {
        return repo.findFirstUnanswered(QuestionStatus.UNANSWERED);
    }

    public Optional<Question> findNextUnansweredExcluding(Long id) {
        return repo.findFirstUnansweredExcluding(QuestionStatus.UNANSWERED, id);
    }

    public Optional<Question> findNextFailed() {
        return repo.findFirstByStatusOrderBySortOrderAsc(QuestionStatus.FAILED);
    }

    public Optional<Question> findNextFailedExcluding(Long id) {
        return repo.findFirstByStatusAndIdNotOrderBySortOrderAsc(QuestionStatus.FAILED, id);
    }

    public Optional<Question> findById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public void markStatus(Long id, QuestionStatus status) {
        repo.findById(id).ifPresent(q -> {
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public int importFromFile(MultipartFile file) throws IOException {
        long existingCount = repo.count();
        List<Question> parsed = importService.parse(file.getInputStream());
        parsed.forEach(q -> q.setSortOrder((int) (existingCount + q.getSortOrder())));
        repo.saveAll(parsed);
        return parsed.size();
    }

    /**
     * Returns questions matching optional text search and status filter.
     * Filtering is performed at the SQL level. Passing null/blank text or null status
     * removes the respective filter.
     */
    public List<Question> findFiltered(String text, QuestionStatus statusFilter) {
        String pattern = (text != null && !text.isBlank()) ? "%" + text.toLowerCase() + "%" : null;
        if (statusFilter == null) {
            return repo.findFilteredNoStatus(pattern);
        }
        if (statusFilter == QuestionStatus.UNANSWERED) {
            return repo.findFilteredUnanswered(pattern, QuestionStatus.UNANSWERED);
        }
        return repo.findFilteredByStatus(pattern, statusFilter);
    }

    @Transactional
    public void updateQuestion(Long id, String questionText, String answerText, QuestionStatus status) {
        repo.findById(id).ifPresent(q -> {
            q.setQuestionText(questionText);
            q.setAnswerText(answerText);
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        repo.deleteAll();
    }

    @Transactional
    public void resetAllStatuses() {
        List<Question> all = repo.findAll();
        all.forEach(q -> q.setStatus(QuestionStatus.UNANSWERED));
        repo.saveAll(all);
    }

    public long countTotal()      { return repo.count(); }
    public long countUnanswered() { return repo.countUnanswered(QuestionStatus.UNANSWERED); }
    public long countSuccess()    { return repo.countByStatus(QuestionStatus.SUCCESS); }
    public long countFailed()     { return repo.countByStatus(QuestionStatus.FAILED); }
}
