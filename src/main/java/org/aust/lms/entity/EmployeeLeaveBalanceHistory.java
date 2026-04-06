package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "emp_leave_balance_history")
@NoArgsConstructor @AllArgsConstructor @ToString
public class EmployeeLeaveBalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    private Integer credit;

    private Integer debit;

    private Integer balanceAfter;

    private LocalDate entryDate;

    private Instant lastModified;
}
