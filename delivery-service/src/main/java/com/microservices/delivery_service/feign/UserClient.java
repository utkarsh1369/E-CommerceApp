package com.microservices.delivery_service.feign;

import com.microservices.delivery_service.config.FeignConfig;
import com.microservices.delivery_service.model.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service",configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);

}
