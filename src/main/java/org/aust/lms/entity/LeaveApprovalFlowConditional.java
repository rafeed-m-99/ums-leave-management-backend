package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.enums.SpecialLeaveCondition;

@Entity
@Table(name = "leave_approval_flow_conditional")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveApprovalFlowConditional {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    private LeaveActionRole approvalRole;

    private Boolean isFinalStep;

    @Enumerated(EnumType.STRING)
    private SpecialLeaveCondition condition;

    @ManyToOne
    @JoinColumn(name = "employee_designation_id")
    private EmployeeDesignation employeeDesignation;

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

    public SpecialLeaveCondition getCondition() {
        return condition;
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

    public void setCondition(SpecialLeaveCondition condition) {
        this.condition = condition;
    }

    public void setEmployeeDesignation(EmployeeDesignation employeeDesignation) {
        this.employeeDesignation = employeeDesignation;
    }
}
