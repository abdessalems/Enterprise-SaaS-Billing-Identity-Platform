package com.saas.controller;

import com.saas.dto.ApiResponse;
import com.saas.dto.subscription.SubscriptionResponse;
import com.saas.dto.subscription.UpgradeRequest;
import com.saas.entity.User;
import com.saas.exception.ErrorResponse;
import com.saas.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "Subscription management — requires JWT")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    @Operation(summary = "Get current subscription")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No active subscription",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrentSubscription(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Subscription retrieved", subscriptionService.getCurrentSubscription(user))
        );
    }

    @PostMapping("/upgrade")
    @Operation(summary = "Upgrade subscription plan",
        description = "Upgrade to PRO (30 days) or ENTERPRISE (365 days). Cannot downgrade or re-subscribe to same plan.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription upgraded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid upgrade — same plan or downgrade attempt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SubscriptionResponse>> upgrade(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpgradeRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Subscription upgraded successfully", subscriptionService.upgrade(user, request))
        );
    }
}
