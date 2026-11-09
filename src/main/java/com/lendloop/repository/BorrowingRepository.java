package com.lendloop.repository;

import com.lendloop.model.Borrowing;
import com.lendloop.model.Student;
import com.lendloop.model.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    List<Borrowing> findByBorrower(Student borrower);

    List<Borrowing> findByOwner(Student owner);

    List<Borrowing> findByItem(Item item);

    List<Borrowing> findByStatus(String status);

    List<Borrowing> findByItemAndStatus(Item item, String status);

    @Query("""
        SELECT COUNT(b) > 0 FROM Borrowing b
        WHERE b.item = :item
        AND b.status IN ('ACTIVE', 'RETURN_REQUESTED')
        AND b.startDate <= :requestedEnd
        AND b.dueDate >= :requestedStart
    """)
    boolean hasOverlappingBorrowing(
            @Param("item") Item item,
            @Param("requestedStart") LocalDate requestedStart,
            @Param("requestedEnd") LocalDate requestedEnd
    );
}