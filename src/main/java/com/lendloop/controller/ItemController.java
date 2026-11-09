
package com.lendloop.controller;

import com.lendloop.model.Item;
import com.lendloop.model.Student;
import com.lendloop.repository.StudentRepository;
import com.lendloop.service.ItemService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class ItemController {

    private final ItemService itemService;
    private final StudentRepository studentRepository;

    public ItemController(
            ItemService itemService,
            StudentRepository studentRepository) {

        this.itemService = itemService;
        this.studentRepository = studentRepository;
    }

    @ModelAttribute("today")
    public LocalDate getToday() {
        return LocalDate.now();
    }

    // Display Add Item page
    @GetMapping("/items/add")
    public String showAddItemPage(
            HttpSession session,
            Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        model.addAttribute("item", new Item());

        return "add-item";
    }

    // Save a new item
    @PostMapping("/items/add")
    public String addItem(
            @Valid @ModelAttribute("item") Item item,
            BindingResult result,
            HttpSession session,
            Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "add-item";
        }

        Student owner = studentRepository.findById(studentId)
                .orElse(null);

        if (owner == null) {
            session.invalidate();
            return "redirect:/login";
        }

        itemService.addItem(item, owner);

        return "redirect:/my-listings";
    }

    // Display all available items
    @GetMapping("/items")
    public String browseItems(
            HttpSession session,
            Model model) {

        if (session.getAttribute("studentId") == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "items", itemService.getAvailableItems());

        return "browse-items";
    }

    // Display items listed by logged-in student
    @GetMapping("/my-listings")
    public String myListings(
            HttpSession session,
            Model model) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return "redirect:/login";
        }

        Student owner = studentRepository.findById(studentId)
                .orElse(null);

        if (owner == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute(
                "items", itemService.getMyItems(owner));

        return "my-listings";
    }

    // Search items by name
    @GetMapping("/items/search")
    public String searchItems(
            @RequestParam("keyword") String keyword,
            HttpSession session,
            Model model) {

        if (session.getAttribute("studentId") == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "items", itemService.searchItems(keyword));

        model.addAttribute("keyword", keyword);

        return "browse-items";
    }
}
