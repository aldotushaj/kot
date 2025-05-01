package com.parkingsystem.service;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.User;
import com.parkingsystem.repository.ParkingRepository;
import com.parkingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public User createUser(String username, String password, String role) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(username, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    // Update user
    @Transactional
    public User updateUser(User user) {
        // Check if user exists
        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("User not found");
        }

        // If updating password, encode it
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // Delete user
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Assign attendant to parking
    @Transactional
    public User assignAttendantToParking(Long userId, Long parkingId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking not found");
        }

        User user = userOpt.get();
        Parking parking = parkingOpt.get();

        // Check if user is an attendant
        if (!user.getRole().equals("ATTENDANT")) {
            throw new RuntimeException("Only attendants can be assigned to parking locations");
        }

        user.setAssignedParking(parking);
        return userRepository.save(user);
    }

    // Remove attendant from parking
    @Transactional
    public User removeAttendantFromParking(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Check if user is an attendant
        if (!user.getRole().equals("ATTENDANT")) {
            throw new RuntimeException("Only attendants can be removed from parking locations");
        }

        user.setAssignedParking(null);
        return userRepository.save(user);
    }

    // Get attendants assigned to a parking location
    public List<User> getAttendantsByParking(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);

        if (parkingOpt.isEmpty()) {
            throw new RuntimeException("Parking not found");
        }

        return userRepository.findByAssignedParking(parkingOpt.get());
    }

    // Change user password
    @Transactional
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect old password");
        }

        // Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }
}