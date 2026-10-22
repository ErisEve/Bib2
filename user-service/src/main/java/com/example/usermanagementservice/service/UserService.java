package com.example.usermanagementservice.service;

import com.example.usermanagementservice.dto.UserActivityDTO;
import com.example.usermanagementservice.model.User;
import com.example.usermanagementservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

   private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public User registerUser(User user) {
        // Check if username already exists
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }
        // encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // save to Database
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public Optional<User> findById(Long id){
        return userRepository.findById(id);
    }
    // Password check helper
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public User updateEmailAndPassword(Long userId, String newEmail, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Hash and update password if provided
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword)); // ← HASH HERE
        }

        return userRepository.save(user);
    }
    // Convert dto in Service
    public List<UserActivityDTO> getUserRoleAndBookCount(){

        // Get the raw result from the native query: [email, role, bookCount])
        List<Object[]> result = userRepository.findUserRoleAndBookCount();

        // Create an empty list to hold the final DTOs
        List<UserActivityDTO> userActivities = new ArrayList<>();

        // Loop through each row of the result and extract values with type safety
        for(Object[] row : result) {
            String email = (String) row[0];
            String role = (String) row[1];
            // handles both Integer and Long
            Long bookCount = ((Number) row[2]).longValue();

            // Create a new DTO and add it to the list
            userActivities.add(new UserActivityDTO(email, role, bookCount));
        }

        return userActivities;

    }

}
