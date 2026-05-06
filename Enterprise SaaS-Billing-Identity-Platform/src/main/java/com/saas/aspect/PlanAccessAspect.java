package com.saas.aspect;

import com.saas.annotation.RequiresPlan;
import com.saas.entity.Subscription;
import com.saas.entity.SubscriptionStatus;
import com.saas.entity.User;
import com.saas.exception.PlanAccessException;
import com.saas.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanAccessAspect {

    private final SubscriptionRepository subscriptionRepository;

    @Around("@annotation(requiresPlan)")
    public Object checkPlanAccess(ProceedingJoinPoint joinPoint, RequiresPlan requiresPlan) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new PlanAccessException("Authentication required");
        }

        Subscription subscription = subscriptionRepository
            .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new PlanAccessException("No active subscription found"));

        if (subscription.getPlan().ordinal() < requiresPlan.minimumPlan().ordinal()) {
            log.warn("Plan access denied for user={} plan={} required={}",
                user.getEmail(), subscription.getPlan(), requiresPlan.minimumPlan());
            throw new PlanAccessException(
                "Your current plan (" + subscription.getPlan() + ") does not have access to this feature. " +
                "Required: " + requiresPlan.minimumPlan() + " or higher."
            );
        }

        return joinPoint.proceed();
    }
}
