package com.microservices.user_service.controller;

import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDto = new UserDto();
        userDto.setUserId("1");
        userDto.setEmail("test@example.com");
        userDto.setName("Test User");
    }

    @Test
    void testGetAllUsers_WithPagination() {
        List<UserDto> userDtoList = List.of(userDto);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserDto> userPage = new PageImpl<>(userDtoList, pageable, 1);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);
        ResponseEntity<Page<UserDto>> response = userController.getAllUsers(pageable);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("Test User", response.getBody().getContent().getFirst().getName());
        assertEquals(0, response.getBody().getNumber());
        assertEquals(1, response.getBody().getTotalPages());
        verify(userService, times(1)).getAllUsers(any(Pageable.class));
    }

    @Test
    void testGetUserById() {
        when(userService.getUserById("1")).thenReturn(userDto);

        ResponseEntity<UserDto> response = userController.getUserById("1");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService, times(1)).getUserById("1");
    }

    @Test
    void testGetUserByEmail() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(userDto);

        ResponseEntity<UserDto> response = userController.getUserByEmail("test@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Test User", response.getBody().getName());
        verify(userService, times(1)).getUserByEmail("test@example.com");
    }

    @Test
    void testUpdateUser() {
        when(userService.updateUser(eq("1"), any(UserDto.class))).thenReturn(userDto);

        ResponseEntity<UserDto> response = userController.updateUser("1", userDto);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("1", response.getBody().getUserId());
        verify(userService, times(1)).updateUser(eq("1"), any(UserDto.class));
    }

    @Test
    void testDeleteUser() {
        doNothing().when(userService).deleteUser("1");

        ResponseEntity<Void> response = userController.deleteUser("1");

        assertEquals(204, response.getStatusCode().value());
        verify(userService, times(1)).deleteUser("1");
    }

    @Test
    void testAssignRoles() {
        Set<Role> roles = Set.of(Role.ORDER_ADMIN);
        when(userService.assignRoles("1", roles)).thenReturn(userDto);

        ResponseEntity<UserDto> response = userController.assignRoles("1", roles);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Test User", response.getBody().getName());
        verify(userService, times(1)).assignRoles("1", roles);
    }
}
