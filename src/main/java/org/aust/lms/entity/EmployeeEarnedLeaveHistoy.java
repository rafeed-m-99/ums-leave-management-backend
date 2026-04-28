package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.EarnedLeaveChangeType;
import org.aust.lms.enums.LeaveSalaryType;

import java.time.*;

@Entity
@Table(name = "emp_el_history")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class EmployeeEarnedLeaveHistoy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private EarnedLeaveChangeType changeType;

    private Integer credit;

    private Integer debit;

    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    private LeaveSalaryType balanceType;

    @Column(length = 18)
    private String modifiedBy;

    private LocalDate entryDate;
}
