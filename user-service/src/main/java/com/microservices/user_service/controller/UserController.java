package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("REST request to get all users");
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        log.info("REST request to get user with ID: {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("REST request to get user with email: {}", email);
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserDto userDto) {
        log.info("REST request to update user with ID: {}", userId);
        UserDto updatedUser = userService.updateUser(userId, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("REST request to delete user with ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> assignRoles(
            @PathVariable String userId,
            @RequestBody Set<Role> roles) {
        log.info("REST request to assign roles to user: {}", userId);
        UserDto user = userService.assignRoles(userId, roles);
        return ResponseEntity.ok(user);
    }
}