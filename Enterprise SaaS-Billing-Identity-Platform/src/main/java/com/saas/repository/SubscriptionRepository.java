package com.saas.repository;

import com.saas.entity.Subscription;
import com.saas.entity.SubscriptionStatus;
import com.saas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);

    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Subscription s SET s.status = :newStatus WHERE s.status = :currentStatus AND s.endDate < :today")
    int markExpiredSubscriptions(
        @Param("newStatus") SubscriptionStatus newStatus,
        @Param("currentStatus") SubscriptionStatus currentStatus,
        @Param("today") LocalDate today
    );
}
