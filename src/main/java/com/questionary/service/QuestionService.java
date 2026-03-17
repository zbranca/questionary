package com.questionary.service;

import com.questionary.entity.AppUser;
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

    public Optional<Question> findNextUnanswered(AppUser user) {
        return repo.findFirstUnanswered(user, QuestionStatus.UNANSWERED);
    }

    public Optional<Question> findNextUnansweredExcluding(Long id, AppUser user) {
        return repo.findFirstUnansweredExcluding(user, QuestionStatus.UNANSWERED, id);
    }

    public Optional<Question> findNextFailed(AppUser user) {
        return repo.findFirstByStatusAndUser(user, QuestionStatus.FAILED);
    }

    public Optional<Question> findNextFailedExcluding(Long id, AppUser user) {
        return repo.findFirstByStatusAndIdNotAndUser(user, QuestionStatus.FAILED, id);
    }

    public Optional<Question> findById(Long id, AppUser user) {
        return repo.findByIdAndUser(id, user);
    }

    @Transactional
    public void markStatus(Long id, QuestionStatus status, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> {
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public int importFromFile(MultipartFile file, AppUser user) throws IOException {
        long existingCount = repo.countByUser(user);
        List<Question> parsed = importService.parse(file.getInputStream());
        parsed.forEach(q -> {
            q.setSortOrder((int) (existingCount + q.getSortOrder()));
            q.setUser(user);
        });
        repo.saveAll(parsed);
        return parsed.size();
    }

    public List<Question> findFiltered(String text, QuestionStatus statusFilter, AppUser user) {
        String pattern = (text != null && !text.isBlank()) ? "%" + text.toLowerCase() + "%" : null;
        if (statusFilter == null) {
            return repo.findFilteredNoStatus(user, pattern);
        }
        if (statusFilter == QuestionStatus.UNANSWERED) {
            return repo.findFilteredUnanswered(user, pattern, QuestionStatus.UNANSWERED);
        }
        return repo.findFilteredByStatus(user, pattern, statusFilter);
    }

    @Transactional
    public void updateQuestion(Long id, String questionText, String answerText, QuestionStatus status, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> {
            q.setQuestionText(questionText);
            q.setAnswerText(answerText);
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public void deleteById(Long id, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> repo.deleteById(q.getId()));
    }

    @Transactional
    public void deleteAll(AppUser user) {
        repo.deleteAllByUser(user);
    }

    @Transactional
    public void resetAllStatuses(AppUser user) {
        List<Question> all = repo.findAllByUserOrderBySortOrderAsc(user);
        all.forEach(q -> q.setStatus(QuestionStatus.UNANSWERED));
        repo.saveAll(all);
    }

    public long countTotal(AppUser user)      { return repo.countByUser(user); }
    public long countUnanswered(AppUser user) { return repo.countUnanswered(user, QuestionStatus.UNANSWERED); }
    public long countSuccess(AppUser user)    { return repo.countByStatusAndUser(user, QuestionStatus.SUCCESS); }
    public long countFailed(AppUser user)     { return repo.countByStatusAndUser(user, QuestionStatus.FAILED); }
}
