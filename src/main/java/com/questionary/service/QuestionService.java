package com.questionary.service;

import com.questionary.entity.Question;
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
        return repo.findFirstByStatusIsNullOrderBySortOrderAsc();
    }

    public Optional<Question> findNextUnansweredExcluding(Long id) {
        return repo.findFirstByStatusIsNullAndIdNotOrderBySortOrderAsc(id);
    }

    public Optional<Question> findById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public void markStatus(Long id, String status) {
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

    public List<Question> findAll() {
        return repo.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public void deleteAll() {
        repo.deleteAll();
    }

    @Transactional
    public void resetAllStatuses() {
        List<Question> all = repo.findAll();
        all.forEach(q -> q.setStatus(null));
        repo.saveAll(all);
    }

    public long countTotal()      { return repo.count(); }
    public long countUnanswered() { return repo.countByStatusIsNull(); }
    public long countSuccess()    { return repo.countByStatus("SUCCESS"); }
    public long countFailed()     { return repo.countByStatus("FAILED"); }
}
