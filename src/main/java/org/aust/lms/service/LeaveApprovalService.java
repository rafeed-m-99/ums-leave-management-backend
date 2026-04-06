package org.aust.lms.service;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveApprovalRequest;
import org.aust.lms.dto.LeaveApprovalResponse;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApprovalRole;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApplicationStatusHistoryRepository;
import org.aust.lms.repository.LeaveApprovalFlowConditionalRepository;
import org.aust.lms.repository.LeaveApprovalFlowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class LeaveApprovalService {

    private final LeaveApplicationHistoryRepository historyRepo;
    private final LeaveApplicationStatusHistoryRepository statusRepo;
    private final LeaveApprovalFlowRepository flowRepo;
    private final LeaveApprovalFlowConditionalRepository conditionalFlowRepo;

    public LeaveApprovalService(LeaveApplicationHistoryRepository historyRepo, LeaveApplicationStatusHistoryRepository statusRepo, LeaveApprovalFlowRepository flowRepo, LeaveApprovalFlowConditionalRepository conditionalFlowRepo) {
        this.historyRepo = historyRepo;
        this.statusRepo = statusRepo;
        this.flowRepo = flowRepo;
        this.conditionalFlowRepo = conditionalFlowRepo;
    }

    public LeaveApprovalResponse processApproval(LeaveApprovalRequest request) {

        // =========================
        // 1. FETCH HISTORY
        // =========================
        LeaveApplicationHistory history = historyRepo.findById(request.applicationHistoryId())
                .orElse(null);

        if (history == null) {
            return new LeaveApprovalResponse(false, "Application not found");
        }

        // =========================
        // 2. SAVE STATUS HISTORY
        // =========================
        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();

        status.setApplicationHistory(history);
        status.setActionTakenOn(Instant.now());
        status.setActionTakenBy(getCurrentUserRole()); // stub
        status.setActionStatus(request.action());
        status.setComment(request.comment());

        statusRepo.save(status);

        // =========================
        // 3. HANDLE REJECTION
        // =========================
        if (request.action() == LeaveActionStatus.REJECTED) {
            return new LeaveApprovalResponse(true, "Application rejected");
        }

        // =========================
        // 4. MOVE TO NEXT STEP
        // =========================
        int nextStep = history.getApplicationStep() + 1;

        LeaveType leaveType = history.getApplication().getLeaveType();
        Long designationId = history.getApplication()
                .getEmployee()
                .getDesignation()
                .getDesignationId();

        boolean isExBd = Boolean.TRUE.equals(history.getExBangladeshLeave());

        // =========================
        // 5. FETCH NEXT FLOW
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

        // =========================
        // 6. FINAL STEP
        // =========================
        if (flowOpt.isEmpty()) {
            finalizeLeave(history);
            return new LeaveApprovalResponse(true, "Leave fully approved");
        }

        // =========================
        // 7. UPDATE NEXT STEP
        // =========================
        Long nextRoleId;

        boolean isFinalStep;

        if (isExBd) {
            LeaveApprovalFlowConditional flow =
                    (LeaveApprovalFlowConditional) flowOpt.get();

            nextRoleId = mapRoleToId(flow.getApprovalRole());
            isFinalStep = Boolean.TRUE.equals(flow.getFinalStep());

        } else {
            LeaveApprovalFlow flow =
                    (LeaveApprovalFlow) flowOpt.get();

            nextRoleId = mapRoleToId(flow.getApprovalRole());
            isFinalStep = Boolean.TRUE.equals(flow.getFinalStep());
        }

        history.setApplicationStep(nextStep);
        history.setNextApprovalRoleId(String.valueOf(nextRoleId));

        historyRepo.save(history);

        // =========================
        // 8. IF FINAL STEP
        // =========================
        if (isFinalStep) {
            finalizeLeave(history);
            return new LeaveApprovalResponse(true, "Leave fully approved");
        }

        return new LeaveApprovalResponse(true, "Forwarded to next approver");
    }

    // =====================================================
    // FINALIZATION LOGIC
    // =====================================================
    private void finalizeLeave(LeaveApplicationHistory history) {

        // TODO: deduct leave balance
        // TODO: insert into emp_leave_balance_history
        // TODO: handle EL conversion if needed

        // For now just mark final status
        LeaveApplicationStatusHistory finalStatus = new LeaveApplicationStatusHistory();

        finalStatus.setApplicationHistory(history);
        finalStatus.setActionTakenOn(Instant.now());
        finalStatus.setActionTakenBy(getCurrentUserRole());
        finalStatus.setActionStatus(LeaveActionStatus.APPROVED);
        finalStatus.setComment("Final approval");

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
            case VC -> 1002L;
            default -> 9999L;
        };
    }
}