package com.questionary.service;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository repo;
    private final ImportService importService;

    public QuestionService(QuestionRepository repo, ImportService importService) {
        this.repo = repo;
        this.importService = importService;
    }

    public Optional<Question> findNextUnanswered(AppUser user) {
        return repo.findFirstUnanswered(user, QuestionStatus.UNANSWERED).stream().findFirst();
    }

    public Optional<Question> findNextUnansweredExcluding(Long id, AppUser user) {
        return repo.findFirstUnansweredExcluding(user, QuestionStatus.UNANSWERED, id).stream().findFirst();
    }

    public Optional<Question> findNextFailed(AppUser user) {
        return repo.findFirstByStatusAndUser(user, QuestionStatus.FAILED).stream().findFirst();
    }

    public Optional<Question> findNextFailedExcluding(Long id, AppUser user) {
        return repo.findFirstByStatusAndIdNotAndUser(user, QuestionStatus.FAILED, id).stream().findFirst();
    }

    public Optional<Question> findById(Long id, AppUser user) {
        return repo.findByIdAndUser(id, user);
    }

    @Transactional
    public void markStatus(Long id, QuestionStatus status, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> {
            log.info("User '{}' marked question id={} as {}", user.getUsername(), id, status);
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public int importFromFile(MultipartFile file, AppUser user) throws IOException {
        String filename = file.getOriginalFilename();
        log.info("User '{}' importing file '{}'", user.getUsername(), filename);
        long existingCount = repo.countByUser(user);
        List<Question> parsed = importService.parse(file.getInputStream());
        parsed.forEach(q -> {
            q.setSortOrder((int) (existingCount + q.getSortOrder()));
            q.setUser(user);
        });
        repo.saveAll(parsed);
        log.info("User '{}' imported {} question(s) from '{}' (total now: {})",
                user.getUsername(), parsed.size(), filename, existingCount + parsed.size());
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
    public void createQuestion(String questionText, String answerText, QuestionStatus status, AppUser user) {
        Question q = new Question();
        q.setQuestionText(questionText);
        q.setAnswerText(answerText);
        q.setStatus(status);
        q.setSortOrder((int) repo.countByUser(user) + 1);
        q.setUser(user);
        repo.save(q);
        log.info("User '{}' created question id={} with status {}", user.getUsername(), q.getId(), status);
    }

    @Transactional
    public void updateQuestion(Long id, String questionText, String answerText, QuestionStatus status, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> {
            log.info("User '{}' updated question id={} (status: {} -> {})",
                    user.getUsername(), id, q.getStatus(), status);
            q.setQuestionText(questionText);
            q.setAnswerText(answerText);
            q.setStatus(status);
            repo.save(q);
        });
    }

    @Transactional
    public void deleteById(Long id, AppUser user) {
        repo.findByIdAndUser(id, user).ifPresent(q -> {
            log.info("User '{}' deleted question id={}", user.getUsername(), id);
            repo.deleteById(q.getId());
        });
    }

    @Transactional
    public void deleteAll(AppUser user) {
        long count = repo.countByUser(user);
        log.warn("User '{}' deleted ALL {} question(s)", user.getUsername(), count);
        repo.deleteAllByUser(user);
    }

    @Transactional
    public void resetAllStatuses(AppUser user) {
        List<Question> all = repo.findAllByUserOrderBySortOrderAsc(user);
        all.forEach(q -> q.setStatus(QuestionStatus.UNANSWERED));
        repo.saveAll(all);
        log.info("User '{}' reset statuses for {} question(s)", user.getUsername(), all.size());
    }

    public long countTotal(AppUser user)      { return repo.countByUser(user); }
    public long countUnanswered(AppUser user) { return repo.countUnanswered(user, QuestionStatus.UNANSWERED); }
    public long countSuccess(AppUser user)    { return repo.countByStatusAndUser(user, QuestionStatus.SUCCESS); }
    public long countFailed(AppUser user)     { return repo.countByStatusAndUser(user, QuestionStatus.FAILED); }
}
