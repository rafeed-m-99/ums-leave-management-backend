package org.aust.lms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.aust.lms.dto.LeaveApplicationFormRequest;
import org.aust.lms.dto.LeaveApplicationResponse;
import org.aust.lms.dto.LeaveApplicationUpdateRequest;
import org.aust.lms.dto.TempUploadResponse;
import org.aust.lms.entity.LeaveAttachment;
import org.aust.lms.repository.LeaveAttachmentRepository;
import org.aust.lms.service.FileStorageService;
import org.aust.lms.service.LeaveApplicationFormService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Leave Application Form API", description = "Leave Application History form and attachments related APIs for an Employee")
@RestController
@RequestMapping("/api/leave")
public class LeaveApplicationFormController {

    private final LeaveApplicationFormService leaveApplicationFormService;
    private final FileStorageService fileStorageService;

    private final LeaveAttachmentRepository attachmentRepo;

    public LeaveApplicationFormController(LeaveApplicationFormService leaveApplicationFormService, FileStorageService fileStorageService, LeaveAttachmentRepository attachmentRepo) {
        this.leaveApplicationFormService = leaveApplicationFormService;
        this.fileStorageService = fileStorageService;
        this.attachmentRepo = attachmentRepo;
    }

    @PostMapping("/apply/{employeeId}/{designationId}/{departmentId}")
    public ResponseEntity<LeaveApplicationResponse> applyForLeave(
            @PathVariable String employeeId,
            @PathVariable Long designationId,
            @PathVariable String departmentId,
            @RequestBody LeaveApplicationFormRequest request,
            @RequestParam(required = false) String sessionId
    ) {

        LeaveApplicationResponse response =
                leaveApplicationFormService.applyForLeave(
                        employeeId,
                        designationId,
                        departmentId,
                        request,
                        sessionId
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/modify/{applicationId}")
    public ResponseEntity<LeaveApplicationResponse> modifyLeave(
            @PathVariable Long applicationId,
            @RequestBody LeaveApplicationUpdateRequest request
    ) {
        LeaveApplicationResponse response =
                leaveApplicationFormService.modifyApplication(applicationId, request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/cancel/{applicationId}")
    public ResponseEntity<LeaveApplicationResponse> cancelLeave(
            @PathVariable Long applicationId
    ) {
        LeaveApplicationResponse response =
                leaveApplicationFormService.cancelApplication(applicationId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/attachments/temp/{sessionId}")
    public ResponseEntity<TempUploadResponse> uploadTemp(
            @PathVariable String sessionId,
            @RequestParam MultipartFile file) {

        return ResponseEntity.ok(
                fileStorageService.saveTemp(file, sessionId)
        );
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {

        LeaveAttachment att = attachmentRepo.findById(id)
                .orElseThrow();

        Resource file = fileStorageService.loadAsResource(
                att.getStoredFileName(),
                att.getLeaveApplication().getId()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + att.getOriginalFileName() + "\"")
                .body(file);
    }

//    @DeleteMapping("/attachments/{id}")
//    public ResponseEntity<?> delete(@PathVariable Long id,
//                                    @AuthenticationPrincipal User user) {
//
//        leaveService.deleteAttachment(id, user.getId());
//        return ResponseEntity.ok("Deleted");
//    }

//    /**
//     * CONVERT EARNED LEAVE
//     */
//    @PostMapping("/convert-el/{employeeId}")
//    public ResponseEntity<?> convertEarnedLeave(
//            @PathVariable String employeeId
//    ) {
//
//        // TODO: call service layer
//        // leaveService.convertEarnedLeave(employeeId);
//
//        return ResponseEntity.ok("Earned leave converted successfully");
//    }

}
