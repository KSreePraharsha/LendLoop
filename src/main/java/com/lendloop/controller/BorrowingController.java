package com.lendloop.controller;

import com.lendloop.model.Borrowing;
import com.lendloop.model.Student;
import com.lendloop.service.BorrowingService;
import com.lendloop.service.StudentService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BorrowingController {

    private final BorrowingService borrowingService;
    private final StudentService studentService;

    public BorrowingController(
            BorrowingService borrowingService,
            StudentService studentService) {

        this.borrowingService = borrowingService;
        this.studentService = studentService;
    }

    // View items borrowed by the logged-in student
    @GetMapping("/my-borrowings")
    public String myBorrowings(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student borrower;
        try {
            borrower = studentService.findStudentById(studentId);
        } catch (IllegalArgumentException e) {
            session.invalidate();
            return "redirect:/login";
        }

        List<Borrowing> borrowings = borrowingService.getMyBorrowings(borrower);
        model.addAttribute("borrowings", borrowings);
        model.addAttribute("requests", borrowings);

        return "my-borrowings";
    }

    // View items lent by the logged-in student
    @GetMapping("/my-lending")
    public String myLending(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student owner;
        try {
            owner = studentService.findStudentById(studentId);
        } catch (IllegalArgumentException e) {
            session.invalidate();
            return "redirect:/login";
        }

        List<Borrowing> borrowings = borrowingService.getMyLending(owner);
        model.addAttribute("borrowings", borrowings);
        model.addAttribute("requests", borrowings);

        return "my-lending";
    }

    // Borrower requests to return an item
    @PostMapping("/borrowings/{borrowingId}/request-return")
    public String requestReturn(
            @PathVariable Long borrowingId,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student borrower;
        try {
            borrower = studentService.findStudentById(studentId);
        } catch (IllegalArgumentException e) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            borrowingService.requestReturn(borrowingId, borrower);
        } catch (IllegalArgumentException exception) {
            return "redirect:/my-borrowings?error";
        }

        return "redirect:/my-borrowings?returnRequested";
    }

    // Owner confirms the returned item
    @PostMapping("/borrowings/{borrowingId}/confirm-return")
    public String confirmReturn(
            @PathVariable Long borrowingId,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student owner;
        try {
            owner = studentService.findStudentById(studentId);
        } catch (IllegalArgumentException e) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            borrowingService.confirmReturn(borrowingId, owner);
        } catch (IllegalArgumentException exception) {
            return "redirect:/my-lending?error";
        }

        return "redirect:/my-lending?returnConfirmed";
    }
}
