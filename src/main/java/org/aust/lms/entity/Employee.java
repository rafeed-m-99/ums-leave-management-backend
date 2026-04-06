package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@NoArgsConstructor @AllArgsConstructor @ToString
public class Employee {

    @Id
    @Column(name = "employee_id", length = 15)
    private String employeeId;

    @ManyToOne
    @JoinColumn(name = "designation_designation_id")
    private EmployeeDesignation designation;

    private String deptOffice;

    private LocalDate joiningDate;

    private Long status;

    private String lastModified;

    private String shortName;

    private Long employeeType;

    private String createdBy;

    private LocalDate createdOn;

    private String updatedBy;

    private LocalDate updatedOn;

    private Long createRequestId;

    private Long employmentType;

    private Long joiningInterval;

    private Long grade;

    public String getEmployeeId() {
        return employeeId;
    }

    public EmployeeDesignation getDesignation() {
        return designation;
    }

    public String getDeptOffice() {
        return deptOffice;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public Long getStatus() {
        return status;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getShortName() {
        return shortName;
    }

    public Long getEmployeeType() {
        return employeeType;
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

    public Long getCreateRequestId() {
        return createRequestId;
    }

    public Long getEmploymentType() {
        return employmentType;
    }

    public Long getJoiningInterval() {
        return joiningInterval;
    }

    public Long getGrade() {
        return grade;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setDesignation(EmployeeDesignation designation) {
        this.designation = designation;
    }

    public void setDeptOffice(String deptOffice) {
        this.deptOffice = deptOffice;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setEmployeeType(Long employeeType) {
        this.employeeType = employeeType;
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

    public void setCreateRequestId(Long createRequestId) {
        this.createRequestId = createRequestId;
    }

    public void setEmploymentType(Long employmentType) {
        this.employmentType = employmentType;
    }

    public void setJoiningInterval(Long joiningInterval) {
        this.joiningInterval = joiningInterval;
    }

    public void setGrade(Long grade) {
        this.grade = grade;
    }
}
