
package com.lendloop.service;

import com.lendloop.model.Student;
import com.lendloop.repository.StudentRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
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

    // Authenticate student login
    public Student authenticateStudent(
            String email, String rawPassword) {

        Student student = studentRepository.findByEmail(email)
                .orElse(null);

        if (student == null) {
            return null;
        }

        if (passwordEncoder.matches(
                rawPassword, student.getPassword())) {
            return student;
        }

        return null;
    }

    // Find student using ID
    public Student findStudentById(Long id) {

        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found!"));
    }
}
