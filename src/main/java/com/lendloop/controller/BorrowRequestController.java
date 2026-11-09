package com.lendloop.controller;

import com.lendloop.model.BorrowRequest;
import com.lendloop.model.Item;
import com.lendloop.model.Student;
import com.lendloop.repository.BorrowRequestRepository;
import com.lendloop.repository.ItemRepository;
import com.lendloop.repository.StudentRepository;
import com.lendloop.service.BorrowRequestService;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ItemRepository itemRepository;
    private final StudentRepository studentRepository;

    public BorrowRequestController(
            BorrowRequestService borrowRequestService,
            BorrowRequestRepository borrowRequestRepository,
            ItemRepository itemRepository,
            StudentRepository studentRepository) {

        this.borrowRequestService = borrowRequestService;
        this.borrowRequestRepository = borrowRequestRepository;
        this.itemRepository = itemRepository;
        this.studentRepository = studentRepository;
    }

    // Send a borrowing request
    @PostMapping("/borrow/{itemId}")
    public String sendRequest(
            @PathVariable Long itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student borrower = studentRepository.findById(studentId).orElse(null);
        Item item = itemRepository.findById(itemId).orElse(null);

        if (borrower == null || item == null) {
            return "redirect:/items";
        }

        // Students cannot borrow their own items
        if (item.getOwner().getId().equals(studentId)) {
            return "redirect:/items?error=ownitem";
        }

        // Check item availability
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            return "redirect:/items?error=unavailable";
        }

        // Validate borrowing dates
        if (startDate == null || endDate == null
                || startDate.isBefore(LocalDate.now())
                || !endDate.isAfter(startDate)) {
            return "redirect:/items?error=invaliddates";
        }

        if (endDate.isAfter(
                startDate.plusDays(item.getMaxBorrowDays()))) {
            return "redirect:/items?error=duration";
        }

        BorrowRequest request = new BorrowRequest();
        request.setItem(item);
        request.setBorrower(borrower);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        borrowRequestService.createRequest(request);

        return "redirect:/my-borrow-requests";
    }

    // View requests sent by the logged-in student
    @GetMapping("/my-borrow-requests")
    public String myRequests(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student borrower = studentRepository.findById(studentId).orElse(null);

        if (borrower == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute(
                "requests",
                borrowRequestService.getMyRequests(borrower));

        return "my-borrow-requests";
    }

    // View requests received by the logged-in owner's items
    @GetMapping("/received-requests")
    public String receivedRequests(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student owner = studentRepository.findById(studentId).orElse(null);

        if (owner == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute(
                "requests",
                borrowRequestService.getReceivedRequests(owner));

        return "received-requests";
    }

    // Approve a request
    @PostMapping("/borrow-requests/{requestId}/approve")
    public String approveRequest(
            @PathVariable Long requestId,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        BorrowRequest request = borrowRequestRepository
                .findById(requestId).orElse(null);

        if (request == null
                || !request.getItem().getOwner().getId().equals(studentId)
                || !"PENDING".equals(request.getStatus())) {
            return "redirect:/received-requests";
        }

        try {
            borrowRequestService.approveRequest(requestId);
            return "redirect:/received-requests?approved";
        } catch (IllegalArgumentException exception) {
            return "redirect:/received-requests?error=conflict";
        }
    }

    // Reject a request
    @PostMapping("/borrow-requests/{requestId}/reject")
    public String rejectRequest(
            @PathVariable Long requestId,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        BorrowRequest request = borrowRequestRepository
                .findById(requestId).orElse(null);

        if (request == null
                || !request.getItem().getOwner().getId().equals(studentId)
                || !"PENDING".equals(request.getStatus())) {
            return "redirect:/received-requests";
        }

        borrowRequestService.rejectRequest(requestId);

        return "redirect:/received-requests";
    }
}