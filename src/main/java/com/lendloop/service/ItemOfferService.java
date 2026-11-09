package com.lendloop.service;

import com.lendloop.model.*;
import com.lendloop.repository.ItemOfferRepository;
import com.lendloop.repository.ItemRepository;
import com.lendloop.repository.WantedItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ItemOfferService {

    private final ItemOfferRepository itemOfferRepository;
    private final WantedItemRepository wantedItemRepository;
    private final ItemRepository itemRepository;
    private final BorrowRequestService borrowRequestService;
    private final BorrowingService borrowingService;

    public ItemOfferService(
            ItemOfferRepository itemOfferRepository,
            WantedItemRepository wantedItemRepository,
            ItemRepository itemRepository,
            BorrowRequestService borrowRequestService,
            BorrowingService borrowingService) {

        this.itemOfferRepository = itemOfferRepository;
        this.wantedItemRepository = wantedItemRepository;
        this.itemRepository = itemRepository;
        this.borrowRequestService = borrowRequestService;
        this.borrowingService = borrowingService;
    }

    // Submit an offer on a wanted item
    @Transactional
    public ItemOffer createOffer(
            Long wantedItemId,
            Long itemId,
            String message,
            LocalDate startDate,
            LocalDate endDate,
            Student offeringStudent) {

        WantedItem wantedItem = wantedItemRepository.findById(wantedItemId)
                .orElseThrow(() -> new IllegalArgumentException("Wanted request not found!"));

        if (!"OPEN".equals(wantedItem.getStatus())) {
            throw new IllegalArgumentException("This request is no longer open for offers!");
        }

        // Prevent self-offers
        if (wantedItem.getRequester().getId().equals(offeringStudent.getId())) {
            throw new IllegalArgumentException("You cannot make an offer on your own request!");
        }

        // Mandatory offered item check
        if (itemId == null) {
            throw new IllegalArgumentException("You must select an item from your listings!");
        }

        Item offeredItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Selected item not found!"));

        // Ownership verification
        if (!offeredItem.getOwner().getId().equals(offeringStudent.getId())) {
            throw new IllegalArgumentException("You can only offer items that belong to your listings!");
        }

        // Availability check
        if (!Boolean.TRUE.equals(offeredItem.getAvailable())) {
            throw new IllegalArgumentException("This item is currently marked as unavailable!");
        }

        // Date validity
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required!");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past!");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date!");
        }

        // Duplicate active offer check
        boolean hasDuplicate = itemOfferRepository.existsByWantedItemAndOfferedByAndStatusIn(
                wantedItem,
                offeringStudent,
                List.of("PENDING", "ACCEPTED")
        );
        if (hasDuplicate) {
            throw new IllegalArgumentException("You already have an active offer for this wanted request!");
        }

        // Upfront date-overlap check
        boolean overlapping = borrowingService.hasOverlappingBorrowing(offeredItem, startDate, endDate);
        if (overlapping) {
            throw new IllegalArgumentException("This item is already booked for borrowing during these dates!");
        }

        ItemOffer offer = new ItemOffer();
        offer.setWantedItem(wantedItem);
        offer.setOfferedBy(offeringStudent);
        offer.setOfferedItem(offeredItem);
        offer.setMessage(message);
        offer.setStartDate(startDate);
        offer.setEndDate(endDate);
        offer.setStatus("PENDING");

        return itemOfferRepository.save(offer);
    }

    // Get offers for a specific wanted request (only requester can view)
    public List<ItemOffer> getOffersForWantedItem(Long wantedItemId, Student requester) {
        WantedItem wantedItem = wantedItemRepository.findById(wantedItemId)
                .orElseThrow(() -> new IllegalArgumentException("Wanted request not found!"));

        if (!wantedItem.getRequester().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You are not authorized to view offers for this request!");
        }

        return itemOfferRepository.findByWantedItemOrderByCreatedAtDesc(wantedItem);
    }

    // Get offers submitted by the logged-in student
    public List<ItemOffer> getMyOffers(Student offeringStudent) {
        return itemOfferRepository.findByOfferedByOrderByCreatedAtDesc(offeringStudent);
    }

    // Offerer withdraws their pending offer
    @Transactional
    public void withdrawOffer(Long offerId, Student offeringStudent) {
        ItemOffer offer = itemOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found!"));

        if (!offer.getOfferedBy().getId().equals(offeringStudent.getId())) {
            throw new IllegalArgumentException("You are not authorized to withdraw this offer!");
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new IllegalArgumentException("Only pending offers can be withdrawn!");
        }

        offer.setStatus("WITHDRAWN");
        itemOfferRepository.save(offer);
    }

    // Requester rejects an offer
    @Transactional
    public void rejectOffer(Long offerId, Student requester) {
        ItemOffer offer = itemOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found!"));

        WantedItem wantedItem = offer.getWantedItem();

        if (!wantedItem.getRequester().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You are not authorized to reject this offer!");
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new IllegalArgumentException("Only pending offers can be rejected!");
        }

        offer.setStatus("REJECTED");
        itemOfferRepository.save(offer);
    }

    // Requester accepts an offer - strictly protected with PESSIMISTIC_WRITE lock and BorrowRequest bridge
    @Transactional
    public void acceptOffer(Long offerId, Student requester) {

        // 1. Fetch offer to identify the WantedItem
        ItemOffer offer = itemOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found!"));

        // 2. Acquire PESSIMISTIC_WRITE lock on WantedItem row
        WantedItem wantedItem = wantedItemRepository.findByIdForUpdate(offer.getWantedItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("Wanted request not found!"));

        // 3. Ownership and atomic status checks under lock
        if (!wantedItem.getRequester().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You are not authorized to accept this offer!");
        }

        if (!"OPEN".equals(wantedItem.getStatus())) {
            throw new IllegalStateException("This wanted request is no longer open!");
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new IllegalStateException("This offer is no longer pending!");
        }

        // 4. Revalidate offered item ownership & availability
        Item offeredItem = offer.getOfferedItem();
        if (offeredItem == null || !offeredItem.getOwner().getId().equals(offer.getOfferedBy().getId())) {
            throw new IllegalArgumentException("The offered item is invalid or ownership has changed!");
        }

        if (!Boolean.TRUE.equals(offeredItem.getAvailable())) {
            throw new IllegalArgumentException("The offered item is currently not available!");
        }

        // 5. Construct a genuine BorrowRequest
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setItem(offeredItem);
        borrowRequest.setBorrower(requester);
        borrowRequest.setStartDate(offer.getStartDate());
        borrowRequest.setEndDate(offer.getEndDate());

        BorrowRequest savedRequest = borrowRequestService.createRequest(borrowRequest);

        // 6. Connect to the existing approval pipeline using the genuine saved BorrowRequest ID.
        // Inside approveRequest: acquires Item lock, rechecks availability & overlap, creates Borrowing (ACTIVE).
        borrowRequestService.approveRequest(savedRequest.getId());

        // 7. Update Offer and WantedItem status
        offer.setStatus("ACCEPTED");
        itemOfferRepository.save(offer);

        wantedItem.setStatus("FULFILLED");
        wantedItemRepository.save(wantedItem);

        // 8. Safely cancel all other pending offers for this request
        List<ItemOffer> pendingOffers = itemOfferRepository.findByWantedItemAndStatus(wantedItem, "PENDING");
        for (ItemOffer other : pendingOffers) {
            if (!other.getId().equals(offer.getId())) {
                other.setStatus("CANCELLED");
                itemOfferRepository.save(other);
            }
        }
    }
}
