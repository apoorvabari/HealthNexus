package org.proj.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.UserFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.entity.UserEntity;
import org.proj.entity.RoleEntity;
import org.proj.mapper.UserMapper;
import org.proj.repository.UserRepo;
import org.proj.repository.RoleRepo;
import org.proj.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.proj.security.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private RoleRepo roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
                throw new IllegalArgumentException("User email already exists");
            }
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("Phone number already exists");
            }

            RoleEntity role = roleRepository.findByRoleName(request.getRole().trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRole()));

            UserEntity user = userMapper.toEntity(request);
            user.setRole(role);

            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setUserId(UUID.randomUUID());

            UserEntity savedUser = userRepository.saveAndFlush(user);
            RegisterResponse response = userMapper.toResponse(savedUser);
            response.setMessage("User registered successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to register user. Please try again.", e);
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            UserEntity user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Email not found"));

            if (Boolean.FALSE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
                throw new IllegalArgumentException("Account is inactive or disabled.");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            UserEntity userDetails = (UserEntity) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return LoginResponse.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                    .accessToken(jwt)
                    .refreshToken(null)
                    .message("Login successful")
                    .build();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid email or password.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to login. Please try again.", e);
        }
    }

    @Override
    public RegisterResponse getUserById(UUID id) {
        try {
            validateId(id);
            UserEntity user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            return userMapper.toResponse(user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch user.");
        }
    }

    @Override
    public List<RegisterResponse> getAllUsers() {
        try {
            List<UserEntity> users = userRepository.findByIsActiveTrueAndIsDeletedFalse();

            return users.stream()
                    .map(userMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch user list.");
        }
    }

    @Override
    public RegisterResponse updateUser(UUID id, RegisterRequest request) {
        try {
            validateId(id);
            UserEntity user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim().toLowerCase();
                if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
                    throw new IllegalArgumentException("Email already exists");
                }
                user.setEmail(newEmail);
            }

            if (request.getRole() != null) {
                RoleEntity role = roleRepository.findByRoleName(request.getRole().trim().toLowerCase())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRole()));
                user.setRole(role);
            }

            userMapper.updateEntity(user, request);

            UserEntity updatedUser = userRepository.save(user);

            RegisterResponse response = userMapper.toResponse(updatedUser);
            response.setMessage("User updated successfully");

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update user.", e);
        }
    }

    @Override
    public void deleteUser(UUID id) {
        try {
            validateId(id);
            UserEntity user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                throw new IllegalArgumentException("User is already deleted.");
            }

            user.setIsDeleted(true);
            user.setIsActive(false);

            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete user.");
        }
    }

    @Override
    public LogoutResponse logout(String userId) {
        try {
            LogoutResponse response = new LogoutResponse();
            if (userId != null && !userId.isBlank()) {
                response.setMessage("Logged out successfully for user " + userId);
            } else {
                response.setMessage("Logged out successfully");
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Unable to logout.");
        }
    }

    @Override
    public List<RegisterResponse> filterUsers(UserFilterRequest filterRequest) {
        try {
            Boolean isActive = null;
            Boolean isDeleted = null;
            String status = filterRequest.getStatus();

            if (status != null) {
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    isActive = true;
                    isDeleted = false;
                } else if ("DELETED".equalsIgnoreCase(status)) {
                    isActive = false;
                    isDeleted = true;
                }
            }

            String firstName = filterRequest.getFirstName();
            if (firstName != null && firstName.trim().isEmpty()) {
                firstName = null;
            } else if (firstName != null) {
                firstName = firstName.trim();
            }

            String middleName = filterRequest.getMiddleName();
            if (middleName != null && middleName.trim().isEmpty()) {
                middleName = null;
            } else if (middleName != null) {
                middleName = middleName.trim();
            }

            String lastName = filterRequest.getLastName();
            if (lastName != null && lastName.trim().isEmpty()) {
                lastName = null;
            } else if (lastName != null) {
                lastName = lastName.trim();
            }

            String email = filterRequest.getEmail();
            if (email != null && email.trim().isEmpty()) {
                email = null;
            } else if (email != null) {
                email = email.trim();
            }

            String phoneNumber = filterRequest.getPhoneNumber();
            if (phoneNumber != null && phoneNumber.trim().isEmpty()) {
                phoneNumber = null;
            } else if (phoneNumber != null) {
                phoneNumber = phoneNumber.trim();
            }

            List<UserEntity> users = userRepository.filterUsers(
                    filterRequest.getId(),
                    firstName,
                    middleName,
                    lastName,
                    email,
                    phoneNumber,
                    isActive,
                    isDeleted);

            return users.stream()
                    .map(userMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error filtering users", e);
            throw new RuntimeException("Unable to filter users.", e);
        }
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User Id is required.");
        }
    }

    @Override
    public void updateLastLogin(String email) {
        try {
            UserEntity user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update last login.", e);
        }
    }

    @Override
    public void resetPassword(org.proj.dto.PasswordResetRequest request) {
        try {
            if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }

            UserEntity user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to reset password.", e);
        }
    }

    @Override
    public RegisterResponse getUserByEmail(String email) {
        try {
            UserEntity user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return userMapper.toResponse(user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch user by email.", e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }
}
