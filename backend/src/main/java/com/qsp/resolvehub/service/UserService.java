
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.Role;

import java.util.List;

public interface UserService {

    // Employee updates their own profile.
    UserResponse updateProfile(
            Integer userId,
            UpdateUserRequest request
    );

    // Admin-only user operations.
    List<UserResponse> getAllUsers();

    // Get users filtered by role.
    List<UserResponse> getUsersByRole(Role role);

    UserResponse getUserById(Integer id);

    UserResponse adminUpdateUser(
            Integer id,
            UpdateUserRequest request
    );

    UserResponse updateUserRole(
            Integer id,
            UpdateRoleRequest request
    );

    UserResponse setUserStatus(
            Integer id,
            boolean active
    );

    // Get all administrators.
    List<UserResponse> getAllAdmins();
}
