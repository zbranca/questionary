package com.questionary.repository;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.user = :user ORDER BY q.sortOrder ASC")
    List<Question> findAllByUserOrderBySortOrderAsc(@Param("user") AppUser user);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstUnanswered(@Param("user") AppUser user, @Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) AND q.id <> :id ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstUnansweredExcluding(@Param("user") AppUser user, @Param("unanswered") QuestionStatus unanswered, @Param("id") Long id);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered)")
    long countUnanswered(@Param("user") AppUser user, @Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND q.status = :status ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstByStatusAndUser(@Param("user") AppUser user, @Param("status") QuestionStatus status);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND q.status = :status AND q.id <> :id ORDER BY q.sortOrder ASC")
    Optional<Question> findFirstByStatusAndIdNotAndUser(@Param("user") AppUser user, @Param("status") QuestionStatus status, @Param("id") Long id);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user AND q.status = :status")
    long countByStatusAndUser(@Param("user") AppUser user, @Param("status") QuestionStatus status);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user")
    long countByUser(@Param("user") AppUser user);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND q.id = :id")
    Optional<Question> findByIdAndUser(@Param("id") Long id, @Param("user") AppUser user);

    // SQL-level filtered search (3 variants to correctly handle UNANSWERED + legacy null rows)
    @Query("SELECT q FROM Question q WHERE q.user = :user AND " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) AND " +
           "(q.status IS NULL OR q.status = :unanswered) " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredUnanswered(@Param("user") AppUser user,
                                          @Param("pattern") String pattern,
                                          @Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) AND " +
           "q.status = :status " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredByStatus(@Param("user") AppUser user,
                                        @Param("pattern") String pattern,
                                        @Param("status") QuestionStatus status);

    @Query("SELECT q FROM Question q WHERE q.user = :user AND " +
           "(:pattern IS NULL OR LOWER(q.questionText) LIKE :pattern) " +
           "ORDER BY q.sortOrder ASC")
    List<Question> findFilteredNoStatus(@Param("user") AppUser user,
                                        @Param("pattern") String pattern);

    @Modifying
    @Query("DELETE FROM Question q WHERE q.user = :user")
    void deleteAllByUser(@Param("user") AppUser user);
}
