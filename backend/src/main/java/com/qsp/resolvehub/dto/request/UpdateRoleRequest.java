package com.qsp.resolvehub.dto.request;

import com.qsp.resolvehub.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private Role roles;
}