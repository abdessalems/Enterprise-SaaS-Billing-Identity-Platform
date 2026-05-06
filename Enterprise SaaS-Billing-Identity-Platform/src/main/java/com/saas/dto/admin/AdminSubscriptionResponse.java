package com.saas.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.saas.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriptionResponse {

    private UUID id;
    private String plan;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private UUID userId;
    private String userEmail;
    private String userFullName;

    public static AdminSubscriptionResponse from(Subscription sub) {
        return AdminSubscriptionResponse.builder()
            .id(sub.getId())
            .plan(sub.getPlan().name())
            .status(sub.getStatus().name())
            .startDate(sub.getStartDate())
            .endDate(sub.getEndDate())
            .createdAt(sub.getCreatedAt())
            .userId(sub.getUser().getId())
            .userEmail(sub.getUser().getEmail())
            .userFullName(sub.getUser().getFirstName() + " " + sub.getUser().getLastName())
            .build();
    }
}
