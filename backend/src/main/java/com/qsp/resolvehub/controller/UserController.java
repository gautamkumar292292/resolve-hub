
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse response =
                userService.updateProfile(user.getId(), request);

        return ResponseEntity.ok(response);
    }
}
