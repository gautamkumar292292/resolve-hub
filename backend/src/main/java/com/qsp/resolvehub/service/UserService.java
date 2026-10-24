
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    // Employee updates their own profile.
    UserResponse updateProfile(
            Integer userId,
            UpdateUserRequest request
    );

    // Admin-only operations.
    List<UserResponse> getAllUsers();

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

    List<UserResponse> getAllAdmins();
}
