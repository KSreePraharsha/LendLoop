package com.lendloop.repository;

import com.lendloop.model.Item;
import com.lendloop.model.Student;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwner(Student owner);

    @Query("SELECT i FROM Item i JOIN FETCH i.owner WHERE i.available = true")
    List<Item> findByAvailableTrue();

    List<Item> findByCategoryIgnoreCase(String category);

    List<Item> findByTitleContainingIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}
