package org.aust.lms.repository;

import org.aust.lms.entity.EmployeeDesignation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeDesignationRepository extends CrudRepository<EmployeeDesignation, Long> {
}
