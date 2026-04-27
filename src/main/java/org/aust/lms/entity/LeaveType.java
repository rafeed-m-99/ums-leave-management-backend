package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.GenderRestriction;

import java.time.Instant;

@Entity
@Table(name = "leave_type")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Boolean isEnabled;

    private Instant lastModified;

    @OneToOne(mappedBy = "leaveType", cascade = CascadeType.ALL, orphanRemoval = true)
    private LeavePolicy policy;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean getEnabled() {
        return isEnabled;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public LeavePolicy getPolicy() {
        return policy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }
}
