package org.aust.lms.service;

import org.aust.lms.dto.*;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.enums.LeaveApplicationStage;
import org.aust.lms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveApprovalService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationHistoryRepository historyRepo;
    private final LeaveApplicationStatusHistoryRepository statusRepo;
    private final LeaveApprovalFlowRepository flowRepo;
    private final LeaveApprovalFlowConditionalRepository conditionalFlowRepo;
    private final EmployeeLeaveBalanceRepository leaveBalanceRepo;
    private final EmployeeLeaveBalanceHistoryRepository leaveBalanceHistoryRepo;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;

    public LeaveApprovalService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository historyRepo, LeaveApplicationStatusHistoryRepository statusRepo, LeaveApprovalFlowRepository flowRepo, LeaveApprovalFlowConditionalRepository conditionalFlowRepo, EmployeeLeaveBalanceRepository leaveBalanceRepo, EmployeeLeaveBalanceHistoryRepository leaveBalanceHistoryRepo, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.historyRepo = historyRepo;
        this.statusRepo = statusRepo;
        this.flowRepo = flowRepo;
        this.conditionalFlowRepo = conditionalFlowRepo;
        this.leaveBalanceRepo = leaveBalanceRepo;
        this.leaveBalanceHistoryRepo = leaveBalanceHistoryRepo;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.leaveApplicationStatusHistoryRepository = leaveApplicationStatusHistoryRepository;
    }

    public LeaveApplicationDetailsResponse getApplicationDetails(Long applicationId) {

        // ✅ Step 1: Check application exists
        LeaveApplication app = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // ✅ Step 2: Get latest history
        LeaveApplicationHistory history = historyRepo
                .findLatestHistory(applicationId)
                .orElseThrow(() -> new RuntimeException("Application history not found"));

        // ✅ Step 3: Get status history using historyId
        List<StatusHistoryDto> statusHistory =
                statusRepo.findStatusHistoryByHistoryId(history.getId());

        Integer balance = leaveBalanceRepo.findByEmployeeIdAndLeaveType(
                app.getEmployee().getEmployeeId(),
                app.getLeaveType()
        );

        List<AttachmentDto> attachments = app.getAttachments().stream()
                .map(att -> new AttachmentDto(
                        att.getId(),
                        att.getOriginalFileName(),
                        att.getFileType(),
                        att.getDescription()
                ))
                .toList();

        // ✅ Build response
        return new LeaveApplicationDetailsResponse(
                app.getId(),
                app.getEmployee().getEmployeeId(),
                app.getEmployee().getShortName(),
                app.getEmployee().getDesignation().getDesignationName(),
                app.getLeaveType().getName(),
                balance,
                history.getFromDate(),
                history.getToDate(),
                history.getTotalDays(),
                app.getAppliedOn(),
                history.getReason(),
                getActionRoleFromRoleId(history.getNextApprovalRoleId()),
                attachments,
                statusHistory,
                app.getSubstitute() != null ? app.getSubstitute().getSubstituteEmployeeId() : null
        );
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationTimelineGroupResponse> getLeaveTimeline(Long applicationId) {

        LeaveApplication app = leaveApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Application not found"));

        List<LeaveApplicationHistory> histories = leaveApplicationHistoryRepository.findAllHistories(applicationId);

        return histories.stream().map(history -> {

                    List<LeaveApplicationStatusHistory> statuses = leaveApplicationStatusHistoryRepository.findNonSystemStatuses(history.getId());

                    List<LeaveApplicationTimelineResponse> timeline =
                            statuses.stream()
                                    .map(status ->
                                            mapTimeline(
                                                    status.getActionStatus(),
                                                    status.getActionTakenBy(),
                                                    status.getComment(),
                                                    history.getNextApprovalRoleId(),
                                                    LocalDateTime.ofInstant(
                                                            status.getActionTakenOn(),
                                                            ZoneId.systemDefault()
                                                    )
                                            )
                                    )
                                    .toList();

                    return new LeaveApplicationTimelineGroupResponse(
                            formatStage(
                                    history.getApplicationStage()),
                            timeline
                    );
                })
                .toList();
    }

    @Transactional
    public LeaveApprovalResponse processApproval(LeaveApprovalRequest request) {

        System.out.println("Received approval request: " + request);

        // =========================
        // 1. FETCH LATEST HISTORY
        // =========================
        LeaveApplicationHistory history = historyRepo
                .findLatestHistory(request.applicationId())
                .orElse(null);

        if (history == null) {
            return new LeaveApprovalResponse(false, "Application not found");
        }

        LeaveActionStatus action = LeaveActionStatus.valueOf(request.action());

        // =========================
        // 2. HANDLE REJECTION
        // =========================
        if (action == LeaveActionStatus.REJECTED) {

            LeaveApplicationStatusHistory rejectedStatus = new LeaveApplicationStatusHistory();
            rejectedStatus.setApplicationHistory(history);
            rejectedStatus.setActionTakenOn(Instant.now());
            rejectedStatus.setActionTakenBy(LeaveActionRole.valueOf(request.roleId()));
            rejectedStatus.setActionStatus(LeaveActionStatus.REJECTED);
            rejectedStatus.setComment(request.comment());

            statusRepo.save(rejectedStatus);

            return new LeaveApprovalResponse(true, "Application rejected");
        }

        // =========================
        // 3. FETCH CURRENT STEP FLOW
        // =========================
        int currentStep = history.getApplicationStep();

        LeaveType leaveType = history.getApplication().getLeaveType();
        Long designationId = history.getApplication()
                .getEmployee()
                .getDesignation()
                .getDesignationId();

        boolean isExBd = Boolean.TRUE.equals(history.getExBangladeshLeave());

        Optional<?> currentFlowOpt;

        if (isExBd) {
            currentFlowOpt = conditionalFlowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, currentStep);
        } else {
            currentFlowOpt = flowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, currentStep);
        }

        if (currentFlowOpt.isEmpty()) {
            return new LeaveApprovalResponse(false, "Approval flow not found for current step");
        }

        boolean isCurrentFinalStep;

        if (isExBd) {
            LeaveApprovalFlowConditional flow =
                    (LeaveApprovalFlowConditional) currentFlowOpt.get();
            isCurrentFinalStep = Boolean.TRUE.equals(flow.getFinalStep());
        } else {
            LeaveApprovalFlow flow =
                    (LeaveApprovalFlow) currentFlowOpt.get();
            isCurrentFinalStep = Boolean.TRUE.equals(flow.getFinalStep());
        }

        // =========================
        // 4. SAVE CURRENT USER APPROVAL
        // =========================
        LeaveApplicationStatusHistory approvedStatus = new LeaveApplicationStatusHistory();
        approvedStatus.setApplicationHistory(history);
        approvedStatus.setActionTakenOn(Instant.now());
        approvedStatus.setActionTakenBy(LeaveActionRole.valueOf(request.roleId()));
        approvedStatus.setActionStatus(LeaveActionStatus.APPROVED);
        approvedStatus.setComment(request.comment());

        statusRepo.save(approvedStatus);

        // =========================
        // 5. IF CURRENT STEP IS FINAL → FINALIZE
        // =========================
        if (isCurrentFinalStep) {
            finalizeLeave(history, request.roleId());
            return new LeaveApprovalResponse(true, "Leave fully approved");
        }

        // =========================
        // 6. MOVE TO NEXT STEP
        // =========================
        int nextStep = currentStep + 1;

        Optional<?> nextFlowOpt;

        if (isExBd) {
            nextFlowOpt = conditionalFlowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, nextStep);
        } else {
            nextFlowOpt = flowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, nextStep);
        }

        if (nextFlowOpt.isEmpty()) {
            return new LeaveApprovalResponse(false, "Next approval step not found");
        }

        Long nextRoleId;

        if (isExBd) {
            LeaveApprovalFlowConditional flow =
                    (LeaveApprovalFlowConditional) nextFlowOpt.get();
            nextRoleId = mapRoleToId(flow.getApprovalRole());
        } else {
            LeaveApprovalFlow flow =
                    (LeaveApprovalFlow) nextFlowOpt.get();
            nextRoleId = mapRoleToId(flow.getApprovalRole());
        }

        // update history
        history.setApplicationStep(nextStep);
        history.setNextApprovalRoleId(String.valueOf(nextRoleId));
        historyRepo.save(history);

        // =========================
        // 7. ADD WAITING STATUS FOR NEXT APPROVER
        // =========================
        LeaveApplicationStatusHistory waitingStatus = new LeaveApplicationStatusHistory();
        waitingStatus.setApplicationHistory(history);
        waitingStatus.setActionTakenOn(Instant.now());
        waitingStatus.setActionTakenBy(LeaveActionRole.SYSTEM); // optional
        waitingStatus.setActionStatus(LeaveActionStatus.WAITING);
        waitingStatus.setComment("Forwarded to next approver");

        statusRepo.save(waitingStatus);

        return new LeaveApprovalResponse(true, "Forwarded to next approver");
    }

    // =====================================================
    // FINALIZATION LOGIC
    // =====================================================
    private void finalizeLeave(LeaveApplicationHistory history, String roleId) {

        // TODO:
        // - deduct leave balance / balance roleback if cancelled
        // - insert into emp_leave_balance_history
        // - handle EL conversion if needed

//        updateLeaveBalance(history);
        switch (history.getApplicationStage()) {

            case INITIAL -> processInitialApproval(history);

            case MODIFICATION -> processModificationApproval(history);

            case CANCELLATION -> processCancellationApproval(history);
        }

        if (history.getApplicationStage() == LeaveApplicationStage.CANCELLATION) {
            LeaveApplicationStatusHistory finalStatus = new LeaveApplicationStatusHistory();
            finalStatus.setApplicationHistory(history);
            finalStatus.setActionTakenOn(Instant.now());
            finalStatus.setActionTakenBy(LeaveActionRole.valueOf(roleId));
            finalStatus.setActionStatus(LeaveActionStatus.CANCELLED);
            finalStatus.setComment(null);
            statusRepo.save(finalStatus);
        }

        // Set nextApprovalRole = null in applicationHistory
        LeaveApplicationHistory finalHistory = leaveApplicationHistoryRepository.findById(history.getId()).orElse(null);
        if (finalHistory != null) {
            finalHistory.setNextApprovalRoleId(null);
            finalHistory.setApplicationStep(null);
            leaveApplicationHistoryRepository.save(finalHistory);
        }
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private LeaveActionRole getCurrentUserRole() {
        // TODO: integrate with auth system
        return LeaveActionRole.HEAD;
    }

    private LeaveActionRole getCurrentUserRole(String roleId) {
        if (roleId == null) return null;
        switch (roleId) {
            case "7001": return LeaveActionRole.VC;
            case "1001": return LeaveActionRole.HEAD;
            default: return null;
        }
    }

    private Long mapRoleToId(LeaveActionRole role) {
        // Stub mapping
        return switch (role) {
            case HEAD -> 1001L;
            case VC -> 7001L;
            default -> 9999L;
        };
    }

    private String getActionRoleFromRoleId(String roleId) {
        if (roleId == null) return null;
        switch (roleId) {
            case "7001": return LeaveActionRole.VC.name();
            case "1001": return LeaveActionRole.HEAD.name();
            default: return null;
        }
    }

    private void processInitialApproval(LeaveApplicationHistory history) {

        Employee employee = history.getApplication().getEmployee();

        LeaveType leaveType = history.getApplication().getLeaveType();

        EmployeeLeaveBalance balance = leaveBalanceRepo.findByEmployeeAndLeaveType(employee, leaveType).orElseThrow(() -> new RuntimeException("Leave balance not found"));

        int duration = Optional.ofNullable(history.getTotalDays()).orElse(0);

        int currentBalance = Optional.ofNullable(balance.getDaysLeft()).orElse(0);

        if (currentBalance < duration) {
            throw new RuntimeException("Insufficient leave balance");
        }

        int balanceAfter = currentBalance - duration;

        balance.setDaysLeft(balanceAfter);
        balance.setUpdatedOn(LocalDate.now());

        leaveBalanceRepo.save(balance);

        saveBalanceAudit(
                employee,
                leaveType,
                null,
                duration,
                balanceAfter);
    }

    private void processCancellationApproval(LeaveApplicationHistory history) {

        Employee employee = history.getApplication().getEmployee();

        LeaveType leaveType = history.getApplication().getLeaveType();

        EmployeeLeaveBalance balance = leaveBalanceRepo.findByEmployeeAndLeaveType(employee, leaveType).orElseThrow(() -> new RuntimeException("Leave balance not found"));

        LeaveApplicationHistory previousHistory = getPreviousHistory(history);

        if (!isFullyApproved(previousHistory)) {
            return;
        }

        int duration = Optional.ofNullable(previousHistory.getTotalDays()).orElse(0);

        int currentBalance = Optional.ofNullable(balance.getDaysLeft()).orElse(0);

        int balanceAfter = currentBalance + duration;

        balance.setDaysLeft(balanceAfter);
        balance.setUpdatedOn(LocalDate.now());

        leaveBalanceRepo.save(balance);

        saveBalanceAudit(
                employee,
                leaveType,
                duration,
                null,
                balanceAfter);
    }

    private void processModificationApproval(LeaveApplicationHistory history) {

        // TODO: Fix "Modify/revert leave balance only if it was fully approved and not forwarded" logic

        Employee employee = history.getApplication().getEmployee();

        LeaveType leaveType = history.getApplication().getLeaveType();

        EmployeeLeaveBalance balance = leaveBalanceRepo.findByEmployeeAndLeaveType(employee, leaveType).orElseThrow(() ->
                new RuntimeException("Leave balance not found"));

        LeaveApplicationHistory previousHistory = getPreviousHistory(history);

        if (!isFullyApproved(previousHistory)) {
            return;
        }

        int previousDays = Optional.ofNullable(previousHistory.getTotalDays()).orElse(0);

        int modifiedDays = Optional.ofNullable(history.getTotalDays()).orElse(0);

        int difference = modifiedDays - previousDays;

        int currentBalance = Optional.ofNullable(balance.getDaysLeft()).orElse(0);

        Integer credit = null;
        Integer debit = null;

        if (difference > 0) {

            if (currentBalance < difference) {
                throw new RuntimeException("Insufficient leave balance");
            }

            currentBalance -= difference;

            debit = difference;
        }
        else if (difference < 0) {

            currentBalance += Math.abs(difference);

            credit = Math.abs(difference);
        }

        balance.setDaysLeft(currentBalance);
        balance.setUpdatedOn(LocalDate.now());

        leaveBalanceRepo.save(balance);

        saveBalanceAudit(
                employee,
                leaveType,
                credit,
                debit,
                currentBalance);
    }

    private void saveBalanceAudit(
            Employee employee,
            LeaveType leaveType,
            Integer credit,
            Integer debit,
            Integer balanceAfter) {

        EmployeeLeaveBalanceHistory audit = new EmployeeLeaveBalanceHistory();

        audit.setEmployee(employee);
        audit.setLeaveType(leaveType);

        audit.setCredit(credit);
        audit.setDebit(debit);

        audit.setBalanceAfter(balanceAfter);

        audit.setEntryDate(LocalDate.now());
        audit.setLastModified(Instant.now());

        leaveBalanceHistoryRepo.save(audit);
    }

    private void updateLeaveBalance(LeaveApplicationHistory history) {

        Employee employee = history.getApplication().getEmployee();

        LeaveType leaveType = history.getApplication().getLeaveType();

        EmployeeLeaveBalance balance = leaveBalanceRepo.findByEmployeeAndLeaveType(employee, leaveType).orElseThrow(() -> new RuntimeException("Leave balance not found"));

        Integer duration = history.getTotalDays();

        if (duration == null) {
            duration = 0;
        }

        Integer currentBalance = balance.getDaysLeft();

        if (currentBalance == null) {
            currentBalance = 0;
        }

        Integer credit = 0;
        Integer debit = 0;
        Integer balanceAfter;

        if (history.getApplicationStage() == LeaveApplicationStage.CANCELLATION) {

            // Return leave balance
            balanceAfter = currentBalance + duration;

            balance.setDaysLeft(balanceAfter);

            credit = duration;

        } else {

            if (currentBalance < duration) {
                throw new RuntimeException("Insufficient leave balance");
            }

            // Deduct leave balance
            balanceAfter = currentBalance - duration;

            balance.setDaysLeft(balanceAfter);

            debit = duration;
        }

        balance.setUpdatedOn(LocalDate.now());

        leaveBalanceRepo.save(balance);

        // ==================================
        // AUDIT ENTRY
        // ==================================
        EmployeeLeaveBalanceHistory audit = new EmployeeLeaveBalanceHistory();

        audit.setEmployee(employee);
        audit.setLeaveType(leaveType);

        audit.setCredit(credit > 0 ? credit : null);
        audit.setDebit(debit > 0 ? debit : null);

        audit.setBalanceAfter(balanceAfter);

        audit.setEntryDate(LocalDate.now());
        audit.setLastModified(Instant.now());

        leaveBalanceHistoryRepo.save(audit);
    }

    private LeaveApplicationHistory getPreviousHistory(LeaveApplicationHistory currentHistory) {

        return historyRepo.findPreviousHistory(
                        currentHistory.getApplication().getId(),
                        currentHistory.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Previous history not found"));
    }

    private boolean isFullyApproved(LeaveApplicationHistory history) {

        LeaveApplicationStatusHistory latestStatus =
                statusRepo.findLatestStatus(
                                history.getId())
                        .orElse(null);

        if (latestStatus == null) {
            return false;
        }

        return latestStatus.getActionStatus()
                == LeaveActionStatus.APPROVED
                && history.getNextApprovalRoleId() == null;
    }

    private String formatStage(LeaveApplicationStage stage) {

        if (stage == null) {
            return "";
        }

        return switch (stage) {
            case INITIAL -> "Application";
            case MODIFICATION -> "Modification";
            case CANCELLATION -> "Cancellation";
        };
    }

    private LeaveApplicationTimelineResponse mapTimeline(LeaveActionStatus status, LeaveActionRole actionBy, String comment, String nextApproverRoleId, LocalDateTime time) {
        if (status == LeaveActionStatus.WAITING) {
            if (actionBy == LeaveActionRole.APPLICANT) {
                return new LeaveApplicationTimelineResponse(
                        "Applied",
                        actionBy.getDescription(),
                        comment,
                        time
                );
            }
        } else if (status == LeaveActionStatus.APPROVED) {
            if (nextApproverRoleId != null) {
                if (nextApproverRoleId.equals(getActionRoleIdFromRole(LeaveActionRole.HEAD))) {
                    return new LeaveApplicationTimelineResponse(
                            "Forwarded to " + LeaveActionRole.HEAD.getDescription(),
                            actionBy.getDescription(),
                            comment,
                            time
                    );
                } else if (nextApproverRoleId.equals(getActionRoleIdFromRole(LeaveActionRole.VC))) {
                    return new LeaveApplicationTimelineResponse(
                            "Forwarded to " + LeaveActionRole.VC.getDescription(),
                            actionBy.getDescription(),
                            comment,
                            time
                    );
                }
            } else {
                return new LeaveApplicationTimelineResponse(
                        LeaveActionStatus.APPROVED.getDescription(),
                        actionBy.getDescription(),
                        comment,
                        time
                );
            }
        } else if (status == LeaveActionStatus.REJECTED) {
            return new LeaveApplicationTimelineResponse(
                    LeaveActionStatus.REJECTED.getDescription(),
                    actionBy.getDescription(),
                    comment,
                    time
            );
        } else if (status == LeaveActionStatus.CANCELLED) {
            return new LeaveApplicationTimelineResponse(
                    LeaveActionStatus.CANCELLED.getDescription(),
                    actionBy.getDescription(),
                    comment,
                    time
            );
        }
        return new LeaveApplicationTimelineResponse(
                null,
                null,
                null,
                null
        );
    }

    private String getActionRoleIdFromRole(LeaveActionRole role) {
        if (role == null) return null;
        return switch (role) {
            case HEAD -> "1001";
            case VC -> "7001";
            default -> null;
        };
    }
}