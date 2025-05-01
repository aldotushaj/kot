// src/main/java/com/parkingsystem/service/UserService.java
package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.User;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ParkingRepository parkingRepository;
    private final PasswordEncoder passwordEncoder;

    // Use constructor injection instead of field injection
    @Autowired
    public UserService(UserRepository userRepository,
                       ParkingRepository parkingRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.parkingRepository = parkingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by id
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get user by username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Get all attendants
    public List<User> getAllAttendants() {
        return userRepository.findByRole("ATTENDANT");
    }

    // Create new user
    public User createUser(String username, String password, String role) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(username, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    // Update user
    public User updateUser(User user) {
        // If updating password, encode it
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // Delete user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Assign attendant to parking
    public User assignAttendantToParking(Long userId, Long parkingId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (userOpt.isPresent() && parkingOpt.isPresent()) {
            User user = userOpt.get();

            // Check if user is an attendant
            if (!user.getRole().equals("ATTENDANT")) {
                throw new RuntimeException("Only attendants can be assigned to parking locations");
            }

            user.setAssignedParking(parkingOpt.get());
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User or Parking not found");
        }
    }

    // Remove attendant from parking
    public User removeAttendantFromParking(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAssignedParking(null);
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    // Get attendants assigned to a parking location
    public List<User> getAttendantsByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isPresent()) {
            return userRepository.findByAssignedParking(parkingOpt.get());
        } else {
            throw new RuntimeException("Parking not found");
        }
    }

    // Change user password
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify old password
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("Incorrect old password");
            }

            // Set new password
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }
}