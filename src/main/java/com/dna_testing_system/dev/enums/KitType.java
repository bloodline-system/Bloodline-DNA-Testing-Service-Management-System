package com.dna_testing_system.dev.enums;

public enum KitType {
    PATERNITY("Paternity Test"),
    MATERNITY("Maternity Test"),
    SIBLING("Sibling Test"),
    GRANDPARENT("Grandparent Test"),
    ANCESTRY("Ancestry Test"),
    RELATIONSHIP("Relationship Test"),
    FORENSIC("Forensic Test"),
    IMMIGRATION("Immigration Test"),
    PRENATAL("Prenatal Test"),
    TWIN_ZYGOSITY("Twin Zygosity Test"),
    GENETIC_HEALTH("Genetic Health Test"),
    CARRIER_SCREENING("Carrier Screening"),
    PHARMACOGENOMICS("Pharmacogenomics Test"),
    OTHER("Other");
    
    private final String displayName;
    
    KitType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
