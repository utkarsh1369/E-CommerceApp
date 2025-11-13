package com.microservices.user_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Response message whenever an error is occurred",example = "")
public class ErrorMessage {
    private String message;
}
