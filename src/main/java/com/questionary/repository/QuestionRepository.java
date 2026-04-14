package com.questionary.repository;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // ── All questions for user ──────────────────────────────────────────────

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user ORDER BY q.sortOrder ASC")
    List<Question> findAllByUserOrderBySortOrderAsc(@Param("user") AppUser user);

    // ── Unanswered navigation (no chapter filter) ───────────────────────────

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstUnanswered(@Param("user") AppUser user, @Param("unanswered") QuestionStatus unanswered);

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) AND q.sortOrder > :sortOrder ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstUnansweredAfterSortOrder(
        @Param("user") AppUser user,
        @Param("unanswered") QuestionStatus unanswered,
        @Param("sortOrder") int sortOrder
    );

    // ── Unanswered navigation (with chapter filter) ─────────────────────────

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) AND q.chapter.id IN :chapterIds ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstUnansweredByChapters(
        @Param("user") AppUser user,
        @Param("unanswered") QuestionStatus unanswered,
        @Param("chapterIds") Collection<Long> chapterIds
    );

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered) AND q.chapter.id IN :chapterIds AND q.sortOrder > :sortOrder ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstUnansweredAfterSortOrderByChapters(
        @Param("user") AppUser user,
        @Param("unanswered") QuestionStatus unanswered,
        @Param("sortOrder") int sortOrder,
        @Param("chapterIds") Collection<Long> chapterIds
    );

    // ── Failed navigation (no chapter filter) ───────────────────────────────

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND q.status = :status ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstByStatusAndUser(@Param("user") AppUser user, @Param("status") QuestionStatus status);

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND q.status = :status AND q.sortOrder > :sortOrder ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstByStatusAfterSortOrder(
        @Param("user") AppUser user,
        @Param("status") QuestionStatus status,
        @Param("sortOrder") int sortOrder
    );

    // ── Failed navigation (with chapter filter) ──────────────────────────────

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND q.status = :status AND q.chapter.id IN :chapterIds ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstByStatusAndUserByChapters(
        @Param("user") AppUser user,
        @Param("status") QuestionStatus status,
        @Param("chapterIds") Collection<Long> chapterIds
    );

    @Query(
        "SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND q.status = :status AND q.sortOrder > :sortOrder AND q.chapter.id IN :chapterIds ORDER BY q.sortOrder ASC"
    )
    List<Question> findFirstByStatusAfterSortOrderByChapters(
        @Param("user") AppUser user,
        @Param("status") QuestionStatus status,
        @Param("sortOrder") int sortOrder,
        @Param("chapterIds") Collection<Long> chapterIds
    );

    // ── Counts ───────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user AND (q.status IS NULL OR q.status = :unanswered)")
    long countUnanswered(@Param("user") AppUser user, @Param("unanswered") QuestionStatus unanswered);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user AND q.status = :status")
    long countByStatusAndUser(@Param("user") AppUser user, @Param("status") QuestionStatus status);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.user = :user")
    long countByUser(@Param("user") AppUser user);

    // ── Single lookup ────────────────────────────────────────────────────────

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.chapter WHERE q.user = :user AND q.id = :id")
    Optional<Question> findByIdAndUser(@Param("id") Long id, @Param("user") AppUser user);

    // ── Bulk ops ─────────────────────────────────────────────────────────────

    @Modifying
    @Query("DELETE FROM Question q WHERE q.user = :user")
    void deleteAllByUser(@Param("user") AppUser user);
}
