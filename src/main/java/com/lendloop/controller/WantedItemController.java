package com.lendloop.controller;

import com.lendloop.model.ItemOffer;
import com.lendloop.model.Student;
import com.lendloop.model.WantedItem;
import com.lendloop.service.ItemOfferService;
import com.lendloop.service.StudentService;
import com.lendloop.service.WantedItemService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WantedItemController {

    private final WantedItemService wantedItemService;
    private final ItemOfferService itemOfferService;
    private final StudentService studentService;

    public WantedItemController(
            WantedItemService wantedItemService,
            ItemOfferService itemOfferService,
            StudentService studentService) {

        this.wantedItemService = wantedItemService;
        this.itemOfferService = itemOfferService;
        this.studentService = studentService;
    }

    // Browse all open wanted items posted by peers
    @GetMapping("/wanted-items")
    public String browseWantedItems(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);
        List<WantedItem> wantedItems = wantedItemService.getOpenWantedItems(student);

        model.addAttribute("wantedItems", wantedItems);
        model.addAttribute("currentStudentId", studentId);

        return "wanted-items";
    }

    // Show form to post a wanted item
    @GetMapping("/wanted-items/post")
    public String showPostForm(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("wantedItem")) {
            model.addAttribute("wantedItem", new WantedItem());
        }

        return "post-wanted-item";
    }

    // Submit a wanted item
    @PostMapping("/wanted-items/post")
    public String submitWantedItem(
            @ModelAttribute WantedItem wantedItem,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student requester = studentService.findStudentById(studentId);

        try {
            wantedItemService.createWantedItem(wantedItem, requester);
            return "redirect:/my-wanted-items?posted";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("wantedItem", wantedItem);
            return "redirect:/wanted-items/post";
        }
    }

    // Requester views their own wanted items and incoming offers
    @GetMapping("/my-wanted-items")
    public String myWantedItems(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);
        List<WantedItem> myRequests = wantedItemService.getMyWantedItems(student);

        // Fetch offers map for each request
        Map<Long, List<ItemOffer>> offersMap = new HashMap<>();
        for (WantedItem item : myRequests) {
            offersMap.put(item.getId(), itemOfferService.getOffersForWantedItem(item.getId(), student));
        }

        model.addAttribute("myRequests", myRequests);
        model.addAttribute("offersMap", offersMap);

        return "my-wanted-items";
    }

    // Requester cancels a wanted request
    @PostMapping("/wanted-items/{id}/cancel")
    public String cancelWantedItem(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);

        try {
            wantedItemService.cancelWantedItem(id, student);
            return "redirect:/my-wanted-items?cancelled";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my-wanted-items?error";
        }
    }
}
