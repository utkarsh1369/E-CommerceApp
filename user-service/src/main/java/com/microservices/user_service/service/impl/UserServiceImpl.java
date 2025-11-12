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
import java.util.UUID;

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

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + registrationDto.getEmail());
        }
        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());

        Users user = userMapper.toEntity(registrationDto, encodedPassword);
        Users savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto createSuperAdmin(UserRegistrationDto registrationDto) {

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + registrationDto.getEmail());
        }
        if (superAdminExists()) {
            throw new IllegalStateException("Super Admin already exists in the system");
        }
        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());

        Users superAdmin = Users.builder()
                .userId(UUID.randomUUID().toString())
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .phoneNumber(registrationDto.getPhoneNumber())
                .address(registrationDto.getAddress())
                .roles(Set.of(Role.SUPER_ADMIN))
                .build();
        Users savedAdmin = userRepository.save(superAdmin);

        return userMapper.toDto(savedAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean superAdminExists() {
        return userRepository.findAll().stream()
                .anyMatch(user -> user.getRoles() != null && user.getRoles().contains(Role.SUPER_ADMIN));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        validateUserAccess(user.getUserId());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        validateUserAccess(user.getUserId());

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public List<UserDto> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Unauthorized access");
        }

        if (!principal.getRoles().contains(Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only SUPER_ADMIN can access all users");
        }

        List<Users> users = userRepository.findAll();
        log.info("Found {} users", users.size());

        return userMapper.toDtoList(users);
    }


    @Override
    @Transactional
    public UserDto updateUser(String userId, UserDto userDto) {

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
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Unauthorized access");
        }
        if (!principal.getUserId().equals(userId) && !principal.getRoles().contains(Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only SUPER_ADMIN or the user themselves can delete this account");
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Deleted user with id: {}", userId);
    }


    @Override
    @Transactional
    public UserDto assignRoles(String userId, Set<Role> roles) {
        log.info("Assigning roles {} to user with ID: {}", roles, userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setRoles(roles);
        Users updatedUser = userRepository.save(user);

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
