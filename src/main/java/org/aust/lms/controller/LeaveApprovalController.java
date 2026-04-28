package org.aust.lms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aust.lms.dto.*;
import org.aust.lms.service.LeaveApprovalQueryService;
import org.aust.lms.service.LeaveApprovalService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Leave Approval API", description = "APIs related to the approval process and approval flow of a leave application")
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
    public ResponseEntity<LeaveApprovalPageResponse> getPendingApplications(
            @PathVariable String roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicationStage,
            @RequestParam(required = false) String leaveType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return ResponseEntity.ok(
                leaveApprovalQueryService.getPendingApplications(
                        roleId,
                        status,
                        applicationStage,
                        leaveType,
                        page,
                        size,
                        sortBy,
                        sortDir
                )
        );
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<LeaveApplicationDetailsResponse> getDetails(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(
                leaveApprovalService.getApplicationDetails(applicationId)
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