package com.saas.service;

import com.saas.dto.admin.AdminSubscriptionResponse;
import com.saas.dto.admin.AdminUserResponse;
import com.saas.entity.Role;
import com.saas.entity.SubscriptionStatus;
import com.saas.entity.User;
import com.saas.exception.ResourceNotFoundException;
import com.saas.exception.SubscriptionException;
import com.saas.repository.SubscriptionRepository;
import com.saas.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(user -> {
                String plan = subscriptionRepository
                    .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                    .map(sub -> sub.getPlan().name())
                    .orElse("NONE");
                return AdminUserResponse.from(user, plan);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll()
            .stream()
            .map(AdminSubscriptionResponse::from)
            .toList();
    }

    @Transactional
    public AdminUserResponse disableUser(@NonNull UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new SubscriptionException("Cannot disable an ADMIN account");
        }

        if (!user.isEnabled()) {
            throw new SubscriptionException("User is already disabled");
        }

        user.setEnabled(false);
        userRepository.save(user);

        String plan = subscriptionRepository
            .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
            .map(sub -> sub.getPlan().name())
            .orElse("NONE");

        return AdminUserResponse.from(user, plan);
    }
}
