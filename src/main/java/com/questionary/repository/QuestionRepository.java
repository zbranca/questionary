package com.questionary.repository;

import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderBySortOrderAsc();

    Optional<Question> findFirstByStatusIsNullOrderBySortOrderAsc();

    Optional<Question> findFirstByStatusIsNullAndIdNotOrderBySortOrderAsc(Long id);

    Optional<Question> findFirstByStatusOrderBySortOrderAsc(QuestionStatus status);

    Optional<Question> findFirstByStatusAndIdNotOrderBySortOrderAsc(QuestionStatus status, Long id);

    long countByStatus(QuestionStatus status);

    long countByStatusIsNull();
}
