package com.dna_testing_system.dev.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "tbl_test_kits")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestKit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kit_id", nullable = false)
    Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "kit_name", nullable = false)
    String kitName;

    @Size(max = 100)
    @NotNull
    @Column(name = "kit_type", nullable = false, length = 100)
    String kitType; //create enum

    @Size(max = 100)
    @NotNull
    @Column(name = "sample_type", nullable = false, length = 100)
    String sampleType; //create enum

    @NotNull
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    BigDecimal basePrice;

    @NotNull
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    BigDecimal currentPrice;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "quantity_in_stock", nullable = false)
    Integer quantityInStock = 0;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    Boolean isAvailable = true;

    @Size(max = 1000)
    @Column(name = "kit_description", length = 1000)
    String kitDescription;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "produced_by")
    String producedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "kit", fetch = FetchType.LAZY)
    Set<OrderKit> orderKits =  new HashSet<>();
}