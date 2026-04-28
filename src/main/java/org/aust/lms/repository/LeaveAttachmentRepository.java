package org.aust.lms.repository;

import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {
    List<LeaveAttachment> findByLeaveApplication(LeaveApplication leaveApplication);
}
