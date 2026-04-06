package org.aust.lms.repository;

import org.aust.lms.dto.LeaveBalanceResponse;
import org.aust.lms.entity.Employee;
import org.aust.lms.entity.EmployeeLeaveBalance;
import org.aust.lms.entity.LeaveType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface EmployeeLeaveBalanceRepository extends CrudRepository<EmployeeLeaveBalance, Long> {
    Optional<List<EmployeeLeaveBalance>> findByEmployee(Employee employee);

    Optional<EmployeeLeaveBalance> findByEmployeeAndLeaveType(Employee employee, LeaveType leaveType);

    @Query("""
    SELECT new org.aust.lms.dto.LeaveBalanceResponse(
        lb.id,
        lt.id,
        lt.name,
        CAST(lp.genderApplicable as string),
        CAST(lp.applicantType as string),
        lb.daysLeft,
        lb.sandwichLeaveTaken,
        lb.halfPayToFullPayConverted,
        lb.additionalDaysStored
    )
    FROM EmployeeLeaveBalance lb
    JOIN lb.leaveType lt
    JOIN LeavePolicy lp ON lp.leaveType = lt
    WHERE lb.employee.employeeId = :employeeId
""")
    List<LeaveBalanceResponse> findByEmployeeId(@Param("employeeId") String employeeId);
}
