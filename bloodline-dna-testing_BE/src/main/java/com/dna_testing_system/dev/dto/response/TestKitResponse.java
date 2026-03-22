package com.dna_testing_system.dev.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestKitResponse {
    Long id;
    String kitName;
    String kitType;
    String sampleType;
    BigDecimal basePrice;
    BigDecimal currentPrice;
    Integer quantityInStock;
    String kitDescription;
    LocalDate expiryDate;
    String producedBy;
    Boolean isAvailable;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Helper methods for UI display
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
    
    public boolean isLowStock() {
        return quantityInStock != null && quantityInStock <= 10;
    }
    
    public String getAvailabilityStatus() {
        if (isExpired()) {
            return "Expired";
        } else if (!isAvailable) {
            return "Unavailable";
        } else if (isLowStock()) {
            return "Low Stock";
        } else {
            return "Available";
        }
    }
}
