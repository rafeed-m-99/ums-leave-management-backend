package org.aust.lms.repository;

import org.aust.lms.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByIsEnabledTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            LocalDate date1,
            LocalDate date2
    );
}
