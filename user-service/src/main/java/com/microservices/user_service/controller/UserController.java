package com.microservices.user_service.controller;

import com.microservices.user_service.model.dto.ErrorMessage;
import com.microservices.user_service.model.dto.ErrorResponse;
import com.microservices.user_service.model.dto.UserDto;
import com.microservices.user_service.model.Role;
import com.microservices.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User APIs",description = "API version is- v1")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get list of all users.",description = "Only SUPER_ADMIN can access it.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of Users Found",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserDto.class))
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    @Operation(summary = "Get User details By userId",description = "Only SUPER_ADMIN and the USER itself can access this.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User with userId Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",description = "User Not Found",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDto> getUserById(@Parameter(description = "The unique identifier(UUID) of User",required = true,
            example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8") @PathVariable String userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get User details By Its e-mail",description = "Only SUPER_ADMIN or the USER itself can access this.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User with e-mail Found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",description = "User Not Found",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDto> getUserByEmail(@Parameter(description = "The unique identifier(e-mail) of User",required = true,
            example = "user@example.com")@PathVariable String email) {
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    @Operation(summary = "Update User Details", description = "Allows a Super Admin or the user themselves to update their information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User Details updated Successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",description = "User Not Found",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",description = "Conflict",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDto> updateUser(@Parameter(description = "The unique identifier(UUID) of User",required = true, example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
                                                  @PathVariable String userId,
                                              @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                      description = "The user Details to update.Roles of User can not be updated here.",required = true)
                                              @Valid @RequestBody UserDto userDto){
        UserDto updatedUser = userService.updateUser(userId, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a User", description = "Only Super-admin can delete a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "No Content",content = @Content
            ),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",description = "User Not Found",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(@Parameter(description = "The unique identifier(UUID) of user",required = true,
            example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/assign-role/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "To assign different Roles to user",description = "Super-admin can assign roles to different users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Role assigned to user.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400",description = "Bad Request",content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401",description = "Unauthorized",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403",description = "Forbidden",content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404",description = "User Not Found",content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDto> assignRoles(@Parameter(description = "The unique identifier(UUID) of user",required = true,
            example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")@PathVariable String userId,@io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "To assign Roles to the User.",required = true
    ) @Valid @RequestBody Set<Role> roles) {
        UserDto user = userService.assignRoles(userId, roles);
        return ResponseEntity.ok(user);
    }
    //TODO:15 factor microservices->cicd left
}