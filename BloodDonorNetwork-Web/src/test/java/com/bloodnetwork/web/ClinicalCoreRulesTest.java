package com.bloodnetwork.web;

import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.web.service.RequestWorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying clinical compatibility tables, 90-day donor eligibility,
 * and request state machine lifecycle transitions.
 */
public class ClinicalCoreRulesTest {

    private boolean isCompatible(String donorGroup, String recipientGroup) {
        return CompatibilityTable.donorGroupsFor(recipientGroup).contains(donorGroup);
    }

    @Test
    @DisplayName("Hemolytic Compatibility: O- is universal red cell donor and receives only O-")
    void testONegativeCompatibility() {
        List<String> donorsForONeg = CompatibilityTable.donorGroupsFor("O-");
        assertEquals(List.of("O-"), donorsForONeg, "O- recipients can only safely receive O- red cells.");

        assertTrue(isCompatible("O-", "O-"));
        assertTrue(isCompatible("O-", "O+"));
        assertTrue(isCompatible("O-", "A+"));
        assertTrue(isCompatible("O-", "AB+"));
    }

    @Test
    @DisplayName("Hemolytic Compatibility: AB+ is universal recipient receiving all 8 groups")
    void testABPositiveCompatibility() {
        List<String> donorsForABPos = CompatibilityTable.donorGroupsFor("AB+");
        assertEquals(8, donorsForABPos.size(), "AB+ can receive red cells from all 8 blood groups.");
        assertTrue(donorsForABPos.containsAll(List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")));
    }

    @Test
    @DisplayName("Hemolytic Compatibility: A+ cannot safely receive B+ or AB+")
    void testIncompatibleTransfusions() {
        assertFalse(isCompatible("B+", "A+"), "B+ blood into A+ patient triggers agglutination.");
        assertFalse(isCompatible("AB+", "A+"), "AB+ blood into A+ patient triggers agglutination.");
    }

    @Test
    @DisplayName("Donor Eligibility: Age boundary enforcement (18 to 65)")
    void testDonorAgeEligibility() {
        Donor underage = new Donor("Young Donor", 17, "M", "O+", "9876543210", "Chennai", null, true);
        assertFalse(underage.isEligible(90), "Donors under 18 must be clinically ineligible.");

        Donor overage = new Donor("Elder Donor", 66, "M", "O+", "9876543210", "Chennai", null, true);
        assertFalse(overage.isEligible(90), "Donors over 65 must be clinically ineligible.");

        Donor validAge = new Donor("Adult Donor", 25, "M", "O+", "9876543210", "Chennai", null, true);
        assertTrue(validAge.isEligible(90), "Donors between 18 and 65 with no prior donations are eligible.");
    }

    @Test
    @DisplayName("Donor Eligibility: 90-day hematopoiesis recovery window")
    void testDonorRecoveryWindow() {
        // Donated 45 days ago -> in recovery
        Donor recentDonor = new Donor("Recent Donor", 30, "M", "A+", "9876543210", "Chennai", LocalDate.now().minusDays(45), true);
        assertFalse(recentDonor.isEligible(90), "Donors within 90 days of last donation are in recovery.");

        // Donated 95 days ago -> eligible
        Donor recoveredDonor = new Donor("Recovered Donor", 30, "M", "A+", "9876543210", "Chennai", LocalDate.now().minusDays(95), true);
        assertTrue(recoveredDonor.isEligible(90), "Donors past 90 days from last donation are clinically eligible.");
    }

    @Test
    @DisplayName("Donor Eligibility: Availability flag enforcement")
    void testDonorAvailability() {
        Donor unavailable = new Donor("Unavailable Donor", 30, "M", "O+", "9876543210", "Chennai", null, false);
        assertFalse(unavailable.isEligible(90), "Donors marked unavailable cannot be allocated.");
    }

    @Test
    @DisplayName("State Machine: PENDING transitions to MATCHED, FULFILLED, or CANCELLED")
    void testPendingTransitions() {
        RequestWorkflowService workflow = new RequestWorkflowService(null);

        assertTrue(workflow.isTransitionAllowed(RequestStatus.PENDING, RequestStatus.MATCHED));
        assertTrue(workflow.isTransitionAllowed(RequestStatus.PENDING, RequestStatus.FULFILLED));
        assertTrue(workflow.isTransitionAllowed(RequestStatus.PENDING, RequestStatus.CANCELLED));
    }

    @Test
    @DisplayName("State Machine: FULFILLED and CANCELLED are terminal and immutable")
    void testTerminalTransitions() {
        RequestWorkflowService workflow = new RequestWorkflowService(null);

        assertFalse(workflow.isTransitionAllowed(RequestStatus.FULFILLED, RequestStatus.PENDING));
        assertFalse(workflow.isTransitionAllowed(RequestStatus.FULFILLED, RequestStatus.CANCELLED));
        assertFalse(workflow.isTransitionAllowed(RequestStatus.CANCELLED, RequestStatus.PENDING));
        assertFalse(workflow.isTransitionAllowed(RequestStatus.CANCELLED, RequestStatus.FULFILLED));
    }
}
