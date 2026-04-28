package org.aust.lms.service;

import org.aust.lms.dto.LeaveApplicationUpdateRequest;
import org.aust.lms.dto.LeaveAttachmentRequest;
import org.aust.lms.dto.LeaveApplicationFormRequest;
import org.aust.lms.dto.LeaveApplicationResponse;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.exception.BadRequestException;
import org.aust.lms.exception.NotFoundException;
import org.aust.lms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeaveApplicationFormService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeavePolicyRepository leavePolicyRepository;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
    private final EmployeeDesignationRepository employeeDesignationRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;

    private final FileStorageService fileStorageService;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;

    public LeaveApplicationFormService(EmployeeRepository employeeRepository, LeaveTypeRepository leaveTypeRepository, LeavePolicyRepository leavePolicyRepository, LeaveApplicationRepository leaveApplicationRepository, EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository, EmployeeDesignationRepository employeeDesignationRepository, LeaveAttachmentRepository leaveAttachmentRepository, FileStorageService fileStorageService, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.employeeLeaveBalanceRepository = employeeLeaveBalanceRepository;
        this.employeeDesignationRepository = employeeDesignationRepository;
        this.leaveAttachmentRepository = leaveAttachmentRepository;
        this.fileStorageService = fileStorageService;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.leaveApplicationStatusHistoryRepository = leaveApplicationStatusHistoryRepository;
    }

    @Transactional
    public LeaveApplicationResponse applyForLeave(
            String employeeId,
            Long designationId,
            String departmentId,
            LeaveApplicationFormRequest request,
            String sessionId
    ) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new BadRequestException("Invalid leave type"));

        EmployeeDesignation designation = employeeDesignationRepository.findById(designationId)
                .orElseThrow(() -> new BadRequestException("Invalid designation"));

        LeavePolicy policy = leavePolicyRepository.findByLeaveType(leaveType)
                .orElseThrow(() -> new BadRequestException("No policy defined"));

        EmployeeLeaveBalance balance = employeeLeaveBalanceRepository
                .findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new BadRequestException("Not eligible"));

        validatePolicy(employee, policy, request);
        validateBalance(employee, balance, request);

        LocalDate fromDate = LocalDate.parse(request.from());
        LocalDate toDate = LocalDate.parse(request.to());

        int totalDays = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        String nextApprover = getNextApprover(
                leaveType, designation, departmentId,
                request.applicationStep(), request.exBdLeave()
        );

        Instant now = Instant.now();

        LeaveApplication application = new LeaveApplication();
        application.setEmployee(employee);
        application.setLeaveType(leaveType);
        application.setAppliedOn(now);
        application.setCreatedOn(now);

        LeaveApplicationHistory history = new LeaveApplicationHistory();
        history.setApplicationStage(LeaveApplicationStage.INITIAL);
        history.setFromDate(fromDate);
        history.setToDate(toDate);
        history.setTotalDays(totalDays);
        history.setReason(request.reason());
        history.setExBangladeshLeave(request.exBdLeave());
        history.setSandwichLeave(false);
        history.setApplicationStep(1);
        history.setNextApprovalRoleId(nextApprover);
        history.setCreatedOn(now);
        history.setIsActive(true);

        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
        status.setActionTakenOn(now);
        status.setActionTakenBy(LeaveActionRole.APPLICANT);
        status.setActionStatus(LeaveActionStatus.WAITING);

        history.addStatus(status);
        application.addHistory(history);

        leaveApplicationRepository.save(application);

        // =========================
        // HANDLE FILES
        // =========================
        if (request.attachments() != null) {

            Path leaveFolder = Paths.get("uploads/leave")
                    .resolve(String.valueOf(application.getId()));

            try {
                Files.createDirectories(leaveFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (LeaveAttachmentRequest a : request.attachments()) {
                Path tempFile = Paths.get("uploads/temp")
                        .resolve(sessionId)
                        .resolve(a.storedFileName());

                Path finalFile = leaveFolder.resolve(a.storedFileName());

                try {
                    Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                LeaveAttachment att = new LeaveAttachment();

                att.setOriginalFileName(a.originalFileName());
                att.setStoredFileName(a.storedFileName());
                att.setFileType(a.fileType());
                att.setFileSize(a.fileSize());

                try {
                    att.setFileSize(Files.size(finalFile));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                att.setDescription(a.description());
                att.setUploadedAt(Instant.now());
                att.setLeaveApplication(application);

                application.getAttachments().add(att);
            }

            fileStorageService.deleteTemp(sessionId);
        }

        return new LeaveApplicationResponse(
                application.getId(),
                true,
                List.of("Leave applied successfully")
        );
    }

    @Transactional
    public LeaveApplicationResponse modifyApplication(
            Long applicationId,
            LeaveApplicationUpdateRequest request
    ) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        LeaveApplicationHistory latestHistory = leaveApplicationHistoryRepository.findLatestHistory(applicationId)
                .orElseThrow(() -> new NotFoundException("History not found"));

        Instant now = Instant.now();

        LocalDate fromDate = LocalDate.parse(request.from());
        LocalDate toDate = LocalDate.parse(request.to());

        int totalDays = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        // =========================
        // CASE 1: NOT YET PROCESSED
        // =========================
        if (latestHistory.getApplicationStep() != null && latestHistory.getApplicationStep() == 1) {

            latestHistory.setFromDate(fromDate);
            latestHistory.setToDate(toDate);
            latestHistory.setTotalDays(totalDays);
            latestHistory.setReason(request.reason());
            latestHistory.setExBangladeshLeave(request.exBdLeave());

            return new LeaveApplicationResponse(
                    application.getId(),
                    true,
                    List.of("Application updated successfully")
            );
        }

        // =========================
        // CASE 2: ALREADY IN WORKFLOW → CREATE NEW VERSION
        // =========================
        latestHistory.setIsActive(false);
        leaveApplicationHistoryRepository.save(latestHistory);

        LeaveApplicationHistory newHistory = new LeaveApplicationHistory();

        newHistory.setApplicationStage(LeaveApplicationStage.MODIFICATION);
        newHistory.setFromDate(fromDate);
        newHistory.setToDate(toDate);
        newHistory.setTotalDays(totalDays);
        newHistory.setReason(request.reason());
        newHistory.setExBangladeshLeave(request.exBdLeave());
        newHistory.setSandwichLeave(false);

        newHistory.setApplicationStep(1); // restart flow
        newHistory.setCreatedOn(now);
        newHistory.setIsActive(true);

        // restart approval flow
        String nextApprover = getNextApprover(
                application.getLeaveType(),
                application.getEmployee().getDesignation(),
                application.getEmployee().getDeptOffice(),
                1,
                request.exBdLeave()
        );

        newHistory.setNextApprovalRoleId(nextApprover);

        // create initial status for new version
        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
        status.setActionTakenOn(now);
        status.setActionTakenBy(LeaveActionRole.APPLICANT);
        status.setActionStatus(LeaveActionStatus.WAITING);

        newHistory.addStatus(status);

        application.addHistory(newHistory);

        leaveApplicationRepository.save(application);

        return new LeaveApplicationResponse(
                application.getId(),
                true,
                List.of("Modification submitted successfully")
        );
    }

    @Transactional
    public LeaveApplicationResponse cancelApplication(Long applicationId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        LeaveApplicationHistory latestHistory = leaveApplicationHistoryRepository.findLatestHistory(applicationId)
                .orElseThrow(() -> new NotFoundException("History not found"));

        Instant now = Instant.now();

        if (latestHistory.getApplicationStep() != null && latestHistory.getApplicationStep() == 1) {
            LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
            status.setActionTakenOn(now);
            status.setActionTakenBy(LeaveActionRole.APPLICANT);
            status.setActionStatus(LeaveActionStatus.CANCELLED);
            latestHistory.addStatus(status);

            return new LeaveApplicationResponse(
                    application.getId(),
                    true,
                    List.of("Application cancelled successfully")
            );
        }

        // =========================
        // CREATE NEW CANCELLATION HISTORY
        // =========================
        latestHistory.setIsActive(false);
        leaveApplicationHistoryRepository.save(latestHistory);

        LeaveApplicationHistory cancelHistory = new LeaveApplicationHistory();

        cancelHistory.setApplicationStage(LeaveApplicationStage.CANCELLATION);

        // carry forward previous data (important!)
        cancelHistory.setFromDate(latestHistory.getFromDate());
        cancelHistory.setToDate(latestHistory.getToDate());
        cancelHistory.setTotalDays(latestHistory.getTotalDays());
        cancelHistory.setReason(latestHistory.getReason());
        cancelHistory.setExBangladeshLeave(latestHistory.getExBangladeshLeave());
        cancelHistory.setIsActive(true);
        cancelHistory.setSandwichLeave(latestHistory.isSandwichLeave());
        cancelHistory.setApplicationStep(1);
        cancelHistory.setCreatedOn(now);

        // approval flow again
        String nextApprover = getNextApprover(
                application.getLeaveType(),
                application.getEmployee().getDesignation(),
                application.getEmployee().getDeptOffice(),
                1,
                latestHistory.getExBangladeshLeave()
        );

        cancelHistory.setNextApprovalRoleId(nextApprover);

        // status entry
        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
        status.setActionTakenOn(now);
        status.setActionTakenBy(LeaveActionRole.APPLICANT);
        status.setActionStatus(LeaveActionStatus.WAITING);

        cancelHistory.addStatus(status);

        application.addHistory(cancelHistory);

        leaveApplicationRepository.save(application);

        return new LeaveApplicationResponse(
                application.getId(),
                true,
                List.of("Cancellation request submitted")
        );
    }

    private void handleAttachments(LeaveApplication application,
                                   List<LeaveAttachmentRequest> attachments,
                                   String sessionId) {

        if (attachments == null || attachments.isEmpty()) return;

        Path leaveFolder = Paths.get("uploads/leave")
                .resolve(String.valueOf(application.getId()));

        try {
            Files.createDirectories(leaveFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (LeaveAttachmentRequest a : attachments) {

            Path tempFile = Paths.get("uploads/temp")
                    .resolve(sessionId)
                    .resolve(a.storedFileName());

            Path finalFile = leaveFolder.resolve(a.storedFileName());

            try {
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LeaveAttachment att = new LeaveAttachment();
            att.setOriginalFileName(a.originalFileName());
            att.setStoredFileName(a.storedFileName());
            att.setFileType(a.fileType());
            att.setFileSize(a.fileSize());
            att.setDescription(a.description());
            att.setUploadedAt(Instant.now());
            att.setLeaveApplication(application);

            application.getAttachments().add(att);
        }

        fileStorageService.deleteTemp(sessionId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId, String userId) {

        LeaveAttachment att = leaveAttachmentRepository.findById(attachmentId)
                .orElseThrow();

        LeaveApplication leave = att.getLeaveApplication();

        // 🔒 SECURITY: ensure user owns this leave
        if (!leave.getEmployee().getEmployeeId().equals(userId)) {
            throw new SecurityException("Unauthorized");
        }

        fileStorageService.delete(att.getStoredFileName(), leave.getId());

        leaveAttachmentRepository.delete(att);
    }

    // =========================================================
    // POLICY VALIDATION
    // =========================================================
    private void validatePolicy(Employee employee,
                                LeavePolicy policy,
                                LeaveApplicationFormRequest request) {

//        LocalDate from = LocalDate.parse(request.from());
//        LocalDate to = LocalDate.parse(request.to());
//
//        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
//
//        // ❌ Date validation
//        if (from.isAfter(to)) {
//            messages.add("From date cannot be after To date.");
//        }
//
//        // ❌ Max duration per application
//        if (policy.getMaxDurationPerApplication() != null &&
//                days > policy.getMaxDurationPerApplication()) {
//            messages.add("Exceeded maximum allowed days per application.");
//        }
//
//        // ❌ Ex-BD leave check
//        if (Boolean.TRUE.equals(request.exBdLeave()) &&
//                Boolean.FALSE.equals(policy.getAllowedExBDLeave())) {
//            messages.add("Ex-Bangladesh leave is not allowed for this leave type.");
//        }
//
//        // ❌ Minimum years of service
//        if (policy.getMinYearsOfService() != null) {
//            long years = ChronoUnit.YEARS.between(
//                    employee.getJoiningDate(),
//                    LocalDate.now()
//            );
//
//            if (years < policy.getMinYearsOfService()) {
//                messages.add("Minimum years of service requirement not met.");
//            }
//        }
//
//        New ones are here:
//        LocalDate from = LocalDate.parse(request.from());
//        LocalDate to = LocalDate.parse(request.to());
//
//        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
//
//        if (from.isAfter(to)) {
//            messages.add("From date cannot be after To date.");
//        }
//
//        // TODO: Verify employee type
//
//        // TODO: Verify gender
//

    }

    private void validateBalance(Employee employee,
                                 EmployeeLeaveBalance employeeLeaveBalance,
                                 LeaveApplicationFormRequest request) {

    }

    // =========================
    // SAMPLE APPROVAL FLOW
    // =========================
    private String getNextApprover(LeaveType leaveType,
                                       EmployeeDesignation designation,
                                       String departmentId,
                                       Integer step,
                                       Boolean isExBdLeave) {

        // TODO: check LeaveApprovalFlow and LeaveApprovalFlowConditional and return the roleId from IUMS

        return "1001"; // sample role ID
    }

}
