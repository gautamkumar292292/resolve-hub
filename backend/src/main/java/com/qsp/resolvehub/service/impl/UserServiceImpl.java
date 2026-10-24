
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // Update the authenticated user's permitted profile fields.
    @Override
    public UserResponse updateProfile(
            Integer userId,
            UpdateUserRequest request) {

        User user = findUser(userId);
        applyProfileChanges(user, request);

        return UserResponse.from(userRepository.save(user));
    }

    // Return all users.
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // Return one user.
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {

        return UserResponse.from(findUser(id));
    }

    // Admin updates permitted profile fields.
    @Override
    public UserResponse adminUpdateUser(
            Integer id,
            UpdateUserRequest request) {

        User user = findUser(id);
        applyProfileChanges(user, request);

        return UserResponse.from(userRepository.save(user));
    }

    // Admin changes a user's role.
    @Override
    public UserResponse updateUserRole(
            Integer id,
            UpdateRoleRequest request) {

        if (request == null || request.getRoles() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role is required"
            );
        }

        User user = findUser(id);
        Role requestedRole = request.getRoles();

        user.setRoles(requestedRole);

        invalidateExistingTokens(user);

        return UserResponse.from(userRepository.save(user));
    }

    // Admin activates or deactivates a user.
    @Override
    public UserResponse setUserStatus(
            Integer id,
            boolean active) {

        User user = findUser(id);

        user.setAccount_status(active);

        invalidateExistingTokens(user);

        return UserResponse.from(userRepository.save(user));
    }

    // Return only users whose role is ADMIN.
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllAdmins() {

        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRoles() == Role.ADMIN)
                .map(UserResponse::from)
                .toList();
    }

    // Find a user or return 404.
    private User findUser(Integer id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found with id: " + id
                        )
                );
    }

    // Update only permitted profile fields.
    private void applyProfileChanges(
            User user,
            UpdateUserRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        if (request.getName() != null) {
            String name = request.getName().trim();

            if (name.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Name cannot be blank"
                );
            }

            user.setName(name);
        }

        if (request.getEmail() != null) {
            String email = request.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (email.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email cannot be blank"
                );
            }

            boolean emailAlreadyUsed =
                    userRepository.existsByEmailAndIdNot(
                            email,
                            user.getId()
                    );

            if (emailAlreadyUsed) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email is already in use"
                );
            }

            user.setEmail(email);
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
    }

    // Invalidate existing tokens when permissions or account status change.
    private void invalidateExistingTokens(User user) {

        Integer currentVersion = user.getTokenVersion();

        user.setTokenVersion(
                currentVersion == null ? 1 : currentVersion + 1
        );
    }
}
