package com.microservices.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.user_service.AbstractIntegrationTest;
import com.microservices.user_service.exception.UserNotFoundException;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.model.UserPrincipal;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserDto userDto;

    @BeforeEach
    void setup() {
        userDto = UserDto.builder()
                .userId("u1")
                .name("John Doe")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .address("123 Street")
                .roles(Set.of(Role.USER))
                .build();
    }

    private Authentication createAuth(String userId, Role... roles) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email("test@example.com")
                .password("password")
                .roles(Set.of(roles))
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getAllUsers_Success_ForSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_Forbidden_ForNormalUser() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(authentication(createAuth("u1", Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_Success_ForSelf() throws Exception {
        when(userService.getUserById("u1")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/u1")
                        .with(authentication(createAuth("u1", Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void getUserById_Success_ForSuperAdmin() throws Exception {
        when(userService.getUserById("u1")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/u1")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void getUserById_Forbidden_ForOtherUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/u1")
                        .with(authentication(createAuth("u2", Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_NotFound() throws Exception {
        when(userService.getUserById("u1")).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/u1")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserByEmail_Success_ForSuperAdmin() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/email/test@example.com")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserByEmail_Forbidden_ForUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/test@example.com")
                        .with(authentication(createAuth("u1", Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_Success_ForSelf() throws Exception {
        when(userService.updateUser(eq("u1"), any(UserDto.class))).thenReturn(userDto);

        mockMvc.perform(put("/api/v1/users/update/u1")
                        .with(authentication(createAuth("u1", Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void updateUser_Forbidden_ForOtherUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/update/u1")
                        .with(authentication(createAuth("u2", Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_BadRequest_InvalidInput() throws Exception {
        UserDto invalidDto = UserDto.builder()
                .userId("u1")
                .name("")
                .email("invalid-email")
                .build();

        mockMvc.perform(put("/api/v1/users/update/u1")
                        .with(authentication(createAuth("u1", Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_Success_ForSuperAdmin() throws Exception {
        doNothing().when(userService).deleteUser("u1");

        mockMvc.perform(delete("/api/v1/users/delete/u1")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_Forbidden_ForUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/delete/u1")
                        .with(authentication(createAuth("u1", Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_NotFound() throws Exception {
        doThrow(new UserNotFoundException("Not found")).when(userService).deleteUser("u1");

        mockMvc.perform(delete("/api/v1/users/delete/u1")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignRoles_Success_ForSuperAdmin() throws Exception {
        Set<Role> newRoles = Set.of(Role.USER, Role.ORDER_ADMIN);
        userDto.setRoles(newRoles);
        when(userService.assignRoles("u1", newRoles)).thenReturn(userDto);

        mockMvc.perform(patch("/api/v1/users/assign-role/u1")
                        .with(authentication(createAuth("admin", Role.SUPER_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void assignRoles_Forbidden_ForUser() throws Exception {
        Set<Role> newRoles = Set.of(Role.SUPER_ADMIN);

        mockMvc.perform(patch("/api/v1/users/assign-role/u1")
                        .with(authentication(createAuth("u1", Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoles)))
                .andExpect(status().isForbidden());
    }
}