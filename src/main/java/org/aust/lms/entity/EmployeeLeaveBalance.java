package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "emp_leave_balance")
@NoArgsConstructor @AllArgsConstructor @ToString
public class EmployeeLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    private Boolean sandwichLeaveTaken;

    private Integer halfPayToFullPayConverted; // For EL

    private Integer daysLeft;

    private Integer additionalDaysStored; // For EL

    private LocalDate createdOn;

    private LocalDate updatedOn;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public Boolean getSandwichLeaveTaken() {
        return sandwichLeaveTaken;
    }

    public Integer getHalfPayToFullPayConverted() {
        return halfPayToFullPayConverted;
    }

    public Integer getDaysLeft() {
        return daysLeft;
    }

    public Integer getAdditionalDaysStored() {
        return additionalDaysStored;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public LocalDate getUpdatedOn() {
        return updatedOn;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public void setSandwichLeaveTaken(Boolean sandwichLeaveTaken) {
        this.sandwichLeaveTaken = sandwichLeaveTaken;
    }

    public void setHalfPayToFullPayConverted(Integer halfPayToFullPayConverted) {
        this.halfPayToFullPayConverted = halfPayToFullPayConverted;
    }

    public void setDaysLeft(Integer daysLeft) {
        this.daysLeft = daysLeft;
    }

    public void setAdditionalDaysStored(Integer additionalDaysStored) {
        this.additionalDaysStored = additionalDaysStored;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public void setUpdatedOn(LocalDate updatedOn) {
        this.updatedOn = updatedOn;
    }
}
