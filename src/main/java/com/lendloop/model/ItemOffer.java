package com.lendloop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_offers")
public class ItemOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wanted_item_id", nullable = false)
    @NotNull(message = "Wanted request reference is mandatory")
    private WantedItem wantedItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offered_by_id", nullable = false)
    @NotNull(message = "Offering student reference is mandatory")
    private Student offeredBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offered_item_id", nullable = false)
    @NotNull(message = "You must select an item from your listings")
    private Item offeredItem;

    @Size(max = 1000, message = "Offer message must not exceed 1000 characters")
    @Column(length = 1000)
    private String message;

    @NotNull(message = "Available start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "Available end date is mandatory")
    private LocalDate endDate;

    private String status = "PENDING";

    private LocalDateTime createdAt = LocalDateTime.now();

    public ItemOffer() {
    }

    public Long getId() {
        return id;
    }

    public WantedItem getWantedItem() {
        return wantedItem;
    }

    public void setWantedItem(WantedItem wantedItem) {
        this.wantedItem = wantedItem;
    }

    public Student getOfferedBy() {
        return offeredBy;
    }

    public void setOfferedBy(Student offeredBy) {
        this.offeredBy = offeredBy;
    }

    public Item getOfferedItem() {
        return offeredItem;
    }

    public void setOfferedItem(Item offeredItem) {
        this.offeredItem = offeredItem;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
