package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "emp_designation")
@NoArgsConstructor @AllArgsConstructor @ToString
public class EmployeeDesignation {

    @Id
    @Column(name = "designation_id")
    private Long designationId;

    private String designationName;

    private Long employeeType;

    private Long viewOrder;

    private Long status;

    private String createdBy;

    private LocalDate createdOn;

    private String updatedBy;

    private LocalDate updatedOn;

    private String lastModified;

    @OneToMany(mappedBy = "employeeDesignation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveApprovalFlow> leaveApprovalFlowList;

    @OneToMany(mappedBy = "employeeDesignation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveApprovalFlowConditional> leaveApprovalFlowConditionalList;

    public Long getDesignationId() {
        return designationId;
    }

    public String getDesignationName() {
        return designationName;
    }

    public Long getEmployeeType() {
        return employeeType;
    }

    public Long getViewOrder() {
        return viewOrder;
    }

    public Long getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public LocalDate getUpdatedOn() {
        return updatedOn;
    }

    public String getLastModified() {
        return lastModified;
    }

    public List<LeaveApprovalFlow> getLeaveApprovalFlowList() {
        return leaveApprovalFlowList;
    }

    public List<LeaveApprovalFlowConditional> getLeaveApprovalFlowConditionalList() {
        return leaveApprovalFlowConditionalList;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
    }

    public void setDesignationName(String designationName) {
        this.designationName = designationName;
    }

    public void setEmployeeType(Long employeeType) {
        this.employeeType = employeeType;
    }

    public void setViewOrder(Long viewOrder) {
        this.viewOrder = viewOrder;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedOn(LocalDate updatedOn) {
        this.updatedOn = updatedOn;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
