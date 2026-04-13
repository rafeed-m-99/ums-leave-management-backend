package org.aust.lms.service;

import org.aust.lms.dto.ApplicantLeaveDetailsResponse;
import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApplicationRepository;
import org.aust.lms.repository.LeaveApplicationStatusHistoryRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;

    public LeaveApplicationService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.leaveApplicationStatusHistoryRepository = leaveApplicationStatusHistoryRepository;
    }

    @Transactional
    public List<ApplicantLeaveDetailsResponse> getLeaveDetails(String employeeId, Long applicationId) {

        LeaveApplication app = leaveApplicationRepository
                .findByIdAndEmployeeId(applicationId, employeeId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        List<LeaveApplicationHistory> histories =
                leaveApplicationHistoryRepository.findByApplicationIdOrderByCreatedOn(applicationId);

        List<Long> historyIds = histories.stream()
                .map(LeaveApplicationHistory::getId)
                .toList();

        List<LeaveApplicationStatusHistory> statuses =
                leaveApplicationStatusHistoryRepository.findNonSystemStatuses(historyIds);

        Map<Long, List<LeaveApplicationStatusHistory>> statusMap =
                statuses.stream().collect(Collectors.groupingBy(
                        s -> s.getApplicationHistory().getId()
                ));

        return histories.stream().map(h -> {

            List<LeaveApplicationStatusHistory> historyStatuses = statusMap.get(h.getId());

            LeaveApplicationStatusHistory latest = (historyStatuses != null && !historyStatuses.isEmpty())
                    ? historyStatuses.stream()
                    .max(Comparator.comparing(LeaveApplicationStatusHistory::getActionTakenOn))
                    .orElse(null)
                    : null;

            return new ApplicantLeaveDetailsResponse(
                    app.getId(),
                    app.getAppliedOn(),
                    app.getLeaveType().getName(),
                    h.getFromDate(),
                    h.getToDate(),
                    h.getTotalDays(),
                    h.getReason(),
                    List.of(),
                    h.getApplicationStage().name(),
                    latest != null ? latest.getActionStatus().name() : "WAITING",
                    latest != null ? latest.getActionTakenBy().name() : null,
                    latest != null ? latest.getActionTakenOn() : null
            );
        }).toList();
    }
}
