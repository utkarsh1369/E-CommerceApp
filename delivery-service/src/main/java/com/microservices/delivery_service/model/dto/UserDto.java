package com.microservices.delivery_service.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String address;
}
