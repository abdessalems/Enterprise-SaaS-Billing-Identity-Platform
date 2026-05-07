package com.saas.aspect;

import com.saas.annotation.RequiresPlan;
import com.saas.entity.*;
import com.saas.exception.PlanAccessException;
import com.saas.repository.SubscriptionRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanAccessAspectTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private RequiresPlan requiresPlan;

    @InjectMocks private PlanAccessAspect aspect;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .password("encoded")
            .role(Role.USER)
            .build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Subscription subscriptionWithPlan(Plan plan) {
        return Subscription.builder()
            .id(UUID.randomUUID())
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .build();
    }

    @Test
    void checkPlanAccess_proceeds_whenUserPlanMeetsMinimum() throws Throwable {
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(subscriptionWithPlan(Plan.PRO)));
        given(requiresPlan.minimumPlan()).willReturn(Plan.PRO);
        given(joinPoint.proceed()).willReturn("ok");

        Object result = aspect.checkPlanAccess(joinPoint, requiresPlan);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void checkPlanAccess_proceeds_whenUserPlanExceedsMinimum() throws Throwable {
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(subscriptionWithPlan(Plan.ENTERPRISE)));
        given(requiresPlan.minimumPlan()).willReturn(Plan.PRO);
        given(joinPoint.proceed()).willReturn("ok");

        aspect.checkPlanAccess(joinPoint, requiresPlan);

        verify(joinPoint).proceed();
    }

    @Test
    void checkPlanAccess_throws_whenUserPlanIsBelowMinimum() {
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(subscriptionWithPlan(Plan.FREE)));
        given(requiresPlan.minimumPlan()).willReturn(Plan.PRO);

        assertThatThrownBy(() -> aspect.checkPlanAccess(joinPoint, requiresPlan))
            .isInstanceOf(PlanAccessException.class)
            .hasMessageContaining("FREE")
            .hasMessageContaining("PRO");
    }

    @Test
    void checkPlanAccess_throws_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> aspect.checkPlanAccess(joinPoint, requiresPlan))
            .isInstanceOf(PlanAccessException.class)
            .hasMessageContaining("Authentication required");
    }

    @Test
    void checkPlanAccess_throws_whenNoActiveSubscription() {
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> aspect.checkPlanAccess(joinPoint, requiresPlan))
            .isInstanceOf(PlanAccessException.class)
            .hasMessageContaining("No active subscription");
    }

    @Test
    void checkPlanAccess_freePlanAllowsAccessToFreeEndpoints() throws Throwable {
        given(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
            .willReturn(Optional.of(subscriptionWithPlan(Plan.FREE)));
        given(requiresPlan.minimumPlan()).willReturn(Plan.FREE);
        given(joinPoint.proceed()).willReturn("ok");

        aspect.checkPlanAccess(joinPoint, requiresPlan);

        verify(joinPoint).proceed();
    }
}
