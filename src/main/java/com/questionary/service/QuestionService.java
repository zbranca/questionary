package com.questionary.service;

import com.questionary.entity.AppUser;
import com.questionary.entity.Chapter;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.repository.ChapterRepository;
import com.questionary.repository.QuestionRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(
        QuestionService.class
    );

    private final QuestionRepository repo;
    private final ChapterRepository chapterRepo;
    private final ImportService importService;

    public QuestionService(
        QuestionRepository repo,
        ChapterRepository chapterRepo,
        ImportService importService
    ) {
        this.repo = repo;
        this.chapterRepo = chapterRepo;
        this.importService = importService;
    }

    // ── Chapter queries ──────────────────────────────────────────────────────

    public List<Chapter> findAllChapters(AppUser user) {
        return chapterRepo.findAllByUserOrderBySortOrderAsc(user);
    }

    // ── Quiz navigation (no chapter filter) ──────────────────────────────────

    public Optional<Question> findNextUnanswered(AppUser user) {
        return repo
            .findFirstUnanswered(user, QuestionStatus.UNANSWERED)
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextFailed(AppUser user) {
        return repo
            .findFirstByStatusAndUser(user, QuestionStatus.FAILED)
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextUnansweredAfter(
        int sortOrder,
        AppUser user
    ) {
        return repo
            .findFirstUnansweredAfterSortOrder(
                user,
                QuestionStatus.UNANSWERED,
                sortOrder
            )
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextFailedAfter(int sortOrder, AppUser user) {
        return repo
            .findFirstByStatusAfterSortOrder(
                user,
                QuestionStatus.FAILED,
                sortOrder
            )
            .stream()
            .findFirst();
    }

    // ── Quiz navigation (with chapter filter) ────────────────────────────────

    public Optional<Question> findNextUnanswered(
        AppUser user,
        Set<Long> chapterIds
    ) {
        if (
            chapterIds == null || chapterIds.isEmpty()
        ) return findNextUnanswered(user);
        return repo
            .findFirstUnansweredByChapters(
                user,
                QuestionStatus.UNANSWERED,
                chapterIds
            )
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextFailed(
        AppUser user,
        Set<Long> chapterIds
    ) {
        if (chapterIds == null || chapterIds.isEmpty()) return findNextFailed(
            user
        );
        return repo
            .findFirstByStatusAndUserByChapters(
                user,
                QuestionStatus.FAILED,
                chapterIds
            )
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextUnansweredAfter(
        int sortOrder,
        AppUser user,
        Set<Long> chapterIds
    ) {
        if (
            chapterIds == null || chapterIds.isEmpty()
        ) return findNextUnansweredAfter(sortOrder, user);
        return repo
            .findFirstUnansweredAfterSortOrderByChapters(
                user,
                QuestionStatus.UNANSWERED,
                sortOrder,
                chapterIds
            )
            .stream()
            .findFirst();
    }

    public Optional<Question> findNextFailedAfter(
        int sortOrder,
        AppUser user,
        Set<Long> chapterIds
    ) {
        if (
            chapterIds == null || chapterIds.isEmpty()
        ) return findNextFailedAfter(sortOrder, user);
        return repo
            .findFirstByStatusAfterSortOrderByChapters(
                user,
                QuestionStatus.FAILED,
                sortOrder,
                chapterIds
            )
            .stream()
            .findFirst();
    }

    // ── Single lookup ────────────────────────────────────────────────────────

    public Optional<Question> findById(Long id, AppUser user) {
        return repo.findByIdAndUser(id, user);
    }

    // ── Status mutation ──────────────────────────────────────────────────────

    @Transactional
    public void markStatus(Long id, QuestionStatus status, AppUser user) {
        repo
            .findByIdAndUser(id, user)
            .ifPresent(q -> {
                log.info(
                    "User '{}' marked question id={} as {}",
                    user.getUsername(),
                    id,
                    status
                );
                q.setStatus(status);
                repo.save(q);
            });
    }

    // ── Import ───────────────────────────────────────────────────────────────

    @Transactional
    public int importFromFile(MultipartFile file, AppUser user)
        throws IOException {
        String filename = file.getOriginalFilename();
        log.info("User '{}' importing file '{}'", user.getUsername(), filename);

        long existingCount = repo.countByUser(user);
        List<Question> parsed = importService.parse(file.getInputStream());

        // Resolve chapter names to Chapter entities (find-or-create per name+user)
        long chapterCount = chapterRepo.countByUser(user);
        Map<String, Chapter> chapterCache = new HashMap<>();

        // Pre-load existing chapters for this user into the cache
        chapterRepo
            .findAllByUserOrderBySortOrderAsc(user)
            .forEach(c -> chapterCache.put(c.getName(), c));

        for (Question q : parsed) {
            q.setSortOrder((int) (existingCount + q.getSortOrder()));
            q.setUser(user);

            String chapterName = q.getChapterName();
            if (chapterName != null && !chapterName.isBlank()) {
                Chapter chapter = chapterCache.computeIfAbsent(
                    chapterName,
                    name -> {
                        Chapter c = new Chapter(
                            name,
                            (int) (chapterCount + chapterCache.size()),
                            user
                        );
                        return chapterRepo.save(c);
                    }
                );
                q.setChapter(chapter);
            }
        }

        repo.saveAll(parsed);
        log.info(
            "User '{}' imported {} question(s) from '{}' (total now: {})",
            user.getUsername(),
            parsed.size(),
            filename,
            existingCount + parsed.size()
        );
        return parsed.size();
    }

    // ── Admin filtering ──────────────────────────────────────────────────────

    public List<Question> findFiltered(
        String text,
        QuestionStatus statusFilter,
        String chapterFilter,
        AppUser user
    ) {
        List<Question> all = repo.findAllByUserOrderBySortOrderAsc(user);
        final String lowerText = (text != null && !text.isBlank())
            ? text.toLowerCase()
            : null;
        final String lowerChapter = (chapterFilter != null &&
            !chapterFilter.isBlank())
            ? chapterFilter.toLowerCase()
            : null;
        return all
            .stream()
            .filter(q -> statusFilter == null || q.getStatus() == statusFilter)
            .filter(
                q ->
                    lowerText == null ||
                    q.getQuestionText().toLowerCase().contains(lowerText)
            )
            .filter(
                q ->
                    lowerChapter == null ||
                    (q.getChapter() != null &&
                        q
                            .getChapter()
                            .getName()
                            .toLowerCase()
                            .contains(lowerChapter))
            )
            .collect(Collectors.toList());
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public void createQuestion(
        String questionText,
        String answerText,
        QuestionStatus status,
        AppUser user
    ) {
        Question q = new Question();
        q.setQuestionText(questionText);
        q.setAnswerText(answerText);
        q.setStatus(status);
        q.setSortOrder((int) repo.countByUser(user) + 1);
        q.setUser(user);
        repo.save(q);
        log.info(
            "User '{}' created question id={} with status {}",
            user.getUsername(),
            q.getId(),
            status
        );
    }

    @Transactional
    public void updateQuestion(
        Long id,
        String questionText,
        String answerText,
        QuestionStatus status,
        AppUser user
    ) {
        repo
            .findByIdAndUser(id, user)
            .ifPresent(q -> {
                log.info(
                    "User '{}' updated question id={} (status: {} -> {})",
                    user.getUsername(),
                    id,
                    q.getStatus(),
                    status
                );
                q.setQuestionText(questionText);
                q.setAnswerText(answerText);
                q.setStatus(status);
                repo.save(q);
            });
    }

    @Transactional
    public void deleteById(Long id, AppUser user) {
        repo
            .findByIdAndUser(id, user)
            .ifPresent(q -> {
                log.info(
                    "User '{}' deleted question id={}",
                    user.getUsername(),
                    id
                );
                repo.deleteById(q.getId());
            });
    }

    @Transactional
    public void deleteAll(AppUser user) {
        long count = repo.countByUser(user);
        log.warn(
            "User '{}' deleted ALL {} question(s)",
            user.getUsername(),
            count
        );
        repo.deleteAllByUser(user);
    }

    @Transactional
    public void resetAllStatuses(AppUser user) {
        List<Question> all = repo.findAllByUserOrderBySortOrderAsc(user);
        all.forEach(q -> q.setStatus(QuestionStatus.UNANSWERED));
        repo.saveAll(all);
        log.info(
            "User '{}' reset statuses for {} question(s)",
            user.getUsername(),
            all.size()
        );
    }

    // ── Counts ───────────────────────────────────────────────────────────────

    public long countTotal(AppUser user) {
        return repo.countByUser(user);
    }

    public long countUnanswered(AppUser user) {
        return repo.countUnanswered(user, QuestionStatus.UNANSWERED);
    }

    public long countSuccess(AppUser user) {
        return repo.countByStatusAndUser(user, QuestionStatus.SUCCESS);
    }

    public long countFailed(AppUser user) {
        return repo.countByStatusAndUser(user, QuestionStatus.FAILED);
    }
}
