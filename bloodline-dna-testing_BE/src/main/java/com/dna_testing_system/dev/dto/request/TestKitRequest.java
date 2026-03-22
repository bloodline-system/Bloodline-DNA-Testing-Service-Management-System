package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.KitType;
import com.dna_testing_system.dev.enums.SampleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestKitRequest {
    
    @NotBlank(message = "Kit name is required")
    @Size(max = 255, message = "Kit name must not exceed 255 characters")
    String kitName;
    
    @NotNull(message = "Kit type is required")
    KitType kitType;
    
    @NotNull(message = "Sample type is required")
    SampleType sampleType;
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be greater than or equal to 0")
    @Digits(integer = 8, fraction = 2, message = "Base price must be a valid monetary amount")
    BigDecimal basePrice;
    
    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Current price must be greater than or equal to 0")
    @Digits(integer = 8, fraction = 2, message = "Current price must be a valid monetary amount")
    BigDecimal currentPrice;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    Integer quantityInStock;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String kitDescription;
    
    @Future(message = "Expiry date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate expiryDate;
    
    @NotBlank(message = "Producer is required")
    @Size(max = 255, message = "Producer name must not exceed 255 characters")
    String producedBy;
    
    Boolean isAvailable = true;
}
