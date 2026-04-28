package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.LeaveActionRole;

@Entity
@Table(name = "leave_approval_flow")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveApprovalFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    private LeaveActionRole approvalRole;

    private Boolean isFinalStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_designation_id")
    private EmployeeDesignation employeeDesignation;

    @Column(name = "special_role")
    private String specialRole;

    public Long getId() {
        return id;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public LeaveActionRole getApprovalRole() {
        return approvalRole;
    }

    public Boolean getFinalStep() {
        return isFinalStep;
    }

    public String getSpecialRole() {
        return specialRole;
    }

    public EmployeeDesignation getEmployeeDesignation() {
        return employeeDesignation;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public void setApprovalRole(LeaveActionRole approvalRole) {
        this.approvalRole = approvalRole;
    }

    public void setFinalStep(Boolean finalStep) {
        isFinalStep = finalStep;
    }

    public void setEmployeeDesignation(EmployeeDesignation employeeDesignation) {
        this.employeeDesignation = employeeDesignation;
    }

    public void setSpecialRole(String specialRole) {
        this.specialRole = specialRole;
    }
}
