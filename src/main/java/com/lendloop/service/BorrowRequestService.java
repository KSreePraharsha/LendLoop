package com.lendloop.service;

import com.lendloop.model.BorrowRequest;
import com.lendloop.model.Borrowing;
import com.lendloop.model.Item;
import com.lendloop.model.Student;
import com.lendloop.repository.BorrowRequestRepository;
import com.lendloop.repository.ItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BorrowRequestService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowingService borrowingService;
    private final ItemRepository itemRepository;

    public BorrowRequestService(
            BorrowRequestRepository borrowRequestRepository,
            BorrowingService borrowingService,
            ItemRepository itemRepository) {

        this.borrowRequestRepository = borrowRequestRepository;
        this.borrowingService = borrowingService;
        this.itemRepository = itemRepository;
    }

    public BorrowRequest createRequest(BorrowRequest request) {
        request.setStatus("PENDING");
        return borrowRequestRepository.save(request);
    }

    public List<BorrowRequest> getMyRequests(Student borrower) {
        return borrowRequestRepository.findByBorrower(borrower);
    }

    public List<BorrowRequest> getReceivedRequests(Student owner) {
        return borrowRequestRepository.findByItem_Owner(owner);
    }

    @Transactional
    public void approveRequest(Long requestId) {

        BorrowRequest request = borrowRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "This request has already been processed!");
        }

        // 1. Acquire item-level PESSIMISTIC_WRITE lock
        Item item = itemRepository.findByIdForUpdate(request.getItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found!"));

        // 2. Recheck item availability while holding lock
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("This item is currently not available for borrowing!");
        }

        // 3. Recheck overlapping borrowing while holding lock
        boolean overlapping = borrowingService.hasOverlappingBorrowing(
                item,
                request.getStartDate(),
                request.getEndDate()
        );

        if (overlapping) {
            throw new IllegalArgumentException(
                    "This item is already borrowed during the requested dates!");
        }

        Borrowing borrowing = new Borrowing();

        borrowing.setItem(item);
        borrowing.setBorrower(request.getBorrower());
        borrowing.setOwner(item.getOwner());
        borrowing.setStartDate(request.getStartDate());
        borrowing.setDueDate(request.getEndDate());

        borrowingService.createBorrowing(borrowing);

        request.setStatus("APPROVED");
        borrowRequestRepository.save(request);
    }

    public void rejectRequest(Long requestId) {

        BorrowRequest request = borrowRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "This request has already been processed!");
        }

        request.setStatus("REJECTED");
        borrowRequestRepository.save(request);
    }
}
