package com.saas.integration;

import com.saas.entity.*;
import com.saas.repository.SubscriptionRepository;
import com.saas.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SubscriptionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private UserRepository userRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    private User savedUser(String email) {
        return userRepository.saveAndFlush(User.builder()
            .firstName("Test")
            .lastName("User")
            .email(email)
            .password("encoded")
            .role(Role.USER)
            .build());
    }

    private Subscription activeSubscription(User user, Plan plan, LocalDate endDate) {
        return Subscription.builder()
            .user(user)
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(endDate)
            .build();
    }

    @Test
    void findByUserAndStatus_returnsSubscription_whenActive() {
        User user = savedUser("findactive@test.com");
        subscriptionRepository.saveAndFlush(
            activeSubscription(user, Plan.PRO, LocalDate.now().plusDays(30)));

        Optional<Subscription> found =
            subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE);

        assertThat(found).isPresent();
        assertThat(found.get().getPlan()).isEqualTo(Plan.PRO);
    }

    @Test
    void findByUserAndStatus_returnsEmpty_whenNoSubscription() {
        User user = savedUser("nosubscription@test.com");

        Optional<Subscription> found =
            subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE);

        assertThat(found).isEmpty();
    }

    @Test
    void markExpiredSubscriptions_updatesExpiredRows_andReturnsCount() {
        User user = savedUser("expired@test.com");
        Subscription expired = subscriptionRepository.saveAndFlush(
            activeSubscription(user, Plan.PRO, LocalDate.now().minusDays(1)));

        int updated = subscriptionRepository.markExpiredSubscriptions(
            SubscriptionStatus.EXPIRED,
            SubscriptionStatus.ACTIVE,
            LocalDate.now()
        );

        assertThat(updated).isGreaterThanOrEqualTo(1);

        Subscription reloaded = subscriptionRepository.findById(expired.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void markExpiredSubscriptions_doesNotAffectActiveNonExpiredRows() {
        User user = savedUser("stillactive@test.com");
        Subscription active = subscriptionRepository.saveAndFlush(
            activeSubscription(user, Plan.PRO, LocalDate.now().plusDays(30)));

        subscriptionRepository.markExpiredSubscriptions(
            SubscriptionStatus.EXPIRED,
            SubscriptionStatus.ACTIVE,
            LocalDate.now()
        );

        Subscription reloaded = subscriptionRepository.findById(active.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void existsByUserAndStatus_returnsTrue_whenActive() {
        User user = savedUser("existsactive@test.com");
        subscriptionRepository.saveAndFlush(
            activeSubscription(user, Plan.FREE, LocalDate.now().plusYears(100)));

        assertThat(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .isTrue();
    }

    @Test
    void existsByUserAndStatus_returnsFalse_whenNoSubscription() {
        User user = savedUser("existsnone@test.com");

        assertThat(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .isFalse();
    }
}
