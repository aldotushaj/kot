package com.parkingsystem.repository;

import com.parkingsystem.model.Parking;
import com.parkingsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);
    List<User> findByAssignedParking(Parking parking);
}