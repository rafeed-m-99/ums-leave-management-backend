package org.aust.lms.service;

import org.aust.lms.dto.ApplicantLeaveListResponse;
import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApplicationRepository;
import org.aust.lms.repository.LeaveApplicationStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveApplicationQueryService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;

    public LeaveApplicationQueryService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveApplicationStatusHistoryRepository = leaveApplicationStatusHistoryRepository;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
    }

    @Transactional
    public List<ApplicantLeaveListResponse> getApplicantLeaveList(
            String employeeId,
            String status,     // WAITING, APPROVED, REJECTED, or null (All)
            String actionRole, // HEAD, VC
            String nextRole    // HEAD, VC
    ) {
        List<LeaveApplication> applications = leaveApplicationRepository.findAllLeaves(employeeId);

        // 1️⃣ Get all application IDs
        List<Long> appIds = applications.stream().map(LeaveApplication::getId).toList();

        // 2️⃣ Fetch latest history for each application
        List<LeaveApplicationHistory> latestHistories = leaveApplicationHistoryRepository.findLatestHistories(appIds);

        // Map applicationId → latestHistory
        Map<Long, LeaveApplicationHistory> latestHistoryMap = latestHistories.stream()
                .collect(Collectors.toMap(lh -> lh.getApplication().getId(), lh -> lh));

        // 3️⃣ Fetch latest statuses
        List<Long> historyIds = latestHistories.stream().map(LeaveApplicationHistory::getId).toList();
        List<LeaveApplicationStatusHistory> latestStatuses = leaveApplicationStatusHistoryRepository.findLatestNonSystemStatuses(historyIds);

        Map<Long, LeaveApplicationStatusHistory> latestStatusMap = latestStatuses.stream()
                .collect(Collectors.toMap(ls -> ls.getApplicationHistory().getId(), ls -> ls));

        // 4️⃣ Filter applications based on status
        List<LeaveApplication> filteredApps = applications.stream().filter(app -> {

            LeaveApplicationHistory lh = latestHistoryMap.get(app.getId());
            LeaveApplicationStatusHistory ls = lh != null ? latestStatusMap.get(lh.getId()) : null;

            if (lh == null) return false;

            // =========================
            // ✅ CANCELLED LOGIC (NEW)
            // =========================
            if ("CANCELLED".equals(status)) {
                return ls != null && "CANCELLED".equals(ls.getActionStatus().name());
            }

            // =========================
            // ✅ WAITING LOGIC
            // =========================
            if ("WAITING".equals(status)) {

                if (ls != null) {
                    String actionStatus = ls.getActionStatus().name();

                    // exclude final states
                    if ("APPROVED".equals(actionStatus)
                            || "REJECTED".equals(actionStatus)
                            || "CANCELLED".equals(actionStatus)) {
                        return false;
                    }
                }

                String mappedNextRole = getActionRoleFromRoleId(lh.getNextApprovalRoleId());

                return nextRole == null || mappedNextRole.equals(nextRole);
            }

            // =========================
            // ✅ APPROVED / REJECTED
            // =========================
            if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                if (ls == null) return false;

                return ls.getActionTakenBy().name().equals(actionRole)
                        && ls.getActionStatus().name().equals(status);
            }

            // =========================
            // ✅ ALL
            // =========================
            return true;

        }).toList();

        // 5️⃣ Map to DTO
        return filteredApps.stream().map(app -> {
            LeaveApplicationHistory lh = latestHistoryMap.get(app.getId());
            LeaveApplicationStatusHistory ls = lh != null ? latestStatusMap.get(lh.getId()) : null;

            return new ApplicantLeaveListResponse(
                    app.getId(),
                    app.getAppliedOn(),
                    app.getLeaveType().getName(),
                    lh.getFromDate(),
                    lh.getToDate(),
                    lh.getTotalDays(),
                    lh.getApplicationStage().name(),
                    ls != null ? ls.getActionStatus().name() : null,
                    ls != null ? ls.getActionTakenBy().name() : null,
                    getActionRoleFromRoleId(lh.getNextApprovalRoleId())
            );
        }).toList();
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
