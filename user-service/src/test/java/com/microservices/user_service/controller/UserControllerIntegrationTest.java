package com.microservices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.security.UserSecurityService;
import com.microservices.user_service.service.UserService;
import com.microservices.user_service.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("FieldCanBeLocal")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserSecurityService userSecurityService;

    private UserDto adminDto;
    private UserDto userDto;
    private List<UserDto> userList;

    @BeforeEach
    void setup() {
        adminDto = UserDto.builder()
                .userId("admin1")
                .name("Admin User")
                .email("admin@example.com")
                .roles(Set.of(Role.SUPER_ADMIN, Role.USER))
                .build();

        userDto = UserDto.builder()
                .userId("u1")
                .name("John Doe")
                .email("john@example.com")
                .roles(Set.of(Role.USER))
                .build();

        userList = List.of(adminDto, userDto);
    }

    // ==================== GET /api/v1/users ====================

    @Test
    @DisplayName("GET /users - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllUsers_asSuperAdmin_returnsUserList() throws Exception {
        when(userService.getAllUsers()).thenReturn(userList);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[1].email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /users - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void getAllUsers_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users - Unauthorized as unauthenticated")
    void getAllUsers_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/v1/users/{userId} ====================

    @Test
    @DisplayName("GET /users/{userId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserById_asSuperAdmin_returnsUser() throws Exception {
        when(userService.getUserById("u1")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /users/{userId} - Success as self")
    @WithMockUser(username = "john@example.com") // Assuming username is email
    void getUserById_asSelf_returnsUser() throws Exception {
        // Mock the custom security check
        when(userSecurityService.isCurrentUser("u1")).thenReturn(true);
        when(userService.getUserById("u1")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    @DisplayName("GET /users/{userId} - Forbidden as other USER")
    @WithMockUser(username = "other.user@example.com")
    void getUserById_asOtherUser_returnsForbidden() throws Exception {
        // Mock the custom security check
        when(userSecurityService.isCurrentUser("u1")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/u1"))
                .andExpect(status().isForbidden());

        // Verify service is never called due to security rule
        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("GET /users/{userId} - Not Found")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserById_notFound_returnsNotFound() throws Exception {
        when(userService.getUserById("not-found"))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/not-found"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/users/email/{email} ====================

    @Test
    @DisplayName("GET /users/email/{email} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserByEmail_asSuperAdmin_returnsUser() throws Exception {
        when(userService.getUserByEmail("john@example.com")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /users/email/{email} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void getUserByEmail_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/john@example.com"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/users/update/{userId} ====================

    @Test
    @DisplayName("PUT /users/update/{userId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUser_asSuperAdmin_returnsUpdatedUser() throws Exception {
        UserDto updatedDto = UserDto.builder()
                .userId("u1")
                .name("John A. Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .address("123 Street xyz")
                .roles(Set.of(Role.USER))
                .build();

        when(userService.updateUser(eq("u1"), any(UserDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/users/update/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John A. Doe"));
    }

    @Test
    @DisplayName("PUT /users/update/{userId} - Success as self")
    @WithMockUser(username = "john@example.com")
    void updateUser_asSelf_returnsUpdatedUser() throws Exception {
        when(userSecurityService.isCurrentUser("u1")).thenReturn(true);

        UserDto updatedDto = UserDto.builder()
                .userId("u1")
                .name("John A. Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .address("123 Street xyz")
                .roles(Set.of(Role.USER))
                .build();

        when(userService.updateUser(eq("u1"), any(UserDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/users/update/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John A. Doe"));
    }

    @Test
    @DisplayName("PUT /users/update/{userId} - Invalid input returns Bad Request")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUser_invalidInput_returnsBadRequest() throws Exception {
        // Assuming UserDto has @Email validation
        UserDto invalidDto = UserDto.builder()
                .userId("u1")
                .name("Test")
                .email("not-an-email")
                .build();

        mockMvc.perform(put("/api/v1/users/update/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any());
    }

    // ==================== DELETE /api/v1/users/delete/{userId} ====================

    @Test
    @DisplayName("DELETE /users/delete/{userId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteUser_asSuperAdmin_returnsNoContent() throws Exception {
        doNothing().when(userService).deleteUser("u1");

        mockMvc.perform(delete("/api/v1/users/delete/u1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser("u1");
    }

    @Test
    @DisplayName("DELETE /users/delete/{userId} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/delete/u1"))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    @DisplayName("DELETE /users/delete/{userId} - Not Found")
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteUser_notFound_returnsNotFound() throws Exception {
        doThrow(new UserNotFoundException("User not found"))
                .when(userService).deleteUser("not-found");

        mockMvc.perform(delete("/api/v1/users/delete/not-found"))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /api/v1/users/assign-role/{userId} ====================

    @Test
    @DisplayName("PUT /users/assign-role/{userId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignRoles_asSuperAdmin_returnsUpdatedUser() throws Exception {
        Set<Role> newRoles = Set.of(Role.USER, Role.SUPER_ADMIN);
        UserDto updatedUser = UserDto.builder()
                .userId("u1")
                .name("John Doe")
                .email("john@example.com")
                .roles(newRoles)
                .build();

        when(userService.assignRoles("u1", newRoles)).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/assign-role/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(2)))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "SUPER_ADMIN")));
    }

    @Test
    @DisplayName("PUT /users/assign-role/{userId} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void assignRoles_asUser_returnsForbidden() throws Exception {
        Set<Role> newRoles = Set.of(Role.USER, Role.SUPER_ADMIN);

        mockMvc.perform(put("/api/v1/users/assign-role/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoles)))
                .andExpect(status().isForbidden());

        verify(userService, never()).assignRoles(any(), any());
    }
}