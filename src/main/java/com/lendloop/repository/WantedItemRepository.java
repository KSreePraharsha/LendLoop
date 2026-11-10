package com.lendloop.repository;

import com.lendloop.model.Student;
import com.lendloop.model.WantedItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WantedItemRepository extends JpaRepository<WantedItem, Long> {

    @Query(value = "SELECT * FROM wanted_items WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<WantedItem> findByIdForUpdate(@Param("id") Long id);

    List<WantedItem> findByStatusOrderByCreatedAtDesc(String status);

    List<WantedItem> findByRequesterOrderByCreatedAtDesc(Student requester);

    List<WantedItem> findByStatusAndRequesterNotOrderByCreatedAtDesc(String status, Student requester);

    List<WantedItem> findByStatusAndCategoryIgnoreCaseOrderByCreatedAtDesc(String status, String category);

    List<WantedItem> findByStatusAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(String status, String title);
}
