package com.questionary.repository;

import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderBySortOrderAsc();

    // Handles both legacy null rows and new "UNANSWERED" string stored explicitly
    @Query("SELECT q FROM Question q WHERE (q.status IS NULL OR q.status = :unanswered) ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstUnanswered(@Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT q FROM Question q WHERE (q.status IS NULL OR q.status = :unanswered) AND q.id <> :id ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstUnansweredExcluding(@Param("unanswered") QuestionStatus unanswered, @Param("id") Long id);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.status IS NULL OR q.status = :unanswered")
    long countUnanswered(@Param("unanswered") QuestionStatus unanswered);

    Optional<Question> findFirstByStatusOrderBySortOrderAsc(QuestionStatus status);

    Optional<Question> findFirstByStatusAndIdNotOrderBySortOrderAsc(QuestionStatus status, Long id);

    long countByStatus(QuestionStatus status);

    // SQL-level filtered search (3 variants to correctly handle UNANSWERED + legacy null rows)
    @Query("SELECT q FROM Question q WHERE " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) AND " +
           "(q.status IS NULL OR q.status = :unanswered) " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredUnanswered(@Param("pattern") String pattern,
                                          @Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT q FROM Question q WHERE " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) AND " +
           "q.status = :status " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredByStatus(@Param("pattern") String pattern,
                                        @Param("status") QuestionStatus status);

    @Query("SELECT q FROM Question q WHERE " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredNoStatus(@Param("pattern") String pattern);
}
