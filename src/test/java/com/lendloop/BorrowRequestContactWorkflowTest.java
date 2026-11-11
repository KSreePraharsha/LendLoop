package com.lendloop;

import com.lendloop.controller.BorrowRequestController;
import com.lendloop.model.BorrowRequest;
import com.lendloop.model.Item;
import com.lendloop.model.Student;
import com.lendloop.repository.BorrowRequestRepository;
import com.lendloop.repository.ItemRepository;
import com.lendloop.repository.StudentRepository;
import com.lendloop.service.BorrowRequestService;
import com.lendloop.service.BorrowingService;
import com.lendloop.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowRequestContactWorkflowTest {

    @Mock
    private BorrowRequestRepository borrowRequestRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BorrowingService borrowingService;

    @Mock
    private HttpSession session;

    private BorrowRequestService borrowRequestService;
    private StudentService studentService;
    private BorrowRequestController borrowRequestController;

    private Student owner;
    private Student borrower;
    private Student unrelatedStudent;
    private Item item;

    @BeforeEach
    void setUp() {
        borrowRequestService = new BorrowRequestService(borrowRequestRepository, borrowingService, itemRepository);
        studentService = new StudentService(studentRepository, null);
        borrowRequestController = new BorrowRequestController(borrowRequestService, borrowRequestRepository, itemRepository, studentRepository);

        // Setup Owner
        owner = new Student();
        owner.setFullName("Alice Lender");
        owner.setEmail("alice@college.edu");
        owner.setPhoneNumber("+91 9876543210");
        owner.setPreferredContactMethod("PHONE");
        owner.setDepartment("Mechanical Engineering");

        // Setup Borrower
        borrower = new Student();
        borrower.setFullName("Bob Borrower");
        borrower.setEmail("bob@college.edu");
        borrower.setPhoneNumber("+91 9123456780");
        borrower.setPreferredContactMethod("EMAIL");
        borrower.setDepartment("Computer Science");

        // Setup Unrelated Student
        unrelatedStudent = new Student();
        unrelatedStudent.setFullName("Charlie Stranger");
        unrelatedStudent.setEmail("charlie@college.edu");

        // Setup Item
        item = new Item();
        item.setTitle("Casio FX-991EX Calculator");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setMaxBorrowDays(14);
    }

    @Test
    @DisplayName("Verify BorrowRequest entity fields store contact info correctly")
    void testBorrowRequestContactFields() {
        BorrowRequest request = new BorrowRequest();
        request.setItem(item);
        request.setBorrower(borrower);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setRequesterPhone("+91 9123456780");
        request.setRequesterEmail("bob@college.edu");
        request.setPreferredContactMethod("PHONE");

        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(i -> i.getArgument(0));

        BorrowRequest saved = borrowRequestService.createRequest(request);

        assertNotNull(saved);
        assertEquals("PENDING", saved.getStatus());
        assertEquals("+91 9123456780", saved.getRequesterPhone());
        assertEquals("bob@college.edu", saved.getRequesterEmail());
        assertEquals("PHONE", saved.getPreferredContactMethod());
    }

    @Test
    @DisplayName("Verify approval workflow transitions request to APPROVED")
    void testApproveRequestWorkflow() {
        BorrowRequest request = new BorrowRequest();
        request.setItem(item);
        request.setBorrower(borrower);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setRequesterPhone("+91 9123456780");
        request.setRequesterEmail("bob@college.edu");

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findByIdForUpdate(any())).thenReturn(Optional.of(item));
        when(borrowingService.hasOverlappingBorrowing(any(), any(), any())).thenReturn(false);
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(i -> i.getArgument(0));

        borrowRequestService.approveRequest(1L);

        assertEquals("APPROVED", request.getStatus());
        verify(borrowingService, times(1)).createBorrowing(any());
    }

    @Test
    @DisplayName("Verify rejection workflow transitions request to REJECTED")
    void testRejectRequestWorkflow() {
        BorrowRequest request = new BorrowRequest();
        request.setItem(item);
        request.setBorrower(borrower);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));

        when(borrowRequestRepository.findById(2L)).thenReturn(Optional.of(request));
        when(borrowRequestRepository.save(any(BorrowRequest.class))).thenAnswer(i -> i.getArgument(0));

        borrowRequestService.rejectRequest(2L);

        assertEquals("REJECTED", request.getStatus());
    }

    @Test
    @DisplayName("Verify Contact Information privacy rules on request details endpoint")
    void testContactPrivacyOnRequestDetails() {
        // Mock session with Borrower ID
        when(session.getAttribute("studentId")).thenReturn(10L);

        // Set IDs using reflection helper
        setId(borrower, 10L);
        setId(owner, 20L);
        setId(unrelatedStudent, 30L);

        BorrowRequest approvedRequest = new BorrowRequest();
        approvedRequest.setItem(item);
        approvedRequest.setBorrower(borrower);
        approvedRequest.setStatus("APPROVED");
        approvedRequest.setRequesterPhone("+91 9123456780");
        approvedRequest.setRequesterEmail("bob@college.edu");

        when(borrowRequestRepository.findById(100L)).thenReturn(Optional.of(approvedRequest));

        Model model = new ExtendedModelMap();
        String view = borrowRequestController.viewRequestDetails(100L, session, model);

        assertEquals("borrow-request-details", view);
        assertTrue((Boolean) model.getAttribute("canViewContact"), "Contact info MUST be visible when request is APPROVED");
        assertTrue((Boolean) model.getAttribute("isBorrower"), "User is borrower");

        // When request is PENDING, contact info MUST NOT be visible
        BorrowRequest pendingRequest = new BorrowRequest();
        pendingRequest.setItem(item);
        pendingRequest.setBorrower(borrower);
        pendingRequest.setStatus("PENDING");

        when(borrowRequestRepository.findById(101L)).thenReturn(Optional.of(pendingRequest));

        Model pendingModel = new ExtendedModelMap();
        borrowRequestController.viewRequestDetails(101L, session, pendingModel);
        assertFalse((Boolean) pendingModel.getAttribute("canViewContact"), "Contact info MUST NOT be visible when PENDING");
    }

    @Test
    @DisplayName("Verify unrelated student cannot view request details (Security Authorization Check)")
    void testUnauthorizedUserAccessPrevention() {
        setId(borrower, 10L);
        setId(owner, 20L);
        setId(unrelatedStudent, 30L);

        // Session logged in as unrelated student (ID 30)
        when(session.getAttribute("studentId")).thenReturn(30L);

        BorrowRequest request = new BorrowRequest();
        request.setItem(item);
        request.setBorrower(borrower);
        request.setStatus("APPROVED");

        when(borrowRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        Model model = new ExtendedModelMap();
        String redirect = borrowRequestController.viewRequestDetails(200L, session, model);

        assertEquals("redirect:/dashboard?error=unauthorized", redirect, "Unrelated student must be redirected with unauthorized error");
    }

    @Test
    @DisplayName("Verify student profile contact details update")
    void testStudentProfileContactUpdate() {
        setId(borrower, 10L);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(borrower));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student updated = studentService.updateContactInfo(10L, "+91 9887766554", "EMAIL");

        assertEquals("+91 9887766554", updated.getPhoneNumber());
        assertEquals("EMAIL", updated.getPreferredContactMethod());
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
