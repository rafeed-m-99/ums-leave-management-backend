package org.aust.lms.service;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveApprovalListResponse;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
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

    public Page<LeaveApprovalListResponse> getPendingApplications(
            String roleId,
            String status,
            String applicationStage,
            String leaveType,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());

        LeaveActionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = LeaveActionStatus.valueOf(status);
        }

        LeaveApplicationStage stageEnum = null;
        if (applicationStage != null && !applicationStage.isBlank()) {
            stageEnum = LeaveApplicationStage.valueOf(applicationStage);
        }

        return historyRepository.findPendingByFilters(
                roleId,
                statusEnum,
                stageEnum,
                leaveType,
                pageable
        );
    }
}
