package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class Holiday {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holiday_type_id")
    private HolidayType holidayType;

    private Integer year;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Instant lastModified;

    private Boolean isEnabled;
}
