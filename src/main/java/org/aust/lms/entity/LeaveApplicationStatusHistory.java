package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApprovalRole;

import java.time.*;

@Entity
@Table(name = "leave_application_status_history")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_history_id")
    private LeaveApplicationHistory applicationHistory;

    private Instant actionTakenOn;

    @Enumerated(EnumType.STRING)
    private LeaveApprovalRole actionTakenBy;

    @Column(length = 200)
    private String comment;

    @Enumerated(EnumType.STRING)
    private LeaveActionStatus actionStatus;

    public Long getId() {
        return id;
    }

    public LeaveApplicationHistory getApplicationHistory() {
        return applicationHistory;
    }

    public Instant getActionTakenOn() {
        return actionTakenOn;
    }

    public LeaveApprovalRole getActionTakenBy() {
        return actionTakenBy;
    }

    public String getComment() {
        return comment;
    }

    public LeaveActionStatus getActionStatus() {
        return actionStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApplicationHistory(LeaveApplicationHistory applicationHistory) {
        this.applicationHistory = applicationHistory;
    }

    public void setActionTakenOn(Instant actionTakenOn) {
        this.actionTakenOn = actionTakenOn;
    }

    public void setActionTakenBy(LeaveApprovalRole actionTakenBy) {
        this.actionTakenBy = actionTakenBy;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setActionStatus(LeaveActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }
}
