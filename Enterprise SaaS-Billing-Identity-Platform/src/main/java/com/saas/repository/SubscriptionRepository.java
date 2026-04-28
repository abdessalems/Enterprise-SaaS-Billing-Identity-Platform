package com.saas.repository;

import com.saas.entity.Subscription;
import com.saas.entity.SubscriptionStatus;
import com.saas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);

    boolean existsByUserAndStatus(User user, SubscriptionStatus status);
}
