package org.aust.lms.controller;

import org.aust.lms.dto.ApplicantLeaveDetailsResponse;
import org.aust.lms.dto.ApplicantLeaveListResponse;
import org.aust.lms.service.LeaveApplicationQueryService;
import org.aust.lms.service.LeaveApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveApplicationController {

    private final LeaveApplicationQueryService leaveApplicationQueryService;
    private final LeaveApplicationService leaveApplicationService;

    public LeaveApplicationController(LeaveApplicationQueryService leaveApplicationQueryService, LeaveApplicationService leaveApplicationService) {
        this.leaveApplicationQueryService = leaveApplicationQueryService;
        this.leaveApplicationService = leaveApplicationService;
    }

    @GetMapping("/applicant/{employeeId}")
    public ResponseEntity<Page<ApplicantLeaveListResponse>> getApplicantLeaves(
            @PathVariable String employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String actionTakenBy,
            @RequestParam(required = false) String nextRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        System.out.println("Payload: " + status + "," + actionTakenBy + "," + nextRole + "," + page + "," + size);
        List<ApplicantLeaveListResponse> allLeaves = leaveApplicationQueryService
                .getApplicantLeaveList(employeeId, status, actionTakenBy, nextRole);

        int start = page * size;
        int end = Math.min(start + size, allLeaves.size());

        List<ApplicantLeaveListResponse> pagedLeaves = start > allLeaves.size() ? List.of() : allLeaves.subList(start, end);

        Page<ApplicantLeaveListResponse> pageResponse = new org.springframework.data.domain.PageImpl<>(
                pagedLeaves,
                PageRequest.of(page, size),
                allLeaves.size()
        );

        return ResponseEntity.ok(pageResponse);
    }

    @GetMapping("/applicant/{employeeId}/{applicationId}")
    public ResponseEntity<List<ApplicantLeaveDetailsResponse>> getLeaveDetails(
            @PathVariable String employeeId,
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(
                leaveApplicationService.getLeaveDetails(employeeId, applicationId)
        );
    }
}
