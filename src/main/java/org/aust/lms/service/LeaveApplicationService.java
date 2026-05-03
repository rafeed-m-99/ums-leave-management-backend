package org.aust.lms.service;

import org.aust.lms.dto.ApplicantLeaveDetailsForUpdateResponse;
import org.aust.lms.dto.ApplicantLeaveDetailsResponse;
import org.aust.lms.dto.AttachmentDto;
import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.aust.lms.entity.LeaveAttachment;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApplicationRepository;
import org.aust.lms.repository.LeaveApplicationStatusHistoryRepository;
import org.aust.lms.repository.LeaveAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveApplicationService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;

    public LeaveApplicationService(LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository, LeaveAttachmentRepository leaveAttachmentRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.leaveApplicationStatusHistoryRepository = leaveApplicationStatusHistoryRepository;
        this.leaveAttachmentRepository = leaveAttachmentRepository;
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

        // ✅ NEW: Fetch attachments once
        List<AttachmentDto> attachmentDtos = app.getAttachments().stream()
                        .map(att -> new AttachmentDto(
                                att.getId(),
                                att.getOriginalFileName(),
                                att.getFileType(),
                                att.getDescription()
                        ))
                        .toList();

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
                    app.getLeaveType().getId(),
                    app.getLeaveType().getName(),
                    h.getFromDate(),
                    h.getToDate(),
                    h.getTotalDays(),
                    h.getReason(),
                    h.getExBangladeshLeave(),
                    attachmentDtos,
                    h.getApplicationStage().name(),
                    latest != null ? latest.getActionStatus().name() : "WAITING",
                    latest != null ? latest.getActionTakenBy().name() : null,
                    latest != null ? latest.getActionTakenOn() : null
            );

        }).toList();
    }

    @Transactional
    public ApplicantLeaveDetailsForUpdateResponse getLatestLeaveDetails(Long applicationId) {
        LeaveApplication app = leaveApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        LeaveApplicationHistory history = leaveApplicationHistoryRepository
                .findLatestHistory(applicationId)
                .orElseThrow(() -> new RuntimeException("Application history not found"));

        List<AttachmentDto> attachmentDtos = app.getAttachments().stream()
                .map(att -> new AttachmentDto(
                        att.getId(),
                        att.getOriginalFileName(),
                        att.getFileType(),
                        att.getDescription()
                ))
                .toList();

        return new ApplicantLeaveDetailsForUpdateResponse(
                app.getId(),
                app.getAppliedOn(),
                app.getLeaveType().getId(),
                app.getLeaveType().getName(),
                history.getFromDate(),
                history.getToDate(),
                history.getTotalDays(),
                history.getReason(),
                history.getExBangladeshLeave(),
                attachmentDtos,
                history.getApplicationStage().name()
        );
    }
}
