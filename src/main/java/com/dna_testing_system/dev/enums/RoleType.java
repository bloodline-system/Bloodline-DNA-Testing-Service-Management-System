package com.dna_testing_system.dev.enums;

public enum RoleType {
    ADMIN("Full system access with administrative privileges."),
    MANAGER("Responsible for managing teams and system operations."),
    STAFF("Operates system functionalities assigned by managers."),
    CUSTOMER("Registered user who can use services provided by the system."),
    GUEST("Unregistered user with limited system access.");

    private final String description;

    RoleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

