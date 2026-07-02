package org.aust.lms.service;

import org.aust.lms.dto.*;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private final HolidayRepository holidayRepository;
    private final LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository;

    public LeaveApplicationFormService(EmployeeRepository employeeRepository, LeaveTypeRepository leaveTypeRepository, LeavePolicyRepository leavePolicyRepository, LeaveApplicationRepository leaveApplicationRepository, EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository, EmployeeDesignationRepository employeeDesignationRepository, LeaveAttachmentRepository leaveAttachmentRepository, FileStorageService fileStorageService, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, HolidayRepository holidayRepository, LeaveApplicationStatusHistoryRepository leaveApplicationStatusHistoryRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.employeeLeaveBalanceRepository = employeeLeaveBalanceRepository;
        this.employeeDesignationRepository = employeeDesignationRepository;
        this.leaveAttachmentRepository = leaveAttachmentRepository;
        this.fileStorageService = fileStorageService;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.holidayRepository = holidayRepository;
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
        history.setSandwichLeave(validateSandwichLeave(employee, policy, fromDate, toDate));
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
        handleAttachments(sessionId, application, request.attachments(), request);

        return new LeaveApplicationResponse(
                application.getId(),
                true,
                List.of("Leave applied successfully")
        );
    }

    @Transactional
    public LeaveApplicationResponse applyForLeaveWithSubstitute(
            String employeeId,
            Long designationId,
            String departmentId,
            LeaveApplicationFormRequestSub request,
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

        String nextApprover = getNextApproverForHead(
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
        history.setSandwichLeave(validateSandwichLeave(employee, policy, fromDate, toDate));
        history.setApplicationStep(1);
        history.setNextApprovalRoleId(nextApprover);
        history.setCreatedOn(now);
        history.setIsActive(true);

        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
        status.setActionTakenOn(now);
        status.setActionTakenBy(LeaveActionRole.APPLICANT);
        status.setActionStatus(LeaveActionStatus.WAITING);

        LeaveApplicationSubstitute substitute = new LeaveApplicationSubstitute();
        substitute.setSubstituteEmployeeId(request.substituteId());

        history.addStatus(status);
        application.addHistory(history);
        application.setSubstitute(substitute);

        leaveApplicationRepository.save(application);

        // =========================
        // HANDLE FILES
        // =========================
        handleAttachments(sessionId, application, request.attachments(), request);

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
        LocalDate from = LocalDate.parse(request.from());
        LocalDate to = LocalDate.parse(request.to());

        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;

        // 1. Years of service
        validateMinimumService(employee, policy);

        // 2. Max duration per application
        validateApplicationDuration(policy, days);

        // 3. Ex Bangladesh
        validateExBangladesh(policy, request);

        // 4. Max times in career
        validateCareerLimit(employee, policy);

    }

    private void validatePolicy(Employee employee,
                                LeavePolicy policy,
                                LeaveApplicationFormRequestSub request) {

        LocalDate from = LocalDate.parse(request.from());
        LocalDate to = LocalDate.parse(request.to());

        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;

        // 1. Years of service
        validateMinimumService(employee, policy);

        // 2. Max duration per application
        validateApplicationDuration(policy, days);

        // 3. Ex Bangladesh
        validateExBangladesh(policy, request);

        // 4. Max times in career
        validateCareerLimit(employee, policy);

    }

    private void validateMinimumService(Employee employee, LeavePolicy policy) {

        if (policy.getMinYearsOfService() == null)
            return;

        long years =
                ChronoUnit.YEARS.between(
                        employee.getJoiningDate(),
                        LocalDate.now());

        if (years < policy.getMinYearsOfService()) {
            throw new BadRequestException(
                    "Minimum " +
                            policy.getMinYearsOfService() +
                            " years of service required.");
        }
    }

    private void validateApplicationDuration(
            LeavePolicy policy,
            int duration
    ) {

        Integer max = policy.getMaxDurationPerApplication();

        if (max != null && duration > max) {

            throw new BadRequestException(
                    "Maximum " + max +
                            " days allowed in one application.");
        }

    }

    private void validateExBangladesh(
            LeavePolicy policy,
            LeaveApplicationFormRequest request
    ) {

        if (Boolean.TRUE.equals(request.exBdLeave())
                && !Boolean.TRUE.equals(policy.getAllowedExBDLeave())) {

            throw new BadRequestException(
                    "This leave type cannot be taken as Ex-Bangladesh leave.");

        }

    }

    private void validateExBangladesh(
            LeavePolicy policy,
            LeaveApplicationFormRequestSub request
    ) {

        if (Boolean.TRUE.equals(request.exBdLeave())
                && !Boolean.TRUE.equals(policy.getAllowedExBDLeave())) {

            throw new BadRequestException(
                    "This leave type cannot be taken as Ex-Bangladesh leave.");

        }

    }

    private void validateCareerLimit(
            Employee employee,
            LeavePolicy policy
    ) {

        if (policy.getMaxTimesInCareer() == null)
            return;

        long count =
                leaveApplicationHistoryRepository.countApprovedApplications(
                        employee.getEmployeeId(),
                        policy.getLeaveType().getId());

        if (count >= policy.getMaxTimesInCareer()) {

            throw new BadRequestException(
                    "Maximum number of applications reached.");

        }

    }

    private void validateBalance(Employee employee,
                                 EmployeeLeaveBalance balance,
                                 LeaveApplicationFormRequest request) {
        int duration =
                (int) ChronoUnit.DAYS.between(
                        LocalDate.parse(request.from()),
                        LocalDate.parse(request.to()))
                        + 1;

        if (balance.getDaysLeft() < duration) {

            throw new BadRequestException(
                    "Insufficient leave balance.");

        }
    }

    private boolean validateSandwichLeave(
            Employee employee,
            LeavePolicy policy,
            LocalDate from,
            LocalDate to
    ) {

        if (Boolean.FALSE.equals(policy.getSandwichAllowed())) {
            return false;
        }

        if (!isSandwichLeave(from, to)) {
            return false;
        }

        int duration =
                (int) ChronoUnit.DAYS.between(from, to) + 1;

        if (policy.getMaxSandwichDaysPerApplication() != null
                && duration > policy.getMaxSandwichDaysPerApplication()) {

            throw new BadRequestException(
                    "Maximum sandwich leave is "
                            + policy.getMaxSandwichDaysPerApplication()
                            + " day(s).");
        }

        if (policy.getMaxSandwichPerYear() != null) {

            long count =
                    leaveApplicationHistoryRepository.countApprovedSandwichLeaves(
                            employee.getEmployeeId(),
                            from.getYear());

            if (count >= policy.getMaxSandwichPerYear()) {

                throw new BadRequestException(
                        "Maximum sandwich leave already used for "
                                + from.getYear());

            }
        }

        return true;
    }

    private boolean isSandwichLeave(
            LocalDate from,
            LocalDate to
    ) {

        LocalDate before = from.minusDays(1);
        LocalDate after = to.plusDays(1);

        return isNonWorkingDay(before)
                && isNonWorkingDay(after);

    }

    private boolean isHoliday(LocalDate date) {

        return holidayRepository
                .existsByIsEnabledTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        date,
                        date
                );

    }

    private boolean isWeekend(LocalDate date) {

        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.FRIDAY
                || day == DayOfWeek.SATURDAY;

    }

    private boolean isNonWorkingDay(LocalDate date) {

        return isWeekend(date) || isHoliday(date);

    }

    private void validateBalance(Employee employee,
                                 EmployeeLeaveBalance balance,
                                 LeaveApplicationFormRequestSub request) {
        int duration =
                (int) ChronoUnit.DAYS.between(
                        LocalDate.parse(request.from()),
                        LocalDate.parse(request.to()))
                        + 1;

        if (balance.getDaysLeft() < duration) {

            throw new BadRequestException(
                    "Insufficient leave balance.");

        }
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

    private String getNextApproverForHead(LeaveType leaveType,
                                   EmployeeDesignation designation,
                                   String departmentId,
                                   Integer step,
                                   Boolean isExBdLeave) {

        // TODO: check LeaveApprovalFlow and LeaveApprovalFlowConditional and return the roleId from IUMS

        return "7001"; // sample role ID
    }

    private void handleAttachments(String sessionId, LeaveApplication application, List<LeaveAttachmentRequest> attachments, LeaveApplicationFormRequest request) {
        if (attachments != null) {

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
    }

    private void handleAttachments(String sessionId, LeaveApplication application, List<LeaveAttachmentRequest> attachments, LeaveApplicationFormRequestSub request) {
        if (attachments != null) {

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
    }

}
