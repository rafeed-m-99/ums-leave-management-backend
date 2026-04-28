package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_application")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @OneToMany(mappedBy = "leaveApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveAttachment> attachments = new ArrayList<>();

    private Instant appliedOn;

    private Instant createdOn;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveApplicationHistory> history = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public List<LeaveAttachment> getAttachments() {
        return attachments;
    }

    public Instant getAppliedOn() {
        return appliedOn;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public List<LeaveApplicationHistory> getHistory() {
        return history;
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

    public void setAttachments(List<LeaveAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setAppliedOn(Instant appliedOn) {
        this.appliedOn = appliedOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    public void addHistory(LeaveApplicationHistory h) {
        history.add(h);
        h.setApplication(this);
    }

    public void addAttachment(LeaveAttachment attachment) {
        attachments.add(attachment);
        attachment.setLeaveApplication(this);
    }
}
