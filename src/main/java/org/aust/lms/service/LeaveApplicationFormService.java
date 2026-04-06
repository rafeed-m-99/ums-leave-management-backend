package org.aust.lms.service;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveApplicationFormRequest;
import org.aust.lms.dto.LeaveApplicationResponse;
import org.aust.lms.entity.*;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.aust.lms.enums.LeaveApprovalRole;
import org.aust.lms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveApplicationFormService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeavePolicyRepository leavePolicyRepository;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;
    private final LeaveApplicationStatusHistoryRepository statusHistoryRepository;
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
    private final EmployeeDesignationRepository employeeDesignationRepository;

    public LeaveApplicationFormService(EmployeeRepository employeeRepository, LeaveTypeRepository leaveTypeRepository, LeavePolicyRepository leavePolicyRepository, LeaveApplicationRepository leaveApplicationRepository, LeaveApplicationHistoryRepository leaveApplicationHistoryRepository, LeaveApplicationStatusHistoryRepository statusHistoryRepository, EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository, EmployeeDesignationRepository employeeDesignationRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveApplicationHistoryRepository = leaveApplicationHistoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.employeeLeaveBalanceRepository = employeeLeaveBalanceRepository;
        this.employeeDesignationRepository = employeeDesignationRepository;
    }

    @Transactional
    public LeaveApplicationResponse applyForLeave(String employeeId, Long designationId, String departmentId, LeaveApplicationFormRequest request) {
        List<String> messages = new ArrayList<>();

        // STEP 0: Fetch required data
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId()).orElse(null);
        EmployeeDesignation employeeDesignation = employeeDesignationRepository.findById(designationId).orElse(null);

        if (employee == null) {
            messages.add("Employee not found");
            return new LeaveApplicationResponse(false, messages);
        }

        if (leaveType == null) {
            messages.add("Invalid leave type");
            return new LeaveApplicationResponse(false, messages);
        }

        if (employeeDesignation == null) {
            messages.add("Invalid employee designation");
            return new LeaveApplicationResponse(false, messages);
        }

        LeavePolicy policy = leavePolicyRepository.findByLeaveType(leaveType).orElse(null);

        if (policy == null) {
            messages.add("No policy defined for this leave type.");
            return new LeaveApplicationResponse(false, messages);
        }

        EmployeeLeaveBalance employeeLeaveBalance = employeeLeaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType).orElse(null);

        if (employeeLeaveBalance == null) {
            messages.add("Employee is not eligible for this leave type.");
            return new LeaveApplicationResponse(false, messages);
        }

        // STEP 1: POLICY and LEAVE BALANCE VALIDATION
        validatePolicy(employee, policy, request, messages);

        validateBalance(employee, employeeLeaveBalance, request, messages);

        if (!messages.isEmpty()) {
            return new LeaveApplicationResponse(false, messages);
        }

        // =========================
        // 3. GET NEXT APPROVER ROLE
        // =========================
        String nextApprover = getNextApprover(
                leaveType,
                employeeDesignation,
                departmentId,
                request.applicationStep(),
                request.exBdLeave()
        );

        // =========================
        // 4. SAVE APPLICATION
        // =========================

        LeaveApplication application = new LeaveApplication();
        application.setEmployee(employee);
        application.setLeaveType(leaveType);
        application.setAppliedOn(Instant.now());
        application.setCreatedOn(Instant.now());

        application = leaveApplicationRepository.save(application);

        // =========================
        // 5. SAVE APPLICATION HISTORY
        // =========================

        LocalDate fromDate = LocalDate.parse(request.from());
        LocalDate toDate = LocalDate.parse(request.to());

        int totalDays = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        LeaveApplicationHistory history = new LeaveApplicationHistory();
        history.setApplication(application);
        history.setApplicationStage(LeaveApplicationStage.INITIAL);
        history.setFronDate(fromDate);
        history.setToDate(toDate);
        history.setTotalDays(totalDays);
        history.setReason(request.reason());
        history.setExBangladeshLeave(request.exBdLeave());
        history.setSandwichLeave(false); // need advanced checking for sandwich casual leave
        history.setApplicationStep(1);
        history.setNextApprovalRoleId(nextApprover);
        history.setCreatedOn(Instant.now());

        history = leaveApplicationHistoryRepository.save(history);

        // =========================
        // 6. SAVE STATUS HISTORY
        // =========================

        LeaveApplicationStatusHistory status = new LeaveApplicationStatusHistory();
        status.setApplicationHistory(history);
        status.setActionTakenOn(Instant.now());
        status.setActionTakenBy(LeaveApprovalRole.APPLICANT);
        status.setComment(null);
        status.setActionStatus(LeaveActionStatus.WAITING);

        statusHistoryRepository.save(status);

        return new LeaveApplicationResponse(true, List.of("Leave applied successfully"));
    }

    // =========================================================
    // POLICY VALIDATION
    // =========================================================
    private void validatePolicy(Employee employee,
                                LeavePolicy policy,
                                LeaveApplicationFormRequest request,
                                List<String> messages) {

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
//        // 👉 Add more rules here as needed
    }

    private void validateBalance(Employee employee,
                                 EmployeeLeaveBalance employeeLeaveBalance,
                                 LeaveApplicationFormRequest request,
                                 List<String> messages) {

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
