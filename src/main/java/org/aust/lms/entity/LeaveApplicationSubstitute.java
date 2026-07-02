package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "leave_application_substitute")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LeaveApplicationSubstitute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private LeaveApplication application;

    private String substituteEmployeeId;

    public Long getId() {
        return id;
    }

    public LeaveApplication getApplication() {
        return application;
    }

    public String getSubstituteEmployeeId() {
        return substituteEmployeeId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApplication(LeaveApplication application) {
        this.application = application;
    }

    public void setSubstituteEmployeeId(String substituteEmployeeId) {
        this.substituteEmployeeId = substituteEmployeeId;
    }
}
