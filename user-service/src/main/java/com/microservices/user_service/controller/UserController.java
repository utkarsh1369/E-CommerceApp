package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(description = "SUPER-ADMIN can access details of all the user.")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("REST request to get all users");
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    @Operation(description = "To get information of a user by id.Only admin and the User itself can access this.")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        log.info("REST request to get user with ID: {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(description = "To get the user details by email.Only Super-admin can access.")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("REST request to get user with email: {}", email);
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    @Operation(description = "To update a User.Super-admin and user itself can access only.")
    public ResponseEntity<UserDto> updateUser(@PathVariable String userId, @Valid @RequestBody UserDto userDto) {
        log.info("REST request to update user with ID: {}", userId);
        UserDto updatedUser = userService.updateUser(userId, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(description = "Super-admin can delete a user.")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("REST request to delete user with ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/assign-role/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(description = "Super-admin can assign roles to different users.")
    public ResponseEntity<UserDto> assignRoles(@PathVariable String userId, @RequestBody Set<Role> roles) {
        log.info("REST request to assign roles to user: {}", userId);
        UserDto user = userService.assignRoles(userId, roles);
        return ResponseEntity.ok(user);
    }
    //TODO:centralized logging-> done
    //TODO:swagger details
    //TODO:15 factor microservices
    //TODO:API versioning->done
    //TODO:RichardSon restapi model->done
}