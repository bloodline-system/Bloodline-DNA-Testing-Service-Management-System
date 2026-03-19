package com.dna_testing_system.dev.enums;

public enum SampleType {
    BLOOD("Blood"),
    URINE("Urine"),
    SALIVA("Saliva"),
    TISSUE("Tissue"),
    HAIR("Hair"),
    SEMEN("Semen"),
    SWAB("Swab"),
    NAIL("Nail"),
    OTHER("Other");
    
    private final String displayName;
    
    SampleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
