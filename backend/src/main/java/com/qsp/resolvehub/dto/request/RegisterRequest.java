
package com.qsp.resolvehub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 180, message = "Email cannot exceed 180 characters")
    private String email;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            max = 72,
            message = "Password must contain 8 to 72 characters"
    )
    private String password;
}
