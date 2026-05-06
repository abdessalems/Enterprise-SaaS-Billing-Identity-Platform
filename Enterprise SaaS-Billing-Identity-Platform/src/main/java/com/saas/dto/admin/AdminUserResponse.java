package com.saas.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saas.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    @JsonProperty("isActive")
    private boolean active;

    private String currentPlan;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user, String currentPlan) {
        return AdminUserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .active(user.isEnabled())
            .currentPlan(currentPlan)
            .createdAt(user.getCreatedAt())
            .build();
    }
}
