package org.aust.lms.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveApprovalListResponse;
import org.aust.lms.dto.LeaveApprovalPageResponse;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApprovalFlowConditionalRepository;
import org.aust.lms.repository.LeaveApprovalFlowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovalQueryService {

    private final LeaveApplicationHistoryRepository historyRepository;

    public LeaveApprovalQueryService(LeaveApplicationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;

    }

    @Transactional
    public LeaveApprovalPageResponse getPendingApplications(
            String roleId,
            String status,
            String applicationStage,
            String leaveType,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        LeaveActionStatus statusEnum =
                (status == null || status.isBlank()) ? null : LeaveActionStatus.valueOf(status);

        LeaveApplicationStage stageEnum =
                (applicationStage == null || applicationStage.isBlank()) ? null : LeaveApplicationStage.valueOf(applicationStage);

        String sortField = mapSortField(sortBy);

        Sort sort = (sortDir != null && sortDir.equalsIgnoreCase("desc"))
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeaveApprovalListResponse> result =
                historyRepository.findPendingByFilters(
                        roleId,
                        statusEnum,
                        stageEnum,
                        leaveType,
                        pageable
                );

        String actionRole = getActionRoleFromRoleId(roleId);

        return new LeaveApprovalPageResponse(result, actionRole);
    }

    private boolean checkVC(String roleId) {
        return false;
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "a.appliedOn";

        return switch (sortBy) {
            case "appliedOn" -> "a.appliedOn";
            case "from" -> "h.fromDate";
            case "to" -> "h.toDate";
            default -> "a.appliedOn";
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
