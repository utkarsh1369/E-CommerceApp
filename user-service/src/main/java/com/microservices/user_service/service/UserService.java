package com.microservices.user_service.service;

import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.dto.UserRegistrationDto;
import com.microservices.user_service.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserService {

    UserDto registerUser(UserRegistrationDto registrationDto);

    UserDto createSuperAdmin(UserRegistrationDto registrationDto);

    boolean superAdminExists();

    UserDto getUserById(String userId);

    UserDto getUserByEmail(String email);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto updateUser(String userId, UserDto userDto);

    void deleteUser(String userId);

    UserDto assignRoles(String userId, Set<Role> roles);

}