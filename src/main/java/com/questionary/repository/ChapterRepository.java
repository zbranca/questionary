package com.questionary.repository;

import com.questionary.entity.AppUser;
import com.questionary.entity.Chapter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    @Query("SELECT c FROM Chapter c WHERE c.user = :user ORDER BY c.sortOrder ASC")
    List<Chapter> findAllByUserOrderBySortOrderAsc(@Param("user") AppUser user);

    @Query("SELECT c FROM Chapter c WHERE c.name = :name AND c.user = :user")
    Optional<Chapter> findByNameAndUser(@Param("name") String name, @Param("user") AppUser user);

    @Query("SELECT COUNT(c) FROM Chapter c WHERE c.user = :user")
    long countByUser(@Param("user") AppUser user);

    @Modifying
    @Query("DELETE FROM Chapter c WHERE c.user = :user")
    void deleteAllByUser(@Param("user") AppUser user);
}
