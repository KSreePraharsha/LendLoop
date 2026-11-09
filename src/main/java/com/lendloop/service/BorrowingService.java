
package com.lendloop.service;

import com.lendloop.model.Borrowing;
import com.lendloop.model.Student;
import com.lendloop.model.Item;
import com.lendloop.repository.BorrowingRepository;
import com.lendloop.repository.ItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final ItemRepository itemRepository;

    public BorrowingService(
            BorrowingRepository borrowingRepository,
            ItemRepository itemRepository) {
        this.borrowingRepository = borrowingRepository;
        this.itemRepository = itemRepository;
    }

    // Create a new borrowing record
    @Transactional
    public Borrowing createBorrowing(Borrowing borrowing) {
        borrowing.setStatus("ACTIVE");
        Item item = borrowing.getItem();
        if (item != null) {
            item.setAvailable(false);
            itemRepository.save(item);
        }
        return borrowingRepository.save(borrowing);
    }

    // Get items borrowed by a student
    public List<Borrowing> getMyBorrowings(Student borrower) {
        return borrowingRepository.findByBorrower(borrower);
    }

    // Get items lent by a student
    public List<Borrowing> getMyLending(Student owner) {
        return borrowingRepository.findByOwner(owner);
    }

    // Get all borrowing records
    public List<Borrowing> getAllBorrowings() {
        return borrowingRepository.findAll();
    }

    // Get borrowing records by status
    public List<Borrowing> getBorrowingsByStatus(String status) {
        return borrowingRepository.findByStatus(status);
    }

    // Get active borrowings for an item
    public List<Borrowing> getActiveBorrowings(Item item) {
        return borrowingRepository.findByItemAndStatus(item, "ACTIVE");
    }

    // Check whether requested dates overlap an existing borrowing
    public boolean hasOverlappingBorrowing(
            Item item,
            LocalDate startDate,
            LocalDate endDate) {

        return borrowingRepository.hasOverlappingBorrowing(
                item, startDate, endDate);
    }

    // Borrower requests to return an item
    @Transactional
    public void requestReturn(Long borrowingId, Student borrower) {

        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Borrowing record not found!"));

        if (!borrowing.getBorrower().getId().equals(borrower.getId())) {
            throw new IllegalArgumentException(
                    "You are not authorized to return this item!");
        }

        if (!"ACTIVE".equals(borrowing.getStatus())) {
            throw new IllegalArgumentException(
                    "This borrowing cannot be returned at this stage!");
        }

        borrowing.requestReturn();
        borrowingRepository.save(borrowing);
    }

    // Owner confirms that the item has been returned
    @Transactional
    public void confirmReturn(Long borrowingId, Student owner) {

        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Borrowing record not found!"));

        if (!borrowing.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "You are not authorized to confirm this return!");
        }

        if (!"RETURN_REQUESTED".equals(borrowing.getStatus())) {
            throw new IllegalArgumentException(
                    "No return request is pending!");
        }

        borrowing.confirmReturn();
        Item item = borrowing.getItem();
        if (item != null) {
            item.setAvailable(true);
            itemRepository.save(item);
        }
        borrowingRepository.save(borrowing);
    }
}
