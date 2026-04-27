package org.aust.lms.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class LeaveAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private Long fileSize;
    private String description;

    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_application_id", nullable = false)
    private LeaveApplication leaveApplication;

    public Long getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getDescription() {
        return description;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public LeaveApplication getLeaveApplication() {
        return leaveApplication;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setLeaveApplication(LeaveApplication leaveApplication) {
        this.leaveApplication = leaveApplication;
    }

    public void setFileSize(long size) {
    }
}