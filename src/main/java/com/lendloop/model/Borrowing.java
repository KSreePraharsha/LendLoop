
package com.lendloop.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrowings")
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "borrower_id", nullable = false)
    private Student borrower;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Student owner;

    private LocalDate startDate;

    private LocalDate dueDate;

    private LocalDate actualReturnDate;

    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor
    public Borrowing() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Student getBorrower() {
        return borrower;
    }

    public void setBorrower(Student borrower) {
        this.borrower = borrower;
    }

    public Student getOwner() {
        return owner;
    }

    public void setOwner(Student owner) {
        this.owner = owner;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDate actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
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

    // Borrower requests to return the item
    public void requestReturn() {
        this.status = "RETURN_REQUESTED";
    }

    // Owner confirms that the item has been returned
    public void confirmReturn() {
        this.status = "RETURNED";
        this.actualReturnDate = LocalDate.now();
    }
}
