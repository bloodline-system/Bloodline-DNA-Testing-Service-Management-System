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
public class StaffAssignmentRequest {
    
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;
    
    @NotBlank(message = "Collect staff ID cannot be blank")
    private String collectStaffId;
    
    @NotBlank(message = "Analysis staff ID cannot be blank")
    private String analysisStaffId;
    
    private String assignmentType;
    
    private String notes;
}
