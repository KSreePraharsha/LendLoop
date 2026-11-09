package com.lendloop.repository;

import com.lendloop.model.BorrowRequest;
import com.lendloop.model.Student;
import com.lendloop.model.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BorrowRequestRepository
        extends JpaRepository<BorrowRequest, Long> {

    // Find requests sent by a particular student
    List<BorrowRequest> findByBorrower(Student borrower);

    // Find requests received for items owned by a particular student
    List<BorrowRequest> findByItem_Owner(Student owner);

    // Find requests with a particular status
    List<BorrowRequest> findByStatus(String status);

    // Find requests for a particular item
    List<BorrowRequest> findByItem(Item item);
}