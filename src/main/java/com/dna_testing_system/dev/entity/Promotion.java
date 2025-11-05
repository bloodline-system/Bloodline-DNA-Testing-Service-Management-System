package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.PromotionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id", nullable = false)
    Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "promotion_code", nullable = false, length = 50)
    String promotionCode;

    @Size(max = 255)
    @NotNull
    @Column(name = "promotion_name", nullable = false)
    String promotionName;

    @Size(max = 1000)
    @Column(name = "promotion_description", length = 1000)
    String promotionDescription;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "promotion_type", nullable = false, length = 50)
    PromotionType promotionType;

    @NotNull
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    BigDecimal discountValue;

    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount_amount", precision = 10, scale = 2)
    BigDecimal maximumDiscountAmount;

    @Column(name = "usage_limit_per_customer")
    Integer usageLimitPerCustomer;

    @Column(name = "total_usage_limit")
    Integer totalUsageLimit;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "current_usage_count", nullable = false)
    Integer currentUsageCount = 0;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @NotNull
    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    Set<Payment> payments = new HashSet<>();
}