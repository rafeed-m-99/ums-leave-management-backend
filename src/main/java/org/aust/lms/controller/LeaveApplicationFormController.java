package org.aust.lms.controller;

import org.aust.lms.dto.LeaveApplicationFormRequest;
import org.aust.lms.dto.LeaveApplicationResponse;
import org.aust.lms.service.LeaveApplicationFormService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave")
public class LeaveApplicationFormController {

    public final LeaveApplicationFormService leaveApplicationFormService;

    public LeaveApplicationFormController(LeaveApplicationFormService leaveApplicationFormService) {
        this.leaveApplicationFormService = leaveApplicationFormService;
    }

    /**
     * APPLY FOR A LEAVE
     * Matches: POST /api/leave/apply/{employeeId}
     */
    @PostMapping("/apply/{employeeId}/{designationId}/{departmentId}")
    public ResponseEntity<LeaveApplicationResponse> applyForLeave(
            @PathVariable String employeeId,
            @PathVariable Long designationId,
            @PathVariable String departmentId,
            @RequestBody LeaveApplicationFormRequest request
    ) {

        // TODO: call service layer
        LeaveApplicationResponse response = leaveApplicationFormService.applyForLeave(employeeId, designationId, departmentId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * CONVERT EARNED LEAVE
     */
    @PostMapping("/convert-el/{employeeId}")
    public ResponseEntity<?> convertEarnedLeave(
            @PathVariable String employeeId
    ) {

        // TODO: call service layer
        // leaveService.convertEarnedLeave(employeeId);

        return ResponseEntity.ok("Earned leave converted successfully");
    }

}
