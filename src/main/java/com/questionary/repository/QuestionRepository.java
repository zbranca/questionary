package com.questionary.repository;

import com.questionary.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderBySortOrderAsc();

    Optional<Question> findFirstByStatusIsNullOrderBySortOrderAsc();

    Optional<Question> findFirstByStatusIsNullAndIdNotOrderBySortOrderAsc(Long id);

    Optional<Question> findFirstByStatusOrderBySortOrderAsc(String status);

    Optional<Question> findFirstByStatusAndIdNotOrderBySortOrderAsc(String status, Long id);

    long countByStatus(String status);

    long countByStatusIsNull();
}
