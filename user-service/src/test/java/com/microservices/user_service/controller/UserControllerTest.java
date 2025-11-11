package com.microservices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.security.*;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@SuppressWarnings("unchecked")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserSecurityService userSecurityService;
    private UserDto userDto;
    private List<UserDto> userList;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        userDto = UserDto.builder()
                .userId("user123")
                .name("John Doe")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .address("123 Main St")
                .roles(Set.of(Role.USER))
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserDto userDto2 = UserDto.builder()
                .userId("user456")
                .name("Jane Smith")
                .email("admin@example.com")
                .phoneNumber("0987654321")
                .address("456 Oak Ave")
                .roles(Set.of(Role.SUPER_ADMIN))
                .createdAt(now)
                .updatedAt(now)
                .build();

        userList = Arrays.asList(userDto, userDto2);
    }

    @Test
    @DisplayName("Should get all users when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllUsers_AsSuperAdmin_ShouldReturnUserList() throws Exception {
        when(userService.getAllUsers()).thenReturn(userList);

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value("user123"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].userId").value("user456"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Should return 403 when non-SUPER_ADMIN tries to get all users")
    @WithMockUser(roles = "USER")
    void getAllUsers_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user tries to get all users")
    void getAllUsers_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getAllUsers();
    }


    @Test
    @DisplayName("Should get user by ID when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserById_AsSuperAdmin_ShouldReturnUser() throws Exception {
        when(userService.getUserById("user123")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.address").value("123 Main St"));

        verify(userService, times(1)).getUserById("user123");
    }

    @Test
    @DisplayName("Should get user by ID when user is accessing their own profile")
    @WithMockUser(username = "user123", roles = "USER")
    void getUserById_AsCurrentUser_ShouldReturnUser() throws Exception {
        when(userSecurityService.isCurrentUser("user123")).thenReturn(true);
        when(userService.getUserById("user123")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"));

        verify(userService, times(1)).getUserById("user123");
        verify(userSecurityService, times(1)).isCurrentUser("user123");
    }

    @Test
    @DisplayName("Should return 403 when user tries to access another user's profile")
    @WithMockUser(username = "user999", roles = "USER")
    void getUserById_AsOtherUser_ShouldReturnForbidden() throws Exception {
        when(userSecurityService.isCurrentUser("user123")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyString());
    }


    @Test
    @DisplayName("Should get user by email when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserByEmail_AsSuperAdmin_ShouldReturnUser() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/email/test@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService, times(1)).getUserByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return 403 when non-SUPER_ADMIN tries to get user by email")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void getUserByEmail_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/test@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserByEmail(anyString());
    }


    @Test
    @DisplayName("Should update user when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUser_AsSuperAdmin_ShouldReturnUpdatedUser() throws Exception {
        UserDto updatedDto = UserDto.builder()
                .userId("user123")
                .name("John Updated")
                .email("updated@example.com")
                .phoneNumber("1111111111")
                .address("789 New St")
                .roles(Set.of(Role.USER))
                .build();

        when(userService.updateUser(eq("user123"), any(UserDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/users/update/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("1111111111"));

        verify(userService, times(1)).updateUser(eq("user123"), any(UserDto.class));
    }

    @Test
    @DisplayName("Should update user when user is updating their own profile")
    @WithMockUser(username = "user123", roles = "USER")
    void updateUser_AsCurrentUser_ShouldReturnUpdatedUser() throws Exception {
        when(userSecurityService.isCurrentUser("user123")).thenReturn(true);
        when(userService.updateUser(eq("user123"), any(UserDto.class))).thenReturn(userDto);

        mockMvc.perform(put("/api/v1/users/update/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateUser(eq("user123"), any(UserDto.class));
        verify(userSecurityService, times(1)).isCurrentUser("user123");
    }

    @Test
    @DisplayName("Should return 403 when user tries to update another user's profile")
    @WithMockUser(username = "user999", roles = "USER")
    void updateUser_AsOtherUser_ShouldReturnForbidden() throws Exception {
        when(userSecurityService.isCurrentUser("user123")).thenReturn(false);

        mockMvc.perform(put("/api/v1/users/update/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(anyString(), any(UserDto.class));
    }

    @Test
    @DisplayName("Should return 400 when update user request has invalid data")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        UserDto invalidDto = UserDto.builder().build(); // Missing required fields

        mockMvc.perform(put("/api/v1/users/update/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(anyString(), any(UserDto.class));
    }

    @Test
    @DisplayName("Should delete user when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteUser_AsSuperAdmin_ShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser("user123");

        mockMvc.perform(delete("/api/v1/users/delete/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser("user123");
    }

    @Test
    @DisplayName("Should return 403 when non-SUPER_ADMIN tries to delete user")
    @WithMockUser(roles = "ORDER_ADMIN")
    void deleteUser_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/delete/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("Should return 403 when user tries to delete their own account")
    @WithMockUser(username = "user123", roles = "USER")
    void deleteUser_AsCurrentUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/delete/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("Should assign roles when authenticated as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignRoles_AsSuperAdmin_ShouldReturnUpdatedUser() throws Exception {
        Set<Role> roles = new HashSet<>(Arrays.asList(Role.PRODUCT_ADMIN, Role.USER));
        UserDto updatedUser = UserDto.builder()
                .userId("user123")
                .name("John Doe")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .address("123 Main St")
                .roles(roles)
                .build();

        when(userService.assignRoles("user123", roles)).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/assign-role/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(2));

        verify(userService, times(1)).assignRoles(eq("user123"), any(Set.class));
    }

    @Test
    @DisplayName("Should return 403 when non-SUPER_ADMIN tries to assign roles")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void assignRoles_AsAdmin_ShouldReturnForbidden() throws Exception {
        Set<Role> roles = Set.of(Role.ORDER_ADMIN);

        mockMvc.perform(put("/api/v1/users/assign-role/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isForbidden());

        verify(userService, never()).assignRoles(anyString(), any(Set.class));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user tries to assign roles")
    void assignRoles_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        Set<Role> roles = Set.of(Role.USER);

        mockMvc.perform(put("/api/v1/users/assign-role/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).assignRoles(anyString(), any(Set.class));
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllUsers_NoUsers_ShouldReturnEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Should handle multiple roles assignment")
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignRoles_MultipleRoles_ShouldReturnUpdatedUser() throws Exception {
        Set<Role> roles = new HashSet<>(Arrays.asList(
                Role.PRODUCT_ADMIN,
                Role.ORDER_ADMIN,
                Role.DELIVERY_ADMIN,
                Role.USER
        ));

        UserDto updatedUser = UserDto.builder()
                .userId("user123")
                .name("John Doe")
                .email("test@example.com")
                .roles(roles)
                .build();

        when(userService.assignRoles("user123", roles)).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/assign-role/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.roles.length()").value(4));

        verify(userService, times(1)).assignRoles(eq("user123"), any(Set.class));
    }
}
