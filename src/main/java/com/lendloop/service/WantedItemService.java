package com.lendloop.service;

import com.lendloop.model.ItemOffer;
import com.lendloop.model.Student;
import com.lendloop.model.WantedItem;
import com.lendloop.repository.ItemOfferRepository;
import com.lendloop.repository.WantedItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WantedItemService {

    private final WantedItemRepository wantedItemRepository;
    private final ItemOfferRepository itemOfferRepository;

    public WantedItemService(
            WantedItemRepository wantedItemRepository,
            ItemOfferRepository itemOfferRepository) {
        this.wantedItemRepository = wantedItemRepository;
        this.itemOfferRepository = itemOfferRepository;
    }

    // Post a new wanted item request
    @Transactional
    public WantedItem createWantedItem(WantedItem wantedItem, Student requester) {

        if (wantedItem.getStartDate() == null || wantedItem.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required!");
        }

        if (wantedItem.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Required start date cannot be in the past!");
        }

        if (wantedItem.getEndDate().isBefore(wantedItem.getStartDate())) {
            throw new IllegalArgumentException("Required end date cannot be earlier than start date!");
        }

        wantedItem.setRequester(requester);
        wantedItem.setStatus("OPEN");
        return wantedItemRepository.save(wantedItem);
    }

    // Get open wanted items for browsing
    public List<WantedItem> getOpenWantedItems(Student currentStudent) {
        if (currentStudent != null) {
            return wantedItemRepository.findByStatusAndRequesterNotOrderByCreatedAtDesc("OPEN", currentStudent);
        }
        return wantedItemRepository.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    // Get all requests posted by a specific student
    public List<WantedItem> getMyWantedItems(Student requester) {
        return wantedItemRepository.findByRequesterOrderByCreatedAtDesc(requester);
    }

    // Find wanted item by ID
    public WantedItem getWantedItemById(Long id) {
        return wantedItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wanted item request not found!"));
    }

    // Requester cancels a wanted request
    @Transactional
    public void cancelWantedItem(Long id, Student requester) {

        // Acquire lock on the WantedItem
        WantedItem wantedItem = wantedItemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Wanted item request not found!"));

        if (!wantedItem.getRequester().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You are not authorized to cancel this request!");
        }

        if (!"OPEN".equals(wantedItem.getStatus())) {
            throw new IllegalArgumentException("This request is no longer open and cannot be cancelled!");
        }

        wantedItem.setStatus("CANCELLED");
        wantedItemRepository.save(wantedItem);

        // Safely cancel any pending offers without deleting history
        List<ItemOffer> pendingOffers = itemOfferRepository.findByWantedItemAndStatus(wantedItem, "PENDING");
        for (ItemOffer offer : pendingOffers) {
            offer.setStatus("CANCELLED");
            itemOfferRepository.save(offer);
        }
    }
}
