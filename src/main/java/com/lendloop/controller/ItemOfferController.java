package com.lendloop.controller;

import com.lendloop.model.Item;
import com.lendloop.model.ItemOffer;
import com.lendloop.model.Student;
import com.lendloop.model.WantedItem;
import com.lendloop.repository.ItemRepository;
import com.lendloop.service.ItemOfferService;
import com.lendloop.service.StudentService;
import com.lendloop.service.WantedItemService;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ItemOfferController {

    private final ItemOfferService itemOfferService;
    private final WantedItemService wantedItemService;
    private final ItemRepository itemRepository;
    private final StudentService studentService;

    public ItemOfferController(
            ItemOfferService itemOfferService,
            WantedItemService wantedItemService,
            ItemRepository itemRepository,
            StudentService studentService) {

        this.itemOfferService = itemOfferService;
        this.wantedItemService = wantedItemService;
        this.itemRepository = itemRepository;
        this.studentService = studentService;
    }

    // Show form to make an offer on a wanted item
    @GetMapping("/wanted-items/{wantedItemId}/offer")
    public String showOfferForm(
            @PathVariable Long wantedItemId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);
        WantedItem wantedItem = wantedItemService.getWantedItemById(wantedItemId);

        // Prevent self-offers
        if (wantedItem.getRequester().getId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot make an offer on your own request!");
            return "redirect:/wanted-items";
        }

        if (!"OPEN".equals(wantedItem.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This request is no longer open for offers!");
            return "redirect:/wanted-items";
        }

        // Fetch student's listed items
        List<Item> myListings = itemRepository.findByOwner(student);

        model.addAttribute("wantedItem", wantedItem);
        model.addAttribute("myListings", myListings);

        return "make-offer";
    }

    // Submit an offer
    @PostMapping("/wanted-items/{wantedItemId}/offer")
    public String submitOffer(
            @PathVariable Long wantedItemId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String message,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);

        try {
            itemOfferService.createOffer(wantedItemId, itemId, message, startDate, endDate, student);
            return "redirect:/my-offers?offered";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/wanted-items/" + wantedItemId + "/offer";
        }
    }

    // View all offers submitted by the logged-in student
    @GetMapping("/my-offers")
    public String myOffers(HttpSession session, Model model) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);
        List<ItemOffer> offers = itemOfferService.getMyOffers(student);

        model.addAttribute("offers", offers);

        return "my-offers";
    }

    // Offering student withdraws their pending offer
    @PostMapping("/offers/{id}/withdraw")
    public String withdrawOffer(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);

        try {
            itemOfferService.withdrawOffer(id, student);
            return "redirect:/my-offers?withdrawn";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my-offers?error";
        }
    }

    // Requester accepts an offer (bridges into Borrowing workflow under PESSIMISTIC_WRITE lock)
    @PostMapping("/offers/{id}/accept")
    public String acceptOffer(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);

        try {
            itemOfferService.acceptOffer(id, student);
            return "redirect:/my-wanted-items?accepted";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my-wanted-items?error=conflict";
        }
    }

    // Requester rejects an offer
    @PostMapping("/offers/{id}/reject")
    public String rejectOffer(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentService.findStudentById(studentId);

        try {
            itemOfferService.rejectOffer(id, student);
            return "redirect:/my-wanted-items?rejected";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my-wanted-items?error";
        }
    }
}
