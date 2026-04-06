package org.aust.lms.controller;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveBalanceListResponse;
import org.aust.lms.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave/balance")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<LeaveBalanceListResponse> getEmployeeLeaveBalance(
            @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(
                leaveBalanceService.getEmployeeLeaveBalance(employeeId)
        );
    }
}