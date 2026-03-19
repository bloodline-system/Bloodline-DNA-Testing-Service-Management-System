package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.CollectionStatus;
import com.dna_testing_system.dev.enums.SampleQuality;
import com.dna_testing_system.dev.enums.SampleType;

import java.time.LocalDateTime;

public class SampleCollectionRequest {
    private Long orderId;
    private Long staffId;
    private LocalDateTime collectionDate;
    private SampleQuality sampleQuality;
    private CollectionStatus collectionStatus;
    private SampleType sampleType;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public LocalDateTime getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDateTime collectionDate) { this.collectionDate = collectionDate; }

    public SampleQuality getSampleQuality() { return sampleQuality; }
    public void setSampleQuality(SampleQuality sampleQuality) { this.sampleQuality = sampleQuality; }

    public CollectionStatus getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(CollectionStatus collectionStatus) { this.collectionStatus = collectionStatus; }

    public SampleType getSampleType() { return sampleType; }
    public void setSampleType(SampleType sampleType) { this.sampleType = sampleType; }
}
