
package com.lendloop.controller;

import com.lendloop.model.Student;
import com.lendloop.service.StudentService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Display registration page
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("student", new Student());
        return "register";
    }

    // Process student registration
    @PostMapping("/register")
    public String registerStudent(
            @Valid @ModelAttribute("student") Student student,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            studentService.registerStudent(student);
            return "redirect:/login?registered";

        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "register";
        }
    }

    // Display login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Display student dashboard
    @GetMapping("/dashboard")
    public String showDashboard(
            HttpSession session,
            Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        // Prevent access if student is not logged in
        if (studentId == null) {
            return "redirect:/login";
        }

        String studentName =
                (String) session.getAttribute("studentName");

        model.addAttribute("studentName", studentName);

        return "dashboard";
    }

    // Display student profile
    @GetMapping("/profile")
    public String showProfile(
            HttpSession session,
            Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        // Prevent access if student is not logged in
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            Student student = studentService.findStudentById(studentId);
            model.addAttribute("student", student);
            return "profile";

        } catch (IllegalArgumentException exception) {
            session.invalidate();
            return "redirect:/login";
        }
    }
}
