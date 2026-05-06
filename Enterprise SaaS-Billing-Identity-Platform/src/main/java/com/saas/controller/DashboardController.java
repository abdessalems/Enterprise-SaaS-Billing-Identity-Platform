package com.saas.controller;

import com.saas.annotation.RequiresPlan;
import com.saas.dto.ApiResponse;
import com.saas.entity.Plan;
import com.saas.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Plan-gated feature endpoints — requires JWT")
public class DashboardController {

    @GetMapping("/summary")
    @Operation(summary = "Basic summary — FREE and above",
        description = "Available to all active subscribers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved", Map.of(
            "user", user.getEmail(),
            "plan", "FREE",
            "feature", "basic-summary"
        )));
    }

    @GetMapping("/reports")
    @RequiresPlan(minimumPlan = Plan.PRO)
    @Operation(summary = "Advanced reports — PRO and above",
        description = "Requires PRO or ENTERPRISE subscription")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved", Map.of(
            "user", user.getEmail(),
            "plan", "PRO",
            "feature", "advanced-reports"
        )));
    }

    @GetMapping("/analytics")
    @RequiresPlan(minimumPlan = Plan.ENTERPRISE)
    @Operation(summary = "Full analytics — ENTERPRISE only",
        description = "Requires ENTERPRISE subscription")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved", Map.of(
            "user", user.getEmail(),
            "plan", "ENTERPRISE",
            "feature", "full-analytics"
        )));
    }
}
