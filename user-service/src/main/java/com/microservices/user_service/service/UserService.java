package com.microservices.user_service.service;

import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.dto.UserRegistrationDto;
import com.microservices.user_service.model.Role;

import java.util.List;
import java.util.Set;

public interface UserService {

    UserDto registerUser(UserRegistrationDto registrationDto);

    UserDto getUserById(String userId);

    UserDto getUserByEmail(String email);

    List<UserDto> getAllUsers();

    UserDto updateUser(String userId, UserDto userDto);

    void deleteUser(String userId);

    UserDto assignRoles(String userId, Set<Role> roles);

}