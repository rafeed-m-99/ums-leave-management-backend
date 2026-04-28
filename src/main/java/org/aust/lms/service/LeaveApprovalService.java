package org.aust.lms.service;

import org.aust.lms.dto.*;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.repository.*;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeaveApprovalService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationHistoryRepository historyRepo;
    private final LeaveApplicationStatusHistoryRepository statusRepo;
    private final LeaveApprovalFlowRepository flowRepo;
    private final LeaveApprovalFlowConditionalRepository conditionalFlowRepo;
    private final EmployeeLeaveBalanceRepository leaveBalanceRepo;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;

    public LeaveApprovalService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository historyRepo, LeaveApplicationStatusHistoryRepository statusRepo, LeaveApprovalFlowRepository flowRepo, LeaveApprovalFlowConditionalRepository conditionalFlowRepo, EmployeeLeaveBalanceRepository leaveBalanceRepo, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.historyRepo = historyRepo;
        this.statusRepo = statusRepo;
        this.flowRepo = flowRepo;
        this.conditionalFlowRepo = conditionalFlowRepo;
        this.leaveBalanceRepo = leaveBalanceRepo;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
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

        // (Optional) attachments
        List<AttachmentDto> attachments = null; // TODO: manage attachments

        // ✅ Build response
        LeaveApplicationDetailsResponse res = new LeaveApplicationDetailsResponse(
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
                statusHistory
        );

        return res;
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
            finalizeLeave(history, "Final approval", request.roleId());
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
    private void finalizeLeave(LeaveApplicationHistory history, String comment) {

        // TODO:
        // - deduct leave balance
        // - insert into emp_leave_balance_history
        // - handle EL conversion if needed

        LeaveApplicationStatusHistory finalStatus = new LeaveApplicationStatusHistory();
        finalStatus.setApplicationHistory(history);
        finalStatus.setActionTakenOn(Instant.now());
        finalStatus.setActionTakenBy(getCurrentUserRole());
        finalStatus.setActionStatus(LeaveActionStatus.APPROVED);
        finalStatus.setComment(comment);

        statusRepo.save(finalStatus);
    }

    private void finalizeLeave(LeaveApplicationHistory history, String comment, String roleId) {

        // TODO:
        // - deduct leave balance
        // - insert into emp_leave_balance_history
        // - handle EL conversion if needed

//        LeaveApplicationStatusHistory finalStatus = new LeaveApplicationStatusHistory();
//        finalStatus.setApplicationHistory(history);
//        finalStatus.setActionTakenOn(Instant.now());
//        finalStatus.setActionTakenBy(LeaveActionRole.SYSTEM);
//        finalStatus.setActionStatus(LeaveActionStatus.APPROVED);
//        finalStatus.setComment(comment);

        // Set nextApprovalRole = null in applicationHistory
        LeaveApplicationHistory finalHistory = leaveApplicationHistoryRepository.findById(history.getId()).orElse(null);
        if (finalHistory != null) {
            finalHistory.setNextApprovalRoleId(null);
            leaveApplicationHistoryRepository.save(finalHistory);
        }

//        statusRepo.save(finalStatus);
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
}