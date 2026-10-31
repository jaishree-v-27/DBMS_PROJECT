package com.bloodnetwork.matching;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates medical red blood cell compatibility rules for blood transfusions.
 * Maps each recipient (patient) blood group to the list of compatible donor blood groups.
 */
public class CompatibilityTable {

    private static final Map<String, List<String>> COMPATIBILITY_MAP = new HashMap<>();

    static {
        // A+ can receive from A+, A-, O+, O-
        COMPATIBILITY_MAP.put("A+", Collections.unmodifiableList(Arrays.asList("A+", "A-", "O+", "O-")));

        // A- can receive from A-, O-
        COMPATIBILITY_MAP.put("A-", Collections.unmodifiableList(Arrays.asList("A-", "O-")));

        // B+ can receive from B+, B-, O+, O-
        COMPATIBILITY_MAP.put("B+", Collections.unmodifiableList(Arrays.asList("B+", "B-", "O+", "O-")));

        // B- can receive from B-, O-
        COMPATIBILITY_MAP.put("B-", Collections.unmodifiableList(Arrays.asList("B-", "O-")));

        // AB+ (Universal recipient) can receive from all 8 blood groups
        COMPATIBILITY_MAP.put("AB+", Collections.unmodifiableList(Arrays.asList(
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
        )));

        // AB- can receive from A-, B-, AB-, O-
        COMPATIBILITY_MAP.put("AB-", Collections.unmodifiableList(Arrays.asList("A-", "B-", "AB-", "O-")));

        // O+ can receive from O+, O-
        COMPATIBILITY_MAP.put("O+", Collections.unmodifiableList(Arrays.asList("O+", "O-")));

        // O- (Universal donor only) can only receive from O-
        COMPATIBILITY_MAP.put("O-", Collections.unmodifiableList(Arrays.asList("O-")));
    }

    private CompatibilityTable() {}

    /**
     * Returns the list of donor blood groups compatible with the given patient blood group.
     *
     * @param patientGroup The blood group needed by the patient.
     * @return List of compatible donor blood groups (or empty list if unknown).
     */
    public static List<String> donorGroupsFor(String patientGroup) {
        if (patientGroup == null) {
            return Collections.emptyList();
        }
        String key = patientGroup.trim().toUpperCase();
        List<String> compatible = COMPATIBILITY_MAP.get(key);
        return compatible != null ? compatible : Collections.emptyList();
    }
}
