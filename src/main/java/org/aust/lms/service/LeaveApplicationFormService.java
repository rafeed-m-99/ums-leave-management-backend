package org.aust.lms.service;

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
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    public LeaveApplicationFormService(EmployeeRepository employeeRepository, LeaveTypeRepository leaveTypeRepository, LeavePolicyRepository leavePolicyRepository, LeaveApplicationRepository leaveApplicationRepository, EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository, EmployeeDesignationRepository employeeDesignationRepository, LeaveAttachmentRepository leaveAttachmentRepository, FileStorageService fileStorageService) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.employeeLeaveBalanceRepository = employeeLeaveBalanceRepository;
        this.employeeDesignationRepository = employeeDesignationRepository;
        this.leaveAttachmentRepository = leaveAttachmentRepository;
        this.fileStorageService = fileStorageService;
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
        history.setFronDate(fromDate);
        history.setToDate(toDate);
        history.setTotalDays(totalDays);
        history.setReason(request.reason());
        history.setExBangladeshLeave(request.exBdLeave());
        history.setSandwichLeave(false);
        history.setApplicationStep(1);
        history.setNextApprovalRoleId(nextApprover);
        history.setCreatedOn(now);

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
