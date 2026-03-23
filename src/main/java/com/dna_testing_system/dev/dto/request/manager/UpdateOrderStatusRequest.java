package com.dna_testing_system.dev.dto.request.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;
    
    @NotBlank(message = "Status cannot be blank")
    private String status;
    
    private String notes;
}
