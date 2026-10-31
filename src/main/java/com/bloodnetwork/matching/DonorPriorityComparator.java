package com.bloodnetwork.matching;

import com.bloodnetwork.model.Donor;
import java.util.Comparator;

/**
 * Comparator to prioritize eligible donors for a blood request according to clinical best practices:
 * 1. Exact blood-group match first (prioritize identical match over compatible alternatives).
 * 2. Donors who have never donated before (first-time donors).
 * 3. Donors with the longest time elapsed since their last donation (earliest lastDonationDate first).
 */
public class DonorPriorityComparator implements Comparator<Donor> {

    private final String requestedBloodGroup;

    /**
     * Constructs a DonorPriorityComparator for the requested blood group.
     *
     * @param requestedBloodGroup The target blood group requested by the hospital.
     */
    public DonorPriorityComparator(String requestedBloodGroup) {
        this.requestedBloodGroup = requestedBloodGroup != null ? requestedBloodGroup.trim().toUpperCase() : "";
    }

    @Override
    public int compare(Donor d1, Donor d2) {
        // 1. Exact blood-group match first
        boolean exact1 = d1.getBloodGroup().equalsIgnoreCase(requestedBloodGroup);
        boolean exact2 = d2.getBloodGroup().equalsIgnoreCase(requestedBloodGroup);
        if (exact1 != exact2) {
            return exact1 ? -1 : 1;
        }

        // 2. Donors who never donated first (lastDonationDate is null)
        boolean never1 = (d1.getLastDonationDate() == null);
        boolean never2 = (d2.getLastDonationDate() == null);
        if (never1 != never2) {
            return never1 ? -1 : 1;
        }

        // If both never donated, break tie by name
        if (never1 && never2) {
            return d1.getName().compareToIgnoreCase(d2.getName());
        }

        // 3. Longest time elapsed since last donation (earliest calendar date comes first)
        int dateComp = d1.getLastDonationDate().compareTo(d2.getLastDonationDate());
        if (dateComp != 0) {
            return dateComp;
        }

        // Fallback tie-breaker by donor name
        return d1.getName().compareToIgnoreCase(d2.getName());
    }
}
