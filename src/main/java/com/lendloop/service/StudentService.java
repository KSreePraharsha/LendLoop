
package com.lendloop.service;

import com.lendloop.model.Student;
import com.lendloop.repository.StudentRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ----------------------------------------------------------------
    // UserDetailsService — called by Spring Security on every login
    // ----------------------------------------------------------------
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No student registered with email: " + email));

        return User.withUsername(student.getEmail())
                .password(student.getPassword())
                .roles("STUDENT")
                .build();
    }

    // Register a new student
    public Student registerStudent(Student student) {

        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered!");
        }

        if (studentRepository.existsByRollNumber(student.getRollNumber())) {
            throw new IllegalArgumentException(
                    "Roll number is already registered!");
        }

        student.setPassword(
                passwordEncoder.encode(student.getPassword()));

        return studentRepository.save(student);
    }

    // Find student by their email (used to populate session after login)
    public Student findStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found!"));
    }

    // Find student using ID
    public Student findStudentById(Long id) {

        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found!"));
    }

    // Update student's contact details (phone number and preferred method)
    public Student updateContactInfo(Long studentId, String phoneNumber, String preferredContactMethod) {
        Student student = findStudentById(studentId);
        if (phoneNumber != null) {
            student.setPhoneNumber(phoneNumber.trim());
        }
        if (preferredContactMethod != null && !preferredContactMethod.isBlank()) {
            student.setPreferredContactMethod("EMAIL".equalsIgnoreCase(preferredContactMethod) ? "EMAIL" : "PHONE");
        }
        return studentRepository.save(student);
    }
}
