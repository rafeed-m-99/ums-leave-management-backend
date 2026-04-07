package org.aust.lms.service;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.*;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApprovalRole;
import org.aust.lms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LeaveApprovalService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository historyRepo, LeaveApplicationStatusHistoryRepository statusRepo, LeaveApprovalFlowRepository flowRepo, LeaveApprovalFlowConditionalRepository conditionalFlowRepo, EmployeeLeaveBalanceRepository leaveBalanceRepo) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.historyRepo = historyRepo;
        this.statusRepo = statusRepo;
        this.flowRepo = flowRepo;
        this.conditionalFlowRepo = conditionalFlowRepo;
        this.leaveBalanceRepo = leaveBalanceRepo;
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
                attachments,
                statusHistory
        );

        return res;
    }

    @Transactional
    public LeaveApprovalResponse processApproval(LeaveApprovalRequest request) {

        // =========================
        // 1. FETCH HISTORY
        // =========================
        LeaveApplicationHistory history = historyRepo.findLatestHistory(request.applicationId())
                .orElse(null);

        if (history == null) {
            return new LeaveApprovalResponse(false, "Application not found");
        }

        // =========================
        // 2. HANDLE REJECTION
        // =========================
        if (LeaveActionStatus.valueOf(request.action()) == LeaveActionStatus.REJECTED) {
            LeaveApplicationStatusHistory rejectedStatus = new LeaveApplicationStatusHistory();
            rejectedStatus.setApplicationHistory(history);
            rejectedStatus.setActionTakenOn(Instant.now());
            rejectedStatus.setActionTakenBy(getCurrentUserRole());
            rejectedStatus.setActionStatus(LeaveActionStatus.REJECTED);
            rejectedStatus.setComment(request.comment());
            statusRepo.save(rejectedStatus);

            return new LeaveApprovalResponse(true, "Application rejected");
        }

        // =========================
        // 3. MOVE TO NEXT STEP
        // =========================
        int nextStep = history.getApplicationStep() + 1;
        LeaveType leaveType = history.getApplication().getLeaveType();
        Long designationId = history.getApplication()
                .getEmployee()
                .getDesignation()
                .getDesignationId();
        boolean isExBd = Boolean.TRUE.equals(history.getExBangladeshLeave());

        // =========================
        // 4. FETCH NEXT FLOW
        // =========================
        Optional<?> flowOpt;
        if (isExBd) {
            flowOpt = conditionalFlowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, nextStep);
        } else {
            flowOpt = flowRepo
                    .findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
                            leaveType.getId(), designationId, nextStep);
        }

        boolean isFinalStep;
        Long nextRoleId = null;

        if (flowOpt.isEmpty()) {
            // No next step → final approval
            finalizeLeave(history, request.comment());
            history.setApplicationStep(nextStep);
            history.setNextApprovalRoleId(null);
            historyRepo.save(history);
            return new LeaveApprovalResponse(true, "Leave fully approved");
        } else {
            if (isExBd) {
                LeaveApprovalFlowConditional flow = (LeaveApprovalFlowConditional) flowOpt.get();
                nextRoleId = mapRoleToId(flow.getApprovalRole());
                isFinalStep = Boolean.TRUE.equals(flow.getFinalStep());
            } else {
                LeaveApprovalFlow flow = (LeaveApprovalFlow) flowOpt.get();
                nextRoleId = mapRoleToId(flow.getApprovalRole());
                isFinalStep = Boolean.TRUE.equals(flow.getFinalStep());
            }
        }

        // =========================
        // 5. SAVE CURRENT USER ACTION
        // =========================
        LeaveApplicationStatusHistory currentStatus = new LeaveApplicationStatusHistory();
        currentStatus.setApplicationHistory(history);
        currentStatus.setActionTakenOn(Instant.now());
        currentStatus.setActionTakenBy(getCurrentUserRole());
        currentStatus.setActionStatus(LeaveActionStatus.APPROVED);
        currentStatus.setComment(request.comment());
        statusRepo.save(currentStatus);

        // =========================
        // 6. UPDATE HISTORY FOR NEXT APPROVER
        // =========================
        history.setApplicationStep(nextStep);
        history.setNextApprovalRoleId(String.valueOf(nextRoleId));
        historyRepo.save(history);

        // =========================
        // 7. IF FINAL STEP → FINALIZE
        // =========================
        if (isFinalStep) {
            finalizeLeave(history, "Final approval");
            return new LeaveApprovalResponse(true, "Leave fully approved");
        }

        // =========================
        // 8. ADD WAITING STATUS FOR NEXT APPROVER
        // =========================
        LeaveApplicationStatusHistory waitingStatus = new LeaveApplicationStatusHistory();
        waitingStatus.setApplicationHistory(history);
        waitingStatus.setActionTakenOn(Instant.now());
        waitingStatus.setActionTakenBy(getCurrentUserRole()); // Could be NULL or next approver
        waitingStatus.setActionStatus(LeaveActionStatus.WAITING);
        waitingStatus.setComment("Forwarded to next approver");
        statusRepo.save(waitingStatus);

        return new LeaveApprovalResponse(true, "Forwarded to next approver");
    }

    // =====================================================
    // FINALIZATION LOGIC
    // =====================================================
    private void finalizeLeave(LeaveApplicationHistory history, String comment) {
        // TODO: deduct leave balance
        // TODO: insert into emp_leave_balance_history
        // TODO: handle EL conversion if needed

        LeaveApplicationStatusHistory finalStatus = new LeaveApplicationStatusHistory();
        finalStatus.setApplicationHistory(history);
        finalStatus.setActionTakenOn(Instant.now());
        finalStatus.setActionTakenBy(getCurrentUserRole());
        finalStatus.setActionStatus(LeaveActionStatus.APPROVED);
        finalStatus.setComment(comment);
        statusRepo.save(finalStatus);
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private LeaveApprovalRole getCurrentUserRole() {
        // TODO: integrate with auth system
        return LeaveApprovalRole.HEAD;
    }

    private Long mapRoleToId(LeaveApprovalRole role) {
        // Stub mapping
        return switch (role) {
            case HEAD -> 1001L;
            case VC -> 7001L;
            default -> 9999L;
        };
    }
}