package com.bloodnetwork.model;

import java.time.LocalDateTime;

/**
 * Represents a blood request raised by a hospital for a patient.
 */
public class BloodRequest {

    private int requestId;
    private int hospitalId;
    private String patientName;
    private String bloodGroup;
    private int unitsNeeded;
    private Urgency urgency;
    private RequestStatus status;
    private LocalDateTime requestDate;

    /**
     * Default constructor.
     */
    public BloodRequest() {
        this.urgency = Urgency.NORMAL;
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    /**
     * Constructor for creating a new blood request (status defaults to PENDING).
     */
    public BloodRequest(int hospitalId, String patientName, String bloodGroup, int unitsNeeded, Urgency urgency) {
        this.hospitalId = hospitalId;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.unitsNeeded = unitsNeeded;
        this.urgency = urgency != null ? urgency : Urgency.NORMAL;
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    /**
     * Full constructor for populating records from the database.
     */
    public BloodRequest(int requestId, int hospitalId, String patientName, String bloodGroup,
                        int unitsNeeded, Urgency urgency, RequestStatus status, LocalDateTime requestDate) {
        this.requestId = requestId;
        this.hospitalId = hospitalId;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.unitsNeeded = unitsNeeded;
        this.urgency = urgency;
        this.status = status;
        this.requestDate = requestDate;
    }

    // Getters and Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnitsNeeded() {
        return unitsNeeded;
    }

    public void setUnitsNeeded(int unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    @Override
    public String toString() {
        return String.format(
                "BloodRequest [ID: %d | Hospital ID: %d | Patient: %-15s | Group: %-3s | Units: %d | Urgency: %-8s | Status: %-9s | Date: %s]",
                requestId,
                hospitalId,
                patientName,
                bloodGroup,
                unitsNeeded,
                urgency,
                status,
                (requestDate != null ? requestDate.toString().replace('T', ' ') : "N/A")
        );
    }
}
