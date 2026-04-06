package org.aust.lms.controller;

import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.LeaveApprovalListResponse;
import org.aust.lms.dto.LeaveApprovalRequest;
import org.aust.lms.dto.LeaveApprovalResponse;
import org.aust.lms.service.LeaveApprovalQueryService;
import org.aust.lms.service.LeaveApprovalService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave")
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    private final LeaveApprovalQueryService leaveApprovalQueryService;

    public LeaveApprovalController(LeaveApprovalService leaveApprovalService, LeaveApprovalQueryService leaveApprovalQueryService) {
        this.leaveApprovalService = leaveApprovalService;
        this.leaveApprovalQueryService = leaveApprovalQueryService;
    }

    @GetMapping("/pending/{roleId}")
    public ResponseEntity<Page<LeaveApprovalListResponse>> getPendingApplications(
            @PathVariable String roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicationStage,
            @RequestParam(required = false) String leaveType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                leaveApprovalQueryService.getPendingApplications(
                        roleId,
                        status,
                        applicationStage,
                        leaveType,
                        page,
                        size
                )
        );
    }

    @PostMapping("/approve")
    public ResponseEntity<LeaveApprovalResponse> approveLeave(
            @RequestBody LeaveApprovalRequest request
    ) {
        return ResponseEntity.ok(
                leaveApprovalService.processApproval(request)
        );
    }
}