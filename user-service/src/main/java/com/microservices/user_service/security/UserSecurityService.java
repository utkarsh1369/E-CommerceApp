package com.microservices.user_service.security;

import com.microservices.user_service.model.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service("userSecurityService")
public class UserSecurityService {

    public boolean isCurrentUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        boolean isCurrentUser = currentUser.getUserId().equals(userId);

        log.debug("Checking if current user {} is same as requested user {}: {}",
                currentUser.getUserId(), userId, isCurrentUser);

        return isCurrentUser;
    }
}