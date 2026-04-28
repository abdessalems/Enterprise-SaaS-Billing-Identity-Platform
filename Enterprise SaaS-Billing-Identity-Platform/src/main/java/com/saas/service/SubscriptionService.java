package com.saas.service;

import com.saas.dto.subscription.SubscriptionResponse;
import com.saas.dto.subscription.UpgradeRequest;
import com.saas.entity.Plan;
import com.saas.entity.Subscription;
import com.saas.entity.SubscriptionStatus;
import com.saas.entity.User;
import com.saas.exception.SubscriptionException;
import com.saas.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final int PRO_DURATION_DAYS = 30;
    private static final int ENTERPRISE_DURATION_DAYS = 365;

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResponse createFreeSubscription(User user) {
        Subscription subscription = Subscription.builder()
            .user(user)
            .plan(Plan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusYears(100))
            .build();

        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse upgrade(User user, UpgradeRequest request) {
        Plan newPlan = request.getPlan();

        Subscription current = subscriptionRepository
            .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new SubscriptionException("No active subscription found"));

        if (current.getPlan() == newPlan) {
            throw new SubscriptionException("Already subscribed to plan: " + newPlan);
        }

        if (newPlan.ordinal() <= current.getPlan().ordinal()) {
            throw new SubscriptionException(
                "Cannot downgrade from " + current.getPlan() + " to " + newPlan
            );
        }

        LocalDate now = LocalDate.now();
        current.setPlan(newPlan);
        current.setStartDate(now);
        current.setEndDate(expirationFor(newPlan, now));

        return toResponse(subscriptionRepository.save(current));
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(User user) {
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new SubscriptionException("No active subscription found"));
        return toResponse(sub);
    }

    // ─── private helpers ─────────────────────────────────────────────────────────

    private LocalDate expirationFor(Plan plan, LocalDate from) {
        return switch (plan) {
            case PRO -> from.plusDays(PRO_DURATION_DAYS);
            case ENTERPRISE -> from.plusDays(ENTERPRISE_DURATION_DAYS);
            case FREE -> from.plusYears(100);
        };
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        return SubscriptionResponse.builder()
            .id(sub.getId())
            .plan(sub.getPlan().name())
            .status(sub.getStatus().name())
            .startDate(sub.getStartDate())
            .endDate(sub.getEndDate())
            .createdAt(sub.getCreatedAt())
            .build();
    }
}
