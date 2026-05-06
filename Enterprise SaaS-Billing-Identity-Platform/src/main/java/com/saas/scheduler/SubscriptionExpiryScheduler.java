package com.saas.scheduler;

import com.saas.entity.SubscriptionStatus;
import com.saas.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionExpiryScheduler {

    private final SubscriptionRepository subscriptionRepository;

    // runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        int expired = subscriptionRepository.markExpiredSubscriptions(
            SubscriptionStatus.EXPIRED,
            SubscriptionStatus.ACTIVE,
            LocalDate.now()
        );

        if (expired > 0) {
            log.info("Subscription expiry job: {} subscription(s) marked as EXPIRED", expired);
        }
    }
}
