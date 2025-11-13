package com.microservices.user_service.model.dto;

import com.microservices.user_service.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "User Details")
public class UserDto {

    @Schema(description = "userId of User",example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",accessMode =  Schema.AccessMode.READ_ONLY)
    private String userId;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    @Schema(description = "User's name",example = "user",requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Schema(description = "user email",example = "user@email.com",requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    @Size(min = 10, max = 10, message = "Phone number must be between 10 and 15 digits")
    @Schema(description = "Phone Number of User",example = "1234567890",requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank(message = "Address cannot be blank")
    @Schema(description = "address of User",example = "Street 123",requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @Schema(description = "Roles of User",example = "[\"USER\",\"ADMIN\"]",accessMode = Schema.AccessMode.READ_ONLY)
    private Set<Role> roles= new HashSet<>();
    @Schema(description = "time when user was created",example = "null", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
    @Schema(description = "time when user was last updated",example = "null", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}
