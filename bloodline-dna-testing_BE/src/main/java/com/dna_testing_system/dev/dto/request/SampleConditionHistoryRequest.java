package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.SampleConditionStatus;

public class SampleConditionHistoryRequest {
    private SampleConditionStatus status;
    private String conditionNote;

    public SampleConditionStatus getStatus() { return status; }
    public void setStatus(SampleConditionStatus status) { this.status = status; }

    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String conditionNote) { this.conditionNote = conditionNote; }
}
