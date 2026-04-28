package org.aust.lms.service;

import org.aust.lms.dto.LeaveBalanceListResponse;
import org.aust.lms.dto.LeaveBalanceResponse;
import org.aust.lms.entity.Employee;
import org.aust.lms.repository.EmployeeLeaveBalanceRepository;
import org.aust.lms.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.List;

@Service
public class LeaveBalanceService {

    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveBalanceService(EmployeeLeaveBalanceRepository leaveBalanceRepository, EmployeeRepository employeeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public LeaveBalanceListResponse getEmployeeLeaveBalance(String employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<LeaveBalanceResponse> balances =
                leaveBalanceRepository.findByEmployeeId(employeeId);

        return new LeaveBalanceListResponse(
                employeeId,
                true, // or custom logic
                false, // TODO: derive from employee.gender
                balances
        );
    }
}