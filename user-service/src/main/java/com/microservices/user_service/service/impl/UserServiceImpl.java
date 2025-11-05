package com.microservices.user_service.service.impl;

import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.dto.UserRegistrationDto;
import com.microservices.user_service.exception.DuplicateEmailException;
import com.microservices.user_service.exception.UnauthorizedException;
import com.microservices.user_service.exception.UserNotFoundException;
import com.microservices.user_service.mapper.UserMapper;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.Users;
import com.microservices.user_service.repository.UserRepository;
import com.microservices.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        log.info("Registering new user with email: {}", registrationDto.getEmail());
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + registrationDto.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());

        Users user = userMapper.toEntity(registrationDto, encodedPassword);
        Users savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getUserId());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        log.info("Fetching user with ID: {}", userId);

        validateUserAccess(userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        validateUserAccess(user.getUserId());

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public List<UserDto> getAllUsers() {
        log.info("Fetching all users");

        List<Users> users = userRepository.findAll();
        log.info("Found {} users", users.size());

        return userMapper.toDtoList(users);
    }

    @Override
    @Transactional
    public UserDto updateUser(String userId, UserDto userDto) {
        log.info("Updating user with ID: {}", userId);

        validateUserAccess(userId);

        Users existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(userDto.getEmail())) {
                throw new DuplicateEmailException("Email already in use: " + userDto.getEmail());
            }
            existingUser.setEmail(userDto.getEmail());
        }

        userMapper.updateEntityFromDto(userDto, existingUser);
        Users updatedUser = userRepository.save(existingUser);

        log.info("User updated successfully with ID: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        log.info("Deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully with ID: {}", userId);
    }

    @Override
    @Transactional
    public UserDto assignRoles(String userId, Set<Role> roles) {
        log.info("Assigning roles {} to user with ID: {}", roles, userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setRoles(roles);
        Users updatedUser = userRepository.save(user);

        log.info("Roles assigned successfully to user: {}", userId);
        return userMapper.toDto(updatedUser);
    }


    private void validateUserAccess(String requestedUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        String currentUserId = currentUser.getUserId();

        if (currentUser.getRoles().contains(Role.SUPER_ADMIN)) {
            return;
        }

        if (!currentUserId.equals(requestedUserId)) {
            throw new UnauthorizedException("You don't have permission to access this user's data");
        }
    }
}