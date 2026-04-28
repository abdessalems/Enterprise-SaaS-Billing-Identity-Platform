package com.saas.dto.subscription;

import com.saas.entity.Plan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpgradeRequest {

    @NotNull(message = "Plan is required")
    private Plan plan;
}
