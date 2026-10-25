package com.qsp.resolvehub.dto.response;

import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer id;
    private String name;
    private String email;
    private String phone;
    private Role roles;
    private boolean accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoles(),
                user.isAccount_status(),
                user.getCreated_at(),
                user.getUpdated_at()
        );
    }
}