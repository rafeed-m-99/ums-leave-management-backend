package org.aust.lms.service;

import org.aust.lms.dto.ApplicantLeaveDetailsForUpdateResponse;
import org.aust.lms.dto.ApplicantLeaveDetailsResponse;
import org.aust.lms.dto.AttachmentDto;
import org.aust.lms.dto.LeaveApplicationTimelineResponse;
import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.aust.lms.entity.LeaveAttachment;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.repository.LeaveApplicationHistoryRepository;
import org.aust.lms.repository.LeaveApplicationRepository;
import org.aust.lms.repository.LeaveApplicationStatusHistoryRepository;
import org.aust.lms.repository.LeaveAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
                    getActionRoleFromRoleId(h.getNextApprovalRoleId()),
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

    @Transactional
    public List<LeaveApplicationTimelineResponse> getLeaveTimeline(Long applicationId) {
        LeaveApplication app = leaveApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        LeaveApplicationHistory history =
                leaveApplicationHistoryRepository.findLatestHistory(applicationId).orElse(null);

        List<LeaveApplicationStatusHistory> statuses = List.of();
        if (history != null) {
            statuses = leaveApplicationStatusHistoryRepository.findNonSystemStatuses(history.getId());
        }

        return statuses.stream().map(status -> mapTimeline(
                status.getActionStatus(),
                status.getActionTakenBy(),
                status.getComment(),
                history.getNextApprovalRoleId(),
                LocalDateTime.ofInstant(status.getActionTakenOn(), ZoneId.systemDefault())
        )).toList();
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

    private String getActionRoleFromRoleId(String roleId) {
        if (roleId == null) return null;
        return switch (roleId) {
            case "7001" -> LeaveActionRole.VC.name();
            case "1001" -> LeaveActionRole.HEAD.name();
            default -> null;
        };
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
