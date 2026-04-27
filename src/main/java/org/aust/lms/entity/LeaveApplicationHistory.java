package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.LeaveApplicationStage;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_application_history")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private LeaveApplication application;

    @Enumerated(EnumType.STRING)
    private LeaveApplicationStage applicationStage;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Integer totalDays;

    @Column(length = 1000)
    private String reason;

    @Column(name = "ex_bd_leave")
    private Boolean exBangladeshLeave;

    private boolean isSandwichLeave;

    private Integer applicationStep;

    private String nextApprovalRoleId;

    private Instant createdOn;

    @OneToMany(mappedBy = "applicationHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveApplicationStatusHistory> statusHistory = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public LeaveApplication getApplication() {
        return application;
    }

    public LeaveApplicationStage getApplicationStage() {
        return applicationStage;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getExBangladeshLeave() {
        return exBangladeshLeave;
    }

    public boolean isSandwichLeave() {
        return isSandwichLeave;
    }

    public Integer getApplicationStep() {
        return applicationStep;
    }

    public String getNextApprovalRoleId() {
        return nextApprovalRoleId;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public List<LeaveApplicationStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApplication(LeaveApplication application) {
        this.application = application;
    }

    public void setApplicationStage(LeaveApplicationStage applicationStage) {
        this.applicationStage = applicationStage;
    }

    public void setFronDate(LocalDate fronDate) {
        this.fromDate = fronDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setExBangladeshLeave(Boolean exBangladeshLeave) {
        this.exBangladeshLeave = exBangladeshLeave;
    }

    public void setSandwichLeave(boolean sandwichLeave) {
        isSandwichLeave = sandwichLeave;
    }

    public void setApplicationStep(Integer applicationStep) {
        this.applicationStep = applicationStep;
    }

    public void setNextApprovalRoleId(String nextApprovalRoleId) {
        this.nextApprovalRoleId = nextApprovalRoleId;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    public void addStatus(LeaveApplicationStatusHistory s) {
        statusHistory.add(s);
        s.setApplicationHistory(this);
    }
}
