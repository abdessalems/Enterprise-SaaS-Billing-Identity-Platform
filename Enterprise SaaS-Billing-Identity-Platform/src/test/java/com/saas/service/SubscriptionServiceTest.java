package com.saas.service;

import com.saas.dto.subscription.SubscriptionResponse;
import com.saas.dto.subscription.UpgradeRequest;
import com.saas.entity.*;
import com.saas.exception.SubscriptionException;
import com.saas.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @InjectMocks private SubscriptionService subscriptionService;

    private User buildUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .password("encoded")
            .role(Role.USER)
            .build();
    }

    private Subscription activeSubscription(Plan plan) {
        return Subscription.builder()
            .id(UUID.randomUUID())
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusYears(100))
            .build();
    }

    // ─── createFreeSubscription ───────────────────────────────────────────────

    @Test
    void createFreeSubscription_savesActiveFreeSubscription() {
        User user = buildUser();
        given(subscriptionRepository.save(any(Subscription.class)))
            .willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.createFreeSubscription(user);

        assertThat(response.getPlan()).isEqualTo("FREE");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createFreeSubscription_setsEndDateFarInFuture() {
        User user = buildUser();
        given(subscriptionRepository.save(any(Subscription.class)))
            .willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.createFreeSubscription(user);

        assertThat(response.getEndDate()).isAfter(LocalDate.now().plusYears(99));
    }

    // ─── upgrade ──────────────────────────────────────────────────────────────

    @Test
    void upgrade_fromFreeToPro_updatesEndDateTo30Days() {
        User user = buildUser();
        Subscription current = activeSubscription(Plan.FREE);
        UpgradeRequest req = new UpgradeRequest();
        req.setPlan(Plan.PRO);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(current));
        given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.upgrade(user, req);

        assertThat(response.getPlan()).isEqualTo("PRO");
        assertThat(current.getEndDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void upgrade_fromFreeToEnterprise_updatesEndDateTo365Days() {
        User user = buildUser();
        Subscription current = activeSubscription(Plan.FREE);
        UpgradeRequest req = new UpgradeRequest();
        req.setPlan(Plan.ENTERPRISE);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(current));
        given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        subscriptionService.upgrade(user, req);

        assertThat(current.getEndDate()).isEqualTo(LocalDate.now().plusDays(365));
    }

    @Test
    void upgrade_throwsSubscriptionException_whenSamePlan() {
        User user = buildUser();
        Subscription current = activeSubscription(Plan.PRO);
        UpgradeRequest req = new UpgradeRequest();
        req.setPlan(Plan.PRO);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(current));

        assertThatThrownBy(() -> subscriptionService.upgrade(user, req))
            .isInstanceOf(SubscriptionException.class)
            .hasMessageContaining("Already subscribed");
    }

    @Test
    void upgrade_throwsSubscriptionException_whenDowngrading() {
        User user = buildUser();
        Subscription current = activeSubscription(Plan.ENTERPRISE);
        UpgradeRequest req = new UpgradeRequest();
        req.setPlan(Plan.PRO);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(current));

        assertThatThrownBy(() -> subscriptionService.upgrade(user, req))
            .isInstanceOf(SubscriptionException.class)
            .hasMessageContaining("Cannot downgrade");
    }

    @Test
    void upgrade_throwsSubscriptionException_whenNoActiveSubscription() {
        User user = buildUser();
        UpgradeRequest req = new UpgradeRequest();
        req.setPlan(Plan.PRO);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.upgrade(user, req))
            .isInstanceOf(SubscriptionException.class)
            .hasMessageContaining("No active subscription");
    }

    // ─── getCurrentSubscription ───────────────────────────────────────────────

    @Test
    void getCurrentSubscription_returnsActiveSubscription() {
        User user = buildUser();
        Subscription sub = activeSubscription(Plan.PRO);

        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(sub));

        SubscriptionResponse response = subscriptionService.getCurrentSubscription(user);

        assertThat(response.getPlan()).isEqualTo("PRO");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getCurrentSubscription_throwsWhenNoActive() {
        User user = buildUser();
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getCurrentSubscription(user))
            .isInstanceOf(SubscriptionException.class)
            .hasMessageContaining("No active subscription");
    }
}
